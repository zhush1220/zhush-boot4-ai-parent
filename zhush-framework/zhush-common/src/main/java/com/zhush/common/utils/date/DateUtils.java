package com.zhush.common.utils.date;

import com.zhush.common.constant.DateFormatConstant;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @ClassName DateUtils
 * @Description 时间工具类
 * @Author zhush
 * @Date 2026/6/29 星期一 21:46
 *
 **/
public class DateUtils {

    public static Date getDateNow() {
        return new Date();
    }

    // ================================ 日期转换为字符串 ================================

    /**
     * 将日期转换为字符串
     * @param format
     * @param date
     * @return
     */
    public static String parseDateToStr(final String format, final Date date) {
        return new SimpleDateFormat(format).format(date);
    }

    /**
     * 获取当前日期 格式为yyyy-MM-dd
     * @return String
     */
    public static String getFormatDate() {
        return parseDateToStr(DateFormatConstant.FORMAT_DATE, getDateNow());
    }

    /**
     * 获取指定日期 格式为yyyy-MM-dd
     * @return String
     */
    public static String getFormatDate(Date date) {
        return parseDateToStr(DateFormatConstant.FORMAT_DATE, date);
    }

    /**
     * 获取当前时间 格式为HH:mm:ss
     * @return String
     */
    public static String getFormatTime() {
        return parseDateToStr(DateFormatConstant.FORMAT_TIME, getDateNow());
    }

    /**
     * 获取指定时间 格式为HH:mm:ss
     * @return String
     */
    public static String getFormatTime(Date date) {
        return parseDateToStr(DateFormatConstant.FORMAT_TIME, date);
    }

    /**
     * 获取当前时间 格式为yyyy-MM-dd HH:mm
     * @return String
     */
    public static String getFormatYearMonthDayHourMinute() {
        return parseDateToStr(DateFormatConstant.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE, getDateNow());
    }

    /**
     * 获取指定时间 格式为yyyy-MM-dd HH:mm
     * @return String
     */
    public static String getFormatYearMonthDayHourMinute(Date date) {
        return parseDateToStr(DateFormatConstant.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE, date);
    }

    /**
     * 获取当前时间 格式为yyyy-MM-dd HH:mm:ss
     * @return String
     */
    public static String getFormatYearMonthDayHourMinuteSecond() {
        return parseDateToStr(DateFormatConstant.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, getDateNow());
    }

    /**
     * 获取指定时间 格式为yyyy-MM-dd HH:mm:ss
     * @return String
     */
    public static String getFormatYearMonthDayHourMinuteSecond(Date date) {
        return parseDateToStr(DateFormatConstant.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, date);
    }

    // ================================ 字符串转换为日期 ================================

