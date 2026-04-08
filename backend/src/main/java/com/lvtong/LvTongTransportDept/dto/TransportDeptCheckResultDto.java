package com.lvtong.LvTongTransportDept.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;

/**
 * 交通局查验结果上报 DTO
 *
 * 【设计说明】
 * 专门用于与交通局平台的 JSON 格式对齐，字段名严格匹配对方接口规范。
 * 独立于此系统的内部实体（VehicleInspection），避免业务模型与外部接口耦合。
 */
@Data
@JsonPropertyOrder({
        "groupId", "fee", "weightCheckBasis", "memo", "checkResult", "photos", "payFee",
        "driverTelephone", "vehicleClass", "vehicleId", "vehicleSign", "vehicleType",
        "freightTypes", "loadRate", "passId", "mediaType", "inspector", "reviewer",
        "enStationId", "transactionId", "exStationId", "exWeight", "checkTime",
        "provinceCount", "exTime", "transPayType", "checkId", "operation", "crateType"
})
public class TransportDeptCheckResultDto {

    // ================================================================
    // 基础信息
    // ================================================================

    /** 查验ID（格式：手机号_yyyyMMddHHmmss_手机号） */
    private String checkId;

    /** 车辆标识（0x02=绿通车，0x03=收割机） */
    private String vehicleSign;

    /** 车种分类（2=绿通车，3=联合收割机） */
    private Integer vehicleClass;

    /** 司机联系电话 */
    private String driverTelephone;

    /** 车牌颜色（车牌_颜色） */
    private String vehicleId;

    /** 货物类型编码（支持多编码，逗号分隔） */
    private String freightTypes;

    /** 车辆类型（"11"~"16"，对应一型~六型货车） */
    private String vehicleType;

    /** 货箱类型 */
    private String crateType;

    /** 重量检测依据（1=动态称重，2=静态称重，3=目测） */
    private String weightCheckBasis;

    /** 入口重量(KG) */
    private Integer enWeight;

    /** 出口重量(KG) */
    private Integer exWeight;

    /** 装载率(%) */
    private Double loadRate;

    /** 车辆外廓尺寸 */
    private String vehicleSize;

    /** 查验时间 */
    private String checkTime;

    /** 入口站编号 */
    private String enStationId;

    /** 出口站编号 */
    private String exStationId;

    /** 班组编号 */
    private Integer groupId;

    /** 查验员联系电话 */
    private String inspector;

    /** 复核员联系电话 */
    private String reviewer;

    /** 查验结果（1=合格，2=不合格） */
    private Integer checkResult;

    /** 不合格原因（合格时为空） */
    private String reason;

    /** 通行介质类型 */
    private Integer mediaType;

    /** 交易流水号 */
    private String transactionId;

    /** 通行标识ID */
    private String passId;

    /** 出口交易时间 */
    private String exTime;

    /** 支付方式 */
    private Integer transPayType;

    /** 通行费（分） */
    private Long fee;

    /** 实付金额（分） */
    private Long payFee;

    /** 通行省份数量 */
    private Integer provinceCount;

    /** 备注 */
    private String memo;

    /** 操作类型（1=新增，2=修改，3=删除） */
    private Integer operation;

    // ================================================================
    // 图片列表
    // ================================================================

    /** 证据照片列表 */
    private List<PhotoItem> photos;

    // ================================================================
    // 内部类：照片项
    // ================================================================

    @Data
    public static class PhotoItem {
        /*
           (必填：是)
           照片编号,最多 20 位字符
         */
        public String id;

        public String typeId;

        public String content;

        public String time;

        public String position;



    }
}
