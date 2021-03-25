package com.mycommonlibrary.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import android.telephony.TelephonyManager;

/**
 * 获取当前手机信息的工具类
 * 需要权限：
 * <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
 * <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
 */
public class TelephonyUtils {
    private static TelephonyManager telephonyManager;

    private static TelephonyManager getPhoneManager(Context context) {
        if (telephonyManager == null) {
            telephonyManager = (TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        }
        return telephonyManager;
    }

    /**
     * 获取手机品牌
     */
    public static String getBrand() {
        return android.os.Build.BRAND;
    }

    /**
     * 获取手机型号
     */
    public static String getModel() {
        return android.os.Build.MODEL;
    }

    /**
     * 获取手机的安卓版本名称
     */
    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public static int getAPIVersion() {
        return Build.VERSION.SDK_INT;
    }

    public static String getIMEI(Context context) {
        int chkResult = PermissionChecker.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE);
        if (chkResult == PermissionChecker.PERMISSION_GRANTED)
            return getPhoneManager(context).getDeviceId();
        return null;
    }

    /**
     * 获取IMSI号(国际移动用户识别码) for a GSM phone
     *
     * @param context
     * @return
     */
    public static String getIMSI(Context context) {
        int chkResult = PermissionChecker.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE);
        if (chkResult == PermissionChecker.PERMISSION_GRANTED)
            return getPhoneManager(context).getSubscriberId();
        return null;
    }

    /**
     * 获取本手机号码
     */
    public static String getPhoneNumber(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return getPhoneManager(context).getLine1Number();
        }
        return null;
    }

    /**
     * 获取IMEI SV
     */
    public static String getIMEISV(Context context) {
        int chkResult = PermissionChecker.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE);
        if (chkResult == PermissionChecker.PERMISSION_GRANTED)
            return getPhoneManager(context).getDeviceSoftwareVersion();
        return null;
    }

    /**
     * 获取手机的信号名称（例如：中国移动4G）
     *
     * @param context
     * @return
     */
    public static String getNetworkOperatorName(Context context) {
        return getPhoneManager(context).getNetworkOperatorName();
    }

    /**
     * 获取手机信号类型
     *
     * @param context
     * @return
     */
    public static String getPhoneType(Context context) {
        int res = getPhoneManager(context).getPhoneType();
        switch (res) {
            case TelephonyManager.PHONE_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.PHONE_TYPE_GSM:
                return "GSM";
            case TelephonyManager.PHONE_TYPE_NONE:
                return "NONE";
            case TelephonyManager.PHONE_TYPE_SIP:
                return "SIP";
        }
        return "";
    }

    /**
     * 获取SIM卡状态
     *
     * @param context
     * @return
     */
    public static String getSIMState(Context context) {
        int state = getPhoneManager(context).getSimState();
        switch (state) {
            case TelephonyManager.SIM_STATE_READY:
                return "SIM卡就绪";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return "锁定状态，需要网络的PIN码解锁";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return "锁定状态，需要用户的PIN码解锁";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return "锁定状态，需要用户的PUK码解锁";
            case TelephonyManager.SIM_STATE_ABSENT:
                return "未插入SIM卡";
        }
        return "未知状态";
    }

    /**
     * 判断SIM卡是否就绪
     *
     * @param context
     * @return
     */
    public static boolean isSIMReady(Context context) {
        return getPhoneManager(context).getSimState() == TelephonyManager.SIM_STATE_READY;
    }

    /**
     * 获取运营商
     *
     * @param context
     * @return
     */
    public static String getServiceName(Context context) {
        if (getPhoneManager(context).getSimState() == TelephonyManager.SIM_STATE_READY)
            return getPhoneManager(context).getSimOperatorName();
        else
            return "";
    }

    /**
     * 获取SIM卡的序列号
     *
     * @param context
     * @return
     */
    public static String getSIMSerialNumber(Context context) {
        return getPhoneManager(context).getSimSerialNumber();
    }
}
