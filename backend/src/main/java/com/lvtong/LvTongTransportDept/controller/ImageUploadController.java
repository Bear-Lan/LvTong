package com.lvtong.LvTongTransportDept.controller;

import com.lvtong.LvTongTransportDept.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 图片文件上传接口
 *
 * 【功能】
 * 接收前端上传的图片文件，保存到本地指定目录，返回数据库可用的文件路径。
 *
 * 【存储路径规则】
 * D:/LvTongTransportDept/LvTongFiles/Images/captures/{yyyy}/{MM}/{dd}/{type}/{filename}
 * 例如：D:/LvTongTransportDept/LvTongFiles/Images/captures/2026/03/27/head/uuid.jpg
 *
 * 【type 类型】
 * head    - 车头照
 * tail    - 车尾照
 * license - 行驶证
 * goods   - 货物照（支持多张，路径用逗号分隔）
 * body    - 车身照
 * transparent - 透视影像
 * passcode   - 通行凭证
 */
@RestController
@RequestMapping("/api/upload")
@Tag(name = "图片上传", description = "查验记录图片上传接口")
public class ImageUploadController {

    private static final String BASE_PATH = "D:/LvTongTransportDept/LvTongFiles/Images/captures";

    @PostMapping("/image")
    @Operation(summary = "上传图片", description = "上传单张图片，返回保存后的本地路径")
    public ApiResponse<Map<String, String>> uploadImage(
            @Parameter(description = "图片文件（支持 jpg/png/gif/bmp/webp）")
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "图片类型：head/tail/license/goods/body/transparent/passcode")
            @RequestParam(value = "type", defaultValue = "head") String type) {

        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的图片文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return ApiResponse.error(400, "文件名无效");
        }

        // 校验文件类型
        String ext = getExtension(originalFilename).toLowerCase();
        if (!isAllowedExtension(ext)) {
            return ApiResponse.error(400, "不支持的图片格式，仅支持 jpg/jpeg/png/gif/bmp/webp");
        }

        // 文件大小限制 20MB
        if (file.getSize() > 20 * 1024 * 1024) {
            return ApiResponse.error(400, "图片大小不能超过 20MB");
        }

        // 生成存储路径
        LocalDate now = LocalDate.now();
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = now.format(DateTimeFormatter.ofPattern("MM"));
        String day = now.format(DateTimeFormatter.ofPattern("dd"));

        // type 参数白名单校验
        if (!isValidType(type)) {
            type = "other";
        }

        String subDir = year + "/" + month + "/" + day + "/" + type;
        Path dirPath = Paths.get(BASE_PATH, subDir);

        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            return ApiResponse.error(500, "无法创建存储目录: " + e.getMessage());
        }

        // 生成唯一文件名：uuid + 原扩展名
        String savedFilename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path targetPath = dirPath.resolve(savedFilename);

        try {
            file.transferTo(targetPath.toFile());

            // 返回 Windows 路径格式
            String savedPath = targetPath.toString().replace("/", "\\");
            return ApiResponse.success("上传成功", Map.of(
                    "path", savedPath,
                    "filename", savedFilename,
                    "type", type
            ));

        } catch (IOException e) {
            return ApiResponse.error(500, "文件保存失败: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) return "";
        return filename.substring(lastDot);
    }

    private boolean isAllowedExtension(String ext) {
        return ext.equals(".jpg") || ext.equals(".jpeg") ||
               ext.equals(".png") || ext.equals(".gif") ||
               ext.equals(".bmp") || ext.equals(".webp");
    }

    private boolean isValidType(String type) {
        return type.equals("head") || type.equals("tail") ||
               type.equals("license") || type.equals("goods") ||
               type.equals("body") || type.equals("transparent") ||
               type.equals("passcode");
    }
}
