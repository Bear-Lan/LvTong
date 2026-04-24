package com.lvtong.LvTongTransportDept;

import com.alibaba.fastjson2.JSON;
import com.lvtong.LvTongTransportDept.dto.ThreeLevelCheckResultDto;
import com.lvtong.LvTongTransportDept.dto.TransportDeptCheckResultDto.PhotoItem;
import com.lvtong.LvTongTransportDept.utils.UploadUtils.ModelSignTools;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 三级平台接口测试 - 单独运行
 */
public class TestThreeLevelPlatform {

    private static final String BASE_URL = "http://localhost:8080/api/three-level";

    public static void main(String[] args) {
        System.out.println("=== 三级平台接口测试 ===");

        // 1. 构建请求体
        ThreeLevelCheckResultDto dto = buildRequestDto();

        // 2. 生成签名
        Map<String, String> headers = buildHeaders(dto);

        // 3. 发送请求
        sendRequest(dto, headers);
    }

    private static ThreeLevelCheckResultDto buildRequestDto() {
        ThreeLevelCheckResultDto dto = new ThreeLevelCheckResultDto();

        dto.setCheckId("13800138000_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_13800138001");
        dto.setVehicleSign("0x02");
        dto.setVehicleClass(2);
        dto.setDriverTelephone("13800138000");
        dto.setVehicleId("京A12345_蓝");
        dto.setFreightTypes("A02");
        dto.setVehicleType("11");
        dto.setCrateType("1");
        dto.setWeightCheckBasis("2");
        dto.setEnWeight(15000);
        dto.setExWeight(18500);
        dto.setLoadRate(85.5);
        dto.setLoadWeight(3.5);
        dto.setVehicleSize("6000*2500*3500");
        dto.setCheckTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        dto.setEnStationId("0101");
        dto.setExStationId("0102");
        dto.setGroupId(1);
        dto.setInspector("13800138000");
        dto.setReviewer("13800138001");
        dto.setCheckResult(1);
        dto.setMediaType(1);
        dto.setTransactionId("TXN" + System.currentTimeMillis());
        dto.setPassId("PASS" + System.currentTimeMillis());
        dto.setExTime(LocalDateTime.now().minusMinutes(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        dto.setTransPayType(1);
        dto.setFee(3500L);
        dto.setPayFee(3500L);
        dto.setProvinceCount(1);
        dto.setOperation(1);
        dto.setMemo("");

        // 照片列表 - 加载图片文件并转为 Base64
        dto.setPhotos(loadPhotos());

        return dto;
    }

    /**
     * 加载照片文件并转换为 PhotoItem 列表（包含所有9种类型）
     */
    private static List<PhotoItem> loadPhotos() {
        List<PhotoItem> photos = new ArrayList<>();
        String imagePath = "D:\\LvTongTransportDept\\20260414_184240_76464.png";

        // 所有照片类型
        String[] typeIds = {"1", "2", "3", "4", "11", "12", "13", "24", "99"};
        String[] typeNames = {
            "通行凭证照片", "透视影像", "车身照", "证据链照片",
            "车头照", "车尾照", "证件照",
            "货物照", "车顶照"
        };

        File file = new File(imagePath);
        if (!file.exists()) {
            System.out.println("图片文件不存在: " + imagePath);
            return photos;
        }

        try {
            byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
            String base64Content = Base64.getEncoder().encodeToString(imageBytes);
            System.out.println("已加载图片: " + imagePath + " (" + imageBytes.length + " bytes)");

            // 每种类型都添加一张照片
            for (int i = 0; i < typeIds.length; i++) {
                PhotoItem photo = new PhotoItem();
                photo.setId("P" + System.currentTimeMillis() + "_" + typeIds[i]);
                photo.setTypeId(typeIds[i]);
                photo.setContent(base64Content);
                photo.setTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                photos.add(photo);
                System.out.println("  - typeId=" + typeIds[i] + " (" + typeNames[i] + ")");
            }
        } catch (IOException e) {
            System.out.println("读取图片失败: " + e.getMessage());
        }

        return photos;
    }

    private static Map<String, String> buildHeaders(ThreeLevelCheckResultDto dto) {
        String auth = "101004_ThreeLevelKey2024";
        String nonce = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 先保存照片列表（用于请求），签名计算时需要排除照片内容
        List<PhotoItem> savedPhotos = dto.getPhotos();
        dto.setPhotos(new ArrayList<>());  // 清空照片避免序列化内存溢出

        // 生成签名字符串
        String jsonStr = JSON.toJSONString(dto);
        String signContent = ModelSignTools.generateSignContent(jsonStr);

        // 恢复照片列表（用于实际请求）
        dto.setPhotos(savedPhotos);

        // 计算签名
        String verifyContent = signContent + "&auth=" + auth + "&nonce=" + nonce;
        String sign = computeSign(verifyContent);

        System.out.println("=== 请求信息 ===");
        System.out.println("auth: " + auth);
        System.out.println("nonce: " + nonce);
        System.out.println("signContent: " + signContent);
        System.out.println("verifyContent: " + verifyContent);
        System.out.println("sign: " + sign);

        Map<String, String> headers = new HashMap<>();
        headers.put("auth", auth);
        headers.put("nonce", nonce);
        headers.put("sign", sign);
        return headers;
    }

    private static String computeSign(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02X", b & 0xff));
            }
            return Base64.getEncoder().encodeToString(hex.toString().getBytes(StandardCharsets.UTF_8)).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("签名计算失败: " + e.getMessage(), e);
        }
    }

    private static void sendRequest(ThreeLevelCheckResultDto dto, Map<String, String> headers) {
        try {
            String urlStr = BASE_URL + "/upload";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

            // 设置签名 Headers
            conn.setRequestProperty("auth", headers.get("auth"));
            conn.setRequestProperty("nonce", headers.get("nonce"));
            conn.setRequestProperty("sign", headers.get("sign"));

            // 写入请求体
            String jsonStr = com.alibaba.fastjson2.JSON.toJSONString(dto);
            System.out.println("=== 请求体 ===");
            System.out.println("请求体大小: " + jsonStr.getBytes(StandardCharsets.UTF_8).length + " bytes");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonStr.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            System.out.println("\n=== 响应 ===");
            System.out.println("Response Code: " + responseCode);

            String response = readResponse(conn);
            System.out.println("Response Body: " + response);

            conn.disconnect();
        } catch (Exception e) {
            System.out.println("请求失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}