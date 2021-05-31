package com.mycommonlibrary.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static android.util.Base64.encodeToString;

/**
 * Base64工具类
 * 作者：许仁方
 * 时间：2016-12-22
 */
public class Base64Utils {
    private static final String CHAR_CODE = "UTF-8";

    /**
     * 将指定的文件内容保存为BASE64格式的字符串
     *
     * @param path 文件路径
     */
    public static String fileToBase64(String path) {
        File file = new File(path);
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            byte[] buffer = new byte[5120];
            int len;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while ((len = inputStream.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            String str = new String(Base64.encode(bos.toByteArray(), Base64.NO_WRAP));
            inputStream.close();
            return str;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 将BASE64格式的字符串保存到指定的文件中
     */
    public static File base64ToFile(String base64, String path) {
        FileOutputStream outputStream = null;
        try {
            File file = new File(path);
            outputStream = new FileOutputStream(file);
            outputStream.write(Base64.decode(base64, Base64.NO_WRAP));
            outputStream.flush();
            outputStream.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;

    }

    /**
     * Bitmap转成Base64字符串
     *
     * @param quality 压缩率0到100，如何是上传图片的话建议50
     */
    public static String bitmapToBase64(@NonNull Bitmap bitmap, int quality) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);

            baos.flush();
            baos.close();

            byte[] bitmapBytes = baos.toByteArray();
            result = encodeToString(bitmapBytes, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * base64转为bitmap
     */
    public static Bitmap base64ToBitmap(String base64Data) {
        Bitmap bitmap = null;
        try {
            byte[] bitmapArray = Base64.decode(base64Data.split(",")[1], Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
        } catch (Exception e) {
            byte[] bitmapArray = Base64.decode(base64Data, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
        }
        return bitmap;
    }

    /**
     * 对字符串进行编码
     */
    public static String encode(String str) {
        return encodeToString(str.getBytes(), Base64.DEFAULT);
    }

    /**
     * 对字符串进行解码
     */
    public static String decode(String str) {
        try {
            return new String(Base64.decode(str, Base64.DEFAULT), CHAR_CODE);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

}