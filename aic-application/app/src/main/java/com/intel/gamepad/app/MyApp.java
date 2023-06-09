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

package com.intel.gamepad.app;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.util.Log;

import androidx.annotation.NonNull;

import com.commonlibrary.utils.DensityUtils;
import com.commonlibrary.utils.MemoryUtils;
import com.commonlibrary.utils.NetDeviceUtils;
import com.commonlibrary.utils.ToastUtils;
import com.intel.gamepad.BuildConfig;
import com.intel.gamepad.utils.LanguageUtils;
import com.jeremy.fastsharedpreferences.FastSharedPreferences;
import com.jeremyliao.liveeventbus.LiveEventBus;

import java.util.Locale;
public class MyApp extends Application {
    public static Context context;
    public static String pId;

    @Override
    public void onCreate() {
        super.onCreate();
        DensityUtils.init(getApplicationContext());
        ToastUtils.init(getApplicationContext());
        NetDeviceUtils.init(getApplicationContext());
        MemoryUtils.init(getApplicationContext());

        pId = System.currentTimeMillis() + "";
        context = getApplicationContext();

        try {
            PackageManager pm = context.getPackageManager();
            if(pm!=null){
                PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
                Log.i("AIC-APPLICATION", "versionName = " + pi.versionName + " Branch = " + BuildConfig.BUILD_BRANCH + " Commit = " + BuildConfig.BUILD_COMMIT + " Build Time = " + BuildConfig.BUILD_TIME);
            }
        } catch (Exception e) {
            Log.e("VersionInfo", "Exception", e);
        }

        initLiveEvent();
        FastSharedPreferences.init(context);
    }

    private void initLiveEvent() {
        LiveEventBus.config().supportBroadcast(this).lifecycleObserverAlwaysActive(false);
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        languageWork();
    }

    private void languageWork() {
        Locale locale = LanguageUtils.getLocale();
        LanguageUtils.updateLocale(this, locale);
    }
}
