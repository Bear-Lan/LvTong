package com.lvtong.LvTongTransportDept.service;

import com.lvtong.LvTongTransportDept.dto.UpdateUserRequest;
import com.lvtong.LvTongTransportDept.entity.UserEntity;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体，不存在返回 null
     */
    UserEntity getUserByUserName(String username);

    /**
     * 根据 ID 查询用户
     *
     * @param id 主键
     * @return 用户实体，不存在返回 null
     */
    UserEntity getUserById(Long id);

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    List<UserEntity> getAllUsers();

    /**
     * 管理员新增用户
     *
     * @param username  用户名（需唯一）
     * @param password  密码（BCrypt 加密存储）
     * @param realName  真实姓名
     * @param email     邮箱
     * @param phone     手机号
     * @param groupId 班组 ID
     * @return 创建成功的用户实体
     */
    UserEntity register(String username, String password, String realName,
                        String email, String phone, Long groupId);

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 包含 JWT Token 和用户基本信息的 Map
     */
    Map<String, Object> login(String username, String password);

    /**
     * 管理员更新用户信息
     *
     * @param id      待更新用户 ID
     * @param request 更新字段
     */
    void updateUserByAdmin(Long id, UpdateUserRequest request);

    /**
     * 普通用户更新自己的个人信息
     *
     * @param id            当前用户 ID（由拦截器从 Token 中提取）
     * @param request       更新字段
     * @param currentUserId 当前登录用户 ID（用于权限校验）
     */
    void updateUserBySelf(Long id, UpdateUserRequest request, Long currentUserId);

    /**
     * 删除用户（物理删除）
     *
     * @param id 用户 ID
     */
    void deleteUser(Long id);

    /**
     * 管理员重置用户密码为随机密码
     *
     * @param id 用户 ID
     */
    void resetPassword(Long id);
}
