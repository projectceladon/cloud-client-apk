package com.intel.gamepad.utils;

import java.math.BigDecimal;

public class FormatUtil {

    public static String transferSize(String size, int decimal) {
        double length = Double.parseDouble(size);
        return transferSize(length, decimal);
    }

    public static String transferSize(double size, int decimal) {
        if (size < 1024) {
            return formatValue(size, decimal) + "B";
        } else {
            size = size / 1024.0;
        }
        if (size < 1024) {
            return formatValue(Math.round(size * 100) / 100.0, decimal) + "KB";
        } else {
            size = size / 1024.0;
        }
        if (size < 1024) {
            return formatValue(Math.round(size * 100) / 100.0, decimal) + "MB";
        } else {
            return formatValue(Math.round(size / 1024 * 100) / 100.0, decimal) + "GB";
        }
    }

    public static String formatValue(double value, int decimal) {
        return new BigDecimal(value).setScale(decimal, BigDecimal.ROUND_DOWN).toString();
    }

}
