/*
 * Copyright (c) 2013 Chun-Ying Huang
 *
 * This file is part of GamingAnywhere (GA).
 *
 * GA is free software; you can redistribute it and/or modify it
 * under the terms of the 3-clause BSD License as published by the
 * Free Software Foundation: http://directory.fsf.org/wiki/License:BSD_3Clause
 *
 * GA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the 3-clause BSD License along with GA;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.intel.gamepad.controller.rtsp;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;

import com.intel.gamepad.R;
import com.intel.gamepad.app.AppConst;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.mycommonlibrary.utils.DensityUtils;
import com.mycommonlibrary.utils.LogEx;

import org.gaminganywhere.gaclient.GAClient;

import java.lang.ref.WeakReference;

public class GAController implements OnTouchListener {
    private Context context;
    private GAClient client = null;
    private int viewWidth = 0;
    private int viewHeight = 0;
    private float mappedX = (float) -1.0;
    private float mappedY = (float) -1.0;
    private float mappedDeltaX = (float) -1.0;
    private float mappedDeltaY = (float) -1.0;

    private RelativeLayout relativeLayout;
    private ImageView panel = null;
    private ImageView cursor = null;
    private boolean showMouse = true;
    private boolean enableTouchClick = true;
    private int mouseX = -1;
    private int mouseY = -1;

    private final long clickDetectionTime = 100;    /* in ms */
    private final float clickDetectionDist = 81;    /* in pixel^2 */

    protected DeviceSwitchListtener devSwitch;
    private WeakReference<Handler> refHandler;
    public static long lastTouchMillis = 0L;

    public GAController(Context context) {
        this.context = context.getApplicationContext();
        relativeLayout = new RelativeLayout(getContext());
    }

    public GAController(Context context, Handler handler) {
        this(context);
        this.refHandler = new WeakReference<>(handler);
        lastTouchMillis = System.currentTimeMillis();
        Message msg = Message.obtain();
        msg.what = AppConst.MSG_SHOW_CONTROLLER;
        this.refHandler.get().sendMessage(msg);
    }

    public GAController(Context context, Handler handler, DeviceSwitchListtener devSwitch) {
        this(context, handler);
        this.devSwitch = devSwitch;
    }

    protected String getName() {
        return null;
    }

    public String getDescription() {
        return null;
    }

    public Context getContext() {
        return this.context;
    }

    public ImageView getPanel() {
        return panel;
    }

    public void onBackPress() {
        Message msg = Message.obtain();
        msg.what = AppConst.MSG_QUIT;
        msg.arg1 = AppConst.EXIT_NORMAL;
        refHandler.get().sendMessage(msg);
    }

    /**
     * 初始化返回按钮
     */
    protected void initBackButton(View btnBack) {
        if (btnBack == null) return;
        btnBack.setOnClickListener(v -> onBackPress());
    }

    /**
     * 初始化设备切换按钮，点击后会弹界面选择可切换的模拟手柄或键盘
     */
    protected void initSwitchDeviceButton(View btnDevice) {
        if (btnDevice == null) return;
        btnDevice.setOnClickListener(v -> devSwitch.showDeviceMenu());
    }

    /**
     * 将WASD作为方向键时的事件响应
     */
    protected void emulateWASDKeys(int action, int part) {
        boolean onKeyLeft, onKeyRight, onKeyUp, onKeyDown;
        onKeyUp = onKeyRight = onKeyDown = onKeyLeft = false;
        boolean isPress = false;
        // 根据方向盘的分区号判断需要响应的方向键
        switch (part) {
            case 0:
                onKeyUp = onKeyRight = onKeyDown = onKeyLeft = false;
                break;
            case 12:
            case 1:
                onKeyUp = true;
                onKeyRight = onKeyDown = onKeyLeft = false;
                break;
            case 3:
            case 4:
                onKeyRight = true;
                onKeyUp = onKeyDown = onKeyLeft = false;
                break;
            case 6:
            case 7:
                onKeyDown = true;
                onKeyUp = onKeyRight = onKeyLeft = false;
                break;
            case 9:
            case 10:
                onKeyLeft = true;
                onKeyUp = onKeyRight = onKeyDown = false;
                break;
            case 2:
                onKeyUp = onKeyRight = true;
                onKeyDown = onKeyLeft = false;
                break;
            case 5:
                onKeyRight = onKeyDown = true;
                onKeyUp = onKeyLeft = false;
                break;
            case 8:
                onKeyDown = onKeyLeft = true;
                onKeyUp = onKeyRight = false;
                break;
            case 11:
                onKeyLeft = onKeyUp = true;
                onKeyRight = onKeyDown = false;
                break;
        }
        // 根据action判断是按键是按下还是抬起
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
                isPress = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                isPress = false;
                break;
        }
        // 将四个方向键置为抬起（防止某个方向键一直处于按钮状态）
        this.sendKeyEvent(false, SDL2.Scancode.W, SDL2.Keycode.w, 0, 0);
        this.sendKeyEvent(false, SDL2.Scancode.S, SDL2.Keycode.s, 0, 0);
        this.sendKeyEvent(false, SDL2.Scancode.A, SDL2.Keycode.a, 0, 0);
        this.sendKeyEvent(false, SDL2.Scancode.D, SDL2.Keycode.d, 0, 0);
        // 根据具体方向设置键的按下和抬起状态
        if (onKeyUp) {
            this.sendKeyEvent(isPress, SDL2.Scancode.W, SDL2.Keycode.w, 0, 0);
        }
        if (onKeyDown) {
            this.sendKeyEvent(isPress, SDL2.Scancode.S, SDL2.Keycode.s, 0, 0);
        }
        if (onKeyLeft) {
            this.sendKeyEvent(isPress, SDL2.Scancode.A, SDL2.Keycode.a, 0, 0);
        }
        if (onKeyRight) {
            this.sendKeyEvent(isPress, SDL2.Scancode.D, SDL2.Keycode.d, 0, 0);
        }
        LogEx.i(">>>>>u=" + onKeyUp + " d=" + onKeyDown + " l=" + onKeyLeft + " r=" + onKeyRight + " press=" + isPress);
    }

    /**
     * 上下左右方向键的事件响应
     */
    protected void emulateArrowKeys(int action, int part) {
        boolean myKeyLeft, myKeyRight, myKeyUp, myKeyDown;
        myKeyUp = myKeyRight = myKeyDown = myKeyLeft = false;
        boolean isPress = false;
        // 根据方向盘的分区号判断需要响应的方向键
        switch (part) {
            case 0:
                myKeyUp = myKeyRight = myKeyDown = myKeyLeft = false;
                break;
            case 12:
            case 1:
                myKeyUp = true;
                myKeyRight = myKeyDown = myKeyLeft = false;
                break;
            case 3:
            case 4:
                myKeyRight = true;
                myKeyUp = myKeyDown = myKeyLeft = false;
                break;
            case 6:
            case 7:
                myKeyDown = true;
                myKeyUp = myKeyRight = myKeyLeft = false;
                break;
            case 9:
            case 10:
                myKeyLeft = true;
                myKeyUp = myKeyRight = myKeyDown = false;
                break;
            // hybrid keys
            case 2:
                myKeyUp = myKeyRight = true;
                myKeyDown = myKeyLeft = false;
                break;
            case 5:
                myKeyRight = myKeyDown = true;
                myKeyUp = myKeyLeft = false;
                break;
            case 8:
                myKeyDown = myKeyLeft = true;
                myKeyUp = myKeyRight = false;
                break;
            case 11:
                myKeyLeft = myKeyUp = true;
                myKeyRight = myKeyDown = false;
                break;
        }
        // 根据action判断是按键是按下还是抬起
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
                isPress = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                isPress = false;
                break;
        }
        if (myKeyUp) {
            this.sendKeyEvent(isPress, SDL2.Scancode.UP, SDL2.Keycode.UP, 0, 0);
        }
        if (myKeyDown) {
            this.sendKeyEvent(isPress, SDL2.Scancode.DOWN, SDL2.Keycode.DOWN, 0, 0);
        }
        if (myKeyLeft) {
            this.sendKeyEvent(isPress, SDL2.Scancode.LEFT, SDL2.Keycode.LEFT, 0, 0);
        }
        if (myKeyRight) {
            this.sendKeyEvent(isPress, SDL2.Scancode.RIGHT, SDL2.Keycode.RIGHT, 0, 0);
        }
        LogEx.i(">>>>>" + myKeyUp + " " + myKeyDown + " " + myKeyLeft + " " + myKeyRight);
    }

    public void setClient(GAClient client) {
        this.client = client;
    }

    public View getView() {
        return relativeLayout;
    }

    /**
     * 分辨率更改后对手柄按键重新布局，此方向由setViewDimension调用，在子类中需要重写
     */
    public void onDimensionChange(int width, int height) {
        relativeLayout.removeAllViews();
        //
        mouseX = width / 2;
        mouseY = height / 2;
        // panel - the lowest UI, note the Z-order
        panel = null;
        panel = new ImageView(getContext());
        panel.setAlpha((float) 0);    // fully transparent
        panel.setOnTouchListener(this);
        placeView(panel, 0, 0, width, height);
        // cursor
        cursor = null;
        cursor = new ImageView(getContext());
        cursor.setImageResource(R.drawable.icon_mouse);
        placeView(cursor, width / 2, height / 2, DensityUtils.dp2px(20), DensityUtils.dp2px(20));
        if (showMouse)
            cursor.setVisibility(View.VISIBLE);
        else
            cursor.setVisibility(View.INVISIBLE);
        // move mouse to its correct position
        sendMouseMotion(mouseX, mouseY, 0, 0, 0, false);
    }

    public void setViewDimension(int width, int height) {
        viewWidth = width;
        viewHeight = height;
        Log.d("ga_log", String.format("controller: view dimension = %dx%d",
                viewWidth, viewHeight));
        onDimensionChange(width, height);
    }

    public int getViewWidth() {
        return viewWidth;
    }

    public int getViewHeight() {
        return viewHeight;
    }

    private boolean mapCoordinate(float x, float y) {
        return mapCoordinate(x, y, (float) 0.0, (float) 0.0);
    }

    private boolean mapCoordinate(float x, float y, float dx, float dy) {
        int pxsize, pysize;
        //
        if (client == null)
            return false;
        if (viewWidth <= 0 || viewHeight <= 0)
            return false;
        // map coordinates
        pxsize = client.getScreenWidth();
        pysize = client.getScreenHeight();
        if (pxsize <= 0 || pysize <= 0)
            return false;
        mappedX = x * pxsize / viewWidth;
        mappedY = y * pysize / viewHeight;
        mappedDeltaX = dx * pxsize / viewWidth;
        mappedDeltaY = dy * pysize / viewHeight;
        return true;
    }

    protected void moveView(View v, int left, int top, int width, int height) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.leftMargin = left;
        params.topMargin = top;
        v.setLayoutParams(params);
    }

    protected void placeView(View v, int left, int top, int width, int height) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.leftMargin = left;
        params.topMargin = top;
        relativeLayout.addView(v, params);
    }

    protected Button newButton(String label, int left, int top, int width, int height) {
        Button b = new Button(getContext());
        //b.setAlpha((float) 0.5);
        b.setBackgroundResource(R.drawable.btn_pad_rect_selector);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setTextSize(10);
        b.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        b.setText(label);
        placeView(b, left, top, width, height);
        return b;
    }

    protected ImageButton newImageButton(int left, int top, int width, int height) {
        ImageButton ibtn = new ImageButton(getContext());
        //ShapeDrawable s = new ShapeDrawable();
        //s.setShape(new OvalShape());
        //b.setBackground(s);
        //b.setAlpha((float) 0.5);
        ibtn.setScaleType(ImageView.ScaleType.CENTER);
        ibtn.setBackgroundResource(R.drawable.btn_pad_rect_selector);
        placeView(ibtn, left, top, width, height);
        return ibtn;
    }

    public boolean handleButtonTouch(int action, int scancode, int keycode, int mod, int unicode) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                sendKeyEvent(true, scancode, keycode, mod, unicode);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                sendKeyEvent(false, scancode, keycode, mod, unicode);
                break;
        }
        return true;
    }

    public void setMouseVisibility(boolean visible) {
        showMouse = visible;
        if (cursor == null)
            return;
        if (visible)
            cursor.setVisibility(View.VISIBLE);
        else
            cursor.setVisibility(View.INVISIBLE);
    }

    public void setEnableTouchClick(boolean enable) {
        enableTouchClick = enable;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public void setMouseX(int mouseX) {
        this.mouseX = mouseX;
    }

    public void setMouseY(int mouseY) {
        this.mouseY = mouseY;
    }

    protected void moveMouse(float dx, float dy) {
        mouseX += dx;
        mouseY += dy;
        if (mouseX < 0)
            mouseX = 0;
        if (mouseY < 0)
            mouseY = 0;
        if (mouseX >= getViewWidth())
            mouseX = getViewWidth() - 1;
        if (mouseY >= getViewHeight())
            mouseY = getViewHeight() - 1;
        return;
    }

    protected void drawCursor(int x, int y) {
        if (cursor == null || showMouse == false)
            return;
        cursor.setVisibility(View.VISIBLE);
        moveView(cursor, x, y, DensityUtils.dp2px(20), DensityUtils.dp2px(20));
    }

    public void sendKeyEvent(boolean pressed, int scancode, int sym, int mod, int unicode) {
        if (client == null) return;
        client.sendKeyEvent(pressed, scancode, sym, mod, unicode);
    }

    public void sendMouseKey(boolean pressed, int button, float x, float y) {
        if (!mapCoordinate(x, y)) return;
        client.sendMouseKey(pressed, button, (int) mappedX, (int) mappedY);
    }

    public void sendMouseMotion(float x, float y, float xrel, float yrel, int state, boolean relative) {
        if (!mapCoordinate(x, y, xrel, yrel))
            return;
        client.sendMouseMotion((int) mappedX, (int) mappedY,
                (int) mappedDeltaX, (int) mappedDeltaY, state, relative);
    }

    public void sendMouseWheel(float dx, float dy) {
        if (!mapCoordinate(dx, dy)) return;
        client.sendMouseWheel((int) mappedX, (int) mappedY);
    }

    private float lastX = (float) -1.0;
    private float lastY = (float) -1.0;
    private float initX = (float) -1.0;
    private float initY = (float) -1.0;
    private long lastTouchTime = -1;

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        int count = evt.getPointerCount();
        int action = evt.getActionMasked();
        float x = evt.getX();
        float y = evt.getY();
//		Log.d("ga_log", String.format("onTouch[panel]: count=%d, action=%d, x=%f, y=%f",
//				count, action, x, y));
        // touch on the panel
        if (v == panel) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    //case MotionEvent.ACTION_POINTER_DOWN:
                    if (count == 1) {
                        initX = lastX = x;
                        initY = lastY = y;
                        lastTouchTime = System.currentTimeMillis();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    //case MotionEvent.ACTION_POINTER_UP:
                    if (count == 1) {
                        long timeOffset = System.currentTimeMillis() - lastTouchTime;
                        float distOffset = (x - initX) * (x - initX) + (y - initY) * (y - initY);
                        if (enableTouchClick
                                && timeOffset < clickDetectionTime
                                && distOffset < clickDetectionDist) {
                            sendMouseKey(true, SDL2.Button.LEFT, getMouseX(), getMouseY());
                            sendMouseKey(false, SDL2.Button.LEFT, getMouseX(), getMouseY());
                        }
                        lastX = -1;
                        lastY = -1;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (count == 1) {
                        float dx = x - lastX;
                        float dy = y - lastY;
                        moveMouse(dx, dy);
                        sendMouseMotion(mouseX, mouseY, dx, dy, 0, /*relative=*/false);
                        drawCursor((int) mouseX, (int) mouseY);
                        lastX = x;
                        lastY = y;
                    }
                    break;
            }
            return true;
        }
        return false;
    }

}