    public static Date parseStrToDate(final String format, final String dateStr) {
        try {
            return new SimpleDateFormat(format).parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取指定日期 格式为yyyy-MM-dd
     * @return Date
     */
    public static Date getFormatDate(final String dateStr) {
        return parseStrToDate(DateFormatConstant.FORMAT_DATE, dateStr);
    }

    /**
     * 获取指定时间 格式为HH:mm:ss
     * @return Date
     */
    public static Date getFormatTime(final String dateStr) {
        return parseStrToDate(DateFormatConstant.FORMAT_TIME, dateStr);
    }

    /**
     * 获取指定时间 格式为yyyy-MM-dd HH:mm
     * @return Date
     */
    public static Date getFormatYearMonthDayHourMinute(final String dateStr) {
        return parseStrToDate(DateFormatConstant.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE, dateStr);
    }

    /**
     * 获取指定时间 格式为yyyy-MM-dd HH:mm:ss
     * @return Date
     */
    public static Date getFormatYearMonthDayHourMinuteSecond(final String dateStr) {
        return parseStrToDate(DateFormatConstant.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, dateStr);
    }

    /**
     * 获取指定时间 格式为yyyy-MM-dd HH:mm:ss.SSS
     * @return Date
     */
    public static Date getFormatYearMonthDayHourMinuteSecondMillis(final String dateStr) {
        return parseStrToDate(DateFormatConstant.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND_MILLIS, dateStr);
    }

    // ================================ 日期转换为时间戳 ================================
    public static long dateToTimestamp(final Date date) {
        return date.getTime();
    }

    /**
     * 获取指定日期的时间戳 格式为yyyy-MM-dd
     * @return long
     */
    public static long dateToTimestamp(final String dateStr) {
        if (dateStr == null) {
            return 0L;
        }
        Date date = parseStrToDate(DateFormatConstant.FORMAT_DATE, dateStr);
        return date != null ? date.getTime() : 0L;
    }

    /**
     * 获取指定日期的时间戳
     * @return String
     */
    public static String dateToTimestampStr(final Date date) {
        return String.valueOf(date.getTime());
    }

    /**
     * 获取指定日期的时间戳
     * @return String
     */
    public static String dateToTimestampStr(final String dateStr) {
        if (dateStr == null) {
            return null;
        }
        Date date = parseStrToDate(DateFormatConstant.FORMAT_DATE, dateStr);
        return date != null ? String.valueOf(date.getTime()) : null;
    }


    // ================================ 时间戳转换为日期 ================================
    public static Date timestampToDate(final long timestamp) {
        return new Date(timestamp);
    }

    /**
     * 时间戳转换为日期
     * @param dateStr 时间戳
     * @return 日期
     */
    public static Date timestampToDate(final String dateStr) {
        if (dateStr == null) {
            return null;
        }
        return new Date(Long.parseLong(dateStr));
    }

    /**
     * 时间戳转换为日期
     * @param timestamp 时间戳
     * @param format 格式
     * @return 日期
     */
    public static String timestampToDateStr(final long timestamp, final String format) {
        return new SimpleDateFormat(format).format(new Date(timestamp));
    }

    /**
     * 字符串时间戳转换为日期
      * @param dateStr 字符串时间戳
     * @param format 格式
     * @return 日期
     */
    public static String timestampToDateStr(final String dateStr, final String format) {
        if (dateStr == null || format == null) {
            return null;
        }
        return new SimpleDateFormat(format).format(new Date(Long.parseLong(dateStr)));
    }

    /**
     * 计算两个日期之间的时间差
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 时间差字符串，格式为"X天X小时X分钟"
     */
    public static String getTimeDifference(Date startDate, Date endDate) {
        if (endDate == null || startDate == null) {
            return "0天0小时0分钟";
        }

        Duration duration = Duration.between(
                startDate.toInstant(),
                endDate.toInstant()
        ).abs();

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        return String.format("%d天%d小时%d分钟", days, hours, minutes);
    }

    /**
     * 根据日期获取mm-dd-ww-hh-MM格式的字符串
     * @param date 日期
     * @return mm-dd-ww-hh-MM格式的字符串
     */
    public static String getMmDdWwHhMmByDate(Date date) {
        if (date == null) {
            return "";
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        // 使用常量或枚举定义星期映射
        String[] weekDays = {"日", "一", "二", "三", "四", "五", "六"};

        return String.format("%d月%d日 周%s %02d:%02d",
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH),
                weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1],
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE)
        );
    }

    /**
     * 获取年月日 周几 时分
     * @param date 日期
     * @return 格式化字符串，如 "2026年6月30日 周二 14:30"
     */
    public static String getYMDWByDate(Date date) {
        if (date == null) {
            return "";
        }
        return parseDateToStr(DateFormatConstant.PATTERN_YMDW_HM, date);
    }

    /**
     * 获取月日 周几 时分
     * @param date 日期
     * @return 格式化字符串，如 "6月30日 周二 14:30"
     */
    public static String getMDWByDate(Date date) {
        if (date == null) {
            return "";
        }
        return parseDateToStr(DateFormatConstant.PATTERN_MDW_HM, date);
    }

