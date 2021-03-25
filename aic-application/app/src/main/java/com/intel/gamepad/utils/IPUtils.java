package com.intel.gamepad.utils;

import com.jeremy.fastsharedpreferences.FastSharedPreferences;
import com.mycommonlibrary.utils.LogEx;

public class IPUtils {
    public static final String DEFAULT_IP = "http://153.35.78.77:8095/";
    public static final String DEFAULT_PEERID = "server0";
    public static final String DEFAULT_TOKENID = "client0";

    public static void saveip(String IP) {
        FastSharedPreferences.get("ip_file").edit().putString("ip", IP).commit();
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
