package com.lvtong.LvTongTransportDept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lvtong.LvTongTransportDept.entity.UserGroup;
import com.lvtong.LvTongTransportDept.entity.UserEntity;
import com.lvtong.LvTongTransportDept.entity.VehicleInspection;
import com.lvtong.LvTongTransportDept.exception.BusinessException;
import com.lvtong.LvTongTransportDept.mapper.UserGroupMapper;
import com.lvtong.LvTongTransportDept.mapper.UserMapper;
import com.lvtong.LvTongTransportDept.mapper.VehicleInspectionMapper;
import com.lvtong.LvTongTransportDept.service.UserGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserGroupServiceImpl implements UserGroupService {

    @Autowired
    private UserGroupMapper groupMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VehicleInspectionMapper inspectionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserGroup> getAllGroups() {
        return groupMapper.selectList(
                new LambdaQueryWrapper<UserGroup>()
                        .orderByAsc(UserGroup::getId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserGroup> getGroupById(Long id) {
        UserGroup group = groupMapper.selectById(id);
        return Optional.ofNullable(group);
    }

    @Override
    @Transactional
    public UserGroup createGroup(UserGroup group) {
        if (group.getName() == null || group.getName().isBlank()) {
            throw new BusinessException("班组名称不能为空");
        }
        if (getGroupByName(group.getName()).isPresent()) {
            throw new BusinessException("班组名称已存在");
        }
        groupMapper.insert(group);
        return group;
    }

    @Override
    @Transactional
    public void updateGroup(Long id, UserGroup group) {
        UserGroup existing = groupMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("班组不存在");
        }
        if (group.getName() != null && !group.getName().isBlank()) {
            Optional<UserGroup> conflict = getGroupByName(group.getName());
            if (conflict.isPresent() && !conflict.get().getId().equals(id)) {
                throw new BusinessException("班组名称已存在");
            }
            existing.setName(group.getName());
        }
        if (group.getDescription() != null) {
            existing.setDescription(group.getDescription());
        }
        if (group.getLeader() != null) {
            existing.setLeader(group.getLeader());
        }
        groupMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        // 检查是否有查验记录：有则拒绝删除（查验数据不能静默丢失）
        long recordCount = inspectionMapper.selectCount(
                new LambdaQueryWrapper<VehicleInspection>()
                        .eq(VehicleInspection::getGroupId, String.valueOf(id))
        );
        if (recordCount > 0) {
            throw new BusinessException("该班组有 " + recordCount + " 条查验记录，无法删除");
        }
        // 将关联用户的 group_id 置为 NULL（允许删除，关联自动解除）
        userMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserEntity>()
                        .eq(UserEntity::getGroupId, id)
                        .set(UserEntity::getGroupId, null)
        );
        groupMapper.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserGroup> getGroupByName(String name) {
        UserGroup group = groupMapper.selectOne(
                new LambdaQueryWrapper<UserGroup>()
                        .eq(UserGroup::getName, name)
        );
        return Optional.ofNullable(group);
    }
}
