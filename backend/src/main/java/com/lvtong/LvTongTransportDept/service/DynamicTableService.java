package com.lvtong.LvTongTransportDept.service;

import com.lvtong.LvTongTransportDept.constant.VehicleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态表管理服务
 * 根据 station_name 动态创建和操作数据表
 */
@Service
public class DynamicTableService {

    private static final Logger log = LoggerFactory.getLogger(DynamicTableService.class);

    private static final DateTimeFormatter CHECK_ID_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter INSPECTION_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private DataSource dataSource;

    private static final String DATABASE = "three_platform";

    @Autowired
    private ProvinceCacheService provinceCacheService;

    /**
     * 缓存已创建的表，避免重复检查
     */
    private final ConcurrentHashMap<String, Boolean> createdTables = new ConcurrentHashMap<>();

    /**
     * 根据 stationCode 获取 stationName（表名）
     */
    public String getStationNameByCode(String stationCode) {
        if (stationCode == null || stationCode.isEmpty()) {
            return null;
        }
        return provinceCacheService.getStationNameByStationId(stationCode);
    }

    /**
     * 确保表存在，不存在则创建
     */
    public void ensureTableExists(String tableName) {
        // 快速路径：已确认创建的表直接返回
        if (createdTables.containsKey(tableName)) {
            return;
        }

        synchronized (tableName.intern()) {
            // 双重检查：加锁后再检查缓存
            if (createdTables.containsKey(tableName)) {
                return;
            }

            try {
                DataSource ds = getMasterDataSource();
                try (Connection conn = ds.getConnection()) {

                    // 检查表是否存在
                    String checkSql = "SELECT COUNT(*) FROM information_schema.tables " +
                                      "WHERE table_schema = ? AND table_name = ?";
                    try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                        ps.setString(1, DATABASE);
                        ps.setString(2, tableName);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                createdTables.put(tableName, true);
                                log.info("表 {} 已存在", tableName);
                                return;
                            }
                        }
                    }

                    // 创建表
                    String createTableSql = buildCreateTableSql(tableName);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(createTableSql);
                        createdTables.put(tableName, true);
                        log.info("表 {} 创建成功", tableName);
                    }
                }
            } catch (Exception e) {
                log.error("创建表 {} 失败: {}", tableName, e.getMessage());
                throw new RuntimeException("创建表失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 插入数据到指定表
     */
    public void insertRecord(String tableName, RecordData record) {
        // 参数校验
        String error = validateRecord(record);
        if (error != null) {
            throw new IllegalArgumentException("参数校验失败: " + error);
        }

        ensureTableExists(tableName);

        // 自动生成 check_id
        if (record.checkId == null || record.checkId.isEmpty()) {
            record.checkId = generateCheckId(record.driverPhone, record.inspectionTime);
        }

        // 检查是否重复上传
        if (isRecordExists(tableName, record.checkId)) {
            throw new IllegalArgumentException("重复上传: check_id=" + record.checkId + " 已存在");
        }

        try {
            DataSource ds = getMasterDataSource();
            try (Connection conn = ds.getConnection()) {
                String sql = buildInsertSql(tableName);
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    setRecordParameters(ps, record);
                    ps.executeUpdate();
                    log.info("数据插入表 {} 成功, plateNumber={}", tableName, record.plateNumber);
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("插入数据到表 {} 失败: {}", tableName, e.getMessage());
            throw new RuntimeException("插入数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查记录是否已存在（通过 check_id）
     */
    private boolean isRecordExists(String tableName, String checkId) {
        try {
            DataSource ds = getMasterDataSource();
            try (Connection conn = ds.getConnection()) {
                String sql = "SELECT COUNT(*) FROM `" + tableName + "` WHERE check_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, checkId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1) > 0;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("检查记录是否存在失败: {}", e.getMessage());
            throw new IllegalArgumentException("检查记录是否存在失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 校验记录数据
     * 返回错误信息，或null表示校验通过
     */
    private String validateRecord(RecordData record) {
        // 必填字段
        if (record.plateNumber == null || record.plateNumber.isEmpty()) {
            return "plateNumber（车牌号）必填";
        }
        if (record.passcodeExStationId == null || record.passcodeExStationId.isEmpty()) {
            return "passcodeExStationId（出口站ID）必填";
        }

        // resultStatus 取值范围：0=待查验, 1=合格, 2=不合格
        if (record.resultStatus != null) {
            if (record.resultStatus < 0 || record.resultStatus > 2) {
                return "resultStatus（查验结果）取值范围：0~2";
            }
        }

        // status 只能是0
        if (record.status != null && record.status != 0) {
            return "status（状态）只能是0";
        }

        // loadRate 取值范围：0.00 ~ 100.00
        if (record.loadRate != null) {
            if (record.loadRate.compareTo(BigDecimal.ZERO) < 0 || record.loadRate.compareTo(new BigDecimal("100.00")) > 0) {
                return "loadRate（满载率）取值范围：0.00 ~ 100.00";
            }
        }

        // loadWeight 必须大于零
        if (record.loadWeight != null && record.loadWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return "loadWeight（载重）必须大于零";
        }

        // nopassType 仅在 resultStatus=2（不合格）时需要检查
        if (record.nopassType != null && record.resultStatus != null && record.resultStatus == 2) {
            String text = VehicleConstants.getNopassTypeText(record.nopassType);
            if ("-".equals(text) || text.matches("\\d+")) {
                return "nopassType（不合格类型）值无效: " + record.nopassType;
            }
        }

        // 时间格式校验（yyyy-MM-ddTHH:mm:ss）
        if (!isValidDateTime(record.passcodeExTime)) {
            return "passcodeExTime（出口时间）格式错误，应为 yyyy-MM-ddTHH:mm:ss";
        }
        if (!isValidDateTime(record.btnPrebookTime)) {
            return "btnPrebookTime（预约时间）格式错误，应为 yyyy-MM-ddTHH:mm:ss";
        }

        return null;
    }

    /**
     * 校验时间格式（yyyy-MM-ddTHH:mm:ss）
     */
    private boolean isValidDateTime(String str) {
        if (str == null || str.isEmpty()) {
            return true; // 空字符串通过校验（非必填字段）
        }
        try {
            // 格式：2026-05-06T14:40:29
            LocalDateTime.parse(str.replace(" ", "T").substring(0, Math.min(str.length(), 19)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成 check_id
     * 格式: 手机号 + yyyyMMddHHmmss + "_" + 手机号
     */
    private String generateCheckId(String phone, String inspectionTime) {
        if (phone == null || phone.isEmpty()) {
            throw new IllegalArgumentException("生成check_id失败: phone（手机号）不能为空");
        }
        String timeStr;
        try {
            if (inspectionTime != null && !inspectionTime.isEmpty()) {
                LocalDateTime dt = LocalDateTime.parse(inspectionTime, INSPECTION_TIME_FORMAT);
                timeStr = dt.format(CHECK_ID_TIME_FORMAT);
            } else {
                throw new IllegalArgumentException("生成check_id失败: inspectionTime（查验时间）不能为空");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("生成check_id失败: inspectionTime格式错误，应为yyyy-MM-dd HH:mm:ss，实际值: " + inspectionTime);
        }
        return phone + timeStr + "_" + phone;
    }

    /**
     * 获取主数据源
     */
    private DataSource getMasterDataSource() {
        return dataSource;
    }

    /**
     * 构建建表 SQL
     */
    private String buildCreateTableSql(String tableName) {
        return """
            CREATE TABLE IF NOT EXISTS `%s` (
              `check_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '查验人_司机手机号',
              `id` int NOT NULL AUTO_INCREMENT,
              `plate_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '车牌号',
              `plate_number_gc` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '挂车号码',
              `driver_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '司机电话',
              `vehicle_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车型',
              `vehicle_containertype` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车厢类型',
              `goods_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '货物类型',
              `goods_category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '货物分类',
              `load_rate` decimal(5,2) NULL DEFAULT 0.00 COMMENT '满载率',
              `load_weight` decimal(8,2) NULL DEFAULT NULL COMMENT '载重率',
              `vehicle_size` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '长宽高',
              `history_record` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '历史记录',
              `body_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车身影像',
              `transparent_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '透视影像',
              `head_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车头照片',
              `tail_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车尾照片',
              `top_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车顶照片',
              `goods_image_path` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '货物照片',
              `evidences_image_path` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '证据照片',
              `license_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '行驶证照片',
              `passcode_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通行证照片',
              `passcode_vehicle_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通行码车辆ID',
              `passcode_vehicle_display_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通行码车辆显示ID',
              `passcode_vehicle_color_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通行码车牌颜色',
              `passcode_en_station_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '入口站ID',
              `passcode_ex_station_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '出口站ID',
              `passcode_en_weight` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '入口重量',
              `passcode_ex_weight` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '出口重量',
              `passcode_media_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '介质类型',
              `passcode_transaction_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '交易ID',
              `passcode_pass_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通行证ID',
              `passcode_ex_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '出口时间',
              `passcode_trans_pay_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '交易支付类型',
              `passcode_fee` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '费用',
              `passcode_pay_fee` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '支付费用',
              `passcode_vehicle_sign` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车辆标志',
              `passcode_province_count` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '省份数量',
              `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作员姓名',
              `btn_prebook_time` datetime NULL DEFAULT NULL COMMENT '司机按键预约',
              `acceptance_time` datetime NULL DEFAULT NULL COMMENT '受理时间',
              `opengate_time` datetime NULL DEFAULT NULL COMMENT '抬杆放行时间',
              `openlightscreen_time` datetime NULL DEFAULT NULL COMMENT '光幕打开时间',
              `closelightscreen_time` datetime NULL DEFAULT NULL COMMENT '光幕关闭时间',
              `cd_photo_time` datetime NULL DEFAULT NULL COMMENT '检测时间',
              `inspection_time` datetime NULL DEFAULT NULL COMMENT '查验时间',
              `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              `result_status` int NULL DEFAULT NULL COMMENT '查验结果',
              `nopass_type` int NULL DEFAULT NULL COMMENT '不合格类型',
              `status` int NULL DEFAULT NULL COMMENT '状态',
              `group_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '班组编号',
              `inspector_phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '验货人手机号',
              `reviewer_phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '复核人手机号',
              `manual_review_state` int NULL DEFAULT 0 COMMENT '人工审核状态',
              `to_transportdept_state` int NULL DEFAULT 0 COMMENT '上传状态',
              `to_transportdept_time` datetime NULL DEFAULT NULL COMMENT '上传时间',
              `to_transportdept_comment` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '上传结果',
              PRIMARY KEY (`id`, `check_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='%s查验记录'
            """.formatted(tableName, tableName);
    }

    /**
     * 构建插入 SQL（55个字段，不含自增id）
     */
    private String buildInsertSql(String tableName) {
        return """
            INSERT INTO `%s` (
              check_id, plate_number, plate_number_gc, driver_phone, vehicle_type,
              vehicle_containertype, goods_type, goods_category, load_rate, load_weight,
              vehicle_size, history_record, body_image_path, transparent_image_path,
              head_image_path, tail_image_path, top_image_path, goods_image_path,
              evidences_image_path, license_image_path, passcode_image_path,
              passcode_vehicle_id, passcode_vehicle_display_id, passcode_vehicle_color_name,
              passcode_en_station_id, passcode_ex_station_id, passcode_en_weight,
              passcode_ex_weight, passcode_media_type, passcode_transaction_id,
              passcode_pass_id, passcode_ex_time, passcode_trans_pay_type, passcode_fee,
              passcode_pay_fee, passcode_vehicle_sign, passcode_province_count,
              operator_name, btn_prebook_time, acceptance_time, opengate_time,
              openlightscreen_time, closelightscreen_time, cd_photo_time, inspection_time,
              result_status, nopass_type, status, group_id, inspector_phone,
              reviewer_phone, manual_review_state, to_transportdept_state,
              to_transportdept_time, to_transportdept_comment
            ) VALUES (
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            """.formatted(tableName);
    }

    /**
     * 设置记录参数（55个）
     */
    private void setRecordParameters(PreparedStatement ps, RecordData record) throws Exception {
        int idx = 1;

        ps.setString(idx++, record.checkId);
        ps.setString(idx++, record.plateNumber);
        ps.setString(idx++, record.plateNumberGc);
        ps.setString(idx++, record.driverPhone);
        ps.setString(idx++, record.vehicleType);
        ps.setString(idx++, record.vehicleContainertype);
        ps.setString(idx++, record.goodsType);
        ps.setString(idx++, record.goodsCategory);
        ps.setBigDecimal(idx++, record.loadRate);
        ps.setBigDecimal(idx++, record.loadWeight);
        ps.setString(idx++, record.vehicleSize);
        ps.setString(idx++, record.historyRecord);
        ps.setString(idx++, record.bodyImagePath);
        ps.setString(idx++, record.transparentImagePath);
        ps.setString(idx++, record.headImagePath);
        ps.setString(idx++, record.tailImagePath);
        ps.setString(idx++, record.topImagePath);
        ps.setString(idx++, record.goodsImagePath);
        ps.setString(idx++, record.evidencesImagePath);
        ps.setString(idx++, record.licenseImagePath);
        ps.setString(idx++, record.passcodeImagePath);
        ps.setString(idx++, record.passcodeVehicleId);
        ps.setString(idx++, record.passcodeVehicleDisplayId);
        ps.setString(idx++, record.passcodeVehicleColorName);
        ps.setString(idx++, record.passcodeEnStationId);
        ps.setString(idx++, record.passcodeExStationId);
        ps.setString(idx++, record.passcodeEnWeight);
        ps.setString(idx++, record.passcodeExWeight);
        ps.setString(idx++, record.passcodeMediaType);
        ps.setString(idx++, record.passcodeTransactionId);
        ps.setString(idx++, record.passcodePassId);
        ps.setString(idx++, record.passcodeExTime);
        ps.setString(idx++, record.passcodeTransPayType);
        ps.setString(idx++, record.passcodeFee);
        ps.setString(idx++, record.passcodePayFee);
        ps.setString(idx++, record.passcodeVehicleSign);
        ps.setString(idx++, record.passcodeProvinceCount);
        ps.setString(idx++, record.operatorName);
        ps.setString(idx++, record.btnPrebookTime);
        ps.setString(idx++, record.acceptanceTime);
        ps.setString(idx++, record.opengateTime);
        ps.setString(idx++, record.openlightscreenTime);
        ps.setString(idx++, record.closelightscreenTime);
        ps.setString(idx++, record.cdPhotoTime);
        ps.setString(idx++, record.inspectionTime);
        ps.setObject(idx++, record.resultStatus);
        ps.setObject(idx++, record.nopassType);
        ps.setObject(idx++, record.status);
        ps.setString(idx++, record.groupId);
        ps.setString(idx++, record.inspectorPhone);
        ps.setString(idx++, record.reviewerPhone);
        ps.setObject(idx++, record.manualReviewState);
        ps.setObject(idx++, record.toTransportdeptState);
        ps.setString(idx++, record.toTransportdeptTime);
        ps.setString(idx++, record.toTransportdeptComment);
    }

    /**
     * 记录数据类
     */
    public static class RecordData {
        public String checkId;
        public String plateNumber;
        public String plateNumberGc;
        public String driverPhone;
        public String vehicleType;
        public String vehicleContainertype;
        public String goodsType;
        public String goodsCategory;
        public java.math.BigDecimal loadRate;
        public java.math.BigDecimal loadWeight;
        public String vehicleSize;
        public String historyRecord;
        public String bodyImagePath;
        public String transparentImagePath;
        public String headImagePath;
        public String tailImagePath;
        public String topImagePath;
        public String goodsImagePath;
        public String evidencesImagePath;
        public String licenseImagePath;
        public String passcodeImagePath;
        public String passcodeVehicleId;
        public String passcodeVehicleDisplayId;
        public String passcodeVehicleColorName;
        public String passcodeEnStationId;
        public String passcodeExStationId;
        public String passcodeEnWeight;
        public String passcodeExWeight;
        public String passcodeMediaType;
        public String passcodeTransactionId;
        public String passcodePassId;
        public String passcodeExTime;
        public String passcodeTransPayType;
        public String passcodeFee;
        public String passcodePayFee;
        public String passcodeVehicleSign;
        public String passcodeProvinceCount;
        public String operatorName;
        public String btnPrebookTime;
        public String acceptanceTime;
        public String opengateTime;
        public String openlightscreenTime;
        public String closelightscreenTime;
        public String cdPhotoTime;
        public String inspectionTime;
        public Integer resultStatus;
        public Integer nopassType;
        public Integer status;
        public String groupId;
        public String inspectorPhone;
        public String reviewerPhone;
        public Integer manualReviewState;
        public Integer toTransportdeptState;
        public String toTransportdeptTime;
        public String toTransportdeptComment;
    }
}