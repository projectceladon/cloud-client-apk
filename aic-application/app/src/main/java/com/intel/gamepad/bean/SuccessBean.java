package com.intel.gamepad.bean;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SuccessBean {
    /**
     * success : false
     * message : 验证码发送失败
     */

    private boolean success;
    private String message;

    @Override
    public String toString() {
        return "SuccessBean{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }

    public static SuccessBean objectFromData(String str) {

        return new Gson().fromJson(str, SuccessBean.class);
    }

    public static List<SuccessBean> arraySuccessBeanFromData(String str) {

        Type listType = new TypeToken<ArrayList<SuccessBean>>() {
        }.getType();

        return new Gson().fromJson(str, listType);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
