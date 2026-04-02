package com.lvtong.LvTongTransportDept.exception;

import com.lvtong.LvTongTransportDept.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局统一异常处理器
 *
 * 【设计说明】
 * 所有 Controller 层抛出的异常均在此统一捕获，转换为标准的 ApiResponse 返回给前端。
 * 避免在每个 Controller 中单独 try-catch，减少代码重复。
 *
 * 【异常处理原则】
 * 1. 业务异常（BusinessException）：消息直接透传给前端
 * 2. 参数校验异常（MethodArgumentNotValidException）：提取所有字段错误，拼接中文提示
 * 3. 参数类型不匹配（MethodArgumentTypeMismatchException）：提示正确的参数类型
 * 4. 文件上传超限（MaxUploadSizeExceededException）：提示文件大小限制
 * 5. 其他未知异常：返回通用错误消息，不暴露内部堆栈信息（防止信息泄露）
 *
 * 【安全说明】
 * 对未知异常（Exception）隐藏具体错误信息，只返回"服务器内部错误"，
 * 避免将数据库结构、代码路径、第三方库信息泄露给前端。
 *
 * @see BusinessException
 * @see ApiResponse
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ================================================================
    // 业务异常（可预期错误）
    // ================================================================

    /**
     * 处理业务异常
     * 消息直接透传给前端，HTTP 状态码由异常中的 code 字段决定。
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<String> handleBusinessException(BusinessException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    // ================================================================
    // 参数校验异常
    // ================================================================

    /**
     * 处理参数校验异常（@Valid 注解触发的校验失败）
     * 将所有字段错误拼接为一条中文提示。
     * 例如：["用户名不能为空", "密码长度不能少于6位"]
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<String> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理参数类型不匹配（如：枚举值不存在、日期格式错误）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<String> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String paramName = e.getName();
        String expectedType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知";
        String message = String.format("参数【%s】类型错误，期望 %s 类型", paramName, expectedType);
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    // ================================================================
    // 文件上传异常
    // ================================================================

    /**
     * 处理文件上传超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return ApiResponse.error("文件大小超出限制，请压缩后重试");
    }

    // ================================================================
    // 认证授权异常
    // ================================================================

    /**
     * 处理 Spring Security 认证异常
     * 由 JwtInterceptor 抛出 ResponseStatusException(403) 时触发。
     */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ApiResponse<String> handleResponseStatusException(org.springframework.web.server.ResponseStatusException e) {
        return ApiResponse.error(e.getStatusCode().value(), e.getReason());
    }

    // ================================================================
    // 未知异常（兜底）
    // ================================================================

    /**
     * 处理所有未分类的异常
     *
     * 【安全注意】
     * 异常消息不暴露具体堆栈、SQL 语句、数据库结构等敏感信息。
     * 仅在日志中记录完整错误，便于运维排查。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<String> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return ApiResponse.error("服务器内部错误，请稍后重试");
    }
}
