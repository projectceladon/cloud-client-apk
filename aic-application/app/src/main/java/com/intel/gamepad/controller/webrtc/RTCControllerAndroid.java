
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
 * Android触屏专用
 */
public class RTCControllerAndroid extends BaseController implements View.OnGenericMotionListener, View.OnKeyListener {
    public static final String NAME = "ANDROID";
    public static final String DESC = "Android Touch Screen ";
    private ViewGroup vgRoot;
    private View viewTouch    = null;

    public RTCControllerAndroid(PlayGameRtcActivity act, Handler handler, DeviceSwitchListtener devSwitch) {
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
        this.vgRoot = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.game_pad_android, null, false);
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
        vgRoot.setOnTouchListener(this);
    }

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
        float width = 32767, height = 32767;
        int nRomoteX = Math.round((x * width) / v.getWidth());
        int nRomoteY = Math.round((y * height) / v.getHeight());
        sendAndroidEvent(action, nRomoteX, nRomoteY);
        return true;
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
//        if ((leftAxisX + leftAxisX + rightAxisX + rightAxisY) != 0) {
//            if (showMouse) {
//                float offx = (leftAxisX + rightAxisX) * 10;
//                float offy = (leftAsixY + rightAxisY) * 10;
//                sendMouseMotionF(mouseX + marginWidth + offx, mouseY + offy, 0, 0);
//            } else {
//                sendMouseRelative(true);
//                sendMouseMotionF(0, 0, leftAxisX * 3, leftAsixY * 3);
//                sendMouseMotionF(0, 0, rightAxisX * 3, rightAxisY * 3);
//                sendMouseRelative(false);
//            }
//        }

        return false;
    }

    /**
     * 手柄按键事件
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        sendAndroidEvent(event.getAction(), event.getKeyCode());
        LogEx.i(keyCode + " " + event + " " + event.getDevice().getId());
        if (event.getDeviceId() == 7 && event.getSource() == 0x301 && keyCode == KeyEvent.KEYCODE_BACK)
            onBackPress();

//        if (leftAxisX != 0f || leftAsixY != 0f || rightAxisX != 0f || rightAxisY != 0f) {
//            switch (event.getAction()) {
//                case MotionEvent.ACTION_DOWN:
//                    speed += 3;
//                    float offx = (leftAxisX + rightAxisX) * (10 + speed);
//                    float offy = (leftAsixY + rightAxisY) * (10 + speed);
//                    sendMouseMotionF(mouseX + marginWidth + offx, mouseY + offy, 0, 0);
//                    return true;
//                case MotionEvent.ACTION_UP:
//                    speed = 0;
//                    break;
//            }
//        }
//        switch (keyCode) {
//            case KeyEvent.KEYCODE_BUTTON_A:
//                sendMouseKey(true, MouseConst.LEFT, mouseX, mouseY);
//                sendMouseKey(false, MouseConst.LEFT, mouseX, mouseY);
//                break;
//            case KeyEvent.KEYCODE_BUTTON_B:
//                sendMouseKey(true, MouseConst.RIGHT, mouseX, mouseY);
//                sendMouseKey(false, MouseConst.RIGHT, mouseX, mouseY);
//                break;
//            case KeyEvent.KEYCODE_BUTTON_SELECT:
//                handleButtonTouch(event.getAction(), KeyConst.VK_SPACE, null);
//                break;
//            case KeyEvent.KEYCODE_BUTTON_START:
//                handleButtonTouch(event.getAction(), KeyConst.VK_ENTER, null);
//                if (keyCount == 5) {
//                    devSwitch.switchGamePad();
//                    keyCount = 0;
//                    ToastUtils.show("Game Pad Mode");
//                } else {
//                    keyCount++;
//                }
//                break;
//        }
        return true;
    }
}
