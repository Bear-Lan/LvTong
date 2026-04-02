package com.lvtong.LvTongTransportDept.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 班组实体（UserGroup）
 *
 * 【字段说明】
 * - id：自增主键，对应 users.group_id 和 vehicle_inspections.group_id
 * - name：班组名称，如"班组1"、"一班"
 * - description：班组描述/备注（可选）
 * - leader：班长姓名（可选）
 */
@TableName("user_groups")
public class UserGroup {

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班组名称（必填，如：班组1、一班） */
    @TableField("name")
    private String name;

    /** 班组描述/备注（可选） */
    @TableField("description")
    private String description;

    /** 班长姓名（可选） */
    @TableField("leader")
    private String leader;

    /** 创建时间（MP 自动填充） */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /** 更新时间（MP 自动填充） */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    // ===== Getter & Setter =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLeader() { return leader; }
    public void setLeader(String leader) { this.leader = leader; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
