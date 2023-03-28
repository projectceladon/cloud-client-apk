/* Copyright (C) 2021 Intel Corporation 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *   
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * SPDX-License-Identifier: Apache-2.0
 */

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
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            fp.edit().putString("mediacodec", codec).commit();
        }
    }

    public static void savePortrait(boolean portrait) {
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            fp.edit().putBoolean("portrait", portrait).commit();
        }
    }

    public static void savetest(boolean test) {
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            fp.edit().putBoolean("fortest", test).commit();
        }
    }

    public static void savealphachannel(boolean alphaChannel) {
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            fp.edit().putBoolean("alphachannel", alphaChannel).commit();
        }
    }

    public static void saveip(String IP) {
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            fp.edit().putString("ip", IP).commit();
        }
    }

    public static void saveCoturn(String IP) {
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            fp.edit().putString("coturnip", IP).commit();
        }
    }

    public static void savepeerid(String peerid) {
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            fp.edit().putString("peerid", peerid).commit();
        }
    }

    public static void savetoken(String token) {
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            fp.edit().putString("token", token).commit();
        }
    }

    public static String loadIP() {
        String ip = "";
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            ip = fp.getString("ip", DEFAULT_IP);
        }
        Log.i(TAG, ip);
        return ip;
    }

    public static String loadCoturnIP() {
        String ip = "";
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            ip = fp.getString("coturnip", DEFAULT_COTURN_IP);
        }
        Log.i(TAG, ip);
        return ip;
    }

    public static String loadPeerID() {
        String peerid = "";
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            peerid = fp.getString("peerid", DEFAULT_PEERID);
        }
        Log.i(TAG, peerid);
        return peerid;
    }

    public static String loadTokenID() {
        String token = "";
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            token = fp.getString("token", DEFAULT_TOKENID);
        }
        Log.i(TAG, token);
        return token;
    }

    public static boolean loadAlphaChannel() {
        boolean alphaChannel = DEFAULT_ALPHA_CHANNEL;
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            alphaChannel = fp.getBoolean("alphachannel", DEFAULT_ALPHA_CHANNEL);
        }
        Log.i(TAG, alphaChannel + "");
        return alphaChannel;
    }

    public static boolean loadTest() {
        boolean value = DEFAULT_FOR_TEST;
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            value = fp.getBoolean("fortest", DEFAULT_FOR_TEST);
        }
        Log.i(TAG, value + "");
        return value;
    }

    public static boolean loadPortrait() {
        boolean value = DEFAULT_PORTRAIT;
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            value = fp.getBoolean("portrait", DEFAULT_PORTRAIT);
        }
        Log.i(TAG, value + "");
        return value;
    }

    public static String loadMediaCodec() {
        String token = "";
        FastSharedPreferences fp= FastSharedPreferences.get("ip_file");
        if(fp!=null){
            token = fp.getString("mediacodec", DEFAULT_CODEC);
        }
        Log.i(TAG, token);
        return token;
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
