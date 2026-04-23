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
            Integer toTransportdeptState,
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

    /** 获取查验时段分布（支持按日期范围） */
    List<Map<String, Object>> getTimeDistribution(LocalDateTime startTime, LocalDateTime endTime);

    /** 获取货物类别统计（默认当日） */
    List<Map<String, Object>> getGoodsTypeStats(LocalDateTime startTime, LocalDateTime endTime);

    /** 获取货物类别统计（所有数据） */
    List<Map<String, Object>> getGoodsTypeStatsAll();

    /** 获取货物类型统计（用于词云图） */
    List<Map<String, Object>> getGoodsTypeStatsForCloud();

    /** 获取最近查验记录 */
    List<VehicleInspection> getRecentRecords(int limit);

    /** 获取大屏统计数据（今日通行、总绿通、总通行金额、伪绿通） */
    Map<String, Object> getDatascreenStats();

    /** 获取信用记录排行榜（合格次数最多的前3辆车） */
    List<Map<String, Object>> getCreditRanking();

    /** 获取信息总览（按时间范围统计绿通车/收割机数量、查验车次、通行费用、合格/不合格数、上传记录数） */
    Map<String, Object> getInfoOverview(LocalDateTime startTime, LocalDateTime endTime);

    /** 获取查验时段分布（按指定时间范围，timeType=day时返回24小时分布，month时返回31天分布，year时返回12月分布） */
    List<Map<String, Object>> getHourlyDistributionByRange(LocalDateTime startTime, LocalDateTime endTime, String timeType);

    /** 获取车型分布统计（横向条形图数据） */
    List<Map<String, Object>> getVehicleTypeStats(LocalDateTime startTime, LocalDateTime endTime);

    /** 获取待办事项列表 */
    List<Map<String, Object>> getTodoItems();

    /** 获取文件通知列表 */
    List<Map<String, Object>> getNotices();

    /** 获取免检比例 */
    Map<String, Object> getExemptRate(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取处理时长分布
     * @param timeType day=按小时, month=按天, year=按月
     */
    List<Map<String, Object>> getProcessTimeDistribution(LocalDateTime startTime, LocalDateTime endTime, String timeType);

    /** 导出查询（全量数据，不分页） */
    List<VehicleInspection> searchForExport(
            String plateNumber,
            String driverPhone,
            String reviewerPhone,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer resultStatus,
            Integer manualReviewState,
            Integer toTransportdeptState);
}
