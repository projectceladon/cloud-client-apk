package com.intel.gamepad.controller.impl;

import android.view.View;

public interface MouseMotionEventListener {
    void onMouseMotion(View v, int x, int y, int dx, int dy);

    void onMouseDown(View v, int x, int y);

    void onMouseUp(View v, int x, int y);
}