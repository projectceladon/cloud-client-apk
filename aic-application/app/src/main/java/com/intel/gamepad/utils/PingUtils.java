package com.intel.gamepad.utils;

import android.text.TextUtils;

import com.mycommonlibrary.utils.LogEx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PingUtils {

    public static String getPing(String ipAddress) {
        Process p = null;
        BufferedReader buf = null;
        try {
            p = Runtime.getRuntime().exec("ping -c 4 " + ipAddress);
            buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String str;
            StringBuilder builder = new StringBuilder();
            while ((str = buf.readLine()) != null) {
                builder.append(str + "\n");
            }
            LogEx.i(builder.toString());
            p.getInputStream().close();
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (p != null) {
                try {
                    p.getInputStream().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (buf != null) {
                try {
                    buf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }

    public static double parseDelay(String pingResult, String which) {
        if (TextUtils.isEmpty(pingResult)
                || pingResult.contains("Destination Host UnreachableFrom")
                || !pingResult.contains("min/avg/max/mdev")
        ) return -1;
        String rtt = pingResult.substring(pingResult.indexOf("rtt"));
        int index = 0;
        switch (which) {
            case "min":
                index = 0;
                break;
            case "avg":
                index = 1;
                break;
            case "max":
                index = 2;
                break;
            case "mdev":
                index = 3;
                break;
        }
        String result = rtt.split("=")[1].split("/")[index];
        return Double.parseDouble(result);
    }

    public static double parseMinDelay(String pingResult) {
        return PingUtils.parseDelay(pingResult, "min");
    }

    public static double parseAvgDelay(String pingResult) {
        return PingUtils.parseDelay(pingResult, "avg");
    }

    public static double parseMaxDelay(String pingResult) {
        return PingUtils.parseDelay(pingResult, "max");
    }

    public static double parseMdevDelay(String pingResult) {
        return PingUtils.parseDelay(pingResult, "mdev");
    }
}
