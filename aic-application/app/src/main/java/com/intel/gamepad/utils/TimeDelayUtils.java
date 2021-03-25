package com.intel.gamepad.utils;

public class TimeDelayUtils {
    /**
     * 实现延时效果
     *
     * @param delay 延时多少毫秒
     */
    public static void sleep(int delay) {
        for (long lastMillis = System.currentTimeMillis(); (System.currentTimeMillis() - lastMillis) < delay; )
            ;
    }
}
