package com.mycommonlibrary.utils;

import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEditUtils {
    /**
     * 关键字高亮
     */
    public static SpannableString keyLight(String text, String key, int color) {
        SpannableString s = new SpannableString(text);
        Matcher m = Pattern.compile(key).matcher(text);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            s.setSpan(new ForegroundColorSpan(color),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    /**
     * 设置输入框最大输入数
     */
    public static void maxInputFilter(EditText et, int max) {
        InputFilter[] filters = new InputFilter[]{new InputFilter.LengthFilter(max)};
        et.setFilters(filters);
    }
}
