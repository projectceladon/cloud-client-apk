package com.intel.gamepad.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;

import com.google.gson.Gson;
import com.jeremy.fastsharedpreferences.FastSharedPreferences;

import java.util.Locale;

/**
 * 多语言切换工具
 */
public class LanguageUtils {
    private static final String LOCALE_SP = "LOCALE_SP";
    private static final String LOCALE_SP_KEY = "LOCALE_SP_KEY";

    public static Locale getLocale() {
        FastSharedPreferences fp = FastSharedPreferences.get(LOCALE_SP);
        if(fp!=null){
            String  localeJson = fp.getString(LOCALE_SP_KEY, "en");
            if(localeJson !=null ){
                return new Gson().fromJson(localeJson, Locale.class);
            }
        }
        return null;
    }

    private static void setLocale(Locale pUserLocale) {
        String json = new Gson().toJson(pUserLocale);
        FastSharedPreferences fp = FastSharedPreferences.get(LOCALE_SP);
        if(fp!=null){
            SharedPreferences.Editor edit = fp.edit();
            edit.putString(LOCALE_SP_KEY, json);
            edit.apply();
        }
    }

    public static boolean updateLocale(Context context, Locale locale) {
        if (needUpdateLocale(context, locale)) {
            Configuration configuration = context.getResources().getConfiguration();
            configuration.setLocale(locale);
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            context.getResources().updateConfiguration(configuration, displayMetrics);
            setLocale(locale);
            return true;
        }
        return false;
    }

    public static boolean needUpdateLocale(Context pContext, Locale newUserLocale) {
        return newUserLocale != null && !newUserLocale.equals(getCurrentLocale(pContext));
    }

    public static Locale getCurrentLocale(Context context) {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //7.0有多语言设置获取顶部的语言
            locale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            locale = context.getResources().getConfiguration().locale;
        }
        return locale;
    }
}