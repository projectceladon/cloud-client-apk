
package com.intel.gamepad.controller.webrtc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.os.Trace;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.gson.Gson;
import com.intel.gamepad.R;
import com.intel.gamepad.activity.PlayGameRtcActivity;
import com.intel.gamepad.app.AppConst;
import com.intel.gamepad.app.KeyConst;
import com.intel.gamepad.bean.MotionEventBean;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.owt.p2p.P2PHelper;
import com.intel.gamepad.utils.TimeDelayUtils;
import com.mycommonlibrary.utils.LogEx;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import owt.base.OwtError;

public abstract class BaseController implements OnTouchListener {
    //    public static boolean isForAndroid = false; // true时发送安卓的原始事件，false发送windows事件
    private Context context;
    private int viewWidth = 0;
    private int viewHeight = 0;
    protected int marginWidth;
    protected int marginHeight;
    private ViewGroup layoutCtrlBox;
    private ImageView cursor = null;
    protected boolean showMouse = false;
    protected int mouseX;
    protected int mouseY;
    protected DeviceSwitchListtener devSwitch;
    protected WeakReference<Handler> refHandler;
    public static long lastTouchMillis = 0L;
    private int lastPartition;
    private int nCountInput;

    public BaseController(PlayGameRtcActivity act) {
        this.context = act.getApplicationContext();
        this.layoutCtrlBox = act.findViewById(R.id.layoutController);
        BaseController.lastTouchMillis = System.currentTimeMillis();
        sendMouseRelative(false);
    }

    public BaseController(PlayGameRtcActivity act, Handler handler) {
        this(act);
        BaseController.lastTouchMillis = System.currentTimeMillis();
        this.refHandler = new WeakReference<>(handler);
        // MSG_SHOW_CONTROLLER是一个循环发送的消息，用于判断是否需要隐藏控制器界面
        this.refHandler.get().removeMessages(AppConst.MSG_SHOW_CONTROLLER);
        Message.obtain(refHandler.get(), AppConst.MSG_SHOW_CONTROLLER).sendToTarget();
    }

    public BaseController(PlayGameRtcActivity act, Handler handler, DeviceSwitchListtener devSwitch) {
        this(act, handler);
        this.devSwitch = devSwitch;
    }

    protected abstract String getName();

    public abstract String getDescription();

    public abstract View getView();

    public Context getContext() {
        return this.context;
    }

    protected void onBackPress() {
        Message.obtain(refHandler.get(), AppConst.MSG_QUIT, AppConst.EXIT_NORMAL).sendToTarget();
    }

    /**
     * 初始化返回按钮
     */
    protected void initBackButton(View btnBack) {
        if (btnBack == null) return;
        btnBack.setOnClickListener(view -> {
            onBackPress();
        });
    }

    /**
     * 初始化设备切换按钮，点击后会弹界面选择可切换的模拟手柄或键盘
     */
    protected void initSwitchDeviceButton(View btnDevice) {
        if (btnDevice == null) return;
        btnDevice.setOnClickListener(v -> devSwitch.switchKeyBoard());
    }

    protected void initSwitchDPadButton(View btnDevice) {
        if (btnDevice == null) return;
        btnDevice.setOnClickListener(v -> devSwitch.switchGamePad());
    }

