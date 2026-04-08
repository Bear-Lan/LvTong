package com.lvtong.LvTongTransportDept.controller;

import com.lvtong.LvTongTransportDept.constant.UserConstants;
import com.lvtong.LvTongTransportDept.dto.ApiResponse;
import com.lvtong.LvTongTransportDept.dto.LoginRequest;
import com.lvtong.LvTongTransportDept.dto.UpdateUserRequest;
import com.lvtong.LvTongTransportDept.entity.UserEntity;
import com.lvtong.LvTongTransportDept.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "用户登录、CRUD接口")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取所有用户电话（用于核验员下拉选择）
     */
    @GetMapping("/phones")
    @Operation(summary = "获取用户电话列表", description = "返回所有正常用户的电话列表，用于核验员下拉选择")
    public ApiResponse<List<Map<String, String>>> getUserPhones() {
        List<UserEntity> users = userService.getAllUsers();
        List<Map<String, String>> data = users.stream()
                .filter(u -> u.getPhone() != null && !u.getPhone().isBlank())
                .map(u -> {
                    Map<String, String> item = new java.util.HashMap<>();
                    item.put("phone", u.getPhone());
                    item.put("realName", u.getRealName() != null ? u.getRealName() : u.getUsername());
                    if (u.getGroupId() != null) {
                        item.put("groupId", String.valueOf(u.getGroupId()));
                    }
                    return item;
                })
                .distinct()
                .sorted(Comparator.comparing(m -> m.get("phone")))
                .toList();
        return ApiResponse.success(data);
    }

    /**
     * 管理员新增用户（无需验证码）
     */
    @PostMapping("/register")
    @Operation(summary = "新增用户", description = "管理员新增用户，注册后角色为普通用户(1)，状态为正常(0)")
    public ApiResponse<String> register(@RequestBody com.lvtong.LvTongTransportDept.entity.UserEntity user) {
        userService.register(
                user.getUsername(),
                user.getPassword(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                user.getGroupId()
        );
        return ApiResponse.success("新增成功");
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "返回JWT Token和用户基本信息")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> userData = userService.login(request.getUsername(), request.getPassword());

        Map<String, Object> data = new HashMap<>(userData);
        // 班组表已删除，班组名称暂时为空
        data.put("groupName", "");
        data.put("roleText", UserConstants.getRoleText((Integer) userData.get("role")));

        return ApiResponse.success("登录成功", data);
    }

    @GetMapping("/list")
    @Operation(summary = "获取用户列表", description = "获取所有用户列表，所有登录用户可查看")
    public ApiResponse<List<Map<String, Object>>> getUserList(HttpServletRequest request) {
        List<UserEntity> users = userService.getAllUsers();
        List<Map<String, Object>> data = users.stream().map(this::convertToMap).collect(Collectors.toList());
        return ApiResponse.success(data);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息，所有登录用户可查看")
    public ApiResponse<Map<String, Object>> getUserById(
            @Parameter(description = "用户ID") @PathVariable Long id,
            HttpServletRequest request) {
        UserEntity user = userService.getUserById(id);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        return ApiResponse.success(convertToMap(user));
    }

    /**
     * 管理员更新指定用户信息
     * 可修改：realName, email, phone, role, status, groupId
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新指定用户信息", description = "仅管理员可用，可修改任意用户的 realName, email, phone, role, status, usergroup")
    public ApiResponse<String> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody UpdateUserRequest updateRequest,
            HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != UserConstants.ROLE_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限，仅管理员可操作");
        }
        userService.updateUserByAdmin(id, updateRequest);
        return ApiResponse.success("更新成功");
    }

    /**
     * 普通用户更新自己的个人信息
     * 可修改：username, password（需提供原密码）, newPassword, realName, email, phone
     */
    @PutMapping("/profile")
    @Operation(summary = "更新个人资料", description = "普通用户修改个人信息，可修改：username, password + newPassword, realName, email, phone")
    public ApiResponse<String> updateProfile(
            @RequestBody UpdateUserRequest updateRequest,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        userService.updateUserBySelf(currentUserId, updateRequest, currentUserId);
        return ApiResponse.success("更新成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据ID删除用户，仅管理员可操作")
    public ApiResponse<String> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != UserConstants.ROLE_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限，仅管理员可操作");
        }
        userService.deleteUser(id);
        return ApiResponse.success("删除成功");
    }

    /**
     * 管理员重置指定用户密码
     * 将目标用户密码重置为默认密码 "123456"
     */
    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置用户密码", description = "管理员将指定用户密码重置为 123456，仅管理员可操作")
    public ApiResponse<String> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != UserConstants.ROLE_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限，仅管理员可操作");
        }
        userService.resetPassword(id);
        return ApiResponse.success("密码已重置，请联系管理员获取");
    }

    private Map<String, Object> convertToMap(UserEntity user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("realName", user.getRealName());
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("groupId", user.getGroupId());
        map.put("groupName", resolveGroupName(user.getGroupId()));
        map.put("role", user.getRole());
        map.put("roleText", UserConstants.getRoleText(user.getRole()));
        map.put("status", user.getStatus());
        map.put("statusText", UserConstants.getStatusText(user.getStatus()));
        map.put("createdTime", user.getCreatedTime());
        map.put("lastLoginTime", user.getLastLoginTime());
        return map;
    }

    /**
     * 根据班组 ID 解析班组名称
     * 班组表已删除，直接返回"班组X"格式
     */
    private String resolveGroupName(Long groupId) {
        if (groupId == null) return "";
        return "班组" + groupId;
    }
}
