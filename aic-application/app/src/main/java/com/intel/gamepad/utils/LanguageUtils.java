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
