
package com.intel.gamepad.controller.rtsp;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.intel.gamepad.R;
import com.intel.gamepad.controller.impl.MouseMotionEventListener;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.controller.rtsp.GAController;
import com.intel.gamepad.controller.rtsp.SDL2;
import com.intel.gamepad.controller.view.PadMouse;

public class GAControllerMouse extends GAController implements MouseMotionEventListener {
    public static final String NAME = "MOUSE";
    public static final String DESC = "鼠标操作";
    private Button btnEsc = null;
    private PadMouse padMouse = null;
    private Button btnL = null;
    private Button btnR = null;

    public GAControllerMouse(Context c, Handler handler, DeviceSwitchListtener devSwitch) {
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
        super.setMouseVisibility(true);
        super.onDimensionChange(width, height);

        View viewPad = LayoutInflater.from(getContext()).inflate(R.layout.game_pad_mouse, null, false);
        placeView(viewPad, 0, 0, width, height);
        initBackButton(viewPad.findViewById(R.id.btnBack));
        initSwitchDeviceButton(viewPad.findViewById(R.id.ibtnShowKeyBoard));

        btnEsc = viewPad.findViewById(R.id.btnEsc);
        btnEsc.setOnTouchListener(this);

        btnL = viewPad.findViewById(R.id.btnL);
        btnL.setOnTouchListener(this);

        btnR = viewPad.findViewById(R.id.btnR);
        btnR.setOnTouchListener(this);

        padMouse = viewPad.findViewById(R.id.padMouse);
        padMouse.setMouseMotionListener(this);
        padMouse.setOnTouchListener(this);
    }

    private int curX = -1;
    private int curY = -1;
    private boolean singleClick = false;// 单击的标志位，一次单击应该只响应down和up事件，如果有move事件则不认为是单击

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        GAController.lastTouchMillis = System.currentTimeMillis();
        super.onTouch(v, evt);
        int action = evt.getActionMasked();
        int x = (int) evt.getX();
        int y = (int) evt.getY();
        if (curX == -1) curX = getMouseX();
        if (curY == -1) curY = getMouseY();

        if (v == btnEsc)
            return handleButtonTouch(action, SDL2.Scancode.ESCAPE, SDL2.Keycode.ESCAPE, 0, 0);
        if (v == btnL) {
            mouseButtonClick(SDL2.Button.LEFT, action);
            return true;
        }

        if (v == btnR) {
            mouseButtonClick(SDL2.Button.RIGHT, action);
            return true;
        }

        if (v == padMouse) {
            padMouse.onTouch(evt);
            return true;
        }
        if (v == this.getPanel()) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    singleClick = true;
                    curX = x;
                    curY = y;
                    break;
                case MotionEvent.ACTION_UP:
                    //lastX = lastY = -1;
                    if (singleClick) {
                        sendMouseMotion(x, y, 0, 0, 0, false);
                        sendMouseKey(true, SDL2.Button.RIGHT, x, y);
                        sendMouseKey(false, SDL2.Button.RIGHT, x, y);
                        drawCursor((int) x, (int) y);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    singleClick = false;
                    float dx = x - curX;
                    float dy = y - curY;
                    moveMouse(dx, dy);
                    sendMouseMotion(x, y, dx, dy, 0, /*relative=*/false);
                    drawCursor((int) x, (int) y);
                    curX = x;
                    curY = y;
                    break;
            }
            return true;
        }

        return false;
    }

    private void mouseButtonClick(int button, int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                sendMouseKey(true, button, curX, curY);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                sendMouseKey(false, button, curX, curY);
                break;
        }
    }


    @Override
    public void onMouseMotion(View v, int x, int y, int dx, int dy) {
        if (curX != -1 && curY != -1) {
            curX += dx;
            curY += dy;
            drawCursor((int) curX, (int) curY);
            sendMouseMotion(curX, curY, 0, 0, 0, false);
        }
    }

    @Override
    public void onMouseDown(View v, int x, int y) {

    }

    @Override
    public void onMouseUp(View v, int x, int y) {

    }
}
