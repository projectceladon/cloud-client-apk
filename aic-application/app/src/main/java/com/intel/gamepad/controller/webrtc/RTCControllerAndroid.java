package com.intel.gamepad.controller.webrtc;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Trace;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.PopupWindow;

import com.intel.gamepad.R;
import com.intel.gamepad.activity.PlayGameRtcActivity;
import com.intel.gamepad.controller.impl.DeviceSwitchListener;
import com.intel.gamepad.utils.KeyTypeEnum;
import com.intel.gamepad.utils.PopupUtil;

import org.webrtc.JniCommon;

import java.util.Locale;

/**
 * Android触屏专用
 */
public class RTCControllerAndroid extends BaseController implements View.OnGenericMotionListener, View.OnKeyListener {
    public static final String NAME = "ANDROID";
    public static final String DESC = "Android Touch Screen ";
    public static final String TAG = "RTCCTLAndroid";
    public static final int invalidDeviceId = -100;
    public static final int deviceSlotIndexZero = 0;
    public static final int deviceSlotIndexOne = 1;
    public static int[] deviceSlot = {invalidDeviceId, invalidDeviceId};
    private final int[] pointArray = new int[10];
    private ViewGroup vgRoot;
    private int nPointCount = 0;
    private int nCountInput;
    private float rightAxisX = 0f;
    private float rightAxisY = 0f;
    private CheckBox chkAlpha;
    private PopupWindow popupNavigator;
    private PopupWindow popupOrientation;

    public RTCControllerAndroid(PlayGameRtcActivity act, Handler handler, DeviceSwitchListener devSwitch) {
        super(act, handler, devSwitch);
        initRightAxisMotion();
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
        this.vgRoot = (ViewGroup) View.inflate(getContext(),R.layout.game_pad_android,null);
        addControllerView(vgRoot);
        initBackButton(vgRoot.findViewById(R.id.btnBack));
        initMenuButton(vgRoot.findViewById(R.id.btnMenu));
        chkAlpha = vgRoot.findViewById(R.id.chkAlpha);
        initSwitchAlpha(chkAlpha);
        CheckBox chkE2E = vgRoot.findViewById(R.id.chkE2e);
        initSwitchE2E(chkE2E);

        // 这个控件用于接收物理手柄的事件
        View viewTouch = vgRoot.findViewById(R.id.viewMouse);
        viewTouch.requestFocus();
        viewTouch.setFocusable(true);
        viewTouch.setOnGenericMotionListener(this);
        viewTouch.setOnKeyListener(this);
        vgRoot.setOnTouchListener(this);
    }

    @Override
    public void switchAlpha(boolean status) {
        chkAlpha.setChecked(status);
    }

