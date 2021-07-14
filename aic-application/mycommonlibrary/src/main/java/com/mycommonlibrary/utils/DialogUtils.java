package com.mycommonlibrary.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.mycommonlibrary.R;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Calendar;

/**
 * 对话框工具类
 * 最后修改：2019-02-19
 */
public class DialogUtils {
    public static final int RESULT_OK = -1;
    public static final int RESULT_CANCELED = 0;
    public static final int RESULT_APPLY = 1;
    // Android 2.x的主题
    // private static int theme = android.R.style.Theme_Light_NoTitleBar;
    // Android 4.x的主题
    // private static int theme = android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth;
    // 本机安卓版本的样式
    private static int theme = android.R.style.Theme_DeviceDefault_Light_Dialog;

    /**
     * 设置对话框的主题
     *
     * @param theme 值小于0时使用本机当前版本的主题
     */
    public static void setTheme(int theme) {
        DialogUtils.theme = theme > 0 ? theme : android.R.style.Theme_DeviceDefault_Light_Dialog;
    }

    public static int getTheme() {
        return DialogUtils.theme;
    }

    /**
     * 显示一个带环形进度条的对话框
     *
     * @param context       必须是包含Activity的上下文，不然会报错
     * @param message       进度框显示的文字
     * @param outSideCancel 是否可以点击对话框以外的区域取消对话框
     * @param listener      当对话框被取消时响应此对象
     * @param isNoActivity  如果是无界面模式下或某些内部类中调用的话需要设为true并加权限
     *                      <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
     */
    public static ProgressDialog showProgressDialog(Context context, String message,
                                                    boolean outSideCancel,
                                                    DialogInterface.OnCancelListener listener,
                                                    boolean isNoActivity) {
        if (TextUtils.isEmpty(message))
            message = "正在载入中...";
        ProgressDialog dialog = new ProgressDialog(new ContextThemeWrapper(context, theme));
        // 在无界面模式下或内部类中调用的Context很可能是无法启动对话框的
        // 所以必须使用如下的方式启动
        if (isNoActivity)
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        // 对话框居中(有些情况下会不居中)
        WindowManager.LayoutParams attr = dialog.getWindow().getAttributes();
        if (attr != null) {
            attr.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            attr.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            attr.gravity = Gravity.CENTER;//设置dialog 在布局中的位置
        }
        dialog.setMessage(message);
        dialog.setCanceledOnTouchOutside(outSideCancel);
        dialog.setOnCancelListener(listener);
        dialog.show();
        return dialog;
    }

    public static ProgressDialog showProgressDialog(Context context, String message) {
        return showProgressDialog(context, message, false, null, false);
    }

    public static ProgressDialog showProgressDialog(Context context) {
        return showProgressDialog(context, null, false, null, false);
    }

    public static ProgressDialog showProgressDialog(Context context, boolean isNoActivity) {
        return showProgressDialog(context, null, false, null, isNoActivity);
    }

    /**
     * 返回一个带环形滚动控件和文本控件的对话框
     *
     * @param context       必须是包含Activity的上下文，不然会报错
     * @param message       进度框显示的文字
     * @param outSideCancel 是否可能点击对话框以外的区域关闭
     * @return
     */
    public static AlertDialog showLoadingDialog(Context context, String message, boolean outSideCancel) {
        LinearLayout layout = new LinearLayout(context, null);
        layout.setBackgroundColor(Color.BLACK);
        layout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10, 10, 10, 10);
        layout.setGravity(Gravity.CENTER);

        ProgressBar progress = new ProgressBar(context);
        layout.addView(progress);

