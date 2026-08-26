package com.zhush.web.core.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 相关异常处理
 *
 * <p>仅在项目引入 Sa-Token 时由 {@code WebAutoConfiguration} 条件性装配。
 *
 * @author zhush
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@RestControllerAdvice
public class SaTokenExceptionAdvice {

    /**
     * 未登录 / 登录态过期：返回 <b>HTTP 401</b>（而非 200 + 业务码），
     * 供前端拦截器识别后触发 refresh-token 无感刷新，刷新失败再跳登录页。
     * Body 仍保持统一 {@code Result} 结构（code = 1000401）。
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<?>> handleNotLogin(NotLoginException e) {
        log.warn("[NotLogin] type={}, msg={}", e.getType(), e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(GlobalErrorCode.UNAUTHORIZED));
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<?> handleNotPermission(NotPermissionException e) {
        log.warn("[NotPermission] {}", e.getPermission());
        return Result.error(GlobalErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(NotRoleException.class)
    public Result<?> handleNotRole(NotRoleException e) {
        log.warn("[NotRole] {}", e.getRole());
        return Result.error(GlobalErrorCode.FORBIDDEN);
    }

    /**
     * 兜底：其他 Sa-Token 异常
     */
    @ExceptionHandler(SaTokenException.class)
    public Result<?> handleSaToken(SaTokenException e) {
        log.warn("[SaTokenException] code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(GlobalErrorCode.FORBIDDEN.getCode(), e.getMessage());
    }

}
