package com.lvtong.LvTongTransportDept.service;

import java.time.LocalDateTime;

/**
 * 海康威视 NVR 回放服务接口
 */
public interface HikPlayBackService {

    /**
     * 按时间范围回放指定通道的录像
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param channel 通道号（1-5）
     * @param outputStream NVR码流输出到的流
     */
    void playBackByTime(LocalDateTime startTime, LocalDateTime endTime, int channel, java.io.OutputStream outputStream);

    /**
     * 停止回放
     */
    void stopPlayback();
}