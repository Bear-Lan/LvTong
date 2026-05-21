package com.lvtong.LvTongTransportDept.controller;

import com.lvtong.LvTongTransportDept.dto.ApiResponse;
import com.lvtong.LvTongTransportDept.service.DynamicTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 二级平台数据接收接口
 * 用于接收各站点（二级平台）上报的查验记录数据
 */
@RestController
@RequestMapping("/api/platform")
@Tag(name = "二级平台数据接收", description = "接收二级平台上报的查验记录数据")
public class PlatformReceiveController {

    private static final Logger log = LoggerFactory.getLogger(PlatformReceiveController.class);

    @Autowired
    private DynamicTableService dynamicTableService;

    /**
     * 接收单条查验记录
     * 二级平台调用此接口上报数据
     */
    @PostMapping("/receive")
    @Operation(summary = "接收查验记录", description = "接收二级平台上报的查验记录，根据出口站ID分发到对应站点表")
    public ApiResponse<String> receiveRecord(@RequestBody Map<String, Object> data) {
        try {
            // 获取出口站ID（支持驼峰和下划线）
            String exStationId = getStationId(data);
            if (exStationId == null || exStationId.isEmpty()) {
                return ApiResponse.error("缺少出口站ID (passcodeExStationId / passcode_ex_station_id)");
            }

            // 根据出口站ID获取站点名称（表名）
            String tableName = dynamicTableService.getStationNameByCode(exStationId);
            if (tableName == null || tableName.isEmpty()) {
                return ApiResponse.error("站点信息不存在: station_id=" + exStationId);
            }

            // 构建记录数据
            DynamicTableService.RecordData record = convertToRecordData(data);

            // 插入数据
            dynamicTableService.insertRecord(tableName, record);

            log.info("接收查验记录成功: plateNumber={}, station={}", record.plateNumber, tableName);
            return ApiResponse.success("接收成功");

        } catch (Exception e) {
            log.error("接收查验记录失败: {}", e.getMessage(), e);
            return ApiResponse.error("接收失败: " + e.getMessage());
        }
    }

    /**
     * 批量接收查验记录
     */
    @PostMapping("/receive/batch")
    @Operation(summary = "批量接收查验记录", description = "批量接收二级平台上报的查验记录")
    public ApiResponse<String> receiveRecords(@RequestBody List<Map<String, Object>> dataList) {
        try {
            int successCount = 0;
            int failCount = 0;

            for (Map<String, Object> data : dataList) {
                try {
                    String exStationId = getStationId(data);
                    if (exStationId == null || exStationId.isEmpty()) {
                        failCount++;
                        continue;
                    }

                    String tableName = dynamicTableService.getStationNameByCode(exStationId);
                    if (tableName == null || tableName.isEmpty()) {
                        tableName = "station_" + exStationId;
                    }

                    DynamicTableService.RecordData record = convertToRecordData(data);
                    dynamicTableService.insertRecord(tableName, record);
                    successCount++;

                } catch (Exception e) {
                    log.warn("处理单条记录失败: {}", e.getMessage());
                    failCount++;
                }
            }

            log.info("批量接收完成: 成功={}, 失败={}", successCount, failCount);
            return ApiResponse.success(String.format("接收完成: 成功%d条, 失败%d条", successCount, failCount));

        } catch (Exception e) {
            log.error("批量接收失败: {}", e.getMessage(), e);
            return ApiResponse.error("批量接收失败: " + e.getMessage());
        }
    }

    /**
     * 获取出口站ID（支持驼峰和下划线）
     */
    private String getStationId(Map<String, Object> data) {
        Object value = data.get("passcodeExStationId");
        if (value == null) {
            value = data.get("passcode_ex_station_id");
        }
        return value != null ? value.toString() : null;
    }

