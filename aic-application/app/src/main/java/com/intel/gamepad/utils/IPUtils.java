package com.intel.gamepad.utils;

import com.jeremy.fastsharedpreferences.FastSharedPreferences;
import com.mycommonlibrary.utils.LogEx;

public class IPUtils {
    public static final String DEFAULT_IP = "http://10.239.93.183:30000/";
    public static final String DEFAULT_COTURN_IP = "10.239.93.183";
    public static final String DEFAULT_PEERID = "s0";
    public static final String DEFAULT_TOKENID = "c0";

    public static void saveip(String IP) {
        FastSharedPreferences.get("ip_file").edit().putString("ip", IP).commit();
    }

    public static void saveCoturn(String IP) {
        FastSharedPreferences.get("ip_file").edit().putString("coturnip", IP).commit();
    }

    public static void savepeerid(String peerid) {
        FastSharedPreferences.get("ip_file").edit().putString("peerid", peerid).commit();
    }

    public static void savetoken(String token) {
        FastSharedPreferences.get("ip_file").edit().putString("token", token).commit();
    }

    public static String loadIP() {
        String ip = FastSharedPreferences.get("ip_file").getString("ip", DEFAULT_IP);
        LogEx.i(ip);
        return ip;
    }

    public static String loadCoturnIP() {
        String ip = FastSharedPreferences.get("ip_file").getString("coturnip", DEFAULT_COTURN_IP);
        LogEx.i(ip);
        return ip;
    }

    public static String loadPeerID() {
        String peerid = FastSharedPreferences.get("ip_file").getString("peerid", DEFAULT_PEERID);
        LogEx.i(peerid);
        return peerid;
    }

    public static String loadTokenID() {
        String token = FastSharedPreferences.get("ip_file").getString("token", DEFAULT_TOKENID);
        LogEx.i(token);
        return token;
    }
}
