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
 * NVR 数据通过回调直接写入 FFmpeg stdin，实现真正的边录边转流式输出
 * 无临时文件，无 EOF 问题
 */
@Slf4j
@Service
public class HikPlayBackServiceImpl implements HikPlayBackService {

    private static final String NVR_IP = "192.168.218.250";
    private static final int NVR_PORT = 8000;
    private static final String NVR_USERNAME = "admin";
    private static final String NVR_PASSWORD = "whds@2025";

    private static final String FFMPEG_PATH = "D:\\tools\\ffmpeg-master-latest-win64-gpl\\bin\\ffmpeg.exe";

    @Autowired
    private HCNetSDK hcNetSDK;

    private volatile AtomicReference<PlaybackResources> currentResources = new AtomicReference<>();
    private final ThreadLocal<PlaybackResources> threadResources = new ThreadLocal<>();

    private static class PlaybackResources {
        final NativeLong playHandle;
        final NativeLong userId;
        final CountDownLatch stopLatch;
        final AtomicReference<Process> ffmpegProcess = new AtomicReference<>();
        final AtomicReference<OutputStream> ffmpegStdinRef = new AtomicReference<>();
        final AtomicLong bytesWritten = new AtomicLong(0);
        final AtomicLong lastDataTime = new AtomicLong(System.currentTimeMillis());
        final AtomicLong stopped = new AtomicLong(0);  // 0=运行中, 1=已停止

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
            return;
        }
        log.info("登录成功，userId={}", userId);

        NativeLong playHandle = new NativeLong(-1);
        Process ffmpegProcess = null;
        PlaybackResources resources = null;

