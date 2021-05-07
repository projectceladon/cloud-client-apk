
package com.intel.gamepad.controller.webrtc;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.intel.gamepad.R;
import com.intel.gamepad.activity.PlayGameRtcActivity;
import com.intel.gamepad.app.KeyConst;
import com.intel.gamepad.app.MouseConst;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.controller.impl.MouseMotionEventListener;
import com.intel.gamepad.controller.impl.PartitionEventListener;
import com.intel.gamepad.controller.view.Pad;
import com.intel.gamepad.controller.view.PadMouse;
import com.mycommonlibrary.utils.LogEx;
import com.mycommonlibrary.utils.ToastUtils;

/**
 * 安卓物理手柄专用
 */
public class RTCControllerXBox extends BaseController implements
        PartitionEventListener, MouseMotionEventListener, View.OnGenericMotionListener, View.OnKeyListener {
    public static final String NAME = "XBOX";
    public static final String DESC = "XBox游戏机手柄";
    private ViewGroup vgRoot;
    private Button btnEsc = null;
    private Button btnSelect = null;
    private Button btnStart = null;
    private Pad padArrow = null;
    private PadMouse padMouseR = null;
    private PadMouse padMouseL = null;
    private Button btnL1 = null;
    private Button btnR1 = null;
    private Button btnL2 = null;
    private Button btnR2 = null;
    private Button btnX = null;
    private Button btnY = null;
    private Button btnA = null;
    private Button btnB = null;
    private View viewTouch = null;

    public RTCControllerXBox(PlayGameRtcActivity act, Handler handler, DeviceSwitchListtener devSwitch) {
        super(act, handler, devSwitch);
        initRightAxisMotion();
    }

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return DESC;
    }

    public View getView() {
        if (this.vgRoot == null) initView();
        return vgRoot;
    }

    private void initView() {
        vgRoot = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.game_pad_xbox, null, false);
        addControllerView(vgRoot);

        initBackButton(vgRoot.findViewById(R.id.btnBack));
        vgRoot.findViewById(R.id.ibtnGamePad).setOnClickListener(view -> devSwitch.switchMapperPad());

        viewTouch = vgRoot.findViewById(R.id.viewMouse);
        viewTouch.requestFocus();
        viewTouch.setFocusable(true);
        viewTouch.setOnTouchListener(this);
        viewTouch.setOnGenericMotionListener(this);
        viewTouch.setOnKeyListener(this);

        btnEsc = vgRoot.findViewById(R.id.btnEsc);
        btnEsc.setOnTouchListener(this);
        //
        btnSelect = vgRoot.findViewById(R.id.btnSelect);
        btnSelect.setOnTouchListener(this);
        //
        btnStart = vgRoot.findViewById(R.id.btnStart);
        btnStart.setOnTouchListener(this);
        //
        btnL1 = vgRoot.findViewById(R.id.btnL);
        btnL1.setOnTouchListener(this);

        btnL2 = vgRoot.findViewById(R.id.btnL2);
        btnL2.setOnTouchListener(this);
        //
        btnR1 = vgRoot.findViewById(R.id.btnR);
        btnR1.setOnTouchListener(this);

        btnR2 = vgRoot.findViewById(R.id.btnR2);
        btnR2.setOnTouchListener(this);
        //
        padArrow = vgRoot.findViewById(R.id.padLeft);
        padArrow.setOnTouchListener(this);
        padArrow.setPartition(12);
        padArrow.setPartitionEventListener(this);
        padArrow.setDrawPartitionAll(false);

        btnX = vgRoot.findViewById(R.id.btnX);
        btnX.setOnTouchListener(this);

        btnY = vgRoot.findViewById(R.id.btnY);
        btnY.setOnTouchListener(this);

        btnA = vgRoot.findViewById(R.id.btnA);
        btnA.setOnTouchListener(this);

        btnB = vgRoot.findViewById(R.id.btnB);
        btnB.setOnTouchListener(this);
        // 鼠标方向盘
        padMouseR = vgRoot.findViewById(R.id.padMouse);
        padMouseR.setMouseMotionListener(this);
        padMouseR.setOnTouchListener(this);
        padMouseL = vgRoot.findViewById(R.id.padMouse2);
        padMouseL.setMouseMotionListener(this);
        padMouseL.setOnTouchListener(this);
    }


    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        this.updateLastTouchEvent();

        int action = evt.getActionMasked();
        int x = (int) evt.getX();
        int y = (int) evt.getY();
        //
        if (v == btnL1)
            return handleJoyKeyTouch(action, KeyConst.VK_DPAD_L1, v);
        if (v == btnL2)
            return handleJoyKeyTouch(action, KeyConst.VK_DPAD_L2, v);
        if (v == btnR1)
            return handleJoyKeyTouch(action, KeyConst.VK_DPAD_R1, v);
        if (v == btnR2)
            return handleJoyKeyTouch(action, KeyConst.VK_DPAD_R2, v);
        if (v == btnX)
            return handleJoyKeyTouch(action, KeyConst.VK_DPAD_X, v);
        if (v == btnY)
            return handleJoyKeyTouch(action, KeyConst.VK_DPAD_Y, v);
        if (v == btnA)
            return handleJoyKeyTouch(action, KeyConst.VK_DPAD_A, v);
        if (v == btnB)
            return handleJoyKeyTouch(action, KeyConst.VK_DPAD_B, v);
        if (v == btnSelect)
            return handleJoyKeyTouch(action, KeyConst.VK_DPAD_SELECT, v);
        if (v == btnStart)
            return handleJoyKeyTouch(action, KeyConst.VK_DPAD_START, v);
        if (v == btnEsc)
            return handleButtonTouch(action, KeyConst.VK_ESCAPE, v);
        if (v == padArrow) {
            padArrow.onTouch(evt);
            return true;
        }
        if (v == padMouseR) {
            padMouseR.onTouch(evt);
            return true;
        }
        if (v == padMouseL) {
            padMouseL.onTouch(evt);
            return true;
        }

        if (v == vgRoot) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    sendMouseMotionF(x, y, 0, 0);
                    sendMouseKey(true, MouseConst.RIGHT, x, y);
                    sendMouseKey(false, MouseConst.RIGHT, x, y);
                    break;
                case MotionEvent.ACTION_UP:
