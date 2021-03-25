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

import com.intel.gamepad.controller.impl.PartitionEventListener;
import com.intel.gamepad.controller.rtsp.GAController;
import com.intel.gamepad.controller.rtsp.SDL2;
import com.intel.gamepad.controller.view.Pad;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class GAControllerPadABXY extends GAController implements
        OnClickListener, PartitionEventListener {
    public static final String NAME = "PadABXY";
    private Button buttonEsc = null;
    private Button buttonBack = null;
    private Button buttonSelect = null;
    private Button buttonStart = null;
    private Pad padLeft = null;
    private Pad padRight = null;

    public GAControllerPadABXY(Context context) {
        super(context);
    }

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Arrow keys and ABXY buttons";
    }

    @Override
    public void onDimensionChange(int width, int height) {
        int keyBtnWidth = width / 13;
        int keyBtnHeight = height / 9;
        int padSize = height * 2 / 5;
        // must be called first
        super.onDimensionChange(width, height);
        //
        buttonEsc = null;
        buttonEsc = new Button(getContext());
        buttonEsc.setTextSize(10);
        buttonEsc.setText("ESC");
        buttonEsc.setOnClickListener(this);
        placeView(buttonEsc, width - keyBtnWidth / 5 - keyBtnWidth, keyBtnHeight / 3, keyBtnWidth, keyBtnHeight);
        //
        buttonBack = null;
        buttonBack = new Button(getContext());
        buttonBack.setTextSize(10);
        buttonBack.setText("<<");
        buttonBack.setOnClickListener(this);
        placeView(buttonBack, keyBtnWidth / 5, keyBtnHeight / 3, keyBtnWidth, keyBtnHeight);
        //
        buttonSelect = null;
        buttonSelect = new Button(getContext());
        buttonSelect.setTextSize(10);
        buttonSelect.setText("SELECT");
        buttonSelect.setOnClickListener(this);
        placeView(buttonSelect, width / 2 - keyBtnWidth * 2, height - height / 30 - padSize / 3, keyBtnWidth * 2, keyBtnHeight);
        //
        buttonStart = null;
        buttonStart = new Button(getContext());
        buttonStart.setTextSize(10);
        buttonStart.setText("START");
        buttonStart.setOnClickListener(this);
        placeView(buttonStart, width / 2 + width / 30, height - height / 30 - padSize / 3, keyBtnWidth * 2, keyBtnHeight);
        //
        padLeft = null;
        padLeft = new Pad(getContext());
        padLeft.setAlpha((float) 0.5);
        padLeft.setOnTouchListener(this);
        padLeft.setPartition(12);
        padLeft.setPartitionEventListener(this);
        padLeft.setDrawPartitionAll(false);
        placeView(padLeft, width / 30, height - padSize - height / 30, padSize, padSize);
        //
        padRight = null;
        padRight = new Pad(getContext());
        padRight.setAlpha((float) 0.5);
        padRight.setOnTouchListener(this);
        padRight.setPartition(8);
        padRight.setPartitionEventListener(this);
        padRight.setDrawPartitionAll(false);
        padRight.setDrawPartitionLine(new int[]{1, 3, 5, 7});
        padRight.setDrawLabel(new int[]{3, 5, 1, 3, 5, 7, 7, 1}, new String[]{"A", "B", "X", "Y"});
        placeView(padRight, width - width / 30 - padSize, height - padSize - height / 30, padSize, padSize);
    }

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        int count = evt.getPointerCount();
        if (count == 1 && (v == padLeft || v == padRight)) {
            if (((Pad) v).onTouch(evt)) ;
            return true;
        }
        // must be called last
        return super.onTouch(v, evt);
    }

    private boolean keyLeft = false;
    private boolean keyRight = false;
    private boolean keyUp = false;
    private boolean keyDown = false;

    private int lastButton = -1;
    private int lastScan = -1, lastKey = -1;

    private void emulateABXZ(int action, int part) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //case MotionEvent.ACTION_POINTER_DOWN:
                switch (part) {
                    case 0:
                        lastButton = SDL2.Button.LEFT;
                        this.sendMouseKey(true, SDL2.Button.LEFT, getMouseX(), getMouseY());
                        break;
                    case 2:
                    case 3: // define 'B' button
                        lastScan = SDL2.Scancode.X;
                        lastKey = SDL2.Keycode.x;
                        break;
                    case 4:
                    case 5: // define 'A' button
                        lastScan = SDL2.Scancode.Z;
                        lastKey = SDL2.Keycode.z;
                        break;
                    case 6:
                    case 7: // define 'X' button
                        lastScan = SDL2.Scancode.A;
                        lastKey = SDL2.Keycode.a;
                        break;
                    case 1:
                    case 8: // define 'Y' button
                        lastScan = SDL2.Scancode.S;
                        lastKey = SDL2.Keycode.s;
                        break;
                }
                if (part >= 1 && part <= 8) {
                    this.sendKeyEvent(true, lastScan, lastKey, 0, 0);
                }
                break;
            case MotionEvent.ACTION_UP:
                //case MotionEvent.ACTION_POINTER_UP:
                if (lastButton != -1) {
                    sendMouseKey(false, lastButton, getMouseX(), getMouseY());
                    lastButton = -1;
                }
                if (lastScan != -1) {
                    sendKeyEvent(false, lastScan, lastKey, 0, 0);
                    lastKey = -1;
                    lastScan = -1;
                }
                break;
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
            emulateArrowKeys(action, part);
            return;
        }
        // Right: emulated ABXZ + mouse click
        if (v == padRight) {
            emulateABXZ(action, part);
            return;
        }
    }

    @Override
    public void onClick(View v) {
        if (v == buttonEsc) {
            sendKeyEvent(true, SDL2.Scancode.ESCAPE, SDL2.Keycode.ESCAPE, 0, 0);
            sendKeyEvent(false, SDL2.Scancode.ESCAPE, SDL2.Keycode.ESCAPE, 0, 0);
        } else if (v == buttonBack) {
            onBackPress();
        } else if (v == buttonSelect) {
            sendKeyEvent(true, SDL2.Scancode.SPACE, SDL2.Keycode.SPACE, 0, 0);
            sendKeyEvent(false, SDL2.Scancode.SPACE, SDL2.Keycode.SPACE, 0, 0);
        } else if (v == buttonStart) {
            sendKeyEvent(true, SDL2.Scancode.RETURN, SDL2.Keycode.RETURN, 0, 0);
            sendKeyEvent(false, SDL2.Scancode.RETURN, SDL2.Keycode.RETURN, 0, 0);
        }
    }
}
