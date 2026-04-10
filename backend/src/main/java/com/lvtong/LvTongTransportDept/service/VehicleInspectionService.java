package com.lvtong.LvTongTransportDept.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lvtong.LvTongTransportDept.entity.VehicleInspection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 车辆查验记录服务接口
 */
public interface VehicleInspectionService {

    /** 根据主键查询单条 */
    VehicleInspection getById(Integer id);

    /** 新增查验记录 */
    VehicleInspection create(VehicleInspection inspection);

    /** 更新记录（部分更新） */
    VehicleInspection update(Integer id, VehicleInspection inspection);

    /** 多条件组合分页查询 */
    IPage<VehicleInspection> searchWithConditions(
            String plateNumber,
            String driverPhone,
            String operatorName,
            String reviewerPhone,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer resultStatus,
            Integer manualReviewState,
            int page,
            int pageSize);

    /** 获取今日统计数据 */
    Map<String, Long> getTodayStats();

    /** 获取待复核记录列表 */
    List<VehicleInspection> getPendingReviewList(int limit);

    /** 获取假冒绿通预警列表 */
    List<VehicleInspection> getFakeGreenList(int limit);

    /** 获取 24 小时查验时段分布 */
    List<Map<String, Object>> getHourlyDistribution();

    /** 获取货物类别统计（默认当日） */
    List<Map<String, Object>> getGoodsTypeStats(LocalDateTime startTime, LocalDateTime endTime);

    /** 获取最近查验记录 */
    List<VehicleInspection> getRecentRecords(int limit);
}
