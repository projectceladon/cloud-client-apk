package com.mycommonlibrary.utils;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 修改日期：2019/6/4.
 */

public class DateTimeUtils {
    private static final int HOURS_OF_DAY = 24;
    private static final int MINUTES_OF_HOUR = 60;
    private static final int SECONDS_OF_MINUTE = 60;
    private static final int MILLS_OF_SECOND = 1000;
    public static final long MILLS_OF_DAY = HOURS_OF_DAY * MINUTES_OF_HOUR * SECONDS_OF_MINUTE * MILLS_OF_SECOND;

    private DateTimeUtils() {
        /* cannot be instantiated */
        throw new UnsupportedOperationException("cannot be instantiated");
    }
    public static int getMonthDays(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        return cal.getActualMaximum(Calendar.DATE);
    }

    public static int getYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    public static int getMonth() {
        return Calendar.getInstance().get(Calendar.MONTH);
    }

    public static int getCurrentMonthDay() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    /**
     * @param year
     * @param month
     * @return
     */
    public static int getMonthFirstDatWeek(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.YEAR, year);
        return calendar.get(Calendar.DAY_OF_WEEK);
    }
    /**
     * @param year
     * @param month
     * @return
     */
    public static String getDayOfMonthWeek(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.YEAR, year);
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case 1:
                return "周日";
            case 2:
                return "周一";
            case 3:
                return "周二";
            case 4:
                return "周三";
            case 5:
                return "周四";
            case 6:
                return "周五";
            case 7:
                return "周六";
        }
        return "";
    }
    /**
     * 格式化日期，月日用两位数保存，不足的前面加0
     */
    public static String formatDateString(String strDate) {
        if (TextUtils.isEmpty(strDate)) return null;
        String[] arrDate = strDate.split("-");
        if (arrDate == null || arrDate.length < 3) return strDate;
        String year = arrDate[0];
        int month = Integer.parseInt(arrDate[1]);
        int day = Integer.parseInt(arrDate[2]);
        return String.format("%s-%02d-%02d", year, month, day);
    }

    /**
     * 格式化时间字符串，时间单位小于10的自动补0
     */
    public static String formatTimeString(String strTime) {
        if (TextUtils.isEmpty(strTime)) return null;
        String[] arrTime = strTime.split(":");
        if (arrTime == null || arrTime.length < 2) return strTime;
        int hour = Integer.parseInt(arrTime[0]);
        int second = Integer.parseInt(arrTime[1]);
        return String.format("%02d:%02d", hour, second);
    }

    /**
     * 获取当前日期
     */
    public static String currentDate() {
        return long2strDate("yyyy-MM-dd", System.currentTimeMillis());
    }

    /**
     * 获取当前时间
     */
    public static String currentTime() {
        return long2strDate("HH:mm:ss", System.currentTimeMillis());
    }

    /**
     * 获取当前的年份
     */
    public static int currentYear() {
        return Integer.parseInt(long2strDate("yyyy", System.currentTimeMillis()));
    }

    /**
     * 获取当前的月份
     */
    public static int currentMonth() {
        return Integer.parseInt(long2strDate("MM", System.currentTimeMillis()));
    }

    /**
     * 获取当前的日期数
     */
    public static int currentDateNum() {
        return Integer.parseInt(long2strDate("dd", System.currentTimeMillis()));
    }

    public static String getWeekStr(Date date) {
        String[] weeks = {"日", "一", "二", "三", "四", "五", "六"};
        return weeks[getWeekInt(date)];
    }

    public static int getWeekInt(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int week_index = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (week_index < 0) {
            week_index = 0;
        }
        return week_index;
    }

    /**
     * 获取指定月份的总天数
     */
    public static int getTotalDaysByMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(strDate2Long("yyyyMM", year + "" + month)));
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取当前的日期时间
     */
    public static String currentDateTime() {
        return long2strDate("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis());
    }

    /**
     * long类型转换成日期格式的字符串
     *
     * @param formatDate ("yyyy-MM-dd HH:mm:ss")
     * @param date       时间毫秒数
     * @return
     */
    public static String long2strDate(String formatDate, long date) {
        SimpleDateFormat sdf = new SimpleDateFormat(formatDate);
        return sdf.format(new Date(date));
    }

    /**
     * String日期转换为Long
     *
     * @param formatDate ("yyyy-MM-dd HH:mm:ss")
     * @param date       ("2013-12-31 21:08:00")
     * @return
     */
    public static long strDate2Long(String formatDate, String date) {
        SimpleDateFormat sdf = new SimpleDateFormat(formatDate);
        try {
            Date dt = sdf.parse(date);
            return dt.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 比较起始日期和结束日期
     *
     * @return 如果两个日期相同也返回true
     */
    public static boolean compareStartEnd(String start, String end) {
        long ls = strDate2Long("yyyy-MM-dd", start);
        long le = strDate2Long("yyyy-MM-dd", end);
        return le - ls >= 0;
    }

    /**
     * 计算两个日期之间的天数
     */
    public static int twoDateBound(String start, String end) {
        long ls = strDate2Long("yyyy-MM-dd", start);
        long le = strDate2Long("yyyy-MM-dd", end);
        return (int) ((le - ls) / (long) (24 * 60 * 60 * 1000));
    }

    /**
     * 返回指定月份的最后一天的日期号
     */
    public static String getDayOfMonth(int month) {
        Calendar cale = Calendar.getInstance();
        cale.add(Calendar.MONTH, month);
        cale.set(Calendar.DAY_OF_MONTH, 0);
        return new SimpleDateFormat("dd").format(cale.getTime());
    }

    /**
     * 毫秒时间戳(13位)转日期字符串 带时分秒
     * <p>
     * 时间戳为0 返回空字符串
     *
     * @param timeStamp
     * @return
     */
    public static final String timeStampToString(long timeStamp) {
        return timeStampToString(timeStamp, '/');
    }


    /**
     * 毫秒时间戳(13位)转日期字符串 不带时分秒
     * <p>
     * 时间戳为0 返回空字符串
     *
     * @param timeStamp
     * @return
     */
    public static final String timeStampToStringWithOutHMS(long timeStamp) {
        return timeStampToString(timeStamp, "yyyy/MM/dd");
    }


    /**
     * 毫秒时间戳(13位)转日期字符串 仅时分
     * <p>
     * 时间戳为0 返回空字符串
     *
     * @param timeStamp
     * @return
     */
    public static final String timeStampToHMSString(long timeStamp) {
        return timeStampToString(timeStamp, "HH:mm");
    }

    /**
     * 毫秒时间戳(13位)转字符串
     *
     * @param timeStamp   13位毫秒时间戳
     * @param dividerChar 年月日分隔符
     * @return
     */
    public static final String timeStampToString(long timeStamp, char dividerChar) {
        String format = "yyyy" + dividerChar + "MM" + dividerChar + "dd HH:mm:ss";
        return timeStampToString(timeStamp, format);
    }


    /**
     * 毫秒时间戳(13位)转字符串
     *
     * @param timeStamp 13位毫秒时间戳
     * @param format    日期格式
     * @return
     */
    public static final String timeStampToString(long timeStamp, String format) {
        if (timeStamp == -1) {
            return "";
        }
        if (format == null || format.trim().equals("")) {
            format = "yyyy/MM/dd HH:mm:ss";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.format(new Date(timeStamp));
        } catch (Exception e) {
        }

        return "";
    }

    /**
     * 將时分秒去掉只保留年月日
     *
     * @param time
     * @return
     */
    public static String getYMDTime(String time) {
        try {
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy/MM/dd");
            Date date = new Date(time);
            return simpleDate.format(date);
        } catch (Exception e) {
        }
        return "";
    }

    @SuppressLint("SimpleDateFormat")
    /**
     * 获取当前时间字符串  yyyy/MM/dd HH:mm:ss
     * @return：当前时间
     */
    public static String getCurrentTimeStr() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat(
                "yyyy/MM/dd HH:mm:ss");
        return sDateFormat.format(new Date());
    }


    /**
     * 获取当前时间字符串  yyyy/MM/dd
     *
     * @return：当前时间
     */
    public static String getCurrentYMDTime() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        return sDateFormat.format(new Date());
    }


    /**
     * 获取当前时间字符串  yyyy/MM
     *
     * @return：当前时间
     */
    public static String getCurrentYM() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy/MM");
        return sDateFormat.format(new Date());
    }

    /**
     * 获取当天整点毫秒数
     *
     * @return：当前时间
     */
    public static long getCurrentDayZeroTimeStamp() {
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }


    /**
     * 获取距当天的第N天整点毫秒数
     *
     * @return：当前时间
     */
    public static long getNDaysTimeStamp(int N) {
        return getCurrentDayZeroTimeStamp() + N * MILLS_OF_DAY;
    }


    /**
     * 获取本月1号的整点毫秒数
     *
     * @return
     */
    public static long getCurrentMonthTimeStamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 1);//自然月内第一天
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static int getWeeks(int year) {
        int week = 0;
        int days = 365;
        int day = 0;
        if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) {//判别是不是闰年，闰年366天
            days = 366;
        }
        //得到一年全部天数后来除以7
        day = days % 7;//得到余下几天
        week = days / 7;//得到多少周
        return week;
    }

    //获取每月天数
    public static int getDaysByYearAndMonth(int year, int month) {
        int result;
        if (2 == month) {
            if (year % 4 == 0 && year % 100 != 0 || year % 400 == 0) {
                result = 29;
            } else {
                result = 28;
            }
        } else if (month == 4 || month == 6 || month == 9 || month == 11) {
            result = 30;
        } else {
            result = 31;
        }
        return result;
    }
    /**
     * 掉此方法输入所要转换的时间输入例如（"2014年06月14日16时09分00秒"）返回时间戳
     *
     * @param time
     * @return
     */
    /**
     * 时间字符串转换成毫秒数  支持 1970/01/01 或者 1970/01/01 09:23:00
     *
     * @param time
     * @return
     */
    public static String getMillisByStr(String time) {
        if (time.contains("-")) {
            time = time.replaceAll("-", "/");
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        if (!time.contains(":")) {
            df = new SimpleDateFormat("yyyy/MM/dd");
        }
        try {
            Date date = df.parse(time);
            return date.getTime() + "";
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    //判断当月属于哪一个季度
    public static int GetQuarter(int month) {
        int Quarter = 0;
        if (month >= 1 && month <= 3) {
            Quarter = 1;
        } else if (month >= 4 && month <= 6) {
            Quarter = 2;
        } else if (month >= 7 && month <= 9) {
            Quarter = 3;
        } else if (month >= 10 && month <= 12) {
            Quarter = 4;
        }
        return Quarter;
    }

    /**
     * 获取当前为本年的第几周
     */
    public static int getWeeksOfYear() {
        Calendar cal = Calendar.getInstance();//这一句必须要设置，否则美国认为第一天是周日，而我国认为是周一，对计算当期日期是第几周会有错误
        cal.setFirstDayOfWeek(Calendar.MONDAY); // 设置每周的第一天为星期一
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);// 每周从周一开始
        cal.setMinimalDaysInFirstWeek(7); // 设置每周最少为7天
        cal.setTime(new Date(System.currentTimeMillis()));
        int weeks = cal.get(Calendar.WEEK_OF_YEAR) + 1;
        return weeks;
    }

    /*
     * 毫秒转化时分秒毫秒
     */
    public static String millis2DayHourMinuteSec(Long ms, boolean isShowMillis) {
        Integer ss = 1000;
        Integer mi = ss * 60;
        Integer hh = mi * 60;
        Integer dd = hh * 24;

        Long day = ms / dd;
        Long hour = (ms - day * dd) / hh;
        Long minute = (ms - day * dd - hour * hh) / mi;
        Long second = (ms - day * dd - hour * hh - minute * mi) / ss;
        Long milliSecond = ms - day * dd - hour * hh - minute * mi - second * ss;

        StringBuffer sb = new StringBuffer();
        if(day > 0) {
            sb.append(day+"天");
        }
        if(hour > 0) {
            sb.append(hour+"小时");
        }
        if(minute > 0) {
            sb.append(minute+"分");
        }
        if(second > 0) {
            sb.append(second+"秒");
        }
        if(milliSecond > 0 && isShowMillis) {
            sb.append(milliSecond+"毫秒");
        }
        return sb.toString();
    }
}
