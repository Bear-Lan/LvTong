package com.lvtong.LvTongTransportDept.converter;

import com.lvtong.LvTongTransportDept.dto.TransportDeptCheckResultDto;
import com.lvtong.LvTongTransportDept.dto.TransportDeptCheckResultDto.PhotoItem;
import com.lvtong.LvTongTransportDept.entity.VehicleInspection;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * VehicleInspection → TransportDeptCheckResultDto 转换器
 *
 * 【职责】
 * 仅负责将业务实体转换为交通局接口的 JSON 格式。
 * 不负责：HTTP 调用、文件读取、数据库操作。
 */
@Component
public class TransportDeptConverter {

    private static final DateTimeFormatter INSPECT_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ================================================================
    // 主转换入口
    // ================================================================

    /**
     * 将查验记录转换为交通局上报 DTO
     */
    public TransportDeptCheckResultDto toDto(VehicleInspection r) {
        TransportDeptCheckResultDto dto = new TransportDeptCheckResultDto();

        // checkId = 电话_yyyyMMddHHmmss_电话
        String phone = StringUtils.hasText(r.getDriverPhone()) ? r.getDriverPhone() : "";
        dto.setCheckId(phone + formatInspectTime(r.getInspectionTime()) + "_" + phone);

        // 根据车种设置 sign 和 class：绿通车=0x02/2，联合收割机=0x03/3
        boolean isHarvester = isHarvesterVehicle(r.getVehicleType());
        dto.setVehicleSign(isHarvester ? "0x03" : "0x02");
        dto.setVehicleClass(isHarvester ? 3 : 2);
        dto.setDriverTelephone(phone);
        dto.setVehicleId(buildVehicleId(r));
        dto.setFreightTypes(r.getGoodsType());
        dto.setVehicleType(r.getVehicleType());
        dto.setCrateType(getOrDefault(r.getVehicleContainertype(), "3.1"));
        dto.setWeightCheckBasis("2");
        dto.setExWeight(parseIntOr(r.getPasscodeExWeight(), 0));
        dto.setLoadRate(parseDoubleOr(r.getLoadRate(), 0.0));
        dto.setVehicleSize(r.getVehicleSize());
        dto.setCheckTime(r.getPasscodeExTime());
        dto.setEnStationId(r.getPasscodeEnStationId());
        dto.setExStationId(r.getPasscodeExStationId());
        dto.setGroupId(parseIntOr(r.getGroupId(), 1));
        dto.setInspector(getOrDefault(r.getInspectorPhone(), ""));
        dto.setReviewer(getOrDefault(r.getReviewerPhone(), ""));
        dto.setCheckResult(r.getResultStatus());
        dto.setReason(r.getResultStatus() != null && r.getResultStatus() == 2
                ? String.valueOf(r.getNopassType()) : "");
        dto.setMediaType(parseMediaType(r.getPasscodeMediaType()));
        dto.setTransactionId(r.getPasscodeTransactionId());
        dto.setPassId(r.getPasscodePassId());
        dto.setExTime(r.getPasscodeExTime());
        dto.setTransPayType(parseTransPayType(r.getPasscodeTransPayType()));
        dto.setFee(parseFeeCent(r.getPasscodeFee()));
        dto.setPayFee(parseFeeCent(r.getPasscodePayFee()));
        dto.setProvinceCount(parseIntOr(r.getPasscodeProvinceCount(), 1));
        dto.setMemo("");
        dto.setOperation(1);
        dto.setPhotos(buildPhotos(r));

        return dto;
    }

    // ================================================================
    // JSON 字段构建
    // ================================================================

    private String buildVehicleId(VehicleInspection r) {
        String plate = getOrDefault(r.getPlateNumber(), "");
        String color = getOrDefault(r.getPlateNumberGc(), "0");
        return plate + "_" + color;
    }

    private String formatInspectTime(LocalDateTime time) {
        if (time == null) return "";
        return time.format(INSPECT_TIME_FMT);
    }