    /**
     * 所有屏幕触摸的回调用
     */
    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        Log.i(TAG, "v = " + v + " evt = " + evt);
        this.updateLastTouchEvent();
        int index;
        float x;
        float y;
        int nRomoteX;
        int nRomoteY;
        int pointId;
        int action = evt.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (action == MotionEvent.ACTION_UP) {
                nCountInput++;
                Trace.beginSection("atou C1 ID: " + nCountInput + " size: " + 0);
                Trace.endSection();
            }
            StringBuilder strCmd = new StringBuilder();
            for (int i = 0; i < nPointCount; i++) {
                strCmd.append("u ").append(pointArray[i]).append("\n");
            }
            sendAndroidEventAsString(strCmd.toString(), -1);
            return true;
        } else {
            nPointCount = evt.getPointerCount();
            if (nPointCount > 10) {
                nPointCount = 10;
            }
            for (int i = 0; i < nPointCount; i++) {
                int pointIdTmp = evt.getPointerId(i);
                pointArray[i] = pointIdTmp;
            }
        }
        if (action == MotionEvent.ACTION_MOVE) {
            int pointerCount = evt.getPointerCount();
            StringBuilder strCmd = new StringBuilder();
            for (int i = 0; i < pointerCount; i++) {
                pointId = evt.getPointerId(i);
                x = evt.getX(i);
                y = evt.getY(i);
                nRomoteX = Math.round((x * BaseController.INPUT_MAX_WIDTH) / v.getWidth());
                nRomoteY = Math.round((y * BaseController.INPUT_MAX_HEIGHT) / v.getHeight());
                strCmd.append("m ").append(pointId).append(" ").append(nRomoteX).append(" ").append(nRomoteY).append(" ").append(255).append("\n");
            }
            sendAndroidEventAsString(strCmd.toString(), -1);
            return true;
        }

        index = evt.getActionIndex();
        pointId = evt.getPointerId(index);
        x = evt.getX(index);
        y = evt.getY(index);
        nRomoteX = Math.round((x * BaseController.INPUT_MAX_WIDTH) / v.getWidth());
        nRomoteY = Math.round((y * BaseController.INPUT_MAX_HEIGHT) / v.getHeight());
        //pressure = (int)evt.getPressure(pointId);
        String strCmd;
        if (action == MotionEvent.ACTION_POINTER_UP) {
            strCmd = "u " + pointId + "\n";
        } else {
            strCmd = "d " + pointId + " " + nRomoteX + " " + nRomoteY + " " + 255 + "\n";
        }
        if (strCmd.startsWith("d")) {
            sendAndroidEventAsString(strCmd, mE2eEnabled ? JniCommon.nativeGetTimeStampNs() : -1);
        } else {
            sendAndroidEventAsString(strCmd, -1);
        }
        v.performClick();
        return true;
    }

    /**
     * 当右摇杆移动时持续发送消息。由于右摇杆被拉着不放时只会发送一次事件，所以这里需要用一个线程循环处理。
     */
    private void initRightAxisMotion() {
        new Thread(() -> {
            while (!onBack) {
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
                int nRomoteX = Math.round((x * BaseController.INPUT_MAX_WIDTH) / v.getWidth());
                int nRomoteY = Math.round((y * BaseController.INPUT_MAX_HEIGHT) / v.getHeight());
                sendAndroidEvent(event.getAction(), nRomoteX, nRomoteY, i);
            }
            float leftAxisX = event.getAxisValue(MotionEvent.AXIS_X);
            float leftAsixY = event.getAxisValue(MotionEvent.AXIS_Y);
            rightAxisX = event.getAxisValue(MotionEvent.AXIS_Z);
            rightAxisY = event.getAxisValue(MotionEvent.AXIS_RZ);
            float axisHatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
            float axisHatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

            leftAxisX = BaseController.filterMinValue(leftAxisX);
            leftAsixY = BaseController.filterMinValue(leftAsixY);
            rightAxisX = BaseController.filterMinValue(rightAxisX);
            rightAxisY = BaseController.filterMinValue(rightAxisY);
            axisHatX = BaseController.filterMinValue(axisHatX);
            axisHatY = BaseController.filterMinValue(axisHatY);

            Log.i(TAG, String.format(Locale.ENGLISH, "%.1f %.1f | %.1f %.1f %.1f %.1f", axisHatX, axisHatY, leftAxisX, leftAsixY, rightAxisX, rightAxisY));
        }
        return false;
    }

    /**
     * 手柄按键事件
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        int eventSource = event.getSource();
        int eventAction = event.getAction();
        if (((eventSource & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
                || ((eventSource & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)) {
            int keyMapCode = -1;
            int actionDown = JOY_KEY_CODE_MAP_DPAD_UP;
            boolean bDpad = false;
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
                    Log.w(TAG, "Bluetooth Event : " + event);
                    break;
            }
            int indexSlot = getDeviceSlotIndex(event.getDeviceId());
            if (eventAction  == MotionEvent.ACTION_DOWN) {
                if (bDpad) {
                    sendJoyStickEvent(RTCControllerAndroid.EV_ABS, keyMapCode, actionDown, true, indexSlot);
                } else {
                    sendJoyStickEvent(RTCControllerAndroid.EV_KEY, keyMapCode, 1, true, indexSlot);
                }
            } else {
                if (bDpad) {
                    sendJoyStickEvent(RTCControllerAndroid.EV_ABS, keyMapCode, JOY_KEY_CODE_MAP_DPAD_UP, true, indexSlot);
                } else {
                    sendJoyStickEvent(RTCControllerAndroid.EV_KEY, keyMapCode, 0, true, indexSlot);
                }
            }
            return true;
        } else if ((eventSource & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) {
            sendKeyEvent("k " + KeyTypeEnum.findValue(event.getKeyCode()) + " " + (eventAction == MotionEvent.ACTION_DOWN ? 1 : 0) + "\nc\n");
            if (event.getDeviceId() == 7 && event.getSource() == 0x301 && keyCode == KeyEvent.KEYCODE_BACK)
                onBackPress();
        } else {
            sendAndroidEvent(eventAction, event.getKeyCode());
            if(event.getDevice()!=null){
                Log.i(TAG, keyCode + " " + event + " " + event.getDevice().getId());
                if (event.getDeviceId() == 7 && event.getSource() == 0x301 && keyCode == KeyEvent.KEYCODE_BACK)
                    onBackPress();
            }
        }

        return false;
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
        if(mInputDevice!=null){
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
    }

    @Override
    public void showPopupWindow(View parent) {
        try{
            if (popupNavigator != null) {
                popupNavigator.dismiss();
            }
            if(parent == null){
                return;
            }
            View popView = getView().inflate(getContext(), R.layout.popup_window, null);
            popView.setAlpha(0.8f);
            popupNavigator = PopupUtil.createPopup(parent, popView, -1);
            if(popView.findViewById(R.id.back)!=null){
                popView.findViewById(R.id.back).setOnClickListener(v -> clickMenu("input keyevent KEYCODE_BACK"));
            }
            if(popView.findViewById(R.id.home)!=null){
                popView.findViewById(R.id.home).setOnClickListener(v -> clickMenu("input keyevent KEYCODE_HOME"));
            }
            if(popView.findViewById(R.id.app_switch)!=null){
                popView.findViewById(R.id.app_switch).setOnClickListener(v -> clickMenu("input keyevent KEYCODE_APP_SWITCH"));
            }
            if(popView.findViewById(R.id.close)!=null){
                popView.findViewById(R.id.close).setOnClickListener(v -> popupNavigator.dismiss());
            }
            popView.setOnTouchListener(new View.OnTouchListener() {
                int orgX, orgY;
                int offsetX, offsetY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            orgX = (int) event.getX();
                            orgY = (int) event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            popView.setAlpha(0.2f);
                            offsetX = (int) event.getRawX() - orgX;
                            offsetY = (int) event.getRawY() - orgY;
                            popupNavigator.update(offsetX, offsetY, -1, -1, true);
                            break;
                        case MotionEvent.ACTION_UP:
                            popView.setAlpha(0.8f);
                            break;
                    }
                    v.performClick();
                    return true;
                }
            });
        }catch (Exception e){

        }
    }

    @Override
    public void showPopupOrientation(View parent, boolean open) {
        if (open) {
            View popView = getView().inflate(getContext(), R.layout.popup_orientation_window, null);
            popView.setAlpha(0.8f);
            popupOrientation = PopupUtil.createPopup(parent, popView, -1);
            CheckBox chkLandscape = popView.findViewById(R.id.chk_landscape);
            CheckBox chkPortrait = popView.findViewById(R.id.chk_portrait);
            if(popView.findViewById(R.id.close)!=null){
                popView.findViewById(R.id.close).setOnClickListener(v -> {
                    if (popupOrientation != null) {
                        popupOrientation.dismiss();
                    }
                });
            }
            if(chkLandscape!=null && chkPortrait!=null){
                if (((Activity) context).getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    chkLandscape.setChecked(false);
                    chkPortrait.setChecked(true);
                    chkPortrait.setClickable(false);
                } else {
                    chkLandscape.setChecked(true);
                    chkPortrait.setChecked(false);
                    chkLandscape.setClickable(false);
                }
                chkLandscape.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    updateLastTouchEvent();
                    if (isChecked) {
                        chkPortrait.setChecked(false);
                        chkPortrait.setClickable(true);
                        devSwitch.switchAlphaOrientation(false);
                    } else {
                        chkPortrait.setChecked(true);
                        chkPortrait.setClickable(false);
                        devSwitch.switchAlphaOrientation(true);
                    }
                });
                chkPortrait.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        chkLandscape.setChecked(false);
                        chkLandscape.setClickable(true);
                    } else {
                        chkLandscape.setChecked(true);
                        chkLandscape.setClickable(false);
                    }
                });
            }
        } else {
            if (popupOrientation != null) {
                popupOrientation.dismiss();
            }
        }
    }

    @Override
    public void hide() {
        if (popupOrientation != null) {
            popupOrientation.dismiss();
        }
    }

    private void clickMenu(String cmd) {
        sendAdbCmdEvent(cmd);
    }


}
