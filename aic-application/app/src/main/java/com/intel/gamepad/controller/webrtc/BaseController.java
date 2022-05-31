package com.intel.gamepad.controller.webrtc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Trace;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.commonlibrary.utils.LogEx;
import com.google.gson.Gson;
import com.intel.gamepad.R;
import com.intel.gamepad.activity.PlayGameRtcActivity;
import com.intel.gamepad.app.AppConst;
import com.intel.gamepad.bean.MotionEventBean;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.owt.p2p.P2PHelper;
import com.intel.gamepad.utils.IPUtils;
import com.intel.gamepad.utils.TimeDelayUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import owt.base.ActionCallback;
import owt.base.OwtError;

public abstract class BaseController implements OnTouchListener {
    public final static int EV_NON = 0x00;
    public final static int EV_KEY = 0x01;
    public final static int EV_ABS = 0x03;
    public final static int EV_COMMIT = 0x03;
    public final static int AXIS_LEFT_X = 0;
    public final static int AXIS_LEFT_Y = 1;
    public final static int AXIS_HAT_X = 16;
    public final static int AXIS_HAT_Y = 17;
    public final static int AXIS_RIGHT_X = 2;
    public final static int AXIS_RIGHT_Y = 5;
    public final static int JOY_KEY_CODE_MAP_DPAD_NORTH_SOUTH = 17;
    public final static int JOY_KEY_CODE_MAP_DPAD_EAST_WEST = 16;
    public final static int JOY_KEY_CODE_MAP_DPAD_NORTH_DOWN = -1;
    public final static int JOY_KEY_CODE_MAP_DPAD_SOUTH_DOWN = 1;
    public final static int JOY_KEY_CODE_MAP_DPAD_EAST_DOWN = -1;
    public final static int JOY_KEY_CODE_MAP_DPAD_WEST_DOWN = 1;
    public final static int JOY_KEY_CODE_MAP_DPAD_UP = 0;
    public static final int JOY_KEY_CODE_MAP_X = 307;
    public static final int JOY_KEY_CODE_MAP_Y = 308;
    public static final int JOY_KEY_CODE_MAP_A = 304;
    public static final int JOY_KEY_CODE_MAP_B = 305;
    public static final int JOY_KEY_CODE_MAP_L_ONE = 310;
    public static final int JOY_KEY_CODE_MAP_L_TWO = 312;
    public static final int JOY_KEY_CODE_MAP_R_ONE = 311;
    public static final int JOY_KEY_CODE_MAP_R_TWO = 313;
    public static final int JOY_KEY_CODE_MAP_SELECT = 314;
    public static final int JOY_KEY_CODE_MAP_START = 315;
    public static long lastTouchMillis = 0L;
    public static AtomicBoolean manuallyPressBackButton = new AtomicBoolean(false);
    //    public static boolean isForAndroid = false; // true时发送安卓的原始事件，false发送windows事件
    protected final Context context;
    private final ViewGroup layoutCtrlBox;
    private final Object sendFileLock = new Object();
    protected int marginWidth;
    protected int marginHeight;
    protected boolean showMouse = false;
    protected int mouseX;
    protected int mouseY;
    protected DeviceSwitchListtener devSwitch;
    protected WeakReference<Handler> refHandler;
    private int viewWidth = 0;
    private int viewHeight = 0;
    private ImageView cursor = null;
    private int lastPartition;
    private int nCountInput;
    private boolean send_block_success_ = false;
    private boolean send_block_failed_ = false;
    protected boolean mE2eEnabled = false;

