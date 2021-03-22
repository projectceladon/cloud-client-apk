package com.mycommonlibrary.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.mycommonlibrary.R;
import com.mycommonlibrary.utils.GalleryUtils;
import com.mycommonlibrary.utils.LogEx;
import com.mycommonlibrary.utils.MyBitmapUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * 拍照和调用图库
 * 最后时间：2017-09-30
 */
public class CameraGalleyActivity extends AppCompatActivity implements OnClickListener {
    private static final String CLZ = CameraGalleyActivity.class.getSimpleName();
    private Button btnCamera;
    private Button btnGallery;
    private Button btnCancel;
    private static final int REQUEST_CAMERA = 0x21;
    private static final int GALLERY_RESULT = 0x22;
    private static final int REQUEST_CROP = 0x23;
    private static final int CANCEL_RESULT = 0x24;
    public static final int REQUEST_READ_PERMISSION = 0x10;
    public static final int REQUEST_WRITE_PERMISSION = 0x11;
    public static final int REQUEST_CAMERA_PERMISSION = 0x12;
    public static final int REQUEST_CAMERA_CALLEY = 0x30;
    private boolean isCrop = false;
    // 此值不宜太大，太大易出错
    // 注意：Intent传递的数据不能超40K，不然会出现!!! FAILED BINDER TRANSACTION !!!
    private int outputX = 200;
    private int outputY = 200;
    // 裁切比例值，如果都是0的情况下为自由裁切
    private int aspectX = 0;
    private int aspectY = 0;
    // 用于存储图片文件的路径，此变量设为静态为了兼容某个特殊型号的三星手机
    private static Uri mFileUri;
    private String mImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camer_gallery);
        initViewController();
    }


    @Override
    protected void onStart() {
        isCrop = this.getIntent().getBooleanExtra("isCrop", false);
        outputX = this.getIntent().getIntExtra("outputX", outputX);
        outputY = this.getIntent().getIntExtra("outputY", outputY);
        aspectX = this.getIntent().getIntExtra("aspectX", 0);
        aspectY = this.getIntent().getIntExtra("aspectY", 0);
        super.onStart();
    }

    private void initViewController() {
        btnCamera = (Button) findViewById(R.id.btn_camer);
        btnCamera.setOnClickListener(this);
        btnGallery = (Button) findViewById(R.id.btn_gallery);
        btnGallery.setOnClickListener(this);
        btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btnCamera) {
            // 调用系统的拍照功能需要“写入”和“摄像头”权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if ((checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                        || (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
                    requestPermissions(new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA}, REQUEST_WRITE_PERMISSION);
                    return;
                }
            }
            callCamera();
        }
        if (v == btnGallery) {
            // 调用系统图库是需要“读取”权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_PERMISSION);
                    return;
                }
            }
            GalleryUtils.callGallery(this, GALLERY_RESULT);
        }

        if (v == btnCancel) {
            setResult(CANCEL_RESULT, null);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasWrite = false;
        boolean hasCamera = false;
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE)
                    && grantResults[i] == PackageManager.PERMISSION_GRANTED)
                GalleryUtils.callGallery(this, GALLERY_RESULT);
            if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && grantResults[i] == PackageManager.PERMISSION_GRANTED)
                hasWrite = true;
            if (permissions[i].equals(Manifest.permission.CAMERA)
                    && grantResults[i] == PackageManager.PERMISSION_GRANTED)
                hasCamera = true;
        }
        if (hasWrite && hasCamera)
            callCamera();
    }

    /**
     * CameraCalleyActivity的入口程序，不带剪切功能
     *
     * @param parentWrapper 需要传入一个Activity或Fragment对象
     * @param requestCode   传入一个返回码
     */
    public static void gotoCameraCalleyNoCrop(Object parentWrapper, int requestCode) {
        Intent intent = new Intent();
        intent.putExtra("isCrop", false);
        if (parentWrapper instanceof Activity) {
            Activity act = (Activity) parentWrapper;
            intent.setAction(act.getPackageName() + ".CameraGalleyActivity");
            act.startActivityForResult(intent, requestCode);
            act.overridePendingTransition(0, 0);
        } else {
            Fragment frg = (Fragment) parentWrapper;
            intent.setAction(frg.getContext().getPackageName() + ".CameraGalleyActivity");
            frg.startActivityForResult(intent, requestCode);
            frg.getActivity().overridePendingTransition(0, 0);
        }
    }

    /**
     * CameraCalleyActivity的入口程序，带有图像剪切功能
     *
     * @param parentWrapper 需要传入一个Activity或Fragment对象
     * @param requestCode   传入一个请求码
     * @param outputX       剪切后输出的宽，不宜过大，而且在某些手机上会失效
     * @param outputY       剪切后输出的高
     */
    public static void gotoCameraCalley(Object parentWrapper, int requestCode, int outputX, int outputY) {
        Intent intent = new Intent();
        intent.putExtra("isCrop", true);
        intent.putExtra("outputX", outputX);
        intent.putExtra("outputY", outputY);
        if (parentWrapper instanceof Activity) {
            Activity act = (Activity) parentWrapper;
            intent.setAction(act.getPackageName() + ".CameraGalleyActivity");
            act.startActivityForResult(intent, requestCode);
            act.overridePendingTransition(0, 0);
        } else {
            Fragment frg = (Fragment) parentWrapper;
            intent.setAction(frg.getContext().getPackageName() + ".CameraGalleyActivity");
            frg.startActivityForResult(intent, requestCode);
            frg.getActivity().overridePendingTransition(0, 0);
        }
    }

    public void callCamera() {
        // 设置拍照保存的路径
        File imagePath;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            imagePath = getExternalCacheDir();
        } else {
            imagePath = getCacheDir();
        }
        mImageFile = imagePath + File.separator + System.currentTimeMillis() + ".jpg";// 图像文件的本地路径
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 安卓7.0以后需要将文件路径包装成content://格式的Uri对象
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(MediaStore.Images.Media.DATA, mImageFile);
            mFileUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        } else
            mFileUri = Uri.fromFile(new File(mImageFile));
        // 申请调用摄像头的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
        }
        gotoCamera();// 跳转到系统的拍照界面
    }

    /**
     * 跳转到系统的拍照界面
     */
    private void gotoCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);
        intent.putExtra("return-data", false);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    /**
     * 跳转到栽剪界面
     *
     * @param uriCropImage 指定要栽剪的文件路径
     * @param outputX
     * @param outputY
     */
    private void gotoCropImage(Uri uriCropImage, int outputX, int outputY) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uriCropImage, "image/*");
        intent.putExtra("crop", "true");
        // aspect值用于定义裁剪的比例，为0是为自由定义
        intent.putExtra("aspectX", aspectX);
        intent.putExtra("aspectY", aspectY);
        // ouput为为裁剪后输出的长宽像素
        intent.putExtra("outputX", outputX);
        intent.putExtra("outputY", outputY);
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);
        intent.putExtra("return-data", true);
        intent.putExtra("outputFormat", CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true); // no face detection
        startActivityForResult(intent, REQUEST_CROP);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 图像在未剪裁时data为null，剪裁后data值为bitmap
        Log.i(CLZ + ".onActivityResult:", "requestCode=" + requestCode + " - resultCode=" + resultCode);
        Log.i(CLZ + ".onActivityResult:", "mFileUri=" + mFileUri + " data=" + data);
        if (data == null)
            data = new Intent();
        // 从拍照界面返回
        if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            if (isCrop) // 判断是否需要剪切
                gotoCropImage(mFileUri, outputX, outputY);
            else {
                data.putExtra("imageUrl", mImageFile);
                data.putExtra("bitmap", (Bundle) data.getExtras().getParcelable("data"));
                setResult(resultCode, data);
                finish();
            }
        }
        // 如果裁切成功data.getExtras().getParcelable("data")的值必然不为null，否则表示取消了裁切操作
        if (requestCode == REQUEST_CROP) {
            data.putExtra("imageUrl", mImageFile);
            data.putExtra("bitmap", (Bundle) data.getExtras().getParcelable("data"));
            // 将裁切到的位图对象再保存到本地文件
            Bitmap bmp = data.getExtras().getParcelable("data");
            if (bmp != null)
                MyBitmapUtils.saveBitmap2File(bmp, mImageFile);
            setResult(resultCode, data);
            finish();
        }
        // 从图库界面返回
        if (requestCode == GALLERY_RESULT && resultCode == Activity.RESULT_OK) {
            // 获取图片的本地路径
            String sourceFile = GalleryUtils.getPath(this, data);
            // 再将文件复制到当前项目指定的路径
            copyImageFile(new File(sourceFile));
            if (isCrop) {
                gotoCropImage(mFileUri, outputX, outputY);
            } else {
                data.putExtra("imageUrl", mImageFile);
                data.putExtra("bitmap", (Bundle) data.getExtras().getParcelable("data"));
                setResult(resultCode, data);
                LogEx.i(mImageFile + " " + data.getExtras().getParcelable("data"));
                finish();
            }
        }
    }

    private void copyImageFile(File sourceFile) {
        File imagePath = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                // imagePath = getExternalCacheDir();
                imagePath = getExternalFilesDir("images");
            } else {
                imagePath = getCacheDir();
            }
            // 根据最后一个点来截取文件的扩展名
            int dot = sourceFile.getPath().lastIndexOf(".") + 1;
            String extName = sourceFile.getPath().substring(dot);
            Log.i(CLZ, "图片扩展名：" + extName);

            this.mImageFile = imagePath + File.separator + System.currentTimeMillis() + "." + extName;
            Log.i(CLZ, "图片完整文件名：" + mImageFile);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 安卓7.0以后需要将文件路径包装成content://格式的Uri对象
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(MediaStore.Images.Media.DATA, mImageFile);
                mFileUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            } else
                mFileUri = Uri.fromFile(imagePath);
        }
        try {
            copyFile(sourceFile, new File(mImageFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 文件复制
     */
    private void copyFile(File sourceFile, File targetFile) throws IOException {
        // 新建文件输入流并对它进行缓冲
        FileInputStream input = new FileInputStream(sourceFile);
        BufferedInputStream inBuff = new BufferedInputStream(input);

        // 新建文件输出流并对它进行缓冲
        FileOutputStream output = new FileOutputStream(targetFile);
        BufferedOutputStream outBuff = new BufferedOutputStream(output);

        // 缓冲数组
        byte[] b = new byte[1024 * 5];
        int len;
        while ((len = inBuff.read(b)) != -1) {
            outBuff.write(b, 0, len);
        }
        // 刷新此缓冲的输出流
        outBuff.flush();
        // 关闭流
        inBuff.close();
        outBuff.close();
        output.close();
        input.close();
    }

}
