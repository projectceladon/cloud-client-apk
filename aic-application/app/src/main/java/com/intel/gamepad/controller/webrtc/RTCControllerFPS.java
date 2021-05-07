
package com.intel.gamepad.controller.webrtc;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.intel.gamepad.R;
import com.intel.gamepad.activity.PlayGameRtcActivity;
import com.intel.gamepad.app.KeyConst;
import com.intel.gamepad.app.MouseConst;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.controller.impl.PartitionEventListener;
import com.intel.gamepad.controller.view.Pad;
import com.mycommonlibrary.utils.LogEx;
import com.mycommonlibrary.utils.ToastUtils;

public class RTCControllerFPS extends BaseController implements
        PartitionEventListener, View.OnGenericMotionListener, View.OnKeyListener {
    public static final String NAME = "FPS";
    public static final String DESC = "射击类型游戏控制器";
    private ViewGroup vgRoot;
    private Button btnEsc = null;
    private Button btnSelect = null;
    private Button btnStart = null;
    private Pad padArrowKey = null;
    private ImageView btnShootRight = null;
    private ImageView btnShootLeft = null;
    private CheckBox btnLock = null;
    private Button btnKeyR = null;
    private Button btnCtrl = null;
    private ImageButton btnChangeGun = null;
    private ImageView btnFight = null;
    private Button btn1 = null;
    private Button btn2 = null;
    private Button btn3 = null;
    private Button btn4 = null;
    private Button btn5 = null;
    private Button btn6 = null;
    private View viewTouch = null;

    public RTCControllerFPS(PlayGameRtcActivity act, Handler handler, DeviceSwitchListtener devSwitch) {
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
        if (vgRoot == null) initView();
        return vgRoot;
    }

    private void initView() {
        vgRoot = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.game_pad_fps, null, false);
        addControllerView(vgRoot);

        initBackButton(vgRoot.findViewById(R.id.btnBack));
        initSwitchDeviceButton(vgRoot.findViewById(R.id.ibtnShowKeyBoard));
        initSwitchDPadButton(vgRoot.findViewById(R.id.ibtnShowDPad));

        vgRoot.setOnTouchListener(this);

        viewTouch = vgRoot.findViewById(R.id.viewMouse);
        viewTouch.requestFocus();
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
        // 方向键
        padArrowKey = vgRoot.findViewById(R.id.padLeft);
        padArrowKey.setOnTouchListener(this);
        padArrowKey.setPartition(12);
        padArrowKey.setPartitionEventListener(this);
        padArrowKey.setDrawPartitionAll(false);

        btnShootRight = vgRoot.findViewById(R.id.btnShootRight);
        btnShootRight.setOnTouchListener(this);
        btnShootLeft = vgRoot.findViewById(R.id.btnShootLeft);
        btnShootLeft.setOnTouchListener(this);

        btnLock = vgRoot.findViewById(R.id.btnLock);
        btnLock.setOnClickListener(v -> {
            if (btnLock.isChecked())
                sendMouseButtonDown(MotionEvent.ACTION_DOWN, MouseConst.RIGHT, 0, 0, v);
            else
                sendMouseButtonUp(MotionEvent.ACTION_UP, MouseConst.RIGHT, 0, 0, v);
        });

        btnKeyR = vgRoot.findViewById(R.id.btnKeyR);
        btnKeyR.setOnTouchListener(this);

        btnChangeGun = vgRoot.findViewById(R.id.btnChangeGun);
        btnChangeGun.setOnTouchListener(this);

        btnCtrl = vgRoot.findViewById(R.id.btnStand);
        btnCtrl.setOnTouchListener(this);

        btnFight = vgRoot.findViewById(R.id.btnFight);
        btnFight.setOnTouchListener(this);

        btn1 = vgRoot.findViewById(R.id.btn1);
        btn1.setOnTouchListener(this);
        btn2 = vgRoot.findViewById(R.id.btn2);
        btn2.setOnTouchListener(this);
        btn3 = vgRoot.findViewById(R.id.btn3);
        btn3.setOnTouchListener(this);
        btn4 = vgRoot.findViewById(R.id.btn4);
        btn4.setOnTouchListener(this);
        btn5 = vgRoot.findViewById(R.id.btn5);
        btn5.setOnTouchListener(this);
        btn6 = vgRoot.findViewById(R.id.btn6);
        btn6.setOnTouchListener(this);
    }

    private boolean isSingleClick = false;

    private float offx = 0;
    private float offy = 0;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        sendAndroidEvent(event.getAction(), event.getX(), event.getY(), 0);
        updateLastTouchEvent();
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();
        sendAndroidEvent(action, x, y, 0);

        if (v == btnShootLeft || v == btnShootRight) {
            return handleMouseButtonTouch(action, MouseConst.LEFT, x, y, v);
        }
        if (v == btnKeyR)
            return handleOvalButtonTouch(action, KeyConst.VK_R, v);
        if (v == btnCtrl)
            return handleButtonTouch(action, KeyConst.VK_CONTROL, v);
        if (v == btnSelect)
            return handleButtonTouch(action, KeyConst.VK_SPACE, v);
        if (v == btnStart)
            return handleButtonTouch(action, KeyConst.VK_ENTER, v);
        if (v == btnEsc)
            return handleButtonTouch(action, KeyConst.VK_ESCAPE, v);
        if (v == btnChangeGun) {
            sendMouseWheel(x, y, 1f);
            return true;
        }
        if (v == btnFight) {
            return handleButtonTouch(action, KeyConst.VK_V, v);
        }
        if (v == btn1) {
            return handleButtonTouch(action, KeyConst.VK_1, v);
        }
        if (v == btn2) {
            return handleButtonTouch(action, KeyConst.VK_2, v);
        }
        if (v == btn3) {
            return handleButtonTouch(action, KeyConst.VK_3, v);
        }
        if (v == btn4) {
            return handleButtonTouch(action, KeyConst.VK_4, v);
        }
        if (v == btn5) {
            return handleButtonTouch(action, KeyConst.VK_5, v);
        }
        if (v == btn6) {
            return handleButtonTouch(action, KeyConst.VK_6, v);
        }
        // 此控件主要是发送鼠标的相对坐标，以实现游戏的视角转动，点击事件用于实现鼠标左键的开枪效果
        if (v == viewTouch) {
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    isSingleClick = true;
                    if (showMouse) {
                        offx = x - marginWidth - mouseX;
                        offy = y - mouseY;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    if (isSingleClick) {
                        long lastMillis = System.currentTimeMillis();
                        sendMouseKey(true, MouseConst.LEFT, x, y);
                        // 鼠标按下就开枪，但有游戏在收到鼠标按下事件后会延时几个毫秒后才开枪，
                        // 所以在这里需要用循环来实现一个延时，然后再发送鼠标释放事件。
                        // 如果控制好循环延时的话也可以实现点击一次打三枪的效果
                        while ((System.currentTimeMillis() - lastMillis) < 50) {
                            System.out.println("single delay" + (System.currentTimeMillis() - lastMillis));
                        }
                        sendMouseKey(false, MouseConst.LEFT, x, y);
                        isSingleClick = false;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    isSingleClick = false;
                    if (showMouse) {
                        sendMouseMotionF(x - offx, y - offy, 0, 0);
                    } else {
                        sendMouseRelative(true);
                        if (event.getHistorySize() > 1) {
                            int size = event.getHistorySize() - 2;
                            float lastX = event.getHistoricalX(size);
                            float lastY = event.getHistoricalY(size);
                            float dx = x - lastX;
                            float dy = y - lastY;
                            sendMouseMotionF(x, y, dx, dy);
                        }
                        sendMouseRelative(false);
                    }
                    break;
            }
            return true;
        }
        if (v == padArrowKey) {
            return ((Pad) v).onTouch(event);
        }
        return false;
    }

    @Override
    public void onPartitionEvent(View v, int action, int part) {
        if (v == padArrowKey) {
            this.emulateWASDKeys(action, part);
        }
    }

    private float leftAxisX = 0f;
    private float leftAsixY = 0f;
    private float rightAxisX = 0f;
    private float rightAxisY = 0f;
    private float axisHatX = 0f;
    private float axisHatY = 0f;
    private int speed = 0;

    /**
     * 当右摇杆移动时持续发送消息。由于右摇杆被拉着不放时只会发送一次事件，所以这里需要用一个线程循环处理。
     */
    private void initRightAxisMotion() {
        new Thread(() -> {
            while (true) {
                showMouse = false;
                if ((rightAxisX + rightAxisY) != 0) {
                    if (showMouse) {
                        float offx = rightAxisX * 5;
                        float offy = rightAxisY * 5;
                        sendMouseMotionF(mouseX + marginWidth + offx, mouseY + offy, 0, 0);
                    } else {
                        sendMouseRelative(true);
                        sendMouseMotionF(0, 0, rightAxisX * 3, rightAxisY * 3);
                        sendMouseRelative(false);
                    }
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
    public boolean onGenericMotion(View v, MotionEvent event) {
        sendAndroidEvent(event.getAction(), event.getX(), event.getY(), 0);
        showMouse = false;

        leftAxisX = event.getAxisValue(MotionEvent.AXIS_X);
        leftAsixY = event.getAxisValue(MotionEvent.AXIS_Y);
        rightAxisX = event.getAxisValue(MotionEvent.AXIS_Z);
        rightAxisY = event.getAxisValue(MotionEvent.AXIS_RZ);
        axisHatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        axisHatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

        LogEx.i(String.format("===%.1f %.1f | %.1f %.1f %.1f %.1f", axisHatX, axisHatY, leftAxisX, leftAsixY, rightAxisX, rightAxisY));

        if ((leftAxisX + leftAsixY) > 0.2 || (leftAxisX + leftAsixY) < -0.2) {
            if (showMouse) {
                float offx = (leftAxisX) * 10;
                float offy = (leftAsixY) * 10;
                sendMouseMotionF(mouseX + marginWidth + offx, mouseY + offy, 0, 0);
            } else {
                sendMouseRelative(true);
                sendMouseMotionF(0, 0, (int) (leftAxisX * 10), (int) (leftAsixY * 10));
                sendMouseRelative(false);
            }
        }

        if ((axisHatX + axisHatY) != 0) {
            if (showMouse) {
                if (axisHatX > 0) sendKeyEvent(true, KeyConst.VK_RIGHT);
                if (axisHatX < 0) sendKeyEvent(true, KeyConst.VK_LEFT);
                if (axisHatY > 0) sendKeyEvent(true, KeyConst.VK_DOWN);
                if (axisHatY < 0) sendKeyEvent(true, KeyConst.VK_UP);
            } else {
                if (axisHatX > 0) sendKeyEvent(true, KeyConst.VK_D);
                if (axisHatX < 0) sendKeyEvent(true, KeyConst.VK_A);
                if (axisHatY > 0) sendKeyEvent(true, KeyConst.VK_S);
                if (axisHatY < 0) sendKeyEvent(true, KeyConst.VK_W);
            }
        } else {
            if (showMouse) {
                sendKeyEvent(false, KeyConst.VK_RIGHT);
                sendKeyEvent(false, KeyConst.VK_LEFT);
                sendKeyEvent(false, KeyConst.VK_DOWN);
                sendKeyEvent(false, KeyConst.VK_UP);
            } else {
                sendKeyEvent(false, KeyConst.VK_D);
                sendKeyEvent(false, KeyConst.VK_A);
                sendKeyEvent(false, KeyConst.VK_W);
                sendKeyEvent(false, KeyConst.VK_S);
            }
        }
        return false;
    }

    private int keyCount = 0;

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        sendAndroidEvent(event.getAction(), event.getKeyCode());
        LogEx.i(keyCode + " " + event);

        if (keyCode == KeyEvent.KEYCODE_BACK) onBackPress();
        if (leftAxisX != 0f || leftAsixY != 0f) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    showMouse = false;
                    if (showMouse) { // 绝对坐标
                        speed += 5;
                        float offx = (leftAxisX) * (10 + speed);
                        float offy = (leftAsixY) * (10 + speed);
                        sendMouseMotionF(mouseX + marginWidth + offx, mouseY + offy, 0, 0);
                    } else { // 相对坐标
                        if (speed < 20)
                            speed++;
                        sendMouseRelative(true);
                        sendMouseMotionF(0, 0, leftAxisX * 10 * speed, leftAsixY * 10 * speed);
                        sendMouseRelative(false);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    speed = 0; // 按键释放后加速度置0
                    break;
            }
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_BUTTON_L2:
                handleMouseButtonTouch(event.getAction(), MouseConst.LEFT, mouseX, mouseY, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_BUTTON_R2:
                handleMouseButtonTouch(event.getAction(), MouseConst.RIGHT, mouseX, mouseY, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_X:
                handleButtonTouch(event.getAction(), KeyConst.VK_SPACE, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_THUMBL:
                handleButtonTouch(event.getAction(), KeyConst.VK_V, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_THUMBR:
                handleButtonTouch(event.getAction(), KeyConst.VK_C, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_SELECT:
                handleButtonTouch(event.getAction(), KeyConst.VK_ESCAPE, null);
                break;
            case KeyEvent.KEYCODE_BUTTON_START:
                handleButtonTouch(event.getAction(), KeyConst.VK_ENTER, null);
                if (keyCount == 5) {
                    devSwitch.switchGamePad();
                    keyCount = 0;
                    ToastUtils.show("Game Pad Mode");
                } else {
                    keyCount++;
                }
                break;
        }
        return true;
    }

}
