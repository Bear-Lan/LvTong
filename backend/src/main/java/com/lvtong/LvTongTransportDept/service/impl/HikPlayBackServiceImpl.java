package com.lvtong.LvTongTransportDept.service.impl;

import com.lvtong.LvTongTransportDept.hksdk.HCNetSDK;
import com.lvtong.LvTongTransportDept.service.HikPlayBackService;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 海康威视 NVR 回放服务
 * 关键改进：
 * 1. 启动顺序：先启动 FFmpeg（等待就绪），再启动海康回放，避免数据丢失
 * 2. FFmpeg 参数优化：去除无效参数，减少 probesize/analyzeduration，考虑 copy 模式
 * 3. 错误处理和超时机制：FFmpeg 启动失败清理海康资源，海康失败强制终止 FFmpeg
 * 4. 数据等待超时：FFmpeg 等待数据超时时安全释放资源
 */
@Slf4j
@Service
public class HikPlayBackServiceImpl implements HikPlayBackService {

    private static final String NVR_IP = "192.168.218.250";
    private static final int NVR_PORT = 8000;
    private static final String NVR_USERNAME = "admin";
    private static final String NVR_PASSWORD = "whds@2025";

    private static final String FFMPEG_PATH = "D:\\tools\\ffmpeg-master-latest-win64-gpl\\bin\\ffmpeg.exe";

    private static final int FFMPEG_START_TIMEOUT_SEC = 10;
    private static final int FFMPEG_DATA_TIMEOUT_SEC = 30;

    @Autowired
    private HCNetSDK hcNetSDK;

    private volatile AtomicReference<PlaybackResources> currentResources = new AtomicReference<>();
    private final ThreadLocal<PlaybackResources> threadResources = new ThreadLocal<>();

    private static class PlaybackResources {
        NativeLong playHandle;
        final NativeLong userId;
        final CountDownLatch stopLatch;
        final AtomicReference<Process> ffmpegProcess = new AtomicReference<>();
        final AtomicReference<OutputStream> ffmpegStdinRef = new AtomicReference<>();
        final AtomicLong bytesWritten = new AtomicLong(0);
        final AtomicLong lastDataTime = new AtomicLong(System.currentTimeMillis());
        final AtomicLong stopped = new AtomicLong(0);

        PlaybackResources(NativeLong playHandle, NativeLong userId, CountDownLatch stopLatch) {
            this.playHandle = playHandle;
            this.userId = userId;
            this.stopLatch = stopLatch;
        }

        boolean isStopped() {
            return stopped.get() != 0;
        }

        void markStopped() {
            stopped.set(1);
        }
    }

