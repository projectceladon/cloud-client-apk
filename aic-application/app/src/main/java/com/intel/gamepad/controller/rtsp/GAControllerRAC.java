
package com.intel.gamepad.controller.rtsp;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.intel.gamepad.R;
import com.intel.gamepad.controller.impl.MouseMotionEventListener;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.controller.rtsp.GAController;
import com.intel.gamepad.controller.rtsp.SDL2;
import com.mycommonlibrary.utils.LogEx;

public class GAControllerRAC extends GAController implements MouseMotionEventListener {
    public static final String NAME = "RAC";
    public static final String DESC = "赛车类游戏控制器";
    private Button btnEsc = null;
    private Button btnSelect = null;
    private Button btnStart = null;
    private Button btnL = null;
    private Button btnR = null;
    private ImageButton btnLeft = null;
    private ImageButton btnRight = null;
    private Button btnGas = null;
    private Button btnBrake = null;

    public GAControllerRAC(Context c, Handler handler, DeviceSwitchListtener devSwitch) {
        super(c, handler, devSwitch);
    }

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return DESC;
    }

    @Override
    public void onDimensionChange(int width, int height) {
        if (width < 0 || height < 0)
            return;
        // must be called first
        super.setMouseVisibility(false);
        super.onDimensionChange(width, height);

        View viewPad = LayoutInflater.from(getContext()).inflate(R.layout.game_pad_rac, null, false);
        placeView(viewPad, 0, 0, width, height);
        initBackButton(viewPad.findViewById(R.id.btnBack));
        initSwitchDeviceButton(viewPad.findViewById(R.id.ibtnShowKeyBoard));

        btnEsc = viewPad.findViewById(R.id.btnEsc);
        btnEsc.setOnTouchListener(this);
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

        btnLeft = viewPad.findViewById(R.id.btnLeft);
        btnLeft.setOnTouchListener(this);

        btnRight = viewPad.findViewById(R.id.btnRight);
        btnRight.setOnTouchListener(this);

        btnGas = viewPad.findViewById(R.id.btnGas);
        btnGas.setOnTouchListener(this);

        btnBrake = viewPad.findViewById(R.id.btnBrake);
        btnBrake.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        GAController.lastTouchMillis = System.currentTimeMillis();
        super.onTouch(v, evt);
        int count = evt.getPointerCount(); // 屏幕的触点数
        int action = evt.getActionMasked();
        int x = (int) evt.getX();
        int y = (int) evt.getY();
        //
        if (v == btnL)
            return handleButtonTouch(action, SDL2.Scancode.L, SDL2.Keycode.l, 0, 0);
        if (v == btnR)
            return handleButtonTouch(action, SDL2.Scancode.R, SDL2.Keycode.r, 0, 0);
        if (v == btnGas)
            return handleButtonTouch(action, SDL2.Scancode.A, SDL2.Keycode.a, 0, 0);
        if (v == btnBrake)
            return handleButtonTouch(action, SDL2.Scancode.B, SDL2.Keycode.b, 0, 0);
        if (v == btnSelect)
            return handleButtonTouch(action, SDL2.Scancode.SPACE, SDL2.Keycode.SPACE, 0, 0);
        if (v == btnStart)
            return handleButtonTouch(action, SDL2.Scancode.RETURN, SDL2.Keycode.RETURN, 0, 0);
        if (v == btnEsc)
            return handleButtonTouch(action, SDL2.Scancode.ESCAPE, SDL2.Keycode.ESCAPE, 0, 0);
        if (v == btnLeft)
            emulateArrowKeys2(action, 9);
        if (v == btnRight)
            emulateArrowKeys2(action, 3);
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

    @Override
    public void onMouseMotion(View v, int x, int y, int dx, int dy) {
        sendMouseMotion(x, y, dx, dy, 0, true);
    }

    @Override
    public void onMouseDown(View v, int x, int y) {

    }

    @Override
    public void onMouseUp(View v, int x, int y) {

    }
}
