package com.intel.gamepad.controller.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;

import com.intel.gamepad.R;
import com.intel.gamepad.controller.impl.MouseMotionEventListener;
import com.mycommonlibrary.utils.LogEx;
import com.mycommonlibrary.utils.MyBitmapUtils;

public class PadMouse extends View {
    private Paint paint = new Paint();
    private int currWidth = -1;
    private int currHeight = -1;
    private int currRadius = -1;
    private int centerX = -1;
    private int centerY = -1;
    private boolean touched = false;
    private float touchedX = -1;
    private float touchedY = -1;
    //
    private MouseMotionEventListener listener;
    //
    private int colorPad = Color.BLACK;
    private int colorPadLine = Color.WHITE;
    private int colorHotspot = Color.CYAN;
    private int colorText = Color.WHITE;
    private static final double radiusRatio = 1.0;
    private static final double centerPartSizeRatio = 0.35;
    private static final float hotspotRatio = (float) 0.4;

    public void setMouseMotionListener(MouseMotionEventListener listener) {
        this.listener = listener;
    }

    public PadMouse(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PadMouse(Context context) {
        super(context);
    }

    private int lastX = -1;
    private int lastY = -1;

    public boolean onTouch(MotionEvent evt) {
        int action = evt.getActionMasked();
        int x = (int) evt.getX();
        int y = (int) evt.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                touchedX = x;
                touchedY = y;
                touched = true;
                this.postInvalidate();
                listener.onMouseDown(this, x, y);
                break;
            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_POINTER_UP:
                touchedX = -1;
                touchedY = -1;
                touched = false;
                this.postInvalidate();
                lastX = lastY = -1;
                listener.onMouseUp(this, x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                touchedX = x;
                touchedY = y;
                this.postInvalidate();
                if (lastX != -1 && lastY != -1) {
                    int dx = x - lastX;
                    int dy = y - lastY;

                    listener.onMouseMotion(this, x, y, dx, dy);
                }
                lastX = x;
                lastY = y;
                break;
        }
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width = this.getWidth();
        int height = this.getHeight();
        int radius = (int) ((width < height ? width : height) / 2 * radiusRatio);
        // assume width == height!
        if (width != currWidth || height != currHeight) {
            centerX = width / 2;
            centerY = height / 2;
            currWidth = width;
            currHeight = height;
            currRadius = radius;
        }
        // draw big circle

        paint.setColor(colorPadLine);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(102);
        canvas.drawCircle(centerX, centerY, radius, paint);
        paint.setColor(colorPad);
        paint.setAlpha(51);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, radius - 1, paint);

        paint.setColor(colorPadLine);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(102);
        canvas.drawCircle(centerX, centerY, (int) ((centerPartSizeRatio + 0.01) * radius), paint);
        paint.setColor(colorPad);
        paint.setAlpha(51);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, (int) ((centerPartSizeRatio - 0.01) * radius), paint);
        //
        if (touched) {
            paint.setColor(colorHotspot);
            canvas.drawCircle(touchedX, touchedY, radius * hotspotRatio, paint);
        }
    }
}
