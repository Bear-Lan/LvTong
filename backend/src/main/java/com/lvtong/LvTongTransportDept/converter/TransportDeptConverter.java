package com.lvtong.LvTongTransportDept.converter;

import com.lvtong.LvTongTransportDept.dto.TransportDeptCheckResultDto;
import com.lvtong.LvTongTransportDept.dto.TransportDeptCheckResultDto.PhotoItem;
import com.lvtong.LvTongTransportDept.entity.VehicleInspection;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        return toDto(r, null, new ArrayList<>());
    }

    /**
     * 将查验记录转换为交通局上报 DTO（支持排除指定图片）
     *
     * @param r 查验记录
     * @param excludePhotoTypes 要排除的图片类型ID列表，如 [11,12,13]
     * @param errors 图片解析错误收集列表
     */
    public TransportDeptCheckResultDto toDto(VehicleInspection r, List<String> excludePhotoTypes, List<String> errors) {
        TransportDeptCheckResultDto dto = new TransportDeptCheckResultDto();

        // checkId = 电话yyyyMMddHHmmss_电话
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
        dto.setEnWeight(parseIntOr(r.getPasscodeEnWeight(),0));
        dto.setExWeight(parseIntOr(r.getPasscodeExWeight(), 0));
        dto.setVehicleSize(r.getVehicleSize());
        dto.setLoadRate(parseDoubleOr(r.getLoadRate(), 0.0));
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
        dto.setPhotos(buildPhotos(r, excludePhotoTypes, errors));

        return dto;
    }

    // ================================================================
    // JSON 字段构建
    // ================================================================

    private String buildVehicleId(VehicleInspection r) {
        String plate = getOrDefault(r.getPlateNumber(), "");
        String color = getOrDefault(r.getPasscodeVehicleColorName(), "0");
        return plate + "_" + color;
    }

    private String formatInspectTime(LocalDateTime time) {
        if (time == null) return "";
        return time.format(INSPECT_TIME_FMT);
    }

    private int parseMediaType(String val) {
        if (val == null) return 9;
        return switch (val.trim()) {
            case "1" -> 1;   // OBU
            case "2" -> 2;   // CPC卡
            case "3" -> 3;   // 纸券
            case "4" -> 4;   // M1卡
            case "0" -> 1;   // 0 也视为 OBU
            case "OBU" -> 1;
            case "CPU" -> 2;
            case "PAPER" -> 3;
            case "M1卡" -> 4;
            default -> 9;   // 无通行介质
        };
    }

    private int parseTransPayType(String val) {
        if (val == null) return 1;
        if (val.contains("刷卡")||val.contains("2")) return 2;
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
        return buildPhotos(r, null, new ArrayList<>());
    }

    /**
     * 构建照片列表（支持排除指定图片类型）
     *
     * @param r 查验记录
     * @param excludeList 排除的图片标识列表：
     *                    证据链照片用typeId如11/12/13/99，
     *                    11-车头，12-车尾，13-行驶证，24-货物照（通行凭证、透视影像、车身照、货物照），99-车顶照
     *                    货物照用goods_0/goods_1等下标格式
     */
    public List<PhotoItem> buildPhotos(VehicleInspection r, List<String> excludeList, List<String> errors) {
        List<PhotoItem> photos = new ArrayList<>();

        // 创建排除集合，便于快速查找
        Set<String> excludeSet = excludeList != null
                ? new HashSet<>(excludeList)
                : new HashSet<>();

        // 只有不在排除列表中时才添加
        //11-车头
        if (!excludeSet.contains("11")) {
            addPhoto(photos, r.getHeadImagePath(), "11", errors);
        }
        //12-车尾
        if (!excludeSet.contains("12")) {
            addPhoto(photos, r.getTailImagePath(), "12", errors);
        }
        //13-行驶证
        if (!excludeSet.contains("13")) {
            addPhoto(photos, r.getLicenseImagePath(), "13", errors);
        }
        //99-车顶照
        if (!excludeSet.contains("99")) {
            addPhoto(photos, r.getTopImagePath(), "99", errors);
        }
        //24-货物照，货物照支持多个，中英文逗号均可分隔，（通行凭证、透视影像、车身照、货物照）
        //通行证
        if(!excludeSet.contains("passcodeImagePath")){
            addPhoto(photos,r.getPasscodeImagePath(),"24", errors);
        }
        //透视影像
        if(!excludeSet.contains("transparentImagePath")){
            addPhoto(photos,r.getTransparentImagePath(),"24", errors);
        }
        //车身照
        if(!excludeSet.contains("bodyImagePath")){
            addPhoto(photos,r.getBodyImagePath(),"24", errors);
        }
        //货物照
        if (StringUtils.hasText(r.getGoodsImagePath())) {
            int goodsIdx = 0;
            for (String path : r.getGoodsImagePath().split("[,，]")) {
                if (StringUtils.hasText(path)) {
                    // 检查该货物照是否被排除（下标格式：goods_0, goods_1, ...）
                    String excludeKey = "goods_" + goodsIdx;
                    if (!excludeSet.contains(excludeKey)) {
                        addPhoto(photos, path.trim(), "24", errors);
                    }
                    goodsIdx++;
                }
            }
        }
        //证据链照片（多张）
        if (StringUtils.hasText(r.getEvidencesImagePath())) {
            int eviIdx = 0;
            for (String path : r.getEvidencesImagePath().split("[,，]")) {
                if (StringUtils.hasText(path)) {
                    // 检查该证据链照片是否被排除（下标格式：evidences_0, evidences_1, ...）
                    String excludeKey = "evidences_" + eviIdx;
                    if (!excludeSet.contains(excludeKey)) {
                        addPhoto(photos, path.trim(), "24", errors);
                    }
                    eviIdx++;
                }
            }
        }

        return photos;
    }

    private void addPhoto(List<PhotoItem> photos, String imagePath, String typeId, List<String> errors) {
        if (!StringUtils.hasText(imagePath)) return;

        // 解析拍摄时间，失败则记录错误并舍弃该图片
        String[] timeResult = extractPhotoTime(imagePath);
        if (timeResult[1] != null) {
            errors.add("图片 [" + imagePath + "] " + timeResult[1]);
            return;
        }

        PhotoItem item = new PhotoItem();
        item.setTypeId(typeId);
        item.setContent(normalizePath(imagePath));
        item.setTime(timeResult[0]);
        photos.add(item);
    }

    /** 统一路径分隔符 */
    private String normalizePath(String path) {
        return path.replace("\\", "/");
    }

    /**
     * 从文件路径提取拍摄时间
     * 路径格式：D:/images/captures/2026/01/25/body/20260125_105201_73786.jpg
     * → 转换为：2026-01-25T10:52:01
     * @return [解析后的时间, 错误信息(有错误时)]
     */
    private String[] extractPhotoTime(String imagePath) {
        if (!StringUtils.hasText(imagePath)) return new String[]{"", "路径为空"};
        imagePath = normalizePath(imagePath);
        int lastSlash = imagePath.lastIndexOf('/');
        if (lastSlash < 0) return new String[]{"", "路径格式异常"};
        String name = imagePath.substring(lastSlash + 1, imagePath.lastIndexOf('.'));
        if (name.length() < 15) return new String[]{"", "文件名长度不足"};

        try {
            // 验证格式：前8位日期 + "_" + 后6位时间
            String datePart = name.substring(0, 8);
            String timePart = name.substring(9, 15);
            Integer.parseInt(datePart);
            Integer.parseInt(timePart);

            String result = name.substring(0, 4) + "-" + name.substring(4, 6) + "-" + name.substring(6, 8)
                    + "T" + name.substring(9, 11) + ":" + name.substring(11, 13) + ":" + name.substring(13, 15);
            return new String[]{result, null};
        } catch (Exception e) {
            return new String[]{"", "文件名格式异常无法解析时间"};
        }
    }
}
