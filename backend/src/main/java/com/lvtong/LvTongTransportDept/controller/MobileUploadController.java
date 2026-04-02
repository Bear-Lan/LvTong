package com.lvtong.LvTongTransportDept.controller;

import com.lvtong.LvTongTransportDept.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 手机APP上传接口（无需JWT认证）
 *
 * 【接口列表】
 * POST /api/mobile/upload/image - 单张图片上传
 * POST /api/mobile/upload/json  - JSON文件上传
 * GET  /api/mobile/health       - 服务健康检查
 *
 * 【存储结构】
 * D:/soft/FtpLvTong/
 *   ├─ {dirName}/
 *   │    ├─ 车头照.jpg
 *   │    └─ 行驶证.png
 *   └─ {jsonFileName}   ← JSON 与文件夹同级
 */
@RestController
@RequestMapping("/api/mobile")
@Tag(name = "手机APP上传", description = "手机端拍照上传接口（无需登录）")
public class MobileUploadController {

    @Value("${upload.base-path:D:/soft/FtpLvTong}")
    private String basePath;

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB

    // ================================================================
    // 单张图片上传
    // ================================================================

    /**
     * POST /api/mobile/upload/image
     *
     * FormData 参数：
     *   dirName - 文件夹名称（如 photo_20260327）
     *   file    - 图片文件（使用原始文件名存储）
     *
     * 存储结构：
     *   {basePath}/{dirName}/{原始文件名}
     *
     * 响应：
     * {
     *   "savedName": "车头照.jpg",
     *   "savedPath": "D:/soft/FtpLvTong/photo_20260327/车头照.jpg"
     * }
     */
    @PostMapping("/upload/image")
    @Operation(summary = "单张图片上传", description = "上传单张图片到指定目录，保留原始文件名")
    public ApiResponse<Map<String, Object>> uploadImage(
            @Parameter(description = "文件夹名称")
            @RequestParam("dirName") String dirName,

            @Parameter(description = "图片文件")
            @RequestParam("file") MultipartFile file) {

        // 1. 记录开始时间
        long startTime = System.currentTimeMillis();

        // 2. 校验文件
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的图片文件");
        }
        if (dirName == null || dirName.isBlank()) {
            return ApiResponse.error(400, "dirName 不能为空");
        }
        if (!dirName.matches("[a-zA-Z0-9_\\-]+")) {
            return ApiResponse.error(400, "dirName 只允许字母、数字、下划线、连字符");
        }

        // 3. 校验格式和大小
        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png")) {
            return ApiResponse.error(400, "不支持的图片格式，仅支持 jpg/jpeg/png");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return ApiResponse.error(400, "图片大小不能超过 10MB");
        }

        // 4. 创建目录并保存
        Path dirPath = Paths.get(basePath, dirName);
        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            return ApiResponse.error(500, "无法创建目录: " + e.getMessage());
        }

        String savedFileName = sanitizeFileName(file.getOriginalFilename());
        Path targetPath = dirPath.resolve(savedFileName);
        try {
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            return ApiResponse.error(500, "文件保存失败: " + e.getMessage());
        }

        // 5. 计算耗时（ms）
        long elapsedMs = System.currentTimeMillis() - startTime;

        return ApiResponse.success("上传成功", Map.of(
                "savedName", savedFileName,
                "savedPath", targetPath.toString().replace("\\", "/"),
                "elapsedMs", elapsedMs
        ));
    }

    // ================================================================
    // JSON文件上传
    // ================================================================

    /**
     * POST /api/mobile/upload/json
     *
     * FormData 参数：
     *   file - JSON文件（使用原始文件名存储）
     *
     * 存储结构：
     *   {basePath}/{原始文件名}
     *   （与图片文件夹同级，不放在 dirName 内）
     *
     * 响应：
     * {
     *   "savedName": "manifest.json",
     *   "savedPath": "D:/soft/FtpLvTong/manifest.json"
     * }
     */
    @PostMapping("/upload/json")
    @Operation(summary = "JSON文件上传", description = "上传JSON文件，与图片文件夹同级存放")
    public ApiResponse<Map<String, Object>> uploadJson(
            @Parameter(description = "JSON文件")
            @RequestParam("file") MultipartFile file) {

        // 1. 记录开始时间
        long startTime = System.currentTimeMillis();

        // 2. 校验文件
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的JSON文件");
        }

        // 3. 校验扩展名
        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        if (!ext.equals(".json")) {
            return ApiResponse.error(400, "仅支持上传 .json 格式文件");
        }

        // 4. 保存文件（使用原始文件名，放根目录）
        String savedFileName = sanitizeFileName(file.getOriginalFilename());
        Path targetPath = Paths.get(basePath, savedFileName);
        try {
            Files.createDirectories(targetPath.getParent());
        } catch (IOException e) {
            return ApiResponse.error(500, "无法创建目录: " + e.getMessage());
        }

        try {
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            return ApiResponse.error(500, "文件保存失败: " + e.getMessage());
        }

        // 5. 计算耗时（ms）
        long elapsedMs = System.currentTimeMillis() - startTime;

        return ApiResponse.success("上传成功", Map.of(
                "savedName", savedFileName,
                "savedPath", targetPath.toString().replace("\\", "/"),
                "elapsedMs", elapsedMs
        ));
    }

    // ================================================================
    // 健康检查
    // ================================================================

    @GetMapping("/health")
    @Operation(summary = "服务健康检查", description = "返回服务状态和当前时间戳")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success("服务正常", Map.of(
                "status", "UP",
                "timestamp", Instant.now().toEpochMilli(),
                "serverTime", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        ));
    }

    // ================================================================
    // 私有工具方法
    // ================================================================

    private String getExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) return "";
        return filename.substring(lastDot);
    }

    /**
     * 清理文件名：保留原始文件名，去除路径穿越风险字符
     */
    private String sanitizeFileName(String filename) {
        if (filename == null || filename.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "") + ".jpg";
        }
        // 去掉路径部分
        String name = Paths.get(filename).getFileName().toString();
        // 去除路径穿越字符
        name = name.replaceAll("[/\\\\]", "_");
        // 去除危险字符
        name = name.replaceAll("[<>:\"|?*]", "_");
        // 去掉首尾空格和点
        name = name.trim().replaceAll("^[. ]+|[. ]+$", "");
        if (name.isEmpty()) {
            name = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        }
        return name;
    }
}
