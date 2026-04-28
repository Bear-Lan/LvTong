package com.lvtong.LvTongTransportDept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lvtong.LvTongTransportDept.entity.VehicleInspection;
import com.lvtong.LvTongTransportDept.exception.BusinessException;
import com.lvtong.LvTongTransportDept.mapper.VehicleInspectionMapper;
import com.lvtong.LvTongTransportDept.service.ProvinceCacheService;
import com.lvtong.LvTongTransportDept.service.VehicleInspectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆查验记录业务逻辑实现
 */
@Service
public class VehicleInspectionServiceImpl implements VehicleInspectionService {

    @Autowired
    private VehicleInspectionMapper mapper;

    @Autowired
    private ProvinceCacheService provinceCacheService;

    @Override
    @Transactional(readOnly = true)
    public VehicleInspection getById(Integer id) {
        return mapper.selectById(id);
    }

    @Override
    @Transactional
    public VehicleInspection create(VehicleInspection inspection) {
        mapper.insert(inspection);
        return inspection;
    }

    @Override
    @Transactional
    public VehicleInspection update(Integer id, VehicleInspection inspection) {
        VehicleInspection existing = mapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("查验记录不存在");
        }

        LambdaUpdateWrapper<VehicleInspection> wrapper = new LambdaUpdateWrapper<>();