        if (message != null) {
            TextView tvText = new TextView(context);
            tvText.setText(message);
            tvText.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            tvText.setTextColor(Color.WHITE);
            layout.addView(tvText);
        }
        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(context, theme))
                .setView(layout).show();
        // 在无界面模式下或内部类中调用的Context很可能是无法启动对话框的
        // 所以必须使用如下的方式启动
        dialog.setCanceledOnTouchOutside(outSideCancel);
        return dialog;
    }

    public static AlertDialog showLoadingDialog(Context context, String message) {
        return showLoadingDialog(context, message, false);
    }

    public static AlertDialog showLoadingDialog(Context context) {
        return showLoadingDialog(context, "正在载入中…", false);
    }

    public static void showDateDialog(Context context, CharSequence title, final TextView tv,
                                      boolean isShowNow) {
        showDateDialog(context, title, tv, isShowNow, null);
    }

    /**
     * 显示日期对话框，默认显示的日期为当前时间
     *
     * @param title     标题
     * @param tv        用于接收选的择日期，如果此控件中已经有一个日期格式的字符串则接此日期显示
     * @param isShowNow 是否显示“至今”按钮
     * @param handler   点击“确认”按钮后会通过handler发消息，可以为空
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void showDateDialog(Context context, CharSequence title,
                                      final TextView tv,
                                      boolean isShowNow,
                                      final Handler handler) {
        String strDate = tv.getText() + "";
        int year, monthOfYear, dayOfMonth;
        try {
            year = Integer.parseInt(strDate.split("-")[0]);
            monthOfYear = Integer.parseInt(strDate.split("-")[1]);
            dayOfMonth = Integer.parseInt(strDate.split("-")[2]);
        } catch (NumberFormatException e) {
            Calendar cal = Calendar.getInstance();
            year = cal.get(Calendar.YEAR);
            monthOfYear = cal.get(Calendar.MONTH) + 1;
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        }
        tv.setTag(year + "-" + monthOfYear + "-" + dayOfMonth);
        OnDateChangedListener dateListener = new OnDateChangedListener() {

            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                tv.setTag(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);
            }
        };
        DatePicker datePicker = new DatePicker(new ContextThemeWrapper(context, theme));
        datePicker.setCalendarViewShown(false);// 不显示日历风格的界面
        datePicker.init(year, monthOfYear - 1, dayOfMonth, dateListener);

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, theme))
                .setTitle(title).setView(datePicker);
        if (isShowNow) {
            builder.setNeutralButton("至今", new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    tv.setText("至今");
                    if (handler != null) {
                        Message msg = Message.obtain();
                        msg.what = DialogUtils.RESULT_OK;
                        msg.obj = tv.getText() + "";
                        handler.sendMessage(msg);
                    }
                }
            });
        }
        builder.setPositiveButton("确认", new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                tv.setText(DateTimeUtils.formatDateString(tv.getTag() + ""));
                if (handler != null) {
                    Message msg = Message.obtain();
                    msg.what = DialogUtils.RESULT_OK;
                    msg.obj = tv.getText() + "";
                    handler.sendMessage(msg);
                }
            }
        });
        builder.setNegativeButton("返回", new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (handler != null) {
                    handler.sendEmptyMessage(DialogUtils.RESULT_CANCELED);
                }
            }
        });
        builder.show();
    }

    public static void showDateTimeDialog(Context context, CharSequence title, final TextView tv) {
        showDateTimeDialog(context, title, tv, null);
    }

    /**
     * 显示日期和时间对话框
     *
     * @param handler 点击“确认”按钮后会通过handler发消息，可以为空
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void showDateTimeDialog(Context context, CharSequence title, final TextView tv,
                                          final Handler handler) {
        String strDateTime = tv.getText() + "";
        int year, monthOfYear, dayOfMonth, hour, minute;
        try {
            String strDate = strDateTime.split(" ")[0];
            year = Integer.parseInt(strDate.split("-")[0]);
            monthOfYear = Integer.parseInt(strDate.split("-")[1]);
            dayOfMonth = Integer.parseInt(strDate.split("-")[2]);
            String strTime = strDateTime.split(" ")[1];
            hour = Integer.parseInt(strTime.split(":")[0]);
            minute = Integer.parseInt(strTime.split(":")[1]);
        } catch (NumberFormatException e) {
            Calendar cal = Calendar.getInstance();
            year = cal.get(Calendar.YEAR);
            monthOfYear = cal.get(Calendar.MONTH) + 1;
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            hour = cal.get(Calendar.HOUR_OF_DAY);
            minute = cal.get(Calendar.MINUTE);
        }
        String strDate = DateTimeUtils.formatDateString(year + "-" + monthOfYear + "-" + dayOfMonth);
        tv.setTag(strDate);
        OnDateChangedListener dateListener = new OnDateChangedListener() {

            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                tv.setTag(DateTimeUtils.formatDateString(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth));
            }
        };
        // 设置日历控件
        DatePicker datePicker = new DatePicker(new ContextThemeWrapper(context, theme));
        datePicker.setCalendarViewShown(false);// 不显示日历风格的界面
        datePicker.init(year, monthOfYear - 1, dayOfMonth, dateListener);
        // 设置时间控件
        final TimePicker timePicker = new TimePicker(new ContextThemeWrapper(context, theme));
        timePicker.setIs24HourView(true);
        timePicker.setCurrentHour(hour);
        timePicker.setCurrentMinute(minute);

        LinearLayout layout = new LinearLayout(context);
        float density = context.getResources().getDisplayMetrics().density;
        int padding = (int) (density * 10);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(datePicker);
        layout.addView(timePicker);
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layout);

        new AlertDialog.Builder(new ContextThemeWrapper(context, theme))
                .setTitle(title)
                .setView(scrollView)
                .setPositiveButton("确认", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String strTime = timePicker.getCurrentHour() + ":" + timePicker.getCurrentMinute();
                        strTime = DateTimeUtils.formatTimeString(strTime);
                        tv.setText(tv.getTag() + " " + strTime);
                        if (handler != null) {
                            Message msg = Message.obtain();
                            msg.what = DialogUtils.RESULT_OK;
                            msg.obj = tv.getText() + "";
                            handler.sendMessage(msg);
                        }
                    }
                })
                .setNegativeButton("返回", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (handler != null)
                            handler.sendEmptyMessage(DialogUtils.RESULT_CANCELED);
                    }
                }).show();
    }

    public static void showTimeDialog(Context context, CharSequence title, final TextView tv) {
        showTimeDialog(context, title, tv, null);
    }

    /**
     * 显示时间对话框
     *
     * @param handler 在按下确认按钮后发送的消息，可以为null
     */
    public static void showTimeDialog(Context context, CharSequence title, final TextView tv, final Handler handler) {
        String strTime = tv.getText() + "";
        int hour, minute;
        try {
            hour = Integer.parseInt(strTime.split(":")[0]);
            minute = Integer.parseInt(strTime.split(":")[1]);
        } catch (NumberFormatException e) {
            Calendar cal = Calendar.getInstance();
            hour = cal.get(Calendar.HOUR_OF_DAY);
            minute = cal.get(Calendar.MINUTE);
        }

        final TimePicker timePicker = new TimePicker(new ContextThemeWrapper(context, theme));
        timePicker.setIs24HourView(true);
        timePicker.setCurrentHour(hour);
        timePicker.setCurrentMinute(minute);

        LinearLayout layout = new LinearLayout(context);
        float density = context.getResources().getDisplayMetrics().density;
        int padding = (int) (density * 10);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(timePicker);

        new AlertDialog.Builder(new ContextThemeWrapper(context, theme))
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("确认", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String strTime = timePicker.getCurrentHour() + ":" + timePicker.getCurrentMinute();
                        strTime = DateTimeUtils.formatTimeString(strTime);// 格式化输出
                        tv.setText(strTime);
                        if (handler != null) {
                            Message msg = Message.obtain();
                            msg.what = DialogUtils.RESULT_OK;
                            msg.obj = strTime;
                            handler.sendMessage(msg);
                        }
                    }
                })
                .setNegativeButton("返回", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (handler != null)
                            handler.sendEmptyMessage(DialogUtils.RESULT_CANCELED);
                    }
                })
                .show();
    }

    public static void showEditTextDialog(Context context, CharSequence title, final TextView tv) {
        showEditTextDialog(context, title, tv, InputType.TYPE_CLASS_TEXT, null);
    }

    public static void showEditTextDialog(Context context, CharSequence title, final TextView tv, Handler handler) {
        showEditTextDialog(context, title, tv, InputType.TYPE_CLASS_TEXT, handler);
    }

    /**
     * 显示一个带有输入框的对话框
     *
     * @param title     对话框标题
     * @param tv        与输入框关联的文本控件，可以为空
     * @param inputType 输入的类型
     * @param handler   在对话框按确认后可以通过消息机制把输入的内容发送出去，可以为空
     */
    public static void showEditTextDialog(Context context, CharSequence title, final TextView tv, int inputType, final Handler handler) {
        LinearLayout layout = new LinearLayout(context);
        float density = context.getResources().getDisplayMetrics().density;
        int padding = (int) (density * 10);
        layout.setPadding(padding, padding, padding, padding);
        final EditText et = new EditText(new ContextThemeWrapper(context, theme));
        et.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        et.setText(tv != null ? tv.getText() + "" : "");
        et.setHint(TextUtils.isEmpty(tv != null ? tv.getHint() + "" : "") ? "" : (tv != null ? tv.getHint() : ""));
        et.setInputType(inputType);
        layout.addView(et);

        new AlertDialog.Builder(new ContextThemeWrapper(context, theme))
                .setTitle(title)
                .setView(layout)
                .setNegativeButton("返回", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (handler != null)
                            handler.sendEmptyMessage(DialogUtils.RESULT_CANCELED);
                    }
                })
                .setPositiveButton("确认", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (tv != null)
                            tv.setText(et.getText() + "");
                        if (handler != null) {
                            Message msg = Message.obtain();
                            msg.what = DialogUtils.RESULT_OK;
                            msg.obj = et.getText() + "";
                            handler.sendMessage(msg);
                        }
                    }
                })
                .show();
    }

    /**
     * 显示一个单选列表的对话框
     *
     * @param title   对话框标题
     * @param items   所有显示的单选数组
     * @param tv      与单选结果关联的文本控件，可以为空
     * @param handler 当选择之后可以把被选项内容通过消息机制发送出去，可以为空
     */
    public static void showSingleSelectDialog(Context context, CharSequence title, final String[] items,
                                              final TextView tv, final Handler handler) {
        int which = 0;
        if (tv != null) {
            if (TextUtils.isEmpty(tv.getText())) {
                tv.setText(items[0] + "");
            } else {
                String currItem = tv.getText() + "";
                for (int i = 0; i < items.length; i++) {
                    if (currItem.equals(items[i]))
                        which = i;
                }
            }
        }
        new AlertDialog.Builder(new ContextThemeWrapper(context, theme))
                .setTitle(title)
                .setSingleChoiceItems(items, which, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (tv != null) {
                            tv.setTag(items[which] + "|" + which);
                            tv.setText(items[which]);
                        }
                        if (handler != null) {
                            Message message = Message.obtain();
                            message.obj = items[which];
                            message.arg1 = which;
                            message.what = DialogUtils.RESULT_OK;
                            handler.sendMessage(message);
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("返回", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (handler != null) {
                            Message message = Message.obtain();
                            message.what = Activity.RESULT_CANCELED;
                            handler.sendMessage(message);
                        }
                    }
                })
                .show();
    }

    public static void showSingleSelectDialog(Context context, CharSequence title, final String[] items, final TextView tv) {
        showSingleSelectDialog(context, title, items, tv, null);
    }


    /**
     * 显示一个带提示文字的对话框，带有“确认”和“取消”两个按钮
     */
    public static AlertDialog showConfirmDialog(Context context, String title, CharSequence message,
                                                final Handler handler) {
        return showConfirmDialog(context, title, message, handler, true);
    }

    /**
     * 显示一个带提示文字的对话框
     *
     * @param context      提供一个带Activity的上下文
     * @param title        对话框的标题
     * @param message      提示文字
     * @param isShowCancel 是否需要显示取消对话框
     * @param handler      点击“确认”按钮后消息处理
     */
    public static AlertDialog showConfirmDialog(Context context, String title, CharSequence message,
                                                final Handler handler, boolean isShowCancel) {
        final AlertDialog dlg = new AlertDialog.Builder(new ContextThemeWrapper(context, theme))
                .setTitle(title).setMessage(message)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .create();
        dlg.setButton(DialogInterface.BUTTON_POSITIVE, "确认", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (handler != null)
                    handler.sendEmptyMessage(DialogUtils.RESULT_OK);
                dlg.dismiss();
            }
        });
        // 根据变量判断是否需要显示“取消”按钮
        if (isShowCancel) {
            dlg.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (handler != null)
                        handler.sendEmptyMessage(DialogUtils.RESULT_CANCELED);
                    dlg.dismiss();
                }
            });
        }
        dlg.show();
        return dlg;
    }

    public static void showPhoneTextDialog(final Context context, CharSequence title, final TextView tv) {
        showPhoneTextDialog(context, title, tv, null);
    }

    /**
     * 显示一个对话框接收电话号码的输入
     */
    public static void showPhoneTextDialog(final Context context, CharSequence title, final TextView tv, final Handler handler) {
        LinearLayout layout = new LinearLayout(context);
        float density = context.getResources().getDisplayMetrics().density;
        int padding = (int) (density * 10);
        layout.setPadding(padding, padding, padding, padding);
        final EditText et = new EditText(new ContextThemeWrapper(context, theme));
        et.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        et.setText(tv != null ? tv.getText() + "" : "");
        et.setInputType(InputType.TYPE_CLASS_PHONE | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(et);

        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(context, theme))
                .setTitle(title).setView(layout)
                .setNegativeButton("返回", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (handler != null)
                            handler.sendEmptyMessage(DialogUtils.RESULT_CANCELED);
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("确认", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (StringFormatUtils.isMobile(et.getText() + "")) {
                            if (tv != null)
                                tv.setText(et.getText() + "");
                            dialog.dismiss();
                            if (handler != null) {
                                Message msg = Message.obtain();
                                msg.what = DialogUtils.RESULT_OK;
                                msg.obj = et.getText() + "";
                                handler.sendMessage(msg);
                            }
                        } else {
                            ToastUtils.show(context, R.string.invalid_phone_number );
                        }
                    }
                }).create();
        // 默认情况下点击按钮后对话框总会被关闭，加了下面这段反射的目的就是防止对话框的默认关闭功能，
        // 必须通过代码用dialog.dismiss()才能关闭代码
        try {
            Field field = dialog.getClass().getDeclaredField("mAlert");
            field.setAccessible(true);
            // 获得mAlert变量的值
            Object obj = field.get(dialog);
            field = obj.getClass().getDeclaredField("mHandler");
            field.setAccessible(true);
            // 修改mHandler变量的值，使用新的ButtonHandler类
            field.set(obj, new ButtonHandler(dialog));
        } catch (Exception e) {
        }
        // 显示对话框
        dialog.show();
    }

    public static void showEmailTextDialog(final Context context, CharSequence title, final TextView tv) {
        showEmailTextDialog(context, title, tv, null);
    }

    /**
     * 显示一个对话框接收邮箱地址的输入
     */
    public static void showEmailTextDialog(final Context context, CharSequence title, final TextView tv, final Handler handler) {
        LinearLayout layout = new LinearLayout(context);
        float density = context.getResources().getDisplayMetrics().density;
        int padding = (int) (density * 10);
        layout.setPadding(padding, padding, padding, padding);
        final EditText et = new EditText(new ContextThemeWrapper(context, theme));
        et.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        et.setText(tv != null ? tv.getText() + "" : "");
        et.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(et);

        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(context, theme))
                .setTitle(title).setView(layout)
                .setNegativeButton("返回", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (handler != null)
                            handler.sendEmptyMessage(DialogUtils.RESULT_CANCELED);
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("确认", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (StringFormatUtils.isEmail(et.getText() + "")) {
                            if (tv != null)
                                tv.setText(et.getText() + "");
                            dialog.dismiss();
                            if (handler != null) {
                                Message msg = Message.obtain();
                                msg.what = DialogUtils.RESULT_OK;
                                msg.obj = et.getText() + "";
                                handler.sendMessage(msg);
                            }
                        } else {
                            ToastUtils.show(context, R.string.invalid_email_address);
                        }
                    }
                }).create();
        // 默认情况下点击按钮后对话框总会被关闭，加了下面这段反射的目的就是防止对话框的默认关闭功能，
        // 必须通过代码用dialog.dismiss()才能关闭代码
        try {
            Field field = dialog.getClass().getDeclaredField("mAlert");
            field.setAccessible(true);
            // 获得mAlert变量的值
            Object obj = field.get(dialog);
            field = obj.getClass().getDeclaredField("mHandler");
            field.setAccessible(true);
            // 修改mHandler变量的值，使用新的ButtonHandler类
            field.set(obj, new ButtonHandler(dialog));
        } catch (Exception e) {
        }
        // 显示对话框
        dialog.show();
    }

    private static class ButtonHandler extends Handler {

        private WeakReference<DialogInterface> mDialog;

        public ButtonHandler(DialogInterface dialog) {
            mDialog = new WeakReference<DialogInterface>(dialog);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case DialogInterface.BUTTON_POSITIVE:
                case DialogInterface.BUTTON_NEGATIVE:
                case DialogInterface.BUTTON_NEUTRAL:
                    ((OnClickListener) msg.obj).onClick(mDialog.get(), msg.what);
                    break;
            }
        }
    }

    /**
     * 设置Activity的透明度
     *
     * @param value 取值范围在0－1之间
     */
    public static void setActivityAlpha(Activity activity, float value) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.alpha = value;
        activity.getWindow().setAttributes(params);
    }

}