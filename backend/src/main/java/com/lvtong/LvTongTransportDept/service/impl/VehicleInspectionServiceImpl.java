package com.lvtong.LvTongTransportDept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lvtong.LvTongTransportDept.entity.VehicleInspection;
import com.lvtong.LvTongTransportDept.exception.BusinessException;
import com.lvtong.LvTongTransportDept.mapper.VehicleInspectionMapper;
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
        Map<String, Object> result = mapper.selectInfoOverview(startTime, endTime);

        // 处理可能为 null 的值，设置默认值
        long greenVehicleCount = result.get("greenVehicleCount") != null ? ((Number) result.get("greenVehicleCount")).longValue() : 0L;
        long harvesterCount = result.get("harvesterCount") != null ? ((Number) result.get("harvesterCount")).longValue() : 0L;
        long inspectionCount = result.get("inspectionCount") != null ? ((Number) result.get("inspectionCount")).longValue() : 0L;
        double passFee = result.get("passFee") != null ? ((Number) result.get("passFee")).doubleValue() : 0.0;
        long passCount = result.get("passCount") != null ? ((Number) result.get("passCount")).longValue() : 0L;
        long failCount = result.get("failCount") != null ? ((Number) result.get("failCount")).longValue() : 0L;
        long uploadCount = result.get("uploadCount") != null ? ((Number) result.get("uploadCount")).longValue() : 0L;

        Map<String, Object> data = new HashMap<>();
        data.put("greenVehicleCount", greenVehicleCount);
        data.put("harvesterCount", harvesterCount);
        data.put("inspectionCount", inspectionCount);
        data.put("passFee", passFee);
        data.put("passCount", passCount);
        data.put("failCount", failCount);
        data.put("uploadCount", uploadCount);
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
    public List<Map<String, Object>> getTodoItems() {
        List<Map<String, Object>> todos = new ArrayList<>();

        // 待复核数量
        LambdaQueryWrapper<VehicleInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleInspection::getResultStatus, 2)
                .and(w -> w.nested((n) -> n.eq(VehicleInspection::getManualReviewState, 0)
                        .or().isNull(VehicleInspection::getManualReviewState)));
        long pendingReviewCount = mapper.selectCount(wrapper);
        if (pendingReviewCount > 0) {
            Map<String, Object> todo = new HashMap<>();
            todo.put("id", 1);
            todo.put("title", "待复核车辆");
            todo.put("count", pendingReviewCount);
            todo.put("type", "pending_review");
            todos.add(todo);
        }

        // 假冒绿通预警
        LambdaQueryWrapper<VehicleInspection> fakeWrapper = new LambdaQueryWrapper<>();
        fakeWrapper.eq(VehicleInspection::getResultStatus, 2)
                .eq(VehicleInspection::getNopassType, 21);
        long fakeGreenCount = mapper.selectCount(fakeWrapper);
        if (fakeGreenCount > 0) {
            Map<String, Object> todo = new HashMap<>();
            todo.put("id", 2);
            todo.put("title", "假冒绿通预警");
            todo.put("count", fakeGreenCount);
            todo.put("type", "fake_green");
            todos.add(todo);
        }

        // 上传失败记录
        LambdaQueryWrapper<VehicleInspection> uploadWrapper = new LambdaQueryWrapper<>();
        uploadWrapper.eq(VehicleInspection::getToTransportdeptState, -1);
        long uploadFailedCount = mapper.selectCount(uploadWrapper);
        if (uploadFailedCount > 0) {
            Map<String, Object> todo = new HashMap<>();
            todo.put("id", 3);
            todo.put("title", "上传失败记录");
            todo.put("count", uploadFailedCount);
            todo.put("type", "upload_failed");
            todos.add(todo);
        }

        return todos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNotices() {
        // 模拟文件通知数据
        List<Map<String, Object>> notices = new ArrayList<>();

        Map<String, Object> notice1 = new HashMap<>();
        notice1.put("id", 1);
        notice1.put("title", "关于加强绿通车辆查验的通知");
        notice1.put("date", LocalDateTime.now().minusDays(2).toLocalDate().toString());
        notice1.put("type", "important");
        notices.add(notice1);

        Map<String, Object> notice2 = new HashMap<>();
        notice2.put("id", 2);
        notice2.put("title", "收割机免费通行政策解读");
        notice2.put("date", LocalDateTime.now().minusDays(5).toLocalDate().toString());
        notice2.put("type", "policy");
        notices.add(notice2);

        Map<String, Object> notice3 = new HashMap<>();
        notice3.put("id", 3);
        notice3.put("title", "月度查验数据统计分析报告");
        notice3.put("date", LocalDateTime.now().minusDays(10).toLocalDate().toString());
        notice3.put("type", "report");
        notices.add(notice3);

        return notices;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getExemptRate(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> result = mapper.selectExemptRate(startTime, endTime);

        long total = result.get("total") != null ? ((Number) result.get("total")).longValue() : 0L;
        long exempt = result.get("exempt") != null ? ((Number) result.get("exempt")).longValue() : 0L;

        double rate = total > 0 ? (double) exempt / total * 100 : 0.0;

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("exempt", exempt);
        data.put("rate", Math.round(rate * 10) / 10.0); // 保留一位小数
        return data;
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
