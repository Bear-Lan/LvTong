package com.lvtong.LvTongTransportDept.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lvtong.LvTongTransportDept.constant.VehicleConstants;
import com.lvtong.LvTongTransportDept.service.AIDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * AI识别服务实现
 *
 * 【职责】
 * 1. 接收前端上传的图片文件
 * 2. 转发请求到AI服务器（车辆/货物识别、行驶证识别）
 * 3. 解析AI服务器响应并返回结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIDetectionServiceImpl implements AIDetectionService {

    private static final String VEHICLE_GOODS_URL = "http://192.168.88.245:8895/detect";
    private static final String DRIVER_LICENSE_URL = "http://192.168.88.245:8894/dl_ocr/front_and_back";

    private static final int CONNECT_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 120000;

    @Override
    public Map<String, Object> detectVehicle(MultipartFile file) {
        try {
            String params = "score_threshold=0.95&detic_score_threshold=0.8";

            // 转发到AI服务器，使用 image 参数名
            String response = doMultipartPostWithParams(VEHICLE_GOODS_URL, file.getInputStream(), file.getOriginalFilename(), "image", params);

            JSONObject jsonResponse = JSON.parseObject(response);

            // 提取 wheel_count、cratetype 和 data 数组
            Map<String, Object> result = new HashMap<>();
            if (jsonResponse.containsKey("wheel_count")) {
                result.put("wheel_count", jsonResponse.get("wheel_count"));
            }
            if (jsonResponse.containsKey("cratetype")) {
                String cratetype = jsonResponse.getString("cratetype");
                result.put("cratetype", cratetype);
                result.put("cratetype_text", VehicleConstants.getContainerTypeText(cratetype));
            }
            if (jsonResponse.containsKey("data")) {
                result.put("data", jsonResponse.get("data"));
            }

            return result;
        } catch (Exception e) {
            log.error("车辆识别失败", e);
            throw new RuntimeException("车辆识别失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> detectGoods(MultipartFile file) {
        try {
            // 添加固定参数
            String params = "score_threshold=0.95&detic_score_threshold=0.8";

            // 转发到AI服务器，使用 real_image 参数名
            String response = doMultipartPostWithParams(VEHICLE_GOODS_URL, file.getInputStream(), file.getOriginalFilename(), "real_image", params);

            JSONObject jsonResponse = JSON.parseObject(response);

            // 过滤 real_object_data 中 product_code != "others" 的项
            Map<String, Object> result = new HashMap<>();
            if (jsonResponse.containsKey("real_object_data")) {
                JSONArray allData = jsonResponse.getJSONArray("real_object_data");
                JSONArray filteredData = new JSONArray();
                if (allData != null) {
                    for (int i = 0; i < allData.size(); i++) {
                        JSONObject item = allData.getJSONObject(i);
                        String productCode = item.getString("product_code");
                        if (productCode != null && !"others".equalsIgnoreCase(productCode)) {
                            filteredData.add(item);
                        }
                    }
                }
                result.put("real_object_data", filteredData);
            }

            return result;
        } catch (Exception e) {
            log.error("货物识别失败", e);
            throw new RuntimeException("货物识别失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> detectDriverLicense(MultipartFile file) {
        try {
            // 转发到行驶证识别服务器，使用 file 参数名
            String response = doMultipartPost(DRIVER_LICENSE_URL, file.getInputStream(), file.getOriginalFilename(), "file");

            // 直接返回完整JSON
            return JSON.parseObject(response);
        } catch (Exception e) {
            log.error("行驶证识别失败", e);
            throw new RuntimeException("行驶证识别失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> detectAxle(MultipartFile file) {
        try {
            String params = "score_threshold=0.95&detic_score_threshold=0.8&return_image=1";
            String response = doMultipartPostWithParams(VEHICLE_GOODS_URL, file.getInputStream(), file.getOriginalFilename(), "image", params);

            JSONObject jsonResponse = JSON.parseObject(response);

            Map<String, Object> result = new HashMap<>();
            if (jsonResponse.containsKey("wheel_count")) {
                result.put("wheel_count", jsonResponse.get("wheel_count"));
            }
            if (jsonResponse.containsKey("cratetype")) {
                result.put("cratetype", jsonResponse.getString("cratetype"));
            }
            if (jsonResponse.containsKey("data")) {
                result.put("data", jsonResponse.get("data"));
            }
            if (jsonResponse.containsKey("result_image")) {
                result.put("result_image", jsonResponse.get("result_image"));
            }

            return result;
        } catch (Exception e) {
            log.error("车轴识别失败", e);
            throw new RuntimeException("车轴识别失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> detectCarriage(MultipartFile file) {
        try {
            String params = "score_threshold=0.95&detic_score_threshold=0.8&return_image=1";
            String response = doMultipartPostWithParams(VEHICLE_GOODS_URL, file.getInputStream(), file.getOriginalFilename(), "image", params);

            JSONObject jsonResponse = JSON.parseObject(response);

            Map<String, Object> result = new HashMap<>();
            if (jsonResponse.containsKey("cratetype")) {
                String cratetype = jsonResponse.getString("cratetype");
                result.put("cratetype", cratetype);
                result.put("cratetype_text", VehicleConstants.getContainerTypeText(cratetype));
            }
            if (jsonResponse.containsKey("data")) {
                result.put("data", jsonResponse.get("data"));
            }
            if (jsonResponse.containsKey("result_image")) {
                result.put("result_image", jsonResponse.get("result_image"));
            }

            return result;
        } catch (Exception e) {
            log.error("车厢识别失败", e);
            throw new RuntimeException("车厢识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送multipart请求到AI服务器
     */
    private String doMultipartPost(String urlString, InputStream fileInputStream, String filename, String fieldName) throws Exception {
        String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString();
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream os = conn.getOutputStream()) {
            // 写入文件字段
            StringBuilder sb = new StringBuilder();
            sb.append(twoHyphens).append(boundary).append(lineEnd);
            sb.append("Content-Disposition: form-data; name=\"").append(fieldName).append("\"; filename=\"").append(filename).append("\"").append(lineEnd);
            sb.append("Content-Type: image/jpeg").append(lineEnd);
            sb.append(lineEnd);

            os.write(sb.toString().getBytes(StandardCharsets.UTF_8));

            // 写入文件内容
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            os.write(lineEnd.getBytes(StandardCharsets.UTF_8));

            // 结束标记
            byte[] endBoundary = (lineEnd + twoHyphens + boundary + twoHyphens + lineEnd).getBytes(StandardCharsets.UTF_8);
            os.write(endBoundary);
        }

        // 读取响应
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("AI服务器返回错误码: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    /**
     * 发送带参数的多部分请求
     */
    private String doMultipartPostWithParams(String urlString, InputStream fileInputStream, String filename, String fieldName, String additionalParams) throws Exception {
        String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString();
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream os = conn.getOutputStream()) {
            // 先写入额外参数
            if (additionalParams != null && !additionalParams.isEmpty()) {
                String[] params = additionalParams.split("&");
                for (String param : params) {
                    String[] kv = param.split("=");
                    if (kv.length == 2) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(twoHyphens).append(boundary).append(lineEnd);
                        sb.append("Content-Disposition: form-data; name=\"").append(kv[0]).append("\"").append(lineEnd);
                        sb.append(lineEnd);
                        sb.append(kv[1]).append(lineEnd);
                        os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                    }
                }
            }

            // 写入文件字段
            StringBuilder sb = new StringBuilder();
            sb.append(twoHyphens).append(boundary).append(lineEnd);
            sb.append("Content-Disposition: form-data; name=\"").append(fieldName).append("\"; filename=\"").append(filename).append("\"").append(lineEnd);
            sb.append("Content-Type: image/jpeg").append(lineEnd);
            sb.append(lineEnd);

            os.write(sb.toString().getBytes(StandardCharsets.UTF_8));

            // 写入文件内容
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            os.write(lineEnd.getBytes(StandardCharsets.UTF_8));

            // 结束标记
            byte[] endBoundary = (lineEnd + twoHyphens + boundary + twoHyphens + lineEnd).getBytes(StandardCharsets.UTF_8);
            os.write(endBoundary);
        }

        // 读取响应
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("AI服务器返回错误码: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }
}