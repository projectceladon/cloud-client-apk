package com.mycommonlibrary.activity.calendar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.mycommonlibrary.R;
import com.mycommonlibrary.utils.DateTimeUtils;
import com.mycommonlibrary.utils.DensityUtils;
import com.mycommonlibrary.utils.ToastUtils;

import java.util.ArrayList;

/**
 * Created by wood on 2017/4/20.
 */

public class CalendarAdapter extends RecyclerView.Adapter {
    private Context context;
    private int currentDayPosition = -1;
    CalendarActivity.Mode mode;
    private int singleSelectedPosition = -1;
    private int singleLimit = -1;
    private int areaPositionLeft = -1, areaPositionRight = -1;

    SingleSelectColor singleColor;

    AreaSelectColor areaSelectColor;

    OnDateSelectListener listener;

    private ArrayList<CalendarDate> list;

    /**
     * 单选模式构造方法
     *
     * @param singleSelected 默认选中的日期
     * @param limit          今天起到limit天后可选择 其他不可选择
     */
    public CalendarAdapter(Context context, CalendarDate singleSelected, int limit) {

        this.context = context;
        this.mode = CalendarActivity.Mode.SingleDate;
        this.singleLimit = limit;
        singleColor = new SingleSelectColor(context);
        initSingleMode();
        if (singleSelected != null) {
            singleSelectedPosition = list.indexOf(singleSelected);
        }
    }

    /**
     * 选择时间范围构造方法
     */
    public CalendarAdapter(Context context, CalendarDate left, CalendarDate right) {
        this.context = context;
        this.mode = CalendarActivity.Mode.DateRange;
        areaSelectColor = new AreaSelectColor(context);
        initAreaMode();

        if (left != null && right != null) {
            int leftPosition = areaPositionLeft = list.indexOf(left);
            int rightPosition = areaPositionLeft = list.indexOf(right);

            if (leftPosition != -1 && rightPosition != -1 && leftPosition < rightPosition) {
                areaPositionLeft = leftPosition;
                areaPositionRight = rightPosition;
            }

        }
    }

    public void setListener(OnDateSelectListener listener) {
        this.listener = listener;
    }

    private void initAreaMode() {
        list = initDates(true);
    }

    private void initSingleMode() {
        int currentYear = DateTimeUtils.getYear();
        int currentMonth = DateTimeUtils.getMonth();
        int currentDay = DateTimeUtils.getCurrentMonthDay();
        CalendarDate today = new CalendarDate(currentYear, currentMonth, currentDay);
        if (singleLimit >= 0) {
            list = initDates(false);
            int todayPosition = list.indexOf(today);
            int temp = 1;//从开始因为当天时间默认可用
            for (int j = todayPosition; j < list.size(); j++) {
                if (temp > singleLimit + 1) {
                    break;
                }
                CalendarDate date = list.get(j);
                if (date.isDirty()) {
                    continue;
                } else {
                    date.setEnable(true);
                    temp++;
                }
            }
        } else {
            list = initDates(false);
            int todayPosition = list.indexOf(today);
            for (int i = todayPosition; i < list.size(); i++) {
                CalendarDate date = list.get(i);
                if (!date.isDirty()) {
                    date.setEnable(true);
                }
            }
        }

    }


