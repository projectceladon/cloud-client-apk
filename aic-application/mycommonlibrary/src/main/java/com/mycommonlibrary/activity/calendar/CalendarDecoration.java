package com.mycommonlibrary.activity.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import com.mycommonlibrary.utils.DensityUtils;

/**
 * Created by wood on 2017/4/20.
 */

public class CalendarDecoration extends RecyclerView.ItemDecoration {
    private Context context;
    private int mColorTitle = 0xff344080;

    private int mTitleSize = 22;
    private int mWeekSize = 14;

    Paint mPaint, mPaintBg, mPaintWeek;

    private int monthHeight;
    private int weekHeight;
    private int titleHeight;
    private int monthPadding = 30;
    private int weekPadding = 10;

    public CalendarDecoration(Context context) {
        this.context = context;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        Typeface font = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        mPaint.setTypeface(font);
        mPaint.setTextSize(DensityUtils.sp2px(mTitleSize));
        mPaint.setColor(mColorTitle);
        Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
        monthHeight = fontMetrics.bottom - fontMetrics.top + monthPadding * 2;

        mPaintWeek = new Paint();
        mPaintWeek.setAntiAlias(true);
        mPaintWeek.setTextSize(DensityUtils.sp2px(mWeekSize));
        mPaintWeek.setTextAlign(Paint.Align.CENTER);
        mPaintWeek.setColor(mColorTitle);
        Paint.FontMetricsInt fontMetricsWeek = mPaint.getFontMetricsInt();
        weekHeight = fontMetricsWeek.bottom - fontMetricsWeek.top + weekPadding * 2;

        titleHeight = monthHeight + weekHeight;


        mPaintBg = new Paint();
        mPaintBg.setAntiAlias(true);
        mPaintBg.setColor(0xfff2f2f2);

    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        CalendarDate calendarDate = (CalendarDate) view.getTag();
        if (calendarDate.isMonthFirstRow()) {
            outRect.set(0, titleHeight, 0, 0);
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
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
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
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
