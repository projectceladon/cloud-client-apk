package com.mycommonlibrary.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

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
        long initial_memory = 0;
        if (Build.VERSION.SDK_INT < 16) {
            String str1 = "/proc/meminfo";// 系统内存信息文件
            String str2;
            String[] arrayOfString;
            FileReader localFileReader = null;
            try {
                localFileReader = new FileReader(str1);
                BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
                str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小
                if(str2 == null) {
                    return initial_memory;
                }
                arrayOfString = str2.split("\\s+");
                initial_memory = Integer.parseInt(arrayOfString[1]) * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
                localBufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(localFileReader != null) {
                    try {
                        localFileReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            initial_memory = mi.totalMem;
        }
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
