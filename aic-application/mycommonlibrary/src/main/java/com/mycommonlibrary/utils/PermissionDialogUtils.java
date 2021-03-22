package com.mycommonlibrary.utils;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

/**
 * Created by wood on 2017/5/18.
 */

public class PermissionDialogUtils {

    public static final AlertDialog show(final Context context, String premissionName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("权限提示")
                .setMessage("使用此功能需要" + premissionName + "权限")
                .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new PermissionPageUtils(context, context.getPackageName());
                        dialog.dismiss();
                    }
                });
        return builder.show();
    }

}