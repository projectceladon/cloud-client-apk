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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import com.intel.gamepad.R;
import com.intel.gamepad.controller.impl.PartitionEventListener;
import com.intel.gamepad.controller.view.Pad;

public class GAControllerPSP extends GAController implements
        OnClickListener, PartitionEventListener {
    public static final String NAME = "PSP";
    private Button btnEsc = null;
    private Button btnBack = null;
    private Button btnSelect = null;
    private Button btnStart = null;
    private Pad padLeft = null;
    private Button btnL = null;
    private Button btnR = null;
    private ImageButton btnOval = null;    // oval
    private ImageButton btnCross = null;    // cross
    private ImageButton btnRect = null;    // rectangle
    private ImageButton btnTri = null;    // triangle

    public GAControllerPSP(Context c) {
        super(c);
    }

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Emulated PSP controller";
    }

    @Override
    public void onDimensionChange(int width, int height) {
        if (width < 0 || height < 0)
            return;
        // must be called first
        super.setMouseVisibility(false);
        super.onDimensionChange(width, height);

        View viewPad = LayoutInflater.from(getContext()).inflate(R.layout.game_pad_psp, null, false);
        placeView(viewPad, 0, 0, width, height);

        btnEsc = viewPad.findViewById(R.id.btnEsc);
        btnEsc.setOnTouchListener(this);
        //
        btnBack = viewPad.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(this);
        //
        btnSelect = viewPad.findViewById(R.id.btnSelect);
        btnSelect.setOnTouchListener(this);
        //
        btnStart = viewPad.findViewById(R.id.btnStart);
        btnStart.setOnTouchListener(this);
        //
        btnL = viewPad.findViewById(R.id.btnL);
        btnL.setOnTouchListener(this);
        //
        btnR = viewPad.findViewById(R.id.btnR);
        btnR.setOnTouchListener(this);
        //
        padLeft = viewPad.findViewById(R.id.padLeft);
        padLeft.setOnTouchListener(this);
        padLeft.setPartition(12);
        padLeft.setPartitionEventListener(this);
        padLeft.setDrawPartitionAll(false);
        // oval
        btnOval = viewPad.findViewById(R.id.ibtnOval);
        btnOval.setOnTouchListener(this);
        // cross
        btnCross = viewPad.findViewById(R.id.ibtnCross);
        btnCross.setOnTouchListener(this);
        // rectangle
        btnRect = viewPad.findViewById(R.id.ibtnRect);
        btnRect.setOnTouchListener(this);
        // triangle
        btnTri = viewPad.findViewById(R.id.ibtnTri);
        btnTri.setOnTouchListener(this);
    }

    private float lastX = -1;
    private float lastY = -1;
    private int lastButton = -1;

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        int count = evt.getPointerCount(); // 屏幕的触点数
        int action = evt.getActionMasked();
        float x = evt.getX();
        float y = evt.getY();
        //
        if (v == btnL)
            return handleButtonTouch(action, SDL2.Scancode.Q, SDL2.Keycode.q, 0, 0);
        if (v == btnR)
            return handleButtonTouch(action, SDL2.Scancode.W, SDL2.Keycode.w, 0, 0);
        if (v == btnOval)
            return handleButtonTouch(action, SDL2.Scancode.X, SDL2.Keycode.x, 0, 0);
        if (v == btnCross)
            return handleButtonTouch(action, SDL2.Scancode.Z, SDL2.Keycode.z, 0, 0);
        if (v == btnRect)
            return handleButtonTouch(action, SDL2.Scancode.A, SDL2.Keycode.a, 0, 0);
        if (v == btnTri)
            return handleButtonTouch(action, SDL2.Scancode.S, SDL2.Keycode.s, 0, 0);
        if (v == btnSelect)
            return handleButtonTouch(action, SDL2.Scancode.SPACE, SDL2.Keycode.SPACE, 0, 0);
        if (v == btnStart)
            return handleButtonTouch(action, SDL2.Scancode.RETURN, SDL2.Keycode.RETURN, 0, 0);
        if (v == btnEsc)
            return handleButtonTouch(action, SDL2.Scancode.ESCAPE, SDL2.Keycode.ESCAPE, 0, 0);
        if (v == padLeft) {
            if (((Pad) v).onTouch(evt)) ;
            return true;
        }
        // must be called last
        // XXX: not calling super.onTouch() because we have our own handler
        //return super.onTouch(v, evt);
        if (v == this.getPanel()) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    //case MotionEvent.ACTION_POINTER_DOWN:
                    if (count == 1) {
                        float dx = x - lastX;
                        float dy = y - lastY;
                        sendMouseMotion(x, y, dx, dy, 0, /*relative=*/false);
                        lastX = x;
                        lastY = y;
                        lastButton = SDL2.Button.LEFT;
                        sendMouseKey(true, SDL2.Button.LEFT, x, y);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    //case MotionEvent.ACTION_POINTER_UP:
                    if (count == 1 && lastButton != -1) {
                        sendMouseKey(false, SDL2.Button.LEFT, x, y);
                        lastButton = -1;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (count == 1) {
                        float dx = x - lastX;
                        float dy = y - lastY;
                        sendMouseMotion(x, y, dx, dy, 0, /*relative=*/false);
                        lastX = x;
                        lastY = y;
                    }
                    break;
            }
            return true;
        }
        return false;
    }

    private boolean keyLeft = false;
    private boolean keyRight = false;
    private boolean keyUp = false;
    private boolean keyDown = false;

