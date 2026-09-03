package com.zhush.system.service;

import cn.hutool.core.util.StrUtil;
import com.zhush.common.response.PageResult;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.vo.MonitorCacheDetailVO;
import com.zhush.system.vo.MonitorCacheKeyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.zhush.common.exception.ServiceExceptionUtil.assertFalse;
import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;
import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 缓存监控（Redis）服务
 *
 * <p>基于 {@link StringRedisTemplate} 提供：实例信息、key 分页（SCAN 而非 KEYS，
 * 避免阻塞 O(N)）、key 详情（按类型读取值并截断预览）、删除（带关键前缀黑名单保护）。
 *
 * @author zhush
 */
@Slf4j
@Service
public class CacheMonitorService {

    /** SCAN 最多收集的 key 数，防止超大数据量卡顿 */
    private static final int MAX_SCAN_KEYS = 10000;

    /** 预览最大字符数，超出即截断 */
    private static final int MAX_VALUE_PREVIEW = 1000;

    /** 禁止删除的 key 前缀（认证/会话等关键路径） */
    private static final List<String> FORBIDDEN_PREFIXES = List.of("sa:", "refresh:", "login:");

    private final StringRedisTemplate redisTemplate;

    public CacheMonitorService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Redis 实例信息：版本、运行模式、内存、连接数、命中率、keyspace 等
     */
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        // 显式指定 RedisCallback 以消除与 SessionCallback 的重载歧义
        Properties props = redisTemplate.execute(
                (RedisCallback<Properties>) connection -> connection.serverCommands().info());
        if (props == null) {
            return info;
        }
        props.stringPropertyNames().forEach(name -> info.put(name, props.getProperty(name)));

        long hits = toLong(info.get("keyspace_hits"));
        long misses = toLong(info.get("keyspace_misses"));
        double hitRate = (hits + misses) == 0 ? 0 : (double) hits / (hits + misses) * 100;
        info.put("keyspace_hit_rate", Math.round(hitRate * 100) / 100.0);
        return info;
    }

    /**
     * key 分页：按 pattern 模糊搜索（默认 {@code *}），基于 SCAN 迭代、内存分页
     */
    public PageResult<MonitorCacheKeyVO> getKeyPage(String pattern, int pageNum, int pageSize) {
        String match = StrUtil.blankToDefault(pattern, "*");
        List<MonitorCacheKeyVO> all = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(match).count(200).build())) {
            while (cursor.hasNext()) {
                if (all.size() >= MAX_SCAN_KEYS) {
                    log.warn("[CacheMonitor] SCAN 数量超过 {}，停止收集", MAX_SCAN_KEYS);
                    break;
                }
                all.add(toKeyVO(cursor.next()));
            }
        } catch (Exception e) {
            log.error("[CacheMonitor] 扫描缓存键失败, pattern={}", match, e);
            throw exception(SystemErrorCode.MONITOR_CACHE_SCAN_FAILED, match);
        }
        return pageList(all, pageNum, pageSize);
    }

    /**
     * key 详情：按类型读取值并做可读化预览（截断）
     */
    public MonitorCacheDetailVO getDetail(String key) {
        assertNotNull(key, SystemErrorCode.MONITOR_CACHE_KEY_NOT_FOUND, key);
        assertTrue(!StrUtil.isBlank(key), SystemErrorCode.MONITOR_CACHE_KEY_NOT_FOUND, key);

        DataType type = redisTemplate.type(key);
        assertNotNull(type, SystemErrorCode.MONITOR_CACHE_KEY_NOT_FOUND, key);
        String typeName = type.name().toLowerCase();

        MonitorCacheDetailVO vo = new MonitorCacheDetailVO();
        vo.setKey(key);
        vo.setType(typeName);
        vo.setTtl(redisTemplate.getExpire(key, TimeUnit.SECONDS));
        vo.setSize(readSize(key, typeName));

        Object value = readValue(key, typeName);
        String preview = serializeValue(value);
        if (preview != null && preview.length() > MAX_VALUE_PREVIEW) {
            vo.setTruncated(true);
            vo.setValuePreview(preview.substring(0, MAX_VALUE_PREVIEW));
        } else if (preview != null) {
            vo.setTruncated(false);
            vo.setValuePreview(preview);
        }
        vo.setEncoding("raw");
        return vo;
    }

    /**
     * 删除 key：命中关键前缀黑名单则拒绝；key 不存在返回错误
     */
    public void deleteKey(String key) {
        assertTrue(!StrUtil.isBlank(key), SystemErrorCode.MONITOR_CACHE_KEY_NOT_FOUND, key);
        boolean forbidden = FORBIDDEN_PREFIXES.stream().anyMatch(key::startsWith);
        assertFalse(forbidden, SystemErrorCode.MONITOR_CACHE_KEY_FORBIDDEN, key);
        Boolean deleted = redisTemplate.delete(key);
        assertTrue(Boolean.TRUE.equals(deleted), SystemErrorCode.MONITOR_CACHE_KEY_NOT_FOUND, key);
    }

    // ================ 内部 ================

    private MonitorCacheKeyVO toKeyVO(String key) {
        MonitorCacheKeyVO vo = new MonitorCacheKeyVO();
        vo.setKey(key);
        DataType type = redisTemplate.type(key);
        String typeName = type == null ? null : type.name().toLowerCase();
        vo.setType(typeName);
        vo.setTtl(redisTemplate.getExpire(key, TimeUnit.SECONDS));
        vo.setSize(readSize(key, typeName));
        return vo;
    }

    /**
     * value 大小：string 为字节长度，其余为元素数量
     */
    private Long readSize(String key, String typeName) {
        if (typeName == null) {
            return null;
        }
        try {
            return switch (typeName) {
                case "string" -> redisTemplate.opsForValue().size(key);
                case "list" -> redisTemplate.opsForList().size(key);
                case "set" -> redisTemplate.opsForSet().size(key);
                case "zset" -> redisTemplate.opsForZSet().size(key);
                case "hash" -> redisTemplate.opsForHash().size(key);
                default -> null;
            };
        } catch (Exception e) {
            log.warn("[CacheMonitor] 读取 key 大小失败, key={}", key, e);
            return null;
        }
    }

    /**
     * 按类型读取 value（list/set/zset 限制条目数，避免大 key 撑爆响应）
     */
    private Object readValue(String key, String typeName) {
        try {
            return switch (typeName) {
                case "string" -> redisTemplate.opsForValue().get(key);
                case "list" -> redisTemplate.opsForList().range(key, 0, 199);
                case "set" -> redisTemplate.opsForSet().members(key);
                case "zset" -> redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 199);
                case "hash" -> redisTemplate.opsForHash().entries(key);
                default -> null;
            };
        } catch (Exception e) {
            log.warn("[CacheMonitor] 读取 key 值失败, key={}", key, e);
            return null;
        }
    }

    private String serializeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str;
        }
        // 集合/Map/score tuple 统一走 toString，仅作预览
        return value.toString();
    }

    private PageResult<MonitorCacheKeyVO> pageList(List<MonitorCacheKeyVO> all, int pageNum, int pageSize) {
        int total = all.size();
        int size = Math.max(1, pageSize);
        int from = Math.max(0, (Math.max(1, pageNum) - 1) * size);
        int to = Math.min(from + size, total);
        List<MonitorCacheKeyVO> list = from >= to ? Collections.emptyList() : all.subList(from, to);
        return PageResult.of(list, (long) total);
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
