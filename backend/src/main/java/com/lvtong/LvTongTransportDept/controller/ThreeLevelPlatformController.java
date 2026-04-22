package com.lvtong.LvTongTransportDept.controller;

import com.lvtong.LvTongTransportDept.dto.ApiResponse;
import com.lvtong.LvTongTransportDept.dto.ThreeLevelCheckResultDto;
import com.lvtong.LvTongTransportDept.exception.BusinessException;
import com.lvtong.LvTongTransportDept.service.ThreeLevelPlatformService;
import com.lvtong.LvTongTransportDept.utils.UploadUtils.ModelSignTools;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

/**
 * 三级平台接收接口
 */
@Slf4j
@Tag(name = "三级平台接收", description = "接收二级平台上交的查验记录")
@RestController
@RequestMapping("/api/three-level")
public class ThreeLevelPlatformController {

    @Autowired
    private ThreeLevelPlatformService threeLevelPlatformService;

    @Operation(summary = "接收查验记录", description = "接收二级平台上交的查验记录（含图片），需要签名验证")
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> receiveUpload(
            @Parameter(description = "查验记录数据") @RequestBody ThreeLevelCheckResultDto dto,
            @Parameter(description = "认证信息") @RequestHeader("auth") String auth,
            @Parameter(description = "随机字符串") @RequestHeader("nonce") String nonce,
            @Parameter(description = "签名") @RequestHeader("sign") String sign,
            HttpServletRequest request) {

        // 1. 验证签名
        try {
            String jsonStr = com.alibaba.fastjson2.JSON.toJSONString(dto);
            String signContent = ModelSignTools.generateSignContent(jsonStr);

            String verifyContent = signContent + "&auth=" + auth + "&nonce=" + nonce;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(verifyContent.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02X", b & 0xff));
            String expectedSign = Base64.getEncoder().encodeToString(hex.toString().getBytes(StandardCharsets.UTF_8)).toUpperCase();

            if (!expectedSign.equals(sign)) {
                log.warn("签名验证失败, expected={}, actual={}", expectedSign, sign);
                return ApiResponse.error(401, "签名验证失败");
            }
        } catch (Exception e) {
            log.error("签名验证异常: {}", e.getMessage());
            return ApiResponse.error(401, "签名验证失败");
        }

        // 2. 保存数据
        try {
            Map<String, Object> result = threeLevelPlatformService.receiveAndSave(dto);
            Boolean success = (Boolean) result.get("success");
            if (Boolean.TRUE.equals(success)) {
                return ApiResponse.success("接收成功", result);
            }
            String msg = (String) result.get("msg");
            return ApiResponse.error(msg);
        } catch (BusinessException e) {
            log.error("接收业务异常: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("接收系统异常: {}", e.getMessage(), e);
            return ApiResponse.error("接收失败: " + e.getMessage());
        }
    }
}