//    private void emulateArrowKeys(int action, int part) {
//        boolean myKeyLeft, myKeyRight, myKeyUp, myKeyDown;
//        myKeyLeft = keyLeft;
//        myKeyRight = keyRight;
//        myKeyUp = keyUp;
//        myKeyDown = keyDown;
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//            case MotionEvent.ACTION_POINTER_DOWN:
//            case MotionEvent.ACTION_MOVE:
//                // partition mappings for keys:
//                // - up: 11, 12, 1, 2
//                // - right: 2, 3, 4, 5
//                // - down: 5, 6, 7, 8
//                // - left: 8, 9, 10, 11
//                switch (part) {
//                    case 0:
//                        myKeyUp = myKeyRight = myKeyDown = myKeyLeft = false;
//                        break;
//                    // single keys
//                    case 12:
//                    case 1:
//                        myKeyUp = true;
//                        myKeyRight = myKeyDown = myKeyLeft = false;
//                        break;
//                    case 3:
//                    case 4:
//                        myKeyRight = true;
//                        myKeyUp = myKeyDown = myKeyLeft = false;
//                        break;
//                    case 6:
//                    case 7:
//                        myKeyDown = true;
//                        myKeyUp = myKeyRight = myKeyLeft = false;
//                        break;
//                    case 9:
//                    case 10:
//                        myKeyLeft = true;
//                        myKeyUp = myKeyRight = myKeyDown = false;
//                        break;
//                    // hybrid keys
//                    case 2:
//                        myKeyUp = myKeyRight = true;
//                        myKeyDown = myKeyLeft = false;
//                        break;
//                    case 5:
//                        myKeyRight = myKeyDown = true;
//                        myKeyUp = myKeyLeft = false;
//                        break;
//                    case 8:
//                        myKeyDown = myKeyLeft = true;
//                        myKeyUp = myKeyRight = false;
//                        break;
//                    case 11:
//                        myKeyLeft = myKeyUp = true;
//                        myKeyRight = myKeyDown = false;
//                        break;
//                }
//                break;
//            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_POINTER_UP:
//                if (keyLeft)
//                    this.sendKeyEvent(false, SDL2.Scancode.LEFT, SDL2.Keycode.LEFT, 0, 0);
//                if (keyRight)
//                    this.sendKeyEvent(false, SDL2.Scancode.RIGHT, SDL2.Keycode.RIGHT, 0, 0);
//                if (keyUp)
//                    this.sendKeyEvent(false, SDL2.Scancode.UP, SDL2.Keycode.UP, 0, 0);
//                if (keyDown)
//                    this.sendKeyEvent(false, SDL2.Scancode.DOWN, SDL2.Keycode.DOWN, 0, 0);
//                myKeyUp = myKeyRight = myKeyDown = myKeyLeft = false;
//                break;
//        }
//        if (myKeyUp != keyUp) {
//            this.sendKeyEvent(myKeyUp, SDL2.Scancode.UP, SDL2.Keycode.UP, 0, 0);
//        }
//        if (myKeyDown != keyDown) {
//            this.sendKeyEvent(myKeyDown, SDL2.Scancode.DOWN, SDL2.Keycode.DOWN, 0, 0);
//        }
//        if (myKeyLeft != keyLeft) {
//            this.sendKeyEvent(myKeyLeft, SDL2.Scancode.LEFT, SDL2.Keycode.LEFT, 0, 0);
//        }
//        if (myKeyRight != keyRight) {
//            this.sendKeyEvent(myKeyRight, SDL2.Scancode.RIGHT, SDL2.Keycode.RIGHT, 0, 0);
//        }
//        keyUp = myKeyUp;
//        keyDown = myKeyDown;
//        keyLeft = myKeyLeft;
//        keyRight = myKeyRight;
//    }

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
            this.sendKeyEvent(isPress, SDL2.Scancode.UP, SDL2.Keycode.UP, 0, 0);
        }
        if (myKeyDown) {
            this.sendKeyEvent(isPress, SDL2.Scancode.DOWN, SDL2.Keycode.DOWN, 0, 0);
        }
        if (myKeyLeft ) {
            this.sendKeyEvent(isPress, SDL2.Scancode.LEFT, SDL2.Keycode.LEFT, 0, 0);
        }
        if (myKeyRight ) {
            this.sendKeyEvent(isPress, SDL2.Scancode.RIGHT, SDL2.Keycode.RIGHT, 0, 0);
        }
    }


    @Override
    public void onPartitionEvent(View v, int action, int part) {
//		String obj = "null";
//		if(v == padLeft)	obj = "padLeft";
//		if(v == padRight)	obj = "padRight";
//		Log.d("ga_log", String.format("[%s] partition event: action=%d, part=%d", obj, action, part));
        // Left: emulated arrow keys
        if (v == padLeft) {
            emulateArrowKeys2(action, part);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnBack) {
            onBackPress();
        }
    }

}
