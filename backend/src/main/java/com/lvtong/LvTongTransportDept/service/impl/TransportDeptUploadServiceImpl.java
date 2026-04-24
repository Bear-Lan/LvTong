package com.lvtong.LvTongTransportDept.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lvtong.LvTongTransportDept.config.TransportDeptProperties;
import com.lvtong.LvTongTransportDept.converter.TransportDeptConverter;
import com.lvtong.LvTongTransportDept.dto.TransportDeptCheckResultDto;
import com.lvtong.LvTongTransportDept.entity.VehicleInspection;
import com.lvtong.LvTongTransportDept.exception.BusinessException;
import com.lvtong.LvTongTransportDept.mapper.VehicleInspectionMapper;
import com.lvtong.LvTongTransportDept.service.TransportDeptUploadService;
import com.lvtong.LvTongTransportDept.utils.UploadUtils.ModelSignTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;

/**
 * 交通局上报服务
 *
 * 【职责】
 * 1. 查询并转换查验记录为交通局 JSON DTO
 * 2. 图片压缩（缩放+JPEG压缩）+ Base64 编码（减小体积避免 OOM）
 * 3. 流式写入 JSON + GZIP 压缩后发送到 HTTPS（避免内存 OOM）
 * 4. 解析响应并更新报送状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransportDeptUploadServiceImpl implements TransportDeptUploadService {

    private final VehicleInspectionMapper mapper;
    private final TransportDeptConverter converter;
    private final TransportDeptProperties props;

    private static final String UPLOAD_PATH = "/greenpass/signapi/channel/uploadCheckResult";

    // ================================================================
    // 对外接口
    // ================================================================

    @Override
    @Transactional
    public Map<String, Object> uploadSingle(Integer id, List<String> excludePhotoTypes) {
        VehicleInspection record = mapper.selectById(id);
        if (record == null) {
            return Map.of("success", false, "code", -1, "msg", "查验记录不存在");
        }
        return uploadOne(record, excludePhotoTypes);
    }


    // ================================================================
    // 单条上报流程
    // ================================================================

    private Map<String, Object> uploadOne(VehicleInspection record, List<String> excludePhotoTypes) {
        List<String> errors = new ArrayList<>();
        try {
            // 1. 转换 DTO（带排除图片类型和错误收集）
            TransportDeptCheckResultDto dto = converter.toDto(record, excludePhotoTypes, errors);

            // 2. 按样例方式发送（不用压缩）
            String response = doPost(dto);

            // 3. 解析响应并更新状态
            Map<String, Object> result = parseAndUpdate(record.getId(), response);

            // 4. 如果有图片解析错误，添加到交通部返回消息后面，标题是 tips
            if (!errors.isEmpty()) {
                String errorMsg = String.join("; ", errors);
                log.warn("图片解析存在错误: {}", errorMsg);
                String originalMsg = (String) result.get("msg");
                result.put("msg", originalMsg + " <br/>[tips] 部分图片因解析错误被舍弃: " + errorMsg);
            }

            return result;

        } catch (BusinessException e) {
            log.error("上报业务异常, id={}: {}", record.getId(), e.getMessage());
            updateUploadState(record.getId(), 2, e.getMessage());
            return Map.of("success", false, "code", 2, "msg", e.getMessage());
        } catch (Exception e) {
            log.error("上报查验记录异常, id={}", record.getId(), e);
            updateUploadState(record.getId(), -1, e.getMessage());
            return Map.of("success", false, "code", 2, "msg", e.getMessage());
        }
    }

    /**
     * 按样例方式上传：
     * 1. DTO 直接序列化 JSON
     * 2. 生成签名
     * 3. HTTPS 发送
     */
    private String doPost(TransportDeptCheckResultDto dto) throws Exception {
        String url = props.getHttpsUrl() + UPLOAD_PATH;
        String jsonStr = JSON.toJSONString(dto);

        // 生成签名
        String signContent = ModelSignTools.generateSignContent(jsonStr);

        log.info("待签名字符串: {}", signContent);

        HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
        try {
            // 设置请求参数
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Java HTTPS Client");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

            // 设置连接参数
            conn.setConnectTimeout(30000);  // 30秒连接超时
            conn.setReadTimeout(300000);    // 300秒读取超时
            conn.setDoOutput(true);

            String auth = props.getClientId() + "_" + props.getClientKey();
            // nonce 格式：yyyyMMddHHmmss_随机字符串（与样例一致）
            String nonce = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String sign = buildSign(auth, nonce, signContent);

            log.info("auth: {}, nonce: {}, sign: {}", auth, nonce, sign);

            conn.setRequestProperty("auth", props.getClientId());
            conn.setRequestProperty("nonce", nonce);
            conn.setRequestProperty("sign", sign);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonStr.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            log.info("交通局 API 响应码: {}", code);

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            code == 200 ? conn.getInputStream() : conn.getErrorStream(),
                            StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } finally {
            conn.disconnect();
        }
    }



    // ================================================================
    // 签名 & HTTPS 辅助
    // ================================================================

    private String buildSign(String auth, String nonce, String signContent) throws Exception {
        String content = signContent + "&auth=" + auth + "&nonce=" + nonce;
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) hex.append(String.format("%02X", b & 0xff));
        return Base64.getEncoder().encodeToString(hex.toString().getBytes(StandardCharsets.UTF_8)).toUpperCase();
    }

    // ================================================================
    // 响应解析 & 状态更新
    // ================================================================

    private Map<String, Object> parseAndUpdate(Integer id, String response) {
        int code = -1;
        String msg = "未知错误";
        try {
            JSONObject json = JSON.parseObject(response);
            code = json.getIntValue("code");
            msg = json.getString("msg");
        } catch (Exception e) {
            msg = response;
            log.warn("响应无法解析为 JSON: {}", response);
        }
        updateUploadState(id, code == 200 ? 1 : 2, msg);
        Map<String, Object> result = new HashMap<>();
        result.put("success", code == 200);
        result.put("code", code);
        result.put("msg", msg);
        return result;
    }

    private void updateUploadState(Integer id, int state, String comment) {
        // 上报成功时设置复核结果为审核通过(1)，失败时设置为审核未通过(2)
        Integer manualReviewState = (state == 1) ? 1 : 2;
        mapper.update(null,
                new LambdaUpdateWrapper<VehicleInspection>()
                        .set(VehicleInspection::getToTransportdeptState, state)
                        .set(VehicleInspection::getToTransportdeptTime, java.time.LocalDateTime.now())
                        .set(VehicleInspection::getToTransportdeptComment, comment)
                        .set(VehicleInspection::getManualReviewState, manualReviewState)
                        .eq(VehicleInspection::getId, id)
        );
    }
}
