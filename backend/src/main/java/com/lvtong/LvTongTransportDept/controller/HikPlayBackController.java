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

        log.info("=== 收到回放请求: channel={}, startTime={}, endTime={} ===", channel, startTime, endTime);

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
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);
        try (OutputStream out = response.getOutputStream()) {
            hikPlayBackService.playBackByTime(startTime, endTime, channel, out);
        } catch (Exception e) {
            log.error("回放异常: {}", e.getMessage(), e);
        }
        log.info("=== 回放请求处理完成: channel={} ===", channel);
    }
    @GetMapping("/stopPlayback")
    @Operation(summary = "停止回放", description = "停止当前进行中的录像回放，释放资源")
    public void stopPlayback() {
        log.info("收到停止回放请求");
        hikPlayBackService.stopPlayback();
    }
}