        wrapper.set(inspection.getPlateNumber() != null,
                VehicleInspection::getPlateNumber, inspection.getPlateNumber());
        wrapper.set(inspection.getPlateNumberGc() != null,
                VehicleInspection::getPlateNumberGc, inspection.getPlateNumberGc());
        wrapper.set(inspection.getDriverPhone() != null,
                VehicleInspection::getDriverPhone, inspection.getDriverPhone());
        wrapper.set(inspection.getVehicleType() != null,
                VehicleInspection::getVehicleType, inspection.getVehicleType());
        wrapper.set(inspection.getVehicleContainertype() != null,
                VehicleInspection::getVehicleContainertype, inspection.getVehicleContainertype());
        wrapper.set(inspection.getGoodsType() != null,
                VehicleInspection::getGoodsType, inspection.getGoodsType());
        wrapper.set(inspection.getGoodsCategory() != null,
                VehicleInspection::getGoodsCategory, inspection.getGoodsCategory());
        wrapper.set(inspection.getLoadRate() != null,
                VehicleInspection::getLoadRate, inspection.getLoadRate());
        wrapper.set(inspection.getLoadWeight() != null,
                VehicleInspection::getLoadWeight, inspection.getLoadWeight());
        wrapper.set(inspection.getVehicleSize() != null,
                VehicleInspection::getVehicleSize, inspection.getVehicleSize());
        wrapper.set(inspection.getHistoryRecord() != null,
                VehicleInspection::getHistoryRecord, inspection.getHistoryRecord());
        wrapper.set(inspection.getBodyImagePath() != null,
                VehicleInspection::getBodyImagePath, inspection.getBodyImagePath());
        wrapper.set(inspection.getTransparentImagePath() != null,
                VehicleInspection::getTransparentImagePath, inspection.getTransparentImagePath());
        wrapper.set(inspection.getHeadImagePath() != null,
                VehicleInspection::getHeadImagePath, inspection.getHeadImagePath());
        wrapper.set(inspection.getTailImagePath() != null,
                VehicleInspection::getTailImagePath, inspection.getTailImagePath());
        wrapper.set(inspection.getTopImagePath() != null,
                VehicleInspection::getTopImagePath, inspection.getTopImagePath());
        wrapper.set(inspection.getGoodsImagePath() != null,
                VehicleInspection::getGoodsImagePath, inspection.getGoodsImagePath());
        wrapper.set(inspection.getLicenseImagePath() != null,
                VehicleInspection::getLicenseImagePath, inspection.getLicenseImagePath());
        wrapper.set(inspection.getPasscodeImagePath() != null,
                VehicleInspection::getPasscodeImagePath, inspection.getPasscodeImagePath());
        wrapper.set(inspection.getPasscodeVehicleId() != null,
                VehicleInspection::getPasscodeVehicleId, inspection.getPasscodeVehicleId());
        wrapper.set(inspection.getPasscodeVehicleDisplayId() != null,
                VehicleInspection::getPasscodeVehicleDisplayId, inspection.getPasscodeVehicleDisplayId());
        wrapper.set(inspection.getPasscodeVehicleColorName() != null,
                VehicleInspection::getPasscodeVehicleColorName, inspection.getPasscodeVehicleColorName());
        wrapper.set(inspection.getPasscodeEnStationId() != null,
                VehicleInspection::getPasscodeEnStationId, inspection.getPasscodeEnStationId());
        wrapper.set(inspection.getPasscodeExStationId() != null,
                VehicleInspection::getPasscodeExStationId, inspection.getPasscodeExStationId());
        wrapper.set(inspection.getPasscodeEnWeight() != null,
                VehicleInspection::getPasscodeEnWeight, inspection.getPasscodeEnWeight());
        wrapper.set(inspection.getPasscodeExWeight() != null,
                VehicleInspection::getPasscodeExWeight, inspection.getPasscodeExWeight());
        wrapper.set(inspection.getPasscodeMediaType() != null,
                VehicleInspection::getPasscodeMediaType, inspection.getPasscodeMediaType());
        wrapper.set(inspection.getPasscodeTransactionId() != null,
                VehicleInspection::getPasscodeTransactionId, inspection.getPasscodeTransactionId());
        wrapper.set(inspection.getPasscodePassId() != null,
                VehicleInspection::getPasscodePassId, inspection.getPasscodePassId());
        wrapper.set(inspection.getPasscodeExTime() != null,
                VehicleInspection::getPasscodeExTime, inspection.getPasscodeExTime());
        wrapper.set(inspection.getPasscodeTransPayType() != null,
                VehicleInspection::getPasscodeTransPayType, inspection.getPasscodeTransPayType());
        wrapper.set(inspection.getPasscodeFee() != null,
                VehicleInspection::getPasscodeFee, inspection.getPasscodeFee());
        wrapper.set(inspection.getPasscodePayFee() != null,
                VehicleInspection::getPasscodePayFee, inspection.getPasscodePayFee());
        wrapper.set(inspection.getPasscodeVehicleSign() != null,
                VehicleInspection::getPasscodeVehicleSign, inspection.getPasscodeVehicleSign());
        wrapper.set(inspection.getPasscodeProvinceCount() != null,
                VehicleInspection::getPasscodeProvinceCount, inspection.getPasscodeProvinceCount());
        wrapper.set(inspection.getOperatorName() != null,
                VehicleInspection::getOperatorName, inspection.getOperatorName());
        wrapper.set(inspection.getInspectionTime() != null,
                VehicleInspection::getInspectionTime, inspection.getInspectionTime());
        wrapper.set(inspection.getResultStatus() != null,
                VehicleInspection::getResultStatus, inspection.getResultStatus());
        wrapper.set(inspection.getNopassType() != null,
                VehicleInspection::getNopassType, inspection.getNopassType());
        wrapper.set(inspection.getStatus() != null,
                VehicleInspection::getStatus, inspection.getStatus());
        wrapper.set(inspection.getGroupId() != null,
                VehicleInspection::getGroupId, inspection.getGroupId());
        wrapper.set(inspection.getInspectorPhone() != null,
                VehicleInspection::getInspectorPhone, inspection.getInspectorPhone());
        wrapper.set(inspection.getReviewerPhone() != null,
                VehicleInspection::getReviewerPhone, inspection.getReviewerPhone());
        wrapper.set(inspection.getManualReviewState() != null,
                VehicleInspection::getManualReviewState, inspection.getManualReviewState());
        wrapper.set(inspection.getToTransportdeptState() != null,
                VehicleInspection::getToTransportdeptState, inspection.getToTransportdeptState());
        wrapper.set(inspection.getToTransportdeptTime() != null,
                VehicleInspection::getToTransportdeptTime, inspection.getToTransportdeptTime());
        wrapper.set(inspection.getToTransportdeptComment() != null,
                VehicleInspection::getToTransportdeptComment, inspection.getToTransportdeptComment());

        wrapper.set(VehicleInspection::getUpdatedTime, LocalDateTime.now());
        wrapper.eq(VehicleInspection::getId, id);
        int affected = mapper.update(null, wrapper);
        if (affected == 0) {
            throw new BusinessException("更新失败，记录可能被其他用户删除");
        }

