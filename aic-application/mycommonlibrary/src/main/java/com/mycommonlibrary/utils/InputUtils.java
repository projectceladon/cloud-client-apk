package com.mycommonlibrary.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class InputUtils {
    /**
     * 切换显示软键盘 这个效果是：如果有软键盘，那么隐藏它；反之，把它显示出来。
     *
     * @param context
     */
    public static void switchInput(Context context) {
        // 1.得到InputMethodManager对象
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        // 2.调用toggleSoftInput方法，实现切换显示软键盘的功能。
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * 显示软键盘
     *
     * @param context
     * @param view
     */
    public static void show(Context context, View view) {
        // 1.得到InputMethodManager对象
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        // 2.调用showSoftInput方法显示软键盘，其中view为聚焦的view组件
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
    }

    /**
     * 隐藏软键盘
     *
     * @param context
     * @param view
     */
    public static void hide(Context context, View view) {
        // 1.得到InputMethodManager对象
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        // 2.调用hideSoftInputFromWindow方法隐藏软键盘
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0); // 强制隐藏键盘
    }

    /**
     * 判断输入法是否打开的状态
     *
     * @param context
     */
    public static boolean isInputMethodActive(Context context) {
        // 1.得到InputMethodManager对象
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        // 获取状态信息
        boolean isOpen = imm.isActive();// isOpen若返回true，则表示输入法打开
        return isOpen;
    }
}
