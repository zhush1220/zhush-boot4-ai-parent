package com.zhush.common.constant;

import java.time.ZoneId;

/**
 *
 * @ClassName DateFormatConstant
 * @Description 日期格式常量
 * @Author zhush
 * @Date 2026/6/29 星期一 22:15
 *
 **/
public class DateFormatConstant {

    public static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    public static final String TIME_ZONE_DEFAULT = "GMT+8";

    public static final long SECOND_MILLIS = 1000;

    public static final String FORMAT_YEAR = "yyyy";

    public static final String FORMAT_YEAR_MONTH = "yyyy-MM";

    public static final String FORMAT_DATE = "yyyy-MM-dd";

    public static final String FORMAT_TIME = "HH:mm:ss";

    public static final String FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE = "yyyy-MM-dd HH:mm";

    public static final String FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND = "yyyy-MM-dd HH:mm:ss";

    public static final String FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND_MILLIS = "yyyy-MM-dd HH:mm:ss.S";

    public static final String PATTERN_YMDW_HM = "yyyy年MM月dd日 E HH:mm";

    public static final String PATTERN_MDW_HM = "MM月dd日 E HH:mm";

    public static final String PATTERN_MD_HM = "MM-dd HH:mm";

    public static final String PATTERN_MD_HM2 = "MM月dd日 HH:mm";
}
