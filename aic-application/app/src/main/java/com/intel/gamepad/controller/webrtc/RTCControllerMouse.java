
package com.intel.gamepad.controller.webrtc;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;

import com.intel.gamepad.R;
import com.intel.gamepad.activity.PlayGameRtcActivity;
import com.intel.gamepad.app.KeyConst;
import com.intel.gamepad.app.MouseConst;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.controller.impl.MouseMotionEventListener;
import com.intel.gamepad.controller.impl.PartitionEventListener;
import com.intel.gamepad.controller.view.Pad;
import com.intel.gamepad.controller.view.PadMouse;
import com.intel.gamepad.utils.TimeDelayUtils;
import com.jeremy.fastsharedpreferences.FastSharedPreferences;
import com.mycommonlibrary.utils.LogEx;
import com.mycommonlibrary.utils.ToastUtils;

/**
 * 鼠标专用
 */
public class RTCControllerMouse extends BaseController implements MouseMotionEventListener, PartitionEventListener, CompoundButton.OnCheckedChangeListener, View.OnGenericMotionListener, View.OnKeyListener {
    public static final String NAME = "MOUSE";
    public static final String DESC = "鼠标操作";
    private ViewGroup vgRoot;
    private Button btnEsc = null;
    private PadMouse padMouse = null;
    private Pad padArrow = null;
    private ImageButton btnL = null;
    private ImageButton btnR = null;
    private ImageButton btnM = null;
    private RadioButton rbtnLockLeft = null;
    private RadioButton rbtnLockRight = null;
    private View viewTouch = null;

    public RTCControllerMouse(PlayGameRtcActivity act, Handler handler, DeviceSwitchListtener devSwitch) {
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
        this.vgRoot = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.game_pad_mouse, null, false);
        addControllerView(vgRoot);
        initBackButton(vgRoot.findViewById(R.id.btnBack));
        initSwitchDeviceButton(vgRoot.findViewById(R.id.ibtnShowKeyBoard));
        initSwitchDPadButton(vgRoot.findViewById(R.id.ibtnShowDPad));

        // 这个控件用于接收物理手柄的事件
        viewTouch = vgRoot.findViewById(R.id.viewMouse);
        viewTouch.requestFocus();
        viewTouch.setFocusable(true);
        viewTouch.setOnGenericMotionListener(this);
        viewTouch.setOnKeyListener(this);

        rbtnLockLeft = vgRoot.findViewById(R.id.chkLockLeft);
        rbtnLockRight = vgRoot.findViewById(R.id.chkLockRight);
        rbtnLockLeft.setOnCheckedChangeListener(this);
        rbtnLockRight.setOnCheckedChangeListener(this);
        if (!loadMouseLeft() && !loadMouseRight())
            saveMouseLeft(true);
        rbtnLockLeft.setChecked(loadMouseLeft());
        rbtnLockRight.setChecked(loadMouseRight());

        vgRoot.setOnTouchListener(this);

        btnEsc = vgRoot.findViewById(R.id.btnEsc);
        btnEsc.setOnTouchListener(this);

        btnL = vgRoot.findViewById(R.id.btnL);
        btnL.setOnTouchListener(this);

        btnR = vgRoot.findViewById(R.id.btnR);
        btnR.setOnTouchListener(this);

        btnM = vgRoot.findViewById(R.id.btnM);
        btnM.setOnTouchListener(this);

        padMouse = vgRoot.findViewById(R.id.padMouse);
        padMouse.setMouseMotionListener(this);
        padMouse.setOnTouchListener(this);

