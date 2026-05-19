package com.lvtong.LvTongTransportDept.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lvtong.LvTongTransportDept.entity.StationInfo;
import com.lvtong.LvTongTransportDept.mapper.StationInfoMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态表管理服务
 * 根据 station_name 动态创建和操作数据表
 */
@Service
public class DynamicTableService {

    private static final Logger log = LoggerFactory.getLogger(DynamicTableService.class);

    @Value("${spring.datasource.host:localhost}")
    private String host;

    @Value("${spring.datasource.port:3306}")
    private int port;

    @Value("${spring.datasource.database:lvtong}")
    private String database;

    @Value("${spring.datasource.username:root}")
    private String username;

    @Value("${spring.datasource.password:123456}")
    private String password;

    @Autowired
    private StationInfoMapper stationInfoMapper;

    /**
     * 缓存已创建的表，避免重复检查
     */
    private final ConcurrentHashMap<String, Boolean> createdTables = new ConcurrentHashMap<>();

    /**
     * 根据 stationCode 获取 stationName（表名）
     */
    public String getStationNameByCode(String stationCode) {
        LambdaQueryWrapper<StationInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StationInfo::getStationId, stationCode);
        StationInfo station = stationInfoMapper.selectOne(wrapper);

        if (station == null) {
            log.warn("未找到站点信息 stationCode={}", stationCode);
            return null;
        }

        return station.getStationName();
    }

    /**
     * 确保表存在，不存在则创建
     */
    public void ensureTableExists(String tableName) {
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
                    ps.setString(1, database);
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

    /**
     * 插入数据到指定表
     */
    public void insertRecord(String tableName, RecordData record) {
        ensureTableExists(tableName);

        try {
            DataSource ds = getMasterDataSource();
            try (Connection conn = ds.getConnection()) {
                String sql = buildInsertSql(tableName, record);
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    setRecordParameters(ps, record);
                    ps.executeUpdate();
                    log.debug("数据插入表 {} 成功", tableName);
                }
            }
        } catch (Exception e) {
            log.error("插入数据到表 {} 失败: {}", tableName, e.getMessage());
            throw new RuntimeException("插入数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取主数据源
     */
    private DataSource getMasterDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
                host, port, database));
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setPoolName("dynamic-table-pool");
        ds.setMaximumPoolSize(5);
        ds.setMinimumIdle(1);
        return ds;
    }

    /**
     * 构建建表 SQL
     */
    private String buildCreateTableSql(String tableName) {
        // 清理表名
        String cleanTableName = tableName.replaceAll("[^a-zA-Z0-9_]", "_");

        return """
            CREATE TABLE IF NOT EXISTS `%s` (
              `id` int NOT NULL AUTO_INCREMENT,
              `check_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '查验人_司机手机号',
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
            """.formatted(cleanTableName, cleanTableName);
    }

    /**
     * 构建插入 SQL
     */
    private String buildInsertSql(String tableName, RecordData record) {
        String cleanTableName = tableName.replaceAll("[^a-zA-Z0-9_]", "_");

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
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            """.formatted(cleanTableName);
    }

    /**
     * 设置记录参数
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
        ps.setTimestamp(idx++, record.btnPrebookTime);
        ps.setTimestamp(idx++, record.acceptanceTime);
        ps.setTimestamp(idx++, record.opengateTime);
        ps.setTimestamp(idx++, record.openlightscreenTime);
        ps.setTimestamp(idx++, record.closelightscreenTime);
        ps.setTimestamp(idx++, record.cdPhotoTime);
        ps.setTimestamp(idx++, record.inspectionTime);
        ps.setInt(idx++, record.resultStatus);
        ps.setInt(idx++, record.nopassType);
        ps.setInt(idx++, record.status);
        ps.setString(idx++, record.groupId);
        ps.setString(idx++, record.inspectorPhone);
        ps.setString(idx++, record.reviewerPhone);
        ps.setInt(idx++, record.manualReviewState);
        ps.setInt(idx++, record.toTransportdeptState);
        ps.setTimestamp(idx++, record.toTransportdeptTime);
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
        public java.sql.Timestamp btnPrebookTime;
        public java.sql.Timestamp acceptanceTime;
        public java.sql.Timestamp opengateTime;
        public java.sql.Timestamp openlightscreenTime;
        public java.sql.Timestamp closelightscreenTime;
        public java.sql.Timestamp cdPhotoTime;
        public java.sql.Timestamp inspectionTime;
        public Integer resultStatus;
        public Integer nopassType;
        public Integer status;
        public String groupId;
        public String inspectorPhone;
        public String reviewerPhone;
        public Integer manualReviewState;
        public Integer toTransportdeptState;
        public java.sql.Timestamp toTransportdeptTime;
        public String toTransportdeptComment;
    }
}