package com.lvtong.LvTongTransportDept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lvtong.LvTongTransportDept.entity.UserEntity;


/**
 * 用户 Mapper
 * - 复杂查询（多条件组合）放到 Service 层用 QueryWrapper 实现
 */
public interface UserMapper extends BaseMapper<UserEntity> {
}
