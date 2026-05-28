package com.lvtong.LvTongTransportDept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lvtong.LvTongTransportDept.entity.VehicleInspection;
import com.lvtong.LvTongTransportDept.exception.BusinessException;
import com.lvtong.LvTongTransportDept.mapper.VehicleInspectionMapper;
import com.lvtong.LvTongTransportDept.service.DynamicTableService;
import com.lvtong.LvTongTransportDept.service.ProvinceCacheService;
import com.lvtong.LvTongTransportDept.service.VehicleInspectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 车辆查验记录业务逻辑实现
 */
@Service
public class VehicleInspectionServiceImpl implements VehicleInspectionService {

    @Autowired
    private VehicleInspectionMapper mapper;

    @Autowired
    private ProvinceCacheService provinceCacheService;

    @Autowired
    private DynamicTableService dynamicTableService;

    @Override
    @Transactional(readOnly = true)
    public VehicleInspection getById(Integer id) {
        // 扫描所有站点表查找记录
        for (String stationId : dynamicTableService.getAllStationIds()) {
            String tableName = dynamicTableService.getTableNameByStationId(stationId);
            Map<String, Object> row = dynamicTableService.selectByIdOnTable(tableName, id);
            if (row != null && !row.isEmpty()) {
                return mapToVehicleInspection(row);
            }
        }
        return null;
    }

    @Override
    @Transactional
    public VehicleInspection create(VehicleInspection inspection) {
        // 根据 passcodeExStationId 确定目标表
        String stationId = inspection.getPasscodeExStationId();
        if (stationId == null || stationId.isEmpty()) {
            throw new BusinessException("出口站ID不能为空");
        }
        String tableName = dynamicTableService.getTableNameByStationId(stationId);
        DynamicTableService.RecordData record = convertToRecordData(inspection);
        dynamicTableService.insertRecord(tableName, record);
        // 查询刚插入的记录返回
        Map<String, Object> row = dynamicTableService.selectByIdOnTable(tableName, Integer.parseInt(record.checkId));
        return mapToVehicleInspection(row);
    }

    private DynamicTableService.RecordData convertToRecordData(VehicleInspection inspection) {
        DynamicTableService.RecordData record = new DynamicTableService.RecordData();
        record.checkId = inspection.getId() != null ? String.valueOf(inspection.getId()) : null;
        record.plateNumber = inspection.getPlateNumber();
        record.plateNumberGc = inspection.getPlateNumberGc();
        record.driverPhone = inspection.getDriverPhone();
        record.vehicleType = inspection.getVehicleType();
        record.vehicleContainertype = inspection.getVehicleContainertype();
        record.goodsType = inspection.getGoodsType();
        record.goodsCategory = inspection.getGoodsCategory();
        record.loadRate = inspection.getLoadRate();
        record.loadWeight = inspection.getLoadWeight();
        record.vehicleSize = inspection.getVehicleSize();
        record.historyRecord = inspection.getHistoryRecord();
        record.bodyImagePath = inspection.getBodyImagePath();
        record.transparentImagePath = inspection.getTransparentImagePath();
        record.headImagePath = inspection.getHeadImagePath();
        record.tailImagePath = inspection.getTailImagePath();
        record.topImagePath = inspection.getTopImagePath();
        // goodsImagePath 和 evidencesImagePath 需要特殊处理，暂时传 null
        record.licenseImagePath = inspection.getLicenseImagePath();
        record.passcodeImagePath = inspection.getPasscodeImagePath();
        record.passcodeVehicleId = inspection.getPasscodeVehicleId();
        record.passcodeVehicleDisplayId = inspection.getPasscodeVehicleDisplayId();
        record.passcodeVehicleColorName = inspection.getPasscodeVehicleColorName();
        record.passcodeEnStationId = inspection.getPasscodeEnStationId();
        record.passcodeExStationId = inspection.getPasscodeExStationId();
        record.passcodeEnWeight = inspection.getPasscodeEnWeight();
        record.passcodeExWeight = inspection.getPasscodeExWeight();
        record.passcodeMediaType = inspection.getPasscodeMediaType();
        record.passcodeTransactionId = inspection.getPasscodeTransactionId();
        record.passcodePassId = inspection.getPasscodePassId();
        record.passcodeExTime = inspection.getPasscodeExTime();
        record.passcodeTransPayType = inspection.getPasscodeTransPayType();
        record.passcodeFee = inspection.getPasscodeFee();
        record.passcodePayFee = inspection.getPasscodePayFee();
        record.passcodeVehicleSign = inspection.getPasscodeVehicleSign();
        record.passcodeProvinceCount = inspection.getPasscodeProvinceCount();
        record.operatorName = inspection.getOperatorName();
        record.btnPrebookTime = inspection.getBtnPrebookTime() != null ? inspection.getBtnPrebookTime().toString() : null;
        record.acceptanceTime = inspection.getAcceptanceTime() != null ? inspection.getAcceptanceTime().toString() : null;
        record.opengateTime = inspection.getOpengateTime() != null ? inspection.getOpengateTime().toString() : null;
        record.openlightscreenTime = inspection.getOpenlightscreenTime() != null ? inspection.getOpenlightscreenTime().toString() : null;
        record.closelightscreenTime = inspection.getCloselightscreenTime() != null ? inspection.getCloselightscreenTime().toString() : null;
        record.cdPhotoTime = inspection.getCdPhotoTime() != null ? inspection.getCdPhotoTime().toString() : null;
        record.inspectionTime = inspection.getInspectionTime() != null ? inspection.getInspectionTime().toString() : null;
        record.resultStatus = inspection.getResultStatus();
        record.nopassType = inspection.getNopassType();
        record.status = inspection.getStatus();
        record.groupId = inspection.getGroupId();
        record.inspectorPhone = inspection.getInspectorPhone();
        record.reviewerPhone = inspection.getReviewerPhone();
        record.manualReviewState = inspection.getManualReviewState();
        record.toTransportdeptState = inspection.getToTransportdeptState();
        record.toTransportdeptTime = inspection.getToTransportdeptTime() != null ? inspection.getToTransportdeptTime().toString() : null;
        record.toTransportdeptComment = inspection.getToTransportdeptComment();
        return record;
    }

