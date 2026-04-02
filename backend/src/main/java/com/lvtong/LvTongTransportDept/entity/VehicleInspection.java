package com.lvtong.LvTongTransportDept.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车辆查验记录实体（MyBatis Plus 版本）
 * 【全局配置效果】
 * MyBatis Plus 全局配置了 map-underscore-to-camel-case=true，
 * 数据库 plate_number → Java plateNumber，无需手动 @TableField 标注。
 */
@TableName("vehicle_inspections")
public class VehicleInspection {

    // ================================================================
    // 主键
    // ================================================================
    @TableId(type = IdType.AUTO)
    private Integer id;

    // ================================================================
    // 基础信息
    // ================================================================
    /** 车牌号码（必填） */
    private String plateNumber;

    /** 车牌颜色 */
    @TableField("plate_number_gc")
    private String plateNumberGc;

    /** 司机电话 */
    @TableField("driver_phone")
    private String driverPhone;

    // ================================================================
    // 车辆信息
    // ================================================================
    /** 车辆类型（"11"~"16"，对应一型~六型货车） */
    @TableField("vehicle_type")
    private String vehicleType;

    /** 货箱类型（"1", "2.1", "3.1" 等） */
    @TableField("vehicle_containertype")
    private String vehicleContainertype;

    // ================================================================
    // 货物信息
    // ================================================================
    @TableField("goods_type")
    private String goodsType;

    /** 货物小类别（如叶菜类、瓜类） */
    @TableField("goods_category")
    private String goodsCategory;

    /** 装载率（%，BigDecimal 保留两位小数） */
    @TableField("load_rate")
    private BigDecimal loadRate;

    /** 装载重量（吨，BigDecimal 保留两位小数） */
    @TableField("load_weight")
    private BigDecimal loadWeight;

    /** 车辆外廓尺寸（长×宽×高，单位 mm） */
    @TableField("vehicle_size")
    private String vehicleSize;

    /** 历史查验记录（TEXT） */
    @TableField("history_record")
    private String historyRecord;

    // ================================================================
    // 证据图片路径（共 7 张）
    // ================================================================
    /** 车侧照片 */
    @TableField("body_image_path")
    private String bodyImagePath;

    /** 透视影像（ETC X光扫描图） */
    @TableField("transparent_image_path")
    private String transparentImagePath;

    /** 车头照片 */
    @TableField("head_image_path")
    private String headImagePath;

    /** 车尾照片 */
    @TableField("tail_image_path")
    private String tailImagePath;

    /** 车顶照片 */
    @TableField("top_image_path")
    private String topImagePath;

    /** 货物照片 */
    @TableField("goods_image_path")
    private String goodsImagePath;

    /** 行驶证照片 */
    @TableField("license_image_path")
    private String licenseImagePath;

    /** 通行凭证照片 */
    @TableField("passcode_image_path")
    private String passcodeImagePath;

    // ================================================================
    // 通行码信息（ETC 通行卡数据）
    // ================================================================
    @TableField("passcode_vehicle_id")
    private String passcodeVehicleId;

    @TableField("passcode_vehicle_display_id")
    private String passcodeVehicleDisplayId;

    @TableField("passcode_vehicle_color_name")
    private String passcodeVehicleColorName;

    @TableField("passcode_en_station_id")
    private String passcodeEnStationId;

    @TableField("passcode_ex_station_id")
    private String passcodeExStationId;

    @TableField("passcode_en_weight")
    private String passcodeEnWeight;

    @TableField("passcode_ex_weight")
    private String passcodeExWeight;

    @TableField("passcode_media_type")
    private String passcodeMediaType;

    @TableField("passcode_transaction_id")
    private String passcodeTransactionId;

    @TableField("passcode_pass_id")
    private String passcodePassId;

    @TableField("passcode_ex_time")
    private String passcodeExTime;

    @TableField("passcode_trans_pay_type")
    private String passcodeTransPayType;

    @TableField("passcode_fee")
    private String passcodeFee;

    @TableField("passcode_pay_fee")
    private String passcodePayFee;

    @TableField("passcode_vehicle_sign")
    private String passcodeVehicleSign;

    @TableField("passcode_province_count")
    private String passcodeProvinceCount;

    // ================================================================
    // 查验业务信息
    // ================================================================
    /** 查验操作员姓名 */
    @TableField("operator_name")
    private String operatorName;

