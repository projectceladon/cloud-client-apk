package com.intel.gamepad.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.input.InputManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LifecycleEventObserver;

import com.commonlibrary.utils.DensityUtils;
import com.commonlibrary.utils.StatusBarUtil;
import com.google.gson.Gson;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.intel.gamepad.R;
import com.intel.gamepad.app.AppConst;
import com.intel.gamepad.bean.MotionEventBean;
import com.intel.gamepad.controller.impl.DeviceSwitchListener;
import com.intel.gamepad.controller.webrtc.BaseController;
import com.intel.gamepad.controller.webrtc.LatencyTextView;
import com.intel.gamepad.controller.webrtc.RTCControllerAndroid;
import com.intel.gamepad.owt.p2p.P2PHelper;
import com.intel.gamepad.utils.AicVideoCapturer;
import com.intel.gamepad.utils.AudioHelper;
import com.intel.gamepad.utils.CameraEventsHandler;
import com.intel.gamepad.utils.IPUtils;
import com.intel.gamepad.utils.ImageManager;
import com.intel.gamepad.utils.LocationUtils;
import com.intel.gamepad.utils.sink.BweStatsVideoSink;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.GlUtil;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoDecoderFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import owt.base.ActionCallback;
import owt.base.LocalStream;
import owt.base.MediaConstraints;
import owt.base.OwtError;
import owt.p2p.P2PClient;
import owt.p2p.Publication;
import owt.p2p.RemoteStream;


public class PlayGameRtcActivity extends AppCompatActivity implements InputManager.InputDeviceListener, DeviceSwitchListener, SensorEventListener {
    public static AtomicBoolean alpha = new AtomicBoolean(false);
    private static String cameraRes;
    private final String TAG = "PlayGameRtcActivity";
    private final boolean isFirst = false;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private final long TIME_INTERVAL_TO_GET_LOCATION = 1000;
    private final long TIME_INTERVAL_BETWEEN_NETWORK_GPS = TIME_INTERVAL_TO_GET_LOCATION * 10;
    private final String fileTransferPath = Environment.getExternalStorageDirectory().getPath();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final boolean remoteStreamEnded = false;
    DynamicReceiver dynamicReceiver;
    IntentFilter filter;
    private RemoteStream remoteStream = null;
    private LocalStream localAudioStream = null;
    private LocalStream localVideoStream = null;
    private Publication audioPublication = null;
    private Publication videoPublication = null;
    private AicVideoCapturer videoCapture = null;
    private BaseController controller = null;
    private int viewWidth = DensityUtils.getmScreenWidth();
    private int viewHeight = DensityUtils.getmScreenHeight();
    private int screenWidth = viewWidth;
    private int screenHeight = viewHeight;
    private Handler handler = null;
    private CameraEventsHandler mCameraEventsHandler = null;
    private boolean requestPermissionFromServer = false;
    private int satelliteCountCurrent = 0;
    private LocationListener mLocationListenerNetwork = null;
    private LocationListener mLocationListenerGPS = null;
    private GnssStatus.Callback mStatusCallback = null;
    private long lastNetworkLocationTime = 0;
    private long lastGpsLocationTime = 0;
    private SensorManager mSensorManager = null;
    private SurfaceViewRenderer fullRenderer = null;
    private BweStatsVideoSink bweStatsVideoSink = null;
    private File requestFile = null;
    private FileOutputStream fileOutputStream = null;
    private ProgressBar loading;
    private boolean isScreenOrientationPortrait = false;
    private boolean isStreamAdded = false;
    private boolean isOnPause = false;
    private LatencyTextView mLatencyTextView;

    public static void actionStart(Activity act, String controller, int gameId, String gameName) {
        Intent intent = new Intent(act, PlayGameRtcActivity.class);
        intent.putExtra("controller", controller);
        intent.putExtra("gameId", gameId);
        intent.putExtra("gameName", gameName);
        act.startActivity(intent);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (IPUtils.loadPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            isScreenOrientationPortrait = true;
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            isScreenOrientationPortrait = false;
        }
        initUIFeature();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_game_rtc);
        loading = findViewById(R.id.loading);
        fullRenderer = findViewById(R.id.fullRenderer);
        if(fullRenderer!=null){
            fullRenderer.init(P2PHelper.getInst().getRootEglBase().getEglBaseContext(), false, ImageManager.getInstance().getBitmapById(IPUtils.loadPortrait() ? R.mipmap.bg_alpha_portrait : R.mipmap.bg_alpha, getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? 90 : 0), new RendererCommon.RendererEvents() {
                @Override
                public void onFirstFrameRendered() {

                }

                @Override
                public void onFrameResolutionChanged(int width, int height, int rotation) {
                    if (loading != null) {
                        loading.setVisibility(View.GONE);
                    }
                    if (fullRenderer.getVisibility() != View.VISIBLE)
                        fullRenderer.setVisibility(View.VISIBLE);
                }
            });
            fullRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            fullRenderer.setEnableHardwareScaler(true);
            fullRenderer.setZOrderMediaOverlay(true);
        }

        mLatencyTextView = findViewById(R.id.tv_latency);
        mLatencyTextView.init(fullRenderer, getHandler());

