package com.lvtong.LvTongTransportDept.service.impl;

import com.lvtong.LvTongTransportDept.hksdk.HCNetSDK;
import com.lvtong.LvTongTransportDept.service.HikPlayBackService;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 海康威视 NVR 回放服务
 * 使用 JNA SDK 获取回放数据 + FFmpeg 转封装为 FLV
 */
@Slf4j
@Service
public class HikPlayBackServiceImpl implements HikPlayBackService {

    // NVR设备配置
    private static final String NVR_IP = "192.168.218.250";
    private static final int NVR_PORT = 8000;
    private static final String NVR_USERNAME = "admin";
    private static final String NVR_PASSWORD = "whds@2025";

    // FFmpeg路径
    private static final String FFMPEG_PATH = "D:\\tools\\ffmpeg-master-latest-win64-gpl\\bin\\ffmpeg.exe";

    // 通道号映射
    private static final int CHANNEL_LANE = 3;
    private static final int CHANNEL_FRONT = 1;
    private static final int CHANNEL_REAR = 2;
    private static final int CHANNEL_APPOINTMENT = 4;
    private static final int CHANNEL_PTZ360 = 5;

    @Autowired
    private HCNetSDK hcNetSDK;

    @Override
    public void playBackByTime(LocalDateTime startTime, LocalDateTime endTime, int channel, OutputStream outputStream) {
        log.info("JNA回放: startTime={}, endTime={}, channel={}", startTime, endTime, channel);

        // 初始化SDK
        if (!hcNetSDK.NET_DVR_Init()) {
            log.error("SDK初始化失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
            return;
        }

        hcNetSDK.NET_DVR_SetConnectTime(2000, 1);
        hcNetSDK.NET_DVR_SetReconnect(100000, true);

        // 登录
        HCNetSDK.NET_DVR_DEVICEINFO_V30 deviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
        deviceInfo.write();
        NativeLong userId = hcNetSDK.NET_DVR_Login_V30(NVR_IP, (short) NVR_PORT, NVR_USERNAME, NVR_PASSWORD, deviceInfo);
        if (userId.intValue() == -1) {
            log.error("登录失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
            return;
        }
        deviceInfo.read();
        log.info("登录成功，userId={}", userId);

        // 创建管道
        java.nio.channels.Pipe pipe;
        try {
            pipe = java.nio.channels.Pipe.open();
        } catch (IOException e) {
            log.error("创建管道失败: {}", e.getMessage());
            return;
        }
        final java.nio.channels.Pipe finalPipe = pipe;
        OutputStream pipeSink = java.nio.channels.Channels.newOutputStream(finalPipe.sink());

        // 构建回放参数
        HCNetSDK.NET_DVR_VOD_PARA vodPara = new HCNetSDK.NET_DVR_VOD_PARA();
        vodPara.dwSize = vodPara.size();
        vodPara.struIDInfo.dwSize = vodPara.struIDInfo.size();
        // 通道号偏移33 (D1=33, D2=34, ...)
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

        // 开始回放
        NativeLong playHandle = hcNetSDK.NET_DVR_PlayBackByTime_V40(userId, vodPara);
        if (playHandle.intValue() == -1) {
            log.error("按时间回放失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
            return;
        }
        log.info("回放句柄: {}", playHandle);

        // 开启取流
        IntByReference intInlen1 = new IntByReference(0);
        boolean bCtrl = hcNetSDK.NET_DVR_PlayBackControl_V40(playHandle, HCNetSDK.NET_DVR_PLAYSTART, Pointer.NULL, 0, Pointer.NULL, intInlen1);
        if (!bCtrl) {
            log.error("NET_DVR_PLAYSTART失败，错误码: {}", hcNetSDK.NET_DVR_GetLastError());
            return;
        }
        log.info("开启取流成功");

        // 设置数据回调
        final int finalChannel = channel;
        HCNetSDK.FPlayDataCallBack callback = new HCNetSDK.FPlayDataCallBack() {
            public void invoke(int lPlayHandle, int dwDataType, Pointer pBuffer, int dwBufSize, int dwUser) {
                log.info("通道{}收到数据: dwDataType={}, dwBufSize={}", finalChannel, dwDataType, dwBufSize);
                if (dwDataType == 1 || dwDataType == 2) { // 视频数据
                    try {
                        ByteBuffer buf = pBuffer.getByteBuffer(0, dwBufSize);
                        byte[] data = new byte[dwBufSize];
                        buf.get(data);
                        pipeSink.write(data);
                        log.debug("通道{}写入{} bytes到管道", finalChannel, dwBufSize);
                    } catch (IOException e) {
                        log.error("写入管道异常: {}", e.getMessage());
                    }
                }
            }
        };
        hcNetSDK.NET_DVR_SetPlayDataCallBack(playHandle, callback, Pointer.NULL);

        // 启动FFmpeg进行转封装
        Process ffmpegProcess = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    FFMPEG_PATH,
                    "-f", "h264",
                    "-i", "pipe:0",
                    "-c:v", "copy",
                    "-f", "flv",
                    "-"
            );
            pb.redirectErrorStream(false);
            ffmpegProcess = pb.start();
            log.info("FFmpeg进程启动");

            InputStream ffmpegInput = ffmpegProcess.getInputStream();
            InputStream ffmpegError = ffmpegProcess.getErrorStream();

            // 启动FFmpeg错误流读取线程
            Thread errorReader = new Thread(() -> {
                try {
                    byte[] errBuf = new byte[1024];
                    int errRead;
                    while ((errRead = ffmpegError.read(errBuf)) != -1) {
                        String errMsg = new String(errBuf, 0, errRead);
                        if (errMsg.contains("frame=") || errMsg.contains("error") || errMsg.contains("Error")) {
                            log.warn("FFmpeg: {}", errMsg.trim());
                        }
                    }
                } catch (IOException e) {
                    // ignore
                }
            });
            errorReader.start();

            // 将管道数据通过FFmpeg转换后输出
            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalBytes = 0;
            while (!Thread.currentThread().isInterrupted()) {
                bytesRead = ffmpegInput.read(buffer);
                if (bytesRead == -1) {
                    log.info("FFmpeg输入结束");
                    break;
                }
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                if (totalBytes % (100 * 8192) < 8192) {
                    log.debug("已写入 {} bytes", totalBytes);
                }
            }
            outputStream.flush();
            log.info("转封装完成，总共写入 {} bytes", totalBytes);

        } catch (IOException e) {
            log.error("FFmpeg转封装异常: {}", e.getMessage());
        } finally {
            if (playHandle != null && playHandle.intValue() > 0) {
                hcNetSDK.NET_DVR_StopPlayBack(playHandle);
            }
            if (userId != null && userId.intValue() > 0) {
                hcNetSDK.NET_DVR_Logout(userId);
            }
            if (ffmpegProcess != null) {
                ffmpegProcess.destroy();
                try {
                    ffmpegProcess.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    // ignore
                }
                if (ffmpegProcess.isAlive()) {
                    ffmpegProcess.destroyForcibly();
                }
            }
            try {
                pipeSink.close();
            } catch (IOException e) {
                // ignore
            }
            log.info("资源已清理");
        }
    }

    @Override
    public void stopPlayback() {
        log.info("停止回放");
    }

    public static int getChannelByCameraType(String cameraType) {
        return switch (cameraType) {
            case "lane" -> CHANNEL_LANE;
            case "front" -> CHANNEL_FRONT;
            case "rear" -> CHANNEL_REAR;
            case "appointment" -> CHANNEL_APPOINTMENT;
            case "ptz360" -> CHANNEL_PTZ360;
            default -> CHANNEL_LANE;
        };
    }
}
