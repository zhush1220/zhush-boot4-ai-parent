package com.zhush.system.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zhush.redis.core.RedisCache;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 管理端 refresh-token（刷新令牌）服务
 *
 * <p>双 Token 机制中的"续期令牌"：
 * <ul>
 *     <li>登录成功签发，存 Redis（7 天 TTL），经 <b>HttpOnly Cookie</b> 下发浏览器；</li>
 *     <li>access-token（Sa-Token 令牌，2 小时）过期后，前端凭 refresh-token 调
 *         {@code /admin/auth/refresh} 换取新 access-token，实现无感续期；</li>
 *     <li><b>单次使用即作废</b>（轮换）：每次刷新后旧令牌立即失效并下发新令牌，防重放；</li>
 *     <li>登出时作废，并清除 Cookie。</li>
 * </ul>
 *
 * <p>读取采用"双通道"以兼容非浏览器端（小程序等无 Cookie 环境）：
 * 优先 Cookie，其次请求头 {@code X-Refresh-Token}，最后请求参数 {@code refreshToken}。
 *
 * @author zhush
 */
@Slf4j
@Service
public class RefreshTokenService {

    /**
     * refresh-token 下发的 Cookie 名
     */
    public static final String REFRESH_COOKIE_NAME = "refresh-token";

    /**
     * 兼容非浏览器端的请求头名
     */
    public static final String REFRESH_HEADER_NAME = "X-Refresh-Token";

    /**
     * 兼容非浏览器端的请求参数名
     */
    public static final String REFRESH_PARAM_NAME = "refreshToken";

    /**
     * Redis key 前缀：完整形如 {@code zhush:auth:refresh:{token}}
     */
    private static final String KEY_PREFIX = "zhush:auth:refresh:";

    /**
     * refresh-token 有效期：7 天
     */
    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    /**
     * Redis 值中 userId 与 device 的分隔符
     */
    private static final String VALUE_SEPARATOR = ":";

    private final RedisCache redisCache;

    public RefreshTokenService(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    // ================ 签发 / 作废 ================

    /**
     * 签发 refresh-token 并写入 Redis（7 天 TTL）
     *
     * @return 新签发的 refresh-token
     */
    public String issue(Long userId, String device) {
        String token = IdUtil.fastSimpleUUID();
        redisCache.set(KEY_PREFIX + token, userId + VALUE_SEPARATOR + StrUtil.nullToEmpty(device), REFRESH_TTL);
        return token;
    }

    /**
     * 校验并消费 refresh-token（<b>一次性</b>：无论有效与否立即从 Redis 删除，防重放）
     *
     * @return 载荷（userId + device）；令牌无效 / 过期返回 {@code null}
     */
    public Payload consume(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        // getAndDelete 原子取出并删除，保证单次使用
        Object value = redisCache.getRedisTemplate().opsForValue().getAndDelete(KEY_PREFIX + token);
        if (value == null) {
            return null;
        }
        return parse(value.toString());
    }

    /**
     * 作废 refresh-token（登出时调用）
     */
    public void invalidate(String token) {
        if (StrUtil.isNotBlank(token)) {
            redisCache.delete(KEY_PREFIX + token);
        }
    }

    // ================ Cookie 下发 / 清除 ================

    /**
     * 下发 refresh-token 到 HttpOnly Cookie（SameSite=Lax，Path=/，7 天）
     */
    public void writeCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(REFRESH_TTL)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 清除 refresh-token Cookie（登出 / 刷新失败时调用）
     */
    public void clearCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // ================ 双通道读取 ================

    /**
     * 从请求中解析 refresh-token：Cookie 优先，其次请求头，最后请求参数
     */
    public String resolveFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // 1. Cookie（Web 端）
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_COOKIE_NAME.equals(cookie.getName()) && StrUtil.isNotBlank(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        // 2. 请求头（非浏览器端）
        String header = request.getHeader(REFRESH_HEADER_NAME);
        if (StrUtil.isNotBlank(header)) {
            return header;
        }
        // 3. 请求参数（非浏览器端兜底）
        return request.getParameter(REFRESH_PARAM_NAME);
    }

    // ================ 内部 ================

    private Payload parse(String value) {
        int idx = value.indexOf(VALUE_SEPARATOR);
        if (idx <= 0) {
            log.warn("[RefreshToken] Redis 值格式非法: {}", value);
            return null;
        }
        try {
            Long userId = Long.valueOf(value.substring(0, idx));
            String device = value.substring(idx + 1);
            return new Payload(userId, device);
        } catch (NumberFormatException e) {
            log.warn("[RefreshToken] userId 解析失败: {}", value);
            return null;
        }
    }

    /**
     * refresh-token 载荷
     */
    public record Payload(Long userId, String device) {
    }

}
