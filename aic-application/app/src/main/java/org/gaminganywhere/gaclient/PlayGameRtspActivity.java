/*
 * Copyright (c) 2013 Chun-Ying Huang
 *
 * This file is part of GamingAnywhere (GA).
 *
 * GA is free software; you can redistribute it and/or modify it
 * under the terms of the 3-clause BSD License as published by the
 * Free Software Foundation: http://directory.fsf.org/wiki/License:BSD_3Clause
 *
 * GA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the 3-clause BSD License along with GA;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.gaminganywhere.gaclient;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.intel.gamepad.app.AppConst;
import com.intel.gamepad.app.MyApp;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.controller.rtsp.*;
import com.intel.gamepad.utils.IPUtils;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import com.mycommonlibrary.utils.ScreenUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import androidx.collection.SimpleArrayMap;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.intel.gamepad.R;
import com.mcxtzhang.commonadapter.rv.CommonAdapter;
import com.mcxtzhang.commonadapter.rv.ViewHolder;
import com.mycommonlibrary.utils.LogEx;
import com.mycommonlibrary.utils.ToastUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class PlayGameRtspActivity extends Activity implements SurfaceHolder.Callback, GLSurfaceView.Renderer, DeviceSwitchListtener {
    public static void actionStart(Activity act, int gameId, String host, int port, String controller) {
        Intent intent = new Intent(act, PlayGameRtspActivity.class);
        intent.putExtra("gameId", gameId);
        intent.putExtra("host", host);
        intent.putExtra("port", port);
        intent.putExtra("builtInAudio", true);
        intent.putExtra("builtInVideo", true);
        intent.putExtra("portraitMode", false);
        intent.putExtra("controller", controller);
        intent.putExtra("dropLateVFrame", -1);
        intent.putExtra("watchdogTimeout", 5);
        act.startActivityForResult(intent, REQUEST_GAME);
        act.overridePendingTransition(0, 0);
    }

    public static void actionStart(Activity act, int gameId, String host, int port) {
        actionStart(act, gameId, host, port, GAControllerMouse.NAME);
    }

    public static final int REQUEST_GAME = 1;
    public static final String RESULT_MSG = "resultMsg";

    private PowerManager.WakeLock mWakeLock = null;
    private GAClient client = null;
    private Surface surface = null;
    private Handler handler = null;
    private boolean builtInVideo = true;
    private boolean builtInAudio = true;
    private int viewWidth = 0;
    private int viewHeight = 0;
    private String host;
    private int port;
    private int watchdogTimeout;
    private int dropLateVideoFrame;
    private int gameId;
    private FrameLayout layoutContainer;
    private GAController controller;
    private SurfaceView contentView;

    public Handler getHandler() {
        if (handler == null) handler = new GameHandler(this);
        return handler;
    }

    private void connect(Surface surface) {
        // create the AndroidClient object
        if (client != null) return;
        client = new GAClient();
        client.setSurface(surface);
        client.setSurfaceView(contentView);
        client.setHandler(handler);

        client.resetConfig();
        client.setProtocol("udp");
        client.setHost(this.host);
        client.setPort(this.port);
        client.setObjectPath("/desktop");
        client.setRTPOverTCP(false);
        client.setCtrlEnable(true);
        client.setCtrlProtocol(false);
        client.setCtrlPort(8555);
        //
        client.setAudioRateChannels(44100, 2);
        client.setBuiltinAudio(builtInAudio);
        client.setBuiltinVideo(builtInVideo);
        //
        client.setDropLateVideoFrame(dropLateVideoFrame > 0 ? dropLateVideoFrame : -1);
        //
        client.startRTSPClient();
        if (watchdogTimeout > 0) {
            client.watchdogSetTimeout(watchdogTimeout);
            client.startWatchdog();
        }
        controller.setClient(client);
    }

    private void disconnect() {
        controller.setClient(null);
        if (client != null) {
            client.stopWatchdog();
            client.stopRTSPClient();
            client.setSurface(null);
            client = null;
        }
    }

    private void getIntentParams() {
        host = getIntent().getStringExtra("host");
        port = getIntent().getIntExtra("port", 8554);
        gameId = getIntent().getIntExtra("gameId", 0);
        builtInVideo = getIntent().getBooleanExtra("builtInVideo", true);
        builtInAudio = getIntent().getBooleanExtra("builtInAudio", true);
        watchdogTimeout = getIntent().getIntExtra("watchdogTimeout", 1);
        dropLateVideoFrame = getIntent().getIntExtra("dropLateVFrame", -1);
        if (TextUtils.isEmpty(host)) {
            ToastUtils.show("未指定IP");
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getIntentParams();

        initPowerManager();// 电源管理
        initScreenOrientation();// 屏幕方向
        windowFullScreen();// 设置窗口全屏显示
        getScreenSize();// 获取屏幕分辨率

        contentView = initSurfaceView(); // 初始化绘图层
        controller = selectGamePad(); // 初始化手柄

        // 实例化一个布局并在右边设置边距，主要是解决手机屏幕圆角的问题
        layoutContainer = new FrameLayout(this);
        int nbHeight = ScreenUtils.getNavigationBarHeight(MyApp.context);
        layoutContainer.setPadding(0, 0, nbHeight, 0);
        // 加载布局
        layoutContainer.addView(contentView);
        layoutContainer.addView(controller.getView());

        setContentView(layoutContainer);
    }


    /**
     * 初始化绘图表面
     */
    private SurfaceView initSurfaceView() {
        if (builtInVideo) {
            contentView = new SurfaceView(this);
            contentView.getHolder().addCallback(this);
            //contentView.setClickable(false);
            surface = contentView.getHolder().getSurface();
            Log.d("ga_log", "Player: use built-in MediaCodec video decoder.");
        } else {
            contentView = new GLSurfaceView(this);
            contentView.getHolder().addCallback(this);
            ((GLSurfaceView) contentView).setRenderer(this);
            ((GLSurfaceView) contentView).setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            Log.d("ga_log", "Player: use ffmpeg video decoer.");
        }
        contentView.setKeepScreenOn(true); // 屏幕长亮
        return contentView;
    }

    /**
     * 获取屏幕的宽和高（单位：像素）
     */
    private void getScreenSize() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        this.viewWidth = displaymetrics.widthPixels;
        this.viewHeight = displaymetrics.heightPixels;
        Log.d("ga_log", String.format("View dimension = %dx%d", viewWidth, viewHeight));
    }

    /**
     * 窗口全屏
     */
    private void windowFullScreen() {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    /**
     * 电源管理
     */
    private void initPowerManager() {
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "GAClient:GaClient");
    }

    /**
     * 设置屏幕方向（横屏/竖屏）
     */
    private void initScreenOrientation() {
        if (!getIntent().getBooleanExtra("portraitMode", false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    /**
     * 初始化游戏手柄
     */
    private GAController selectGamePad() {
        String cname = getIntent().getStringExtra("controller");
        switch (cname) {
            case GAControllerDualPad.NAME:
                controller = new GAControllerDualPad(this);
                break;
            case GAControllerLimbo.NAME:
                controller = new GAControllerLimbo(this);
                break;
            case GAControllerNDS.NAME:
                controller = new GAControllerNDS(this);
                break;
            case GAControllerPadABXY.NAME:
                controller = new GAControllerPadABXY(this);
                break;
            case GAControllerPSP.NAME:
                controller = new GAControllerPSP(this);
                break;
            case GAControllerXBox.NAME:
                controller = new GAControllerXBox(this, getHandler(), this);
                break;
            case GAControllerFPS.NAME:
                controller = new GAControllerFPS(this, getHandler(), this);
                break;
            case GAControllerRAC.NAME:
                controller = new GAControllerRAC(this, getHandler(), this);
                break;
            case GAControllerACT.NAME:
                controller = new GAControllerACT(this, getHandler(), this);
                break;
            case GAControllerMouse.NAME:
                controller = new GAControllerMouse(this, getHandler(), this);
                break;
            default:
                controller = new GAControllerBasic(this);
                break;
        }
        controller.setViewDimension(viewWidth, viewHeight);
        return controller;
    }


    @Override
    public void onBackPressed() {
        this.setResult(AppConst.EXIT_NORMAL);
        super.onBackPressed();
    }

    /**
     * 自定义消息机制，接收来自GAClient的消息后，根据消息的what值做不同的处理
     */
    public static class GameHandler extends Handler {
        private WeakReference<PlayGameRtspActivity> ref;

        GameHandler(PlayGameRtspActivity act) {
            ref = new WeakReference<>(act);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            PlayGameRtspActivity act = ref.get();
            switch (msg.what) {
                case AppConst.MSG_SHOWTOAST:
                    ToastUtils.show(msg.obj.toString());
                    break;
                case AppConst.MSG_QUIT:
                    LogEx.i("Exit Result:" + msg.arg1);
                    act.requestCloseGame();

                    Intent intent = act.getIntent();
                    intent.putExtra(RESULT_MSG, msg.arg1);
                    act.setResult(RESULT_OK, intent);
                    act.finish();
                    break;
                case AppConst.MSG_SHOW_CONTROLLER:
                    act.showOrHideController();
                    break;
                case AppConst.MSG_RENDER:
                    if (!ref.get().builtInVideo) {
                        ((GLSurfaceView) ref.get().contentView).requestRender();
                    }
                    break;
            }
        }
    }

    private void showOrHideController() {
        if (controller == null || handler == null) return;
        if ((System.currentTimeMillis() - GAController.lastTouchMillis) > 10000)
            controller.getView().setAlpha(0f);
        else {
            controller.getView().setAlpha(1f);
        }
        Message m = Message.obtain();
        m.what = AppConst.MSG_SHOW_CONTROLLER;
        handler.sendMessageDelayed(m, 1000);
    }

    @Override
    protected void onResume() {
        if (handler == null) handler = new GameHandler(this);
        if (!builtInVideo) ((GLSurfaceView) contentView).onResume();
        mWakeLock.acquire();
        connect(this.surface);
        super.onResume();
    }

    @Override
    protected void onPause() {
        disconnect();
        mWakeLock.release();
        handler = null;
        if (!builtInVideo) ((GLSurfaceView) contentView).onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 通知服务器关游戏
     */
    private void requestCloseGame() {
        OkGo.<String>get(IPUtils.loadIP() + AppConst.CLOSE_GAME)
                .params("iid", gameId)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        LogEx.i(">>>>>>>>>" + response.body());
                    }
                });
    }

    // surfaceHolder callbacks
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("ga_log", String.format("surface changed: format=%d,%d x %d", format, width, height));
        viewWidth = width;
        viewHeight = height;
        controller.setViewDimension(width, height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("ga_log", "surface created.");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("ga_log", "surface destroyed.");
    }

    // GL
    private int frames = 0;
    private long lastFPSt = 0;

    @Override
    public void onDrawFrame(GL10 gl) {
        long currFPSt = System.currentTimeMillis();
        if (client == null)
            return;
        if (currFPSt - lastFPSt > 10000) {
            if (lastFPSt > 0) {
                Log.d("ga_log", "Render: fps = "
                        + Double.toString(1000.0 * frames / (currFPSt - lastFPSt)));
            }
            lastFPSt = currFPSt;
            frames = 0;
        }
        if (client.GLrender())
            frames++;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d("ga_log", String.format("GL surface changed, width=%d;height=%d", width, height));
        if (client != null)
            client.GLresize(width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d("ga_log", "GL surface created.");
    }

    @Override
    public void switchKeyBoard() {
        layoutContainer.removeView(controller.getView());
        controller = new GAControllerKeyBoard(this, this);
        controller.setViewDimension(viewWidth, viewHeight);
        controller.setClient(client);
        layoutContainer.addView(controller.getView());
    }

    @Override
    public void switchMapperPad() {
        layoutContainer.removeView(controller.getView());
        controller = selectGamePad();
        controller.setViewDimension(viewWidth, viewHeight);
        controller.setClient(client);
        layoutContainer.addView(controller.getView());
    }

    @Override
    public void switchGamePad() {

    }

    @Override
    public void showDeviceMenu() {
        controller.getView().setVisibility(View.GONE);
        // 初始化菜单布局
        View view = LayoutInflater.from(this).inflate(R.layout.game_device_switch, null, false);
        // 实例化对话框并加载布局
        PopupWindow pw = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        initRvController(view, pw);
        view.findViewById(R.id.ibtnExit).setOnClickListener(v -> pw.dismiss());
        // 设置对话框属性
        pw.setOnDismissListener(() -> controller.getView().setVisibility(View.VISIBLE));
        pw.setContentView(view);
        pw.setBackgroundDrawable(new ColorDrawable());
        pw.setOutsideTouchable(true);
        pw.showAtLocation(layoutContainer, Gravity.CENTER, 0, 0);
    }

    private void initRvController(View view, PopupWindow pw) {
        SimpleArrayMap<String, String> mapCtrl = new SimpleArrayMap<>();
        mapCtrl.put(GAControllerXBox.DESC, GAControllerXBox.NAME);
        mapCtrl.put(GAControllerFPS.DESC, GAControllerFPS.NAME);
        mapCtrl.put(GAControllerRAC.DESC, GAControllerRAC.NAME);
        mapCtrl.put(GAControllerACT.DESC, GAControllerACT.NAME);
        mapCtrl.put(GAControllerKeyBoard.DESC, GAControllerKeyBoard.NAME);
        mapCtrl.put(GAControllerMouse.DESC, GAControllerMouse.NAME);

        List<String> listDesc = new ArrayList<>();
        for (int i = 0; i < mapCtrl.size(); i++) {
            listDesc.add(mapCtrl.keyAt(i));
        }

        RecyclerView rvController = view.findViewById(R.id.rvController);
        rvController.setLayoutManager(new LinearLayoutManager(this));
        rvController.setAdapter(new CommonAdapter<String>(this, listDesc, R.layout.item_controller) {
            @Override
            public void convert(ViewHolder vh, String ctrlDesc) {
                vh.setText(R.id.chkController, ctrlDesc);
                if(controller.getDescription() != null) {
                    vh.setChecked(R.id.chkController, controller.getDescription().equals(ctrlDesc));
                }
                vh.setOnClickListener(R.id.chkController, v -> {
                    pw.dismiss();
                    if (ctrlDesc.equals(GAControllerKeyBoard.DESC)) {
                        pw.dismiss();
                        switchKeyBoard();
                    } else {
                        getIntent().putExtra("controller", mapCtrl.get(ctrlDesc));
                        switchMapperPad();
                    }
                });
            }

        });
    }
}