    private int parseMediaType(String val) {
        if (val == null) return 9;
        switch (val.trim()) {
            case "1":    return 1;   // OBU
            case "2":    return 2;   // CPC卡
            case "3":    return 3;   // 纸券
            case "4":    return 4;   // M1卡
            case "0":    return 1;   // 0 也视为 OBU
            case "OBU":  return 1;
            case "CPU":  return 2;
            case "PAPER":return 3;
            default:     return 9;   // 无通行介质
        }
    }

    private int parseTransPayType(String val) {
        if (val == null) return 1;
        if (val.contains("刷卡")) return 2;
        return 1;
    }

    /**
     * 将元转换为分（乘以 100）
     */
    private long parseFeeCent(String fee) {
        if (!StringUtils.hasText(fee)) return 0L;
        try {
            return (long) (Double.parseDouble(fee) * 100);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private int parseIntOr(String val, int def) {
        if (!StringUtils.hasText(val)) return def;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private double parseDoubleOr(BigDecimal val, double def) {
        return val != null ? val.doubleValue() : def;
    }

    private String getOrDefault(String val, String def) {
        return StringUtils.hasText(val) ? val : def;
    }

    /**
     * 判断是否为联合收割机车辆
     * 绿通车 vehicleType 为 "11"~"16"（一型~六型货车）
     * 其余均视为联合收割机
     */
    private boolean isHarvesterVehicle(String vehicleType) {
        if (!StringUtils.hasText(vehicleType)) return false;
        try {
            int type = Integer.parseInt(vehicleType.trim());
            return type < 11 || type > 16;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    // ================================================================
    // 照片列表构建
    // ================================================================

    /**
     * 构建照片列表（仅记录文件路径，由 ImageEncoder 负责编码）
     *
     * 【说明】
     * 这里只记录每个图片的类型和路径，Base64 编码由 ImageEncoder 统一处理。
     * 这样可以分离"数据结构定义"和"文件 I/O 操作"的职责。
     */
    public List<PhotoItem> buildPhotos(VehicleInspection r) {
        List<PhotoItem> photos = new ArrayList<>();

        addPhoto(photos, r.getHeadImagePath(), "11");
        addPhoto(photos, r.getTailImagePath(), "12");
        addPhoto(photos, r.getLicenseImagePath(), "13");
        addPhoto(photos, r.getTopImagePath(), "99");

        // 货物照支持多个，中英文逗号均可分隔
        if (StringUtils.hasText(r.getGoodsImagePath())) {
            for (String path : r.getGoodsImagePath().split("[,，]")) {
                if (StringUtils.hasText(path)) {
                    addPhoto(photos, path.trim(), "24");
                }
            }
        }

        return photos;
    }

    private void addPhoto(List<PhotoItem> photos, String imagePath, String typeId) {
        if (!StringUtils.hasText(imagePath)) return;
        PhotoItem item = new PhotoItem();
        item.setTypeId(typeId);
        item.setContent(normalizePath(imagePath));
        item.setTime(extractPhotoTime(imagePath));
        photos.add(item);
    }

    /** 统一路径分隔符 */
    private String normalizePath(String path) {
        return path.replace("\\", "/");
    }

    /**
     * 从文件路径提取拍摄时间
     *
     * 路径格式：D:/images/captures/2026/01/25/body/20260125_105201_73786.jpg
     * → 转换为：2026-01-25T10:52:01
     */
    private String extractPhotoTime(String imagePath) {
        if (!StringUtils.hasText(imagePath)) return "";
        int lastSlash = imagePath.lastIndexOf('/');
        if (lastSlash < 0) return "";
        String name = imagePath.substring(lastSlash + 1, imagePath.lastIndexOf('.'));
        if (name.length() < 15) return name;
        return name.substring(0, 4) + "-" + name.substring(4, 6) + "-" + name.substring(6, 8)
                + "T" + name.substring(9, 11) + ":" + name.substring(11, 13) + ":" + name.substring(13, 15);
    }
}
