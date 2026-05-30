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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 海康威视 NVR 回放服务
 * NVR 数据先写入临时文件，FFmpeg 实时读取并转码为 H.264，流式输出到前端
 */
@Slf4j
@Service
public class HikPlayBackServiceImpl implements HikPlayBackService {

    private static final String NVR_IP = "192.168.218.250";
    private static final int NVR_PORT = 8000;
    private static final String NVR_USERNAME = "admin";
    private static final String NVR_PASSWORD = "whds@2025";

    private static final String FFMPEG_PATH = "D:\\tools\\ffmpeg-master-latest-win64-gpl\\bin\\ffmpeg.exe";
    private static final String TEMP_DIR = "D:\\hik_temp";

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private HCNetSDK hcNetSDK;

    private volatile AtomicReference<PlaybackResources> currentResources = new AtomicReference<>();
    private final ThreadLocal<PlaybackResources> threadResources = new ThreadLocal<>();

    private static class PlaybackResources {
        final NativeLong playHandle;
        final NativeLong userId;
        final CountDownLatch stopLatch;
        final Path rawFile;
        final AtomicReference<Process> ffmpegProcess = new AtomicReference<>();
        final CountDownLatch dataReceivedLatch = new CountDownLatch(1);
        final AtomicLong bytesWritten = new AtomicLong(0);
        final AtomicLong lastDataTime = new AtomicLong(System.currentTimeMillis());