        VehicleInspection updated = mapper.selectById(id);
        if (updated == null) {
            throw new BusinessException("更新后查询失败，记录已被删除");
        }
        return updated;
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

        Map<String, Object> row = mapper.selectTodayStats(startOfDay, endOfDay);

        long total = row.get("total") != null ? ((Number) row.get("total")).longValue() : 0L;
        long passCount = row.get("passCount") != null ? ((Number) row.get("passCount")).longValue() : 0L;
        long failCount = row.get("failCount") != null ? ((Number) row.get("failCount")).longValue() : 0L;

        LambdaQueryWrapper<VehicleInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleInspection::getResultStatus, 2)
                .and(w -> w.nested((n) -> n.eq(VehicleInspection::getManualReviewState, 0)
                        .or().isNull(VehicleInspection::getManualReviewState)));
        long pendingReviewCount = mapper.selectCount(wrapper);

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("passCount", passCount);
        stats.put("failCount", failCount);
        stats.put("pendingReviewCount", pendingReviewCount);
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleInspection> getPendingReviewList(int limit) {
        LambdaQueryWrapper<VehicleInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleInspection::getResultStatus, 2)
                .and(w -> w.nested((n) -> n.eq(VehicleInspection::getManualReviewState, 0)
                        .or().isNull(VehicleInspection::getManualReviewState)))
                .orderByDesc(VehicleInspection::getInspectionTime);
        return mapper.selectPage(new Page<>(1, limit), wrapper).getRecords();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleInspection> getFakeGreenList(int limit) {
        LambdaQueryWrapper<VehicleInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleInspection::getResultStatus, 2)
                .eq(VehicleInspection::getNopassType, 21)
                .orderByDesc(VehicleInspection::getInspectionTime);
        return mapper.selectPage(new Page<>(1, limit), wrapper).getRecords();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHourlyDistribution() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Map<String, Object>> dbRows = mapper.selectHourlyDistribution(startOfDay, endOfDay);
        Map<Integer, Long> hourCounts = new HashMap<>();
        for (int i = 0; i < 24; i++) hourCounts.put(i, 0L);
        for (Map<String, Object> row : dbRows) {
            hourCounts.put(((Number) row.get("hour")).intValue(),
                          ((Number) row.get("count")).longValue());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> item = new HashMap<>();
            item.put("hour", h);
            item.put("count", hourCounts.get(h));
            item.put("label", String.format("%02d:00", h));
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTimeDistribution(LocalDateTime startTime, LocalDateTime endTime) {
        // 计算时间范围跨度（天数）
        long days = java.time.Duration.between(startTime, endTime).toDays();
        System.out.println("getTimeDistribution: start=" + startTime + ", end=" + endTime + ", days=" + days);

        if (days <= 1) {
            // 一天内：按小时统计
            return getHourlyDistribution();
        } else {
            // 多天：按天统计
            List<Map<String, Object>> dbRows = mapper.selectDailyDistribution(startTime, endTime);
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
        return mapper.selectGoodsTypeStats(startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGoodsTypeStatsAll() {
        return mapper.selectGoodsTypeStatsAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGoodsTypeStatsForCloud() {
        return mapper.selectGoodsTypeStatsForCloud();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleInspection> getRecentRecords(int limit) {
        LambdaQueryWrapper<VehicleInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(VehicleInspection::getInspectionTime);
        return mapper.selectPage(new Page<>(1, limit), wrapper).getRecords();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDatascreenStats() {
        return mapper.selectDatascreenStats();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCreditRanking() {
        return mapper.selectCreditRanking();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getInfoOverview(LocalDateTime startTime, LocalDateTime endTime) {
        // 只查询绿通减免和追逃费用（其他字段由 Controller 复用已有数据）
        Map<String, Object> result = mapper.selectInfoOverview(startTime, endTime);

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
        switch (timeType) {
            case "day":
                // 按小时统计（24小时分布），补充完整24小时数据
                return fillHourlyData(mapper.selectHourlyDistribution(startTime, endTime));
            case "month":
                // 按天统计（当月所有天数），补充完整日期数据
                return fillDailyData(mapper.selectTimeDistribution(startTime, endTime), startTime.toLocalDate(), endTime.toLocalDate());
            case "year":
                // 按月统计（最近12月），补充完整12月数据
                return fillMonthlyData(mapper.selectMonthlyDistribution(startTime, endTime));
            default:
                // 默认按小时统计
                return fillHourlyData(mapper.selectHourlyDistribution(startTime, endTime));
        }
    }

    /** 补充完整的24小时数据 */
    private List<Map<String, Object>> fillHourlyData(List<Map<String, Object>> dbRows) {
        Map<Integer, Long> hourCounts = new HashMap<>();
        for (int i = 0; i < 24; i++) hourCounts.put(i, 0L);
        for (Map<String, Object> row : dbRows) {
            hourCounts.put(((Number) row.get("hour")).intValue(),
                      ((Number) row.get("count")).longValue());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> item = new HashMap<>();
            item.put("hour", h);
            item.put("count", hourCounts.get(h));
            item.put("label", String.format("%02d:00", h));
            result.add(item);
        }
        return result;
    }

    /** 补充完整的天数据 */
    private List<Map<String, Object>> fillDailyData(List<Map<String, Object>> dbRows, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> dayCounts = new LinkedHashMap<>();
        // 遍历日期范围
        LocalDate current = startDate;
        while (current.isBefore(endDate)) {
            dayCounts.put(current, 0L);
            current = current.plusDays(1);
        }
        // 填充数据库结果
        for (Map<String, Object> row : dbRows) {
            Object labelObj = row.get("label");
            if (labelObj != null) {
                LocalDate date = LocalDate.parse(labelObj.toString());
                dayCounts.put(date, ((Number) row.get("count")).longValue());
            }
        }
        // 按日期顺序返回
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : dayCounts.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            LocalDate date = entry.getKey();
            item.put("label", String.format("%d月%d日", date.getMonthValue(), date.getDayOfMonth()));
            item.put("count", entry.getValue());
            result.add(item);
        }
        return result;
    }

    /** 补充完整的月数据 */
    private List<Map<String, Object>> fillMonthlyData(List<Map<String, Object>> dbRows) {
        // 使用LinkedHashMap保持插入顺序
        Map<String, Long> monthCounts = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        // 填充最近12月的空数据（从最早到最近）
        for (int i = 11; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String label = String.format("%s-%02d", monthDate.getYear(), monthDate.getMonthValue());
            monthCounts.put(label, 0L);
        }
        // 填充数据库结果
        for (Map<String, Object> row : dbRows) {
            String label = row.get("label") != null ? row.get("label").toString() : null;
            if (label != null) {
                monthCounts.put(label, ((Number) row.get("count")).longValue());
            }
        }
        // 按顺序返回
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : monthCounts.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("label", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVehicleTypeStats(LocalDateTime startTime, LocalDateTime endTime) {
        return mapper.selectVehicleTypeStats(startTime, endTime);
    }


    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProcessTimeDistribution(LocalDateTime startTime, LocalDateTime endTime, String timeType) {
        switch (timeType) {
            case "day":
                return fillHourlyProcessTime(mapper.selectHourlyProcessTime(startTime, endTime));
            case "month":
                return fillDailyProcessTime(mapper.selectDailyProcessTime(startTime, endTime), startTime.toLocalDate(), endTime.toLocalDate());
            case "year":
                return fillMonthlyProcessTime(mapper.selectMonthlyProcessTime(startTime, endTime));
            default:
                return fillHourlyProcessTime(mapper.selectHourlyProcessTime(startTime, endTime));
        }
    }

    @Override
    public Map<String, Object> getAvgProcessTime(LocalDateTime startTime, LocalDateTime endTime) {
        return mapper.selectAvgProcessTime(startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMostProvince(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> provinceList = mapper.selectProvinceStats(startTime, endTime);
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
        List<Map<String, Object>> hubeiStationStats = mapper.selectTopStationInHubei(startTime, endTime);
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
            Integer toTransportdeptState) {

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

        wrapper.orderByDesc(VehicleInspection::getInspectionTime);
        return mapper.selectList(wrapper);
    }
}