    /**
     * 获取某月某日 某时某分 例如：02-01 12:19
     * @param date 日期
     * @return 例如：02-01 12:19
     */
    public static String getMdhmByDate(Date date) {
        if (date == null) {
            return "";
        }
        return parseDateToStr(DateFormatConstant.PATTERN_MD_HM, date);
    }

    public static String getMdhmByDate2(Date date) {
        if (date == null) {
            return "";
        }
        return parseDateToStr(DateFormatConstant.PATTERN_MD_HM2, date);
    }

    // ================================ 日期范围（开始/结束时间） ================================

    /**
     * 获取指定日期的开始时间（当天 00:00:00）
     * @param date 指定日期
     * @return 当天零点
     */
    public static Date getDayStartTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 获取指定日期的结束时间（当天 23:59:59）
     * @param date 指定日期
     * @return 当天最后一刻
     */
    public static Date getDayEndTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    /**
     * 获取当前年份的开始时间（1月1日 00:00:00）
     * @return 当年第一天零点
     */
    public static Date getYearStartTime() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        cal.clear();
        cal.set(Calendar.YEAR, year);
        return cal.getTime();
    }

    /**
     * 获取当前年份的结束时间（12月31日 23:59:59）
     * @return 当年最后一天最后一刻
     */
    public static Date getYearEndTime() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        cal.clear();
        cal.set(Calendar.YEAR, year);
        cal.roll(Calendar.DAY_OF_YEAR, -1);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    // ================================ 周范围 ================================

    /**
     * 获取上周的开始时间（上周一 00:00:00）
     * @param date 指定日期
     * @return 上周一零点
     */
    public static Date getLastWeekStartTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.add(Calendar.DATE, -7);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 获取上周的结束时间（上周日 23:59:59）
     * @param date 指定日期
     * @return 上周日最后一刻
     */
    public static Date getLastWeekEndTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.add(Calendar.DATE, -7);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    /**
     * 获取本周的开始时间（本周一 00:00:00）
     * @param date 指定日期
     * @return 本周一零点
     */
    public static Date getWeekStartTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 获取本周的结束时间（本周日 23:59:59）
     * @param date 指定日期
     * @return 本周日最后一刻
     */
    public static Date getWeekEndTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    // ================================ 月范围 ================================

    /**
     * 获取指定日期所在月份的第一天（00:00:00）
     * @param date 指定日期
     * @return 当月第一天零点
     */
    public static Date getMonthStartTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 获取指定日期所在月份的最后一天（23:59:59）
     * @param date 指定日期
     * @return 当月最后一天最后一刻
     */
    public static Date getMonthEndTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    /**
     * 获取指定年份某月的第一天（00:00:00）
     * @param date 指定日期（取年份）
     * @param month 月份（0-based，如 Calendar.JANUARY = 0）
     * @return 指定月份第一天零点
     */
    public static Date getMonthStartTime(Date date, int month) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        cal.clear();
        cal.set(year, month, 1);
        return cal.getTime();
    }

    /**
     * 获取指定年份某月的最后一天（23:59:59）
     * @param date 指定日期（取年份）
     * @param month 月份（0-based，如 Calendar.JANUARY = 0）
     * @return 指定月份最后一天最后一刻
     */
    public static Date getMonthEndTime(Date date, int month) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        cal.clear();
        cal.set(year, month, 1);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    // ================================ 日期偏移 ================================

    /**
     * 获取指定日期偏移N天后的开始时间（00:00:00）
     * @param date 指定日期
     * @param days 偏移天数（正数向后，负数向前）
     * @return 偏移后日期的零点
     */
    public static Date getDaysStartTime(Date date, int days) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 获取指定日期偏移N天后的结束时间（23:59:59）
     * @param date 指定日期
     * @param days 偏移天数（正数向后，负数向前）
     * @return 偏移后日期的最后一刻
     */
    public static Date getDaysEndTime(Date date, int days) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    /**
     * 获取指定日期偏移N天后的结束时间字符串（yyyy-MM-dd 23:59:59）
     * @param date 指定日期
     * @param days 偏移天数（正数向后，负数向前）
     * @return 格式化后的结束时间字符串
     */
    public static String getDaysEndTimeStr(Date date, int days) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return new SimpleDateFormat(DateFormatConstant.FORMAT_DATE).format(cal.getTime()) + " 23:59:59";
    }

    /**
     * 获取指定日期偏移N天后的日期
     * @param date 指定日期
     * @param days 偏移天数（正数向后，负数向前）
     * @return 偏移后的日期
     */
    public static Date getDaysAfterDate(Date date, int days) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    /**
     * 获取指定日期偏移N天前的日期
     * @param date 指定日期
     * @param days 偏移天数（正数向前）
     * @return 偏移后的日期
     */
    public static Date getDaysBeforeDate(Date date, int days) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, -days);
        return cal.getTime();
    }

    /**
     * 获取指定日期偏移N分钟前的时间
     * @param date 指定日期
     * @param minutes 偏移分钟数
     * @return 偏移后的时间
     */
    public static Date getMinutesBefore(Date date, int minutes) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MINUTE, -minutes);
        return cal.getTime();
    }

    /**
     * 获取指定日期偏移N小时前的时间
     * @param date 指定日期
     * @param hours 偏移小时数
     * @return 偏移后的时间
     */
    public static Date getHoursBefore(Date date, int hours) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.HOUR_OF_DAY, -hours);
        return cal.getTime();
    }

    /**
     * 获取指定日期偏移N小时后的时间
     * @param date 指定日期
     * @param hours 偏移小时数
     * @return 偏移后的时间
     */
    public static Date getHoursAfter(Date date, int hours) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.HOUR_OF_DAY, hours);
        return cal.getTime();
    }

    /**
     * 获取指定日期偏移N月后的时间
     * @param date 指定日期
     * @param months 偏移月数
     * @return 偏移后的时间
     */
    public static Date getMonthsAfter(Date date, int months) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, months);
        return cal.getTime();
    }

    // ================================ 日期判断与计算 ================================

    /**
     * 判断指定时间是否在开始时间和结束时间之间（包含边界）
     * @param currentDate 指定时间
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 是否在范围内
     */
    public static boolean isBetween(Date currentDate, Date startDate, Date endDate) {
        if (currentDate == null || startDate == null || endDate == null) {
            return false;
        }
        long time = currentDate.getTime();
        return time >= startDate.getTime() && time <= endDate.getTime();
    }

    /**
     * 获取两个日期之间的天数差
     * @param before 起始日期
     * @param after 结束日期
     * @return 天数差
     */
    public static long getDaysBetween(Date before, Date after) {
        if (before == null || after == null) {
            return 0;
        }
        LocalDate beforeDate = before.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate afterDate = after.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return ChronoUnit.DAYS.between(beforeDate, afterDate);
    }

    /**
     * 根据出生日期计算年龄
     * @param birthDay 出生日期
     * @return 年龄
     * @throws IllegalArgumentException 出生日期晚于当前时间
     */
    public static int getAge(Date birthDay) {
        if (birthDay == null) {
            throw new IllegalArgumentException("出生日期不能为空");
        }
        LocalDate birth = birthDay.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate now = LocalDate.now();
        if (now.isBefore(birth)) {
            throw new IllegalArgumentException("出生日期晚于当前时间");
        }
        return (int) ChronoUnit.YEARS.between(birth, now);
    }

    /**
     * 获取某年某月某日（中文格式）
     * @param date 日期
     * @return 如 "2026年6月30日"
     */
    public static String getYearMonthDay(Date date) {
        if (date == null) {
            return "";
        }
        return parseDateToStr("yyyy年M月d日", date);
    }

    /**
     * 将整型日期（如20260630）转换为 yyyy-MM-dd 格式字符串
     * @param date 整型日期
     * @return 格式化后的日期字符串
     */
    public static String getFormattedDate(Integer date) {
        if (date == null) {
            return null;
        }
        String str = date.toString();
        return str.substring(0, 4) + "-" + str.substring(4, 6) + "-" + str.substring(6, 8);
    }

}
