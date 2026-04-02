package com.lvtong.LvTongTransportDept.controller;

import com.lvtong.LvTongTransportDept.dto.ApiResponse;
import com.lvtong.LvTongTransportDept.service.TransportDeptUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 交通局上报接口
 */
@Tag(name = "交通局上报", description = "查验记录上报至交通局平台")
@RestController
@RequestMapping("/api/transport-dept")
@RequiredArgsConstructor
public class TransportDeptUploadController {

    @Autowired
    private final TransportDeptUploadService uploadService;

    @Operation(summary = "上报单条查验记录", description = "将指定的查验记录上报至交通局平台")
    @PostMapping("/upload/{id}")
    public ApiResponse<Map<String, Object>> uploadSingle(
            @Parameter(description = "查验记录 ID") @PathVariable Integer id) {
        Map<String, Object> result = uploadService.uploadSingle(id);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ApiResponse.success("上报成功", result);
        }
        return ApiResponse.error((String) result.get("msg"));
    }

    @Operation(summary = "批量上报查验记录", description = "将多条查验记录批量上报至交通局平台")
    @PostMapping("/upload/batch")
    public ApiResponse<Map<String, Object>> uploadBatch(
            @Parameter(description = "查验记录 ID 列表") @RequestBody List<Integer> ids) {
        Map<String, Object> result = uploadService.uploadBatch(ids);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ApiResponse.success("批量上报完成", result);
        }
        return ApiResponse.error((String) result.get("msg"));
    }
}
