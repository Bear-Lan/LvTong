package com.lvtong.LvTongTransportDept.service;

import com.lvtong.LvTongTransportDept.constant.VehicleConstants;
import com.lvtong.LvTongTransportDept.entity.AgriculturalProduct;
import com.lvtong.LvTongTransportDept.entity.StationInfo;
import com.lvtong.LvTongTransportDept.mapper.AgriculturalProductMapper;
import com.lvtong.LvTongTransportDept.mapper.StationInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Autowired
    private StationInfoMapper stationInfoMapper;

    @Autowired
    private AgriculturalProductMapper agriculturalProductMapper;

    /**
     * 图片存储基础路径
     */
    @Value("${three-level-platform.local-dir:D:/three_platform}")
    private String imageBasePath;

    /**
     * 缓存已创建的表，避免重复检查
     */
    private final ConcurrentHashMap<String, Boolean> createdTables = new ConcurrentHashMap<>();

    /**
     * 站点ID列表缓存（10分钟过期）
     * key: "allStationIds", value: List of stationId
     */
    private final ConcurrentHashMap<String, Object> stationIdCache = new ConcurrentHashMap<>();
    private static final String STATION_ID_CACHE_KEY = "allStationIds";
    private static final long STATION_ID_CACHE_TTL_MS = 10 * 60 * 1000; // 10分钟

    /**
     * 获取所有站点ID列表（带10分钟缓存）
     */
    public List<String> getAllStationIds() {
        long now = System.currentTimeMillis();
        Object cached = stationIdCache.get(STATION_ID_CACHE_KEY);
        if (cached instanceof StationIdCache cache) {
            if (now - cache.timestamp < STATION_ID_CACHE_TTL_MS) {
                return cache.stationIds;
            }
        }
        List<String> stationIds = stationInfoMapper.selectList(null)
                .stream()
                .map(StationInfo::getStationId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toList());
        stationIdCache.put(STATION_ID_CACHE_KEY, new StationIdCache(stationIds, now));
        log.info("刷新站点ID缓存，共 {} 个站点", stationIds.size());
        return stationIds;
    }

    /**
     * 站点ID缓存对象
     */
    private static class StationIdCache {
        final List<String> stationIds;
        final long timestamp;
        StationIdCache(List<String> stationIds, long timestamp) {
            this.stationIds = stationIds;
            this.timestamp = timestamp;
        }
    }

    /**
     * 在单表执行查询并返回结果列表
     */
    private List<Map<String, Object>> executeQueryOnTable(String sql, String tableName, LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(String.format(sql, tableName))) {
            ps.setObject(1, startTime);
            ps.setObject(2, endTime);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnLabel(i), rs.getObject(i));
                    }
                    result.add(row);
                }
            }
        } catch (Exception e) {
            log.warn("查询表 {} 失败: {}", tableName, e.getMessage());
        }
        return result;
    }

    /**
     * 在单表执行聚合查询（无GROUP BY）
     */
    private Map<String, Object> executeAggregateOnTable(String sql, String tableName, LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(String.format(sql, tableName))) {
            ps.setObject(1, startTime);
            ps.setObject(2, endTime);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        result.put(metaData.getColumnLabel(i), rs.getObject(i));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("聚合查询表 {} 失败: {}", tableName, e.getMessage());
        }
        return result;
    }

    /**
     * 跨表聚合查询（无GROUP BY）
     * 用于 selectTodayStats, selectExemptRate, selectAvgProcessTime, selectAvgDetectionTime, selectInfoOverview
     */
    public Map<String, Object> crossTableAggregate(String sql, LocalDateTime startTime, LocalDateTime endTime) {
        List<String> stationIds = getAllStationIds();
        Map<String, Object> merged = new HashMap<>();
        for (String stationId : stationIds) {
            String tableName = getTableNameByStationId(stationId);
            Map<String, Object> row = executeAggregateOnTable(sql, tableName, startTime, endTime);
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                Object v1 = merged.get(key);
                Object v2 = entry.getValue();
                if (v1 == null) {
                    merged.put(key, v2);
                } else if (v1 instanceof Number && v2 instanceof Number) {
                    merged.put(key, ((Number) v1).doubleValue() + ((Number) v2).doubleValue());
                }
            }
        }
        return merged;
    }

    /**
     * 跨表GROUP BY查询（按时间维度）
     * 用于 selectHourlyDistribution, selectDailyDistribution, selectTimeDistribution, selectMonthlyDistribution
     */
    public List<Map<String, Object>> crossTableGroupBy(String sql, LocalDateTime startTime, LocalDateTime endTime) {
        List<String> stationIds = getAllStationIds();
        List<Map<String, Object>> allRows = new ArrayList<>();
        for (String stationId : stationIds) {
            String tableName = getTableNameByStationId(stationId);
            allRows.addAll(executeQueryOnTable(sql, tableName, startTime, endTime));
        }
        return allRows;
    }

    /**
     * 跨表GROUP BY查询（按字段维度）
     * 用于 selectVehicleTypeStats
     */
    public List<Map<String, Object>> crossTableGroupByField(String sql, LocalDateTime startTime, LocalDateTime endTime) {
        return crossTableGroupBy(sql, startTime, endTime);
    }

    /**
     * 跨表处理时长查询（按小时/天/月）
     */
    public List<Map<String, Object>> crossTableProcessTime(String sql, LocalDateTime startTime, LocalDateTime endTime) {
        return crossTableGroupBy(sql, startTime, endTime);
    }

    /**
     * 跨表查询信用排行（跨所有站点查，然后取前3）
     */
    public List<Map<String, Object>> crossTableCreditRanking(String sql, LocalDateTime startTime, LocalDateTime endTime) {
        List<String> stationIds = getAllStationIds();
        List<Map<String, Object>> allRows = new ArrayList<>();
        for (String stationId : stationIds) {
            String tableName = getTableNameByStationId(stationId);
            allRows.addAll(executeQueryOnTable(sql, tableName, startTime, endTime));
        }
        return allRows;
    }

    /**
     * 跨表货物类型统计
     * 先查所有站点的 goods_type 计数，再在 Java 中 JOIN agricultural_products
     */
    public List<Map<String, Object>> crossTableGoodsTypeStats(String sql, LocalDateTime startTime, LocalDateTime endTime) {
        List<String> stationIds = getAllStationIds();
        // 收集所有 goods_type 计数
        Map<String, Long> goodsTypeCounts = new HashMap<>();
        for (String stationId : stationIds) {
            String tableName = getTableNameByStationId(stationId);
            List<Map<String, Object>> rows = executeQueryOnTable(sql, tableName, startTime, endTime);
            for (Map<String, Object> row : rows) {
                String goodsType = row.get("goods_type") != null ? row.get("goods_type").toString() : "未填写";
                long count = row.get("count") != null ? ((Number) row.get("count")).longValue() : 0L;
                goodsTypeCounts.merge(goodsType, count, Long::sum);
            }
        }
        // 在 Java 中 JOIN agricultural_products 获取 product_type 和 variety_name
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, AgriculturalProduct> productCache = new HashMap<>();
        for (Map.Entry<String, Long> entry : goodsTypeCounts.entrySet()) {
            String goodsType = entry.getKey();
            Long count = entry.getValue();
            String productType;
            if (goodsType.contains("|")) {
                String[] codes = goodsType.split("\\|");
                StringBuilder sb = new StringBuilder();
                for (String code : codes) {
                    AgriculturalProduct p = getProductByCode(code.trim(), productCache);
                    if (p != null) {
                        if (sb.length() > 0) sb.append("|");
                        sb.append(p.getProductType());
                    }
                }
                productType = sb.length() > 0 ? sb.toString() : "未填写";
            } else {
                AgriculturalProduct p = getProductByCode(goodsType, productCache);
                productType = p != null ? p.getProductType() : "未填写";
            }
            Map<String, Object> item = new HashMap<>();
            item.put("productType", productType);
            item.put("count", count);
            result.add(item);
        }
        return result;
    }

    private AgriculturalProduct getProductByCode(String code, Map<String, AgriculturalProduct> cache) {
        if (code == null || code.isEmpty()) return null;
        AgriculturalProduct p = cache.get(code);
        if (p == null) {
            p = agriculturalProductMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgriculturalProduct>()
                            .eq(AgriculturalProduct::getProductCode, code));
            if (p != null) cache.put(code, p);
        }
        return p;
    }

    /**
     * 跨表货物类型统计（按品种名，用于信息卡片）
     */
    public List<Map<String, Object>> crossTableGoodsTypeStatsByVariety(String sql, LocalDateTime startTime, LocalDateTime endTime) {
        List<String> stationIds = getAllStationIds();
        Map<String, Long> varietyCounts = new HashMap<>();
        for (String stationId : stationIds) {
            String tableName = getTableNameByStationId(stationId);
            List<Map<String, Object>> rows = executeQueryOnTable(sql, tableName, startTime, endTime);
            for (Map<String, Object> row : rows) {
                String goodsType = row.get("goods_type") != null ? row.get("goods_type").toString() : "未填写";
                long count = row.get("count") != null ? ((Number) row.get("count")).longValue() : 0L;
                varietyCounts.merge(goodsType, count, Long::sum);
            }
        }
        // 在 Java 中 JOIN agricultural_products 获取 variety_name
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, AgriculturalProduct> productCache = new HashMap<>();
        for (Map.Entry<String, Long> entry : varietyCounts.entrySet()) {
            String goodsType = entry.getKey();
            Long count = entry.getValue();
            String varietyName;
            if (goodsType.contains("|")) {
                String[] codes = goodsType.split("\\|");
                StringBuilder sb = new StringBuilder();
                for (String code : codes) {
                    AgriculturalProduct p = getProductByCode(code.trim(), productCache);
                    if (p != null) {
                        if (sb.length() > 0) sb.append("|");
                        sb.append(p.getVarietyName());
                    }
                }
                varietyName = sb.length() > 0 ? sb.toString() : "未填写";
            } else {
                AgriculturalProduct p = getProductByCode(goodsType, productCache);
                varietyName = p != null ? p.getVarietyName() : "未填写";
            }
            Map<String, Object> item = new HashMap<>();
            item.put("goodsTypeName", varietyName);
            item.put("count", count);
            result.add(item);
        }
        return result;
    }

    /**
     * 跨表货物类型全量统计（不限时间）
     */
    public List<Map<String, Object>> crossTableGoodsTypeStatsAll(String sql) {
        List<String> stationIds = getAllStationIds();
        Map<String, Long> varietyCounts = new HashMap<>();
        for (String stationId : stationIds) {
            String tableName = getTableNameByStationId(stationId);
            List<Map<String, Object>> rows = executeQueryOnTableNoTime(sql, tableName);
            for (Map<String, Object> row : rows) {
                String goodsType = row.get("goods_type") != null ? row.get("goods_type").toString() : "未填写";
                long count = row.get("count") != null ? ((Number) row.get("count")).longValue() : 0L;
                varietyCounts.merge(goodsType, count, Long::sum);
            }
        }
        Map<String, AgriculturalProduct> productCache = new HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : varietyCounts.entrySet()) {
            String goodsType = entry.getKey();
            Long count = entry.getValue();
            String varietyName;
            if (goodsType.contains("|")) {
                String[] codes = goodsType.split("\\|");
                StringBuilder sb = new StringBuilder();
                for (String code : codes) {
                    AgriculturalProduct p = getProductByCode(code.trim(), productCache);
                    if (p != null) {
                        if (sb.length() > 0) sb.append("|");
                        sb.append(p.getVarietyName());
                    }
                }
                varietyName = sb.length() > 0 ? sb.toString() : "未填写";
            } else {
                AgriculturalProduct p = getProductByCode(goodsType, productCache);
                varietyName = p != null ? p.getVarietyName() : "未填写";
            }
            Map<String, Object> item = new HashMap<>();
            item.put("goodsTypeName", varietyName);
            item.put("count", count);
            result.add(item);
        }
        return result;
    }

    /**
     * 跨表货物类型词云统计
     */
    public List<Map<String, Object>> crossTableGoodsTypeStatsForCloud(String sql) {
        return crossTableGoodsTypeStatsAll(sql);
    }

    /**
     * 跨表货物类型饼图统计（按大类）
     */
    public List<Map<String, Object>> crossTableGoodsTypeStatsByCategory(String sql) {
        List<String> stationIds = getAllStationIds();
        Map<String, Long> typeCounts = new HashMap<>();
        for (String stationId : stationIds) {
            String tableName = getTableNameByStationId(stationId);
            List<Map<String, Object>> rows = executeQueryOnTableNoTime(sql, tableName);
            for (Map<String, Object> row : rows) {
                String goodsType = row.get("goods_type") != null ? row.get("goods_type").toString() : "未填写";
                long count = row.get("count") != null ? ((Number) row.get("count")).longValue() : 0L;
                typeCounts.merge(goodsType, count, Long::sum);
            }
        }
        Map<String, AgriculturalProduct> productCache = new HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : typeCounts.entrySet()) {
            String goodsType = entry.getKey();
            Long count = entry.getValue();
            String productType;
            if (goodsType.contains("|")) {
                String[] codes = goodsType.split("\\|");
                String firstCode = codes[0].trim();
                AgriculturalProduct p = getProductByCode(firstCode, productCache);
                productType = p != null ? p.getProductType() : "未填写";
            } else {
                AgriculturalProduct p = getProductByCode(goodsType, productCache);
                productType = p != null ? p.getProductType() : "未填写";
            }
            Map<String, Object> item = new HashMap<>();
            item.put("name", productType);
            item.put("count", count);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> executeQueryOnTableNoTime(String sql, String tableName) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(String.format(sql, tableName))) {
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnLabel(i), rs.getObject(i));
                    }
                    result.add(row);
                }
            }
        } catch (Exception e) {
            log.warn("查询表 {} 失败: {}", tableName, e.getMessage());
        }
        return result;
    }

    /**
     * 跨表省份统计
     */
    public List<Map<String, Object>> crossTableProvinceStats(String sql, LocalDateTime startTime, LocalDateTime endTime) {
        List<String> stationIds = getAllStationIds();
        Map<String, Long> stationCounts = new HashMap<>();
        for (String stationId : stationIds) {
            String tableName = getTableNameByStationId(stationId);
            List<Map<String, Object>> rows = executeQueryOnTable(sql, tableName, startTime, endTime);
            for (Map<String, Object> row : rows) {
                String enStationId = row.get("passcode_en_station_id") != null ? row.get("passcode_en_station_id").toString() : null;
                if (enStationId != null && !enStationId.isEmpty()) {
                    long count = row.get("count") != null ? ((Number) row.get("count")).longValue() : 0L;
                    stationCounts.merge(enStationId, count, Long::sum);
                }
            }
        }
        // 汇总按省份
        Map<String, Long> provinceCounts = new HashMap<>();
        for (Map.Entry<String, Long> entry : stationCounts.entrySet()) {
            String province = provinceCacheService.getProvinceByStationId(entry.getKey());
            if (province != null && !province.isEmpty() && !"42".equals(province)) {
                provinceCounts.merge(province, entry.getValue(), Long::sum);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : provinceCounts.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("provinceCode", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    /**
     * 跨表湖北省内站点统计
     */
    public List<Map<String, Object>> crossTableTopStationInHubei(String sql, LocalDateTime startTime, LocalDateTime endTime) {
        List<String> stationIds = getAllStationIds();
        Map<String, Long> hubeiStationCounts = new HashMap<>();
        for (String stationId : stationIds) {
            String province = provinceCacheService.getProvinceByStationId(stationId);
            if (!"42".equals(province)) continue;
            String tableName = getTableNameByStationId(stationId);
            List<Map<String, Object>> rows = executeQueryOnTable(sql, tableName, startTime, endTime);
            for (Map<String, Object> row : rows) {
                String stationName = row.get("station_name") != null ? row.get("station_name").toString() : null;
                if (stationName != null && !stationName.isEmpty()) {
                    long count = row.get("count") != null ? ((Number) row.get("count")).longValue() : 0L;
                    hubeiStationCounts.merge(stationName, count, Long::sum);
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : hubeiStationCounts.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("stationName", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    /**
     * 跨表省份统计（不限时间）
     */
    public List<Map<String, Object>> crossTableProvinceStatsAll(String sql) {
        List<String> stationIds = getAllStationIds();
        Map<String, Long> stationCounts = new HashMap<>();
        for (String stationId : stationIds) {
            String tableName = getTableNameByStationId(stationId);
            List<Map<String, Object>> rows = executeQueryOnTableNoTime(sql, tableName);
            for (Map<String, Object> row : rows) {
                String enStationId = row.get("passcode_en_station_id") != null ? row.get("passcode_en_station_id").toString() : null;
                if (enStationId != null && !enStationId.isEmpty()) {
                    long count = row.get("count") != null ? ((Number) row.get("count")).longValue() : 0L;
                    stationCounts.merge(enStationId, count, Long::sum);
                }
            }
        }
        Map<String, Long> provinceCounts = new HashMap<>();
        for (Map.Entry<String, Long> entry : stationCounts.entrySet()) {
            String province = provinceCacheService.getProvinceByStationId(entry.getKey());
            if (province != null && !province.isEmpty()) {
                provinceCounts.merge(province, entry.getValue(), Long::sum);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : provinceCounts.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("provinceCode", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    /**
     * 跨表大屏统计（4个子查询）
     */
    public Map<String, Object> crossTableDatascreenStats() {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        // 今日查验数量
        String todaySql = "SELECT COUNT(*) AS cnt FROM %s WHERE DATE(inspection_time) = CURDATE()";
        double todayTotal = 0;
        for (String stationId : getAllStationIds()) {
            Map<String, Object> row = executeAggregateOnTable(todaySql, getTableNameByStationId(stationId), startOfDay, endOfDay);
            Object cnt = row.get("cnt");
            if (cnt instanceof Number) todayTotal += ((Number) cnt).doubleValue();
        }
        result.put("todayTotal", (long) todayTotal);

        // 总查验数量
        String totalSql = "SELECT COUNT(*) AS cnt FROM %s";
        double total = 0;
        for (String stationId : getAllStationIds()) {
            Map<String, Object> row = executeAggregateOnTable(totalSql, getTableNameByStationId(stationId), startOfDay, endOfDay);
            Object cnt = row.get("cnt");
            if (cnt instanceof Number) total += ((Number) cnt).doubleValue();
        }
        result.put("total", (long) total);

        // 总通行费用
        String feeSql = "SELECT COALESCE(SUM(passcode_fee), 0) AS totalFee FROM %s";
        double totalFee = 0;
        for (String stationId : getAllStationIds()) {
            Map<String, Object> row = executeAggregateOnTable(feeSql, getTableNameByStationId(stationId), startOfDay, endOfDay);
            Object fee = row.get("totalFee");
            if (fee instanceof Number) totalFee += ((Number) fee).doubleValue();
        }
        result.put("totalPassAmount", totalFee);

        // 伪绿通数量
        String fakeSql = "SELECT COUNT(*) AS cnt FROM %s WHERE result_status = 2 AND nopass_type IN (21, 22, 23, 24)";
        double fakeCount = 0;
        for (String stationId : getAllStationIds()) {
            Map<String, Object> row = executeAggregateOnTable(fakeSql, getTableNameByStationId(stationId), startOfDay, endOfDay);
            Object cnt = row.get("cnt");
            if (cnt instanceof Number) fakeCount += ((Number) cnt).doubleValue();
        }
        result.put("fakeGreenCount", (long) fakeCount);

        return result;
    }

    /**
     * 跨表查询最近记录
     */
    public List<Map<String, Object>> crossTableRecentRecords(int limit) {
        List<String> stationIds = getAllStationIds();
        List<Map<String, Object>> allRecords = new ArrayList<>();
        String sql = "SELECT * FROM %s ORDER BY inspection_time DESC LIMIT %d";
        for (String stationId : stationIds) {
            String tableName = getTableNameByStationId(stationId);
            allRecords.addAll(executeQueryOnTableNoTime(String.format(sql, tableName, limit), tableName));
        }
        // 按 inspection_time 降序排序，取前 limit 条
        allRecords.sort((a, b) -> {
            Object t1 = a.get("inspection_time");
            Object t2 = b.get("inspection_time");
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.toString().compareTo(t1.toString());
        });
        return allRecords.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 跨表条件查询（用于 searchWithConditions 和 searchForExport）
     * 查询所有站点表，收集所有记录后按 inspection_time 排序
     * 注意：这个方法会查询所有站点的所有数据，对于大数据量性能较差
     */
    public List<Map<String, Object>> crossTableSearch(
            String plateNumber,
            String driverPhone,
            String operatorName,
            String reviewerPhone,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer resultStatus,
            Integer manualReviewState,
            Integer toTransportdeptState) {
        List<String> stationIds = getAllStationIds();
        List<Map<String, Object>> allRecords = new ArrayList<>();

        for (String stationId : stationIds) {
            String tableName = getTableNameByStationId(stationId);
            List<Map<String, Object>> rows = executeSearchOnTable(tableName, plateNumber, driverPhone,
                    operatorName, reviewerPhone, startTime, endTime, resultStatus, manualReviewState, toTransportdeptState);
            allRecords.addAll(rows);
        }

        // 按 inspection_time 降序排序
        allRecords.sort((a, b) -> {
            Object t1 = a.get("inspection_time");
            Object t2 = b.get("inspection_time");
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.toString().compareTo(t1.toString());
        });

        return allRecords;
    }

    private List<Map<String, Object>> executeSearchOnTable(
            String tableName,
            String plateNumber,
            String driverPhone,
            String operatorName,
            String reviewerPhone,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer resultStatus,
            Integer manualReviewState,
            Integer toTransportdeptState) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(tableName).append(" WHERE 1=1");

        List<Object> params = new ArrayList<>();
        int paramIndex = 0;

        if (plateNumber != null && !plateNumber.isEmpty()) {
            sql.append(" AND plate_number LIKE ?");
            params.add("%" + plateNumber + "%");
        }
        if (driverPhone != null && !driverPhone.isEmpty()) {
            sql.append(" AND driver_phone = ?");
            params.add(driverPhone);
        }
        if (operatorName != null && !operatorName.isEmpty()) {
            sql.append(" AND operator_name = ?");
            params.add(operatorName);
        }
        if (reviewerPhone != null && !reviewerPhone.isEmpty()) {
            sql.append(" AND reviewer_phone = ?");
            params.add(reviewerPhone);
        }
        if (startTime != null) {
            sql.append(" AND inspection_time >= ?");
            params.add(startTime);
        }
        if (endTime != null) {
            sql.append(" AND inspection_time < ?");
            params.add(endTime);
        }
        if (resultStatus != null) {
            sql.append(" AND result_status = ?");
            params.add(resultStatus);
        }
        if (manualReviewState != null) {
            sql.append(" AND manual_review_state = ?");
            params.add(manualReviewState);
        }
        if (toTransportdeptState != null) {
            sql.append(" AND to_transportdept_state = ?");
            params.add(toTransportdeptState);
        }

        sql.append(" ORDER BY inspection_time DESC");

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnLabel(i), rs.getObject(i));
                    }
                    result.add(row);
                }
            }
        } catch (Exception e) {
            log.warn("查询表 {} 失败: {}", tableName, e.getMessage());
        }
        return result;
    }

    /**
     * 跨表查询待审核列表（result_status=2 且 manual_review_state=0 或 null）
     */
    public List<Map<String, Object>> crossTablePendingReview(int limit) {
        List<String> stationIds = getAllStationIds();
        List<Map<String, Object>> allRecords = new ArrayList<>();

        for (String stationId : stationIds) {
            String tableName = getTableNameByStationId(stationId);
            String sql = String.format(
                    "SELECT * FROM %s WHERE result_status = 2 AND (manual_review_state = 0 OR manual_review_state IS NULL) ORDER BY inspection_time DESC LIMIT %d",
                    tableName, limit);
            allRecords.addAll(executeQueryOnTableNoTime(sql, tableName));
        }

        // 合并后排序取前 limit 条
        allRecords.sort((a, b) -> {
            Object t1 = a.get("inspection_time");
            Object t2 = b.get("inspection_time");
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.toString().compareTo(t1.toString());
        });

        return allRecords.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 跨表查询伪绿通列表（result_status=2 且 nopass_type=21）
     */
    public List<Map<String, Object>> crossTableFakeGreen(int limit) {
        List<String> stationIds = getAllStationIds();
        List<Map<String, Object>> allRecords = new ArrayList<>();

        for (String stationId : stationIds) {
            String tableName = getTableNameByStationId(stationId);
            String sql = String.format(
                    "SELECT * FROM %s WHERE result_status = 2 AND nopass_type = 21 ORDER BY inspection_time DESC LIMIT %d",
                    tableName, limit);
            allRecords.addAll(executeQueryOnTableNoTime(sql, tableName));
        }

        allRecords.sort((a, b) -> {
            Object t1 = a.get("inspection_time");
            Object t2 = b.get("inspection_time");
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.toString().compareTo(t1.toString());
        });

        return allRecords.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 在指定表中按 ID 查询单条记录
     */
    public Map<String, Object> selectByIdOnTable(String tableName, Integer id) {
        String sql = String.format("SELECT * FROM %s WHERE id = ?", tableName);
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnLabel(i), rs.getObject(i));
                    }
                    result.add(row);
                }
            }
            return result.isEmpty() ? null : result.get(0);
        } catch (Exception e) {
            log.warn("查询表 {} id={} 失败: {}", tableName, id, e.getMessage());
            return null;
        }
    }

    /**
     * 在指定表中按 ID 更新记录
     */
    public void updateOnTable(String tableName, Integer id, Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) return;
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(tableName).append(" SET ");
        List<Object> params = new ArrayList<>();
        boolean first = true;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (first) first = false;
            else sql.append(", ");
            sql.append(entry.getKey()).append(" = ?");
            params.add(entry.getValue());
        }
        sql.append(" WHERE id = ?");
        params.add(id);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.executeUpdate();
            log.info("更新表 {} id={} 成功", tableName, id);
        } catch (Exception e) {
            log.warn("更新表 {} id={} 失败: {}", tableName, id, e.getMessage());
            throw new RuntimeException("更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据 stationId 获取表名（直接使用 stationId 作为表名）
     */
    public String getTableNameByStationId(String stationId) {
        if (stationId == null || stationId.isEmpty()) {
            return null;
        }
        return stationId;
    }

    /**
     * 保存Base64图片到本地磁盘
     * 路径结构: basePath/inspectionDate/fieldName/
     * 文件名: driverPhone_inspectionTime.extension
     * @param base64Data Base64编码的图片数据
     * @param driverPhone 司机电话（用于文件名）
     * @param inspectionTime 查验时间（用于文件夹和文件名）
     * @param fieldName 字段名（用于子目录）
     * @param index 多图片时的序号（单图片传0）
     * @return 保存后的文件路径
     */
    public String saveImage(String base64Data, String driverPhone, String inspectionTime, String fieldName, int index) {
        if (base64Data == null || base64Data.isEmpty()) {
            return null;
        }
        try {
            // 去掉Base64前缀（如果有）
            String pureBase64 = base64Data;
            if (base64Data.contains(",")) {
                pureBase64 = base64Data.substring(base64Data.indexOf(",") + 1);
            }

            // 解码
            byte[] imageBytes = Base64.getDecoder().decode(pureBase64);

            // 确定文件扩展名（默认jpg）
            String extension = "jpg";
            if (base64Data.contains("image/png")) {
                extension = "png";
            } else if (base64Data.contains("image/gif")) {
                extension = "gif";
            }

            // 格式化时间：yyyy-MM-dd HH:mm:ss -> yyyyMMddHHmmss
            String timeStr = inspectionTime.replace("-", "").replace(" ", "").replace(":", "").substring(0, 14);
            // 清理司机电话中的非数字字符
            String phone = driverPhone != null ? driverPhone.replaceAll("[^0-9]", "") : "UNKNOWN";
            // 生成文件名
            String fileName;
            if (index > 0) {
                fileName = phone + "_" + timeStr + "_" + index + "." + extension;
            } else {
                fileName = phone + "_" + timeStr + "." + extension;
            }

            // 创建目录: basePath/inspectionDate/fieldName/
            String dateStr = inspectionTime.substring(0, 10); // yyyy-MM-dd
            Path imageDir = Paths.get(imageBasePath, dateStr, fieldName);
            if (!Files.exists(imageDir)) {
                Files.createDirectories(imageDir);
            }

            // 保存文件
            Path filePath = imageDir.resolve(fileName);
            Files.write(filePath, imageBytes);

            log.info("图片保存成功: {}", filePath);
            return filePath.toString().replace("\\", "/");

        } catch (IOException e) {
            log.error("保存图片失败: {}", e.getMessage());
            throw new IllegalArgumentException("保存图片失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Base64解码失败: {}", e.getMessage());
            throw new IllegalArgumentException("Base64图片格式错误: " + e.getMessage());
        }
    }

    /**
     * 处理图片字段：如果值是Base64格式则保存到本地，否则直接返回路径
     */
    private String processImageField(String imageData, String tableName, String prefix, String driverPhone, String inspectionTime) {
        if (imageData == null || imageData.isEmpty()) {
            return null;
        }
        // 判断是否是Base64格式（以data:image开头或有base64标志）
        if (imageData.startsWith("data:image") || (imageData.length() > 100 && !imageData.contains("/") && !imageData.contains("\\"))) {
            return saveImage(imageData, driverPhone, inspectionTime, prefix, 0);
        }
        // 否则直接返回（可能是已有路径）
        return imageData;
    }

    /**
     * 处理多图片字段（List格式）
     * @param imageList 图片Base64列表
     */
    private List<String> processMultiImageField(List<String> imageList, String tableName, String prefix, String driverPhone, String inspectionTime) {
        if (imageList == null || imageList.isEmpty()) {
            return null;
        }
        List<String> result = new java.util.ArrayList<>();
        int imgIndex = 1;
        for (String imageData : imageList) {
            if (imageData == null || imageData.isEmpty()) continue;
            try {
                String savedPath = saveImage(imageData, driverPhone, inspectionTime, prefix, imgIndex);
                if (savedPath != null) {
                    result.add(savedPath);
                    imgIndex++;
                }
            } catch (Exception e) {
                log.warn("解析第{}张图片失败，跳过: {}", imgIndex, e.getMessage());
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * 将图片List转为逗号分隔的字符串
     */
    private String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return String.join(",", list);
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

        // 保存Base64图片（如果字段值是Base64格式）
        // 文件名: driverPhone_inspectionTime.extension
        // 路径: basePath/inspectionDate/fieldName/
        String driverPhone = record.driverPhone != null ? record.driverPhone : "";
        String inspectionTime = record.inspectionTime != null ? record.inspectionTime : "";
        record.bodyImagePath = processImageField(record.bodyImagePath, tableName, "body", driverPhone, inspectionTime);
        record.transparentImagePath = processImageField(record.transparentImagePath, tableName, "transparent", driverPhone, inspectionTime);
        record.headImagePath = processImageField(record.headImagePath, tableName, "head", driverPhone, inspectionTime);
        record.tailImagePath = processImageField(record.tailImagePath, tableName, "tail", driverPhone, inspectionTime);
        record.topImagePath = processImageField(record.topImagePath, tableName, "top", driverPhone, inspectionTime);
        record.goodsImagePath = processMultiImageField(record.goodsImagePath, tableName, "goods", driverPhone, inspectionTime);
        record.evidencesImagePath = processMultiImageField(record.evidencesImagePath, tableName, "evidences", driverPhone, inspectionTime);
        record.licenseImagePath = processImageField(record.licenseImagePath, tableName, "license", driverPhone, inspectionTime);
        record.passcodeImagePath = processImageField(record.passcodeImagePath, tableName, "passcode", driverPhone, inspectionTime);

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
        ps.setString(idx++, listToString(record.goodsImagePath));
        ps.setString(idx++, listToString(record.evidencesImagePath));
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
        public List<String> goodsImagePath;
        public List<String> evidencesImagePath;
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