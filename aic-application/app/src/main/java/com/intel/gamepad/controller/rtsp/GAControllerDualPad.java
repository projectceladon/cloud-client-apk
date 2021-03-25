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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.intel.gamepad.controller.impl.PartitionEventListener;
import com.intel.gamepad.controller.view.Pad;

public class GAControllerDualPad extends GAController implements
        OnClickListener, PartitionEventListener {
    public static final String NAME = "DualPad";
    private Button buttonEsc = null;
    private Button buttonBack = null;
    private Pad padLeft = null;
    private Pad padRight = null;

    public GAControllerDualPad(Context context) {
        super(context);
    }

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Arrow keys and mouse buttons";
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
        padRight.setPartition(2);
        padRight.setPartitionEventListener(this);
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

    @Override
    public void onClick(View v) {
        if (v == buttonEsc) {
            sendKeyEvent(true, SDL2.Scancode.ESCAPE, 0x1b, 0, 0);
            sendKeyEvent(false, SDL2.Scancode.ESCAPE, 0x1b, 0, 0);
        } else if (v == buttonBack) {
            onBackPress();
        }
    }

    private boolean keyLeft = false;
    private boolean keyRight = false;
    private boolean keyUp = false;
    private boolean keyDown = false;
    private int mouseButton = -1;

    private void emulateMouseButtons(int action, int part) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //case MotionEvent.ACTION_POINTER_DOWN:
                if (part == 0 || part == 2)
                    mouseButton = SDL2.Button.LEFT;
                else
                    mouseButton = SDL2.Button.RIGHT;
                this.sendMouseKey(true, mouseButton, getMouseX(), getMouseY());
                break;
            case MotionEvent.ACTION_UP:
                //case MotionEvent.ACTION_POINTER_UP:
                if (mouseButton != -1) {
                    sendMouseKey(false, mouseButton, getMouseX(), getMouseY());
                    mouseButton = -1;
                }
                break;
        }
    }

    @Override
    public void onPartitionEvent(View v, int action, int part) {
        // Left: emulated arrow keys
        if (v == padLeft) {
            emulateArrowKeys(action, part);
            return;
        }
        // Right: emulated mouse buttons
        if (v == padRight) {
            emulateMouseButtons(action, part);
            return;
        }
    }
}
