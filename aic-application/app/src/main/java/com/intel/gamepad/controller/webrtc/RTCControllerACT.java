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

package com.intel.gamepad.controller.webrtc;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.intel.gamepad.R;
import com.intel.gamepad.activity.PlayGameRtcActivity;
import com.intel.gamepad.app.KeyConst;
import com.intel.gamepad.controller.impl.MouseMotionEventListener;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.controller.impl.PartitionEventListener;
import com.intel.gamepad.controller.view.Pad;
import com.intel.gamepad.controller.view.PadMouse;
import com.mycommonlibrary.utils.LogEx;

public class RTCControllerACT extends BaseController implements PartitionEventListener, MouseMotionEventListener {
    public static final String NAME = "ACT";
    public static final String DESC = "动作类游戏控制器";
    private ViewGroup layoutGamePad;
    private Button btnEsc = null;
    private Button btnSelect = null;
    private Button btnStart = null;
    private Pad padLeft = null;
    private PadMouse padMouseR = null;
    private PadMouse padMouseL = null;
    private Button btnL = null;
    private Button btnR = null;
    private Button btnX = null;
    private Button btnY = null;
    private Button btnA = null;
    private Button btnB = null;

    public RTCControllerACT(PlayGameRtcActivity act, Handler handler, DeviceSwitchListtener devSwitch) {
        super(act, handler, devSwitch);
    }

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return DESC;
    }

    public View getView() {
        if (this.layoutGamePad == null) initView();
        return layoutGamePad;
    }

    private void initView() {
        layoutGamePad = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.game_pad_xbox, null, false);
        addControllerView(layoutGamePad);
        initBackButton(layoutGamePad.findViewById(R.id.btnBack));

        btnEsc = layoutGamePad.findViewById(R.id.btnEsc);
        btnEsc.setOnTouchListener(this);
        //
        btnSelect = layoutGamePad.findViewById(R.id.btnSelect);
        btnSelect.setOnTouchListener(this);
        //
        btnStart = layoutGamePad.findViewById(R.id.btnStart);
        btnStart.setOnTouchListener(this);
        //
        btnL = layoutGamePad.findViewById(R.id.btnL);
        btnL.setOnTouchListener(this);
        //
        btnR = layoutGamePad.findViewById(R.id.btnR);
        btnR.setOnTouchListener(this);
        //
        padLeft = layoutGamePad.findViewById(R.id.padLeft);
        padLeft.setOnTouchListener(this);
        padLeft.setPartition(12);
        padLeft.setPartitionEventListener(this);
        padLeft.setDrawPartitionAll(false);

        btnX = layoutGamePad.findViewById(R.id.btnX);
        btnX.setOnTouchListener(this);

        btnY = layoutGamePad.findViewById(R.id.btnY);
        btnY.setOnTouchListener(this);

        btnA = layoutGamePad.findViewById(R.id.btnA);
        btnA.setOnTouchListener(this);

        btnB = layoutGamePad.findViewById(R.id.btnB);
        btnB.setOnTouchListener(this);
        // 鼠标方向盘
        padMouseR = layoutGamePad.findViewById(R.id.padMouse);
        padMouseR.setMouseMotionListener(this);
        padMouseR.setOnTouchListener(this);
        padMouseL = layoutGamePad.findViewById(R.id.padMouse2);
        padMouseL.setMouseMotionListener(this);
        padMouseL.setOnTouchListener(this);

        layoutGamePad.findViewById(R.id.ibtnShowKeyBoard).setOnClickListener(v -> devSwitch.showDeviceMenu());
    }

    private int lastX = -1;
    private int lastY = -1;

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
//        BaseController.lastTouchMillis = System.currentTimeMillis();
//        super.onTouch(v, evt);
        updateLastTouchEvent();
        int count = evt.getPointerCount(); // 屏幕的触点数
        int action = evt.getActionMasked();
        int x = (int) evt.getX();
        int y = (int) evt.getY();
        //
        if (v == btnL)
            return handleButtonTouch(action, KeyConst.VK_L);
        if (v == btnR)
            return handleButtonTouch(action, KeyConst.VK_R);
        if (v == btnX)
            return handleButtonTouch(action, KeyConst.VK_X);
        if (v == btnY)
            return handleButtonTouch(action, KeyConst.VK_Y);
        if (v == btnA)
            return handleButtonTouch(action, KeyConst.VK_A);
        if (v == btnB)
            return handleButtonTouch(action, KeyConst.VK_B);
        if (v == btnSelect)
            return handleButtonTouch(action, KeyConst.VK_SPACE);
        if (v == btnStart)
            return handleButtonTouch(action, KeyConst.VK_ENTER);
        if (v == btnEsc)
            return handleButtonTouch(action, KeyConst.VK_ESCAPE);
        if (v == padLeft) {
            ((Pad) v).onTouch(evt);
            return true;
        }
        if (v == padMouseR) {
            ((PadMouse) v).onTouch(evt);
            return true;
        }
        if (v == padMouseL) {
            ((PadMouse) v).onTouch(evt);
            return true;
        }
        // must be called last
        // XXX: not calling super.onTouch() because we have our own handler
        //return super.onTouch(v, evt);
        if (v == layoutGamePad) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_UP:
                    lastX = lastY = -1;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (lastX != -1 && lastY != -1) {
                        int dx = x - lastX;
                        int dy = y - lastY;
                        //controller.sendMouseMotion(x, y, dx, dy, 0, true);
                        onMouseMotion(v, x, y, dx, dy);
                    }
                    lastX = x;
                    lastY = y;
                    break;
            }
            return true;
        }

        return false;
    }

    private void emulateArrowKeys2(int action, int part) {
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
            this.sendKeyEvent(isPress, KeyConst.VK_UP);
        }
        if (myKeyDown) {
            this.sendKeyEvent(isPress, KeyConst.VK_DOWN);
        }
        if (myKeyLeft) {
            this.sendKeyEvent(isPress, KeyConst.VK_LEFT);
        }
        if (myKeyRight) {
            this.sendKeyEvent(isPress, KeyConst.VK_RIGHT);
        }
        LogEx.i(">>>>>" + myKeyUp + " " + myKeyDown + " " + myKeyLeft + " " + myKeyRight);
    }


    @Override
    public void onPartitionEvent(View v, int action, int part) {
        // Left: emulated arrow keys
        if (v == padLeft) {
            emulateArrowKeys2(action, part);
        }
    }

    @Override
    public void onMouseMotion(View v, int x, int y, int dx, int dy) {
        sendMouseMotionF(x, y, dx, dy);
    }

    @Override
    public void onMouseDown(View v, int x, int y) {

    }

    @Override
    public void onMouseUp(View v, int x, int y) {

    }
}
