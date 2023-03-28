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

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

public class ActivityManager {
    private static final List<Activity> ACTIVITY_LIST = new ArrayList<>();

    private ActivityManager() {
    }

    public static ActivityManager getInstance() {
        return Holder.inst;
    }

    public static void add(Activity activity) {
        if (activity != null)
            ActivityManager.getInstance().getList().add(activity);
    }

    public static void remove(Activity activity) {
        if (activity != null) {
            List<Activity> list = ActivityManager.getInstance().getList();
            int index = list.lastIndexOf(activity);
            list.remove(index).finish();
        }
    }

    public static void finishAll() {
        List<Activity> list = ActivityManager.getInstance().getList();
        for (Activity act : list) {
            act.finish();
        }
        list.clear();
    }

    private List<Activity> getList() {
        return ACTIVITY_LIST;
    }

    private static class Holder {
        private static final ActivityManager inst = new ActivityManager();
    }
}