    public BaseController(PlayGameRtcActivity act) {
        this.context = act;
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

    public static float filterMinValue(float value) {
        return (value > 0.1 || value < -0.1) ? value : 0.0f;
    }

    protected abstract String getName();

    public abstract String getDescription();

    public abstract View getView();

    protected abstract void showPopupWindow(View parent);

    public abstract void showPopupOrientation(View parent, boolean open);

    public abstract void switchAlpha(boolean status);

    public abstract void hide();

    public Context getContext() {
        return this.context;
    }

    public void onBackPress() {
        IPUtils.savealphachannel(false);
        sendAlphaEvent(0);
        BaseController.manuallyPressBackButton.set(true);
        Message.obtain(refHandler.get(), AppConst.MSG_QUIT, AppConst.EXIT_NORMAL).sendToTarget();
    }

    /**
     * 初始化返回按钮
     */
    protected void initBackButton(View btnBack) {
        if (btnBack == null) return;
        btnBack.setOnClickListener(view -> {
            onBackPress();
            P2PHelper.closeP2PClient();
        });
    }

    /**
     * 初始化菜单
     */
    protected void initMenuButton(View btnBack) {
        if (btnBack == null) return;
        if (IPUtils.loadTest()) {
            btnBack.setVisibility(View.VISIBLE);
            btnBack.setOnClickListener(view -> BaseController.this.showPopupWindow(btnBack));
        } else {
            btnBack.setVisibility(View.GONE);
        }

    }


    public void setE2eEnabled(boolean enabled) {
        mE2eEnabled = enabled;
    }

    public void sendFile(String fileName) {
        new Thread(() -> {
            send_block_success_ = false;
            send_block_failed_ = false;

            File file = new File(fileName);
            if (!file.exists()) {
                LogEx.d("There is no file");
                return;
            }
            //send header
            Long file_length = file.length();

            Map<String, Object> mapKey = new HashMap<>();
            Map<String, Object> mapData = new HashMap<>();
            Map<String, Object> mapDataForFileBegin = new HashMap<>();
            mapKey.put("type", "control");
            mapKey.put("data", mapData);
            mapData.put("event", "file");
            mapData.put("parameters", mapDataForFileBegin);
            mapDataForFileBegin.put("file_name", file.getName());
            mapDataForFileBegin.put("file_size", String.valueOf(file_length));
            mapDataForFileBegin.put("indicator", "begin");
            String jsonString = new JSONObject(mapKey).toString();
            P2PHelper.getClient().send2(P2PHelper.peerId, jsonString, new ActionCallback<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Log.e("BaseController", "Send begin success");
                    synchronized (sendFileLock) {
                        send_block_success_ = true;
                        sendFileLock.notify();
                    }
                }

                @Override
                public void onFailure(OwtError owtError) {
                    LogEx.e(owtError.errorMessage + " " + owtError.errorCode);
                    synchronized (sendFileLock) {
                        send_block_failed_ = true;
                        sendFileLock.notify();
                    }
                }
            });

            synchronized (sendFileLock) {
                while (!send_block_failed_ && !send_block_success_) {
                    try {
                        sendFileLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (send_block_failed_) {
                    Log.e("BaseController", "Send begin failed, return directly");
                    return;
                }
            }

            int size = 32 * 1024;
            byte[] buf = new byte[size];
            try {
                FileInputStream in = new FileInputStream(file);
                int byteread;
                while ((byteread = in.read(buf)) != -1) {

                    send_block_success_ = false;

                    Log.e("BaseController", "read = " + byteread);
                    Map<String, Object> mapDataForFileContent = new HashMap<>();
                    mapData.put("parameters", mapDataForFileContent);
                    mapDataForFileContent.put("file_name", file.getName());
                    mapDataForFileContent.put("block_size", String.valueOf(byteread));
                    mapDataForFileContent.put("indicator", "sending");

                    byte[] buf_copy = new byte[byteread];
                    System.arraycopy(buf, 0, buf_copy, 0, byteread);
                    String block;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        block = android.util.Base64.encodeToString(buf_copy, android.util.Base64.DEFAULT);
                    } else {
                        block = Base64.getEncoder().encodeToString(buf_copy);
                    }
                    mapDataForFileContent.put("block", block);
                    jsonString = new JSONObject(mapKey).toString();
                    P2PHelper.getClient().send2(P2PHelper.peerId, jsonString, new ActionCallback<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.e("BaseController", "Send one of block success");
                            synchronized (sendFileLock) {
                                send_block_success_ = true;
                                sendFileLock.notify();
                            }
                        }

                        @Override
                        public void onFailure(OwtError owtError) {
                            LogEx.e(owtError.errorMessage + " " + owtError.errorCode);
                            synchronized (sendFileLock) {
                                send_block_failed_ = true;
                                sendFileLock.notify();
                            }
                        }
                    });

                    synchronized (sendFileLock) {
                        while (!send_block_failed_ && !send_block_success_) {
                            try {
                                sendFileLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        if (send_block_failed_) {
                            Log.e("BaseController", "Send block failed, return directly");
                            return;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            send_block_success_ = false;
            Map<String, Object> mapDataForFileEnd = new HashMap<>();
            mapData.put("parameters", mapDataForFileEnd);
            mapDataForFileEnd.put("file_name", file.getName());
            mapDataForFileEnd.put("indicator", "end");
            jsonString = new JSONObject(mapKey).toString();
            P2PHelper.getClient().send2(P2PHelper.peerId, jsonString, new ActionCallback<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Log.e("BaseController", "Send end success");
                    synchronized (sendFileLock) {
                        send_block_success_ = true;
                        sendFileLock.notify();
                    }
                }

                @Override
                public void onFailure(OwtError owtError) {
                    LogEx.e(owtError.errorMessage + " " + owtError.errorCode);
                    synchronized (sendFileLock) {
                        send_block_failed_ = true;
                        sendFileLock.notify();
                    }
                }
            });

            synchronized (sendFileLock) {
                while (!send_block_failed_ && !send_block_success_) {
                    try {
                        sendFileLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
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

    protected void initSwitchAlpha(final CheckBox chkAlpha) {
        if (chkAlpha == null) return;
        chkAlpha.setVisibility(View.VISIBLE);
        IPUtils.savealphachannel(false);
        chkAlpha.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateLastTouchEvent();
            BaseController.this.showPopupOrientation(chkAlpha, isChecked);
            devSwitch.switchAlpha(chkAlpha, !IPUtils.loadAlphaChannel());
        });
    }

    protected void initSwitchE2E(CheckBox chkE2E) {
        if (chkE2E == null) return;
        chkE2E.setVisibility(View.VISIBLE);
        chkE2E.setOnClickListener(v -> devSwitch.switchE2E(chkE2E.isChecked()));
    }

    /**
     * 发送alpha开启事件
     */
    public void sendAlphaEvent(int strCommand) {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "videoalpha");
        mapData.put("parameters", mapParams);
        mapParams.put("action", strCommand);
        String jsonString = new JSONObject(mapKey).toString();
        LogEx.e("sendAlphaEvent data:" + jsonString);
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onSuccess(Void unused) {
                LogEx.e("sendAlphaEvent Success");
            }

            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e("sendAlphaEvent Failed : " + owtError.errorMessage + " " + owtError.errorCode);
            }
        });
    }

    /**
     * Send ADB Cmd Event
     */
    public void sendAdbCmdEvent(String strCommand) {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "cmdchannel");
        mapData.put("parameters", mapParams);
        mapParams.put("cmd", strCommand);
        String jsonString = new JSONObject(mapKey).toString();
        LogEx.e("sendLifeCycleSyncEvent data: " + jsonString);
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onSuccess(Void unused) {
                LogEx.e("sendLifeCycleSyncEvent Success");
            }

            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e("sendLifeCycleSyncEvent Failed: " + owtError.errorMessage + " " + owtError.errorCode);
            }
        });
    }

    protected void addControllerView(ViewGroup viewGroup) {
        this.layoutCtrlBox.removeAllViews();
        this.layoutCtrlBox.addView(viewGroup, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        //Listen for physical mouse events.
        viewGroup.setOnGenericMotionListener((v, event) -> {
            // Determine whether it is a mouse event
            if (event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_BUTTON_PRESS:
                        // Send mouse button down event
                        sendMouseKey(true, 1, event.getX(), event.getY());
                        break;
                    case MotionEvent.ACTION_BUTTON_RELEASE:
                        // Send mouse button up event
                        sendMouseKey(false, 1, event.getX(), event.getY());
                        break;
                    default:
                        // Send mouse button move event
                        sendMouseMotionF(event.getX(), event.getY(), 0, 0);
                        break;
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
                if (v != null) v.setBackgroundResource(R.drawable.bg_oval_btn_press_true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                sendJoyKeyEvent(false, keyCode);
                if (v != null) v.setBackgroundResource(R.drawable.bg_oval_btn_press_false);
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
     * @param data key event
     */
    public void sendKeyEvent(String data) {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "joystick");
        mapData.put("parameters", mapParams);
        mapParams.put("data", data);
        mapParams.put("jID", 0);
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
     * Send mouse event
     *
     * @param pressed true down,false up
     * @param button  1 left 2 medium 3 right
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

    public void sendAndroidEventAsString(String strCommand, long inputTimeStamp) {
        strCommand = strCommand + "c\n";
        MotionEventBean meb = new MotionEventBean();
        meb.setType("control");
        meb.setData(new MotionEventBean.DataBean());
        meb.getData().setEvent("touch");
        MotionEventBean.DataBean.ParametersBean parametersBean = new MotionEventBean.DataBean.ParametersBean();
        meb.getData().setParameters(parametersBean);
        MotionEventBean.DataBean.ParametersBean parameters = meb.getData().getParameters();
        if (parameters != null) {
            parameters.setData(strCommand);
            parameters.settID(0);
            if (inputTimeStamp != -1) {
                parameters.setE2ELatency(inputTimeStamp);
            }
            String jsonString = new Gson().toJson(meb, MotionEventBean.class);
            //LogEx.d(jsonString);
            P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
                @Override
                public void onFailure(OwtError owtError) {
                    LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
                }
            });
        }
    }

    /**
     * 发送安卓端的原始事件信息
     */
    public void sendAndroidEvent(int action, float x, float y, int pointId) {
        MotionEventBean meb = new MotionEventBean();
        meb.setType("control");
        meb.setData(new MotionEventBean.DataBean());
        meb.getData().setEvent("android");
        MotionEventBean.DataBean.ParametersBean parametersBean = new MotionEventBean.DataBean.ParametersBean();
        meb.getData().setParameters(parametersBean);
        MotionEventBean.DataBean.ParametersBean parameters = meb.getData().getParameters();
        if (parameters != null) {
            parameters.setAction(action);
            parameters.setTouchx(x);
            parameters.setTouchy(y);
            parameters.setFingerId(pointId);
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
    }

    public void sendAndroidEvent(int action, int keyCode) {
        MotionEventBean meb = new MotionEventBean();
        meb.setType("control");
        meb.setData(new MotionEventBean.DataBean());
        meb.getData().setEvent("android");
        MotionEventBean.DataBean.ParametersBean parametersBean = new MotionEventBean.DataBean.ParametersBean();
        meb.getData().setParameters(parametersBean);
        MotionEventBean.DataBean.ParametersBean parameters = meb.getData().getParameters();
        if (parameters != null) {
            parameters.setAction(action);
            parameters.setKeycode(keyCode);

            String jsonString = new Gson().toJson(meb, MotionEventBean.class);
            LogEx.d(jsonString);
            P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
                @Override
                public void onFailure(OwtError owtError) {
                    LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
                }
            });
        }
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
        if ((x - 0.2) > 0) x = (float) ((x - 0.2) / (1 - 0.2));
        if ((y - 0.2) > 0) y = (float) ((y - 0.2) / (1 - 0.2));
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
        if ((x - 0.2) > 0) x = (float) ((x - 0.2) / (1 - 0.2));
        if ((y - 0.2) > 0) y = (float) ((y - 0.2) / (1 - 0.2));
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

    public void sendJoyStickEvent(int type, int keyCode, int keyValue, Boolean enableJoy, int joyId) {
        MotionEventBean meb = new MotionEventBean();
        meb.setType("control");
        meb.setData(new MotionEventBean.DataBean());
        meb.getData().setEvent("joystick");
        MotionEventBean.DataBean.ParametersBean parametersBean = new MotionEventBean.DataBean.ParametersBean();
        meb.getData().setParameters(parametersBean);
        MotionEventBean.DataBean.ParametersBean parameters = meb.getData().getParameters();
        if (parameters != null) {
            parameters.setjID(joyId);
            if (EV_NON == type) {
                if (enableJoy) {
                    parameters.setData("i\n");
                } else {
                    parameters.setData("p\n");
                }
            } else {
                String data = null;
                if (EV_ABS == type) {
                    data = "a " + keyCode + " " + keyValue + "\n";
                } else if (EV_KEY == type) {
                    data = "k " + keyCode + " " + keyValue + "\n";
                }
                if (data != null) {
                    parameters.setData(data);
                }
            }

            String jsonString = new Gson().toJson(meb, MotionEventBean.class);
            P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
                @Override
                public void onFailure(OwtError owtError) {
                    LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
                }
            });

            if (EV_NON != type) {
                sendJoyStickEventCommit(EV_COMMIT, true, joyId);
            }
        }
    }

    public void sendJoyStickEventCommit(int type, Boolean enableJoy, int joyId) {
        MotionEventBean meb = new MotionEventBean();
        meb.setType("control");
        meb.setData(new MotionEventBean.DataBean());
        meb.getData().setEvent("joystick");
        MotionEventBean.DataBean.ParametersBean parametersBean = new MotionEventBean.DataBean.ParametersBean();
        meb.getData().setParameters(parametersBean);
        MotionEventBean.DataBean.ParametersBean parameters = meb.getData().getParameters();
        if (parameters != null) {
            parameters.setjID(joyId);
            if (EV_NON == type) {
                if (enableJoy) {
                    parameters.setData("i\n");
                } else {
                    parameters.setData("p\n");
                }
            } else {
                String data = "c\n";
                parameters.setData(data);
            }
            String jsonString = new Gson().toJson(meb, MotionEventBean.class);
            P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
                @Override
                public void onFailure(OwtError owtError) {
                    LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
                }
            });
        }
    }

    public void sendFileNameToStreamer(String fileName) {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapDataForFileBegin = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "file-request");
        mapData.put("parameters", mapDataForFileBegin);
        mapDataForFileBegin.put("file_name", fileName);
        String jsonString = new JSONObject(mapKey).toString();
        P2PHelper.getClient().send2(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
            }
        });
    }

}
