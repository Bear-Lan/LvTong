package com.lvtong.LvTongTransportDept.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lvtong.LvTongTransportDept.entity.StationInfo;
import com.lvtong.LvTongTransportDept.mapper.StationInfoMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 省份缓存服务
 * 启动时从 station_info 表加载所有站点，建立 stationId → province 映射
 */
@Service
public class ProvinceCacheService {

    @Autowired
    private StationInfoMapper stationInfoMapper;

    /** station_id → province 代码 */
    private Map<String, String> stationToProvince = new HashMap<>();

    /** station_id → 省份中文名称 */
    private Map<String, String> stationToProvinceName = new HashMap<>();

    /** province 代码 → 省份中文名称 */
    private Map<String, String> provinceCodeToName = new HashMap<>();

    private static final Map<String, String> PROVINCE_CODE_MAP = Map.ofEntries(
            // 华北
            Map.entry("11", "北京市"),
            Map.entry("12", "天津市"),
            Map.entry("13", "河北省"),
            Map.entry("14", "山西省"),
            Map.entry("15", "内蒙古自治区"),
            // 东北
            Map.entry("21", "辽宁省"),
            Map.entry("22", "吉林省"),
            Map.entry("23", "黑龙江省"),
            // 华东
            Map.entry("31", "上海市"),
            Map.entry("32", "江苏省"),
            Map.entry("33", "浙江省"),
            Map.entry("34", "安徽省"),
            Map.entry("35", "福建省"),
            Map.entry("36", "江西省"),
            Map.entry("37", "山东省"),
            // 华中
            Map.entry("41", "河南省"),
            Map.entry("42", "湖北省"),
            Map.entry("43", "湖南省"),
            // 华南
            Map.entry("44", "广东省"),
            Map.entry("45", "广西壮族自治区"),
            Map.entry("46", "海南省"),
            // 西南
            Map.entry("50", "重庆市"),
            Map.entry("51", "四川省"),
            Map.entry("52", "贵州省"),
            Map.entry("53", "云南省"),
            Map.entry("54", "西藏自治区"),
            // 西北
            Map.entry("61", "陕西省"),
            Map.entry("62", "甘肃省"),
            Map.entry("63", "青海省"),
            Map.entry("64", "宁夏回族自治区"),
            Map.entry("65", "新疆维吾尔自治区"),
            // 港澳台
            Map.entry("71", "台湾省"),
            Map.entry("81", "香港特别行政区"),
            Map.entry("82", "澳门特别行政区")
    );

    @PostConstruct
    public void load() {
        // 预热省份代码→名称映射
        provinceCodeToName.putAll(PROVINCE_CODE_MAP);

        List<StationInfo> stations = stationInfoMapper.selectList(
                new LambdaQueryWrapper<StationInfo>().select(StationInfo::getStationId, StationInfo::getProvince)
        );

        for (StationInfo s : stations) {
            if (s.getStationId() == null || s.getProvince() == null) continue;
            stationToProvince.put(s.getStationId(), s.getProvince());
            // station_id → 省份中文名
            String provinceName = provinceCodeToName.getOrDefault(s.getProvince(), s.getProvince());
            stationToProvinceName.put(s.getStationId(), provinceName);
        }
    }

    /**
     * 根据入口站 ID 获取省份代码
     */
    public String getProvinceByStationId(String stationId) {
        if (stationId == null) return null;
        return stationToProvince.get(stationId);
    }

    /**
     * 根据入口站 ID 获取省份中文名称
     */
    public String getProvinceNameByStationId(String stationId) {
        if (stationId == null) return null;
        return stationToProvinceName.getOrDefault(stationId, stationToProvince.get(stationId));
    }

    /**
     * 省份代码 → 中文名称
     */
    public String getProvinceNameByCode(String code) {
        if (code == null) return null;
        return provinceCodeToName.getOrDefault(code, code);
    }
}