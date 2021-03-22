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
        String localeJson = FastSharedPreferences
                .get(LOCALE_SP)
                .getString(LOCALE_SP_KEY, "en");
        return new Gson().fromJson(localeJson, Locale.class);
    }

    private static void setLocale(Locale pUserLocale) {
        String json = new Gson().toJson(pUserLocale);
        SharedPreferences.Editor edit = FastSharedPreferences.get(LOCALE_SP).edit();
        edit.putString(LOCALE_SP_KEY, json);
        edit.apply();
    }

    public static boolean updateLocale(Context context, Locale locale) {
        if (needUpdateLocale(context, locale)) {
            Configuration configuration = context.getResources().getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                configuration.setLocale(locale);
            } else {
                configuration.locale = locale;
            }
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            context.getResources().updateConfiguration(configuration, displayMetrics);
            setLocale(locale);
            return true;
        }
        return false;
    }

    public static boolean needUpdateLocale(Context pContext, Locale newUserLocale) {
        return newUserLocale != null && !getCurrentLocale(pContext).equals(newUserLocale);
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