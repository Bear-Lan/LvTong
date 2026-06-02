package com.lvtong.LvTongTransportDept.controller;

import com.lvtong.LvTongTransportDept.dto.ApiResponse;
import com.lvtong.LvTongTransportDept.service.AIDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * AI识别接口
 *
 * 【功能】
 * 接收前端上传的图片，转发到AI服务器进行识别，返回识别结果。
 *
 * 【AI服务器地址】
 * 车辆/货物识别： http://192.168.88.245:8895/detect
 * 行驶证识别：    http://192.168.88.245:8894/dl_ocr/front_and_back
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI识别", description = "AI图片识别接口")
public class AIDetectionController {

    @Autowired
    private AIDetectionService aiDetectionService;

    /**
     * 车辆照片识别
     * 识别车厢类型、车轮数量
     */
    @PostMapping("/vehicle")
    @Operation(summary = "车辆照片识别", description = "上传车辆图片，识别车厢类型和车轮数量")
    public ApiResponse<Map<String, Object>> detectVehicle(
            @Parameter(description = "车辆图片文件")
            @RequestParam("image") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的图片文件");
        }
        try {
            Map<String, Object> result = aiDetectionService.detectVehicle(file);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(500, "车辆识别失败: " + e.getMessage());
        }
    }

    /**
     * 货物类型识别
     * 识别货物类型（过滤掉product_code为others的结果）
     */
    @PostMapping("/goods")
    @Operation(summary = "货物类型识别", description = "上传货物图片，识别货物类型")
    public ApiResponse<Map<String, Object>> detectGoods(
            @Parameter(description = "货物图片文件")
            @RequestParam("real_image") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的图片文件");
        }
        try {
            Map<String, Object> result = aiDetectionService.detectGoods(file);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(500, "货物识别失败: " + e.getMessage());
        }
    }

    /**
     * 行驶证识别
     * 识别行驶证正反面信息
     */
    @PostMapping("/driver-license")
    @Operation(summary = "行驶证识别", description = "上传行驶证图片，识别行驶证信息")
    public ApiResponse<Map<String, Object>> detectDriverLicense(
            @Parameter(description = "行驶证图片文件")
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的图片文件");
        }
        try {
            Map<String, Object> result = aiDetectionService.detectDriverLicense(file);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(500, "行驶证识别失败: " + e.getMessage());
        }
    }

    /**
     * 车轴识别
     * 识别车轴数量和位置
     */
    @PostMapping("/axle")
    @Operation(summary = "车轴识别", description = "上传车轴图片，识别车轴数量和位置")
    public ApiResponse<Map<String, Object>> detectAxle(
            @Parameter(description = "车轴图片文件")
            @RequestParam("image") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的图片文件");
        }
        try {
            Map<String, Object> result = aiDetectionService.detectAxle(file);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(500, "车轴识别失败: " + e.getMessage());
        }
    }

    /**
     * 车厢识别
     * 识别车厢类型
     */
    @PostMapping("/carriage")
    @Operation(summary = "车厢识别", description = "上传车厢图片，识别车厢类型")
    public ApiResponse<Map<String, Object>> detectCarriage(
            @Parameter(description = "车厢图片文件")
            @RequestParam("image") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的图片文件");
        }
        try {
            Map<String, Object> result = aiDetectionService.detectCarriage(file);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(500, "车厢识别失败: " + e.getMessage());
        }
    }

    /**
     * 货物透视图识别
     * task=product_xray
     */
    @PostMapping("/product-xray")
    @Operation(summary = "货物透视图识别", description = "上传图片，AI透视图识别货物")
    public ApiResponse<Map<String, Object>> detectProductXray(
            @Parameter(description = "图片文件")
            @RequestParam("image") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的图片文件");
        }
        try {
            Map<String, Object> result = aiDetectionService.detectProductXray(file);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(500, "货物透视图识别失败: " + e.getMessage());
        }
    }

    /**
     * 雷达车头识别
     * task=truck_lidar
     */
    @PostMapping("/truck-lidar-head")
    @Operation(summary = "雷达车头识别", description = "上传图片，雷达车头识别")
    public ApiResponse<Map<String, Object>> detectTruckLidarHead(
            @Parameter(description = "图片文件")
            @RequestParam("image") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的图片文件");
        }
        try {
            Map<String, Object> result = aiDetectionService.detectTruckLidarHead(file);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(500, "雷达车头识别失败: " + e.getMessage());
        }
    }

    /**
     * 雷达车高识别
     * task=truck_lidar
     */
    @PostMapping("/truck-lidar-height")
    @Operation(summary = "雷达车高识别", description = "上传图片，雷达车高识别")
    public ApiResponse<Map<String, Object>> detectTruckLidarHeight(
            @Parameter(description = "图片文件")
            @RequestParam("image") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的图片文件");
        }
        try {
            Map<String, Object> result = aiDetectionService.detectTruckLidarHeight(file);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(500, "雷达车高识别失败: " + e.getMessage());
        }
    }

    /**
     * 车厢混装识别
     * task=product_xray
     */
    @PostMapping("/mixed-load")
    @Operation(summary = "车厢混装识别", description = "上传图片，AI检测车厢混装")
    public ApiResponse<Map<String, Object>> detectMixedLoad(
            @Parameter(description = "图片文件")
            @RequestParam("image") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的图片文件");
        }
        try {
            Map<String, Object> result = aiDetectionService.detectMixedLoad(file);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(500, "车厢混装识别失败: " + e.getMessage());
        }
    }

    /**
     * 车厢货物装载率识别
     * task=truck_xray_box
     */
    @PostMapping("/truck-xray-box")
    @Operation(summary = "车厢货物装载率识别", description = "上传图片，AI识别车厢货物装载率")
    public ApiResponse<Map<String, Object>> detectTruckXrayBox(
            @Parameter(description = "图片文件")
            @RequestParam("image") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的图片文件");
        }
        try {
            Map<String, Object> result = aiDetectionService.detectTruckXrayBox(file);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(500, "车厢货物装载率识别失败: " + e.getMessage());
        }
    }
}