        PlaybackResources(NativeLong playHandle, NativeLong userId, CountDownLatch stopLatch, Path rawFile) {
            this.playHandle = playHandle;
            this.userId = userId;
            this.stopLatch = stopLatch;
            this.rawFile = rawFile;
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
        deviceInfo.write();
        NativeLong userId = hcNetSDK.NET_DVR_Login_V30(NVR_IP, (short) NVR_PORT, NVR_USERNAME, NVR_PASSWORD, deviceInfo);
        if (userId.intValue() == -1) {
            log.error("登录失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
            return;
        }
        deviceInfo.read();
        log.info("登录成功，userId={}", userId);

        Path rawFile = null;
        NativeLong playHandle = new NativeLong(-1);
        FileOutputStream fos = null;
        AtomicReference<PlaybackResources> resourcesRef = new AtomicReference<>();

        try {
            Files.createDirectories(Path.of(TEMP_DIR));
            String prefix = startTime.format(FILE_DATE_FORMAT) + "_ch" + channel;
            rawFile = Path.of(TEMP_DIR, prefix + "_raw.dat");

            fos = new FileOutputStream(rawFile.toFile());

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
                return;
            }
            log.info("回放句柄: {}", playHandle);

            PlaybackResources resources = new PlaybackResources(playHandle, userId, stopLatch, rawFile);
            resourcesRef.set(resources);
            currentResources.set(resources);
            threadResources.set(resources);

            IntByReference intInlen1 = new IntByReference(0);
            boolean bCtrl = hcNetSDK.NET_DVR_PlayBackControl_V40(playHandle, HCNetSDK.NET_DVR_PLAYSTART, Pointer.NULL, 0, Pointer.NULL, intInlen1);
            if (!bCtrl) {
                log.error("NET_DVR_PLAYSTART失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
                return;
            }
            log.info("开启取流成功");

            // NVR 返回 H.265 ES（HEVC 视频 + G711 音频），回调线程直接写入文件
            final FileOutputStream fosForCallback = fos;
            final AtomicLong bytesWritten = resources.bytesWritten;
            final AtomicLong lastDataTime = resources.lastDataTime;
            final CountDownLatch dataReceivedLatch = resources.dataReceivedLatch;
            AtomicLong lastLogTime = new AtomicLong(System.currentTimeMillis());

            HCNetSDK.FPlayDataCallBack callback = new HCNetSDK.FPlayDataCallBack() {
                public void invoke(int lPlayHandle, int dwDataType, Pointer pBuffer, int dwBufSize, int dwUser) {
                    if (dwBufSize <= 0 || pBuffer == null) return;
                    try {
                        ByteBuffer buf = pBuffer.getByteBuffer(0, dwBufSize);
                        byte[] data = new byte[dwBufSize];
                        buf.get(data);
                        fosForCallback.write(data);
                        fosForCallback.flush();
                        lastDataTime.set(System.currentTimeMillis());
                        if (dataReceivedLatch.getCount() > 0) {
                            dataReceivedLatch.countDown();
                        }
                        long total = bytesWritten.addAndGet(dwBufSize);
                        long now = System.currentTimeMillis();
                        if (now - lastLogTime.get() > 5000) {
                            lastLogTime.set(now);
                            log.info("数据采集中... dwDataType={}, 此次={}, 累计={}", dwDataType, dwBufSize, total);
                        }
                    } catch (IOException e) {
                        log.warn("文件写入异常: {}", e.getMessage());
                    }
                }
            };
            hcNetSDK.NET_DVR_SetPlayDataCallBack(playHandle, callback, Pointer.NULL);

            // 等待 NVR 数据积累足够后再启动 FFmpeg（减少开头碎片NALU导致的雪花）
            log.info("等待 NVR 数据写入文件...");
            boolean dataReceived = dataReceivedLatch.await(30, TimeUnit.SECONDS);
            if (!dataReceived) {
                log.error("NVR 数据未写入，超时未收到数据");
            }

            // 等待数据稳定（文件大小连续2秒不变，且至少有500KB）
            long stableStart = System.currentTimeMillis();
            long prevSize = 0;
            while (System.currentTimeMillis() - stableStart < 30_000) {
                long currentSize = bytesWritten.get();
                long quietTime = System.currentTimeMillis() - lastDataTime.get();
                if (currentSize > 500 * 1024 && currentSize == prevSize && quietTime > 2000) {
                    log.info("数据已稳定，静止 {}ms，累计: {} bytes", quietTime, currentSize);
                    break;
                }
                prevSize = currentSize;
                Thread.sleep(200);
            }
            long accumulated = bytesWritten.get();
            log.info("NVR 数据已积累，累计: {} bytes", accumulated);

            // NVR 数据开始写入后，启动 FFmpeg 进程
            Process ffmpegProcess = startFfmpegStreaming(rawFile, outputStream);
            log.info("FFmpeg 流式转码进程已启动, pid={}", ffmpegProcess.pid());
            resources.ffmpegProcess.set(ffmpegProcess);

            log.info("等待回放停止信号...");
            boolean signalled = stopLatch.await(5, TimeUnit.MINUTES);
            if (signalled) {
                log.info("收到停止信号，停止数据采集");
            } else {
                log.info("等待超时（5分钟），自动停止数据采集");
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
            if (r != null) {
                if (r.playHandle != null && r.playHandle.intValue() > 0) hcNetSDK.NET_DVR_StopPlayBack(r.playHandle);
            }
            if (userId != null && userId.intValue() > 0) hcNetSDK.NET_DVR_Logout(userId);

            threadResources.remove();
            currentResources.set(null);

            // 关闭写入流
            if (fos != null) {
                try { fos.flush(); fos.close(); } catch (IOException e) {/* ignore */}
            }

            // 停止 FFmpeg 进程
            if (r != null) {
                Process p = r.ffmpegProcess.get();
                if (p != null && p.isAlive()) {
                    log.info("停止 FFmpeg 进程, pid={}", p.pid());
                    p.destroy();
                }
            }

            // 清理临时文件
            cleanupTempFiles(rawFile);
        }

        log.info("流式回放结束");
    }

    /**
     * 启动 FFmpeg 进程，持续读取临时文件并转码输出到 outputStream
     * 调用此方法时 NVR 数据已写入文件，不会出现空文件问题
     */
    private Process startFfmpegStreaming(Path rawFile, OutputStream outputStream) throws IOException, InterruptedException {
        long fileSize = Files.exists(rawFile) ? Files.size(rawFile) : 0;
        log.info("启动 FFmpeg，临时文件大小: {} bytes", fileSize);

        // FFmpeg 命令：按原始速率读取文件，转码为 H.264，输出到 stdout
        ProcessBuilder pb = new ProcessBuilder(
                FFMPEG_PATH,
                "-re",                           // 按原始速率读取
                "-fflags", "+genpts+igndts",     // 生成pts，处理不连续的dts
                "-f", "hevc",                    // 指定输入格式为 HEVC 裸流
                "-i", rawFile.toAbsolutePath().toString(),
                "-probesize", "5000000",
                "-analyzeduration", "5000000",
                "-c:v", "libx264",              // H.265 -> H.264 转码
                "-preset", "ultrafast",
                "-crf", "18",                   // 低 CRF 值提高质量
                "-vf", "scale=1920:-2",         // 缩放到 1080p，缓解源流 3200x1800@1200kbps 压缩伪影
                "-an",
                "-movflags", "empty_moov",
                "-f", "mp4",
                "pipe:1"
        );
        pb.redirectErrorStream(false); // 不合并错误流，单独处理
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
        Thread outputThread = new Thread(() -> {
            byte[] buffer = new byte[8192];
            try {
                long totalBytes = 0;
                long lastLogTime = System.currentTimeMillis();
                int bytesRead;
                while ((bytesRead = process.getInputStream().read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
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

    private void cleanupTempFiles(Path... paths) {
        for (Path p : paths) {
            if (p != null && Files.exists(p)) {
                try {
                    Files.deleteIfExists(p);
                    log.info("已删除临时文件: {}", p);
                } catch (IOException e) {
                    log.warn("删除临时文件失败: {}: {}", p, e.getMessage());
                }
            }
        }
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
        r.stopLatch.countDown();
        if (r.playHandle != null && r.playHandle.intValue() > 0) {
            hcNetSDK.NET_DVR_StopPlayBack(r.playHandle);
        }
        if (r.userId != null && r.userId.intValue() > 0) {
            hcNetSDK.NET_DVR_Logout(r.userId);
        }

        // 停止 FFmpeg 进程
        Process p = r.ffmpegProcess.get();
        if (p != null && p.isAlive()) {
            p.destroy();
        }

        threadResources.remove();
        currentResources.set(null);
        log.info("停止回放完成");
    }
}