    private ArrayList<CalendarDate> initDates(boolean defaultEnable) {
        int currentYear = DateTimeUtils.getYear();
        int currentMonth = DateTimeUtils.getMonth();
        int currentDay = DateTimeUtils.getCurrentMonthDay();
        int startYear = currentYear - 1;
        int endYear = currentYear + 1;

        ArrayList<CalendarDate> calendarDates = new ArrayList<>();

        for (int tempYear = startYear; tempYear <= endYear; tempYear++) {

            for (int month = 0; month < 12; month++) {
                //获取该月份第一天是周几  在月首行补空格 周日 返回1 周一返回2  再减去1 即为当月第一天在第几列
                int tempWeek = DateTimeUtils.getMonthFirstDatWeek(tempYear, month) - 1;
                for (int i = 0; i < tempWeek; i++) {
                    CalendarDate calendarDate = new CalendarDate(tempYear, month, -1);
                    calendarDate.setDirty(true);
                    calendarDate.setMonthFirstItem(i == 0);
                    calendarDate.setMonthFirstRow(true);
                    calendarDate.setWeek(getWeekByRow(i));
                    calendarDates.add(calendarDate);
                }

                int monthFirstWeekCleanCount = 7 - tempWeek; //记录当月第一行有效日期的个数

                int monthDays = DateTimeUtils.getMonthDays(tempYear, month);
                for (int tempDay = 1; tempDay <= monthDays; tempDay++) {
                    CalendarDate calendarDate = new CalendarDate(tempYear, month, tempDay);
                    calendarDate.setDirty(false);
                    calendarDate.setMonthFirstItem((monthFirstWeekCleanCount == 7 && tempDay == 1));
                    calendarDate.setMonthFirstRow((tempDay - 1 < monthFirstWeekCleanCount));
                    calendarDate.setWeek(DateTimeUtils.getDayOfMonthWeek(tempYear, month, tempDay));
                    calendarDate.setEnable(defaultEnable);
                    calendarDates.add(calendarDate);
                    if (tempYear == currentYear && month == currentMonth && tempDay == currentDay) {
                        currentDayPosition = calendarDates.size() - 1;
                    }
                }
                //月尾补空格
                int temp = calendarDates.size() % 7;
                if (temp != 0) {
                    for (int i = 1; i <= 7 - temp; i++) {
                        CalendarDate calendarDate = new CalendarDate(tempYear, month, -1);
                        calendarDate.setDirty(true);
                        calendarDate.setMonthFirstItem(false);
                        calendarDate.setMonthFirstRow(false);
                        calendarDate.setWeek(getWeekByRow(i));
                        calendarDates.add(calendarDate);
                    }
                }
            }
        }
        return calendarDates;
    }


