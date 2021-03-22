package com.mycommonlibrary.application;

import android.app.Application;
import com.mycommonlibrary.utils.DensityUtils;
import com.mycommonlibrary.utils.MemoryUtils;
import com.mycommonlibrary.utils.NetDeviceUtils;
import com.mycommonlibrary.utils.ToastUtils;

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
