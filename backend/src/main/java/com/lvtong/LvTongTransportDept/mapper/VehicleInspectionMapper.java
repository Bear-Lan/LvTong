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
}
