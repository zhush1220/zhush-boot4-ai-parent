package com.zhush.biz.ip.core;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * IP 归属地解析结果
 *
 * <p>对应 ip2region v3 xdb 的 region 字符串格式：{@code 国家|省份|城市|运营商|国家代码}，
 * 例如：{@code 中国|广东省|深圳市|电信|CN}。无效字段（{@code 0} 或空串）统一置为 {@code null}。
 *
 * @author zhush
 */
@Data
public class IpRegion {

    /**
     * 内网 IP 的归属地描述
     */
    public static final String INNER_IP = "内网IP";

    /**
     * region 字段占位符，表示该字段无数据
     */
    private static final String EMPTY_FLAG = "0";

    /**
     * 原始 region 字符串，如：中国|广东省|深圳市|电信|CN
     */
    private String raw;

    /**
     * 国家，如：中国
     */
    private String country;

    /**
     * 省份，如：广东省
     */
    private String province;

    /**
     * 城市，如：深圳市
     */
    private String city;

    /**
     * 运营商，如：电信
     */
    private String isp;

    /**
     * 国家代码（ISO 3166-1 alpha-2），如：CN
     */
    private String countryCode;

    /**
     * 解析 ip2region 返回的 region 字符串
     *
     * @param region 原始 region 字符串，格式：国家|省份|城市|运营商|国家代码
     * @return 解析结果；region 为空时返回 {@code null}
     */
    public static IpRegion of(String region) {
        if (StrUtil.isBlank(region)) {
            return null;
        }
        IpRegion result = new IpRegion();
        result.setRaw(region);
        String[] parts = region.split("\\|", -1);
        result.setCountry(fieldOf(parts, 0));
        result.setProvince(fieldOf(parts, 1));
        result.setCity(fieldOf(parts, 2));
        result.setIsp(fieldOf(parts, 3));
        result.setCountryCode(fieldOf(parts, 4));
        return result;
    }

    /**
     * 构建内网 IP 的归属地结果
     */
    public static IpRegion inner() {
        IpRegion result = new IpRegion();
        result.setRaw(INNER_IP);
        result.setCountry(INNER_IP);
        return result;
    }

    /**
     * 拼接可读的地理位置，如：中国 广东省 深圳市（跳过无数据字段）
     *
     * @return 地理位置描述，无任何数据时返回空串
     */
    public String getAddress() {
        List<String> parts = new ArrayList<>(3);
        if (country != null) {
            parts.add(country);
        }
        if (province != null) {
            parts.add(province);
        }
        if (city != null) {
            parts.add(city);
        }
        return String.join(" ", parts);
    }

    /**
     * 拼接地理位置 + 运营商，如：中国 广东省 深圳市 电信
     */
    public String getFullAddress() {
        String address = getAddress();
        if (isp == null) {
            return address;
        }
        return address.isEmpty() ? isp : address + " " + isp;
    }

    private static String fieldOf(String[] parts, int index) {
        if (index >= parts.length) {
            return null;
        }
        String value = StrUtil.trim(parts[index]);
        return StrUtil.isBlank(value) || EMPTY_FLAG.equals(value) ? null : value;
    }

}
