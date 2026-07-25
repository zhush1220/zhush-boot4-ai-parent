package com.zhush.biz.ip.core;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.lang.Validator;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.service.Ip2Region;

/**
 * IP 归属地查询工具类（基于 ip2region 离线库）
 *
 * <p>同时支持 IPv4 和 IPv6 地址，内部自动识别版本；查询服务由
 * {@code IpRegionAutoConfiguration} 在启动时注入，并发安全。
 *
 * <p>使用示例：
 * <pre>{@code
 * IpRegionUtils.getRegion("113.92.157.29");                          // IPv4
 * IpRegionUtils.getRegion("240e:3b7:3272:d8d0:db09:c067:8d59:539e"); // IPv6
 * IpRegionUtils.getAddress("113.92.157.29");                         // 中国 广东省 深圳市
 * }</pre>
 *
 * @author zhush
 */
@Slf4j
public class IpRegionUtils {

    /**
     * IPv6 环回地址（简写与全写）
     */
    private static final String IPV6_LOOPBACK_SHORT = "::1";
    private static final String IPV6_LOOPBACK_FULL = "0:0:0:0:0:0:0:1";

    /**
     * ip2region 查询服务，由自动配置注入
     */
    private static volatile Ip2Region ip2Region;

    private IpRegionUtils() {
    }

    /**
     * 初始化查询服务（由自动配置调用，业务代码无需关心）
     */
    public static void init(Ip2Region service) {
        ip2Region = service;
    }

    /**
     * 是否为合法的 IPv4 地址
     */
    public static boolean isIpv4(String ip) {
        return StrUtil.isNotBlank(ip) && Validator.isIpv4(ip);
    }

    /**
     * 是否为合法的 IPv6 地址
     */
    public static boolean isIpv6(String ip) {
        return StrUtil.isNotBlank(ip) && Validator.isIpv6(ip);
    }

    /**
     * 是否为合法的 IP 地址（IPv4 或 IPv6）
     */
    public static boolean isIp(String ip) {
        return isIpv4(ip) || isIpv6(ip);
    }

    /**
     * 是否为内网 / 环回地址
     */
    public static boolean isInnerIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        if (IPV6_LOOPBACK_SHORT.equals(ip) || IPV6_LOOPBACK_FULL.equalsIgnoreCase(ip)) {
            return true;
        }
        return isIpv4(ip) && NetUtil.isInnerIP(ip);
    }

    /**
     * 查询 IP 归属地原始 region 字符串
     *
     * @param ip IPv4 或 IPv6 地址
     * @return region 字符串，格式：国家|省份|城市|运营商|国家代码；查询失败或无数据返回 {@code null}
     */
    public static String searchRaw(String ip) {
        if (!isIp(ip)) {
            return null;
        }
        Ip2Region service = ip2Region;
        if (service == null) {
            log.warn("[searchRaw][ip2region 查询服务未初始化，无法查询 IP({})]", ip);
            return null;
        }
        try {
            String region = service.search(ip);
            return StrUtil.isBlank(region) ? null : region;
        } catch (Exception e) {
            log.error("[searchRaw][查询 IP({}) 归属地失败]", ip, e);
            return null;
        }
    }

    /**
     * 查询 IP 归属地并解析为结构化对象
     *
     * @param ip IPv4 或 IPv6 地址
     * @return 归属地信息；内网地址返回 {@link IpRegion#inner()}，查询失败或无数据返回 {@code null}
     */
    public static IpRegion getRegion(String ip) {
        if (isInnerIp(ip)) {
            return IpRegion.inner();
        }
        return IpRegion.of(searchRaw(ip));
    }

    /**
     * 查询 IP 的地理位置描述，如：中国 广东省 深圳市
     *
     * @param ip IPv4 或 IPv6 地址
     * @return 地理位置描述，查询失败或无数据返回 {@code null}
     */
    public static String getAddress(String ip) {
        IpRegion region = getRegion(ip);
        return region == null ? null : StrUtil.emptyToNull(region.getAddress());
    }

    /**
     * 查询 IP 的地理位置 + 运营商描述，如：中国 广东省 深圳市 电信
     *
     * @param ip IPv4 或 IPv6 地址
     * @return 描述文本，查询失败或无数据返回 {@code null}
     */
    public static String getFullAddress(String ip) {
        IpRegion region = getRegion(ip);
        return region == null ? null : StrUtil.emptyToNull(region.getFullAddress());
    }

    /**
     * 查询 IP 的运营商，如：电信
     *
     * @param ip IPv4 或 IPv6 地址
     * @return 运营商名称，查询失败或无数据返回 {@code null}
     */
    public static String getIsp(String ip) {
        IpRegion region = getRegion(ip);
        return region == null ? null : region.getIsp();
    }

}
