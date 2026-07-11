package com.zhush.common.annotation;

import java.lang.annotation.*;

/**
 * 标记需要加密存储的字段
 * <p>
 * 配合 MyBatis-Flex 的 {@code @Column(typeHandler = EncryptTypeHandler.class)} 使用，
 * 实现字段级的透明加解密。写入数据库时自动 SM4 加密，读取时自动解密。
 * <p>
 * 使用示例：
 * <pre>{@code
 * @Column(value = "phone", typeHandler = EncryptTypeHandler.class)
 * @EncryptField
 * private String phone;
 * }</pre>
 *
 * @author zhush
 * @since 2026/7/11
 * @see com.zhush.mybatis.core.handler.EncryptTypeHandler
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptField {

}
