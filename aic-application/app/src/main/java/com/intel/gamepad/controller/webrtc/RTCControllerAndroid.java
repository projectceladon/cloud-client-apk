
package com.intel.gamepad.controller.webrtc;

import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Trace;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;

import com.google.gson.Gson;
import com.intel.gamepad.R;
import com.intel.gamepad.activity.PlayGameRtcActivity;
import com.intel.gamepad.app.KeyConst;
import com.intel.gamepad.app.MouseConst;
import com.intel.gamepad.bean.MotionEventBean;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.controller.impl.MouseMotionEventListener;
import com.intel.gamepad.controller.impl.PartitionEventListener;
import com.intel.gamepad.controller.view.Pad;
import com.intel.gamepad.controller.view.PadMouse;
import com.intel.gamepad.owt.p2p.P2PHelper;
import com.intel.gamepad.utils.TimeDelayUtils;
import com.jeremy.fastsharedpreferences.FastSharedPreferences;
import com.mycommonlibrary.utils.LogEx;
import com.mycommonlibrary.utils.ToastUtils;
import owt.base.OwtError;

/**
 * Android触屏专用
 */
public class RTCControllerAndroid extends BaseController implements View.OnGenericMotionListener, View.OnKeyListener {
    public static final String NAME = "ANDROID";
    public static final String DESC = "Android Touch Screen ";
    public static final String TAG = "RTCCTLAndroid";
    private ViewGroup vgRoot;
    private View viewTouch = null;
    public static final int invalidDeviceId = -100;
    public static final int deviceSlotIndexZero = 0;
    public static final int deviceSlotIndexOne = 1;
    public static int[] deviceSlot = {invalidDeviceId, invalidDeviceId};
    private int nPointCount = 0;
    private int[] pointArray =  new int[10];
    private int nCountInput;

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
        Log.d("test", "evt: " + evt);
        this.updateLastTouchEvent();
        int index;
        float x;
        float y;
        int nRomoteX;
        int nRomoteY;
        int pointId;
        int pressure;
        float width = 32767, height = 32767;
        int action = evt.getActionMasked();
        if(action == MotionEvent.ACTION_UP || action ==  MotionEvent.ACTION_CANCEL) {
            if(action == MotionEvent.ACTION_UP) {
                nCountInput++;
                Trace.beginSection("atou C1 ID: " + nCountInput + " size: " + 0);
                Trace.endSection();
            }
            String strCmd = "";
            for (int i = 0; i < nPointCount; i++) {
                strCmd = strCmd + "u " + pointArray[i] + "\n";
            }
            sendAndroidEventAsString(strCmd);
            return true;
        } else {
            nPointCount =  evt.getPointerCount();
            if(nPointCount > 10) {
                nPointCount = 10;
            }
            for (int i = 0; i < nPointCount; i++) {
                int pointIdTmp = evt.getPointerId(i);
                pointArray[i] = pointIdTmp;
            }
        }
        if (action == MotionEvent.ACTION_MOVE) {
            int pointerCount = evt.getPointerCount();
            String strCmd = "";
            for (int i = 0; i < pointerCount; i++) {
                pointId = evt.getPointerId(i);
                x = evt.getX(i);
                y = evt.getY(i);
                nRomoteX = Math.round((x * width) / v.getWidth());
                nRomoteY = Math.round((y * height) / v.getHeight());
                strCmd = strCmd + "m " + pointId + " " + nRomoteX + " " + nRomoteY + " " + 255 + "\n";
            }
            sendAndroidEventAsString(strCmd);
            return true;
        }

        index = evt.getActionIndex();
        pointId = evt.getPointerId(index);
        x = evt.getX(index);
        y = evt.getY(index);
        nRomoteX = Math.round((x * width) / v.getWidth());
        nRomoteY = Math.round((y * height) / v.getHeight());
        //pressure = (int)evt.getPressure(pointId);
        String strCmd;
        if(action == MotionEvent.ACTION_POINTER_UP) {
            Log.d("test", "ACTION_POINTER_UP: ");
            strCmd = "u " + pointId + "\n";
        } else {
            strCmd = "d " + pointId + " " + nRomoteX + " " + nRomoteY + " " + 255 + "\n";
        }