    /**
     * 将Map转换为RecordData（支持驼峰和下划线两种格式）
     */
    private DynamicTableService.RecordData convertToRecordData(Map<String, Object> data) {
        DynamicTableService.RecordData record = new DynamicTableService.RecordData();

        // 支持驼峰和下划线两种格式
        record.checkId = getString(data, "checkId", "check_id");
        record.plateNumber = getString(data, "plateNumber", "plate_number");
        record.plateNumberGc = getString(data, "plateNumberGc", "plate_number_gc");
        record.driverPhone = getString(data, "driverPhone", "driver_phone");
        record.vehicleType = getString(data, "vehicleType", "vehicle_type");
        record.vehicleContainertype = getString(data, "vehicleContainertype", "vehicle_containertype");
        record.goodsType = getString(data, "goodsType", "goods_type");
        record.goodsCategory = getString(data, "goodsCategory", "goods_category");
        record.loadRate = getBigDecimal(data, "loadRate", "load_rate");
        record.loadWeight = getBigDecimal(data, "loadWeight", "load_weight");
        record.vehicleSize = getString(data, "vehicleSize", "vehicle_size");
        record.historyRecord = getString(data, "historyRecord", "history_record");
        record.bodyImagePath = getString(data, "bodyImagePath", "body_image_path");
        record.transparentImagePath = getString(data, "transparentImagePath", "transparent_image_path");
        record.headImagePath = getString(data, "headImagePath", "head_image_path");
        record.tailImagePath = getString(data, "tailImagePath", "tail_image_path");
        record.topImagePath = getString(data, "topImagePath", "top_image_path");
        record.goodsImagePath = getString(data, "goodsImagePath", "goods_image_path");
        record.evidencesImagePath = getString(data, "evidencesImagePath", "evidences_image_path");
        record.licenseImagePath = getString(data, "licenseImagePath", "license_image_path");
        record.passcodeImagePath = getString(data, "passcodeImagePath", "passcode_image_path");
        record.passcodeVehicleId = getString(data, "passcodeVehicleId", "passcode_vehicle_id");
        record.passcodeVehicleDisplayId = getString(data, "passcodeVehicleDisplayId", "passcode_vehicle_display_id");
        record.passcodeVehicleColorName = getString(data, "passcodeVehicleColorName", "passcode_vehicle_color_name");
        record.passcodeEnStationId = getString(data, "passcodeEnStationId", "passcode_en_station_id");
        record.passcodeExStationId = getString(data, "passcodeExStationId", "passcode_ex_station_id");
        record.passcodeEnWeight = getString(data, "passcodeEnWeight", "passcode_en_weight");
        record.passcodeExWeight = getString(data, "passcodeExWeight", "passcode_ex_weight");
        record.passcodeMediaType = getString(data, "passcodeMediaType", "passcode_media_type");
        record.passcodeTransactionId = getString(data, "passcodeTransactionId", "passcode_transaction_id");
        record.passcodePassId = getString(data, "passcodePassId", "passcode_pass_id");
        record.passcodeExTime = getString(data, "passcodeExTime", "passcode_ex_time");
        record.passcodeTransPayType = getString(data, "passcodeTransPayType", "passcode_trans_pay_type");
        record.passcodeFee = getString(data, "passcodeFee", "passcode_fee");
        record.passcodePayFee = getString(data, "passcodePayFee", "passcode_pay_fee");
        record.passcodeVehicleSign = getString(data, "passcodeVehicleSign", "passcode_vehicle_sign");
        record.passcodeProvinceCount = getString(data, "passcodeProvinceCount", "passcode_province_count");
        record.operatorName = getString(data, "operatorName", "operator_name");
        record.btnPrebookTime = getString(data, "btnPrebookTime", "btn_prebook_time");
        record.acceptanceTime = getString(data, "acceptanceTime", "acceptance_time");
        record.opengateTime = getString(data, "opengateTime", "opengate_time");
        record.openlightscreenTime = getString(data, "openlightscreenTime", "openlightscreen_time");
        record.closelightscreenTime = getString(data, "closelightscreenTime", "closelightscreen_time");
        record.cdPhotoTime = getString(data, "cdPhotoTime", "cd_photo_time");
        record.inspectionTime = getString(data, "inspectionTime", "inspection_time");
        record.resultStatus = getInt(data, "resultStatus", "result_status");
        record.nopassType = getInt(data, "nopassType", "nopass_type");
        record.status = getInt(data, "status", "status");
        record.groupId = getString(data, "groupId", "group_id");
        record.inspectorPhone = getString(data, "inspectorPhone", "inspector_phone");
        record.reviewerPhone = getString(data, "reviewerPhone", "reviewer_phone");
        record.manualReviewState = getInt(data, "manualReviewState", "manual_review_state");
        record.toTransportdeptState = getInt(data, "toTransportdeptState", "to_transportdept_state");
        record.toTransportdeptTime = getString(data, "toTransportdeptTime", "to_transportdept_time");
        record.toTransportdeptComment = getString(data, "toTransportdeptComment", "to_transportdept_comment");

        return record;
    }

    private String getString(Map<String, Object> data, String camelKey, String snakeKey) {
        Object value = data.get(camelKey);
        if (value == null) {
            value = data.get(snakeKey);
        }
        if (value == null) return null;
        String str = value.toString();
        return str.isEmpty() ? null : str;
    }

    private BigDecimal getBigDecimal(Map<String, Object> data, String camelKey, String snakeKey) {
        Object value = data.get(camelKey);
        if (value == null) {
            value = data.get(snakeKey);
        }
        if (value == null) return null;
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getInt(Map<String, Object> data, String camelKey, String snakeKey) {
        Object value = data.get(camelKey);
        if (value == null) {
            value = data.get(snakeKey);
        }
        if (value == null) return null;
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}