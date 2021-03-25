
package com.intel.gamepad.controller.rtsp;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.intel.gamepad.R;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.controller.impl.MouseMotionEventListener;
import com.intel.gamepad.controller.impl.PartitionEventListener;
import com.intel.gamepad.controller.view.Pad;
import com.intel.gamepad.controller.view.PadMouse;

public class GAControllerXBox extends GAController implements PartitionEventListener, MouseMotionEventListener {
    public static final String NAME = "XBOX";
    public static final String DESC = "XBox游戏机手柄";
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

    public GAControllerXBox(Context c, Handler handler, DeviceSwitchListtener devSwitch) {
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

        View viewPad = LayoutInflater.from(getContext()).inflate(R.layout.game_pad_xbox, null, false);
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
        //
        padLeft = viewPad.findViewById(R.id.padLeft);
        padLeft.setOnTouchListener(this);
        padLeft.setPartition(12);
        padLeft.setPartitionEventListener(this);
        padLeft.setDrawPartitionAll(false);

        btnX = viewPad.findViewById(R.id.btnX);
        btnX.setOnTouchListener(this);

        btnY = viewPad.findViewById(R.id.btnY);
        btnY.setOnTouchListener(this);

        btnA = viewPad.findViewById(R.id.btnA);
        btnA.setOnTouchListener(this);

        btnB = viewPad.findViewById(R.id.btnB);
        btnB.setOnTouchListener(this);
        // 鼠标方向盘
        padMouseR = viewPad.findViewById(R.id.padMouse);
        padMouseR.setMouseMotionListener(this);
        padMouseR.setOnTouchListener(this);
        padMouseL = viewPad.findViewById(R.id.padMouse2);
        padMouseL.setMouseMotionListener(this);
        padMouseL.setOnTouchListener(this);
    }

    private int lastX = -1;
    private int lastY = -1;

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        GAController.lastTouchMillis = System.currentTimeMillis();
        super.onTouch(v, evt);
        int action = evt.getActionMasked();
        int x = (int) evt.getX();
        int y = (int) evt.getY();
        //
        if (v == btnL)
            return handleButtonTouch(action, SDL2.Scancode.L, SDL2.Keycode.l, 0, 0);
        if (v == btnR)
            return handleButtonTouch(action, SDL2.Scancode.R, SDL2.Keycode.r, 0, 0);
        if (v == btnX)
            return handleButtonTouch(action, SDL2.Scancode.X, SDL2.Keycode.x, 0, 0);
        if (v == btnY)
            return handleButtonTouch(action, SDL2.Scancode.Y, SDL2.Keycode.y, 0, 0);
        if (v == btnA)
            return handleButtonTouch(action, SDL2.Scancode.A, SDL2.Keycode.a, 0, 0);
        if (v == btnB)
            return handleButtonTouch(action, SDL2.Scancode.B, SDL2.Keycode.b, 0, 0);
        if (v == btnSelect)
            return handleButtonTouch(action, SDL2.Scancode.SPACE, SDL2.Keycode.SPACE, 0, 0);
        if (v == btnStart)
            return handleButtonTouch(action, SDL2.Scancode.RETURN, SDL2.Keycode.RETURN, 0, 0);
        if (v == btnEsc)
            return handleButtonTouch(action, SDL2.Scancode.ESCAPE, SDL2.Keycode.ESCAPE, 0, 0);
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
        if (v == this.getPanel()) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    sendMouseMotion(x, y, 0, 0, 0, false);
                    sendMouseKey(true, SDL2.Button.RIGHT, x, y);
                    sendMouseKey(false, SDL2.Button.RIGHT, x, y);
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

    @Override
    public void onPartitionEvent(View v, int action, int part) {
        if (v == padLeft) {
            emulateArrowKeys(action, part);
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
