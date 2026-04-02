package com.lvtong.LvTongTransportDept.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

/**
 * 图片文件 HTTP 接口
 *
 * 【背景】
 * 数据库中图片路径为 Windows 本地路径（ D:\xxx\xxx.jpg ），
 * 浏览器无法直接通过 file:// 协议访问，需通过此接口转发。
 *
 * 【安全说明】
 * - 路径直接来自数据库字段，非用户输入，无需额外路径限制
 * - 使用 getCanonicalFile() 防止路径穿越攻击
 */
@RestController
@RequestMapping("/api/images")
@Tag(name = "图片服务", description = "查验图片 HTTP 访问接口")
public class ImageController {

    /**
     * 通过完整路径读取图片文件
     *
     * @param path 数据库中存储的完整路径（如 D:\inspection\2024\01\car.jpg）
     * @return 图片二进制流
     */
    @GetMapping
    @Operation(summary = "获取图片", description = "根据数据库存储的路径读取图片文件")
    public ResponseEntity<Resource> getImage(
            @Parameter(description = "图片完整路径（如 D:\\inspection\\xxx.jpg）")
            @RequestParam("path") String path) {

        if (path == null || path.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        File file;
        try {
            // 标准化路径并解析真实路径（防止 ..\..\..\etc/passwd 穿越攻击）
            file = new File(path).getCanonicalFile();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        if (!file.exists() || !file.isFile() || !file.canRead()) {
            return ResponseEntity.notFound().build();
        }

        // 根据扩展名推断 MIME 类型
        String filename = file.getName().toLowerCase();
        MediaType mediaType = inferMediaType(filename);

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    /**
     * 根据扩展名推断 MediaType
     */
    private MediaType inferMediaType(String filename) {
        if (filename.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (filename.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (filename.endsWith(".bmp")) return MediaType.parseMediaType("image/bmp");
        if (filename.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG; // jpg/jpeg 默认
    }
}
