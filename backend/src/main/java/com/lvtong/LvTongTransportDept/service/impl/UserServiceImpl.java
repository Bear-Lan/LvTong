package com.lvtong.LvTongTransportDept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lvtong.LvTongTransportDept.constant.UserConstants;
import com.lvtong.LvTongTransportDept.dto.UpdateUserRequest;
import com.lvtong.LvTongTransportDept.entity.UserEntity;
import com.lvtong.LvTongTransportDept.exception.BusinessException;
import com.lvtong.LvTongTransportDept.mapper.UserMapper;
import com.lvtong.LvTongTransportDept.service.UserService;
import com.lvtong.LvTongTransportDept.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户业务逻辑实现
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserEntity getUserByUserName(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, username)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserEntity> getAllUsers() {
        return userMapper.selectList(
                new LambdaQueryWrapper<UserEntity>()
                        .ne(UserEntity::getStatus, UserConstants.STATUS_DISABLED)
        );
    }

    @Override
    @Transactional
    public UserEntity register(String username, String password, String realName,
                               String email, String phone, Long groupId) {
        if (getUserByUserName(username) != null) {
            throw new BusinessException("用户名已存在");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setGroupId(groupId);
        user.setRole(UserConstants.ROLE_USER);
        user.setStatus(UserConstants.STATUS_ACTIVE);

        userMapper.insert(user);
        return user;
    }

    @Override
    @Transactional
    public Map<String, Object> login(String username, String password) {
        UserEntity user = getUserByUserName(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == UserConstants.STATUS_DISABLED) {
            throw new BusinessException("账号已被禁用");
        }

        userMapper.update(null,
                new LambdaUpdateWrapper<UserEntity>()
                        .eq(UserEntity::getId, user.getId())
                        .set(UserEntity::getLastLoginTime, LocalDateTime.now())
        );

        String token = jwtUtil.generateToken(username, user.getId(), user.getRole());
        return Map.of(
                "token", token,
                "userId", user.getId(),
                "username", user.getUsername(),
                "realName", user.getRealName(),
                "phone", user.getPhone(),
                "groupId", user.getGroupId(),
                "role", user.getRole()
        );
    }

    @Override
    @Transactional
    public void updateUserByAdmin(Long id, UpdateUserRequest request) {
        if (userMapper.selectById(id) == null) {
            throw new BusinessException("用户不存在");
        }

        LambdaUpdateWrapper<UserEntity> wrapper = new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, id);

        if (request.getRealName() != null) {
            wrapper.set(UserEntity::getRealName, request.getRealName());
        }
        if (request.getEmail() != null) {
            wrapper.set(UserEntity::getEmail, request.getEmail());
        }
        if (request.getPhone() != null) {
            wrapper.set(UserEntity::getPhone, request.getPhone());
        }
        if (request.getRole() != null) {
            wrapper.set(UserEntity::getRole, request.getRole());
        }
        if (request.getStatus() != null) {
            wrapper.set(UserEntity::getStatus, request.getStatus());
        }
        if (request.getGroupId() != null) {
            wrapper.set(UserEntity::getGroupId, request.getGroupId());
        }

        wrapper.set(UserEntity::getUpdatedTime, LocalDateTime.now());
        userMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void updateUserBySelf(Long id, UpdateUserRequest request, Long currentUserId) {
        if (!id.equals(currentUserId)) {
            throw new BusinessException("无权限，只能修改自己的信息");
        }

        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (request.getUsername() != null && !request.getUsername().isEmpty()
                && !request.getUsername().equals(user.getUsername())) {
            if (getUserByUserName(request.getUsername()) != null) {
                throw new BusinessException("用户名已被占用");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw new BusinessException("修改密码需提供原密码");
            }
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BusinessException("原密码错误");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void resetPassword(Long id) {
        if (userMapper.selectById(id) == null) {
            throw new BusinessException("用户不存在");
        }
        String newPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        userMapper.update(null,
                new LambdaUpdateWrapper<UserEntity>()
                        .eq(UserEntity::getId, id)
                        .set(UserEntity::getPassword, passwordEncoder.encode(newPassword))
                        .set(UserEntity::getUpdatedTime, LocalDateTime.now())
        );
    }
}