        sendAndroidEventAsString(strCmd);
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
        int eventSource = event.getSource();
        if ((((eventSource & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) ||
                ((eventSource & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK))
                && event.getAction() == MotionEvent.ACTION_MOVE) {
            int indexSlot = RTCControllerAndroid.getDeviceSlotIndex(event.getDeviceId());
            processJoystickInput(event, -1, indexSlot);
        } else {
            int pointerCount = event.getPointerCount();
            for (int i = 0; i < pointerCount; i++) {
                float x = event.getX(i);
                float y = event.getY(i);
                float width = 32767, height = 32767;
                int nRomoteX = Math.round((x * width) / v.getWidth());
                int nRomoteY = Math.round((y * height) / v.getHeight());
                sendAndroidEvent(event.getAction(), nRomoteX, nRomoteY, i);
            }
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
        }
        return false;
    }

    /**
     * 手柄按键事件
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        int eventSource = event.getSource();
        if (((eventSource & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
                || ((eventSource & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)) {
            int keyMapCode = -1;
            int actionDown = JOY_KEY_CODE_MAP_DPAD_UP;
            Boolean bDpad = false;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    keyMapCode = JOY_KEY_CODE_MAP_DPAD_EAST_WEST;
                    actionDown = JOY_KEY_CODE_MAP_DPAD_EAST_DOWN;
                    bDpad = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    keyMapCode = JOY_KEY_CODE_MAP_DPAD_EAST_WEST;
                    actionDown = JOY_KEY_CODE_MAP_DPAD_WEST_DOWN;
                    bDpad = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    keyMapCode = JOY_KEY_CODE_MAP_DPAD_NORTH_SOUTH;
                    actionDown = JOY_KEY_CODE_MAP_DPAD_NORTH_DOWN;
                    bDpad = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    keyMapCode = JOY_KEY_CODE_MAP_DPAD_NORTH_SOUTH;
                    actionDown = JOY_KEY_CODE_MAP_DPAD_SOUTH_DOWN;
                    bDpad = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_X:
                    keyMapCode = JOY_KEY_CODE_MAP_X;
                    break;
                case KeyEvent.KEYCODE_BUTTON_A:
                    keyMapCode = JOY_KEY_CODE_MAP_A;
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    keyMapCode = JOY_KEY_CODE_MAP_Y;
                    break;
                case KeyEvent.KEYCODE_BUTTON_B:
                    keyMapCode = JOY_KEY_CODE_MAP_B;
                    break;
                case KeyEvent.KEYCODE_BUTTON_START:
                    keyMapCode = JOY_KEY_CODE_MAP_START;
                    break;
                case KeyEvent.KEYCODE_BUTTON_SELECT:
                    keyMapCode = JOY_KEY_CODE_MAP_SELECT;
                    break;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    keyMapCode = JOY_KEY_CODE_MAP_R_ONE;
                    break;
                case KeyEvent.KEYCODE_BUTTON_R2:
                    keyMapCode = JOY_KEY_CODE_MAP_R_TWO;
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    keyMapCode = JOY_KEY_CODE_MAP_L_ONE;
                    break;
                case KeyEvent.KEYCODE_BUTTON_L2:
                    keyMapCode = JOY_KEY_CODE_MAP_L_TWO;
                    break;
                default:
                    Log.e(TAG, "Bluetooth Event : " + event.toString());
                    break;
            }
            int indexSlot = getDeviceSlotIndex(event.getDeviceId());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (bDpad) {
                    sendJoyStickEvent(RTCControllerAndroid.EV_ABS, keyMapCode, actionDown, true, indexSlot);
                } else {
                    sendJoyStickEvent(RTCControllerAndroid.EV_KEY, keyMapCode, 1, true, indexSlot);
                }
                return true;
            } else {
                if (bDpad) {
                    sendJoyStickEvent(RTCControllerAndroid.EV_ABS, keyMapCode, JOY_KEY_CODE_MAP_DPAD_UP, true, indexSlot);
                } else {
                    sendJoyStickEvent(RTCControllerAndroid.EV_KEY, keyMapCode, 0, true, indexSlot);
                }
                return true;
            }
        } else {
            sendAndroidEvent(event.getAction(), event.getKeyCode());
            LogEx.i(keyCode + " " + event + " " + event.getDevice().getId());
            if (event.getDeviceId() == 7 && event.getSource() == 0x301 && keyCode == KeyEvent.KEYCODE_BACK)
                onBackPress();
        }

        return false;
    }

    public static int getDeviceSlotIndex(int deviceId) {
        if (deviceSlot[deviceSlotIndexZero] == deviceId) {
            return deviceSlotIndexZero;
        } else if (deviceSlot[deviceSlotIndexOne] == deviceId) {
            return deviceSlotIndexOne;
        } else if (deviceSlot[deviceSlotIndexZero] == invalidDeviceId) {
            deviceSlot[deviceSlotIndexZero] = deviceId;
            return deviceSlotIndexZero;
        } else if (deviceSlot[deviceSlotIndexOne] == invalidDeviceId) {
            deviceSlot[deviceSlotIndexOne] = deviceId;
            return deviceSlotIndexOne;
        }
        return invalidDeviceId;
    }

    public static int updateDeviceSlot(int deviceId) {
        if (deviceSlot[deviceSlotIndexZero] == deviceId) {
            deviceSlot[deviceSlotIndexZero] = invalidDeviceId;
            return deviceSlotIndexZero;
        } else if (deviceSlot[deviceSlotIndexOne] == deviceId) {
            deviceSlot[deviceSlotIndexOne] = invalidDeviceId;
            return deviceSlotIndexOne;
        }
        return -1;
    }

    private void processJoystickInput(MotionEvent event, int historyPos, int indexDeviceSlot) {
        // Get joystick position.
        // Many game pads with two joysticks report the position of the
        // second
        // joystick
        // using the Z and RZ axes so we also handle those.
        // In a real game, we would allow the user to configure the axes
        // manually.

        InputDevice mInputDevice = event.getDevice();
        int typeX = AXIS_LEFT_X;
        int x = Math.round(getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_X, historyPos) * 128);
        if (x == 0) {
            x = Math.round(getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_HAT_X, historyPos));
            typeX = AXIS_HAT_X;
        }
        if (x == 0) {
            x = Math.round(getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_Z, historyPos) * 128);
            typeX = AXIS_RIGHT_X;
        }

        sendJoyStickEvent(BaseController.EV_ABS, typeX, x, true, indexDeviceSlot);

        int typeY = BaseController.AXIS_LEFT_Y;
        int y = Math.round(getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_Y, historyPos) * 128);
        if (y == 0) {
            y = Math.round(getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_HAT_Y, historyPos));
            typeY = BaseController.AXIS_HAT_Y;
        }
        if (y == 0) {
            y = Math.round(getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_RZ, historyPos) * 128);
            typeY = BaseController.AXIS_RIGHT_Y;
        }
        sendJoyStickEvent(BaseController.EV_ABS, typeY, y, true, indexDeviceSlot);
    }

    private static float getCenteredAxis(MotionEvent event, InputDevice device,
                                         int axis, int historyPos) {
        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());
        if (range != null) {
            final float flat = range.getFlat();
            final float value = historyPos < 0 ? event.getAxisValue(axis)
                    : event.getHistoricalAxisValue(axis, historyPos);

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            // A joystick at rest does not always report an absolute position of
            // (0,0).
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }
}
