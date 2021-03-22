package com.mycommonlibrary.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 短信读取
 * 注意设置权限[添加到AndroidMainfest.xml]
 * <uses-permission android:name="android.permission.READ_SMS" />
 * 2015/12/9.
 */
public class SmsUtils {
    private static final String TAG = "SmsUtils";
    public static final String SMS_URI_ALL = "content://sms/";//所有短信
    public static final String SMS_URI_INBOX = "content://sms/inbox";//收件箱
    public static final String SMS_URI_SEND = "content://sms/sent";//已发送
    public static final String SMS_URI_DRAFT = "content://sms/draft";//草稿
    public static final String SMS_URI_OUTBOX = "content://sms/outbox";//发件箱
    public static final String SMS_URI_FAILED = "content://sms/failed";//发送失败
    public static final String SMS_URI_QUEUED = "content://sms/queued";//待发送列表

    /**
     * 获取指定uri的短信集合
     *
     * @param context
     * @param smsUri  值为null时接收所有的短信
     * @return
     */
    public static List<SmsInfo> getMessages(Context context, String smsUri) {
        List<SmsInfo> listSms = new ArrayList<>();
        try {
            ContentResolver cr = context.getContentResolver();
            String[] projection = new String[]{"_id", "address", "person",
                    "body", "date", "type"};
            Uri uri = Uri.parse(smsUri == null ? SMS_URI_ALL : smsUri);
            Cursor cur = cr.query(uri, projection, null, null, "date desc");

            if (cur.moveToFirst()) {
                String name;
                String phoneNumber;
                String smsbody;
                String date;
                String type;

                int nameColumn = cur.getColumnIndex("person");
                int phoneNumberColumn = cur.getColumnIndex("address");
                int smsbodyColumn = cur.getColumnIndex("body");
                int dateColumn = cur.getColumnIndex("date");
                int typeColumn = cur.getColumnIndex("type");

                do {
                    name = cur.getString(nameColumn);
                    phoneNumber = cur.getString(phoneNumberColumn);
                    smsbody = cur.getString(smsbodyColumn);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    Date d = new Date(Long.parseLong(cur.getString(dateColumn)));
                    date = dateFormat.format(d);

                    int typeId = cur.getInt(typeColumn);
                    if (typeId == 1) {
                        type = "接收";
                    } else if (typeId == 2) {
                        type = "发送";
                    } else {
                        type = "";
                    }
                    if (smsbody == null) smsbody = "";
                    SmsInfo item = new SmsInfo();
                    item.setName(name);
                    item.setPhone(phoneNumber);
                    item.setSmsBody(smsbody);
                    item.setDate(date);
                    item.setType(type);
                    listSms.add(item);
                } while (cur.moveToNext());
            } else {
                listSms = null;
            }
        } catch (SQLiteException ex) {
            Log.d(TAG, ex.getMessage());
        }
        return listSms;
    }

    public static class SmsInfo {
        private String name;
        private String phone;
        private String smsBody;
        private String date;
        private String type;

        @Override
        public String toString() {
            return "SmsInfo{" +
                    "name='" + name + '\'' +
                    ", phone='" + phone + '\'' +
                    ", smsBody='" + smsBody + '\'' +
                    ", date='" + date + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SmsInfo sMsInfo = (SmsInfo) o;

            if (name != null ? !name.equals(sMsInfo.name) : sMsInfo.name != null) return false;
            if (phone != null ? !phone.equals(sMsInfo.phone) : sMsInfo.phone != null) return false;
            if (smsBody != null ? !smsBody.equals(sMsInfo.smsBody) : sMsInfo.smsBody != null)
                return false;
            if (date != null ? !date.equals(sMsInfo.date) : sMsInfo.date != null) return false;
            return !(type != null ? !type.equals(sMsInfo.type) : sMsInfo.type != null);

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (phone != null ? phone.hashCode() : 0);
            result = 31 * result + (smsBody != null ? smsBody.hashCode() : 0);
            result = 31 * result + (date != null ? date.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getSmsBody() {
            return smsBody;
        }

        public void setSmsBody(String smsBody) {
            this.smsBody = smsBody;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