    @Override
    @Transactional
    public VehicleInspection update(Integer id, VehicleInspection inspection) {
        // 根据 passcodeExStationId 确定目标表
        String stationId = inspection.getPasscodeExStationId();
        if (stationId == null || stationId.isEmpty()) {
            throw new BusinessException("出口站ID不能为空");
        }
        String tableName = dynamicTableService.getTableNameByStationId(stationId);

        // 构建更新字段
        Map<String, Object> fields = new HashMap<>();
        fields.put("plate_number", inspection.getPlateNumber());
        fields.put("plate_number_gc", inspection.getPlateNumberGc());
        fields.put("driver_phone", inspection.getDriverPhone());
        fields.put("vehicle_type", inspection.getVehicleType());
        fields.put("vehicle_containertype", inspection.getVehicleContainertype());
        fields.put("goods_type", inspection.getGoodsType());
        fields.put("goods_category", inspection.getGoodsCategory());
        fields.put("load_rate", inspection.getLoadRate());
        fields.put("load_weight", inspection.getLoadWeight());
        fields.put("vehicle_size", inspection.getVehicleSize());
        fields.put("history_record", inspection.getHistoryRecord());
        fields.put("body_image_path", inspection.getBodyImagePath());
        fields.put("transparent_image_path", inspection.getTransparentImagePath());
        fields.put("head_image_path", inspection.getHeadImagePath());
        fields.put("tail_image_path", inspection.getTailImagePath());
        fields.put("top_image_path", inspection.getTopImagePath());
        fields.put("goods_image_path", inspection.getGoodsImagePath());
        fields.put("license_image_path", inspection.getLicenseImagePath());
        fields.put("passcode_image_path", inspection.getPasscodeImagePath());
        fields.put("passcode_vehicle_id", inspection.getPasscodeVehicleId());
        fields.put("passcode_vehicle_display_id", inspection.getPasscodeVehicleDisplayId());
        fields.put("passcode_vehicle_color_name", inspection.getPasscodeVehicleColorName());
        fields.put("passcode_en_station_id", inspection.getPasscodeEnStationId());
        fields.put("passcode_ex_station_id", inspection.getPasscodeExStationId());
        fields.put("passcode_en_weight", inspection.getPasscodeEnWeight());
        fields.put("passcode_ex_weight", inspection.getPasscodeExWeight());
        fields.put("passcode_media_type", inspection.getPasscodeMediaType());
        fields.put("passcode_transaction_id", inspection.getPasscodeTransactionId());
        fields.put("passcode_pass_id", inspection.getPasscodePassId());
        fields.put("passcode_ex_time", inspection.getPasscodeExTime());
        fields.put("passcode_trans_pay_type", inspection.getPasscodeTransPayType());
        fields.put("passcode_fee", inspection.getPasscodeFee());
        fields.put("passcode_pay_fee", inspection.getPasscodePayFee());
        fields.put("passcode_vehicle_sign", inspection.getPasscodeVehicleSign());
        fields.put("passcode_province_count", inspection.getPasscodeProvinceCount());
        fields.put("operator_name", inspection.getOperatorName());
        fields.put("result_status", inspection.getResultStatus());
        fields.put("nopass_type", inspection.getNopassType());
        fields.put("status", inspection.getStatus());
        fields.put("group_id", inspection.getGroupId());
        fields.put("inspector_phone", inspection.getInspectorPhone());
        fields.put("reviewer_phone", inspection.getReviewerPhone());
        fields.put("manual_review_state", inspection.getManualReviewState());
        fields.put("to_transportdept_state", inspection.getToTransportdeptState());
        fields.put("to_transportdept_time", inspection.getToTransportdeptTime());
        fields.put("to_transportdept_comment", inspection.getToTransportdeptComment());
        fields.put("updated_time", LocalDateTime.now());

        // 移除所有 null 值
        fields.values().removeIf(Objects::isNull);

        dynamicTableService.updateOnTable(tableName, id, fields);
        Map<String, Object> row = dynamicTableService.selectByIdOnTable(tableName, id);
        return row != null ? mapToVehicleInspection(row) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<VehicleInspection> searchWithConditions(
            String plateNumber,
            String driverPhone,
            String operatorName,
            String reviewerPhone,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer resultStatus,
            Integer manualReviewState,
            Integer toTransportdeptState,
            String goodsType,
            int page,
            int pageSize) {

        LambdaQueryWrapper<VehicleInspection> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(plateNumber)) {
            wrapper.like(VehicleInspection::getPlateNumber, plateNumber);
        }
        if (StringUtils.hasText(driverPhone)) {
            wrapper.like(VehicleInspection::getDriverPhone, driverPhone);
        }
        if (StringUtils.hasText(operatorName)) {
            wrapper.eq(VehicleInspection::getOperatorName, operatorName);
        }
        if (StringUtils.hasText(reviewerPhone)) {
            wrapper.eq(VehicleInspection::getReviewerPhone, reviewerPhone);
        }
        if (startTime != null && endTime != null) {
            wrapper.between(VehicleInspection::getInspectionTime, startTime, endTime);
        } else if (startTime != null) {
            wrapper.ge(VehicleInspection::getInspectionTime, startTime);
        } else if (endTime != null) {
            wrapper.le(VehicleInspection::getInspectionTime, endTime);
        }
        if (resultStatus != null) {
            wrapper.eq(VehicleInspection::getResultStatus, resultStatus);
        }
        if (manualReviewState != null) {
            wrapper.eq(VehicleInspection::getManualReviewState, manualReviewState);
        }
        if (toTransportdeptState != null) {
            wrapper.eq(VehicleInspection::getToTransportdeptState, toTransportdeptState);
        }

        wrapper.orderByDesc(VehicleInspection::getInspectionTime);
        return mapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getTodayStats() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        String sql = "SELECT COUNT(*) AS total, " +
                "SUM(CASE WHEN result_status = 1 THEN 1 ELSE 0 END) AS passCount, " +
                "SUM(CASE WHEN result_status = 2 THEN 1 ELSE 0 END) AS failCount " +
                "FROM %s WHERE inspection_time >= ? AND inspection_time < ?";
        Map<String, Object> row = dynamicTableService.crossTableAggregate(sql, startOfDay, endOfDay);

        long total = row.get("total") != null ? ((Number) row.get("total")).longValue() : 0L;
        long passCount = row.get("passCount") != null ? ((Number) row.get("passCount")).longValue() : 0L;
        long failCount = row.get("failCount") != null ? ((Number) row.get("failCount")).longValue() : 0L;

        // pendingReviewCount 暂不跨表查询，返回0
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("passCount", passCount);
        stats.put("failCount", failCount);
        stats.put("pendingReviewCount", 0L);
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleInspection> getPendingReviewList(int limit) {
        List<Map<String, Object>> rows = dynamicTableService.crossTablePendingReview(limit);
        return rows.stream().map(this::mapToVehicleInspection).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleInspection> getFakeGreenList(int limit) {
        List<Map<String, Object>> rows = dynamicTableService.crossTableFakeGreen(limit);
        return rows.stream().map(this::mapToVehicleInspection).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHourlyDistribution() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        String sql = "SELECT HOUR(inspection_time) AS hour, " +
                "COUNT(*) AS count, " +
                "SUM(CASE WHEN result_status = 1 THEN 1 ELSE 0 END) AS passCount, " +
                "SUM(CASE WHEN result_status = 2 THEN 1 ELSE 0 END) AS failCount " +
                "FROM %s WHERE inspection_time >= ? AND inspection_time < ? " +
                "GROUP BY HOUR(inspection_time) ORDER BY hour";
        List<Map<String, Object>> dbRows = dynamicTableService.crossTableGroupBy(sql, startOfDay, endOfDay);

        Map<Integer, Long> hourCounts = new HashMap<>();
        Map<Integer, Long> passCounts = new HashMap<>();
        Map<Integer, Long> failCounts = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            hourCounts.put(i, 0L);
            passCounts.put(i, 0L);
            failCounts.put(i, 0L);
        }
        for (Map<String, Object> row : dbRows) {
            if (row.get("hour") != null) {
                int h = ((Number) row.get("hour")).intValue();
                hourCounts.put(h, row.get("count") != null ? ((Number) row.get("count")).longValue() : 0L);
                passCounts.put(h, row.get("passCount") != null ? ((Number) row.get("passCount")).longValue() : 0L);
                failCounts.put(h, row.get("failCount") != null ? ((Number) row.get("failCount")).longValue() : 0L);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> item = new HashMap<>();
            item.put("hour", h);
            item.put("count", hourCounts.get(h));
            item.put("passCount", passCounts.get(h));
            item.put("failCount", failCounts.get(h));
            item.put("label", String.format("%02d:00", h));
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTimeDistribution(LocalDateTime startTime, LocalDateTime endTime) {
        long days = java.time.Duration.between(startTime, endTime).toDays();
        System.out.println("getTimeDistribution: start=" + startTime + ", end=" + endTime + ", days=" + days);

        if (days <= 1) {
            return getHourlyDistribution();
        } else {
            String sql = "SELECT DATE(inspection_time) AS date, COUNT(*) AS count " +
                    "FROM %s WHERE inspection_time >= ? AND inspection_time < ? " +
                    "GROUP BY DATE(inspection_time) ORDER BY date";
            List<Map<String, Object>> dbRows = dynamicTableService.crossTableGroupBy(sql, startTime, endTime);
            System.out.println("Daily distribution result count: " + dbRows.size());
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> row : dbRows) {
                Map<String, Object> item = new HashMap<>();
                item.put("date", row.get("date"));
                item.put("count", row.get("count"));
                item.put("label", row.get("date").toString());
                result.add(item);
            }
            return result;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGoodsTypeStats(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT goods_type, COUNT(*) AS count FROM %s WHERE inspection_time >= ? AND inspection_time < ? GROUP BY goods_type";
        return dynamicTableService.crossTableGoodsTypeStats(sql, startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGoodsTypeStatsByVariety(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT goods_type, COUNT(*) AS count FROM %s WHERE inspection_time >= ? AND inspection_time < ? GROUP BY goods_type";
        return dynamicTableService.crossTableGoodsTypeStatsByVariety(sql, startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGoodsTypeStatsAll() {
        String sql = "SELECT goods_type, COUNT(*) AS count FROM %s GROUP BY goods_type ORDER BY count DESC";
        return dynamicTableService.crossTableGoodsTypeStatsAll(sql);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGoodsTypeStatsForCloud() {
        String sql = "SELECT goods_type, COUNT(*) AS count FROM %s GROUP BY goods_type ORDER BY count DESC LIMIT 70";
        return dynamicTableService.crossTableGoodsTypeStatsForCloud(sql);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGoodsTypeStatsByCategory() {
        String sql = "SELECT goods_type, COUNT(*) AS count FROM %s GROUP BY goods_type ORDER BY count DESC";
        return dynamicTableService.crossTableGoodsTypeStatsByCategory(sql);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleInspection> getRecentRecords(int limit) {
        List<Map<String, Object>> rows = dynamicTableService.crossTableRecentRecords(limit);
        return rows.stream().map(this::mapToVehicleInspection).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDatascreenStats() {
        return dynamicTableService.crossTableDatascreenStats();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCreditRanking() {
        String sql = "SELECT plate_number AS plateNumber, " +
                "SUM(CASE WHEN result_status = 1 THEN 1 ELSE 0 END) AS passCount, " +
                "COUNT(*) AS totalCount, " +
                "ROUND(SUM(CASE WHEN result_status = 1 THEN 1 ELSE 0 END) * 10.0 / COUNT(*), 1) AS creditScore " +
                "FROM %s GROUP BY plate_number ORDER BY passCount DESC LIMIT 3";
        List<Map<String, Object>> allRows = dynamicTableService.crossTableCreditRanking(sql, LocalDateTime.now().minusYears(10), LocalDateTime.now());
        // 按 passCount 降序取前3
        return allRows.stream()
                .sorted((a, b) -> Long.compare(
                        ((Number) b.getOrDefault("passCount", 0)).longValue(),
                        ((Number) a.getOrDefault("passCount", 0)).longValue()))
                .limit(3)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getInfoOverview(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN result_status = 1 THEN passcode_fee ELSE 0 END), 0) AS exemptFee, " +
                "COALESCE(SUM(CASE WHEN result_status = 2 THEN passcode_fee ELSE 0 END), 0) AS chaseFee " +
                "FROM %s WHERE inspection_time >= ? AND inspection_time < ?";
        Map<String, Object> result = dynamicTableService.crossTableAggregate(sql, startTime, endTime);

        double exemptFee = result.get("exemptFee") != null ? ((Number) result.get("exemptFee")).doubleValue() : 0.0;
        double chaseFee = result.get("chaseFee") != null ? ((Number) result.get("chaseFee")).doubleValue() : 0.0;

        Map<String, Object> data = new HashMap<>();
        data.put("exemptFee", exemptFee);
        data.put("chaseFee", chaseFee);
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHourlyDistributionByRange(LocalDateTime startTime, LocalDateTime endTime, String timeType) {
        String hourlySql = "SELECT HOUR(inspection_time) AS hour, " +
                "COUNT(*) AS count, " +
                "SUM(CASE WHEN result_status = 1 THEN 1 ELSE 0 END) AS passCount, " +
                "SUM(CASE WHEN result_status = 2 THEN 1 ELSE 0 END) AS failCount " +
                "FROM %s WHERE inspection_time >= ? AND inspection_time < ? " +
                "GROUP BY HOUR(inspection_time) ORDER BY hour";
        String dailySql = "SELECT DATE(inspection_time) AS label, COUNT(*) AS count, " +
                "SUM(CASE WHEN result_status = 1 THEN 1 ELSE 0 END) AS passCount, " +
                "SUM(CASE WHEN result_status = 2 THEN 1 ELSE 0 END) AS failCount " +
                "FROM %s WHERE inspection_time >= ? AND inspection_time < ? " +
                "GROUP BY DATE(inspection_time) ORDER BY label";
        String monthlySql = "SELECT DATE_FORMAT(inspection_time, '%Y-%m') AS label, COUNT(*) AS count, " +
                "SUM(CASE WHEN result_status = 1 THEN 1 ELSE 0 END) AS passCount, " +
                "SUM(CASE WHEN result_status = 2 THEN 1 ELSE 0 END) AS failCount " +
                "FROM %s WHERE inspection_time >= ? AND inspection_time < ? " +
                "GROUP BY DATE_FORMAT(inspection_time, '%Y-%m') ORDER BY label";

        switch (timeType) {
            case "day":
                return fillHourlyData(dynamicTableService.crossTableGroupBy(hourlySql, startTime, endTime));
            case "month":
                return fillDailyData(dynamicTableService.crossTableGroupBy(dailySql, startTime, endTime), startTime.toLocalDate(), endTime.toLocalDate());
            case "year":
                return fillMonthlyData(dynamicTableService.crossTableGroupBy(monthlySql, startTime, endTime));
            default:
                return fillHourlyData(dynamicTableService.crossTableGroupBy(hourlySql, startTime, endTime));
        }
    }

    /** 补充完整的24小时数据 */
    private List<Map<String, Object>> fillHourlyData(List<Map<String, Object>> dbRows) {
        Map<Integer, Long> hourCounts = new HashMap<>();
        Map<Integer, Long> passCounts = new HashMap<>();
        Map<Integer, Long> failCounts = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            hourCounts.put(i, 0L);
            passCounts.put(i, 0L);
            failCounts.put(i, 0L);
        }
        for (Map<String, Object> row : dbRows) {
            int h = ((Number) row.get("hour")).intValue();
            hourCounts.put(h, ((Number) row.get("count")).longValue());
            passCounts.put(h, row.get("passCount") != null ? ((Number) row.get("passCount")).longValue() : 0L);
            failCounts.put(h, row.get("failCount") != null ? ((Number) row.get("failCount")).longValue() : 0L);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> item = new HashMap<>();
            item.put("hour", h);
            item.put("count", hourCounts.get(h));
            item.put("passCount", passCounts.get(h));
            item.put("failCount", failCounts.get(h));
            item.put("label", String.format("%02d:00", h));
            result.add(item);
        }
        return result;
    }

    /** 补充完整的天数据 */
    private List<Map<String, Object>> fillDailyData(List<Map<String, Object>> dbRows, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> dayCounts = new LinkedHashMap<>();
        Map<LocalDate, Long> passCounts = new LinkedHashMap<>();
        Map<LocalDate, Long> failCounts = new LinkedHashMap<>();
        // 遍历日期范围
        LocalDate current = startDate;
        while (current.isBefore(endDate)) {
            dayCounts.put(current, 0L);
            passCounts.put(current, 0L);
            failCounts.put(current, 0L);
            current = current.plusDays(1);
        }
        // 填充数据库结果
        for (Map<String, Object> row : dbRows) {
            Object labelObj = row.get("label");
            if (labelObj != null) {
                LocalDate date = LocalDate.parse(labelObj.toString());
                dayCounts.put(date, ((Number) row.get("count")).longValue());
                passCounts.put(date, row.get("passCount") != null ? ((Number) row.get("passCount")).longValue() : 0L);
                failCounts.put(date, row.get("failCount") != null ? ((Number) row.get("failCount")).longValue() : 0L);
            }
        }
        // 按日期顺序返回
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : dayCounts.entrySet()) {
            LocalDate date = entry.getKey();
            Map<String, Object> item = new HashMap<>();
            item.put("label", String.format("%d月%d日", date.getMonthValue(), date.getDayOfMonth()));
            item.put("count", entry.getValue());
            item.put("passCount", passCounts.get(date));
            item.put("failCount", failCounts.get(date));
            result.add(item);
        }
        return result;
    }

    /** 补充完整的月数据 */
    private List<Map<String, Object>> fillMonthlyData(List<Map<String, Object>> dbRows) {
        // 使用LinkedHashMap保持插入顺序
        Map<String, Long> monthCounts = new LinkedHashMap<>();
        Map<String, Long> passCounts = new LinkedHashMap<>();
        Map<String, Long> failCounts = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        // 填充最近12月的空数据（从最早到最近）
        for (int i = 11; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String label = String.format("%s-%02d", monthDate.getYear(), monthDate.getMonthValue());
            monthCounts.put(label, 0L);
            passCounts.put(label, 0L);
            failCounts.put(label, 0L);
        }
        // 填充数据库结果
        for (Map<String, Object> row : dbRows) {
            String label = row.get("label") != null ? row.get("label").toString() : null;
            if (label != null) {
                monthCounts.put(label, ((Number) row.get("count")).longValue());
                passCounts.put(label, row.get("passCount") != null ? ((Number) row.get("passCount")).longValue() : 0L);
                failCounts.put(label, row.get("failCount") != null ? ((Number) row.get("failCount")).longValue() : 0L);
            }
        }
        // 按顺序返回
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : monthCounts.entrySet()) {
            String label = entry.getKey();
            Map<String, Object> item = new HashMap<>();
            item.put("label", label);
            item.put("count", entry.getValue());
            item.put("passCount", passCounts.get(label));
            item.put("failCount", failCounts.get(label));
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVehicleTypeStats(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT vehicle_type AS type, COUNT(*) AS count " +
                "FROM %s WHERE inspection_time >= ? AND inspection_time < ? " +
                "GROUP BY vehicle_type ORDER BY count DESC";
        return dynamicTableService.crossTableGroupBy(sql, startTime, endTime);
    }


    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProcessTimeDistribution(LocalDateTime startTime, LocalDateTime endTime, String timeType) {
        String hourlySql = "SELECT HOUR(inspection_time) AS hour, " +
                "AVG(TIMESTAMPDIFF(SECOND, acceptance_time, inspection_time)) AS avgSeconds " +
                "FROM %s WHERE acceptance_time IS NOT NULL AND inspection_time IS NOT NULL " +
                "AND inspection_time >= ? AND inspection_time < ? " +
                "GROUP BY HOUR(inspection_time) ORDER BY hour";
        String dailySql = "SELECT DATE(inspection_time) AS label, " +
                "AVG(TIMESTAMPDIFF(SECOND, acceptance_time, inspection_time)) AS avgSeconds " +
                "FROM %s WHERE acceptance_time IS NOT NULL AND inspection_time IS NOT NULL " +
                "AND inspection_time >= ? AND inspection_time < ? " +
                "GROUP BY DATE(inspection_time) ORDER BY label";
        String monthlySql = "SELECT DATE_FORMAT(inspection_time, '%Y-%m') AS label, " +
                "AVG(TIMESTAMPDIFF(SECOND, acceptance_time, inspection_time)) AS avgSeconds " +
                "FROM %s WHERE acceptance_time IS NOT NULL AND inspection_time IS NOT NULL " +
                "AND inspection_time >= ? AND inspection_time < ? " +
                "GROUP BY DATE_FORMAT(inspection_time, '%Y-%m') ORDER BY label";

        switch (timeType) {
            case "day":
                return fillHourlyProcessTime(dynamicTableService.crossTableProcessTime(hourlySql, startTime, endTime));
            case "month":
                return fillDailyProcessTime(dynamicTableService.crossTableProcessTime(dailySql, startTime, endTime), startTime.toLocalDate(), endTime.toLocalDate());
            case "year":
                return fillMonthlyProcessTime(dynamicTableService.crossTableProcessTime(monthlySql, startTime, endTime));
            default:
                return fillHourlyProcessTime(dynamicTableService.crossTableProcessTime(hourlySql, startTime, endTime));
        }
    }

    @Override
    public Map<String, Object> getAvgProcessTime(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT " +
                "COUNT(*) AS totalCount, " +
                "AVG(TIMESTAMPDIFF(SECOND, acceptance_time, inspection_time)) AS avgSeconds " +
                "FROM %s WHERE acceptance_time IS NOT NULL AND inspection_time IS NOT NULL " +
                "AND inspection_time >= ? AND inspection_time < ?";
        return dynamicTableService.crossTableAggregate(sql, startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAvgDetectionTime(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT " +
                "AVG(TIMESTAMPDIFF(SECOND, acceptance_time, cd_photo_time)) AS avgSeconds " +
                "FROM %s WHERE acceptance_time IS NOT NULL AND cd_photo_time IS NOT NULL " +
                "AND inspection_time >= ? AND inspection_time < ?";
        return dynamicTableService.crossTableAggregate(sql, startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMostProvince(LocalDateTime startTime, LocalDateTime endTime) {
        String provinceSql = "SELECT si.province AS provinceCode, COUNT(*) AS count " +
                "FROM %s vi INNER JOIN station_info si ON si.station_id = vi.passcode_en_station_id " +
                "WHERE vi.inspection_time >= ? AND vi.inspection_time < ? " +
                "AND si.province IS NOT NULL AND si.province != '' AND si.province != '42' " +
                "GROUP BY si.province ORDER BY count DESC";
        List<Map<String, Object>> provinceList = dynamicTableService.crossTableProvinceStats(provinceSql, startTime, endTime);
        if (provinceList == null || provinceList.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("provinceName", "暂无数据");
            empty.put("provinceCode", "");
            empty.put("count", 0);
            empty.put("topCity", "");
            empty.put("topCityCount", 0);
            return empty;
        }

        // 最大省份
        Map<String, Object> top = provinceList.get(0);
        String provinceCode = top.get("provinceCode") != null ? top.get("provinceCode").toString() : "";
        String provinceName = provinceCacheService.getProvinceNameByCode(provinceCode);
        long totalCount = ((Number) top.get("count")).longValue();

        // 湖北省内出现次数最多的站点（固定查42省份）
        String hubeiSql = "SELECT si.station_name AS stationName, COUNT(*) AS count " +
                "FROM %s vi INNER JOIN station_info si ON si.station_id = vi.passcode_en_station_id " +
                "WHERE vi.inspection_time >= ? AND vi.inspection_time < ? " +
                "AND si.province = '42' AND si.station_name IS NOT NULL AND si.station_name != '' " +
                "GROUP BY si.station_name ORDER BY count DESC LIMIT 1";
        List<Map<String, Object>> hubeiStationStats = dynamicTableService.crossTableTopStationInHubei(hubeiSql, startTime, endTime);
        String topStation = "";
        long topStationCount = 0;
        if (hubeiStationStats != null && !hubeiStationStats.isEmpty()) {
            Map<String, Object> stationTop = hubeiStationStats.get(0);
            topStation = stationTop.get("stationName") != null ? stationTop.get("stationName").toString() : "";
            topStationCount = ((Number) stationTop.get("count")).longValue();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("provinceName", provinceName);
        result.put("provinceCode", provinceCode);
        result.put("count", totalCount);
        result.put("topStation", topStation);
        result.put("topStationCount", topStationCount);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProvinceStatsAll() {
        String sql = "SELECT si.province AS provinceCode, COUNT(*) AS count " +
                "FROM %s vi INNER JOIN station_info si ON si.station_id = vi.passcode_en_station_id " +
                "WHERE si.province IS NOT NULL AND si.province != '' " +
                "GROUP BY si.province ORDER BY count DESC";
        List<Map<String, Object>> provinceList = dynamicTableService.crossTableProvinceStatsAll(sql);
        for (Map<String, Object> item : provinceList) {
            String provinceCode = item.get("provinceCode") != null ? item.get("provinceCode").toString() : "";
            String provinceName = provinceCacheService.getProvinceNameByCode(provinceCode);
            item.put("name", provinceName);
        }
        return provinceList;
    }

    private List<Map<String, Object>> fillHourlyProcessTime(List<Map<String, Object>> dbRows) {
        Map<Integer, Double> hourData = new HashMap<>();
        for (int i = 0; i < 24; i++) hourData.put(i, 0.0);
        for (Map<String, Object> row : dbRows) {
            if (row.get("hour") != null && row.get("avgSeconds") != null) {
                hourData.put(((Number) row.get("hour")).intValue(), ((Number) row.get("avgSeconds")).doubleValue());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> item = new HashMap<>();
            item.put("hour", h);
            item.put("avgSeconds", hourData.get(h));
            item.put("label", String.format("%02d:00", h));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> fillDailyProcessTime(List<Map<String, Object>> dbRows, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Double> dayData = new LinkedHashMap<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate.minusDays(1))) {
            dayData.put(current, 0.0);
            current = current.plusDays(1);
        }
        for (Map<String, Object> row : dbRows) {
            if (row.get("label") != null && row.get("avgSeconds") != null) {
                LocalDate date = LocalDate.parse(row.get("label").toString());
                dayData.put(date, ((Number) row.get("avgSeconds")).doubleValue());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Double> entry : dayData.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("label", entry.getKey().toString());
            item.put("avgSeconds", entry.getValue());
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> fillMonthlyProcessTime(List<Map<String, Object>> dbRows) {
        Map<String, Double> monthData = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            monthData.put(String.format("%d-%02d", month.getYear(), month.getMonthValue()), 0.0);
        }
        for (Map<String, Object> row : dbRows) {
            if (row.get("label") != null && row.get("avgSeconds") != null) {
                monthData.put(row.get("label").toString(), ((Number) row.get("avgSeconds")).doubleValue());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : monthData.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("label", entry.getKey());
            item.put("avgSeconds", entry.getValue());
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleInspection> searchForExport(
            String plateNumber,
            String driverPhone,
            String reviewerPhone,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer resultStatus,
            Integer manualReviewState,
            Integer toTransportdeptState,
            String goodsType) {

        LambdaQueryWrapper<VehicleInspection> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(plateNumber)) {
            wrapper.like(VehicleInspection::getPlateNumber, plateNumber);
        }
        if (StringUtils.hasText(driverPhone)) {
            wrapper.like(VehicleInspection::getDriverPhone, driverPhone);
        }
        if (StringUtils.hasText(reviewerPhone)) {
            wrapper.eq(VehicleInspection::getReviewerPhone, reviewerPhone);
        }
        if (startTime != null && endTime != null) {
            wrapper.between(VehicleInspection::getInspectionTime, startTime, endTime);
        } else if (startTime != null) {
            wrapper.ge(VehicleInspection::getInspectionTime, startTime);
        } else if (endTime != null) {
            wrapper.le(VehicleInspection::getInspectionTime, endTime);
        }
        if (resultStatus != null) {
            wrapper.eq(VehicleInspection::getResultStatus, resultStatus);
        }
        if (manualReviewState != null) {
            wrapper.eq(VehicleInspection::getManualReviewState, manualReviewState);
        }
        if (toTransportdeptState != null) {
            wrapper.eq(VehicleInspection::getToTransportdeptState, toTransportdeptState);
        }

        wrapper.orderByAsc(VehicleInspection::getAcceptanceTime);
        return mapper.selectList(wrapper);
    }

    private VehicleInspection mapToVehicleInspection(Map<String, Object> row) {
        VehicleInspection v = new VehicleInspection();
        v.setId(row.get("id") != null ? ((Number) row.get("id")).intValue() : null);
        v.setPlateNumber(row.get("plate_number") != null ? row.get("plate_number").toString() : null);
        v.setPlateNumberGc(row.get("plate_number_gc") != null ? row.get("plate_number_gc").toString() : null);
        v.setDriverPhone(row.get("driver_phone") != null ? row.get("driver_phone").toString() : null);
        v.setVehicleType(row.get("vehicle_type") != null ? row.get("vehicle_type").toString() : null);
        v.setVehicleContainertype(row.get("vehicle_containertype") != null ? row.get("vehicle_containertype").toString() : null);
        v.setGoodsType(row.get("goods_type") != null ? row.get("goods_type").toString() : null);
        v.setGoodsCategory(row.get("goods_category") != null ? row.get("goods_category").toString() : null);
        v.setLoadRate(row.get("load_rate") != null ? new java.math.BigDecimal(row.get("load_rate").toString()) : null);
        v.setLoadWeight(row.get("load_weight") != null ? new java.math.BigDecimal(row.get("load_weight").toString()) : null);
        v.setVehicleSize(row.get("vehicle_size") != null ? row.get("vehicle_size").toString() : null);
        v.setBodyImagePath(row.get("body_image_path") != null ? row.get("body_image_path").toString() : null);
        v.setTransparentImagePath(row.get("transparent_image_path") != null ? row.get("transparent_image_path").toString() : null);
        v.setHeadImagePath(row.get("head_image_path") != null ? row.get("head_image_path").toString() : null);
        v.setTailImagePath(row.get("tail_image_path") != null ? row.get("tail_image_path").toString() : null);
        v.setTopImagePath(row.get("top_image_path") != null ? row.get("top_image_path").toString() : null);
        v.setGoodsImagePath(row.get("goods_image_path") != null ? row.get("goods_image_path").toString() : null);
        v.setLicenseImagePath(row.get("license_image_path") != null ? row.get("license_image_path").toString() : null);
        v.setPasscodeImagePath(row.get("passcode_image_path") != null ? row.get("passcode_image_path").toString() : null);
        v.setPasscodeVehicleId(row.get("passcode_vehicle_id") != null ? row.get("passcode_vehicle_id").toString() : null);
        v.setPasscodeVehicleDisplayId(row.get("passcode_vehicle_display_id") != null ? row.get("passcode_vehicle_display_id").toString() : null);
        v.setPasscodeVehicleColorName(row.get("passcode_vehicle_color_name") != null ? row.get("passcode_vehicle_color_name").toString() : null);
        v.setPasscodeEnStationId(row.get("passcode_en_station_id") != null ? row.get("passcode_en_station_id").toString() : null);
        v.setPasscodeExStationId(row.get("passcode_ex_station_id") != null ? row.get("passcode_ex_station_id").toString() : null);
        v.setPasscodeEnWeight(row.get("passcode_en_weight") != null ? row.get("passcode_en_weight").toString() : null);
        v.setPasscodeExWeight(row.get("passcode_ex_weight") != null ? row.get("passcode_ex_weight").toString() : null);
        v.setPasscodeMediaType(row.get("passcode_media_type") != null ? row.get("passcode_media_type").toString() : null);
        v.setPasscodeTransactionId(row.get("passcode_transaction_id") != null ? row.get("passcode_transaction_id").toString() : null);
        v.setPasscodePassId(row.get("passcode_pass_id") != null ? row.get("passcode_pass_id").toString() : null);
        v.setPasscodeExTime(row.get("passcode_ex_time") != null ? row.get("passcode_ex_time").toString() : null);
        v.setPasscodeFee(row.get("passcode_fee") != null ? row.get("passcode_fee").toString() : null);
        v.setOperatorName(row.get("operator_name") != null ? row.get("operator_name").toString() : null);
        v.setReviewerPhone(row.get("reviewer_phone") != null ? row.get("reviewer_phone").toString() : null);
        v.setInspectionTime(row.get("inspection_time") != null ? LocalDateTime.parse(row.get("inspection_time").toString().replace(" ", "T")) : null);
        v.setAcceptanceTime(row.get("acceptance_time") != null ? LocalDateTime.parse(row.get("acceptance_time").toString().replace(" ", "T")) : null);
        v.setResultStatus(row.get("result_status") != null ? ((Number) row.get("result_status")).intValue() : null);
        v.setManualReviewState(row.get("manual_review_state") != null ? ((Number) row.get("manual_review_state")).intValue() : null);
        v.setToTransportdeptState(row.get("to_transportdept_state") != null ? ((Number) row.get("to_transportdept_state")).intValue() : null);
        return v;
    }
}
