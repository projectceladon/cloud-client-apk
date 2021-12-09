package com.intel.gamepad.controller.impl;

import android.widget.CheckBox;

public interface DeviceSwitchListtener {
    void switchKeyBoard();

    void switchMapperPad();

    void switchGamePad();

    void showDeviceMenu();

    void switchAlpha(CheckBox btnDevice, boolean state);
}