package com.intel.gamepad.utils.permission;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionsUtils {
    private static PermissionsUtils permissionsUtils;
    private final int mRequestCode = 100;
    private IPermissionResult mPermissionsResult;
    private AlertDialog mPermissionDialog;

    private PermissionsUtils() {
    }

    public static PermissionsUtils getInstance() {
        if (permissionsUtils == null) {
            permissionsUtils = new PermissionsUtils();
        }
        return permissionsUtils;
    }

    public void checkPermissions(Activity context, String[] permissions, @NonNull IPermissionResult permissionResult) {
        mPermissionsResult = permissionResult;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            permissionResult.passPermission(true, permissions);
            return;
        }

        List<String> permissionList = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
            }
        }

        if (permissionList.size() > 0) {
            String[] arrayAskFor = new String[permissionList.size()];
            arrayAskFor = permissionList.toArray(arrayAskFor);
            ActivityCompat.requestPermissions(context, arrayAskFor, mRequestCode);
        } else {
            permissionResult.passPermission(true, permissions);
        }
    }

    public void onRequestPermissionsResult(Activity context, int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        List<String> denyList = new ArrayList<>();
        if (mRequestCode == requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    denyList.add(permissions[i]);
                }
            }
            if (!denyList.isEmpty()) {
                showSystemPermissionsSettingDialog(context, denyList);
            } else {
                mPermissionsResult.passPermission(false, permissions);
            }
        }

    }

    private void showSystemPermissionsSettingDialog(final Activity context, List<String> denyList) {
        final String mPackName = context.getPackageName();
        if (mPermissionDialog == null) {

            String denyName = "";
            for (String permission : denyList) {
                denyName = permission + " ";
            }

            mPermissionDialog = new AlertDialog.Builder(context)
                    .setMessage(denyName + "is denied, please grant it manually")
                    .setPositiveButton("Setting", (dialog, which) -> {
                        cancelPermissionDialog();
                        Uri packageURI = Uri.parse("package:" + mPackName);
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                        context.startActivity(intent);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        cancelPermissionDialog();
                        String[] denyArr = new String[denyList.size()];
                        denyArr = denyList.toArray(denyArr);
                        mPermissionsResult.denyPermission(denyArr);
                    })
                    .create();
        }
        mPermissionDialog.show();
    }

    private void cancelPermissionDialog() {
        if (mPermissionDialog != null) {
            mPermissionDialog.cancel();
            mPermissionDialog = null;
        }

    }

    public interface IPermissionResult {
        void passPermission(boolean history, String[] permissions);

        void denyPermission(String[] permissions);
    }

}
