package com.intel.gamepad.utils;

import android.os.Handler;

import java.util.HashMap;
import java.util.Map;

public class TimingTaskUtil {

    private final Handler mHanlder;

    private final Map<Runnable,Runnable> mTaskMap = new HashMap<>();

    public TimingTaskUtil(Handler handler) {
        mHanlder = handler;
    }

    public void startTask(Runnable runnable, long interval) {
        startTask(runnable, interval, false);
    }

    public void startTask(final Runnable runnable, final long interval,
                             boolean runImmediately) {

        if (runImmediately) {
            runnable.run();
        }

        Runnable task = mTaskMap.get(runnable);
        if (task == null) {
            task = () -> {
                runnable.run();
                post(runnable, interval);
            };
            mTaskMap.put(runnable, task);
        }
        post(runnable, interval);
    }

    public void endTask(Runnable runnable) {
        if (mTaskMap.containsKey(runnable)) {
            mHanlder.removeCallbacks(mTaskMap.get(runnable));
        }

    }

    private void post(Runnable runnable, long interval) {
        Runnable task = mTaskMap.get(runnable);
        mHanlder.removeCallbacks(task);
        mHanlder.postDelayed(task, interval);
    }
}
