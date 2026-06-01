package com.lvtong.LvTongTransportDept.service.impl;

import com.lvtong.LvTongTransportDept.hksdk.HCNetSDK;
import com.lvtong.LvTongTransportDept.service.HikPlayBackService;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 海康威视 NVR 回放服务 - 重构版
 *
 * 关键改进：
 * 1. BufferedOutputStream 包装 FFmpeg stdin，消除高频 flush
 * 2. 移除 stderr 就绪检测，改用 process.isAlive() + sleep
 * 3. ThreadLocal byte[] buffer 复用，避免频繁分配
 * 4. 移除 tempKey 机制，修正启动顺序避免首包丢失
 * 5. 支持 GPU 硬件转码（可配置）
 * 6. FFmpeg 参数低延迟优化
 * 7. 配置化 NVR 凭据（@ConfigurationProperties）
 * 8. FFmpeg 生命周期监控（watchdog）
 * 9. 共享线程池（ExecutorService + ScheduledExecutorService）
 * 10. Micrometer 指标采集
 */
@Slf4j
@Service
public class HikPlayBackServiceImpl implements HikPlayBackService {

    // ======================== NVR 配置 ========================
    @ConfigurationProperties(prefix = "hik.nvr")
    public static class HikNvrConfig {
        private String ip = "192.168.218.250";
        private int port = 8000;
        private String username = "admin";
        private String password = "whds@2025";
        private int channelBase = 33;

        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public int getChannelBase() { return channelBase; }
        public void setChannelBase(int channelBase) { this.channelBase = channelBase; }
    }

    // ======================== FFmpeg 配置 ========================
    @ConfigurationProperties(prefix = "hik.ffmpeg")
    public static class HikFfmpegConfig {
        private String path = "D:\\tools\\ffmpeg-master-latest-win64-gpl\\bin\\ffmpeg.exe";
        private boolean hwaccel = false;
        private String hwType = "nvidia"; // nvidia / amd / intel
        private int startTimeoutSec = 10;
        private int dataTimeoutSec = 30;
        private boolean transcodeToH264 = true; // true=H.265重编码为H.264 MP4，false=仅封装为H.265 MP4(-c copy)

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public boolean isHwaccel() { return hwaccel; }
        public void setHwaccel(boolean hwaccel) { this.hwaccel = hwaccel; }
        public String getHwType() { return hwType; }
        public void setHwType(String hwType) { this.hwType = hwType; }
        public int getStartTimeoutSec() { return startTimeoutSec; }
        public void setStartTimeoutSec(int startTimeoutSec) { this.startTimeoutSec = startTimeoutSec; }
        public int getDataTimeoutSec() { return dataTimeoutSec; }
        public void setDataTimeoutSec(int dataTimeoutSec) { this.dataTimeoutSec = dataTimeoutSec; }
        public boolean isTranscodeToH264() { return transcodeToH264; }
        public void setTranscodeToH264(boolean transcodeToH264) { this.transcodeToH264 = transcodeToH264; }
    }

    // ======================== 指标配置 ========================
    private final AtomicLong activePlaybackCount = new AtomicLong(0);
    private final AtomicLong totalBytesWritten = new AtomicLong(0);
    private final AtomicLong ffmpegRestartCount = new AtomicLong(0);
    private final Timer callbackLatencyTimer;

