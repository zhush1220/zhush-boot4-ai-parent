package com.zhush.system.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.stat.DruidStatManagerFacade;
import com.zhush.system.enums.SystemErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * 数据监控（Druid）服务
 *
 * <p>通过 {@link DruidStatManagerFacade} 读取 Druid 统计的**内存态只读**数据：数据源、
 * SQL。Druid Web（/druid/*）默认关闭，前端统一走本类暴露的授权接口。
 *
 * <p>注：当前引入的 Druid 1.2.28（重写版）Facade 仅提供数据源 / SQL / Wall 统计，已无独立的
 * URI 统计接口，故 {@link #getUriList()} 返回空列表。
 *
 * @author zhush
 */
@Slf4j
@Service
public class DruidMonitorService {

    /** Druid Wall 统计 map 中承载列表的字段名 */
    private static final String WALL_LIST_KEY = "wallList";

    /**
     * 数据源监控列表：连接数 / 活跃 / 空闲 / 提交 / 回滚 / 最大并发等
     */
    public List<Map<String, Object>> getDataSourceList() {
        DruidStatManagerFacade facade = requireFacade();
        List<Map<String, Object>> dataSources = facade.getDataSourceStatDataList();
        assertNotNull(dataSources, SystemErrorCode.MONITOR_DRUID_DISABLED, "Druid 数据源统计未采集");
        return dataSources;
    }

    /**
     * SQL 监控列表，可按 SQL 文本关键词过滤
     */
    public List<Map<String, Object>> getSqlList(String sql) {
        List<Map<String, Object>> list = safeList(
                requireFacade().getSqlStatDataList((Object) null));
        if (StrUtil.isBlank(sql)) {
            return list;
        }
        String keyword = sql.toLowerCase();
        return list.stream()
                .filter(item -> {
                    Object text = item.get("SQL");
                    return text != null && text.toString().toLowerCase().contains(keyword);
                })
                .toList();
    }

    /**
     * URI 访问监控列表：当前 Druid 版本无独立 URI 统计接口，返回空列表
     */
    public List<Map<String, Object>> getUriList() {
        return Collections.emptyList();
    }

    /**
     * Wall 防火墙统计列表：拦截数 / 黑名单 / SQL 注入命中
     */
    public List<Map<String, Object>> getWallList() {
        DruidStatManagerFacade facade = requireFacade();
        try {
            Map<String, Object> wall = facade.getWallStatMap((Integer) null);
            if (wall == null || wall.isEmpty()) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> inner = extractList(wall, WALL_LIST_KEY);
            return inner.isEmpty() ? List.of(wall) : inner;
        } catch (Exception e) {
            log.warn("[DruidMonitor] 读取 Wall 统计失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 重置全部监控统计
     */
    public void reset() {
        requireFacade().resetAll();
    }

    private DruidStatManagerFacade requireFacade() {
        DruidStatManagerFacade facade = DruidStatManagerFacade.getInstance();
        assertNotNull(facade, SystemErrorCode.MONITOR_DRUID_DISABLED, "Druid 未装配");
        return facade;
    }

    private List<Map<String, Object>> safeList(List<Map<String, Object>> list) {
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * 从 Druid stat map 中提取列表字段；不含列表字段时返回空列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractList(Map<String, Object> stat, String listKey) {
        if (stat == null) {
            return Collections.emptyList();
        }
        Object listObj = stat.get(listKey);
        if (listObj instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return Collections.emptyList();
    }
}
