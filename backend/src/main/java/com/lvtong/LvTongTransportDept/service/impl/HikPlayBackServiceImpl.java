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