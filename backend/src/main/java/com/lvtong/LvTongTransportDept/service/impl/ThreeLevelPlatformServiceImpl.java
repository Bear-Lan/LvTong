package com.lvtong.LvTongTransportDept.service.impl;

import com.lvtong.LvTongTransportDept.config.ThreeLevelPlatformProperties;
import com.lvtong.LvTongTransportDept.dto.ThreeLevelCheckResultDto;
import com.lvtong.LvTongTransportDept.dto.TransportDeptCheckResultDto;
import com.lvtong.LvTongTransportDept.entity.VehicleInspection;
import com.lvtong.LvTongTransportDept.mapper.VehicleInspectionMapper;
import com.lvtong.LvTongTransportDept.service.ThreeLevelPlatformService;
import com.lvtong.LvTongTransportDept.utils.UploadUtils.ModelSignTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 三级平台接收服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThreeLevelPlatformServiceImpl implements ThreeLevelPlatformService {

    private final VehicleInspectionMapper mapper;
    private final ThreeLevelPlatformProperties props;

    private static final String UPLOAD_PATH = "/greenpass/signapi/channel/uploadCheckResult";

    @Override
    @Transactional
    public Map<String, Object> receiveAndSave(ThreeLevelCheckResultDto dto) {
        try {
            // 1. 保存图片到本地
            Map<String, List<String>> imagePaths = saveImages(dto.getPhotos());

            // 2. 转换并保存查验记录
            VehicleInspection record = convertToEntity(dto, imagePaths);
            mapper.insert(record);

            // 3. 上报到三级平台（如果配置了上行地址）
            if (props.getHttpsUrl() != null && !props.getHttpsUrl().isEmpty()) {
                uploadToThreeLevelPlatform(dto);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("code", 200);
            result.put("msg", "接收成功");
            result.put("id", record.getId());
            return result;

        } catch (Exception e) {
            log.error("接收三级平台数据异常: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("code", 500);
            result.put("msg", e.getMessage());
            return result;
        }
    }

    /**
     * 保存图片到本地
     * @return Map<字段名, 图片路径列表>
     */
    private Map<String, List<String>> saveImages(List<TransportDeptCheckResultDto.PhotoItem> photos) {
        Map<String, List<String>> paths = new HashMap<>();
        if (photos == null || photos.isEmpty()) {
            return paths;
        }

        String basePath = props.getLocalDir();
        if (basePath == null || basePath.isEmpty()) {
            basePath = "D:/soft/FtpLvTong";
        }

        try {
            Path dirPath = Paths.get(basePath);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
        } catch (Exception e) {
            log.warn("创建图片目录失败: {}", e.getMessage());
        }

        for (TransportDeptCheckResultDto.PhotoItem photo : photos) {
            if (photo.getContent() == null || photo.getContent().isEmpty()) {
                continue;
            }

            // 生成文件名：使用照片时间或当前时间
            String fileName = generatePhotoFileName(photo.getTime(), photo.getTypeId());
            String fullPath = basePath + "/" + fileName;

            try {
                // 解码 Base64 图片并保存
                byte[] imageBytes = Base64.getDecoder().decode(photo.getContent());
                Files.write(Paths.get(fullPath), imageBytes);

                // 根据 typeId 映射到对应字段，支持多张
                String fieldName = mapTypeIdToField(photo.getTypeId());
                if (fieldName != null) {
                    paths.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(fullPath);
                }
                log.info("保存图片成功: {} -> {}", fileName, fieldName);
            } catch (Exception e) {
                log.warn("保存图片失败, typeId={}: {}", photo.getTypeId(), e.getMessage());
            }
        }

        return paths;
    }

    /**
     * 根据照片时间生成文件名
     * 格式：yyyyMMdd_HHmmss_typeId.jpg
     */
    private String generatePhotoFileName(String photoTime, String typeId) {
        try {
            if (photoTime != null && photoTime.length() >= 15) {
                // 格式如 2026-04-24T14:30:00 或 20260424143000
                String timePart = photoTime.replace("-", "").replace(":", "").replace("T", "");
                timePart = timePart.substring(0, 14); // yyyyMMddHHmmss
                return timePart + "_" + typeId + ".jpg";
            }
        } catch (Exception e) {
            // 使用 UUID
        }
        return UUID.randomUUID().toString() + "_" + typeId + ".jpg";
    }

    /**
     * 根据 typeId 映射到实体字段名
     * 二级平台上传策略：
     * 1-通行凭证照片, 2-透视影像, 3-车身照, 4-证据链照片, 24-货物照
     * 11-车头照, 12-车尾照, 13-证件照, 99-车顶照
     */
    private String mapTypeIdToField(String typeId) {
        if (typeId == null) return null;
        return switch (typeId) {
            case "1" -> "passcodeImagePath";   // 通行凭证照片
            case "2" -> "transparentImagePath"; // 透视影像
            case "3" -> "bodyImagePath";      // 车身照
            case "4" -> "evidencesImagePath"; // 证据链照片
            case "11" -> "headImagePath";     // 车头照
            case "12" -> "tailImagePath";     // 车尾照
            case "13" -> "licenseImagePath";  // 证件照
            case "24" -> "goodsImagePath";   // 货物照
            case "99" -> "topImagePath";      // 车顶照
            default -> null;
        };
    }

    /**
     * 转换 DTO 为实体
     */
    private VehicleInspection convertToEntity(ThreeLevelCheckResultDto dto, Map<String, List<String>> imagePaths) {
        VehicleInspection record = new VehicleInspection();

        // 基础信息（从父类字段映射）
        // driverTelephone → driverPhone
        record.setDriverPhone(dto.getDriverTelephone());
        // 从 vehicleId 提取车牌号（格式：车牌_颜色）
        String plateNumber = dto.getVehicleId();
        if (plateNumber != null && plateNumber.contains("_")) {
            record.setPlateNumber(plateNumber.split("_")[0]);
            record.setPasscodeVehicleColorName(plateNumber.split("_")[1]);
        } else {
            record.setPlateNumber(plateNumber);
        }
        // freightTypes → goodsType
        record.setGoodsType(dto.getFreightTypes());

        // 三级平台新增字段
        record.setPlateNumberGc(dto.getPlateNumberGc());
        record.setGoodsCategory(dto.getGoodsCategory());
        record.setLoadWeight(dto.getLoadWeight() != null ? BigDecimal.valueOf(dto.getLoadWeight()) : null);

        // 车辆信息
        record.setVehicleType(dto.getVehicleType());
        record.setVehicleContainertype(dto.getCrateType());

        // 装载信息（父类已有）
        record.setLoadRate(dto.getLoadRate() != null ? BigDecimal.valueOf(dto.getLoadRate()) : null);
        record.setVehicleSize(dto.getVehicleSize());

        // 图片路径（支持多张，用逗号分隔）
        record.setPasscodeImagePath(joinPaths(imagePaths.get("passcodeImagePath")));
        record.setTransparentImagePath(joinPaths(imagePaths.get("transparentImagePath")));
        record.setBodyImagePath(joinPaths(imagePaths.get("bodyImagePath")));
        record.setGoodsImagePath(joinPaths(imagePaths.get("goodsImagePath")));
        record.setEvidencesImagePath(joinPaths(imagePaths.get("evidencesImagePath")));

        // 填充缺失字段为空（交通部上报不包含的字段）
        record.setPasscodeVehicleId(dto.getVehicleId());
        record.setPasscodeEnStationId(dto.getEnStationId());
        record.setPasscodeExStationId(dto.getExStationId());
        record.setPasscodeEnWeight(dto.getEnWeight() != null ? dto.getEnWeight().toString() : null);
        record.setPasscodeExWeight(dto.getExWeight() != null ? dto.getExWeight().toString() : null);
        record.setPasscodeMediaType(dto.getMediaType() != null ? dto.getMediaType().toString() : null);
        record.setPasscodeTransactionId(dto.getTransactionId());
        record.setPasscodePassId(dto.getPassId());
        record.setPasscodeExTime(dto.getExTime());
        record.setPasscodeTransPayType(dto.getTransPayType() != null ? dto.getTransPayType().toString() : null);
        record.setPasscodeFee(dto.getFee() != null ? dto.getFee().toString() : null);
        record.setPasscodePayFee(dto.getPayFee() != null ? dto.getPayFee().toString() : null);
        record.setPasscodeVehicleSign(dto.getVehicleSign());
        record.setPasscodeProvinceCount(dto.getProvinceCount() != null ? dto.getProvinceCount().toString() : null);

        // 操作员信息（父类字段）
        record.setOperatorName(dto.getInspector());

        // 业务信息
        record.setInspectionTime(parseDateTime(dto.getCheckTime()));
        record.setGroupId(dto.getGroupId() != null ? dto.getGroupId().toString() : null);

        // 查验结果
        record.setResultStatus(dto.getCheckResult());

        // 状态默认
        record.setStatus(0);
        record.setToTransportdeptState(0); // 未上传交通部

        return record;
    }

    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * 将图片路径列表转换为逗号分隔的字符串
     */
    private String joinPaths(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return null;
        }
        return String.join(",", paths);
    }

    /**
     * 上报到三级平台
     */
    private void uploadToThreeLevelPlatform(ThreeLevelCheckResultDto dto) {
        try {
            // 设置默认值（交通部要求）
            if (dto.getWeightCheckBasis() == null) {
                dto.setWeightCheckBasis("2");
            }
            if (dto.getMemo() == null) {
                dto.setMemo("");
            }
            if (dto.getOperation() == null) {
                dto.setOperation(1);
            }

            String url = props.getHttpsUrl() + UPLOAD_PATH;
            String jsonStr = com.alibaba.fastjson2.JSON.toJSONString(dto);

            String signContent = ModelSignTools.generateSignContent(jsonStr);
            log.info("三级平台上行待签名字符串: {}", signContent);

            HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
            try {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(300000);
                conn.setDoOutput(true);

                String auth = props.getClientId() + "_" + props.getClientKey();
                String nonce = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                String sign = buildSign(auth, nonce, signContent);

                conn.setRequestProperty("auth", props.getClientId());
                conn.setRequestProperty("nonce", nonce);
                conn.setRequestProperty("sign", sign);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonStr.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int code = conn.getResponseCode();
                log.info("三级平台 API 响应码: {}", code);

            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            log.warn("上报三级平台失败: {}", e.getMessage());
        }
    }

    /**
     * 构建签名
     */
    private String buildSign(String auth, String nonce, String signContent) throws Exception {
        String content = signContent + "&auth=" + auth + "&nonce=" + nonce;
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) hex.append(String.format("%02X", b & 0xff));
        return Base64.getEncoder().encodeToString(hex.toString().getBytes(StandardCharsets.UTF_8)).toUpperCase();
    }
}