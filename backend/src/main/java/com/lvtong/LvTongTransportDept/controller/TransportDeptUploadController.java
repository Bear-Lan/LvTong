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

    @Operation(summary = "上报单条查验记录（指定排除图片）", description = "上报时排除指定的图片，上报成功后自动将复核结果更新为审核通过")
    @PostMapping("/upload/{id}/exclude")
    public ApiResponse<Map<String, Object>> uploadSingleWithExclude(
            @Parameter(description = "查验记录 ID") @PathVariable Integer id,
            @Parameter(description = "排除的图片标识列表：证据链照片用typeId如11/12/13/99，货物照用goods_0/goods_1等下标")
            @RequestBody List<String> excludeList) {
        Map<String, Object> result = uploadService.uploadSingle(id, excludeList);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ApiResponse.success("上报成功", result);
        }
        return ApiResponse.error((String) result.get("msg"));
    }
}
