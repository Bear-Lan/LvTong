package com.lvtong.LvTongTransportDept.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 收费站站点信息实体
 * 用于根据 passcode_en_station_id 获取入口站点名称和省份信息
 */
@TableName("station_info")
public class StationInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stationId;

    private String stationName;

    private String province;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
}