    /**
     * 将WASD作为方向键时的事件响应
     */
    protected void emulateWASDKeys(int action, int part) {
        boolean onKeyLeft, onKeyRight, onKeyUp, onKeyDown;
        onKeyUp = onKeyRight = onKeyDown = onKeyLeft = false;
        boolean isPress = false;
        // 根据方向盘的分区号判断需要响应的方向键
        switch (part) {
            case 0:
                onKeyUp = onKeyRight = onKeyDown = onKeyLeft = false;
                break;
            case 12:
            case 1:
                onKeyUp = true;
                onKeyRight = onKeyDown = onKeyLeft = false;
                break;
            case 3:
            case 4:
                onKeyRight = true;
                onKeyUp = onKeyDown = onKeyLeft = false;
                break;
            case 6:
            case 7:
                onKeyDown = true;
                onKeyUp = onKeyRight = onKeyLeft = false;
                break;
            case 9:
            case 10:
                onKeyLeft = true;
                onKeyUp = onKeyRight = onKeyDown = false;
                break;
            case 2:
                onKeyUp = onKeyRight = true;
                onKeyDown = onKeyLeft = false;
                break;
            case 5:
                onKeyRight = onKeyDown = true;
                onKeyUp = onKeyLeft = false;
                break;
            case 8:
                onKeyDown = onKeyLeft = true;
                onKeyUp = onKeyRight = false;
                break;
            case 11:
                onKeyLeft = onKeyUp = true;
                onKeyRight = onKeyDown = false;
                break;
        }
        // 根据action判断是按键是按下还是抬起
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                isPress = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (lastPartition != part) {
                    sendKeyEvent(false, KeyConst.VK_W);
                    sendKeyEvent(false, KeyConst.VK_S);
                    sendKeyEvent(false, KeyConst.VK_A);
                    sendKeyEvent(false, KeyConst.VK_D);
                }
                lastPartition = part;
                isPress = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                isPress = false;
                break;
        }
        // 根据具体方向设置键的按下和抬起状态
        if (onKeyUp) {
            sendKeyEvent(isPress, KeyConst.VK_W);
        }
        if (onKeyDown) {
            sendKeyEvent(isPress, KeyConst.VK_S);
        }
        if (onKeyLeft) {
            sendKeyEvent(isPress, KeyConst.VK_A);
        }
        if (onKeyRight) {
            sendKeyEvent(isPress, KeyConst.VK_D);
        }
        LogEx.i(">>>>>u=" + onKeyUp + " d=" + onKeyDown + " l=" + onKeyLeft + " r=" + onKeyRight + " press=" + isPress);
    }

    /**
     * 上下左右方向键的事件响应
     */
    protected void emulateArrowKeys(int action, int part) {
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
                isPress = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (lastPartition != part) {
                    sendKeyEvent(false, KeyConst.VK_LEFT);
                    sendKeyEvent(false, KeyConst.VK_RIGHT);
                    sendKeyEvent(false, KeyConst.VK_UP);
                    sendKeyEvent(false, KeyConst.VK_DOWN);
                }
                lastPartition = part;
                isPress = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                isPress = false;
                break;
        }
        if (myKeyUp) {
            sendKeyEvent(isPress, KeyConst.VK_UP);
        }
        if (myKeyDown) {
            sendKeyEvent(isPress, KeyConst.VK_DOWN);
        }
        if (myKeyLeft) {
            sendKeyEvent(isPress, KeyConst.VK_LEFT);
        }
        if (myKeyRight) {
            sendKeyEvent(isPress, KeyConst.VK_RIGHT);
        }
        LogEx.i(">>>>>" + myKeyUp + " " + myKeyDown + " " + myKeyLeft + " " + myKeyRight);
    }

    protected void emulateDPadArrowKeys(int action, int part) {
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
                isPress = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (lastPartition != part) {
                    sendJoyKeyEvent(false, KeyConst.VK_DPAD_LEFT);
                    sendJoyKeyEvent(false, KeyConst.VK_DPAD_RIGHT);
                    sendJoyKeyEvent(false, KeyConst.VK_DPAD_UP);
                    sendJoyKeyEvent(false, KeyConst.VK_DPAD_DOWN);
                }
                lastPartition = part;
                isPress = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                isPress = false;
                break;
        }
        if (myKeyUp) {
            sendJoyKeyEvent(isPress, KeyConst.VK_DPAD_UP);
        }
        if (myKeyDown) {
            sendJoyKeyEvent(isPress, KeyConst.VK_DPAD_DOWN);
        }
        if (myKeyLeft) {
            sendJoyKeyEvent(isPress, KeyConst.VK_DPAD_LEFT);
        }
        if (myKeyRight) {
            sendJoyKeyEvent(isPress, KeyConst.VK_DPAD_RIGHT);
        }
        LogEx.i(">>>>>" + myKeyUp + " " + myKeyDown + " " + myKeyLeft + " " + myKeyRight);
    }

    protected void addControllerView(ViewGroup viewGroup) {
        this.layoutCtrlBox.removeAllViews();
        this.layoutCtrlBox.addView(viewGroup,
                new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        // 监听物理鼠标事件
        viewGroup.setOnGenericMotionListener((v, event) -> {
            // 判断是否是鼠标事件
            if (event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE) {
                // 发送鼠标移动事件
                sendMouseMotionF(event.getX(), event.getY(), 0, 0);
                // 发送鼠标点击事件
                if (event.getButtonState() == 0) {
                    sendMouseKey(false, 1, event.getX(), event.getY());
                    sendMouseKey(false, 3, event.getX(), event.getY());
                } else {
                    switch (event.getButtonState()) {
                        case MotionEvent.BUTTON_PRIMARY:
                            sendMouseKey(true, 1, event.getX(), event.getY());
                            break;
                        case MotionEvent.BUTTON_BACK:
                            sendMouseKey(true, 3, event.getX(), event.getY());
                            break;
                    }
                }
                return true;
            }
            return false;
        });
        initMouseCursor();
    }

    private void initMouseCursor() {
        cursor = null;
        cursor = new ImageView(getContext());
        cursor.setImageResource(R.drawable.icon_mouse);
        cursor.setVisibility(View.GONE);
        if (layoutCtrlBox != null) {
            layoutCtrlBox.addView(cursor, new RelativeLayout.LayoutParams(32, 32));
        }
    }

    public void setViewDimenson(int width, int height, int offx, int offy) {
        this.viewWidth = width;
        this.viewHeight = height;
        this.marginWidth = offx;
        this.marginHeight = offy;
    }

    protected int getViewWidth() {
        return viewWidth;
    }

    protected int getViewHeight() {
        return viewHeight;
    }

    protected void moveView(View v, int left, int top, int width, int height) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.leftMargin = left;
        params.topMargin = top;
        v.setLayoutParams(params);
    }

    public boolean handleJoyKeyTouch(int action, int keyCode, View v) {
        LogEx.i(">>>>>" + action + " " + keyCode);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                sendJoyKeyEvent(true, keyCode);
                if (v != null)
                    v.setBackgroundResource(R.drawable.bg_oval_btn_press_true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                sendJoyKeyEvent(false, keyCode);
                if (v != null)
                    v.setBackgroundResource(R.drawable.bg_oval_btn_press_false);
                break;
        }
        return true;
    }

    public boolean handleButtonTouch(int action, int keyCode) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                sendKeyEvent(true, keyCode);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                sendKeyEvent(false, keyCode);
                break;
        }
        return true;
    }

    public boolean handleButtonTouch(int action, int keyCode, View v) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                sendKeyEvent(true, keyCode);
                if (v != null)
                    v.setBackgroundResource(R.drawable.bg_rect_btn_press_true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                sendKeyEvent(false, keyCode);
                if (v != null)
                    v.setBackgroundResource(R.drawable.bg_rect_btn_press_false);
                break;
        }
        return true;
    }

    public boolean handleOvalButtonTouch(int action, int keyCode, View v) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                sendKeyEvent(true, keyCode);
                if (v != null)
                    v.setBackgroundResource(R.drawable.bg_oval_btn_press_true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                sendKeyEvent(false, keyCode);
                if (v != null)
                    v.setBackgroundResource(R.drawable.bg_oval_btn_press_false);
                break;
        }
        return true;
    }

    /**
     * 鼠标单击事件
     *
     * @param delay 按下和释放之间的延时间隔为多少毫秒
     */
    public void mouseSingleClick(int mouseKey, float x, float y, int delay) {
        sendMouseKey(true, mouseKey, x, y);
        if (delay > 0) TimeDelayUtils.sleep(delay);
        sendMouseKey(false, mouseKey, x, y);
    }

    /**
     * 基本事件的鼠标按钮响应
     */
    public boolean handleMouseButtonTouch(int action, int mouseKey, float x, float y, View v) {
        sendMouseButtonDown(action, mouseKey, x, y, v);
        sendMouseButtonUp(action, mouseKey, x, y, v);
        return true;
    }

    public void sendMouseButtonDown(int action, int mouseKey, float x, float y, View v) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
                sendMouseKey(true, mouseKey, x, y);
                if (v != null) v.setBackgroundResource(R.drawable.bg_oval_btn_press_true);
                break;
        }
    }

    public void sendMouseButtonUp(int action, int mouseKey, float x, float y, View v) {
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: // 长按后释放就会触发这此件
                sendMouseKey(false, mouseKey, x, y);
                if (v != null) v.setBackgroundResource(R.drawable.bg_oval_btn_press_false);
                break;
        }
    }

    public void setMouseVisibility(boolean visible) {
        showMouse = visible;
        cursor.setVisibility(showMouse ? View.VISIBLE : View.INVISIBLE);
    }

    public void setMouseCursor(Bitmap cur) {
        if (cur == null) {
            cur = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_mouse);
        }
        cursor.setImageBitmap(cur);
        setMouseVisibility(true);
    }

    public void setMouseData(int x, int y, int curWidth, int curHeight, boolean visible) {
        mouseX = x;
        mouseY = y;
        setMouseVisibility(visible);
        drawCursor(curWidth, curHeight);
    }

    public void drawCursor(int curWidth, int curHeight) {
        if (cursor == null) initMouseCursor();
        cursor.setVisibility(View.VISIBLE);
        moveView(cursor, mouseX + marginWidth, mouseY, curWidth, curHeight);
    }

    /**
     * 发送手柄按键事件
     *
     * @param pressed true按下，false释放
     */
    public void sendJoyKeyEvent(boolean pressed, int keyCode) {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", pressed ? "vjoykeydown" : "vjoykeyup");
        mapData.put("parameters", mapParams);
        mapParams.put("which", keyCode);
        String jsonString = new JSONObject(mapKey).toString();
        LogEx.i(jsonString + " ");
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + jsonString);
            }
        });
    }

    /**
     * 发送键盘按键事件
     *
     * @param pressed true按下，false释放
     */
    public void sendKeyEvent(boolean pressed, int keyCode) {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", pressed ? "keydown" : "keyup");
        mapData.put("parameters", mapParams);
        mapParams.put("which", keyCode);
        String jsonString = new JSONObject(mapKey).toString();
        LogEx.i(jsonString + " ");
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + jsonString);
            }
        });
    }

    /**
     * 发送鼠标按键事件
     *
     * @param pressed true按下,false释放
     * @param button  1左2中3右
     */
    public void sendMouseKey(boolean pressed, int button, float x, float y) {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", pressed ? "mousedown" : "mouseup");
        mapData.put("parameters", mapParams);
        mapParams.put("which", button);
        mapParams.put("x", x - marginWidth);
        mapParams.put("y", y);
        String jsonString = new JSONObject(mapKey).toString();
        LogEx.i(jsonString);
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
            }
        });
    }

    public void sendMouseRelative(boolean relativeMovement) {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "pointerlockchange");
        mapData.put("parameters", mapParams);
        mapParams.put("locked", relativeMovement);
        String jsonString = new JSONObject(mapKey).toString();
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode);
            }
        });
    }


    public void sendMouseMotionF(float x, float y, float movementX, float movementY) {
        x = (x < marginWidth) ? 0f : (x - marginWidth);
        y = y + marginHeight;
        if (x > viewWidth || y > viewHeight) return;
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "mousemove");
        mapData.put("parameters", mapParams);
        mapParams.put("x", x);
        mapParams.put("y", y);
        mapParams.put("movementX", movementX);
        mapParams.put("movementY", movementY);
        String jsonString = new JSONObject(mapKey).toString();
        LogEx.i(">>>++++" + jsonString);
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
            }
        });
    }

    /**
     * 发送安卓端的原始事件信息
     */
    public void sendAndroidEvent(int action, float x, float y) {
        MotionEventBean meb = new MotionEventBean();
        meb.setType("control");
        meb.setData(new MotionEventBean.DataBean());
        meb.getData().setEvent("android");
        meb.getData().setParameters(new MotionEventBean.DataBean.ParametersBean());
        meb.getData().getParameters().setAction(action);
        meb.getData().getParameters().setTouchx(x);
        meb.getData().getParameters().setTouchy(y);

        String jsonString = new Gson().toJson(meb, MotionEventBean.class);
        //LogEx.d(jsonString);
        if (action == MotionEvent.ACTION_UP) {
            nCountInput++;
            Trace.beginSection("atou C1 ID: " + nCountInput + " size: " + 0);
            Trace.endSection();
        }
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
            }
        });
    }

    public void sendAndroidEvent(int action, int keyCode) {
        MotionEventBean meb = new MotionEventBean();
        meb.setType("control");
        meb.setData(new MotionEventBean.DataBean());
        meb.getData().setEvent("android");
        meb.getData().setParameters(new MotionEventBean.DataBean.ParametersBean());
        meb.getData().getParameters().setAction(action);
        meb.getData().getParameters().setKeycode(keyCode);

        String jsonString = new Gson().toJson(meb, MotionEventBean.class);
        LogEx.d(jsonString);
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
            }
        });
    }

    /**
     * 发送鼠标滚轮事件
     */
    public void sendMouseWheel(float dx, float dy, float dz) {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "wheel");
        mapData.put("parameters", mapParams);
        mapParams.put("deltaX", dx);
        mapParams.put("deltaY", dy);
        mapParams.put("daltaZ", dz);
        String jsonString = new JSONObject(mapKey).toString();
        LogEx.i("mouse_wheel dx:$dx,dy:$dy,dz:$dz");
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
            }
        });
    }

    public void sendLeftTrigger(float value) {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "vjoyltrigger");
        mapData.put("parameters", mapParams);
        mapParams.put("trigger", value);
        String jsonString = new JSONObject(mapKey).toString();
        LogEx.i(jsonString);
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
            }
        });
    }

    public void sendRightTrigger(float value) {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "vjoyrtrigger");
        mapData.put("parameters", mapParams);
        mapParams.put("trigger", value);
        String jsonString = new JSONObject(mapKey).toString();
        LogEx.i(jsonString);
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
            }
        });
    }

    public void sendLeftAxisMotion(float x, float y) {
        if ((x - 0.2) > 0)
            x = (float) ((x - 0.2) / (1 - 0.2));
        if ((y - 0.2) > 0)
            y = (float) ((y - 0.2) / (1 - 0.2));
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "vjoylstick");
        mapData.put("parameters", mapParams);
        mapParams.put("lx", x);
        mapParams.put("ly", y);
        String jsonString = new JSONObject(mapKey).toString();
        LogEx.i(jsonString);
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
            }
        });
    }

    public void sendRightAxisMotion(float x, float y) {
        if ((x - 0.2) > 0)
            x = (float) ((x - 0.2) / (1 - 0.2));
        if ((y - 0.2) > 0)
            y = (float) ((y - 0.2) / (1 - 0.2));
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "vjoyrstick");
        mapData.put("parameters", mapParams);
        mapParams.put("rx", x);
        mapParams.put("ry", y);
        String jsonString = new JSONObject(mapKey).toString();
        LogEx.i(jsonString);
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
            }
        });
    }

    /**
     * 更新最后一次触屏时间，并立即发送消息更新
     */
    protected void updateLastTouchEvent() {
        BaseController.lastTouchMillis = System.currentTimeMillis();
        if (refHandler != null && refHandler.get() != null)
            refHandler.get().sendEmptyMessage(AppConst.MSG_UPDATE_CONTROLLER);
    }

    public static float filterMinValue(float value) {
        return (value > 0.1 || value < -0.1) ? value : 0.0f;
    }
}
