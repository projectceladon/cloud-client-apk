package com.commonlibrary.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import com.commonlibrary.R;

/**
 * 作者：许仁方
 * 日期：2016-2-1
 * 网络设备工具类
 * 需要权限：
 * <!--访问网络状态-->
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
 * <!--访问WIFI状态-->
 * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
 */
public class NetDeviceUtils {
    public static final String TAG = "NetDeviceUtils";
    private static Context context;

    private NetDeviceUtils() {
    }

    public static void init(Context context) {
        NetDeviceUtils.context = context.getApplicationContext();
    }

    /**
     * 判断是否有网络连接
     *
     * @param isPrompt 当网络未连接时用Toast提示
     */
    @SuppressLint("MissingPermission")
    public static boolean isNetworkConnected(boolean isPrompt) {
        if (context == null) return false;
        ConnectivityManager mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isAvailable() && netInfo.isConnected()) {
            return true;
        }
        if (isPrompt)
            Toast.makeText(context, R.string.no_network, Toast.LENGTH_LONG).show();
        return false;
    }

    /**
     * 判断是否有网络连接
     */
    public static boolean isNetworkConnected() {
        return isNetworkConnected(true);
    }

    /**
     * 获取网络连接的状态，包括设备是否可用、是否连接和是否漫游三个状态的信息
     *
     * @return 将状态以“|”字符分隔拼接，例如：“可用|连接|空闲”
     */
    @SuppressLint("MissingPermission")
    public static String getNetworkState() {
        String state = null;
        ConnectivityManager mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = mConnectivityManager.getActiveNetworkInfo();
        if (network != null) {
            state = network.isAvailable() ? "可用|" : "禁用|";
            state += network.isConnected() ? "连接|" : "断开|";
            state += network.isRoaming() ? "漫游" : "空闲";
        }
        return state;
    }

    /**
     * 判断WIFI网络是否可用
     *
     * @param isPrompt 　当WIFI未连接时提示
     * @return
     */
    @SuppressLint("MissingPermission")
    public static boolean isWifiConnected(boolean isPrompt) {
        if (context == null) return false;
        ConnectivityManager mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifi != null && wifi.isAvailable() && wifi.isConnected()) {
            return true;
        }
        if (isPrompt)
            Toast.makeText(context, R.string.no_wifi, Toast.LENGTH_LONG).show();
        return false;
    }

    /**
     * 获取WIFI的状态，包括设备是否可用、是否连接和是否漫游三个状态的信息
     *
     * @return 将状态以“|”字符分隔拼接，例如：“可用|连接|空闲”
     */
    @SuppressLint("MissingPermission")
    public static String getWifiState() {
        String state = null;
        ConnectivityManager mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifi != null) {
            state = wifi.isAvailable() ? "可用|" : "禁用|";
            state += wifi.isConnected() ? "连接|" : "断开|";
            state += wifi.isRoaming() ? "漫游" : "空闲";
        }
        return state;
    }

    /**
     * 判断移动网络是否可用，如果WIFI网络开启时会断开移动网络
     *
     * @param isPrompt 当网络设备不可用时弹Toast提示
     * @return
     */
    @SuppressLint("MissingPermission")
    public static boolean isMobileConnected(boolean isPrompt) {
        if (context == null) return false;
        ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobile != null && mobile.isAvailable() && mobile.isConnected()) {
            return true;
        }
        if (isPrompt)
            Toast.makeText(context, R.string.no_mobile_network, Toast.LENGTH_LONG).show();
        return false;
    }

    /**
     * 获取移动网络状态，包括设备是否可用、是否连接和是否漫游三个状态的信息
     *
     * @return 将状态以“|”字符分隔拼接，例如：“可用|连接|空闲”
     */
    @SuppressLint("MissingPermission")
    public static String getMobileState() {
        String state = null;
        ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobile != null) {
            state = mobile.isAvailable() ? "可用|" : "禁用|";
            state += mobile.isConnected() ? "连接|" : "断开|";
            state += mobile.isRoaming() ? "漫游" : "空闲";
        }
        return state;
    }

    /**
     * 获取当前网络连接的类型信息
     *
     * @return int 返回类型
     * @throws
     * @Title: getConnectedType
     */
    @SuppressLint("MissingPermission")
    public static int getConnectedType() {
        if (context == null) return -1;
        ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (mNetworkInfo != null && mNetworkInfo.isAvailable()) {
            return mNetworkInfo.getType();
        } else
            return -1;
    }

    /**
     * 返回已连接的设备类型名称
     *
     * @return
     */
    public static String getConnectedTypeString() {
        String typeString = null;
        switch (getConnectedType()) {
            case 0:
                typeString = "GPRS移动网络";
                break;
            case 1:
                typeString = "WIFI网络";
                break;
            case 7:
                typeString = "蓝牙";
                break;
            case 9:
                typeString = "以太网";
                break;
        }
        return typeString;
    }

    /**
     * 获取WIFI环境下的IP地址
     */
    public static long getIPAddress() {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifi.getConnectionInfo().getIpAddress();
    }

    /**
     * 当以WIFI形式连接时可以获取WIFI的详情
     *
     * @return
     */
    public static String getWifiInformation() {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        return "HiddenSSID=" + info.getHiddenSSID() + "\n" +
                "IpAddress=" + formatIP4(info.getIpAddress()) + "\n" +
                "LinkSpeed=" + info.getLinkSpeed() + "\n" +
                "NetworkId=" + info.getNetworkId() + "\n" +
                "Rssi=" + info.getRssi() + "\n" +
                "SSID=" + info.getSSID() + "\n" +
                "MacAddress=" + info.getMacAddress() + "\n";
    }

    /**
     * 将10进制整数形式转换成127.0.0.1形式的IP地址
     */
    public static String formatIP4(long longIP) {
        return (longIP & 0xFF) + "." +
                ((longIP >> 8) & 0xFF) + "." +
                ((longIP >> 16) & 0xFF) + "." +
                (longIP >> 24 & 0xFF);
    }

}
