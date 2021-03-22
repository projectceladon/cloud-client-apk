package com.intel.gamepad.utils;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

public class ActivityManager {
    private static final List<Activity> ACTIVITY_LIST = new ArrayList<>();

    private static class Holder {
        private static ActivityManager inst = new ActivityManager();
    }

    private ActivityManager() {
    }

    public static ActivityManager getInstance() {
        return Holder.inst;
    }

    private List<Activity> getList() {
        return ACTIVITY_LIST;
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
}
