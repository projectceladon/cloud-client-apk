package com.commonlibrary.utils;

import android.app.ActivityManager;
import android.content.Context;

/**
 * 手机内存工具类
 */
public class MemoryUtils {
    private static Context context;

    public static void init(Context context) {
        MemoryUtils.context = context.getApplicationContext();
    }

    /**
     * 获取手机内存大小（单位B）
     *
     * @return
     */
    public static long getTotalMemory() {
        long initial_memory;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        initial_memory = mi.totalMem;
        return initial_memory;
    }

    /**
     * 获取系统可用内存大小（单位B）
     */
    public static long getAvailMemory() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem;
    }

    /**
     * 判断系统是否内存不足
     */
    public static boolean isLowMemory() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.lowMemory;
    }

    /**
     * 获取已使用的内存大小（单位KB）
     */
    public static long getUsedMemory() {
        return getTotalMemory() - getAvailMemory();
    }


}
