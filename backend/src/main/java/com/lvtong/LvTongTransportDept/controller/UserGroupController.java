package com.lvtong.LvTongTransportDept.controller;

import com.lvtong.LvTongTransportDept.constant.UserConstants;
import com.lvtong.LvTongTransportDept.dto.ApiResponse;
import com.lvtong.LvTongTransportDept.entity.UserGroup;
import com.lvtong.LvTongTransportDept.service.UserGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@Tag(name = "班组管理", description = "班组 CRUD（管理员专用）")
public class UserGroupController {

    @Autowired
    private UserGroupService groupService;

    /**
     * 获取所有班组（供下拉选择使用，所有登录用户可访问）
     */
    @GetMapping
    @Operation(summary = "获取班组列表", description = "返回所有班组，供下拉选择使用")
    public ApiResponse<List<UserGroup>> getAllGroups() {
        return ApiResponse.success(groupService.getAllGroups());
    }

    /**
     * 根据 ID 获取单个班组
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取班组详情", description = "根据 ID 获取单个班组信息")
    public ApiResponse<UserGroup> getGroupById(
            @Parameter(description = "班组ID") @PathVariable Long id) {
        return groupService.getGroupById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("班组不存在"));
    }

    /**
     * 管理员新增班组
     */
    @PostMapping
    @Operation(summary = "新增班组", description = "管理员新增班组")
    public ApiResponse<UserGroup> createGroup(
            @RequestBody UserGroup group,
            HttpServletRequest request) {
        checkAdmin(request);
        return ApiResponse.success("新增成功", groupService.createGroup(group));
    }

    /**
     * 管理员更新班组
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新班组", description = "管理员更新班组信息")
    public ApiResponse<String> updateGroup(
            @Parameter(description = "班组ID") @PathVariable Long id,
            @RequestBody UserGroup group,
            HttpServletRequest request) {
        checkAdmin(request);
        groupService.updateGroup(id, group);
        return ApiResponse.success("更新成功");
    }

    /**
     * 管理员删除班组
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除班组", description = "管理员删除班组（需无关联用户和查验记录）")
    public ApiResponse<String> deleteGroup(
            @Parameter(description = "班组ID") @PathVariable Long id,
            HttpServletRequest request) {
        checkAdmin(request);
        groupService.deleteGroup(id);
        return ApiResponse.success("删除成功");
    }

    private void checkAdmin(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != UserConstants.ROLE_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限，仅管理员可操作");
        }
    }
}
