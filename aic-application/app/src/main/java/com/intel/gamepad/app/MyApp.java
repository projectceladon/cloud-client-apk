package com.intel.gamepad.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.util.Log;

import androidx.annotation.NonNull;

import com.commonlibrary.application.UtilsApplication;
import com.intel.gamepad.BuildConfig;
import com.intel.gamepad.utils.LanguageUtils;
import com.jeremy.fastsharedpreferences.FastSharedPreferences;
import com.jeremyliao.liveeventbus.LiveEventBus;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheEntity;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.cookie.CookieJarImpl;
import com.lzy.okgo.cookie.store.SPCookieStore;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import okhttp3.OkHttpClient;

public class MyApp extends UtilsApplication {
    public static Context context;
    public static String pId;

    @Override
    public void onCreate() {
        super.onCreate();
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

        initOkGo();
        initLiveEvent();
        FastSharedPreferences.init(context);
    }

    private void initLiveEvent() {
        LiveEventBus.config().supportBroadcast(this).lifecycleObserverAlwaysActive(false);
    }

    private void initOkGo() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // 配置LOG
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");
        //log打印级别，决定了log显示的详细程度
        loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
        //log颜色级别，决定了log在控制台显示的颜色
        loggingInterceptor.setColorLevel(Level.INFO);
        builder.addInterceptor(loggingInterceptor);

        //全局的读取超时时间
        long timeout = 10000L; // 10秒
        builder.readTimeout(timeout, TimeUnit.MILLISECONDS);
        //全局的写入超时时间
        builder.writeTimeout(timeout, TimeUnit.MILLISECONDS);
        //全局的连接超时时间
        builder.connectTimeout(timeout, TimeUnit.MILLISECONDS);

        //使用sp保持cookie，如果cookie不过期，则一直有效
        builder.cookieJar(new CookieJarImpl(new SPCookieStore(this)));

        OkGo.getInstance().init(this)                           //必须调用初始化
                .setOkHttpClient(builder.build())               //建议设置OkHttpClient，不设置将使用默认的
                .setCacheMode(CacheMode.NO_CACHE)               //全局统一缓存模式，默认不使用缓存，可以不传
                .setCacheTime(CacheEntity.CACHE_NEVER_EXPIRE)   //全局统一缓存时间，默认永不过期，可以不传
                .setRetryCount(3)                               //全局统一超时重连次数，默认为三次，那么最差的情况会请求4次(一次原始请求，三次重连请求)，不需要可以设置为0
        ;
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
