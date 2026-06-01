package com.lvtong.LvTongTransportDept.service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;

/**
 * 海康威视 NVR 回放服务接口
 */
public interface HikPlayBackService {

    /**
     * 按时间范围回放指定通道的录像（FLV实时流）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param channel 通道号（1-5）
     * @param outputStream NVR码流输出到的流
     */
    void playBackByTime(LocalDateTime startTime, LocalDateTime endTime, int channel, OutputStream outputStream);

    /**
     * 下载指定通道的录像（转码为H.264 MP4，或封装为H.265 MP4）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param channel 通道号（1-5）
     * @param outputStream 下载流
     * @return 是否下载成功
     */
    boolean downloadVideo(LocalDateTime startTime, LocalDateTime endTime, int channel, OutputStream outputStream) throws IOException;

    /**
     * 停止回放
     */
    void stopPlayback();
}