package com.lvtong.LvTongTransportDept.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 字段自动填充处理器
 *
 * 【填充规则】
 * - created_time：仅 INSERT 时自动填充
 * - updated_time：INSERT 和 UPDATE 时均自动填充
 *
 * 【使用前提】
 * 实体字段需标注对应 fill 策略：
 * <pre>
 * {@code @TableField(fill = FieldFill.INSERT) LocalDateTime createdTime;}
 * {@code @TableField(fill = FieldFill.INSERT_UPDATE) LocalDateTime updatedTime;}
 * </pre>
 *
 * @see com.lvtong.LvTongTransportDept.entity.UserEntity
 * @see com.lvtong.LvTongTransportDept.entity.VehicleInspection
 */
@Component
public class AutoFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
    }
}
