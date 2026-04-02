package com.lvtong.LvTongTransportDept.service;

import com.lvtong.LvTongTransportDept.entity.UserGroup;
import java.util.List;
import java.util.Optional;

/**
 * 班组服务接口
 */
public interface UserGroupService {

    /**
     * 查询所有班组（用于下拉列表，按 ID 升序）
     *
     * @return 所有班组列表
     */
    List<UserGroup> getAllGroups();

    /**
     * 根据 ID 查询单个班组
     *
     * @param id 班组 ID
     * @return 班组实体，不存在返回 null
     */
    Optional<UserGroup> getGroupById(Long id);

    /**
     * 管理员新增班组
     *
     * @param group 班组实体（至少需要 name）
     * @return 创建后的班组实体（含自增 ID）
     */
    UserGroup createGroup(UserGroup group);

    /**
     * 管理员更新班组
     *
     * @param id   班组 ID
     * @param group 待更新的字段
     */
    void updateGroup(Long id, UserGroup group);

    /**
     * 管理员删除班组
     * 删除前检查是否有关联用户和查验记录，有则拒绝删除
     *
     * @param id 班组 ID
     */
    void deleteGroup(Long id);

    /**
     * 根据名称精确查找（用于数据校验）
     *
     * @param name 班组名称
     * @return 班组实体，不存在返回 null
     */
    Optional<UserGroup> getGroupByName(String name);
}
