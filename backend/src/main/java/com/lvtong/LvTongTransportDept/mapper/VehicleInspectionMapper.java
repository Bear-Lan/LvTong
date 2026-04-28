package com.lvtong.LvTongTransportDept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lvtong.LvTongTransportDept.entity.VehicleInspection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 车辆查验记录 Mapper
 */
public interface VehicleInspectionMapper extends BaseMapper<VehicleInspection> {

    /**
     * SQL 聚合统计今日数据（代替 Java 内存遍历）
     * 【性能说明】
     * 直接在数据库层用 COUNT 聚合，只返回 3 行数字结果，
     * 不再把全量记录拉到 Java 内存遍历，效率提升 10 倍以上。
     */
    @Select("SELECT COUNT(*) AS total, " +
            "SUM(CASE WHEN result_status = 1 THEN 1 ELSE 0 END) AS passCount, " +
            "SUM(CASE WHEN result_status = 2 THEN 1 ELSE 0 END) AS failCount " +
            "FROM vehicle_inspections " +
            "WHERE inspection_time >= #{startTime} AND inspection_time < #{endTime}")
    Map<String, Object> selectTodayStats(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 按小时分组统计今日查验量（代替 Java 分组）
     * 【性能说明】
     * GROUP BY HOUR() 在数据库层聚合，只返回 24 行结果，
     * 而非 SELECT * 全表拉取后再 Java 分组。
     */
    @Select("SELECT HOUR(inspection_time) AS hour, COUNT(*) AS count " +
            "FROM vehicle_inspections " +
            "WHERE inspection_time >= #{startTime} AND inspection_time < #{endTime} " +
            "GROUP BY HOUR(inspection_time) ORDER BY hour")
    List<Map<String, Object>> selectHourlyDistribution(@Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 按天统计查验数量（用于最近一周/一月）
     */
    @Select("SELECT DATE(inspection_time) AS date, COUNT(*) AS count " +
            "FROM vehicle_inspections " +
            "WHERE inspection_time >= #{startTime} AND inspection_time < #{endTime} " +
            "GROUP BY DATE(inspection_time) ORDER BY date")
    List<Map<String, Object>> selectDailyDistribution(@Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 按货物类型统计记录数（代替 Java 分组）
     * 【JOIN 解析品种名】
     * goods_type 存储 product_code，可能包含多编码（如 "10601|10801"）。
     * 使用 SUBSTRING_INDEX 取第一个编码进行 JOIN，未匹配时显示原始编码。
     * 【优化】加入时间范围过滤，避免全表扫描。
     * 返回字段 goodsTypeName（品种名），前端直接使用。
     */
    @Select("<script>" +
            "SELECT " +
            "  COALESCE(ap.variety_name, vi.goods_type, '未填写') AS goodsTypeName, " +
            "  COUNT(*) AS count " +
            "FROM vehicle_inspections vi " +
            "LEFT JOIN agricultural_products ap " +
            "  ON ap.product_code = SUBSTRING_INDEX(vi.goods_type, '|', 1) " +
            "<where>" +
            "  <if test='startTime != null'> AND vi.inspection_time &gt;= #{startTime} </if>" +
            "  <if test='endTime != null'> AND vi.inspection_time &lt; #{endTime} </if>" +
            "</where>" +
            "GROUP BY ap.variety_name, vi.goods_type " +
            "ORDER BY count DESC LIMIT 50" +
            "</script>")
    List<Map<String, Object>> selectGoodsTypeStats(@Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 获取货物类别统计（查询所有数据，不限时间）
     * @return 货物品种名称和出现次数
     */
    @Select("SELECT " +
            "  COALESCE(ap.variety_name, vi.goods_type, '未填写') AS goodsTypeName, " +
            "  COUNT(*) AS count " +
            "FROM vehicle_inspections vi " +
            "LEFT JOIN agricultural_products ap " +
            "  ON ap.product_code = SUBSTRING_INDEX(vi.goods_type, '|', 1) " +
            "GROUP BY ap.variety_name, vi.goods_type " +
            "ORDER BY count DESC")
    List<Map<String, Object>> selectGoodsTypeStatsAll();

    /**
     * 获取货物类型统计（用于词云图，查询所有记录）
     * @return 货物品种名称和出现次数
     */
    @Select("SELECT " +
            "  COALESCE(ap.variety_name, vi.goods_type, '未填写') AS name, " +
            "  COUNT(*) AS count " +
            "FROM vehicle_inspections vi " +
            "LEFT JOIN agricultural_products ap " +
            "  ON ap.product_code = SUBSTRING_INDEX(vi.goods_type, '|', 1) " +
            "GROUP BY ap.variety_name, vi.goods_type " +
            "ORDER BY count DESC " +
            "LIMIT 70")
    List<Map<String, Object>> selectGoodsTypeStatsForCloud();

    /**
     * 获取大屏统计数据
     * @return 包含 今日通行车辆, 总绿通车辆, 总通行金额, 伪绿通车辆
     */
    @Select("SELECT " +
            "(SELECT COUNT(*) FROM vehicle_inspections WHERE DATE(inspection_time) = CURDATE()) AS todayTotal, " +
            "(SELECT COUNT(*) FROM vehicle_inspections) AS total, " +
            "(SELECT COALESCE(SUM(passcode_fee), 0) FROM vehicle_inspections) AS totalPassAmount, " +
            "(SELECT COUNT(*) FROM vehicle_inspections WHERE result_status = 2 AND nopass_type IN (21, 22, 23, 24)) AS fakeGreenCount")
    Map<String, Object> selectDatascreenStats();

    /**
     * 获取信用记录排行榜（合格次数最多的前3辆车）
     * @return 包含车牌号、合格次数、总次数、信用评分(10分制)
     */
    @Select("SELECT " +
            "plate_number AS plateNumber, " +
            "SUM(CASE WHEN result_status = 1 THEN 1 ELSE 0 END) AS passCount, " +
            "COUNT(*) AS totalCount, " +
            "ROUND(SUM(CASE WHEN result_status = 1 THEN 1 ELSE 0 END) * 10.0 / COUNT(*), 1) AS creditScore " +
            "FROM vehicle_inspections " +
            "GROUP BY plate_number " +
            "ORDER BY passCount DESC " +
            "LIMIT 3")
    List<Map<String, Object>> selectCreditRanking();

    /**
     * 获取信息总览
     * 绿通车/收割机数量、查验车次、绿通减免（合格）、追逃费用（不合格）、
     * 绿通减免、追逃费用
     */
    @Select("SELECT " +
            "COALESCE(SUM(CASE WHEN result_status = 1 THEN passcode_fee ELSE 0 END), 0) AS exemptFee, " +
            "COALESCE(SUM(CASE WHEN result_status = 2 THEN passcode_fee ELSE 0 END), 0) AS chaseFee " +
            "FROM vehicle_inspections " +
            "WHERE inspection_time >= #{startTime} AND inspection_time < #{endTime}")
    Map<String, Object> selectInfoOverview(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 按天统计查验数量（用于 Dashboard 时段分析）
     */
    @Select("SELECT DATE(inspection_time) AS label, COUNT(*) AS count " +
            "FROM vehicle_inspections " +
            "WHERE inspection_time >= #{startTime} AND inspection_time < #{endTime} " +
            "GROUP BY DATE(inspection_time) ORDER BY label")
    List<Map<String, Object>> selectTimeDistribution(@Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 按月统计查验数量（用于 Dashboard 年视图时段分析）
     */
    @Select("SELECT DATE_FORMAT(inspection_time, '%Y-%m') AS label, COUNT(*) AS count " +
            "FROM vehicle_inspections " +
            "WHERE inspection_time >= #{startTime} AND inspection_time < #{endTime} " +
            "GROUP BY DATE_FORMAT(inspection_time, '%Y-%m') ORDER BY label")
    List<Map<String, Object>> selectMonthlyDistribution(@Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 获取车型分布统计（横向条形图数据）
     */
    @Select("SELECT " +
            "vehicle_type AS type, " +
            "COUNT(*) AS count " +
            "FROM vehicle_inspections " +
            "WHERE inspection_time >= #{startTime} AND inspection_time < #{endTime} " +
            "GROUP BY vehicle_type ORDER BY count DESC")
    List<Map<String, Object>> selectVehicleTypeStats(@Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 获取免检比例统计
     * 免检定义：result_status = 1 (合格) 且 to_transportdept_state = 1 (上传成功)
     */
    @Select("SELECT " +
            "COUNT(*) AS total, " +
            "SUM(CASE WHEN result_status = 1 AND to_transportdept_state = 1 THEN 1 ELSE 0 END) AS exempt " +
            "FROM vehicle_inspections " +
            "WHERE inspection_time >= #{startTime} AND inspection_time < #{endTime}")
    Map<String, Object> selectExemptRate(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 按小时统计平均处理时长（秒）
     */
    @Select("SELECT HOUR(inspection_time) AS hour, " +
            "AVG(TIMESTAMPDIFF(SECOND, acceptance_time, inspection_time)) AS avgSeconds " +
            "FROM vehicle_inspections " +
            "WHERE acceptance_time IS NOT NULL AND inspection_time IS NOT NULL " +
            "AND inspection_time >= #{startTime} AND inspection_time < #{endTime} " +
            "GROUP BY HOUR(inspection_time) ORDER BY hour")
    List<Map<String, Object>> selectHourlyProcessTime(@Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 按天统计平均处理时长（秒）
     */
    @Select("SELECT DATE(inspection_time) AS label, " +
            "AVG(TIMESTAMPDIFF(SECOND, acceptance_time, inspection_time)) AS avgSeconds " +
            "FROM vehicle_inspections " +
            "WHERE acceptance_time IS NOT NULL AND inspection_time IS NOT NULL " +
            "AND inspection_time >= #{startTime} AND inspection_time < #{endTime} " +
            "GROUP BY DATE(inspection_time) ORDER BY label")
    List<Map<String, Object>> selectDailyProcessTime(@Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 按月统计平均处理时长（秒）
     */
    @Select("SELECT DATE_FORMAT(inspection_time, '%Y-%m') AS label, " +
            "AVG(TIMESTAMPDIFF(SECOND, acceptance_time, inspection_time)) AS avgSeconds " +
            "FROM vehicle_inspections " +
            "WHERE acceptance_time IS NOT NULL AND inspection_time IS NOT NULL " +
            "AND inspection_time >= #{startTime} AND inspection_time < #{endTime} " +
            "GROUP BY DATE_FORMAT(inspection_time, '%Y-%m') ORDER BY label")
    List<Map<String, Object>> selectMonthlyProcessTime(@Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 聚合统计平均处理时长（总时长 / 总次数）
     */
    @Select("SELECT " +
            "COUNT(*) AS totalCount, " +
            "AVG(TIMESTAMPDIFF(SECOND, acceptance_time, inspection_time)) AS avgSeconds " +
            "FROM vehicle_inspections " +
            "WHERE acceptance_time IS NOT NULL AND inspection_time IS NOT NULL " +
            "AND inspection_time >= #{startTime} AND inspection_time < #{endTime}")
    Map<String, Object> selectAvgProcessTime(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 按省份统计查验数量（通过 passcode_en_station_id 关联 station_info 获取省份）
     * 返回最大省份及其数量
     */
    @Select("SELECT " +
            "si.province AS provinceCode, " +
            "COUNT(*) AS count " +
            "FROM vehicle_inspections vi " +
            "INNER JOIN station_info si ON si.station_id = vi.passcode_en_station_id " +
            "WHERE vi.inspection_time >= #{startTime} AND vi.inspection_time < #{endTime} " +
            "AND si.province IS NOT NULL AND si.province != '' " +
            "GROUP BY si.province " +
            "ORDER BY count DESC")
    List<Map<String, Object>> selectProvinceStats(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 湖北省内站点统计查验数量（获取湖北省内出现次数最多的站点名称）
     */
    @Select("SELECT " +
            "si.station_name AS stationName, " +
            "COUNT(*) AS count " +
            "FROM vehicle_inspections vi " +
            "INNER JOIN station_info si ON si.station_id = vi.passcode_en_station_id " +
            "WHERE vi.inspection_time >= #{startTime} AND vi.inspection_time < #{endTime} " +
            "AND si.province = '42' " +
            "AND si.station_name IS NOT NULL AND si.station_name != '' " +
            "GROUP BY si.station_name " +
            "ORDER BY count DESC " +
            "LIMIT 1")
    List<Map<String, Object>> selectTopStationInHubei(@Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);
}
