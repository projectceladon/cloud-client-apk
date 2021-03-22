
package com.intel.gamepad.controller.rtsp;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.intel.gamepad.R;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.controller.impl.MouseMotionEventListener;
import com.intel.gamepad.controller.impl.PartitionEventListener;
import com.intel.gamepad.controller.view.Pad;
import com.mycommonlibrary.utils.LogEx;


public class GAControllerFPS extends GAController implements
        PartitionEventListener, MouseMotionEventListener {
    public static final String NAME = "FPS";
    public static final String DESC = "射击类型游戏控制器";
    private Button btnEsc = null;
    private Button btnSelect = null;
    private Button btnStart = null;
    private Pad padArrowKey = null;
    private ImageView btnShoot = null;
    private ImageView btnShootLeft = null;
    private CheckBox btnLock = null;
    private Button btnRest = null;
    private Button btnStand = null;
    private Button btnCrouch = null;
    private Button btnDrop = null;
    private View viewMouse = null;

    public GAControllerFPS(Context c, Handler handler, DeviceSwitchListtener devSwitch) {
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

        View viewPad = LayoutInflater.from(getContext()).inflate(R.layout.game_pad_fps, null, false);
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
        // 方向键
        padArrowKey = viewPad.findViewById(R.id.padLeft);
        padArrowKey.setOnTouchListener(this);
        padArrowKey.setPartition(12);
        padArrowKey.setPartitionEventListener(this);
        padArrowKey.setDrawPartitionAll(false);
        // 鼠标屏幕
        viewMouse = viewPad.findViewById(R.id.viewMouse);
        viewMouse.setOnTouchListener(this);

        btnShoot = viewPad.findViewById(R.id.btnShootRight);
        btnShoot.setOnTouchListener(this);
        btnShootLeft = viewPad.findViewById(R.id.btnShootLeft);
        btnShootLeft.setOnTouchListener(this);

        btnLock = viewPad.findViewById(R.id.btnLock);
        btnLock.setOnTouchListener(this);

        btnRest = viewPad.findViewById(R.id.btnKeyR);
        btnRest.setOnTouchListener(this);

        btnStand = viewPad.findViewById(R.id.btnStand);
        btnStand.setOnTouchListener(this);
    }

    private int lastX = -1;
    private int lastY = -1;

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        GAController.lastTouchMillis = System.currentTimeMillis();
        int count = evt.getPointerCount(); // 屏幕的触点数
        int action = evt.getActionMasked();
        LogEx.i(v.getClass().getName() + " " + action + " " + evt.getX() + " " + evt.getY());
        int x = (int) evt.getX();
        int y = (int) evt.getY();
        if (v == btnShoot || v == btnShootLeft)
            return handleButtonTouch(action, SDL2.Scancode.X, SDL2.Keycode.x, 0, 0);
        if (v == btnRest)
            return handleButtonTouch(action, SDL2.Scancode.Y, SDL2.Keycode.y, 0, 0);
        if (v == btnStand)
            return handleButtonTouch(action, SDL2.Scancode.A, SDL2.Keycode.a, 0, 0);
        if (v == btnCrouch)
            return handleButtonTouch(action, SDL2.Scancode.B, SDL2.Keycode.b, 0, 0);
        if (v == btnDrop)
            return handleButtonTouch(action, SDL2.Scancode.D, SDL2.Keycode.d, 0, 0);
        if (v == btnSelect)
            return handleButtonTouch(action, SDL2.Scancode.SPACE, SDL2.Keycode.SPACE, 0, 0);
        if (v == btnStart)
            return handleButtonTouch(action, SDL2.Scancode.RETURN, SDL2.Keycode.RETURN, 0, 0);
        if (v == btnEsc)
            return handleButtonTouch(action, SDL2.Scancode.ESCAPE, SDL2.Keycode.ESCAPE, 0, 0);

        if (v == btnLock) {
            sendKeyEvent(btnLock.isChecked(), SDL2.Scancode.O, SDL2.Keycode.o, 0, 0);
            return true;
        }
        // must be called last
        //return super.onTouch(v, evt);
        if (v == viewMouse) {
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
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
        if (v == padArrowKey) {
            return ((Pad) v).onTouch(evt);
        }
        return false;
    }


    @Override
    public void onPartitionEvent(View v, int action, int part) {
        if (v == padArrowKey) {
            this.emulateWASDKeys(action, part);
        }
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
