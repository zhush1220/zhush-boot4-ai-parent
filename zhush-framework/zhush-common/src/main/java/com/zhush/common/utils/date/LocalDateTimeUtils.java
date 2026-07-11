package com.zhush.common.utils.date;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.zhush.common.constant.DateFormatConstant;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

/**
 *
 * @ClassName LocalDateTimeUtils
 * @Description 时间工具类
 * @Author zhush
 * @Date 2026/6/29 星期一 21:46
 *
 **/
public class LocalDateTimeUtils {

    public static LocalDateTime addTime(Duration duration) {
        return LocalDateTime.now().plus(duration);
    }

    public static LocalDateTime minusTime(Duration duration) {
        return LocalDateTime.now().minus(duration);
    }

    public static boolean beforeNow(LocalDateTime date) {
        return date.isBefore(LocalDateTime.now());
    }

    public static boolean afterNow(LocalDateTime date) {
        return date.isAfter(LocalDateTime.now());
    }

    public static LocalDateTime getNow() {
        return LocalDateTime.now();
    }

    /**
     * 解析时间
     * @param time 时间
     * @return 时间字符串
     */
    public static LocalDateTime parse(String time) {
        try {
            return LocalDateTimeUtil.parse(time, DatePattern.NORM_DATE_PATTERN);
        } catch (DateTimeParseException e) {
            return LocalDateTimeUtil.parse(time);
        }
    }

    /**
     * LocalDateTime 转 Date（使用系统默认时区）
     */
    public static Date toDate(LocalDateTime localDateTime) {
        return toDate(localDateTime, DateFormatConstant.DEFAULT_ZONE);
    }

    /**
     * LocalDateTime 转 Date（指定时区）
     * @param localDateTime LocalDateTime 对象
     * @param zoneId 时区，如 ZoneId.of("Asia/Shanghai")
     * @return Date 对象
     */
    public static Date toDate(LocalDateTime localDateTime, ZoneId zoneId) {
        if (localDateTime == null) {
            return null;
        }
        if (zoneId == null) {
            zoneId = DateFormatConstant.DEFAULT_ZONE;
        }
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * Date 转 LocalDateTime（使用系统默认时区）
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return toLocalDateTime(date, DateFormatConstant.DEFAULT_ZONE);
    }

    /**
     * Date 转 LocalDateTime（指定时区）
     * @param date Date 对象
     * @param zoneId 时区，如 ZoneId.of("Asia/Shanghai")
     * @return LocalDateTime 对象
     */
    public static LocalDateTime toLocalDateTime(Date date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        if (zoneId == null) {
            zoneId = DateFormatConstant.DEFAULT_ZONE;
        }
        Instant instant = date.toInstant();
        return instant.atZone(zoneId).toLocalDateTime();
    }

    /**
     * 判指定断时间，是否在该时间范围内
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param time 指定时间
     * @return 是否
     */
    public static boolean isBetween(LocalDateTime startTime, LocalDateTime endTime, Timestamp time) {
        if (startTime == null || endTime == null || time == null) {
            return false;
        }
        return LocalDateTimeUtil.isIn(LocalDateTimeUtil.of(time), startTime, endTime);
    }

    /**
     * 判指定断时间，是否在该时间范围内
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param time 指定时间
     * @return 是否
     */
    public static boolean isBetween(LocalDateTime startTime, LocalDateTime endTime, String time) {
        if (startTime == null || endTime == null || time == null) {
            return false;
        }
        return LocalDateTimeUtil.isIn(parse(time), startTime, endTime);
    }