    /** 查验时间 */
    @TableField("inspection_time")
    private LocalDateTime inspectionTime;

    /** 记录创建时间（MP 自动填充） */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /** 记录更新时间（MP 自动填充） */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 查验结果
     * 0=待查验，1=合格，2=不合格
     */
    @TableField("result_status")
    private Integer resultStatus;

    /**
     * 上传交通部状态：-1=上传失败，0=未上传，1=上传成功
     */
    @TableField("to_transportdept_state")
    private Integer toTransportdeptState;

    /**
     * 上传交通部时间
     */
    @TableField("to_transportdept_time")
    private LocalDateTime toTransportdeptTime;

    /**
     * 上传交通部备注
     */
    @TableField("to_transportdept_comment")
    private String toTransportdeptComment;

    /**
     * 不合格类型（绿通 11-26，收割机 31-42）
     */
    @TableField("nopass_type")
    private Integer nopassType;

    /**
     * 状态标记：0=正常，-1=已删除（物理删除）
     */
    private Integer status;

    /** 班组编号 */
    @TableField("group_id")
    private String groupId;

    /** 查验员联系电话 */
    @TableField("inspector_phone")
    private String inspectorPhone;

    /** 复核员联系电话 */
    @TableField("reviewer_phone")
    private String reviewerPhone;

    /** 人工复核状态：0=未复核，1=已复核 */
    @TableField("manual_review_state")
    private Integer manualReviewState;

    // ================================================================
    // Getter & Setter
    // ================================================================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public String getPlateNumberGc() { return plateNumberGc; }
    public void setPlateNumberGc(String plateNumberGc) { this.plateNumberGc = plateNumberGc; }

    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getVehicleContainertype() { return vehicleContainertype; }
    public void setVehicleContainertype(String vehicleContainertype) { this.vehicleContainertype = vehicleContainertype; }

    public String getGoodsType() { return goodsType; }
    public void setGoodsType(String goodsType) { this.goodsType = goodsType; }

    public String getGoodsCategory() { return goodsCategory; }
    public void setGoodsCategory(String goodsCategory) { this.goodsCategory = goodsCategory; }

    public BigDecimal getLoadRate() { return loadRate; }
    public void setLoadRate(BigDecimal loadRate) { this.loadRate = loadRate; }

    public BigDecimal getLoadWeight() { return loadWeight; }
    public void setLoadWeight(BigDecimal loadWeight) { this.loadWeight = loadWeight; }

    public String getVehicleSize() { return vehicleSize; }
    public void setVehicleSize(String vehicleSize) { this.vehicleSize = vehicleSize; }

    public String getHistoryRecord() { return historyRecord; }
    public void setHistoryRecord(String historyRecord) { this.historyRecord = historyRecord; }

    public String getBodyImagePath() { return bodyImagePath; }
    public void setBodyImagePath(String bodyImagePath) { this.bodyImagePath = bodyImagePath; }

    public String getTransparentImagePath() { return transparentImagePath; }
    public void setTransparentImagePath(String transparentImagePath) { this.transparentImagePath = transparentImagePath; }

    public String getHeadImagePath() { return headImagePath; }
    public void setHeadImagePath(String headImagePath) { this.headImagePath = headImagePath; }

    public String getTailImagePath() { return tailImagePath; }
    public void setTailImagePath(String tailImagePath) { this.tailImagePath = tailImagePath; }

    public String getTopImagePath() { return topImagePath; }
    public void setTopImagePath(String topImagePath) { this.topImagePath = topImagePath; }

    public String getGoodsImagePath() { return goodsImagePath; }
    public void setGoodsImagePath(String goodsImagePath) { this.goodsImagePath = goodsImagePath; }

    public String getLicenseImagePath() { return licenseImagePath; }
    public void setLicenseImagePath(String licenseImagePath) { this.licenseImagePath = licenseImagePath; }

    public String getPasscodeImagePath() { return passcodeImagePath; }
    public void setPasscodeImagePath(String passcodeImagePath) { this.passcodeImagePath = passcodeImagePath; }

    public String getPasscodeVehicleId() { return passcodeVehicleId; }
    public void setPasscodeVehicleId(String passcodeVehicleId) { this.passcodeVehicleId = passcodeVehicleId; }

