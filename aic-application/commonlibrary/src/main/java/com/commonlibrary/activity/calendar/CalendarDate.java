package com.commonlibrary.activity.calendar;

import java.util.Calendar;
import java.util.Date;

/**
 * 日历的单天日期
 * Created by wood on 2017/4/20.
 */

public class CalendarDate {
    private int year;
    private int month;
    private int day;

    private String week;
    private boolean isDirty = false;
    private boolean isMonthFirstItem = false;
    private boolean isMonthFirstRow = false;
    private boolean enable = true;


    public CalendarDate(long time) {
        Date date = new Date(time);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        this.year = calendar.get(Calendar.YEAR);
        this.month = calendar.get(Calendar.MONTH);
        this.day = calendar.get(Calendar.DAY_OF_MONTH);
    }

    public CalendarDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }


    public CalendarDate(int year, int month, int day, boolean isMonthFirstItem, boolean isMonthFirstRow) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.isMonthFirstItem = isMonthFirstItem;
        this.isMonthFirstRow = isMonthFirstRow;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }


    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
        if (isDirty()) {
            setEnable(false);
        }
    }

    public boolean isMonthFirstItem() {
        return isMonthFirstItem;
    }

    public void setMonthFirstItem(boolean monthFirstItem) {
        isMonthFirstItem = monthFirstItem;
    }

    public boolean isMonthFirstRow() {
        return isMonthFirstRow;
    }

    public void setMonthFirstRow(boolean monthFirstRow) {
        isMonthFirstRow = monthFirstRow;
    }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    /**
     * 获取时间字符串
     *
     * @return data string
     */
    public String getTimeString() {
        if (year == -1) {
            return "";
        } else {
            return year + "/" + (month + 1) + "/" + day;
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof CalendarDate) {
            CalendarDate objData = (CalendarDate) obj;
            return this.getYear() == objData.getYear() && this.getMonth() == objData.getMonth() && this.getDay() == objData.getDay();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = result * 31 + getYear();
        result = result * 31 + getMonth();
        result = result * 31 + getDay();
        return result;
    }
}
