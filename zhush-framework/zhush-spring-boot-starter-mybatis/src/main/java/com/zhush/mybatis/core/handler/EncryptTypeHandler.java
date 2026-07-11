package com.zhush.mybatis.core.handler;

import cn.hutool.core.util.StrUtil;
import com.zhush.common.utils.SmUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SM4 字段加密 TypeHandler
 * <p>
 * 核心功能：
 * <ul>
 *     <li>写入数据库时自动 SM4/ECB 加密（Hex 编码存储）</li>
 *     <li>读取时智能判断：密文自动解密，明文原样返回（兼容旧数据）</li>
 *     <li>支持全局开关：通过 {@code zhush.encrypt.enabled} 控制是否启用加解密</li>
 * </ul>
 * <p>
 * 使用方式：在实体字段上配合 {@code @Column(typeHandler = EncryptTypeHandler.class)} 使用。
 *
 * @author zhush
 * @since 2026/7/11
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class EncryptTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    String parameter, JdbcType jdbcType) throws SQLException {
        if (!EncryptContextHolder.isEnabled()) {
            ps.setString(i, parameter);
            return;
        }
        ps.setString(i, SmUtils.sm4EncryptEcb(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return doDecrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return doDecrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return doDecrypt(cs.getString(columnIndex));
    }

    /**
     * 智能解密：自动判断值是密文还是明文
     * <ul>
     *     <li>全局开关关闭 → 原样返回</li>
     *     <li>值为 null 或空 → 原样返回</li>
     *     <li>值为合法 Hex 且长度 ≥ 32（SM4 最小密文块）→ 尝试解密</li>
     *     <li>解密成功 → 返回明文</li>
     *     <li>解密失败或非 Hex → 判定为明文，原样返回（兼容旧数据）</li>
     * </ul>
     */
    private String doDecrypt(String value) {
        if (!EncryptContextHolder.isEnabled()) {
            return value;
        }
        if (StrUtil.isBlank(value)) {
            return value;
        }
        // SM4/ECB 加密后为 Hex 编码，最小密文长度 = 32（16 字节 = 1 个 block）
        // 且 Hex 字符串长度必须为偶数
        if (value.length() < 32 || value.length() % 2 != 0) {
            return value;
        }
        if (!isHex(value)) {
            return value;
        }
        try {
            return SmUtils.sm4DecryptEcb(value);
        } catch (Exception e) {
            // 解密失败 → 判定为明文旧数据，原样返回
            return value;
        }
    }

    /**
     * 判断字符串是否为合法的 Hex 编码
     */
    private static boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }
}
