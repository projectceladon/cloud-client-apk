package com.commonlibrary.application;

import android.app.Application;

import com.commonlibrary.utils.DensityUtils;
import com.commonlibrary.utils.MemoryUtils;
import com.commonlibrary.utils.NetDeviceUtils;
import com.commonlibrary.utils.ToastUtils;

public class UtilsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DensityUtils.init(getApplicationContext());
        ToastUtils.init(getApplicationContext());
        NetDeviceUtils.init(getApplicationContext());
        MemoryUtils.init(getApplicationContext());
    }
}