//                    lastX = lastY = -1;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (evt.getHistorySize() > 1) {
                        int lastX = (int) evt.getHistoricalX(evt.getHistorySize() - 2);
                        int lastY = (int) evt.getHistoricalY(evt.getHistorySize() - 2);
                        onMouseMotion(v, x, y, x - lastX, y - lastY);
                        LogEx.i(String.format("%d %d %d %d", x, y, x - lastX, y - lastY));
                    }
                    break;
            }
            return true;
        }

        return false;
    }

    @Override
    public void onPartitionEvent(View v, int action, int part) {
        if (v == padArrow) {
            emulateDPadArrowKeys(action, part);
        }
    }

    @Override
    public void onMouseMotion(View v, int x, int y, int dx, int dy) {
        LogEx.i(String.format("%d,%d,%d,%d", x, y, dx, dy));
//        sendMouseMotionF(x, y, dx, dy);
        float sx = (float) (dx * 2) / (float) v.getWidth();
        float sy = (float) (dy * 2) / (float) v.getHeight();
        if (v == padMouseL) sendLeftAxisMotion(sx, sy);
        if (v == padMouseR) sendRightAxisMotion(sx, sy);
    }

    @Override
    public void onMouseDown(View v, int x, int y) {
        sendMouseRelative(true);
    }

    @Override
    public void onMouseUp(View v, int x, int y) {
        sendMouseRelative(false);
    }

    private float leftAxisX = 0f;
    private float leftAsixY = 0f;
    private float rightAxisX = 0f;
    private float rightAxisY = 0f;
    private float lastLeftAxisX = 0f;
    private float lastLeftAxisY = 0f;
    private float lastRightAxisX = 0f;
    private float lastRightAxisY = 0f;

    /**
     * 当右摇杆移动时持续发送消息。由于右摇杆被拉着不放时只会发送一次事件，所以这里需要用一个线程循环处理。
     */
    private void initRightAxisMotion() {
        new Thread(() -> {
            while (true) {
                if ((lastRightAxisX + lastRightAxisY) != 0) {
                    sendRightAxisMotion(rightAxisX, rightAxisY);
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public boolean onGenericMotion(View v, MotionEvent evt) {
        sendAndroidEvent(evt.getAction(), evt.getX(), evt.getY(), 0);
        leftAxisX = evt.getAxisValue(MotionEvent.AXIS_X);
        leftAsixY = evt.getAxisValue(MotionEvent.AXIS_Y);
        rightAxisX = evt.getAxisValue(MotionEvent.AXIS_Z);
        rightAxisY = evt.getAxisValue(MotionEvent.AXIS_RZ);
        float axisHatX = evt.getAxisValue(MotionEvent.AXIS_HAT_X);
        float axisHatY = evt.getAxisValue(MotionEvent.AXIS_HAT_Y);

        LogEx.i(String.format("%.1f %.1f | %.1f %.1f | %.1f %.1f", axisHatX, axisHatY, leftAxisX, leftAsixY, rightAxisX, rightAxisY));
        leftAxisX = BaseController.filterMinValue(leftAxisX);
        leftAsixY = BaseController.filterMinValue(leftAsixY);
        rightAxisX = BaseController.filterMinValue(rightAxisX);
        rightAxisY = BaseController.filterMinValue(rightAxisY);


        sendRightAxisMotion(rightAxisX, rightAxisY);
        lastRightAxisX = rightAxisX;
        lastRightAxisY = rightAxisY;

        float triggerL = evt.getAxisValue(MotionEvent.AXIS_BRAKE);
        if (triggerL > 0.1 || triggerL < -0.1)
            sendLeftTrigger(triggerL);
        else
            sendLeftTrigger(0f);

        float triggerR = evt.getAxisValue(MotionEvent.AXIS_GAS);
        if (triggerR > 0.1 || triggerR < -0.1)
            sendRightTrigger(triggerR);
        else
            sendRightTrigger(0f);

        if ((axisHatX + axisHatY) != 0) {
            if (axisHatX > 0) sendJoyKeyEvent(true, KeyConst.VK_DPAD_RIGHT);
            if (axisHatX < 0) sendJoyKeyEvent(true, KeyConst.VK_DPAD_LEFT);
            if (axisHatY > 0) sendJoyKeyEvent(true, KeyConst.VK_DPAD_DOWN);
            if (axisHatY < 0) sendJoyKeyEvent(true, KeyConst.VK_DPAD_UP);
        } else {
            sendJoyKeyEvent(false, KeyConst.VK_DPAD_RIGHT);
            sendJoyKeyEvent(false, KeyConst.VK_DPAD_LEFT);
            sendJoyKeyEvent(false, KeyConst.VK_DPAD_DOWN);
            sendJoyKeyEvent(false, KeyConst.VK_DPAD_UP);
        }

        return false;
    }

    private int keyCount = 0;

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        LogEx.i(keyCode + " " + event);
        sendAndroidEvent(event.getAction(), event.getKeyCode());
        if (keyCode == KeyEvent.KEYCODE_BACK) onBackPress();

        if ((lastLeftAxisX + lastLeftAxisY) != 0) {
            sendLeftAxisMotion(leftAxisX, leftAsixY);
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_A, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_B:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_B, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_X:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_X, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_Y:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_Y, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_SELECT:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_SELECT, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_START:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_START, null);
                if (keyCount == 5) {
                    devSwitch.switchMapperPad();
                    keyCount = 0;
                    ToastUtils.show("Keyboard+Mouse Mode");
                } else {
                    keyCount++;
                }
                break;
            case KeyEvent.KEYCODE_BUTTON_L2:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_L2, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_R2:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_R2, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_L1:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_L1, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_R1:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_R1, null);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_DOWN, null);
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_UP, null);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_LEFT, null);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                handleJoyKeyTouch(event.getAction(), KeyConst.VK_DPAD_RIGHT, null);
                break;
            default:
                handleJoyKeyTouch(event.getAction(), event.getKeyCode(), null);
                break;
        }
        return true;
    }
}