        padArrow = vgRoot.findViewById(R.id.padArrow);
        padArrow.setOnTouchListener(this);
        padArrow.setPartition(12);
        padArrow.setPartitionEventListener(this);
        padArrow.setDrawPartitionAll(false);
    }

    private float prevX = 0;
    private float prevY = 0;
    private boolean singleClick = false;// 单击的标志位，一次单击应该只响应down和up事件，如果有move事件则不认为是单击

    /**
     * 所有屏幕触摸的回调用
     */
    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        LogEx.i(">>>>" + v + " " + evt);
        this.updateLastTouchEvent();
        int action = evt.getActionMasked();
        float x = evt.getX();
        float y = evt.getY();
        sendAndroidEvent(action, x, y);

        if (v == btnEsc)
            return handleButtonTouch(action, KeyConst.VK_ESCAPE, v);
        if (v == btnL) {
            handleMouseButtonTouch(action, MouseConst.LEFT, x, y, btnL);
            return true;
        }
        if (v == btnR) {
            handleMouseButtonTouch(action, MouseConst.RIGHT, x, y, btnR);
            return true;
        }
        // 鼠标滚轮
        if (v == btnM) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    btnM.setBackgroundResource(R.drawable.bg_oval_btn_press_true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                    btnM.setBackgroundResource(R.drawable.bg_oval_btn_press_false);
                    break;
                case MotionEvent.ACTION_MOVE:
                    sendMouseWheel(0, y, 0);
                    break;
            }
            return true;
        }
        if (v == padArrow) {
            padArrow.onTouch(evt);
            return true;
        }
        if (v == padMouse) {
            padMouse.onTouch(evt);
            return true;
        }
        if (v == this.vgRoot) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    singleClick = true;
                    prevX = x;
                    prevY = y;
                    sendMouseMotionF(x, y, 0, 0);
                    break;
                case MotionEvent.ACTION_UP:
                    if (singleClick) {
                        sendMouseKey(true, loadMouseLeft() ? MouseConst.LEFT : MouseConst.RIGHT, mouseX, mouseY);
                        sendMouseKey(false, loadMouseLeft() ? MouseConst.LEFT : MouseConst.RIGHT, mouseX, mouseY);
                    }
                    prevX = x;
                    prevY = y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    singleClick = false;
                    if (showMouse) {
                        sendMouseMotionF(x, y, 0, 0);
                        prevX = x;
                        prevY = y;
                    } else {
                        sendMouseRelative(true);
                        if (evt.getHistorySize() > 1) {
                            int size = evt.getHistorySize() - 2;
                            float lastX = evt.getHistoricalX(size);
                            float lastY = evt.getHistoricalY(size);
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
        return false;
    }

    @Override
    public void onMouseMotion(View v, int x, int y, int dx, int dy) {
        if (showMouse) {
            if (prevX != -1 && prevY != -1) {
                prevX += dx;
                prevY += dy;
                sendMouseMotionF(prevX + dx, prevY + dy, 0, 0);
            }
        }
    }

    @Override
    public void onMouseDown(View v, int x, int y) {

    }

    @Override
    public void onMouseUp(View v, int x, int y) {

    }

    @Override
    public void onPartitionEvent(View v, int action, int part) {
        if (v == padArrow) {
            emulateArrowKeys(action, part);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == rbtnLockLeft) saveMouseLeft(isChecked);
        if (buttonView == rbtnLockRight) saveMouseRight(isChecked);
    }

    private void saveMouseLeft(boolean lock) {
        FastSharedPreferences.get("mouse_lock").edit().putBoolean("left", lock).commit();
    }

    private void saveMouseRight(boolean lock) {
        FastSharedPreferences.get("mouse_lock").edit().putBoolean("right", lock).commit();
    }

    private boolean loadMouseLeft() {
        return FastSharedPreferences.get("mouse_lock").getBoolean("left", false);
    }

    private boolean loadMouseRight() {
        return FastSharedPreferences.get("mouse_lock").getBoolean("right", false);
    }

    private float leftAxisX = 0f;
    private float leftAsixY = 0f;
    private float rightAxisX = 0f;
    private float rightAxisY = 0f;
    private float axisHatX = 0f;
    private float axisHatY = 0f;
    private int lastMouseX = 0;
    private int lastMouseY = 0;
    private int speed = 0;

    /**
     * 当右摇杆移动时持续发送消息。由于右摇杆被拉着不放时只会发送一次事件，所以这里需要用一个线程循环处理。
     */
    private void initRightAxisMotion() {
        new Thread(() -> {
            while (true) {
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

    /**
     * 手柄摇杆事件
     */
    @Override
    public boolean onGenericMotion(View v, MotionEvent event) {
        sendAndroidEvent(event.getAction(), event.getX(), event.getY());
        leftAxisX = event.getAxisValue(MotionEvent.AXIS_X);
        leftAsixY = event.getAxisValue(MotionEvent.AXIS_Y);
        rightAxisX = event.getAxisValue(MotionEvent.AXIS_Z);
        rightAxisY = event.getAxisValue(MotionEvent.AXIS_RZ);
        axisHatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        axisHatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

        leftAxisX = BaseController.filterMinValue(leftAxisX);
        leftAsixY = BaseController.filterMinValue(leftAsixY);
        rightAxisX = BaseController.filterMinValue(rightAxisX);
        rightAxisY = BaseController.filterMinValue(rightAxisY);
        axisHatX = BaseController.filterMinValue(axisHatX);
        axisHatY = BaseController.filterMinValue(axisHatY);

        LogEx.i(String.format("%.1f %.1f | %.1f %.1f %.1f %.1f", axisHatX, axisHatY, leftAxisX, leftAsixY, rightAxisX, rightAxisY));
        // 响应摇杆
        if ((leftAxisX + leftAxisX + rightAxisX + rightAxisY) != 0) {
            if (showMouse) {
                float offx = (leftAxisX + rightAxisX) * 10;
                float offy = (leftAsixY + rightAxisY) * 10;
                LogEx.i(">>>>>>>" + mouseX + " " + offx + " " + (mouseX + marginWidth + offx));
                sendMouseMotionF(mouseX + marginWidth + offx, mouseY + offy, 0, 0);
            } else {
                sendMouseRelative(true);
                sendMouseMotionF(0, 0, leftAxisX * 3, leftAsixY * 3);
                sendMouseMotionF(0, 0, rightAxisX * 3, rightAxisY * 3);
                sendMouseRelative(false);
            }
        }
        // 响应方向键
        if ((axisHatX + axisHatY) != 0) {
            fullScreenMove();
        }

        return false;
    }

    /**
     * 实现移动鼠标到屏幕边缘时全屏移动的效果
     */
    private void fullScreenMove() {
        if ((axisHatX + axisHatY) != 0) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            LogEx.i(">>>> " + lastMouseX + " " + lastMouseY);
            int offx = lastMouseX;
            int offy = lastMouseY;
            if (axisHatX > 0) offx = 10000;
            if (axisHatX < 0) offx = -10000;
            if (axisHatY > 0) offy = 10000;
            if (axisHatY < 0) offy = -10000;
            sendMouseMotionF(offx, offy, 0, 0);
            TimeDelayUtils.sleep(100);// 延时
            sendMouseMotionF(lastMouseX + marginWidth, lastMouseY, 0, 0);
        }
    }

    private int keyCount = 0;

    /**
     * 手柄按键事件
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        sendAndroidEvent(event.getAction(), event.getKeyCode());
        LogEx.i(keyCode + " " + event + " " + event.getDevice().getId());
        if (event.getDeviceId() == 7 && event.getSource() == 0x301 && keyCode == KeyEvent.KEYCODE_BACK)
            onBackPress();

        if (leftAxisX != 0f || leftAsixY != 0f || rightAxisX != 0f || rightAxisY != 0f) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    speed += 3;
                    float offx = (leftAxisX + rightAxisX) * (10 + speed);
                    float offy = (leftAsixY + rightAxisY) * (10 + speed);
                    sendMouseMotionF(mouseX + marginWidth + offx, mouseY + offy, 0, 0);
                    return true;
                case MotionEvent.ACTION_UP:
                    speed = 0;
                    break;
            }
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:
                sendMouseKey(true, MouseConst.LEFT, mouseX, mouseY);
                sendMouseKey(false, MouseConst.LEFT, mouseX, mouseY);
                break;
            case KeyEvent.KEYCODE_BUTTON_B:
                sendMouseKey(true, MouseConst.RIGHT, mouseX, mouseY);
                sendMouseKey(false, MouseConst.RIGHT, mouseX, mouseY);
                break;
            case KeyEvent.KEYCODE_BUTTON_SELECT:
                handleButtonTouch(event.getAction(), KeyConst.VK_SPACE, null);
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
