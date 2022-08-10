package com.intel.gamepad.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.intel.gamepad.app.MyApp;

import org.webrtc.GlDrawerBg;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class ImageManager {

    private static ImageManager mInstance;
    final private Map<String, SoftReference<GlDrawerBg>> imageCache = new HashMap<>();

    private ImageManager() {
    }

    public static synchronized ImageManager getInstance() {
        if (mInstance == null) {
            mInstance = new ImageManager();
        }
        return mInstance;
    }

    public void clear() {
        imageCache.clear();
    }

    public GlDrawerBg pushImageById(final int src, final int orientationDegree) {
        Bitmap bitmap = BitmapFactory.decodeResource(MyApp.context.getResources(), src, null);
        if (orientationDegree != 0) {
            bitmap = adjustPhotoRotation(bitmap, 90);
        }
        GlDrawerBg drawImg = DecorateBg(bitmap);
        SoftReference<GlDrawerBg> softBitmap = new SoftReference<>(drawImg);
        imageCache.put(src + "", softBitmap);
        return drawImg;
    }

    public GlDrawerBg getBitmapById(final int src, final int orientationDegree) {
        SoftReference<GlDrawerBg> softDrawImg = imageCache.get(src + "");
        if (softDrawImg == null) {
            return pushImageById(src, orientationDegree);
        }
        return softDrawImg.get();
    }

    private GlDrawerBg DecorateBg(Bitmap bitmap) {
        byte[] buffer = new byte[bitmap.getWidth() * bitmap.getHeight() * 3];
        for (int y = 0; y < bitmap.getHeight(); y++)
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int pixel = bitmap.getPixel(x, y);
                buffer[(y * bitmap.getWidth() + x) * 3] = (byte) ((pixel >> 16) & 0xFF);
                buffer[(y * bitmap.getWidth() + x) * 3 + 1] = (byte) ((pixel >> 8) & 0xFF);
                buffer[(y * bitmap.getWidth() + x) * 3 + 2] = (byte) ((pixel) & 0xFF);
            }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 3);
        byteBuffer.put(buffer).position(0);

        return new GlDrawerBg(bitmap.getWidth(), bitmap.getHeight(), byteBuffer);
    }

    private Bitmap adjustPhotoRotation(Bitmap bitmap, final int orientationDegree) {
        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
        float targetX, targetY;
        if (orientationDegree == 90) {
            targetX = bitmap.getHeight();
            targetY = 0;
        } else {
            targetX = bitmap.getHeight();
            targetY = bitmap.getWidth();
        }

        final float[] values = new float[9];
        m.getValues(values);

        float x1 = values[Matrix.MTRANS_X];
        float y1 = values[Matrix.MTRANS_Y];

        m.postTranslate(targetX - x1, targetY - y1);

        int w = bitmap.getHeight();
        int h = bitmap.getWidth();
        Bitmap bm1 = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        Paint paint = new Paint();
        Canvas canvas = new Canvas(bm1);
        canvas.drawBitmap(bitmap, m, paint);
        return bm1;
    }

}