    @Override
    public void playBackByTime(LocalDateTime startTime, LocalDateTime endTime, int channel, OutputStream outputStream) {
        log.info("流式回放开始: startTime={}, endTime={}, channel={}", startTime, endTime, channel);

        CountDownLatch stopLatch = new CountDownLatch(1);

        if (!hcNetSDK.NET_DVR_Init()) {
            log.error("SDK初始化失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
            return;
        }

        hcNetSDK.NET_DVR_SetConnectTime(2000, 1);
        hcNetSDK.NET_DVR_SetReconnect(100000, true);

        HCNetSDK.NET_DVR_DEVICEINFO_V30 deviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
        NativeLong userId = hcNetSDK.NET_DVR_Login_V30(NVR_IP, (short) NVR_PORT, NVR_USERNAME, NVR_PASSWORD, deviceInfo);
        if (userId.intValue() == -1) {
            log.error("登录失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
            hcNetSDK.NET_DVR_Cleanup();
            return;
        }
        log.info("登录成功，userId={}", userId);

        NativeLong playHandle = new NativeLong(-1);
        Process ffmpegProcess = null;
        final PlaybackResources[] resourcesHolder = new PlaybackResources[1];

        try {
            final NativeLong finalPlayHandle = playHandle;
            PlaybackResources resources = new PlaybackResources(finalPlayHandle, userId, stopLatch);
            resourcesHolder[0] = resources;
            currentResources.set(resources);
            threadResources.set(resources);

            log.info("启动 FFmpeg 进程（等待数据写入）...");
            ffmpegProcess = startFfmpegWithPipe(outputStream, resources);
            resources.ffmpegProcess.set(ffmpegProcess);
            log.info("FFmpeg 进程已启动, pid={}", ffmpegProcess.pid());

            if (!waitForFfmpegReady(ffmpegProcess, FFMPEG_START_TIMEOUT_SEC)) {
                log.error("FFmpeg 启动超时（{}秒），强制终止", FFMPEG_START_TIMEOUT_SEC);
                ffmpegProcess.destroyForcibly();
                hcNetSDK.NET_DVR_Logout(userId);
                hcNetSDK.NET_DVR_Cleanup();
                return;
            }
            log.info("FFmpeg 已就绪，可以接受数据输入");

            final OutputStream ffmpegStdin = ffmpegProcess.getOutputStream();
            resources.ffmpegStdinRef.set(ffmpegStdin);

            HCNetSDK.NET_DVR_VOD_PARA vodPara = new HCNetSDK.NET_DVR_VOD_PARA();
            vodPara.dwSize = vodPara.size();
            vodPara.struIDInfo.dwSize = vodPara.struIDInfo.size();
            vodPara.struIDInfo.dwChannel = 33 + (channel - 1);

            vodPara.struBeginTime.dwYear = startTime.getYear();
            vodPara.struBeginTime.dwMonth = startTime.getMonthValue();
            vodPara.struBeginTime.dwDay = startTime.getDayOfMonth();
            vodPara.struBeginTime.dwHour = startTime.getHour();
            vodPara.struBeginTime.dwMinute = startTime.getMinute();
            vodPara.struBeginTime.dwSecond = startTime.getSecond();

            vodPara.struEndTime.dwYear = endTime.getYear();
            vodPara.struEndTime.dwMonth = endTime.getMonthValue();
            vodPara.struEndTime.dwDay = endTime.getDayOfMonth();
            vodPara.struEndTime.dwHour = endTime.getHour();
            vodPara.struEndTime.dwMinute = endTime.getMinute();
            vodPara.struEndTime.dwSecond = endTime.getSecond();
            vodPara.hWnd = null;
            vodPara.write();

            log.info("启动海康回放，通道={}, dwChannel={}", channel, vodPara.struIDInfo.dwChannel);

            playHandle = hcNetSDK.NET_DVR_PlayBackByTime_V40(userId, vodPara);
            resources.playHandle = playHandle;

            if (playHandle.intValue() == -1) {
                log.error("按时间回放失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
                forceStopFfmpeg(ffmpegProcess);
                hcNetSDK.NET_DVR_Logout(userId);
                hcNetSDK.NET_DVR_Cleanup();
                return;
            }
            log.info("回放句柄: {}", playHandle);

            IntByReference intInlen = new IntByReference(0);
            boolean bCtrl = hcNetSDK.NET_DVR_PlayBackControl_V40(playHandle, HCNetSDK.NET_DVR_PLAYSTART, Pointer.NULL, 0, Pointer.NULL, intInlen);
            if (!bCtrl) {
                log.error("NET_DVR_PLAYSTART失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
                hcNetSDK.NET_DVR_StopPlayBack(playHandle);
                forceStopFfmpeg(ffmpegProcess);
                hcNetSDK.NET_DVR_Logout(userId);
                hcNetSDK.NET_DVR_Cleanup();
                return;
            }
            log.info("PLAYSTART 指令发送成功");

            final OutputStream ffmpegStdinRef = ffmpegStdin;
            final AtomicLong bytesWritten = resources.bytesWritten;
            final AtomicLong lastDataTimeRef = new AtomicLong(System.currentTimeMillis());

            HCNetSDK.FPlayDataCallBack callback = new HCNetSDK.FPlayDataCallBack() {
                public void invoke(int lPlayHandle, int dwDataType, Pointer pBuffer, int dwBufSize, int dwUser) {
                    if (dwBufSize <= 0 || pBuffer == null) return;
                    if (resources.isStopped()) return;

                    try {
                        ByteBuffer buf = pBuffer.getByteBuffer(0, dwBufSize);
                        byte[] data = new byte[dwBufSize];
                        buf.get(data);

                        ffmpegStdinRef.write(data);
                        ffmpegStdinRef.flush();

                        lastDataTimeRef.set(System.currentTimeMillis());
                        bytesWritten.addAndGet(dwBufSize);
                    } catch (IOException e) {
                        log.warn("FFmpeg stdin 写入异常: {}", e.getMessage());
                        resources.markStopped();
                    }
                }
            };
            hcNetSDK.NET_DVR_SetPlayDataCallBack(playHandle, callback, Pointer.NULL);
            log.info("数据回调已设置，开始从第一帧采集数据");

            Thread dataTimeoutThread = new Thread(() -> {
                while (!resources.isStopped() && !Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastDataTimeRef.get() > FFMPEG_DATA_TIMEOUT_SEC * 1000L) {
                        log.warn("FFmpeg 等待数据超时（{}秒），数据源无响应，强制停止", FFMPEG_DATA_TIMEOUT_SEC);
                        resources.markStopped();
                        resources.stopLatch.countDown();
                        break;
                    }
                }
            });
            dataTimeoutThread.setDaemon(true);
            dataTimeoutThread.start();

            log.info("等待回放停止信号...");
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            long timeoutSeconds = Math.max(180L, Math.min((long) (durationSeconds * 1.5), 600L));
            log.info("录像时长 {}s，设置超时 {}s", durationSeconds, timeoutSeconds);

            boolean signalled = stopLatch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (signalled) {
                log.info("收到停止信号，停止数据采集");
            } else {
                log.info("等待超时（{}s），自动停止数据采集", timeoutSeconds);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("回放被中断");
        } catch (Exception e) {
            log.error("回放异常: {}", e.getMessage());
        } finally {
            cleanupResources(resourcesHolder[0], ffmpegProcess, userId);
        }

        log.info("流式回放结束");
    }

    private Process startFfmpegWithPipe(OutputStream outputStream, PlaybackResources resources) throws IOException {
        String[] ffmpegArgs = new String[]{
                FFMPEG_PATH,
                "-i", "pipe:0",
                "-probesize", "1M",
                "-analyzeduration", "500K",
                "-fflags", "nobuffer",
                "-flags", "low_delay",
                "-c:v", "libx264",
                "-preset", "fast",
                "-crf", "23",
                "-vf", "scale='min(1920,iw)':-2",
                "-an",
                "-movflags", "frag_keyframe+empty_moov+faststart",
                "-f", "mp4",
                "pipe:1"
        };

        ProcessBuilder pb = new ProcessBuilder(ffmpegArgs);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        final Process ffmpegProc = process;
        Thread errorThread = new Thread(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(ffmpegProc.getErrorStream()));
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    log.debug("FFmpeg stderr: {}", line);
                }
            } catch (IOException e) {
                // ignore
            }
        });
        errorThread.setDaemon(true);
        errorThread.start();

        final long startTime = System.currentTimeMillis();
        final PlaybackResources res = resources;
        Thread outputThread = new Thread(() -> {
            byte[] buffer = new byte[8192];
            try {
                long totalBytes = 0;
                long lastLogTime = System.currentTimeMillis();
                int bytesRead;
                while ((bytesRead = process.getInputStream().read(buffer)) != -1) {
                    if (res.isStopped()) break;
                    try {
                        outputStream.write(buffer, 0, bytesRead);
                        outputStream.flush();
                    } catch (IOException e) {
                        log.info("HTTP 连接已断开，停止输出");
                        break;
                    }
                    totalBytes += bytesRead;
                    long now = System.currentTimeMillis();
                    if (now - lastLogTime > 5000) {
                        lastLogTime = now;
                        long elapsed = (now - startTime) / 1000;
                        log.info("FFmpeg 输出中... 已输出 {} bytes, 已耗时 {}s", totalBytes, elapsed);
                    }
                }
                log.info("FFmpeg 输出完成，共 {} bytes", totalBytes);
            } catch (IOException e) {
                log.warn("FFmpeg 输出异常: {}", e.getMessage());
            }
        });
        outputThread.setDaemon(true);
        outputThread.start();

        Thread monitorThread = new Thread(() -> {
            try {
                boolean exited = process.waitFor(3, TimeUnit.SECONDS);
                if (exited && process.exitValue() != 0) {
                    log.error("FFmpeg 进程异常退出，exitCode={}", process.exitValue());
                    res.markStopped();
                    res.stopLatch.countDown();
                } else if (exited) {
                    log.info("FFmpeg 进程正常退出");
                    res.markStopped();
                    res.stopLatch.countDown();
                }
            } catch (InterruptedException e) {
                // ignore
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();

        return process;
    }

    private boolean waitForFfmpegReady(Process process, int timeoutSec) {
        final long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        final StringBuffer output = new StringBuffer();

        log.info("开始等待 FFmpeg 就绪，超时 {} 秒", timeoutSec);

        // 单独线程持续读取 stderr，避免数据被截断
        Thread readerThread = new Thread(() -> {
            try {
                byte[] buf = new byte[1024];
                while (System.currentTimeMillis() < deadline && process.isAlive()) {
                    int n = process.getErrorStream().read(buf);
                    if (n > 0) {
                        String chunk = new String(buf, 0, n);
                        output.append(chunk);
                        log.debug("FFmpeg stderr chunk: {}", chunk.replace("\n", "\\n"));
                    } else if (n < 0) {
                        log.debug("FFmpeg stderr 读取结束");
                        break;
                    } else {
                        Thread.sleep(20);
                    }
                }
            } catch (Exception e) {
                log.debug("FFmpeg stderr 读取异常: {}", e.getMessage());
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();

        // 等待 FFmpeg 输出就绪信号
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                log.error("FFmpeg 进程启动期间退出，exitCode={}", process.exitValue());
                return false;
            }
            String sofar = output.toString();
            if (sofar.contains("Press [q] to stop")) {
                log.info("检测到 FFmpeg 就绪信号: Press [q] to stop");
                return true;
            }
            if (sofar.contains("configuration:")) {
                log.info("检测到 FFmpeg 就绪信号: configuration (版权信息说明已就绪)");
                return true;
            }
            if (sofar.contains("libavformat")) {
                log.info("检测到 FFmpeg 就绪信号: libavformat (库版本说明已就绪)");
                return true;
            }
            if (sofar.contains("Input #0")) {
                log.info("检测到 FFmpeg 就绪信号: Input #0");
                return true;
            }
            if (sofar.contains("Stream mapping")) {
                log.info("检测到 FFmpeg 就绪信号: Stream mapping");
                return true;
            }
            if (sofar.contains("Error") || sofar.contains("error") || sofar.contains("Invalid")) {
                log.error("FFmpeg 启动错误: {}", sofar);
                return false;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                return false;
            }
        }
        String sofar = output.toString();
        log.warn("等待 FFmpeg 就绪超时（{}秒），已输出: {}", timeoutSec, sofar.length() > 300 ? sofar.substring(0, 300) + "..." : sofar);
        return false;
    }

    private void forceStopFfmpeg(Process process) {
        if (process != null && process.isAlive()) {
            log.info("强制停止 FFmpeg 进程, pid={}", process.pid());
            process.destroy();
            try {
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void cleanupResources(PlaybackResources resources, Process ffmpegProcess, NativeLong userId) {
        log.info("Finally块开始执行，清理资源...");

        if (resources != null && resources.playHandle != null && resources.playHandle.intValue() > 0) {
            hcNetSDK.NET_DVR_StopPlayBack(resources.playHandle);
        }

        if (userId != null && userId.intValue() > 0) {
            hcNetSDK.NET_DVR_Logout(userId);
        }

        hcNetSDK.NET_DVR_Cleanup();

        if (resources != null) {
            OutputStream stdin = resources.ffmpegStdinRef.get();
            if (stdin != null) {
                try {
                    stdin.close();
                    log.info("已关闭 FFmpeg stdin");
                } catch (IOException e) {
                    log.warn("关闭 FFmpeg stdin 异常: {}", e.getMessage());
                }
            }
        }
        Process toStop = (resources != null && resources.ffmpegProcess.get() != null)
                ? resources.ffmpegProcess.get()
                : ffmpegProcess;
        if (toStop != null && toStop.isAlive()) {
            log.info("停止 FFmpeg 进程, pid={}", toStop.pid());
            toStop.destroy();
            try {
                if (!toStop.waitFor(2, TimeUnit.SECONDS)) {
                    toStop.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        threadResources.remove();
        currentResources.set(null);

        log.info("资源清理完成");
    }

    @Override
    public void stopPlayback() {
        PlaybackResources r = threadResources.get();
        if (r == null) r = currentResources.get();
        if (r == null) {
            log.info("停止回放：无进行中的回放");
            return;
        }
        log.info("停止回放: playHandle={}", r.playHandle);
        r.markStopped();
        r.stopLatch.countDown();

        if (r.playHandle != null && r.playHandle.intValue() > 0) {
            hcNetSDK.NET_DVR_StopPlayBack(r.playHandle);
        }
        if (r.userId != null && r.userId.intValue() > 0) {
            hcNetSDK.NET_DVR_Logout(r.userId);
        }
        hcNetSDK.NET_DVR_Cleanup();

        OutputStream stdin = r.ffmpegStdinRef.get();
        if (stdin != null) {
            try {
                stdin.close();
            } catch (IOException e) {
                /* ignore */
            }
        }
        Process p = r.ffmpegProcess.get();
        if (p != null && p.isAlive()) {
            p.destroy();
            try {
                if (!p.waitFor(2, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        threadResources.remove();
        currentResources.set(null);
        log.info("停止回放完成");
    }
}