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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
            wrapper.eq(VehicleInspection::getDriverPhone, driverPhone);
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
                        .or().isNull(VehicleInspection::getManualReviewState)))
                .ge(VehicleInspection::getInspectionTime, startOfDay);
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
    public List<Map<String, Object>> getGoodsTypeStats(LocalDateTime startTime, LocalDateTime endTime) {
        return mapper.selectGoodsTypeStats(startTime, endTime);
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
}