    private String getWeekByRow(int row) {
        switch (row) {
            case 0:
                return "周日";
            case 1:
                return "周一";
            case 2:
                return "周二";
            case 3:
                return "周三";
            case 4:
                return "周四";
            case 5:
                return "周五";
            case 6:
                return "周六";
            default:
                return "";
        }
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView textView = new TextView(context);
        int margin = 10;
        int height = DensityUtils.getmScreenWidth() < DensityUtils.getmScreenWidth() ? (DensityUtils.getmScreenWidth() / 7 - (margin * 2)) : (DensityUtils.getmScreenWidth() / 7 - (margin * 2));
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        layoutParams.setMargins(margin, margin, margin, margin);
        textView.setLayoutParams(layoutParams);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(14);
        return new CalendarHolder(textView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        CalendarHolder calendarHolder = (CalendarHolder) holder;
        CalendarDate calendarDate = list.get(position);
        holder.itemView.setTag(calendarDate);
        if (calendarDate.isDirty()) {
            calendarHolder.itemView.setOnClickListener(null);
            calendarHolder.textView.setBackgroundColor(bgColorNormal);
            calendarHolder.textView.setTextColor(textColorNormal);
            calendarHolder.textView.setTextSize(16);
            calendarHolder.textView.setText(calendarDate.isDirty() ? "" : (calendarDate.getDay() + ""));
            return;
        }
        if (position == currentDayPosition) {
            calendarHolder.textView.setText("今天");
        } else {
            calendarHolder.textView.setText(calendarDate.getDay() + "");
        }
        switch (mode) {
            case SingleDate:
                singleSelectModeBind(calendarHolder, calendarDate, position);
                break;
            case DateRange:
                areaSelectModeBind(calendarHolder, calendarDate, position);
                break;
        }
    }


    private void singleSelectModeBind(CalendarHolder calendarHolder, CalendarDate calendarDate, final int position) {
        if (singleSelectedPosition == position) {
            calendarHolder.textView.setTextSize(18);
            calendarHolder.textView.setBackground(singleColor.bgColorSelected);
            calendarHolder.textView.setTextColor(singleColor.textColorSelected);
        } else {
            calendarHolder.textView.setTextSize(16);
            if (position == currentDayPosition) {
                calendarHolder.textView.setBackground(singleColor.bgColorToday);
            } else if (calendarDate.isEnable()) {
                calendarHolder.textView.setBackground(singleColor.bgColorEnable);
                calendarHolder.textView.setTextColor(textColorNormal);
            } else {
                calendarHolder.textView.setBackgroundColor(bgColorNormal);
                calendarHolder.textView.setTextColor(singleColor.textColorEnable);
            }

        }
        if (calendarDate.isEnable()) {
            calendarHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int temp = singleSelectedPosition;
                    singleSelectedPosition = position;
                    if (temp != -1) {
                        notifyItemChanged(temp);
                    }
                    notifyItemChanged(singleSelectedPosition);
                    if (listener != null) {
                        listener.onSingleDateSelect(list.get(singleSelectedPosition));
                    }
                }
            });
        } else {
            calendarHolder.itemView.setOnClickListener(null);
        }


    }


    private void areaSelectModeBind(CalendarHolder calendarHolder, CalendarDate calendarDate, final int position) {

        if (areaPositionLeft == -1) {
            //没有任何选择
            calendarHolder.textView.setBackgroundColor(bgColorNormal);
            calendarHolder.textView.setTextColor(textColorNormal);
        } else {
            if (position == areaPositionLeft) {
                calendarHolder.textView.setBackground(areaSelectColor.bgColorBoundary);
                calendarHolder.textView.setTextColor(areaSelectColor.textColorBoundary);
            } else {
                if (areaPositionRight == -1) {
                    calendarHolder.textView.setBackgroundColor(bgColorNormal);
                    calendarHolder.textView.setTextColor(textColorNormal);
                } else {
                    if (position == areaPositionRight) {
                        calendarHolder.textView.setBackground(areaSelectColor.bgColorBoundary);
                        calendarHolder.textView.setTextColor(areaSelectColor.textColorBoundary);
                    } else if (position > areaPositionLeft && position < areaPositionRight) {
                        calendarHolder.textView.setBackground(areaSelectColor.bgColorInner);
                        calendarHolder.textView.setTextColor(areaSelectColor.textColorInner);
                    } else {
                        calendarHolder.textView.setBackgroundColor(bgColorNormal);
                        calendarHolder.textView.setTextColor(textColorNormal);
                    }

                }
            }
        }
        if (calendarDate.isEnable()) {
            calendarHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (areaPositionLeft == -1) {
                        areaPositionRight = -1;
                        areaPositionLeft = position;
                        notifyItemChanged(areaPositionLeft);
                        ToastUtils.show( R.string.select_end_date);
                    } else {
                        if (areaPositionRight == -1) {
                            if (position > areaPositionLeft) {
                                areaPositionRight = position;
                                notifyItemRangeChanged(areaPositionLeft, areaPositionRight - areaPositionLeft + 1);
                                if (listener != null) {
                                    listener.onAreaSelect(list.get(areaPositionLeft), list.get(areaPositionRight));
                                }
                            } else {
                                int temp = areaPositionLeft;
                                areaPositionLeft = position;
                                notifyItemChanged(temp);
                                notifyItemChanged(areaPositionLeft);
                                ToastUtils.show( R.string.select_end_date);
                            }
                        } else {
                            int tempL = areaPositionLeft;
                            int tempR = areaPositionRight;
                            areaPositionLeft = position;
                            areaPositionRight = -1;
                            notifyItemRangeChanged(tempL, tempR - tempL + 1);
                            notifyItemChanged(areaPositionLeft);
                            ToastUtils.show( R.string.select_end_date);
                        }
                    }
                }
            });
        } else {
            calendarHolder.itemView.setOnClickListener(null);
        }

    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    private class CalendarHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public CalendarHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }


    public int getCurrentMonthPosition() {
        int currentYear = DateTimeUtils.getYear();
        int currentMonth = DateTimeUtils.getMonth();
        int position = list.indexOf(new CalendarDate(currentYear, currentMonth, 1));
        return position;
    }


    public class SingleSelectColor {
        public SingleSelectColor(Context context) {
            textColorSelected = Color.WHITE;
            textColorEnable = Color.GRAY;
            bgColorSelected = ContextCompat.getDrawable(context, R.drawable.calendar_bg_select);
            bgColorEnable = ContextCompat.getDrawable(context, R.drawable.calendar_bg_enable);
            bgColorToday = ContextCompat.getDrawable(context, R.drawable.calendar_bg_today);
        }

        int textColorSelected;
        int textColorEnable;
        Drawable bgColorSelected;
        Drawable bgColorEnable;
        Drawable bgColorToday;
    }


    public class AreaSelectColor {

        public AreaSelectColor(Context context) {
            textColorBoundary = Color.WHITE;
            bgColorBoundary = ContextCompat.getDrawable(context, R.drawable.calendar_bg_select);
            textColorInner = Color.WHITE;
            bgColorInner = ContextCompat.getDrawable(context, R.drawable.calendar_bg_today);
        }

        int textColorBoundary;
        Drawable bgColorBoundary;

        int textColorInner;
        Drawable bgColorInner;
    }

    private int bgColorNormal = Color.WHITE;

    private int textColorNormal = 0xff7987c8;


    public interface OnDateSelectListener {
        void onSingleDateSelect(CalendarDate calendarDate);

        void onAreaSelect(CalendarDate left, CalendarDate right);
    }

}
