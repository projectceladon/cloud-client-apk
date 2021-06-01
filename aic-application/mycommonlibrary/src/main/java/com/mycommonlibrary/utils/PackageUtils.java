package com.mycommonlibrary.utils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Created by XuRenfang on 2019/2/18.
 * 应用程序的包信息工具类
 */
public class PackageUtils {

    /**
     * 返回当前应用的包信息
     */
    public static PackageInfo getPackageInfo(Context context) {
        PackageManager manager;
        PackageInfo info = null;
        manager = context.getPackageManager();
        try {
            info = manager.getPackageInfo(context.getPackageName(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
    }

    /**
     * 返回当前应用的版本号，版本号整型
     */
    public static int getVersionCode(Context context) {
        PackageInfo info = getPackageInfo(context);
        if(info != null) {
            return info.versionCode;
        } else {
            return 0;
        }
    }

    /**
     * 返回当前应用的版本号，版本号为字符串（主要给用户看）
     */
    public static String getVersionName(Context context) {
        PackageInfo info = getPackageInfo(context);
        if(info != null) {
            return info.versionName;
        } else {
            return null;
        }
    }

    /**
     * 返回当前应用的包名
     */
    public static String getPackageName(Context context) {
        PackageInfo info = getPackageInfo(context);
        if(info != null) {
            return info.packageName;
        } else {
            return null;
        }
    }

    /**
     * APP重启
     */
    public static void resetApp(Context ctx) {
        Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(getPackageName(ctx));
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(launchIntent);
    }

    /**
     * 安装APK文件
     * 注意：从7.0以上apk路径会包装进ContentValues并被本地保存，如果本地
     * 已有相同路径的记录时Uri会返回null，所以文件名最好加上随机数或毫秒数
     * 以防止出现ActivityNotFoundException异常
     */
    public static void installApk(Activity act, File apk) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //Android 7.0及以上
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(MediaStore.Images.Media.DATA, apk.getPath());
            Uri apkInst = act.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            intent.setDataAndType(apkInst, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
        }
        act.startActivity(intent);
    }

    /**
     * 安装APK文件2（挺麻烦的）
     * 在res下创建xml目录，创建file_paths.xml文件，内容：如下
     * <?xml version="1.0" encoding="utf-8"?>
     * <paths>
     * <root-path name="root" path="" />
     * <external-path name="external_storage_root" path="." />
     * <external-path name="external_storage_download" path="Download" />
     * </paths>
     * 在AndroidManifest.xml里注册
     * <provider
     * android:name="android.support.v4.content.FileProvider"
     * android:authorities="${applicationId}.fileprovider"
     * android:exported="false"
     * android:grantUriPermissions="true">
     * <meta-data
     * android:name="android.support.FILE_PROVIDER_PATHS"
     * android:resource="@xml/file_paths"/>
     * </provider>
     * 添加权限 <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
     */
    public static void installApp(Context ctx, File file) {
        if (null == file || !file.exists()) return;
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        ctx.startActivity(intent);
    }

}