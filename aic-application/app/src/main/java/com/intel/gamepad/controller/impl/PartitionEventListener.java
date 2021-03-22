package com.intel.gamepad.controller.impl;

import android.view.View;

public interface PartitionEventListener {
    public abstract void onPartitionEvent(View v, int action, int part);
}