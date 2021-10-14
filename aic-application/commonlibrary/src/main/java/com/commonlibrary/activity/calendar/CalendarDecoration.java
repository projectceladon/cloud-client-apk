package com.commonlibrary.activity.calendar;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.commonlibrary.utils.DensityUtils;

/**
 * Created by wood on 2017/4/20.
 */

public class CalendarDecoration extends RecyclerView.ItemDecoration {

    private final int monthHeight;
    private final int weekHeight;
    private final int titleHeight;
    private final int monthPadding = 30;
    Paint mPaint, mPaintBg, mPaintWeek;

    public CalendarDecoration() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        Typeface font = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        mPaint.setTypeface(font);
        int mTitleSize = 22;
        mPaint.setTextSize(DensityUtils.sp2px(mTitleSize));
        int mColorTitle = 0xff344080;
        mPaint.setColor(mColorTitle);
        Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
        monthHeight = fontMetrics.bottom - fontMetrics.top + monthPadding * 2;

        mPaintWeek = new Paint();
        mPaintWeek.setAntiAlias(true);
        int mWeekSize = 14;
        mPaintWeek.setTextSize(DensityUtils.sp2px(mWeekSize));
        mPaintWeek.setTextAlign(Paint.Align.CENTER);
        mPaintWeek.setColor(mColorTitle);
        Paint.FontMetricsInt fontMetricsWeek = mPaint.getFontMetricsInt();
        int weekPadding = 10;
        weekHeight = fontMetricsWeek.bottom - fontMetricsWeek.top + weekPadding * 2;

        titleHeight = monthHeight + weekHeight;


        mPaintBg = new Paint();
        mPaintBg.setAntiAlias(true);
        mPaintBg.setColor(0xfff2f2f2);

    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        CalendarDate calendarDate = (CalendarDate) view.getTag();
        if (calendarDate.isMonthFirstRow()) {
            outRect.set(0, titleHeight, 0, 0);
        }
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);

        int childCount = parent.getChildCount();

        for (int i = 0; i < childCount; i++) {
            View view = parent.getChildAt(i);
            CalendarDate calendarDate = (CalendarDate) view.getTag();
            if (calendarDate.isMonthFirstItem()) {
                int left = view.getLeft() + monthPadding;
                String str = getYearMonth(calendarDate);
                Rect bounds = new Rect();
                mPaint.getTextBounds(str, 0, str.length(), bounds);
                Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
                int baseline = view.getTop() - weekHeight - monthHeight / 2 - fontMetrics.bottom / 2 - fontMetrics.top / 2;
                c.drawRect(new Rect(0, view.getTop() - titleHeight, parent.getWidth(), view.getTop() - weekHeight), mPaintBg);
                c.drawText(str, left, baseline, mPaint);
            }
        }
    }


    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDraw(c, parent, state);
        int childCount = parent.getChildCount();

        for (int i = 0; i < childCount; i++) {
            View view = parent.getChildAt(i);
            CalendarDate calendarDate = (CalendarDate) view.getTag();
            if (calendarDate.isMonthFirstRow()) {
                Log.d("onDrawOver", i + "");
                Paint.FontMetricsInt fontMetrics = mPaintWeek.getFontMetricsInt();
                int baseline = view.getTop() - weekHeight / 2 - fontMetrics.bottom / 2 - fontMetrics.top / 2;
                c.drawText(calendarDate.getWeek(), view.getLeft() + view.getWidth() / 2, baseline, mPaintWeek);
            }
        }
    }

    private String getYearMonth(CalendarDate calendarDate) {
        if (calendarDate.getMonth() + 1 < 10) {
            return calendarDate.getYear() + "/0" + (calendarDate.getMonth() + 1);
        }
        return calendarDate.getYear() + "/" + (calendarDate.getMonth() + 1);
    }


}
