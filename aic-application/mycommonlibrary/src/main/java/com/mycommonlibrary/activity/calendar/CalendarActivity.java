package com.mycommonlibrary.activity.calendar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.mycommonlibrary.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by wood on 2017/4/20.
 */

public class CalendarActivity extends AppCompatActivity implements CalendarAdapter.OnDateSelectListener {

    RecyclerView mCalendar;
    CalendarAdapter calendarAdapter;
    CalendarDecoration calendarDecoration;
    private Mode mode;
    public static final int REQUEST_RANGE = 0X210;
    public static final int REQUEST_SINGLE = 0x211;
    public static final int SINGLE_RESULT_CODE = 0X200;
    public static final int RANGE_RESULT_CODE = 0X201;
    public static final String RESULT_PARAMS_1 = "PARAMS1";
    public static final String RESULT_PARAMS_2 = "PARAMS2";

    public static void selectSingleDate(Activity act, String fromFlag, int limit, long defaultSelectTime) {
        Intent intent = new Intent();
        intent.setAction(act.getPackageName() + ".CalendarActivity");
        intent.putExtra("mode", Mode.SingleDate);
        intent.putExtra("defaultSingleTime", defaultSelectTime);
        intent.putExtra("limit", limit);
        intent.putExtra("fromFlag", fromFlag);
        act.startActivityForResult(intent, REQUEST_SINGLE);
    }

    public static void selectSingleDateFragment(Fragment frg, String fromFlag, int limit, long defaultSelectTime) {
        Intent intent = new Intent();
        intent.setAction(frg.getContext().getPackageName() + ".CalendarActivity");
        intent.putExtra("mode", Mode.SingleDate);
        intent.putExtra("defaultSingleTime", defaultSelectTime);
        intent.putExtra("limit", limit);
//        intent.putExtra("fromFlag", fromFlag);
        frg.startActivityForResult(intent, REQUEST_SINGLE);
    }

    public static void selectDateRange(Activity act, String fromFlag, long left, long right) {
        Intent intent = new Intent();
        intent.setAction(act.getPackageName() + ".CalendarActivity");
        intent.putExtra("mode", Mode.DateRange);
        intent.putExtra("defaultAreaTimeLeft", left);
        intent.putExtra("defaultAreaTimeRight", right);
//        intent.putExtra("fromFlag", fromFlag);
        act.startActivityForResult(intent, REQUEST_RANGE);
    }

    public static void selectDateRangeFragment(Fragment frg, String fromFlag, long left, long right) {
        Intent intent = new Intent();
        intent.setAction(frg.getContext().getPackageName() + ".CalendarActivity");
        intent.putExtra("mode", Mode.DateRange);
        intent.putExtra("defaultAreaTimeLeft", left);
        intent.putExtra("defaultAreaTimeRight", right);
        intent.putExtra("fromFlag", fromFlag);
        frg.startActivityForResult(intent, REQUEST_RANGE);
    }

    public static Intent getSingleIntent(Context context, int limit, long defaultSelectTime) {
        Intent intent = new Intent();
        intent.setAction(context.getPackageName() + ".CalendarActivity");
        intent.putExtra("mode", Mode.SingleDate);
        intent.putExtra("defaultSingleTime", defaultSelectTime);
        intent.putExtra("limit", limit);
        return intent;
    }

    public static Intent getAreaIntent(Context context, long left, long right) {
        Intent intent = new Intent();
        intent.setAction(context.getPackageName() + ".CalendarActivity");
        intent.putExtra("mode", Mode.DateRange);
        intent.putExtra("defaultAreaTimeLeft", left);
        intent.putExtra("defaultAreaTimeRight", right);
        return intent;
    }


    @Override
    public void onSingleDateSelect(CalendarDate calendarDate) {
        long ts = calendarDateToLong(calendarDate);
        if (ts != -1L) {
            Intent intent = new Intent();
            intent.putExtra(RESULT_PARAMS_1, ts);
            setResult(SINGLE_RESULT_CODE, intent);
        }
        finish();
    }

    @Override
    public void onAreaSelect(CalendarDate left, CalendarDate right) {
        long tsLeft = calendarDateToLong(left);
        long tsRight = calendarDateToLong(right);
        if (tsLeft != -1 && tsRight != -1) {
            Intent intent = new Intent();
            intent.putExtra(RESULT_PARAMS_1, tsLeft);
            intent.putExtra(RESULT_PARAMS_2, tsRight);
            setResult(RANGE_RESULT_CODE, intent);
        }
        finish();
    }


    private long calendarDateToLong(CalendarDate calendarDate) {
        long ts = -1;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = simpleDateFormat.parse(calendarDate.getYear() + "-" + (calendarDate.getMonth() + 1) + "-" + calendarDate.getDay());
            ts = date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return ts;
    }

    public static enum Mode {
        SingleDate,//日期单选
        DateRange//时间段选择
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        setWindowStatusBarColor(Color.WHITE);
        Toolbar toolbar = (Toolbar) findViewById(R.id.tl_calendar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mCalendar = (RecyclerView) findViewById(R.id.rv_calendar);
        ((SimpleItemAnimator) mCalendar.getItemAnimator()).setSupportsChangeAnimations(false);

        mode = (Mode) getIntent().getSerializableExtra("mode");
        if (mode == Mode.DateRange) {
            long left = getIntent().getLongExtra("defaultAreaTimeLeft", -1);
            long right = getIntent().getLongExtra("defaultAreaTimeRight", -1);
            CalendarDate calendarDateLeft = null, calendarDateRight = null;
            if (left != -1 && right != -1) {
                calendarDateLeft = new CalendarDate(left);
                calendarDateRight = new CalendarDate(right);
            }
            calendarAdapter = new CalendarAdapter(this, calendarDateLeft, calendarDateRight);
        } else {
            int limit = getIntent().getIntExtra("limit", -1);
            long defaultSelect = getIntent().getLongExtra("defaultSingleTime", -1);
            CalendarDate calendarDate = null;
            if (defaultSelect != -1) {
                calendarDate = new CalendarDate(defaultSelect);
            }
            calendarAdapter = new CalendarAdapter(this, calendarDate, limit);
        }
        mCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        calendarAdapter.setListener(this);
        mCalendar.setAdapter(calendarAdapter);
        calendarDecoration = new CalendarDecoration(this);
        mCalendar.addItemDecoration(calendarDecoration);
        mCalendar.scrollToPosition(calendarAdapter.getCurrentMonthPosition());
    }

    /**
     * 设置状态栏颜色  仅5.0以上
     *
     * @param color
     */
    public void setWindowStatusBarColor(int color) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                if (color == 0xffffffff) {
                    color = 0xffafb0b0;
                }
                window.setStatusBarColor(color);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
