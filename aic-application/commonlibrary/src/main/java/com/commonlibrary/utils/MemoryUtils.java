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

package com.commonlibrary.utils;

import android.app.ActivityManager;
import android.content.Context;

/**
 * 手机内存工具类
 */
public class MemoryUtils {
    private static Context context;

    public static void init(Context context) {
        MemoryUtils.context = context.getApplicationContext();
    }

    /**
     * 获取手机内存大小（单位B）
     *
     * @return
     */
    public static long getTotalMemory() {
        long initial_memory;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        initial_memory = mi.totalMem;
        return initial_memory;
    }

    /**
     * 获取系统可用内存大小（单位B）
     */
    public static long getAvailMemory() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem;
    }

    /**
     * 判断系统是否内存不足
     */
    public static boolean isLowMemory() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.lowMemory;
    }

    /**
     * 获取已使用的内存大小（单位KB）
     */
    public static long getUsedMemory() {
        return getTotalMemory() - getAvailMemory();
    }


}
