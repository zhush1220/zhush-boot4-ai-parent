package com.zhush.web.core.handler;

import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Spring Data 数据访问异常处理
 *
 * <p>仅在项目引入 spring-tx 时由 {@code WebAutoConfiguration} 条件性装配。
 *
 * @author zhush
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@RestControllerAdvice
public class DataAccessExceptionAdvice {

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<?> handleDuplicateKey(DuplicateKeyException e) {
        log.error("[DuplicateKey] {}", e.getMessage());
        return Result.error(GlobalErrorCode.DB_DUPLICATE_KEY);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<?> handleIntegrityViolation(DataIntegrityViolationException e) {
        log.error("[DataIntegrityViolation] {}", e.getMessage());
        return Result.error(GlobalErrorCode.DB_INTEGRITY_VIOLATION);
    }

}