        bweStatsVideoSink = new BweStatsVideoSink();
        bweStatsVideoSink.setBweStatsEvent((frameDelay, frameSize, packetsLost) -> executor.execute(() -> {
            mLatencyTextView.addBweStream(frameSize);
            Map<String, Object> mapKey = new HashMap<>();
            Map<String, Object> mapData = new HashMap<>();
            Map<String, Object> mapParams = new HashMap<>();
            mapKey.put("type", "control");
            mapKey.put("data", mapData);
            mapData.put("event", "framestats");
            mapData.put("parameters", mapParams);
            mapParams.put("framets", 0);
            mapParams.put("framesize", frameSize); // Frame size
            mapParams.put("framedelay", frameDelay); // last_duration - start_duration
            mapParams.put("framestartdelay", 0);
            mapParams.put("packetloss", packetsLost);
            String jsonString = new JSONObject(mapKey).toString();
            // Log.e(TAG, "setBweStatsEvent data:" + jsonString);
            P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<>() {
                @Override
                public void onSuccess(Void unused) {
                    // Log.e(TAG, "setBweStatsEvent Success");
                }

                @Override
                public void onFailure(OwtError owtError) {
                    Log.e(TAG, "setBweStatsEvent Failed : " + owtError.errorMessage + " " + owtError.errorCode);
                }
            });
        }));
        executor.execute(() -> {
            if (remoteStream != null && !remoteStreamEnded) {
                remoteStream.attach(bweStatsVideoSink);
                remoteStream.attach(fullRenderer);
            }
        });
        initAudioManager();
        initP2PClient();
        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(outMetrics);
        screenWidth = outMetrics.widthPixels;
        screenHeight = outMetrics.heightPixels;
        controller = selectGamePad();
        onConnectRequest(P2PHelper.serverIP, P2PHelper.peerId, P2PHelper.clientId);
        InputManager mIm = (InputManager) getSystemService(Context.INPUT_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mIm.registerInputDeviceListener(this, null);
        checkPermissions();

        filter = new IntentFilter();
        filter.addAction("com.intel.gamepad.sendfiletoaic");
        filter.addAction("com.intel.gamepad.sendfiletoapp");
        dynamicReceiver = new DynamicReceiver();
        mCameraEventsHandler = new CameraEventsHandler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(dynamicReceiver, filter);
        hideStatusBar();
        if (remoteStream != null) {
            remoteStream.attach(bweStatsVideoSink);
            remoteStream.attach(fullRenderer);
        }
        Log.i(TAG, "RTC Activity onResume called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dynamicReceiver != null) {
            unregisterReceiver(dynamicReceiver);
        }
        if (remoteStream != null) {
            try {
                remoteStream.detach(fullRenderer);
            } catch (Exception e) {
                Log.e(TAG, "remoteStream connect wrong");
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disableLocation();
        ImageManager.getInstance().clear();
        mSensorManager.unregisterListener(this);
        handler.removeMessages(AppConst.MSG_SHOW_CONTROLLER);
        if (mLatencyTextView != null) {
            mLatencyTextView.onStreamExit();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (controller != null) {
                controller.onBackPress();
            }
            P2PHelper.closeP2PClient();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initUIFeature() {
        StatusBarUtil.setTranslucentStatus(this);
        this.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideStatusBar();
    }

    private void hideStatusBar() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams lp = window.getAttributes();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        window.setAttributes(lp);
        View v = window.getDecorView();
        if(v!=null){
            v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

    }

    private void initAudioManager() {
        AudioHelper.getInstance(this);
    }

    private void initP2PClient() {
        getHandler().sendEmptyMessageDelayed(AppConst.MSG_NO_STREAM_ADDED, 1000 * 10);
        P2PHelper.init(this, new P2PClient.P2PClientObserver() {
            @Override
            public void onServerDisconnected() {
                Log.w(TAG, "Server disconnected");
                if (!isNetworkAvailable()) {
                    Log.e(TAG, "Network is unavailable. Quit.");
                    Message.obtain(getHandler(), AppConst.MSG_QUIT, AppConst.EXIT_DISCONNECT).sendToTarget();
                }
                getHandler().sendEmptyMessage(AppConst.MSG_UNRECOVERABLE);
            }

            @Override
            public void onStreamAdded(RemoteStream remoteStream) {
                Log.i(TAG, "onStreamAdded called");
                getHandler().removeMessages(AppConst.MSG_NO_STREAM_ADDED);
                runOnUiThread(() -> {
                    if (!isFirst) fitScreenSize();
                });
                PlayGameRtcActivity.this.remoteStream = remoteStream;
                remoteStream.addObserver(new owt.base.RemoteStream.StreamObserver() {
                    @Override
                    public void onEnded() {
                        isStreamAdded = false;
                        if (BaseController.manuallyPressBackButton.get()) {
                            Log.i(TAG, "remoteStream onEnded(). Manually press back button. Do not reconnect.");
                        } else {
                            getHandler().sendEmptyMessage(AppConst.MSG_UNRECOVERABLE);
                            Log.i(TAG, "remoteStream onEnded(). Try to reconnect...");
                            runOnUiThread(() -> {
                                initP2PClient();
                                onConnectRequest(P2PHelper.serverIP, P2PHelper.peerId, P2PHelper.clientId);
                            });
                        }
                    }

                    @Override
                    public void onUpdated() {
                        Log.i(TAG, " remoteStream updated");
                    }
                });
                executor.execute(() -> {
                    if (fullRenderer != null) {
                        remoteStream.attach(bweStatsVideoSink);
                        remoteStream.attach(fullRenderer);
                    }
                });
                controller.sendAlphaEvent(IPUtils.loadAlphaChannel() ? 1 : 0);
                isStreamAdded = true;
            }

            @Override
            public void onDataReceived(String s, String s1) {
                if (s1.startsWith("{")) {
                    try {
                        JSONObject jsonObject = new JSONObject(s1);
                        jsonObject.isNull("type");
                        if (!jsonObject.isNull("key")) {
                            String key = jsonObject.getString("key");
                            switch (key) {
                                case "gps-start":
                                    runOnUiThread(() -> {
                                        getPositionNetwork();
                                        getPositionGPS();
                                    });
                                    break;
                                case "gps-stop":
                                    runOnUiThread(() -> disableLocation());
                                    break;
                                case "start-audio":
                                case "start-audio-rec": {
                                    Log.d(TAG, "Received start-audio-rec");
                                    Thread thread = new Thread(() -> {
                                        Log.d(TAG, "publishing localAudioStream");
                                        audioPublication = null;
                                        localAudioStream = new LocalStream(new MediaConstraints.AudioTrackConstraints());
                                        localAudioStream.enableAudio();
                                        Log.d(TAG, "localAudioStream id: " + localAudioStream.id());
                                        P2PHelper.getClient().publish(P2PHelper.peerId, localAudioStream, new ActionCallback<>() {
                                            @Override
                                            public void onSuccess(Publication publication) {
                                                audioPublication = publication;
                                                Log.d(TAG, "onSuccess localAudioStream published!!");
                                                audioPublication.addObserver(() -> Log.e(TAG, "audioPublication onEnded "));
                                            }

                                            @Override
                                            public void onFailure(OwtError owtError) {
                                                Log.e(TAG, "onFailure: " + owtError.errorMessage);
                                            }
                                        });
                                    });
                                    thread.start();
                                    break;
                                }
                                case "stop-audio":
                                case "stop-audio-rec":
                                    Log.d(TAG, "Received stop-audio-rec");
                                    Log.d(TAG, "stopping localAudioStream");
                                    if (localAudioStream != null) {
                                        localAudioStream.disableAudio();
                                    }
                                    if (audioPublication != null) {
                                        audioPublication.stop();
                                    }

                                    break;
                                case "start-camera-preview": {
                                    Log.d(TAG, "Received start-camera-preview");
                                    AicVideoCapturer.cameraId = jsonObject.getString("cameraId");
                                    cameraRes = jsonObject.getString("cameraRes");
                                    Log.d(TAG, "cameraId = " + AicVideoCapturer.cameraId + ", cameraRes = " + cameraRes);
                                    Thread thread = new Thread(() -> publishLocalVideo());
                                    thread.start();
                                    break;
                                }
                                case "stop-camera-preview":
                                    Log.d(TAG, "Received stop-camera-preview");
                                    Log.d(TAG, "stopping localVideoStream");
                                    if (localVideoStream != null) {
                                        localVideoStream.disableVideo();
                                    }
                                    if (videoPublication != null) {
                                        videoPublication.stop();
                                    }
                                    if (videoCapture != null) {
                                        videoCapture.stopCapture();
                                    }
                                    break;
                                case "sensor-start":
                                    Log.d(TAG, "Received sensor start");
                                    if (!jsonObject.isNull("type")) {
                                        int type = jsonObject.getInt("type");
                                        int samplingPeriod_ms = jsonObject.getInt("samplingPeriod_ms");
                                        registerSensorEvents(type, samplingPeriod_ms);
                                    }
                                    break;
                                case "sensor-stop":
                                    Log.d(TAG, "Received sensor stop");
                                    if (!jsonObject.isNull("type")) {
                                        int type = jsonObject.getInt("type");
                                        deRegisterSensorEvents(type);
                                    }
                                    break;
                                case "video-alpha-success":
                                    Log.d(TAG, "video-alpha-success");
                                    runOnUiThread(() -> {
                                        controller.switchAlpha(IPUtils.loadAlphaChannel());
                                        GlUtil.setAlphaChannel(IPUtils.loadAlphaChannel());
                                        fullRenderer.requestLayout();
                                        alpha.set(false);
                                    });
                                    break;
                                case "video-alpha-failed":
                                    Log.d(TAG, "video-alpha-failed");
                                    runOnUiThread(() -> {
                                        IPUtils.savealphachannel(!IPUtils.loadAlphaChannel());
                                        controller.switchAlpha(IPUtils.loadAlphaChannel());
                                        alpha.set(false);
                                    });
                                    break;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onDataReceived2(String s, String s1) {
                try {
                    String type = "";
                    JSONObject jsonObject = new JSONObject(s1);
                    if (!jsonObject.isNull("type")) {
                        type = jsonObject.getString("type");
                    }

                    if (!type.equals("control")) {
                        return;
                    }

                    JSONObject data = jsonObject.getJSONObject("data");
                    String event = "";
                    if (!data.isNull("event")) {
                        event = data.getString("event");
                    }

                    if (event.equals("file")) {
                        String msg_id = data.getString("id");
                        JSONObject parameters = data.getJSONObject("parameters");
                        String file_name = parameters.getString("file_name");
                        String indicator = parameters.getString("indicator");
                        switch (indicator) {
                            case "begin":
                                long file_size = Long.parseLong(parameters.getString("file_size"));
                                if (file_size <= 0) {
                                    return;
                                }

                                if (requestFile != null) {
                                    try {
                                        if (fileOutputStream != null) {
                                            fileOutputStream.close();
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    fileOutputStream = null;
                                    requestFile = null;
                                }

                                requestFile = new File(fileTransferPath + "/" + file_name);
                                StringBuilder newFileName;
                                int ii = 1;
                                while (requestFile.exists()) {
                                    int pointIndex = file_name.lastIndexOf(".");
                                    if (pointIndex > 0) {
                                        newFileName = new StringBuilder(file_name.substring(0, pointIndex) + ii + file_name.substring(pointIndex));
                                    } else {
                                        newFileName = new StringBuilder(file_name + ii);
                                    }
                                    requestFile = new File(fileTransferPath + "/" + newFileName);
                                    ii++;
                                }
                                try {
                                    fileOutputStream = new FileOutputStream(requestFile);
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case "end":
                                try {
                                    if (fileOutputStream != null) {
                                        fileOutputStream.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                fileOutputStream = null;
                                long finalFile_size = requestFile.length();
                                runOnUiThread(() -> Toast.makeText(PlayGameRtcActivity.this, "File: " + file_name + " Size: " + finalFile_size + " transfer finished", Toast.LENGTH_LONG).show());
                                requestFile = null;
                                break;
                            case "sending":
                                long block_size = Long.parseLong(parameters.getString("block_size"));
                                if (block_size <= 0) {
                                    return;
                                }

                                String block = parameters.getString("block");
                                byte[] buf;
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                    buf = android.util.Base64.decode(block, android.util.Base64.DEFAULT);
                                } else {
                                    buf = Base64.getDecoder().decode(block);
                                }

                                if (buf.length != block_size) {
                                    Log.e(TAG, "The real size is not same as size in message");
                                }

                                try {
                                    if (fileOutputStream != null) {
                                        fileOutputStream.write(buf);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            default:
                                Log.e(TAG, "Cannot support indicator: " + indicator);
                                break;
                        }

                        Map<String, Object> mapKey = new HashMap<>();
                        Map<String, Object> mapData = new HashMap<>();
                        Map<String, Object> mapDataForFileBegin = new HashMap<>();
                        mapKey.put("type", "control");
                        mapKey.put("data", mapData);
                        mapData.put("event", "file-block-recv-ack");
                        mapData.put("id", msg_id);
                        mapData.put("parameters", mapDataForFileBegin);
                        mapDataForFileBegin.put("file_name", file_name);
                        String jsonString = new JSONObject(mapKey).toString();
                        P2PHelper.getClient().send2(P2PHelper.peerId, jsonString, new ActionCallback<>() {
                            @Override
                            public void onSuccess(Void unused) {
                            }

                            @Override
                            public void onFailure(OwtError owtError) {
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void getPositionNetwork() {
        LocationManager mLocationManagerNetwork = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean isNetworkEnabled = mLocationManagerNetwork.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (isNetworkEnabled) {
            if (mLocationListenerNetwork == null) {
                mLocationListenerNetwork = new LocationListener() {
                    public void onLocationChanged(Location location) {
                        lastNetworkLocationTime = System.currentTimeMillis();
                        if (lastNetworkLocationTime - lastGpsLocationTime > TIME_INTERVAL_BETWEEN_NETWORK_GPS) {
                            String strNMEA = LocationUtils.buildComposedNmeaMessage(location.getLatitude(), location.getLongitude());
                            sendGPSData(strNMEA);
                        }
                    }

                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    public void onProviderEnabled(String provider) {
                    }

                    public void onProviderDisabled(String provider) {
                    }
                };
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
                    requestPermissionFromServer = true;
                } else {
                    isNetworkEnabled = mLocationManagerNetwork.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    if (isNetworkEnabled) {
                        mLocationManagerNetwork.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, TIME_INTERVAL_TO_GET_LOCATION, 0f, mLocationListenerNetwork);
                    }
                }
            }
        }
    }

    private void getPositionGPS() {
        LocationManager mLocationManagerGPS = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = mLocationManagerGPS.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mStatusCallback = new GnssStatus.Callback() {
                    @Override
                    public void onStarted() {
                    }

                    @Override
                    public void onStopped() {
                    }

                    @Override
                    public void onFirstFix(int tiffMillis) {
                    }

                    @Override
                    public void onSatelliteStatusChanged(GnssStatus status) {
                        satelliteCountCurrent = status.getSatelliteCount();
                    }
                };
            }
            OnNmeaMessageListener mOnNmeaMessageListener = (message, timestamp) -> {
                if (satelliteCountCurrent > 0 && message != null && !message.contains("GPGGA,,,,,,")) {
                    if (message.startsWith("$" + "GPGGA") || message.startsWith("$" + "GNGGA") || message.startsWith("$" + "GNRMC") || message.startsWith("$" + "GPRMC")) {

                        lastGpsLocationTime = System.currentTimeMillis();
                    }
                    sendGPSData(message);
                }
            };
            mLocationListenerGPS = new LocationListener() {
                public void onLocationChanged(Location location) {
                    lastNetworkLocationTime = System.currentTimeMillis();
                    if (lastNetworkLocationTime - lastGpsLocationTime > TIME_INTERVAL_BETWEEN_NETWORK_GPS) {
                        String strNMEA = LocationUtils.buildComposedNmeaMessage(location.getLatitude(), location.getLongitude());
                        sendGPSData(strNMEA);
                    }
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
                    requestPermissionFromServer = true;
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        mLocationManagerGPS.addNmeaListener(mOnNmeaMessageListener, null);
                        mLocationManagerGPS.registerGnssStatusCallback(mStatusCallback, null);
                    }
                    mLocationManagerGPS.requestLocationUpdates(LocationManager.GPS_PROVIDER, TIME_INTERVAL_TO_GET_LOCATION, 0f, mLocationListenerGPS);
                }
            }
        }
    }

    private void disableLocation() {
        LocationManager mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationListenerGPS != null) {
            mLocationManager.removeUpdates(mLocationListenerGPS);
        }

        if (mLocationListenerNetwork != null) {
            mLocationManager.removeUpdates(mLocationListenerNetwork);
        }
        if (mStatusCallback != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLocationManager.unregisterGnssStatusCallback(mStatusCallback);
            }
        }
        satelliteCountCurrent = 0;
        requestPermissionFromServer = false;
    }

    private void publishLocalVideo() {
        Log.d(TAG, "publishing localVideoStream.");

        synchronized (CameraEventsHandler.cameraLock) {
            while (!CameraEventsHandler.isCameraSessionClosed) {
                try {
                    CameraEventsHandler.cameraLock.wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error in wait()");
                    e.printStackTrace();
                }
            }
        }

        videoPublication = null;

        // Set resolution based on the user request.
        switch (cameraRes) {
            case "1":
                Log.d(TAG, "Requested for 480p");
                videoCapture = AicVideoCapturer.create(640, 480, mCameraEventsHandler);
                break;
            case "2":
                Log.d(TAG, "Requested for 720p");
                videoCapture = AicVideoCapturer.create(1280, 720, mCameraEventsHandler);
                break;
            case "4":
                Log.d(TAG, "Requested for 1080p");
                videoCapture = AicVideoCapturer.create(1920, 1080, mCameraEventsHandler);
                break;
        }

        localVideoStream = createLocalStream(videoCapture);
        P2PHelper.getClient().publish(P2PHelper.peerId, localVideoStream, new ActionCallback<>() {
            @Override
            public void onSuccess(Publication publication) {
                videoPublication = publication;
                Log.d(TAG, "onSuccess localVideoStream published!!");
                videoPublication.addObserver(() -> Log.e(TAG, "videoPublication onEnded "));
            }

            @Override
            public void onFailure(OwtError owtError) {
                Log.e(TAG, "onFailure: " + owtError.errorMessage);
            }
        });
    }

    private LocalStream createLocalStream(AicVideoCapturer capture) {
        LocalStream localCameraStream = new LocalStream(capture, null);
        Log.d(TAG, "localVideoStream id: " + localCameraStream.id() + " hasVideo: " + localCameraStream.hasVideo());
        return localCameraStream;
    }

    private void registerSensorEvents(int sensorType, int samplingPeriod_ms) {
        if (samplingPeriod_ms <= 20) samplingPeriod_ms = 20;
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(sensorType), samplingPeriod_ms);
    }

    private void deRegisterSensorEvents(int sensorType) {
        Log.d(TAG, "UnRegister sensor events for sensor type: " + sensorType);
        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(sensorType));
    }

    private void fitScreenSize() {
        viewWidth = fullRenderer.getWidth();
        viewHeight = fullRenderer.getHeight();
        sendSizeChange();
        controller.setViewDimension(viewWidth, viewHeight, 0, 0);
    }

    private void sendSizeChange() {
        Map<String, Integer> mapScreenSize = new HashMap<>();
        mapScreenSize.put("width", screenWidth);
        mapScreenSize.put("height", screenHeight);
        Map<String, Integer> mapRenderSize = new HashMap<>();
        mapRenderSize.put("width", viewWidth);
        mapRenderSize.put("height", viewHeight);
        Map<String, Object> mapParams = new HashMap<>();
        mapParams.put("rendererSize", mapRenderSize);
        mapParams.put("screenSize", mapScreenSize);
        mapParams.put("mode", "stretch");
        Map<String, Object> mapData = new HashMap<>();
        mapData.put("event", "sizechange");
        mapData.put("parameters", mapParams);
        Map<String, Object> mapKey = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        JSONObject json = new JSONObject(mapKey);
        String jsonString = json.toString();
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<>() {
            @Override
            public void onFailure(OwtError owtError) {
                Log.e(TAG, owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
            }
        });
    }

    private Handler getHandler() {
        if (handler == null) handler = new GameHandler(Looper.getMainLooper(),this);
        return handler;
    }

    private void showOrHideController() {
        if (controller == null || handler == null) return;
        updateControllerStatus();
        handler.sendEmptyMessageDelayed(AppConst.MSG_SHOW_CONTROLLER, 1000);
    }

    private void updateControllerStatus() {
        if(controller!=null && controller.getView()!=null){
            if ((System.currentTimeMillis() - BaseController.lastTouchMillis) > 10000) {
                controller.getView().setAlpha(0f);
                controller.hide();
            } else {
                controller.getView().setAlpha(1f);
            }
        }
    }

    private BaseController selectGamePad() {
        return new RTCControllerAndroid(this, getHandler(), this);
    }

    private void onConnectRequest(String server, String peerId, String myId) {
        Log.i(TAG, "onConnectRequest called");
        Map<String, String> mapKey = new HashMap<>();
        mapKey.put("host", server);
        mapKey.put("token", myId);
        String jsonLogin = new Gson().toJson(mapKey, mapKey.getClass());
        Log.d(TAG, "jsonLogin: " + jsonLogin);
        P2PClient client = P2PHelper.getClient();
        if (client != null) {
            client.addAllowedRemotePeer(peerId);
            client.connect(jsonLogin, new ActionCallback<>() {
                @Override
                public void onSuccess(String s) {
                    runOnUiThread(() -> onCallRequest(P2PHelper.peerId));
                }

                @Override
                public void onFailure(OwtError owtError) {
                    runOnUiThread(() -> {
                        Log.e(TAG, R.string.connect_failed + owtError.errorMessage);
                        Toast.makeText(PlayGameRtcActivity.this, R.string.connect_failed + owtError.errorMessage, Toast.LENGTH_LONG).show();
                        getHandler().removeMessages(AppConst.MSG_UNRECOVERABLE);
                    });
                }
            });
        }
    }

    private String getCodecProfile() {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> mapParams = new HashMap<>();
        Map<String, Object> mapVersion = new HashMap<>();
        List<Map<String, Object>> listVideoConfigs = new ArrayList<>();
        int defaultFps = 30;

        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "start");
        mapData.put("parameters", mapParams);
        mapParams.put("version", mapVersion);
        mapVersion.put("major", 1);
        mapVersion.put("minor", 1);
        mapParams.put("video-configs", listVideoConfigs);
        //P2PHelper.getVideoEncoderFactory();
        VideoDecoderFactory vdf = P2PHelper.getVideoDecoderFactory();
        if (vdf != null) {
            for (VideoCodecInfo vci : vdf.getSupportedCodecs()) {
                Log.i(TAG, "vci.name = " + vci.name + " vci.params.toString() = " + vci.params.toString());
                if (vci.name.equals("H264") || vci.name.equals("H265")) {
                    Map<String, Object> mapOneVideoConfig = new HashMap<>();
                    mapOneVideoConfig.put("codec", vci.name);
                    if (vci.params.containsKey("profile-level-id")) {
                        String profileLevelId = vci.params.get("profile-level-id");
                        if (vci.name.equals("H264")) {
                            if (profileLevelId != null) {
                                if (profileLevelId.startsWith("42e0")) {
                                    mapOneVideoConfig.put("profile", "baseline");
                                } else if (profileLevelId.startsWith("640c")) {
                                    mapOneVideoConfig.put("profile", "high");
                                }
                            }
                        }
                    } else {
                        if (vci.name.equals("H264")) {
                            mapOneVideoConfig.put("profile", "baseline");
                        } else {
                            mapOneVideoConfig.put("profile", "main"); // H265 default value
                        }
                    }
                    mapOneVideoConfig.put("fps", defaultFps);
                    listVideoConfigs.add(mapOneVideoConfig);
                }
            }
        } else {
            Log.w(TAG, "VideoDecoderFactory is null!");
        }

        String jsonString = new JSONObject(mapKey).toString();
        Log.i(TAG, "getCodecProfile data: " + jsonString);
        return jsonString;
    }

    private void onCallRequest(String peerId) {
        Log.i(TAG, "onCallRequest called");
        P2PClient client = P2PHelper.getClient();
        if (client != null) {
            client.addAllowedRemotePeer(peerId);
            client.stop(peerId);
            String startJsonObject = "start " + getCodecProfile();
            Log.i(TAG, "startJsonObject = '" + startJsonObject + "'");
            String legacyStartCommand = "start";
            Log.i(TAG, "legacyStartCommand = '" + legacyStartCommand + "'");
            Log.i(TAG, "start Command is '" + legacyStartCommand + "'");

            client.send(peerId, legacyStartCommand, new ActionCallback<>() {
                @Override
                public void onSuccess(Void unused) {
                    sendSizeChange();
                    getCameraHwCapability();
                    initJoyStickDevices();
                    sensorsInit();
                    runOnUiThread(() -> getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
                        switch (event) {
                            case ON_RESUME:
                                Log.i(TAG, "webrtc onResume called");
                                if (controller != null) {
                                    if (isOnPause) {
                                        isOnPause = false;
                                        controller.sendAdbCmdEvent("input keyevent KEYCODE_BACK && pm clear com.intel.aic.lifecyclesync");
                                    }
                                }
                                break;
                            case ON_PAUSE:
                                Log.i(TAG, "webrtc onPause called");
                                if (controller != null && !isOnPause) {
                                    isOnPause = true;
                                    controller.sendAdbCmdEvent("am start com.intel.aic.lifecyclesync/com.intel.aic.lifecyclesync.MainActivity");
                                }
                                break;
                            case ON_DESTROY:
                                Log.i(TAG, "webrtc onDestroy called");
                                break;
                        }
                    }));


                }

                @Override
                public void onFailure(OwtError owtError) {
                    getHandler().sendEmptyMessage(AppConst.MSG_UNRECOVERABLE);
                    Log.e(TAG, owtError.errorMessage + " " + owtError.errorCode);
                }
            });
        }
    }

    private void getCameraHwCapability() {
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String[] cameraIds = manager.getCameraIdList();
            String[] camOrientation = new String[cameraIds.length];
            String[] camFacing = new String[cameraIds.length];
            String[] maxCameraRes = new String[cameraIds.length];
            // Camera sensor orientation would be zero always for landscape mode.
            int CAMERA_SENSOR_ORIENTATION_FOR_LANDSCAPE_MODE = 0;

            Log.d(TAG, "Number of cameras available in the HW = " + cameraIds.length);

            for (int i = 0; i < cameraIds.length; i++) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraIds[i]);
                // Update the camera sensor orientation based on screen orientation.
                if (isScreenOrientationPortrait) {
                    camOrientation[i] = Integer.toString(characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
                    Log.d(TAG, "set PORTRAIT orientation for camera Id " + i);
                } else {
                    camOrientation[i] = Integer.toString(CAMERA_SENSOR_ORIENTATION_FOR_LANDSCAPE_MODE);
                    Log.d(TAG, "Set LANDSCAPE orientation for camera Id " + i);
                }
                camFacing[i] = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT ? "front" : "back";

                StreamConfigurationMap map = characteristics.get( CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP );
                if (map == null) {
                    throw new IllegalStateException("Failed to get configuration map: " + cameraIds[i]);
                }
                Size[] supportedSize = map.getOutputSizes(SurfaceTexture.class);
                if (supportedSize != null && supportedSize.length != 0) {
                    int width = supportedSize[0].getWidth();
                    int height = supportedSize[0].getHeight();
                    Log.d(TAG, "width = " + width + ", height = " + height + ", facing = " + camFacing[i] + ", orientation = " + camOrientation[i] + " for Camera Id = " + i);
                    if (width >= 7680 && height >= 4320) maxCameraRes[i] = "4320p"; // 8k
                    else if (width >= 3840 && height >= 2160) maxCameraRes[i] = "2160p"; // 4k
                    else if (width >= 1920 && height >= 1080) maxCameraRes[i] = "1080p";
                    else if (width >= 1280 && height >= 720) maxCameraRes[i] = "720p";
                    else maxCameraRes[i] = "480p";
                    Log.d(TAG, "Max supported camera resolution = " + maxCameraRes[i] + " for Camera Id = " + i);
                }

            }

            Map<String, Object> mapParams = new HashMap<>();
            mapParams.put("numOfCameras", cameraIds.length);
            mapParams.put("camOrientation", camOrientation);
            mapParams.put("camFacing", camFacing);
            mapParams.put("maxCameraRes", maxCameraRes);

            Map<String, Object> mapData = new HashMap<>();
            mapData.put("event", "camerainfo");
            mapData.put("parameters", mapParams);
            Map<String, Object> mapKey = new HashMap<>();
            mapKey.put("type", "control");
            mapKey.put("data", mapData);
            JSONObject json = new JSONObject(mapKey);
            String jsonString = json.toString();
            P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<>() {
                @Override
                public void onFailure(OwtError owtError) {
                    Log.e(TAG, owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
                }
            });
            Log.d(TAG, "Sent camera HW capability info to remote server..");


        }catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void sensorsInit() {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "sensorcheck");
        String jsonString = new JSONObject(mapKey).toString();
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<>() {
            @Override
            public void onFailure(OwtError owtError) {
                Log.e(TAG, owtError.errorMessage + " " + owtError.errorCode + "Failure at sensorsInit");
            }
        });
    }

    private void checkPermissions() {
        Log.d(TAG, "checkPermissions called");
        XXPermissions.with(this)
                .permission(Permission.ACCESS_FINE_LOCATION)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (all) {
                            getPositionNetwork();
                            getPositionGPS();
                            requestPermissionFromServer = false;
                        }
                    }
                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                       if(never){
                           Toast.makeText(PlayGameRtcActivity.this, "The LOCATION has been denied", Toast.LENGTH_LONG).show();
                           requestPermissionFromServer = false;
                       }
                    }
                });

    }

    private void stopLoadingFlash() {
        if (loading != null) {
            loading.setVisibility(View.GONE);
        }
    }

    private void initJoyStickDevices() {
        final int[] devices = InputDevice.getDeviceIds();
        for (int deviceId : devices) {
            InputDevice device = InputDevice.getDevice(deviceId);
            if (device != null) {
                if ((device.getSources() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
                    int joyId = RTCControllerAndroid.getDeviceSlotIndex(deviceId);
                    sendJoyStickEvent(BaseController.EV_NON, 0, 0, true, joyId);
                }
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        if (device != null) {
            int source = device.getSources();
            if ((source & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK || ((source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)) {
                int joyId = RTCControllerAndroid.getDeviceSlotIndex(deviceId);
                sendJoyStickEvent(BaseController.EV_NON, 0, 0, true, joyId);
            } else {
                Log.d(TAG, "Bluetooth Device source:  " + source);
            }
        }
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        int joyId = RTCControllerAndroid.updateDeviceSlot(deviceId);
        if (joyId != -1) {
            sendJoyStickEvent(BaseController.EV_NON, 0, 0, false, joyId);
        } else {
            Log.w(TAG, "This is not joystick: " + deviceId);
        }
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {

    }

    @Override
    public void switchKeyBoard() {

    }

    @Override
    public void switchMapperPad() {

    }

    @Override
    public void switchGamePad() {

    }

    @Override
    public void showDeviceMenu() {

    }

    @Override
    public void switchAlphaOrientation(boolean portrait) {
        if (portrait) {
            fullRenderer.setDrawerBg(ImageManager.getInstance().getBitmapById(R.mipmap.bg_alpha_portrait, getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? 90 : 0));
        } else {
            fullRenderer.setDrawerBg(ImageManager.getInstance().getBitmapById(R.mipmap.bg_alpha, getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? 90 : 0));
        }
    }

    @Override
    public void switchAlpha(CheckBox chkAlpha, boolean state) {
        if (alpha.get()) {
            Log.i(TAG, "alpha is switching");
            return;
        }
        if (isStreamAdded) {
            alpha.set(true);
            IPUtils.savealphachannel(state);
            controller.sendAlphaEvent(IPUtils.loadAlphaChannel() ? 1 : 0);
        } else {
            chkAlpha.setChecked(IPUtils.loadAlphaChannel());
            Toast.makeText(PlayGameRtcActivity.this, "Stream is not added. Do not send video alpha command.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void switchE2E(boolean on) {
        Log.d(TAG, "switchE2E " + on );
        if (on) {
            controller.setE2eEnabled(true);
            mLatencyTextView.open();
        } else {
            controller.setE2eEnabled(false);
            if(mLatencyTextView!=null){
                mLatencyTextView.close();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null) {
            return;
        }
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        Map<String, Object> sensorInfo = new HashMap<>();
        float[] data = new float[6];
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "sensordata");
        mapData.put("parameters", sensorInfo);
        sensorInfo.put("type", event.sensor.getType());
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_GYROSCOPE:
                data[0] = event.values[0];
                data[1] = event.values[1];
                data[2] = event.values[2];
                break;
            case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                data[0] = event.values[0];
                data[1] = event.values[1];
                data[2] = event.values[2];
                data[3] = event.values[3];
                data[4] = event.values[4];
                data[5] = event.values[5];
                break;
            case Sensor.TYPE_LIGHT:
            case Sensor.TYPE_PROXIMITY:
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                data[0] = event.values[0];
                break;
            default:
                return;
        }
        sensorInfo.put("data", data);
        String jsonString = new JSONObject(mapKey).toString();
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<>() {
            @Override
            public void onFailure(OwtError owtError) {
                Log.e(TAG, owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    void sendGPSData(String strNMEA) {
        MotionEventBean meb = new MotionEventBean();
        meb.setType("control");
        meb.setData(new MotionEventBean.DataBean());
        meb.getData().setEvent("gps");
        MotionEventBean.DataBean.ParametersBean parametersBean = new MotionEventBean.DataBean.ParametersBean();
        meb.getData().setParameters(parametersBean);
        MotionEventBean.DataBean.ParametersBean parameters = meb.getData().getParameters();
        if (parameters != null) {
            parameters.setData(strNMEA);
            String jsonString = new Gson().toJson(meb, MotionEventBean.class);
            P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<>() {
                @Override
                public void onFailure(OwtError owtError) {
                    Log.e(TAG, owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
                }
            });
        }
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
            if (BaseController.EV_NON == type) {
                if (enableJoy) {
                    parameters.setData("i\n");
                } else {
                    parameters.setData("p\n");
                }
            } else {
                String data = null;
                if (BaseController.EV_ABS == type) {
                    data = "a " + keyCode + " " + keyValue + "\n";
                } else if (BaseController.EV_KEY == type) {
                    data = "k " + keyCode + " " + keyValue + "\n";
                }
                if (data != null) {
                    parameters.setData(data);
                }
            }

            String jsonString = new Gson().toJson(meb, MotionEventBean.class);
            P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<>() {
                @Override
                public void onFailure(OwtError owtError) {
                    Log.e(TAG, owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
                }
            });
        }
    }

    public static class GameHandler extends Handler {
        private final WeakReference<PlayGameRtcActivity> activity;

        public GameHandler(@NonNull Looper looper, PlayGameRtcActivity act) {
            super(looper);
            activity = new WeakReference<>(act);
        }

        @Override
        public void handleMessage(@NonNull @NotNull Message msg) {
            super.handleMessage(msg);
            PlayGameRtcActivity actPlay = activity.get();
            String RESULT_MSG = "resultMsg";
            String TAG = "GameHandler";
            switch (msg.what) {
                case AppConst.MSG_QUIT:
                    Log.i(TAG, "Exit Result" + msg.arg1);
                    Intent intent = actPlay.getIntent();
                    intent.putExtra(RESULT_MSG, msg.arg1);
                    actPlay.setResult(Activity.RESULT_OK, intent);
                    actPlay.finish();
                    break;
                case AppConst.MSG_SHOW_CONTROLLER:
                    actPlay.showOrHideController();
                    break;
                case AppConst.MSG_UPDATE_CONTROLLER:
                    actPlay.updateControllerStatus();
                    break;
                case AppConst.MSG_NO_STREAM_ADDED:
                    if (BaseController.manuallyPressBackButton.get()) {
                        Log.i(TAG, " Stream is not added. Manually press back button. Do not sent 'Start' again.");
                    } else {
                        Log.i(TAG, " Stream is not added. Sent 'Start' again.");
                        Toast.makeText(actPlay, "Stream is not added. Sent 'Start' again.", Toast.LENGTH_LONG).show();
                        actPlay.stopLoadingFlash();
                        this.sendEmptyMessage(AppConst.MSG_UNRECOVERABLE);
                    }
                    break;
                case AppConst.MSG_UNRECOVERABLE:
                    if (BaseController.manuallyPressBackButton.get()) {
                        Log.i(TAG, " Get MSG_RECOVERABLE message. Manually press back button. Do not popup.");
                    } else {
                        Log.i(TAG, " Get MSG_RECOVERABLE message.");
                        Toast.makeText(actPlay, "Get MSG_RECOVERABLE message.", Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
    }

    class DynamicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: This method is called when the BroadcastReceiver is receiving
            // an Intent broadcast.
            String action = intent.getAction();
            String uri = intent.getStringExtra("uri");
            if(action!=null && uri != null){
                if (action.equals("com.intel.gamepad.sendfiletoaic")) {
                    Log.i("MyReceiver", "To aic uri = " + uri);
                    sendFileToAIC(uri);
                } else if (action.equals("com.intel.gamepad.sendfiletoapp")) {
                    Log.i("MyReceiver", "To app uri = " + uri);
                    sendFileToApp(uri);
                }
            }
        }

        private void sendFileToAIC(String uri) {
            File file = new File(uri);
            if (!file.exists()) {
                runOnUiThread(() -> Toast.makeText(PlayGameRtcActivity.this, "There is no file: " + uri, Toast.LENGTH_LONG).show());
                return;
            }
            long file_length = file.length();
            runOnUiThread(() -> Toast.makeText(PlayGameRtcActivity.this, "File: " + file.getName() + " Size: " + file_length + " start transfer", Toast.LENGTH_LONG).show());
            controller.sendFile(uri);
        }

        private void sendFileToApp(String uri) {
            controller.sendFileNameToStreamer(uri);
        }
    }
}
