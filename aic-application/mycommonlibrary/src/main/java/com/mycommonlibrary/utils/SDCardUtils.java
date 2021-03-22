package com.mycommonlibrary.utils;

import android.os.Environment;

import java.io.File;

/**
 * Created by XuRenfang on 2016/1/28.
 */
public class SDCardUtils {
    /**
     * 判断SD卡是否已加载
     *
     * @return
     */
    public static boolean isReady() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static String getState() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return "SD卡已挂载";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_UNMOUNTED))
            return "SD卡已卸载";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED))
            return "SD卡卸载并移除";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_BAD_REMOVAL))
            return "SD卡被强制取出或已损坏";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_CHECKING))
            return "SD卡状态扫描中";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_SHARED))
            return "SD卡挂载并作为U盘使用";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_NOFS))
            return "未支持的文件系统";
        return null;
    }

    /**
     * 获取SD卡的总容量
     *
     * @return
     */
    public static long getTotalSize() {
        if (isReady() == false)
            return -1;
        File path = Environment.getExternalStorageDirectory();
        return path.getTotalSpace();
    }

    /**
     * 获取SD卡的剩余容量
     *
     * @return
     */
    public static long getFreeSize() {
        if (isReady() == false)
            return -1;
        File path = Environment.getExternalStorageDirectory();
        return path.getFreeSpace();
    }

    /**
     * 获取SD卡的已用容量
     *
     * @return
     */
    public static long getUsedSize() {
        return getTotalSize() - getFreeSize();
    }
}
