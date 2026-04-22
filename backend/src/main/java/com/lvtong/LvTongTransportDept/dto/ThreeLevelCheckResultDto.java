package com.lvtong.LvTongTransportDept.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 三级平台查验结果接收 DTO
 * 继承交通部DTO，仅新增数据库有但交通部没有的字段
 *
 * 【照片 typeId 策略】
 * 1-通行凭证照片, 2-透视影像, 3-车身照, 4-证据链照片, 24-货物照
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({
        // 基础信息
        "checkId", "vehicleSign", "vehicleClass", "driverTelephone",
        "vehicleId", "freightTypes", "vehicleType", "crateType",
        "weightCheckBasis", "enWeight", "exWeight", "loadRate", "loadWeight",
        "vehicleSize", "checkTime", "enStationId", "exStationId",
        "groupId", "inspector", "reviewer", "checkResult", "reason",
        "mediaType", "transactionId", "passId", "exTime",
        "transPayType", "fee", "payFee", "provinceCount", "memo", "operation",

        // 三级平台新增字段（数据库有但交通部没有）
        "plateNumberGc", "goodsCategory",

        // 图片列表
        "photos"
})
public class ThreeLevelCheckResultDto extends TransportDeptCheckResultDto {

    // ================================================================
    // 三级平台新增字段（数据库有但交通部没有）
    // ================================================================

    /** 挂车号码 */
    private String plateNumberGc;

    /** 货物小类别 */
    private String goodsCategory;

    /** 装载重量（吨） */
    private Double loadWeight;
}