    /**
     * 判断当前时间是否在该时间范围内
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 是否
     */
    public static boolean isBetween(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }
        return LocalDateTimeUtil.isIn(LocalDateTime.now(), startTime, endTime);
    }

    /**
     * 判断当前时间是否在该时间范围内
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 是否
     */
    public static boolean isBetween(String startTime, String endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }
        LocalDate nowDate = LocalDate.now();
        return LocalDateTimeUtil.isIn(LocalDateTime.now(),
                LocalDateTime.of(nowDate, LocalTime.parse(startTime)),
                LocalDateTime.of(nowDate, LocalTime.parse(endTime)));
    }

    /**
     * 判断时间段是否重叠
     *
     * @param startTime1 开始 time1
     * @param endTime1   结束 time1
     * @param startTime2 开始 time2
     * @param endTime2   结束 time2
     * @return 重叠：true 不重叠：false
     */
    public static boolean isOverlap(LocalTime startTime1, LocalTime endTime1, LocalTime startTime2, LocalTime endTime2) {
        LocalDate nowDate = LocalDate.now();
        return LocalDateTimeUtil.isOverlap(LocalDateTime.of(nowDate, startTime1), LocalDateTime.of(nowDate, endTime1),
                LocalDateTime.of(nowDate, startTime2), LocalDateTime.of(nowDate, endTime2));
    }

    /**
     * 获取指定日期与当前日期，相差几天，获取结果为负
     * @param dateTime 指定日期
     * @return 相差天数
     */
    public static Long between(LocalDateTime dateTime) {
        return LocalDateTimeUtil.between(dateTime, LocalDateTime.now(), ChronoUnit.DAYS);
    }

    /**
     * 获得指定日期所在季度
     *
     * @param date 日期
     * @return 所在季度
     */
    public static int getQuarterOfYear(LocalDateTime date) {
        return (date.getMonthValue() - 1) / 3 + 1;
    }


    // ========================= 开始时间 =========================
    /**
     * 获取今天的开始时间
     *
     * @return 今天
     */
    public static LocalDateTime getToday() {
        return LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
    }

    /**
     * 获取昨天的开始时间
     *
     * @return 昨天
     */
    public static LocalDateTime getYesterday() {
        return LocalDateTimeUtil.beginOfDay(LocalDateTime.now().minusDays(1));
    }

    /**
     * 获取本月的开始时间
     *
     * @return 本月
     */
    public static LocalDateTime getMonth() {
        return beginOfMonth(LocalDateTime.now());
    }

    /**
     * 获取本年的开始时间
     *
     * @return 本年
     */
    public static LocalDateTime getYear() {
        return LocalDateTime.now().with(TemporalAdjusters.firstDayOfYear()).with(LocalTime.MIN);
    }

    /**
     * 获取指定日期所在的月份的开始时间
     * 例如：2023-09-30 00:00:00,000
     *
     * @param date 日期
     * @return 月份的开始时间
     */
    public static LocalDateTime beginOfMonth(LocalDateTime date) {
        return date.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
    }

    // ========================= 结束时间 =========================
    /**
     * 获取今天的结束时间
     *
     * @return 今天
     */
    public static LocalDateTime getTodayEnd() {
        return LocalDateTimeUtil.endOfDay(LocalDateTime.now());
    }

    /**
     * 获取昨天的结束时间
     *
     * @return 昨天
     */
    public static LocalDateTime getYesterdayEnd() {
        return LocalDateTimeUtil.endOfDay(LocalDateTime.now().minusDays(1));
    }

    /**
     * 获取本月的结束时间
     *
     * @return 本月
     */
    public static LocalDateTime getMonthEnd() {
        return endOfMonth(LocalDateTime.now());
    }

    /**
     * 获取本年的结束时间
     *
     * @return 本年
     */
    public static LocalDateTime getYearEnd() {
        return LocalDateTime.now().with(TemporalAdjusters.lastDayOfYear()).with(LocalTime.MAX);
    }

    /**
     * 获取指定日期所在的月份的最后时间
     * 例如：2023-09-30 23:59:59,999
     *
     * @param date 日期
     * @return 月份的结束时间
     */
    public static LocalDateTime endOfMonth(LocalDateTime date) {
        return date.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
    }

}
