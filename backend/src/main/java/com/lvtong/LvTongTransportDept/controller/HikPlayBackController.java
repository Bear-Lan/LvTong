package com.lvtong.LvTongTransportDept.controller;

import com.lvtong.LvTongTransportDept.service.HikPlayBackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 海康威视 NVR 录像回放接口
 * 按时间范围回放录像，转FLV流输出到前端
 */
@Slf4j
@RestController
@RequestMapping("/api/hikNet")
@Tag(name = "海康网络设备", description = "海康NVR录像回放接口")
@CrossOrigin
public class HikPlayBackController {

    @Autowired
    private HikPlayBackService hikPlayBackService;

    private static final int MAX_DURATION_HOURS = 1;

    /**
     * 单通道回放（按时间范围）
     * 返回 FLV 视频流
     */
    @GetMapping("/playBackVideo")
    @Operation(summary = "NVR录像回放（单通道）", description = "按时间范围回放指定通道的录像，返回FLV流")
    public void playBackVideo(
            @Parameter(description = "开始时间 yyyy-MM-dd HH:mm:ss")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间 yyyy-MM-dd HH:mm:ss")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "通道号 1-5")
            @RequestParam(defaultValue = "1") int channel,
            HttpServletResponse response) {

        // 时间范围校验
        if (startTime.isAfter(endTime)) {
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("开始时间不能大于结束时间");
            } catch (IOException e) {
                log.error("响应写入异常", e);
            }
            return;
        }
        if (startTime.plusHours(MAX_DURATION_HOURS).isBefore(endTime)) {
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("时间区间不能超过一小时");
            } catch (IOException e) {
                log.error("响应写入异常", e);
            }
            return;
        }

        response.setContentType("video/x-flv");
        response.setHeader("Connection", "keep-alive");
        response.setStatus(HttpServletResponse.SC_OK);

        try (OutputStream out = response.getOutputStream()) {
            hikPlayBackService.playBackByTime(startTime, endTime, channel, out);
        } catch (Exception e) {
            log.error("回放异常: {}", e.getMessage());
        } finally {
            hikPlayBackService.stopPlayback();
        }
    }

    /**
     * 五通道同时回放（acceptance_time 到 inspection_time）
     * 返回 FLV 视频流（按预约机通道返回）
     */
    @GetMapping("/playBackVideoAll")
    @Operation(summary = "NVR录像回放（5通道）", description = "按时间范围回放所有5个通道的录像，返回FLV流")
    public void playBackVideoAll(
            @Parameter(description = "开始时间 yyyy-MM-dd HH:mm:ss")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间 yyyy-MM-dd HH:mm:ss")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpServletResponse response) {

        // 时间范围校验
        if (startTime.isAfter(endTime)) {
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("开始时间不能大于结束时间");
            } catch (IOException e) {
                log.error("响应写入异常", e);
            }
            return;
        }
        if (startTime.plusHours(MAX_DURATION_HOURS).isBefore(endTime)) {
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("时间区间不能超过一小时");
            } catch (IOException e) {
                log.error("响应写入异常", e);
            }
            return;
        }

        response.setContentType("video/x-flv");
        response.setHeader("Connection", "keep-alive");
        response.setStatus(HttpServletResponse.SC_OK);

        try (OutputStream out = response.getOutputStream()) {
            // 预约机通道作为主回放通道（5个通道同步回放）
            hikPlayBackService.playBackByTime(startTime, endTime, 4, out);
        } catch (Exception e) {
            log.error("回放异常: {}", e.getMessage());
        } finally {
            hikPlayBackService.stopPlayback();
        }
    }
}