        try {
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

            log.info("开始回放，通道={}, dwChannel={}", channel, vodPara.struIDInfo.dwChannel);

            playHandle = hcNetSDK.NET_DVR_PlayBackByTime_V40(userId, vodPara);
            if (playHandle.intValue() == -1) {
                log.error("按时间回放失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
                hcNetSDK.NET_DVR_Logout(userId);
                hcNetSDK.NET_DVR_Cleanup();
                return;
            }
            log.info("回放句柄: {}", playHandle);

            resources = new PlaybackResources(playHandle, userId, stopLatch);
            currentResources.set(resources);
            threadResources.set(resources);

            IntByReference intInlen1 = new IntByReference(0);
            boolean bCtrl = hcNetSDK.NET_DVR_PlayBackControl_V40(playHandle, HCNetSDK.NET_DVR_PLAYSTART, Pointer.NULL, 0, Pointer.NULL, intInlen1);
            if (!bCtrl) {
                log.error("NET_DVR_PLAYSTART失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
                hcNetSDK.NET_DVR_StopPlayBack(playHandle);
                hcNetSDK.NET_DVR_Logout(userId);
                hcNetSDK.NET_DVR_Cleanup();
                return;
            }
            log.info("开启取流成功");

            // 启动 FFmpeg，stdin 接收 NVR 数据，实现真正的边录边转
            ffmpegProcess = startFfmpegWithPipe(outputStream, resources);
            log.info("FFmpeg 流式转码进程已启动, pid={}", ffmpegProcess.pid());
            resources.ffmpegProcess.set(ffmpegProcess);

            // NVR 返回 H.265 ES，回调线程直接写入 FFmpeg stdin
            final OutputStream ffmpegStdin = ffmpegProcess.getOutputStream();
            resources.ffmpegStdinRef.set(ffmpegStdin);
            final AtomicLong bytesWritten = resources.bytesWritten;
            final AtomicLong lastDataTime = resources.lastDataTime;
            AtomicLong lastLogTime = new AtomicLong(System.currentTimeMillis());

            HCNetSDK.FPlayDataCallBack callback = new HCNetSDK.FPlayDataCallBack() {
                public void invoke(int lPlayHandle, int dwDataType, Pointer pBuffer, int dwBufSize, int dwUser) {
                    if (dwBufSize <= 0 || pBuffer == null) return;
                    try {
                        ByteBuffer buf = pBuffer.getByteBuffer(0, dwBufSize);
                        byte[] data = new byte[dwBufSize];
                        buf.get(data);
                        ffmpegStdin.write(data);
                        ffmpegStdin.flush();
                        lastDataTime.set(System.currentTimeMillis());
                        long total = bytesWritten.addAndGet(dwBufSize);
                        long now = System.currentTimeMillis();
                        long last = lastLogTime.getAndSet(now);
                        if (now - last > 5000) {
                            log.info("数据采集中... dwDataType={}, 此次={}, 累计={}", dwDataType, dwBufSize, total);
                        }
                    } catch (IOException e) {
                        log.warn("FFmpeg stdin 写入异常: {}", e.getMessage());
                    }
                }
            };
            hcNetSDK.NET_DVR_SetPlayDataCallBack(playHandle, callback, Pointer.NULL);

            // 主线程等待停止信号，同时 NVR 回调持续写入数据，FFmpeg 实时转码输出
            log.info("等待回放停止信号...");
            // 超时时间：录像时长的 1.5 倍，最少 3 分钟，最多 10 分钟
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
            log.info("Finally块开始执行，清理资源...");
            PlaybackResources r = threadResources.get();
            if (r == null) r = currentResources.get();
            if (r != null && r.playHandle != null && r.playHandle.intValue() > 0) {
                hcNetSDK.NET_DVR_StopPlayBack(r.playHandle);
            }
            if (userId != null && userId.intValue() > 0) {
                hcNetSDK.NET_DVR_Logout(userId);
            }

            threadResources.remove();
            currentResources.set(null);

            // 关闭 FFmpeg stdin 并停止进程
            if (r != null) {
                OutputStream stdin = r.ffmpegStdinRef.get();
                if (stdin != null) {
                    try {
                        stdin.close();
                        log.info("已关闭 FFmpeg stdin");
                    } catch (IOException e) {
                        log.warn("关闭 FFmpeg stdin 异常: {}", e.getMessage());
                    }
                }
                Process p = r.ffmpegProcess.get();
                if (p != null && p.isAlive()) {
                    log.info("停止 FFmpeg 进程, pid={}", p.pid());
                    p.destroy();
                    try {
                        if (!p.waitFor(2, TimeUnit.SECONDS)) {
                            p.destroyForcibly();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // 停止外层 ffmpegProcess 引用（当 r 为 null 时）
            if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                log.info("停止 FFmpeg 进程, pid={}", ffmpegProcess.pid());
                ffmpegProcess.destroy();
                try {
                    if (!ffmpegProcess.waitFor(2, TimeUnit.SECONDS)) {
                        ffmpegProcess.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.info("流式回放结束");
    }

    /**
     * 启动 FFmpeg 进程，从 stdin 读取 H.265 裸流，实时转码为 H.264 输出到 outputStream
     * NVR 数据回调写入 FFmpeg stdin，实现真正的边录边转，无临时文件
     */
    private Process startFfmpegWithPipe(OutputStream outputStream, PlaybackResources resources) throws IOException {
        // FFmpeg 命令：从 stdin 读取 H.265 ES 裸流，转码为 H.264，输出到 stdout
        ProcessBuilder pb = new ProcessBuilder(
                FFMPEG_PATH,
                "-i", "pipe:0",
                "-c:v", "libx264",
                "-preset", "fast",
                "-crf", "23",
                "-vf", "scale='min(1920,iw)':-2",  // 智能缩放
                "-an",
                "-movflags", "frag_keyframe+empty_moov",
                "-f", "mp4",
                "pipe:1"
        );
        pb.redirectErrorStream(false);
        Process process = pb.start();

        // 读取 FFmpeg 的 stderr（错误输出包含转码进度信息）
        Thread errorThread = new Thread(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    log.info("FFmpeg stderr: {}", line);
                }
            } catch (IOException e) {
                // ignore
            }
        });
        errorThread.setDaemon(true);
        errorThread.start();

        // 持续将 FFmpeg 输出写入 HTTP 响应
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
                    if (now - lastLogTime > 2000) {
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

        // 检查 FFmpeg 是否立即退出（说明转码失败）
        Thread monitorThread = new Thread(() -> {
            try {
                boolean exited = process.waitFor(3, TimeUnit.SECONDS);
                if (exited && process.exitValue() != 0) {
                    log.error("FFmpeg 进程异常退出，exitCode={}", process.exitValue());
                } else if (exited) {
                    log.info("FFmpeg 进程正常退出");
                }
            } catch (InterruptedException e) {
                // ignore
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();

        return process;
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

        // 关闭 FFmpeg stdin 并停止进程
        OutputStream stdin = r.ffmpegStdinRef.get();
        if (stdin != null) {
            try { stdin.close(); } catch (IOException e) { /* ignore */ }
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