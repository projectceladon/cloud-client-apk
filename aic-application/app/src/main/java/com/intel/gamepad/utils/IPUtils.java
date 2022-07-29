package com.intel.gamepad.utils;

import android.util.Log;

import com.intel.gamepad.app.AppConst;
import com.jeremy.fastsharedpreferences.FastSharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPUtils {
    public static final String DEFAULT_IP = "http://10.239.93.183:8095/";
    public static final String DEFAULT_COTURN_IP = "10.239.93.183";
    public static final String DEFAULT_PEERID = "s0";
    public static final String DEFAULT_TOKENID = "c0";
    public static final boolean DEFAULT_ALPHA_CHANNEL = true;
    public static final boolean DEFAULT_FOR_TEST = false;
    public static final boolean DEFAULT_PORTRAIT = false;
    public static final String DEFAULT_CODEC = AppConst.H264;
    private static final String TAG = "IPUtils";

    public static void saveMediaCodec(String codec) {
        FastSharedPreferences.get("ip_file").edit().putString("mediacodec", codec).commit();
    }

    public static void savePortrait(boolean portrait) {
        FastSharedPreferences.get("ip_file").edit().putBoolean("portrait", portrait).commit();
    }

    public static void savetest(boolean test) {
        FastSharedPreferences.get("ip_file").edit().putBoolean("fortest", test).commit();
    }

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
        if (ip != null) {
            Log.i(TAG, ip);
        }
        return ip;
    }

    public static String loadCoturnIP() {
        String ip = FastSharedPreferences.get("ip_file").getString("coturnip", DEFAULT_COTURN_IP);
        if (ip != null) {
            Log.i(TAG, ip);
        }
        return ip;
    }

    public static String loadPeerID() {
        String peerid = FastSharedPreferences.get("ip_file").getString("peerid", DEFAULT_PEERID);
        if (peerid != null) {
            Log.i(TAG, peerid);
        }
        return peerid;
    }

    public static String loadTokenID() {
        String token = FastSharedPreferences.get("ip_file").getString("token", DEFAULT_TOKENID);
        if (token != null) {
            Log.i(TAG, token);
        }
        return token;
    }

    public static boolean loadAlphaChannel() {
        boolean alphaChannel = FastSharedPreferences.get("ip_file").getBoolean("alphachannel", DEFAULT_ALPHA_CHANNEL);
        Log.i(TAG, alphaChannel + "");
        return alphaChannel;
    }

    public static boolean loadTest() {
        return FastSharedPreferences.get("ip_file").getBoolean("fortest", DEFAULT_FOR_TEST);
    }

    public static boolean loadPortrait() {
        return FastSharedPreferences.get("ip_file").getBoolean("portrait", DEFAULT_PORTRAIT);
    }

    public static String loadMediaCodec() {
        String codec = FastSharedPreferences.get("ip_file").getString("mediacodec", DEFAULT_CODEC);
        if (codec != null) {
            Log.i(TAG, codec);
        }
        return codec;
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
