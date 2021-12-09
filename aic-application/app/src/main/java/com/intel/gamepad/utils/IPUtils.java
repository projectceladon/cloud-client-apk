package com.intel.gamepad.utils;

import com.commonlibrary.utils.LogEx;
import com.jeremy.fastsharedpreferences.FastSharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPUtils {
    public static final String DEFAULT_IP = "http://10.239.93.183:30000/";
    public static final String DEFAULT_COTURN_IP = "10.239.93.183";
    public static final String DEFAULT_PEERID = "s0";
    public static final String DEFAULT_TOKENID = "c0";
    public static final boolean DEFAULT_ALPHA_CHANNEL = true;

    public static void savealphachannel(boolean alphaChannel) {
        FastSharedPreferences.get("ip_file").edit().putBoolean("alphachannel", alphaChannel).commit();
    }

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

    public static boolean loadAlphaChannel() {
        boolean alphaChannel = FastSharedPreferences.get("ip_file").getBoolean("alphachannel", DEFAULT_ALPHA_CHANNEL);
        LogEx.i(alphaChannel + "");
        return alphaChannel;
    }

    public static List<String> getIp(String str) {
        ArrayList<String> ipInfo = new ArrayList<>();
        Pattern p = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+):(\\d+)");
        Matcher m = p.matcher(str);
        while (m.find()) {
            ipInfo.add(m.group(1));
            ipInfo.add(m.group(2));
        }
        return ipInfo;
    }


}
