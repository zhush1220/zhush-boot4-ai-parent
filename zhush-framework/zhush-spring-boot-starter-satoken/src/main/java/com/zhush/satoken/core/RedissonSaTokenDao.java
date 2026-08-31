package com.zhush.satoken.core;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoForRedisTemplate;

/**
 * 兼容 Redisson 的 Sa-Token Redis 存储实现
 *
 * <p>官方 {@link SaTokenDaoForRedisTemplate#setStringAndKeepTTL} 依赖
 * {@code Expiration.keepTtl()}（即 Redis 6.0 的 {@code SET key value KEEPTTL} 语义）。
 * 而项目底层使用 Redisson 连接（{@code RedissonConnection}），其未实现 KEEPTTL 语义，
 * 会将哨兵值 {@code -2} 误当作毫秒过期时间，最终下发 {@code SET ... PX -2000 XX}，
 * 触发 Redis 报错 {@code ERR invalid expire time in set}（登录创建会话 {@code addTerminal} 时必现）。
 *
 * <p>本实现改为显式读取剩余 TTL 后重新写入：
 * <ul>
 *     <li>key 不存在（{@link SaTokenDao#NOT_VALUE_EXPIRE}，首次写入）：以全局配置的超时时间兜底</li>
 *     <li>永不过期（{@link SaTokenDao#NEVER_EXPIRE}）与正常秒数：交由父类 {@code set} 处理
 *         （-1 写无过期，正数按秒写，0/-2 跳过）</li>
 * </ul>
 *
 * @author zhush
 */
public class RedissonSaTokenDao extends SaTokenDaoForRedisTemplate {

    @Override
    public void setStringAndKeepTTL(String key, String value) {
        long timeout = getTimeout(key);
        // key 不存在（首次写入）：用全局配置的超时时间兜底，避免下发负数 TTL
        if (timeout == SaTokenDao.NOT_VALUE_EXPIRE) {
            timeout = SaManager.getConfig().getTimeout();
        }
        set(key, value, timeout);
    }
}
