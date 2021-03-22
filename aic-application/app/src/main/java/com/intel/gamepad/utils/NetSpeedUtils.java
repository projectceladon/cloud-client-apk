package com.intel.gamepad.utils;

import android.net.TrafficStats;

import com.intel.gamepad.app.MyApp;

public class NetSpeedUtils {
    private static long lastTotalRxBytes = 0L;
    private static long lastTimeStamp = 0L;

    private static long getTotalRxBytes() {
        return TrafficStats.getUidRxBytes(
                MyApp.context.getApplicationInfo().uid) == TrafficStats.UNSUPPORTED ? 0 :
                (TrafficStats.getTotalRxBytes() / 1024);//转为KB
    }

    public static String showNetSpeed() {
        long nowTotalRxBytes = getTotalRxBytes();
        long nowTimeStamp = System.currentTimeMillis();
        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / (nowTimeStamp - lastTimeStamp));//毫秒转换
        long speed2 = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 % (nowTimeStamp - lastTimeStamp));//毫秒转换

        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;

        String strSpeed = String.valueOf(speed2);
        if (strSpeed.length() > 2) strSpeed = strSpeed.substring(0, 1);
        return String.valueOf(speed) + "." + strSpeed + " K/s";
    }
}
