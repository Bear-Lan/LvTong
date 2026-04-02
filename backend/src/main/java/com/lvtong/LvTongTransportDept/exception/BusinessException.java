package com.lvtong.LvTongTransportDept.exception;

/**
 * 业务异常类
 *
 * 【设计说明】
 * 用于表示业务层产生的可预期异常（如：用户名已存在、记录不存在、权限不足等）。
 * 此类继承 RuntimeException，无需在方法签名中声明，抛出后由 GlobalExceptionHandler 统一捕获。
 *
 * 【与 RuntimeException 的区别】
 * - RuntimeException：系统级异常（空指针、数组越界等不可恢复的错误）
 * - BusinessException：业务级异常（可预期、可恢复，需要提示用户具体原因）
 *
 * 【使用示例】
 * throw new BusinessException("用户名已存在");
 * throw new BusinessException(400, "无权限访问该资源");
 *
 * 【注意】
 * 异常消息（message）应面向用户，语言简洁清晰，禁止将数据库结构、
 * 堆栈信息、SQL 语句等内部细节暴露给前端。
 */
public class BusinessException extends RuntimeException {

    /** HTTP 状态码，默认 400（客户端错误） */
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