    public String getPasscodeVehicleDisplayId() { return passcodeVehicleDisplayId; }
    public void setPasscodeVehicleDisplayId(String passcodeVehicleDisplayId) { this.passcodeVehicleDisplayId = passcodeVehicleDisplayId; }

    public String getPasscodeVehicleColorName() { return passcodeVehicleColorName; }
    public void setPasscodeVehicleColorName(String passcodeVehicleColorName) { this.passcodeVehicleColorName = passcodeVehicleColorName; }

    public String getPasscodeEnStationId() { return passcodeEnStationId; }
    public void setPasscodeEnStationId(String passcodeEnStationId) { this.passcodeEnStationId = passcodeEnStationId; }

    public String getPasscodeExStationId() { return passcodeExStationId; }
    public void setPasscodeExStationId(String passcodeExStationId) { this.passcodeExStationId = passcodeExStationId; }

    public String getPasscodeEnWeight() { return passcodeEnWeight; }
    public void setPasscodeEnWeight(String passcodeEnWeight) { this.passcodeEnWeight = passcodeEnWeight; }

    public String getPasscodeExWeight() { return passcodeExWeight; }
    public void setPasscodeExWeight(String passcodeExWeight) { this.passcodeExWeight = passcodeExWeight; }

    public String getPasscodeMediaType() { return passcodeMediaType; }
    public void setPasscodeMediaType(String passcodeMediaType) { this.passcodeMediaType = passcodeMediaType; }

    public String getPasscodeTransactionId() { return passcodeTransactionId; }
    public void setPasscodeTransactionId(String passcodeTransactionId) { this.passcodeTransactionId = passcodeTransactionId; }

    public String getPasscodePassId() { return passcodePassId; }
    public void setPasscodePassId(String passcodePassId) { this.passcodePassId = passcodePassId; }

    public String getPasscodeExTime() { return passcodeExTime; }
    public void setPasscodeExTime(String passcodeExTime) { this.passcodeExTime = passcodeExTime; }

    public String getPasscodeTransPayType() { return passcodeTransPayType; }
    public void setPasscodeTransPayType(String passcodeTransPayType) { this.passcodeTransPayType = passcodeTransPayType; }

    public String getPasscodeFee() { return passcodeFee; }
    public void setPasscodeFee(String passcodeFee) { this.passcodeFee = passcodeFee; }

    public String getPasscodePayFee() { return passcodePayFee; }
    public void setPasscodePayFee(String passcodePayFee) { this.passcodePayFee = passcodePayFee; }

    public String getPasscodeVehicleSign() { return passcodeVehicleSign; }
    public void setPasscodeVehicleSign(String passcodeVehicleSign) { this.passcodeVehicleSign = passcodeVehicleSign; }

    public String getPasscodeProvinceCount() { return passcodeProvinceCount; }
    public void setPasscodeProvinceCount(String passcodeProvinceCount) { this.passcodeProvinceCount = passcodeProvinceCount; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public LocalDateTime getInspectionTime() { return inspectionTime; }
    public void setInspectionTime(LocalDateTime inspectionTime) { this.inspectionTime = inspectionTime; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    public Integer getResultStatus() { return resultStatus; }
    public void setResultStatus(Integer resultStatus) { this.resultStatus = resultStatus; }

    public Integer getNopassType() { return nopassType; }
    public void setNopassType(Integer nopassType) { this.nopassType = nopassType; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getInspectorPhone() { return inspectorPhone; }
    public void setInspectorPhone(String inspectorPhone) { this.inspectorPhone = inspectorPhone; }

    public String getReviewerPhone() { return reviewerPhone; }
    public void setReviewerPhone(String reviewerPhone) { this.reviewerPhone = reviewerPhone; }

    public Integer getManualReviewState() { return manualReviewState; }
    public void setManualReviewState(Integer manualReviewState) { this.manualReviewState = manualReviewState; }

    public Integer getToTransportdeptState() { return toTransportdeptState; }
    public void setToTransportdeptState(Integer toTransportdeptState) { this.toTransportdeptState = toTransportdeptState; }

    public LocalDateTime getToTransportdeptTime() { return toTransportdeptTime; }
    public void setToTransportdeptTime(LocalDateTime toTransportdeptTime) { this.toTransportdeptTime = toTransportdeptTime; }

    public String getToTransportdeptComment() { return toTransportdeptComment; }
    public void setToTransportdeptComment(String toTransportdeptComment) { this.toTransportdeptComment = toTransportdeptComment; }
}