    // ======================== 共享线程池 ========================
    private final ExecutorService sharedExecutor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService sharedScheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
    );

    // ======================== ThreadLocal buffer 复用 ========================
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB
    private final ConcurrentHashMap<Integer, ThreadLocal<byte[]>> channelBuffers = new ConcurrentHashMap<>();

    private ThreadLocal<byte[]> getBufferForChannel(int channel) {
        return channelBuffers.computeIfAbsent(channel, ch -> new ThreadLocal<byte[]>() {
            @Override
            protected byte[] initialValue() {
                return new byte[BUFFER_SIZE];
            }
        });
    }

    // ======================== SDK 连接单例 ========================
    @Autowired
    private HCNetSDK hcNetSDK;

    @Autowired
    private HikNvrConfig nvrConfig;

    @Autowired
    private HikFfmpegConfig ffmpegConfig;

    // 存储所有通道的资源，key 是 playHandle
    private final ConcurrentHashMap<Integer, PlaybackResources> resourcesMap = new ConcurrentHashMap<>();

    private volatile NativeLong sharedUserId = new NativeLong(-1);
    private volatile boolean sdkInitialized = false;
    private final Object sdkInitLock = new Object();

    // ======================== 资源内部类 ========================
    private static class PlaybackResources {
        final CountDownLatch stopLatch;
        final AtomicReference<Process> ffmpegProcess = new AtomicReference<>();
        final AtomicReference<BufferedOutputStream> ffmpegStdinRef = new AtomicReference<>();
        final AtomicLong bytesWritten = new AtomicLong(0);
        final AtomicLong lastDataTime = new AtomicLong(0);
        final AtomicLong stopped = new AtomicLong(0);
        volatile Thread dataTimeoutThread;
        final AtomicLong stdinWriteFailCount = new AtomicLong(0);

        PlaybackResources(CountDownLatch stopLatch) {
            this.stopLatch = stopLatch;
        }

        boolean isStopped() {
            return stopped.get() != 0;
        }

        void markStopped() {
            stopped.set(1);
        }
    }

    public HikPlayBackServiceImpl(MeterRegistry meterRegistry) {
        // 注册 Micrometer 指标
        Gauge.builder("hik.playback.active.count", activePlaybackCount, AtomicLong::get)
                .description("当前活跃播放通道数")
                .register(meterRegistry);

        Gauge.builder("hik.playback.total.bytes", totalBytesWritten, AtomicLong::get)
                .description("总写入字节数")
                .register(meterRegistry);

        Gauge.builder("hik.playback.ffmpeg.restart.count", ffmpegRestartCount, AtomicLong::get)
                .description("FFmpeg 重启次数")
                .register(meterRegistry);

        callbackLatencyTimer = Timer.builder("hik.playback.callback.latency")
                .description("回调延迟")
                .register(meterRegistry);
    }

    // ======================== SDK 初始化 ========================
    private boolean ensureInitialized() {
        if (sdkInitialized && sharedUserId.intValue() > 0) {
            log.info("SDK 已在之前初始化且已登录，userId={}", sharedUserId);
            return true;
        }
        synchronized (sdkInitLock) {
            if (sdkInitialized && sharedUserId.intValue() > 0) {
                log.info("SDK 已在之前初始化且已登录，userId={}", sharedUserId);
                return true;
            }
            log.info("SDK 正在初始化...");
            if (!hcNetSDK.NET_DVR_Init()) {
                log.error("SDK初始化失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
                return false;
            }
            hcNetSDK.NET_DVR_SetConnectTime(2000, 1);
            hcNetSDK.NET_DVR_SetReconnect(100000, true);

            log.info("正在登录 NVR: ip={}, port={}, username={}", nvrConfig.getIp(), nvrConfig.getPort(), nvrConfig.getUsername());
            HCNetSDK.NET_DVR_DEVICEINFO_V30 deviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
            sharedUserId = hcNetSDK.NET_DVR_Login_V30(
                    nvrConfig.getIp(),
                    (short) nvrConfig.getPort(),
                    nvrConfig.getUsername(),
                    nvrConfig.getPassword(),
                    deviceInfo
            );
            if (sharedUserId.intValue() == -1) {
                log.error("登录失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
                hcNetSDK.NET_DVR_Cleanup();
                return false;
            }
            log.info("SDK初始化并登录成功，userId={}", sharedUserId);
            sdkInitialized = true;
            return true;
        }
    }

    // ======================== 启动顺序：FFmpeg → 海康回放 → 注册回调 ========================
    @Override
    public void playBackByTime(LocalDateTime startTime, LocalDateTime endTime, int channel, OutputStream outputStream) {
        log.info("流式回放开始: startTime={}, endTime={}, channel={}", startTime, endTime, channel);

        if (!ensureInitialized()) {
            log.error("SDK未初始化，无法进行回放");
            return;
        }

        CountDownLatch stopLatch = new CountDownLatch(1);
        NativeLong playHandle = new NativeLong(-1);
        Process ffmpegProcess = null;
        PlaybackResources resources = null;

        try {
            // 1. 启动 FFmpeg
            log.info("启动 FFmpeg 进程（等待数据写入）...");
            ffmpegProcess = startFfmpegWithPipe(outputStream, stopLatch);

            // 3. 等待 FFmpeg 就绪：sleep 500ms 后检查 isAlive()
            if (!waitForFfmpegReady(ffmpegProcess, ffmpegConfig.getStartTimeoutSec())) {
                log.error("FFmpeg 启动超时（{}秒），强制终止", ffmpegConfig.getStartTimeoutSec());
                ffmpegProcess.destroyForcibly();
                return;
            }
            log.info("FFmpeg 已就绪，可以接受数据输入");

            // 2. 使用 BufferedOutputStream 包装 stdin（1MB buffer）
            BufferedOutputStream bufferedStdin = new BufferedOutputStream(
                    ffmpegProcess.getOutputStream(), BUFFER_SIZE
            );

            // 4. 创建海康回放
            HCNetSDK.NET_DVR_VOD_PARA vodPara = new HCNetSDK.NET_DVR_VOD_PARA();
            vodPara.dwSize = vodPara.size();
            vodPara.struIDInfo.dwSize = vodPara.struIDInfo.size();
            vodPara.struIDInfo.dwChannel = nvrConfig.getChannelBase() + (channel - 1);

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

            playHandle = hcNetSDK.NET_DVR_PlayBackByTime_V40(sharedUserId, vodPara);

            if (playHandle.intValue() == -1) {
                log.error("按时间回放失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
                forceStopFfmpeg(ffmpegProcess);
                return;
            }
            log.info("回放句柄: {}", playHandle);

            // 5. 构建 PlaybackResources，直接用真实 playHandle
            resources = new PlaybackResources(stopLatch);
            resources.ffmpegProcess.set(ffmpegProcess);
            resources.ffmpegStdinRef.set(bufferedStdin);
            resourcesMap.put(playHandle.intValue(), resources);

            // 6. 启动超时监控（使用共享线程池）
            startDataTimeoutMonitor(playHandle.intValue(), resources);

            // 7. 注册数据回调
            final NativeLong finalPlayHandle = playHandle;
            final int channelId = channel;
            HCNetSDK.FPlayDataCallBack callback = new HCNetSDK.FPlayDataCallBack() {
                public void invoke(int lPlayHandle, int dwDataType, Pointer pBuffer, int dwBufSize, int dwUser) {
                    if (dwBufSize <= 0 || pBuffer == null) return;
                    PlaybackResources res = resourcesMap.get(lPlayHandle);
                    if (res == null || res.isStopped()) return;

                    long startNs = System.nanoTime();
                    try {
                        // 使用 ThreadLocal buffer 复用，避免每次 new byte[]
                        ThreadLocal<byte[]> bufferHolder = getBufferForChannel(channelId);
                        byte[] buf = bufferHolder.get();
                        if (dwBufSize > buf.length) {
                            buf = new byte[dwBufSize];
                            bufferHolder.set(buf);
                        }

                        ByteBuffer bb = pBuffer.getByteBuffer(0, dwBufSize);
                        bb.get(buf, 0, dwBufSize);

                        BufferedOutputStream stdin = res.ffmpegStdinRef.get();
                        if (stdin != null) {
                            synchronized (stdin) {
                                stdin.write(buf, 0, dwBufSize);
                                // 不逐包 flush，由 BufferedOutputStream 自己控制
                            }
                        }
                        res.lastDataTime.set(System.currentTimeMillis());
                        res.bytesWritten.addAndGet(dwBufSize);
                        totalBytesWritten.addAndGet(dwBufSize);
                    } catch (IOException e) {
                        log.warn("FFmpeg stdin 写入异常: {}", e.getMessage());
                        res.stdinWriteFailCount.incrementAndGet();
                        if (res.stdinWriteFailCount.get() > 10) {
                            res.markStopped();
                        }
                    } finally {
                        callbackLatencyTimer.record(System.nanoTime() - startNs, TimeUnit.NANOSECONDS);
                    }
                }
            };
            hcNetSDK.NET_DVR_SetPlayDataCallBack(finalPlayHandle, callback, Pointer.NULL);
            log.info("数据回调已设置，开始从第一帧采集数据");

            // 8. 发送 PLAYSTART
            IntByReference intInlen = new IntByReference(0);
            boolean bCtrl = hcNetSDK.NET_DVR_PlayBackControl_V40(
                    finalPlayHandle, HCNetSDK.NET_DVR_PLAYSTART, Pointer.NULL, 0, Pointer.NULL, intInlen
            );
            if (!bCtrl) {
                log.error("NET_DVR_PLAYSTART失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
                hcNetSDK.NET_DVR_StopPlayBack(finalPlayHandle);
                forceStopFfmpeg(ffmpegProcess);
                resourcesMap.remove(finalPlayHandle.intValue());
                return;
            }
            log.info("PLAYSTART 指令发送成功");

            activePlaybackCount.incrementAndGet();

            // 9. 等待停止信号
            log.info("等待回放停止信号...");
            long durationSeconds = Duration.between(startTime, endTime).getSeconds();
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
            activePlaybackCount.decrementAndGet();
            cleanupResources(resources, playHandle);
        }

        log.info("流式回放结束");
    }

    // ======================== FFmpeg 进程启动 ========================
    private Process startFfmpegWithPipe(OutputStream outputStream, CountDownLatch stopLatch) throws IOException {
        String ffmpegPath = ffmpegConfig.getPath();
        log.info("FFmpeg 路径: {}", ffmpegPath);
        java.nio.file.Path p = java.nio.file.Paths.get(ffmpegPath);
        log.info("FFmpeg 文件是否存在: {}", java.nio.file.Files.exists(p));

        List<String> ffmpegArgs = new ArrayList<>();
        ffmpegArgs.add(ffmpegConfig.getPath());
        ffmpegArgs.add("-i");
        ffmpegArgs.add("pipe:0");

        // 低延迟参数（CPU 模式）
        ffmpegArgs.add("-fflags");
        ffmpegArgs.add("nobuffer");
        ffmpegArgs.add("-flags");
        ffmpegArgs.add("low_delay");
        ffmpegArgs.add("-preset");
        ffmpegArgs.add("ultrafast");
        ffmpegArgs.add("-tune");
        ffmpegArgs.add("zerolatency");
        ffmpegArgs.add("-crf");
        ffmpegArgs.add("28");
        ffmpegArgs.add("-an");

        // GPU 模式 vs CPU 模式
        if (ffmpegConfig.isHwaccel()) {
            String hwType = ffmpegConfig.getHwType().toLowerCase();
            if ("nvidia".equals(hwType)) {
                ffmpegArgs.add("-hwaccel");
                ffmpegArgs.add("cuda");
                ffmpegArgs.add("-c:v");
                ffmpegArgs.add("hevc_cuvid");
                ffmpegArgs.add("-c:v");
                ffmpegArgs.add("h264_nvenc");
                ffmpegArgs.add("-preset");
                ffmpegArgs.add("p1");
                ffmpegArgs.add("-tune");
                ffmpegArgs.add("ll");
                ffmpegArgs.add("-rc");
                ffmpegArgs.add("constqp");
            } else if ("amd".equals(hwType)) {
                ffmpegArgs.add("-hwaccel");
                ffmpegArgs.add("dxva2");
                ffmpegArgs.add("-c:v");
                ffmpegArgs.add("h264_amf");
            } else {
                ffmpegArgs.add("-c:v");
                ffmpegArgs.add("libx264");
            }
        } else {
            ffmpegArgs.add("-c:v");
            ffmpegArgs.add("libx264");
        }

        ffmpegArgs.add("-f");
        ffmpegArgs.add("flv");
        ffmpegArgs.add("pipe:1");

        String[] args = ffmpegArgs.toArray(new String[0]);

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        final Process ffmpegProc = process;

        // stderr 异步消费（防止 pipe 满导致阻塞）
        sharedExecutor.submit(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(ffmpegProc.getErrorStream()));
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    log.debug("FFmpeg stderr: {}", line);
                }
            } catch (IOException e) {
                log.debug("FFmpeg stderr 读取结束");
            }
        });

        // FFmpeg 输出线程：从 pipe 读数据，放入有界队列（队列满则阻塞，形成背压）
        final int QUEUE_CAPACITY = 50;
        final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        final AtomicLong stoppedFlag = new AtomicLong(0);
        final AtomicLong firstOutputTime = new AtomicLong(0);

        // sender 线程：从队列取数据写到 HTTP response，send buffer 满时自然阻塞
        Thread senderThread = new Thread(() -> {
            try {
                long totalBytes = 0;
                long startTime = System.currentTimeMillis();
                long lastLogTime = startTime;
                while (true) {
                    byte[] buffer = queue.poll(5, TimeUnit.SECONDS);
                    if (buffer == null) {
                        // 超时说明队列空了但 FFmpeg 还在运行，等待数据
                        if (stoppedFlag.get() != 0) break;
                        continue;
                    }
                    if (stoppedFlag.get() != 0) break;
                    try {
                        outputStream.write(buffer);
                        totalBytes += buffer.length;
                        long now = System.currentTimeMillis();
                        if (now - lastLogTime > 5000) {
                            lastLogTime = now;
                            log.info("FFmpeg 输出中... 已输出 {} bytes, 已耗时 {}s",
                                    totalBytes, (now - startTime) / 1000);
                        }
                    } catch (IOException e) {
                        log.info("HTTP 连接已断开，停止输出");
                        stoppedFlag.set(1);
                        stopLatch.countDown();
                        break;
                    }
                }
                log.info("FFmpeg 输出完成，共 {} bytes", totalBytes);
            } catch (InterruptedException e) {
                log.debug("Sender 线程被中断");
            }
        });
        senderThread.setDaemon(true);
        senderThread.start();

        Thread outputThread = new Thread(() -> {
            byte[] buffer = new byte[8192];
            try {
                int bytesRead;
                while ((bytesRead = process.getInputStream().read(buffer)) != -1) {
                    if (stoppedFlag.get() != 0) break;
                    if (firstOutputTime.get() == 0) {
                        firstOutputTime.set(System.currentTimeMillis());
                        log.info("FFmpeg 开始输出数据");
                    }
                    // offer 阻塞时 FFmpeg 读 pipe 也跟着停，实现背压
                    byte[] chunk = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                    queue.put(chunk);
                }
                log.info("FFmpeg pipe 读取完成");
            } catch (InterruptedException e) {
                log.debug("FFmpeg 输出线程被中断");
            } catch (IOException e) {
                log.warn("FFmpeg 输出异常: {}", e.getMessage());
            }
        });
        outputThread.setDaemon(true);
        outputThread.start();

        // FFmpeg 生命周期 watchdog
        sharedExecutor.submit(() -> {
            try {
                boolean exited = process.waitFor(3, TimeUnit.SECONDS);
                if (exited && process.exitValue() != 0) {
                    log.error("FFmpeg 进程异常退出，exitCode={}", process.exitValue());
                    ffmpegRestartCount.incrementAndGet();
                    stoppedFlag.set(1);
                    stopLatch.countDown();
                } else if (exited) {
                    log.info("FFmpeg 进程正常退出");
                    stoppedFlag.set(1);
                    stopLatch.countDown();
                }
            } catch (InterruptedException e) {
                log.debug("FFmpeg 监控线程被中断");
            }
        });

        return process;
    }

    // ======================== 新的 FFmpeg 就绪检测 ========================
    private boolean waitForFfmpegReady(Process process, int timeoutSec) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        if (!process.isAlive()) {
            log.error("FFmpeg 进程启动期间退出，exitCode={}", process.exitValue());
            return false;
        }

        log.info("FFmpeg 进程存活，认为已就绪");
        return true;
    }

    // ======================== 数据超时监控（使用共享线程池） ========================
    private void startDataTimeoutMonitor(int playHandleInt, PlaybackResources resources) {
        Thread dataTimeoutThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                PlaybackResources r = resourcesMap.get(playHandleInt);
                if (r == null || r.isStopped()) break;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
                long lastData = r.lastDataTime.get();
                if (lastData > 0 && System.currentTimeMillis() - lastData > ffmpegConfig.getDataTimeoutSec() * 1000L) {
                    log.warn("FFmpeg 等待数据超时（{}秒），数据源无响应，强制停止", ffmpegConfig.getDataTimeoutSec());
                    r.markStopped();
                    r.stopLatch.countDown();
                    break;
                }
            }
        });
        dataTimeoutThread.setDaemon(true);
        dataTimeoutThread.start();
        resources.dataTimeoutThread = dataTimeoutThread;
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

    // ======================== 资源清理 ========================
    private void cleanupResources(PlaybackResources resources, NativeLong playHandle) {
        log.info("Finally块开始执行，清理资源...");

        if (playHandle != null && playHandle.intValue() > 0) {
            hcNetSDK.NET_DVR_StopPlayBack(playHandle);
            resourcesMap.remove(playHandle.intValue());
            log.info("已从 resourcesMap 移除 playHandle={}", playHandle.intValue());
        }

        if (resources != null) {
            if (resources.dataTimeoutThread != null) {
                resources.dataTimeoutThread.interrupt();
                log.info("已中断数据超时监控线程");
            }

            BufferedOutputStream stdin = resources.ffmpegStdinRef.getAndSet(null);
            if (stdin != null) {
                try {
                    stdin.close();
                    log.info("已关闭 FFmpeg stdin");
                } catch (IOException e) {
                    log.warn("关闭 FFmpeg stdin 异常: {}", e.getMessage());
                }
            }

            // FFmpeg watchdog 会自动处理 process 清理
            Process proc = resources.ffmpegProcess.get();
            if (proc != null && proc.isAlive()) {
                log.info("停止 FFmpeg 进程, pid={}", proc.pid());
                proc.destroy();
                try {
                    if (!proc.waitFor(2, TimeUnit.SECONDS)) {
                        proc.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.info("资源清理完成");
    }

    // ======================== 下载录像 ========================
    @Override
    public boolean downloadVideo(LocalDateTime startTime, LocalDateTime endTime, int channel, OutputStream outputStream) throws IOException {
        log.info("下载录像开始: startTime={}, endTime={}, channel={}", startTime, endTime, channel);

        if (!ensureInitialized()) {
            log.error("SDK未初始化，无法进行下载");
            return false;
        }

        // 通道有效性检查
        int dwChannel = nvrConfig.getChannelBase() + (channel - 1);
        int maxChannel = nvrConfig.getChannelBase() + 31;
        if (channel < 1 || dwChannel > maxChannel) {
            log.error("通道号无效: channel={} (有效范围 1-{}), dwChannel={}", channel, maxChannel - nvrConfig.getChannelBase() + 1, dwChannel);
            return false;
        }

        long durationSeconds = Duration.between(startTime, endTime).getSeconds();
        if (durationSeconds <= 0) {
            log.error("结束时间必须晚于开始时间");
            return false;
        }

        Path rawFile = null;
        Path mp4File = null;
        NativeLong fileHandle = new NativeLong(-1);

        try {
            // 1. 创建临时文件（SDK写入原始H.265，再转码为H.264 MP4）
            rawFile = Files.createTempFile("hik_raw_", ".265");
            mp4File = Files.createTempFile("hik_converted_", ".mp4");
            log.info("原始文件: {}, MP4文件: {}", rawFile, mp4File);

            // 2. 准备下载条件
            HCNetSDK.NET_DVR_PLAYCOND playCond = new HCNetSDK.NET_DVR_PLAYCOND();
            playCond.read();
            playCond.dwChannel = dwChannel;
            playCond.struStartTime.dwYear = startTime.getYear();
            playCond.struStartTime.dwMonth = startTime.getMonthValue();
            playCond.struStartTime.dwDay = startTime.getDayOfMonth();
            playCond.struStartTime.dwHour = startTime.getHour();
            playCond.struStartTime.dwMinute = startTime.getMinute();
            playCond.struStartTime.dwSecond = startTime.getSecond();
            playCond.struStopTime.dwYear = endTime.getYear();
            playCond.struStopTime.dwMonth = endTime.getMonthValue();
            playCond.struStopTime.dwDay = endTime.getDayOfMonth();
            playCond.struStopTime.dwHour = endTime.getHour();
            playCond.struStopTime.dwMinute = endTime.getMinute();
            playCond.struStopTime.dwSecond = endTime.getSecond();
            playCond.byDrawFrame = 0;  // 0:不抽帧
            playCond.byStreamType = 0; // 0-主码流
            playCond.write();

            log.info("启动海康下载，通道={}, dwChannel={}", channel, dwChannel);

            // 3. 启动下载（SDK写入原始H.265文件）
            fileHandle = hcNetSDK.NET_DVR_GetFileByTime_V40(sharedUserId, rawFile.toString(), playCond);
            if (fileHandle.intValue() == -1) {
                log.error("按时间下载失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
                return false;
            }
            log.info("下载句柄: {}", fileHandle);

            // 4. 发送 PLAYSTART 开始下载
            boolean bCtrl = hcNetSDK.NET_DVR_PlayBackControl(fileHandle, HCNetSDK.NET_DVR_PLAYSTART, 0, Pointer.NULL);
            if (!bCtrl) {
                log.error("NET_DVR_PLAYSTART失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
                hcNetSDK.NET_DVR_StopGetFile(fileHandle);
                return false;
            }
            log.info("PLAYSTART 指令发送成功，开始下载");

            // 5. 轮询下载进度，等待完成
            // 超时时间：录像时长 + 2分钟，最多30分钟
            long timeoutMs = Math.min(durationSeconds * 1000L + 120_000L, 1_800_000L);
            log.info("录像时长 {}s，超时 {}ms，开始轮询进度", durationSeconds, timeoutMs);
            long startMs = System.currentTimeMillis();
            long lastLogMs = startMs;

            while (System.currentTimeMillis() - startMs < timeoutMs) {
                // 获取下载进度
                int nPos = hcNetSDK.NET_DVR_GetDownloadPos(fileHandle);

                // 进度大于100表示异常终止
                if (nPos > 100) {
                    log.error("下载异常终止，进度={}，错误码: {}", nPos, hcNetSDK.NET_DVR_GetLastError());
                    hcNetSDK.NET_DVR_StopGetFile(fileHandle);
                    throw new IOException("下载异常终止，进度=" + nPos);
                }

                // 每5秒打印一次进度
                long now = System.currentTimeMillis();
                if (now - lastLogMs > 5000) {
                    long elapsed = (now - startMs) / 1000;
                    log.info("下载进度: {}%，已等待 {}s", nPos, elapsed);
                    lastLogMs = now;
                }

                // 进度达到100表示下载完成
                if (nPos == 100) {
                    log.info("下载完成，进度达到100%");
                    break;
                }

                Thread.sleep(500); // 每0.5秒检查一次
            }

            // 6. 停止下载
            int stopResult = hcNetSDK.NET_DVR_StopGetFile(fileHandle);
            if (stopResult == 0) {
                log.error("停止下载失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
            } else {
                log.info("已停止下载");
            }
            fileHandle = new NativeLong(-1); // 标记已关闭

            // 7. 检查原始文件是否存在且有内容
            if (!Files.exists(rawFile) || Files.size(rawFile) == 0) {
                log.error("下载文件不存在或为空: {}", rawFile);
                throw new IOException("下载文件不存在或为空");
            }

            // 8. FFmpeg 转码：H.265 -> H.264
            log.info("启动 FFmpeg 转码（transcodeToH264={}）...", ffmpegConfig.isTranscodeToH264());
            Process ffmpegProcess = startFfmpegConvertLocal(rawFile, mp4File, ffmpegConfig.isTranscodeToH264());

            // 9. 等待 FFmpeg 完成：超时按录像时长的1.5倍计算，最少5分钟
            long ffmpegTimeoutSec = Math.max(durationSeconds * 1, 300);
            log.info("等待 FFmpeg 转换完成，超时 {}s（录像时长 {}s）", ffmpegTimeoutSec, durationSeconds);
            boolean ffmpegDone = ffmpegProcess.waitFor(ffmpegTimeoutSec, TimeUnit.SECONDS);
            if (!ffmpegDone) {
                log.error("FFmpeg 转换超时，强制终止");
                ffmpegProcess.destroyForcibly();
                throw new IOException("FFmpeg 转换超时");
            }

            if (ffmpegProcess.exitValue() != 0) {
                log.error("FFmpeg 转换失败，exitCode={}", ffmpegProcess.exitValue());
                throw new IOException("FFmpeg 转换失败，exitCode=" + ffmpegProcess.exitValue());
            }

            // 10. 检查转码后的文件
            if (!Files.exists(mp4File) || Files.size(mp4File) == 0) {
                log.error("转码后文件不存在或为空: {}", mp4File);
                throw new IOException("转码后文件不存在或为空");
            }

            // 11. 发送文件给用户
            long fileSize = Files.size(mp4File);
            log.info("转换完成，MP4 文件大小 {} bytes，开始发送给用户", fileSize);
            try (InputStream fis = Files.newInputStream(mp4File)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = fis.read(buf)) != -1) {
                    outputStream.write(buf, 0, read);
                }
                outputStream.flush();
            }
            log.info("文件已发送给用户");
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("下载被中断");
            return false;
        } catch (IOException e) {
            log.error("下载 IO 异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("下载异常: {}", e.getMessage());
            throw new IOException("下载异常: " + e.getMessage(), e);
        } finally {
            // 确保下载句柄被关闭
            if (fileHandle != null && fileHandle.intValue() != -1) {
                hcNetSDK.NET_DVR_StopGetFile(fileHandle);
                log.info("finally中已停止下载句柄");
            }
            // 确保临时文件被清理
            if (rawFile != null) {
                try { Files.deleteIfExists(rawFile); log.info("已删除临时文件: {}", rawFile); } catch (IOException e) { log.warn("删除临时文件失败: {}: {}", rawFile, e.getMessage()); }
            }
            if (mp4File != null) {
                try { Files.deleteIfExists(mp4File); log.info("已删除临时文件: {}", mp4File); } catch (IOException e) { log.warn("删除临时文件失败: {}: {}", mp4File, e.getMessage()); }
            }
        }
    }

    // ======================== 本地文件转换 ========================
    private Process startFfmpegConvertLocal(Path inputFile, Path outputFile, boolean transcodeToH264) throws IOException {
        List<String> ffmpegArgs = new ArrayList<>();
        ffmpegArgs.add(ffmpegConfig.getPath());
        ffmpegArgs.add("-y"); // 覆盖输出文件
        ffmpegArgs.add("-i");
        ffmpegArgs.add(inputFile.toString());

        if (transcodeToH264) {
            // H.265 -> H.264 重编码
            ffmpegArgs.add("-c:v");
            ffmpegArgs.add("libx264");
            ffmpegArgs.add("-preset");
            ffmpegArgs.add("ultrafast");
            ffmpegArgs.add("-crf");
            ffmpegArgs.add("28");
        } else {
            // H.265 -> H.265 重编码
            ffmpegArgs.add("-c:v");
            ffmpegArgs.add("libx265");
            ffmpegArgs.add("-preset");
            ffmpegArgs.add("ultrafast");
            ffmpegArgs.add("-crf");
            ffmpegArgs.add("28");
        }
        ffmpegArgs.add("-an");

        ffmpegArgs.add("-f");
        ffmpegArgs.add("mp4");
        ffmpegArgs.add(outputFile.toString());

        String[] args = ffmpegArgs.toArray(new String[0]);
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        // stderr 消费线程
        sharedExecutor.submit(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    log.info("FFmpeg convert stderr: {}", line);
                }
            } catch (IOException e) {
                log.debug("FFmpeg convert stderr 结束");
            }
        });

        return process;
    }

    // ======================== IMKH 私有格式 -> Annex-B H.265 ========================
    // IMKH 是海康私有封装：IMKH(4B) + version(1B) + frameType(1B) + timestamp(8B) + dataLen(4B) + NAL数据
    private static final byte[] IMKH_MAGIC = {0x49, 0x4d, 0x4b, 0x48}; // "IMKH"
    private static final byte[] ANNEX_B_START_CODE = {0x00, 0x00, 0x00, 0x01};

    private byte[] parseImkhToAnnexB(byte[] data, int length) {
        if (length < 20) return null;

        // 检查 IMKH 魔数
        if (data[0] != IMKH_MAGIC[0] || data[1] != IMKH_MAGIC[1]
                || data[2] != IMKH_MAGIC[2] || data[3] != IMKH_MAGIC[3]) {
            // 不是 IMKH，搜索 start code
            return findH265StartCode(data, length);
        }

        // IMKH 帧头结构：
        // 0-3:   "IMKH" 魔数
        // 4:     版本
        // 5-8:   保留
        // 9:     帧类型
        // 10-17: 时间戳 (8字节)
        // 18-21: 数据长度 (little-endian)
        // 22+:   NAL 单元数据

        // 解析帧类型（字节9）
        int frameType = data[9] & 0xFF;
        // 0=视频I帧, 1=视频P帧, 2=视频B帧, 3=视频S帧/关键帧，其他=音频/封装，跳过
        if (frameType > 3) {
            log.debug("跳过非视频帧，frameType={}, size={}", frameType, length);
            return null;
        }

        // 解析数据长度 (字节18-21, little-endian)
        int dataLen = (data[18] & 0xFF) | ((data[19] & 0xFF) << 8)
                | ((data[20] & 0xFF) << 16) | ((data[21] & 0xFF) << 24);

        log.debug("IMKH: frameType={}, dataLen={}, length={}", frameType, dataLen, length);

        int nalOffset = 22;
        if (dataLen > 0 && nalOffset + dataLen <= length) {
            // 检查 NAL 数据是否全为0（无实际视频内容）
            boolean allZeros = true;
            for (int i = nalOffset; i < nalOffset + Math.min(dataLen, 16); i++) {
                if (data[i] != 0) { allZeros = false; break; }
            }
            if (allZeros) {
                log.debug("IMKH dataLen={} 但 NAL 数据全为0，跳过", dataLen);
                return null;
            }
            // 有实际内容，直接提取 NAL 并加上 start code
            byte[] result = new byte[ANNEX_B_START_CODE.length + dataLen];
            System.arraycopy(ANNEX_B_START_CODE, 0, result, 0, ANNEX_B_START_CODE.length);
            System.arraycopy(data, nalOffset, result, ANNEX_B_START_CODE.length, dataLen);
            return result;
        }

        // dataLen 无效或为0，搜索 H.265 start code
        logBinary("IMKH dataLen fallback，data", data, Math.min(48, length));
        if (nalOffset < length - 4) {
            for (int i = nalOffset; i < length - 4; i++) {
                if (data[i] == 0x00 && data[i+1] == 0x00 && data[i+2] == 0x00 && data[i+3] == 0x01) {
                    byte[] nal = new byte[length - i];
                    System.arraycopy(data, i, nal, 0, length - i);
                    return nal;
                }
            }
        }

        // 搜不到，返回原始数据加 start code
        return findH265StartCode(data, length);
    }

    private byte[] findH265StartCode(byte[] data, int length) {
        for (int i = 0; i < length - 4; i++) {
            if (data[i] == 0x00 && data[i+1] == 0x00 && data[i+2] == 0x00 && data[i+3] == 0x01) {
                byte[] nal = new byte[length - i];
                System.arraycopy(data, i, nal, 0, length - i);
                return nal;
            }
        }
        byte[] result = new byte[ANNEX_B_START_CODE.length + length];
        System.arraycopy(ANNEX_B_START_CODE, 0, result, 0, ANNEX_B_START_CODE.length);
        System.arraycopy(data, 0, result, ANNEX_B_START_CODE.length, length);
        return result;
    }

    private void logBinary(String prefix, byte[] data, int len) {
        StringBuilder sb = new StringBuilder(prefix).append(": ");
        int limit = Math.min(len, data.length);
        for (int i = 0; i < limit; i++) {
            sb.append(String.format("%02X ", data[i]));
            if ((i + 1) % 16 == 0 && i < limit - 1) sb.append("\n");
        }
        log.info(sb.toString());
    }

    // ======================== 停止回放 ========================
    @Override
    public void stopPlayback() {
        log.info("停止回放: 遍历 resourcesMap");

        List<Integer> keys = new ArrayList<>(resourcesMap.keySet());
        for (Integer playHandleInt : keys) {
            PlaybackResources r = resourcesMap.get(playHandleInt);
            if (r != null) {
                log.info("停止回放: playHandle={}", playHandleInt);
                r.markStopped();
                r.stopLatch.countDown();
                if (r.dataTimeoutThread != null) {
                    r.dataTimeoutThread.interrupt();
                }
                hcNetSDK.NET_DVR_StopPlayBack(new NativeLong(playHandleInt));
                resourcesMap.remove(playHandleInt);
            }
        }

        log.info("停止回放完成");
    }

    @PreDestroy
    public void destroy() {
        log.info("应用关闭，强制停止所有回放资源...");
        stopPlayback();
        sharedScheduler.shutdownNow();
        sharedExecutor.shutdownNow();
        log.info("应用关闭完成");
    }
}