package com.mycommonlibrary.utils;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;
import android.widget.RemoteViews;

import androidx.collection.ArrayMap;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mycommonlibrary.R;

/**
 * 通知栏工具类
 */
public class NotificationHelper {
    private static Context context;
    private int notifyId = 1;
    private String channelId = "1";
    private ArrayMap<Integer, Notification> mapBuilder = new ArrayMap<>();
    private NotificationManagerCompat managerCompat;
    private NotificationManager manager;

    private NotificationHelper() {
    }

    public static NotificationHelper getInstance(Context context) {
        NotificationHelper.context = context;
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final NotificationHelper INSTANCE = new NotificationHelper();
    }

    public int getNotifyId() {
        return this.notifyId;
    }

    private void initManager() {
        manager = (NotificationManager) NotificationHelper.context.getSystemService(Context.NOTIFICATION_SERVICE);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "channel_name", importance);
            manager.createNotificationChannel(channel);
        }
    }

    private void initManagerCompat() {
        managerCompat = NotificationManagerCompat.from(NotificationHelper.context.getApplicationContext());
    }

    public void cancel(int notifyId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.cancel(notifyId);
        } else {
            managerCompat.cancel(notifyId);
        }
        mapBuilder.remove(notifyId);
    }

    public void cancelAll() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.cancelAll();
        } else {
            managerCompat.cancelAll();
        }
        mapBuilder.clear();
    }

    public NotificationManager getManager() {
        if (manager == null) initManager();
        return manager;
    }

    public NotificationManagerCompat getManagerCompat() {
        if (managerCompat == null) initManagerCompat();
        return managerCompat;
    }

    public NotificationCompat.Builder getBuilder() {
        return new NotificationCompat.Builder(context, channelId);
    }

    public static void cancel(Context context, int notifyId) {
        NotificationHelper.getInstance(context).cancel(notifyId);
    }

    public static void cancelAll(Context context) {
        NotificationHelper.getInstance(context).cancelAll();
    }

    /**
     * 创建一个简易的通知
     *
     * @param pendingIntent 如果没有需要跳的组件可以为null
     */
    public NotificationCompat.Builder buildSimple(int smallIconId,
                                                  String title,
                                                  String content,
                                                  PendingIntent pendingIntent) {
        return getBuilder()
                .setSmallIcon(smallIconId)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX) //设置为最高重要程度
                .setDefaults(NotificationCompat.DEFAULT_ALL);//设置音效为默认
    }

    public NotificationCompat.Builder buildView(RemoteViews view, int smallIconId) {
        return getBuilder()
                .setContent(view)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(smallIconId)
                .setOngoing(true);
    }

    /**
     * 创建一个多行文本的通知样式
     *
     * @param bigText 多行文本内容
     */
    public static NotificationCompat.Style buildBigTextStyle(String bigText, String title, String summary) {
        NotificationCompat.BigTextStyle bts = new NotificationCompat.BigTextStyle()
                .bigText(bigText);
        if (!TextUtils.isEmpty(title))
            bts.setBigContentTitle(title);
        if (!TextUtils.isEmpty(summary))
            bts.setSummaryText(summary);
        return bts;
    }

    /**
     * 创建一个图片格式的通知样式
     */
    public static NotificationCompat.Style buildBigPictureStyle(Bitmap bmp, String title, String summary) {
        NotificationCompat.BigPictureStyle bps = new NotificationCompat.BigPictureStyle()
                .bigPicture(bmp);
        if (!TextUtils.isEmpty(title))
            bps.setBigContentTitle(title);
        if (!TextUtils.isEmpty(summary))
            bps.setSummaryText(summary);
        return bps;
    }

    public static NotificationCompat.Style buildInboxStyle(String[] inbox, String title, String summary) {
        NotificationCompat.InboxStyle bis = new NotificationCompat.InboxStyle();
        for (String str : inbox)
            bis.addLine(str);
        if (!TextUtils.isEmpty(title))
            bis.setBigContentTitle(title);
        if (!TextUtils.isEmpty(summary))
            bis.setSummaryText(summary);
        return bis;
    }

    /**
     * 发布一条通知并返回该通知的ID号
     */
    public static int show(Context context, NotificationCompat.Builder builder) {
        NotificationHelper helper = NotificationHelper.getInstance(context);
        Notification notify = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            helper.getManager()
                    .notify(helper.notifyId, notify);
        } else {
            helper.getManagerCompat()
                    .notify(helper.notifyId, notify);
        }
        int id = helper.notifyId++;
        helper.mapBuilder.put(id, notify);
        return id;
    }

    /**
     * 刷新指定ID的通知
     */
    public static void update(Context context, int notifyId) {
        Notification notify = NotificationHelper.getInstance(context).mapBuilder.get(notifyId);
        NotificationHelper helper = NotificationHelper.getInstance(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            helper.getManager().notify(notifyId, notify);
        } else {
            helper.getManagerCompat().notify(notifyId, notify);
        }
    }

    /**
     * 设置通知栏的进度
     *
     * @param notifyId      需要更新进度的通知ID
     * @param progressResId 进度条控件ID
     * @param progress      进度（0-100）
     */
    public static void setProgress(Context context, int notifyId, int progressResId, int progress) {
        Notification notify = NotificationHelper.getInstance(context).mapBuilder.get(notifyId);
        notify.contentView.setProgressBar(progressResId, 100, progress, false);
        update(context, notifyId);
    }

    /**
     * 根据ID获取通知
     */
    public static Notification getNotification(Context context, int notifyId) {
        return NotificationHelper.getInstance(context).mapBuilder.get(notifyId);
    }

    /**
     * 创建一个通知记录被点击后启动的Activity
     *
     * @return
     */
    public static PendingIntent buildActivity(Context context, Class<? extends Activity> clz, int requestCode, int flags) {
        Intent intent = new Intent(context, clz);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    public static PendingIntent buildActivity(Context context, Class<? extends Activity> clz) {
        return buildActivity(context, clz, 0, 0);
    }

    /**
     * 创建一个通知栏记录被点击后启动的Service服务
     */
    public static PendingIntent buildService(Context context, Class<? extends Service> clz, int requestCode, int flags) {
        Intent intent = new Intent(context, clz);
        return PendingIntent.getService(context, requestCode, intent, flags);
    }

    public static PendingIntent buildService(Context context, Class<? extends Service> clz) {
        return buildService(context, clz, 0, 0);
    }

    /**
     * 创建一个通知记录被点击后可启动的广播
     */
    public static PendingIntent buildBroadcast(Context context, Class<? extends BroadcastReceiver> clz, int requestCode, int flags) {
        Intent intent = new Intent(context, clz);
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    public static PendingIntent buildBroadcast(Context context, Class<? extends BroadcastReceiver> clz) {
        return buildBroadcast(context, clz, 0, 0);
    }
}
