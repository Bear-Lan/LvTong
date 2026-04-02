package com.lvtong.LvTongTransportDept.constant;

public class UserConstants {

    private UserConstants() {}

    // 角色（权限等级）
    // 0 = 管理员：可管理所有用户，修改他人信息、角色、用户组、状态
    // 1 = 普通员工：只能查看用户列表，仅可修改自己的个人信息（用户名、密码、姓名、邮箱、电话）
    public static final int ROLE_ADMIN = 0;
    public static final int ROLE_USER = 1;

    // 状态: 0=正常, -1=已禁用
    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_DISABLED = -1;

    public static String getRoleText(Integer role) {
        if (role == null) return "未知";
        switch (role) {
            case ROLE_ADMIN: return "管理员";
            case ROLE_USER: return "普通用户";
            default: return "未知";
        }
    }

    public static String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case STATUS_ACTIVE: return "正常";
            case STATUS_DISABLED: return "已禁用";
            default: return "未知";
        }
    }
}
