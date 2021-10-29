package com.intel.gamepad.activity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.commonlibrary.utils.DensityUtils;
import com.commonlibrary.utils.LogEx;
import com.commonlibrary.utils.StatusBarUtil;
import com.google.gson.Gson;
import com.intel.gamepad.R;
import com.intel.gamepad.app.AppConst;
import com.intel.gamepad.bean.MotionEventBean;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.controller.webrtc.BaseController;
import com.intel.gamepad.controller.webrtc.RTCControllerAndroid;
import com.intel.gamepad.owt.p2p.P2PHelper;
import com.intel.gamepad.utils.AicVideoCapturer;
import com.intel.gamepad.utils.AudioHelper;
import com.intel.gamepad.utils.LocationUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import owt.base.ActionCallback;
import owt.base.LocalStream;
import owt.base.MediaConstraints;
import owt.base.OwtError;
import owt.p2p.P2PClient;
import owt.p2p.Publication;
import owt.p2p.RemoteStream;


public class PlayGameRtcActivity extends AppCompatActivity
        implements InputManager.InputDeviceListener,
        DeviceSwitchListtener,
        SensorEventListener {
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
    private AicVideoCapturer videoCapturer = null;
    private BaseController controller = null;
    private int viewWidth = DensityUtils.getmScreenWidth();
    private int viewHeight = DensityUtils.getmScreenHeight();
    private int screenWidth = viewWidth;
    private int screenHeight = viewHeight;
    private Handler handler = null;
    private boolean requesPermissionFromServer = false;
    private int satelliteCountCurrent = 0;
    private LocationListener mLocationListenerNetwork = null;
    private LocationListener mLocationListenerGPS = null;
    private GnssStatus.Callback mStatusCallback = null;
    private long lastNetworkLocationTime = 0;
    private long lastGpsLocationTime = 0;
    private SensorManager mSensorManager = null;
    private SurfaceViewRenderer fullRenderer = null;
    private File requestFile = null;
    private FileOutputStream fileOutputStream = null;

    public static void actionStart(Activity act, String controller, int gameId, String gameName) {
        Intent intent = new Intent(act, PlayGameRtcActivity.class);
        intent.putExtra("controller", controller);
        intent.putExtra("gameId", gameId);
        intent.putExtra("gameName", gameName);
        act.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initUIFeature();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_game_rtc);
        fullRenderer = findViewById(R.id.fullRenderer);
        fullRenderer.init(P2PHelper.getInst().getRootEglBase().getEglBaseContext(), null);
        fullRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        fullRenderer.setEnableHardwareScaler(true);
        fullRenderer.setZOrderMediaOverlay(true);
        executor.execute(() -> {
            if (remoteStream != null && !remoteStreamEnded) {
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
        checkMediaCodecSupportTypes();

        filter = new IntentFilter();
        filter.addAction("com.intel.gamepad.sendfiletoaic");
        filter.addAction("com.intel.gamepad.sendfiletoapp");
        dynamicReceiver = new DynamicReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(dynamicReceiver, filter);
        hideStatusBar();
        if (remoteStream != null) {
            remoteStream.attach(fullRenderer);
        }
        LogEx.e("RTC Activity onResume called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dynamicReceiver != null) {
            unregisterReceiver(dynamicReceiver);
        }
        if (remoteStream != null) {
            remoteStream.detach(fullRenderer);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disableLocation();
        mSensorManager.unregisterListener(this);
        handler.removeMessages(AppConst.MSG_SHOW_CONTROLLER);
    }

    private void initUIFeature() {
        StatusBarUtil.setTranslucentStatus(this);
        this.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideStatusBar();
    }

    private void hideStatusBar() {
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View v = this.getWindow().getDecorView();
        v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void initAudioManager() {
        AudioHelper.getInstance(this);
    }

    private void initP2PClient() {
        P2PHelper.init(this, new P2PClient.P2PClientObserver() {
            @Override
            public void onServerDisconnected() {
                LogEx.e("服务连接断开");
                if (!isNetworkAvailable()) {
                    Message.obtain(getHandler(), AppConst.MSG_QUIT, AppConst.EXIT_DISCONNECT)
                            .sendToTarget();
                }
            }

            @Override
            public void onStreamAdded(RemoteStream remoteStream) {
                LogEx.e("onStreamAdded called");
                runOnUiThread(() -> {
                    if (!isFirst) fitScreenSize();
                });
                PlayGameRtcActivity.this.remoteStream = remoteStream;
                remoteStream.addObserver(new owt.base.RemoteStream.StreamObserver() {
                    @Override
                    public void onEnded() {
                    }

                    @Override
                    public void onUpdated() {
                        LogEx.e(" remoteStream updated");
                    }
                });
                executor.execute(() -> {
                    if (fullRenderer != null) {
                        remoteStream.attach(fullRenderer);
                    }
                });
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
                                case "start-audio": {
                                    LogEx.d("Received start-audio");
                                    Thread thread = new Thread(() -> {
                                        LogEx.d("publishing localAudioStream");
                                        audioPublication = null;
                                        localAudioStream =
                                                new LocalStream(new MediaConstraints.AudioTrackConstraints());
                                        localAudioStream.enableAudio();
                                        LogEx.d("localAudioStream id: " + localAudioStream.id());
                                        P2PHelper.getClient().publish(P2PHelper.peerId, localAudioStream, new ActionCallback<Publication>() {
                                            @Override
                                            public void onSuccess(Publication publication) {
                                                audioPublication = publication;
                                                LogEx.d("onSuccess localAudioStream published!!");
                                                audioPublication.addObserver(() -> LogEx.e("audioPublication onEnded "));
                                            }

                                            @Override
                                            public void onFailure(OwtError owtError) {
                                                LogEx.e("onFailure: " + owtError.errorMessage);
                                            }
                                        });
                                    });
                                    thread.start();
                                    break;
                                }
                                case "stop-audio":
                                    LogEx.d("Received stop-audio");
                                    LogEx.d("stopping localAudioStream");
                                    if (localAudioStream != null) {
                                        localAudioStream.disableAudio();
                                    }
                                    if (audioPublication != null) {
                                        audioPublication.stop();
                                    }

                                    break;
                                case "start-camera-preview": {
                                    LogEx.d("Received start-camera-preview");
                                    Thread thread = new Thread(() -> publishLocalVideo());
                                    thread.start();
                                    break;
                                }
                                case "stop-camera-preview":
                                    LogEx.d("Received stop-camera-preview");
                                    LogEx.d("stopping localVideoStream");
                                    if (localVideoStream != null) {
                                        localVideoStream.disableVideo();
                                    }
                                    if (videoPublication != null) {
                                        videoPublication.stop();
                                    }
                                    if (videoCapturer != null) {
                                        videoCapturer.stopCapture();
                                    }
                                    break;
                                case "sensor-start":
                                    LogEx.d("Received sensor start");
                                    if (!jsonObject.isNull("sType")) {
                                        int type = jsonObject.getInt("sType");
                                        registerSensorEvents(type);
                                    }
                                    break;
                                case "sensor-stop":
                                    LogEx.d("Received sensor stop");
                                    if (!jsonObject.isNull("sType")) {
                                        int type = jsonObject.getInt("sType");
                                        deRegisterSensorEvents(type);
                                    }
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
                                StringBuilder newFileName = new StringBuilder(file_name);
                                while (requestFile.exists()) {
                                    int pointIndex = newFileName.indexOf(".");
                                    if (pointIndex > 0) {
                                        newFileName = new StringBuilder(newFileName.substring(0, pointIndex) + "1" + newFileName.substring(pointIndex));
                                    } else {
                                        newFileName.append("1");
                                    }
                                    requestFile = new File(fileTransferPath + "/" + newFileName);
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
                        P2PHelper.getClient().send2(P2PHelper.peerId, jsonString, new ActionCallback<Void>() {
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
                            String strNMEA = LocationUtils.buildComposedNmeaMessage(
                                    location.getLatitude(),
                                    location.getLongitude()
                            );
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
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_PERMISSIONS_REQUEST_CODE);
                    requesPermissionFromServer = true;
                } else {
                    isNetworkEnabled =
                            mLocationManagerNetwork.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    if (isNetworkEnabled) {
                        mLocationManagerNetwork.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                TIME_INTERVAL_TO_GET_LOCATION,
                                0f,
                                mLocationListenerNetwork
                        );
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
                    public void onFirstFix(int ttffMillis) {
                    }

                    @Override
                    public void onSatelliteStatusChanged(GnssStatus status) {
                        satelliteCountCurrent = status.getSatelliteCount();
                    }
                };
            }
            OnNmeaMessageListener mOnNmeaMessageListener = (message, timestamp) -> {
                if (satelliteCountCurrent > 0
                        && message != null
                        && !message.contains("GPGGA,,,,,,")
                ) {
                    if (message.startsWith("$" + "GPGGA")
                            || message.startsWith("$" + "GNGGA")
                            || message.startsWith("$" + "GNRMC")
                            || message.startsWith("$" + "GPRMC")
                    ) {

                        lastGpsLocationTime = System.currentTimeMillis();
                    }
                    sendGPSData(message);
                }
            };
            mLocationListenerGPS = new LocationListener() {
                public void onLocationChanged(Location location) {
                    lastNetworkLocationTime = System.currentTimeMillis();
                    if (lastNetworkLocationTime - lastGpsLocationTime > TIME_INTERVAL_BETWEEN_NETWORK_GPS) {
                        String strNMEA = LocationUtils.buildComposedNmeaMessage(
                                location.getLatitude(),
                                location.getLongitude()
                        );
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
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_PERMISSIONS_REQUEST_CODE);
                    requesPermissionFromServer = true;
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        mLocationManagerGPS.addNmeaListener(mOnNmeaMessageListener, null);
                        mLocationManagerGPS.registerGnssStatusCallback(mStatusCallback, null);
                    }
                    mLocationManagerGPS.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            TIME_INTERVAL_TO_GET_LOCATION,
                            0f,
                            mLocationListenerGPS
                    );
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
        requesPermissionFromServer = false;
    }

    private void publishLocalVideo() {
        LogEx.d("publishing localVideoStream.");
        videoPublication = null;
        videoCapturer = AicVideoCapturer.create(640, 480);
        localVideoStream = createLocalStream(videoCapturer);
        P2PHelper.getClient().publish(P2PHelper.peerId,
                localVideoStream,
                new ActionCallback<Publication>() {
                    @Override
                    public void onSuccess(Publication publication) {
                        videoPublication = publication;
                        LogEx.d("onSuccess localVideoStream published!!");
                        videoPublication.addObserver(() -> LogEx.e("videoPublication onEnded "));
                    }

                    @Override
                    public void onFailure(OwtError owtError) {
                        LogEx.e("onFailure: " + owtError.errorMessage);
                    }
                });
    }

    private LocalStream createLocalStream(AicVideoCapturer capturer) {
        LocalStream localCameraStream = new LocalStream(capturer, null);
        LogEx.d("localVideoStream id: " + localCameraStream.id() + " hasVideo: " + localCameraStream.hasVideo());
        return localCameraStream;
    }

    private void registerSensorEvents(int sensorType) {
        Log.d(TAG, "Register sensor events for sensor type: " + sensorType);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(sensorType),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void deRegisterSensorEvents(int sensorType) {
        Log.d(TAG, "UnRegister sensor events for sensor type: " + sensorType);
        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(sensorType));
    }

    private void fitScreenSize() {
        viewWidth = fullRenderer.getWidth();
        viewHeight = fullRenderer.getHeight();
        sendSizeChange();
        controller.setViewDimenson(viewWidth, viewHeight, 0, 0);
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
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
            }
        });
    }

    private Handler getHandler() {
        if (handler == null) handler = new GameHandler(this);
        return handler;
    }

    private void showOrHideController() {
        if (controller == null || handler == null) return;
        updateControllerStatus();
        handler.sendEmptyMessageDelayed(AppConst.MSG_SHOW_CONTROLLER, 1000);
    }

    private void updateControllerStatus() {
        if ((System.currentTimeMillis() - BaseController.lastTouchMillis) > 10000) {
            controller.getView().setAlpha(0f);
        } else {
            controller.getView().setAlpha(1f);
        }
    }

    private BaseController selectGamePad() {
        return new RTCControllerAndroid(this, getHandler(), this);
    }

    private void onConnectRequest(String server, String peerId, String myId) {
        LogEx.e("onConnectRequest called");
        Map<String, String> mapKey = new HashMap<>();
        mapKey.put("host", server);
        mapKey.put("token", myId);
        String jsonLogin = new Gson().toJson(mapKey, mapKey.getClass());
        LogEx.e("jsonLogin: " + jsonLogin);
        P2PClient client = P2PHelper.getClient();
        if (client != null) {
            client.addAllowedRemotePeer(peerId);
            client.connect(jsonLogin, new ActionCallback<String>() {
                @Override
                public void onSuccess(String s) {
                    runOnUiThread(() -> onCallRequest(P2PHelper.peerId));
                }

                @Override
                public void onFailure(OwtError owtError) {
                    runOnUiThread(() -> {
                        Toast.makeText(PlayGameRtcActivity.this, R.string.connect_failed + owtError.errorMessage, Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            });
        }
    }

    private void onCallRequest(String peerId) {
        LogEx.e("onCallRequest called");
        P2PClient client = P2PHelper.getClient();
        if (client != null) {
            client.addAllowedRemotePeer(peerId);
            client.stop(peerId);
            client.send(peerId, "start", new ActionCallback<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    sendSizeChange();
                    initJoyStickDevices();
                    sensorsInit();
                    runOnUiThread(() -> getLifecycle().addObserver(new LifecycleObserver() {
                        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                        public void onDestroy() {
                            LogEx.e(" webrtc onDestroy called");
                        }
                    }));
                }

                @Override
                public void onFailure(OwtError owtError) {
                    LogEx.e(owtError.errorMessage + " " + owtError.errorCode);
                }
            });
        }
    }

    private void sensorsInit() {
        Map<String, Object> mapKey = new HashMap<>();
        Map<String, Object> mapData = new HashMap<>();
        mapKey.put("type", "control");
        mapKey.put("data", mapData);
        mapData.put("event", "sensorcheck");
        String jsonString = new JSONObject(mapKey).toString();
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + "Failure at sensorsInit");
            }
        });
    }

    private void checkPermissions() {
        LogEx.d("checkPermissions called");
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        ArrayList<String> permissionsToAskFor = new ArrayList<>();
        for (String permission : permissions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        permission
                ) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToAskFor.add(permission);
                }
            }
        }
        if (!permissionsToAskFor.isEmpty()) {
            String[] arrayAskfor = new String[permissionsToAskFor.size()];
            arrayAskfor = permissionsToAskFor.toArray(arrayAskfor);
            ActivityCompat.requestPermissions(
                    this,
                    arrayAskfor,
                    REQUEST_PERMISSIONS_REQUEST_CODE
            );
        } else {
            LogEx.d("No need to request permissions to user, as App already has all the required permissions");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // grantResults.length <= 0. If user interaction was interrupted, the permission request is cancelled and you receive empty arrays.
        if (grantResults.length > 0) {
            for (int i = 0; i < permissions.length; i++) {
                if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.checkSelfPermission(this, permissions[i]) == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "The " + permissions[i] + "has been granted", Toast.LENGTH_LONG).show();
                            if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && requesPermissionFromServer) {
                                getPositionNetwork();
                                getPositionGPS();
                                requesPermissionFromServer = false;
                            }
                        } else {
                            Toast.makeText(this, "The " + permissions[i] + " has been denied", Toast.LENGTH_LONG).show();
                            if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION))
                                requesPermissionFromServer = false;
                        }
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        int source = device.getSources();
        if ((source & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                || ((source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)) {
            int joyId = RTCControllerAndroid.getDeviceSlotIndex(deviceId);
            sendJoyStickEvent(BaseController.EV_NON, 0, 0, true, joyId);
        } else {
            Log.d(TAG, "Bluetooth Device source:  " + source);
        }
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        int joyId = RTCControllerAndroid.updateDeviceSlot(deviceId);
        if (joyId != -1) {
            sendJoyStickEvent(BaseController.EV_NON, 0, 0, false, joyId);
        } else {
            Log.d(TAG, "This is not joystick: " + deviceId);
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
        P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
            @Override
            public void onFailure(OwtError owtError) {
                LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
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
            P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
                @Override
                public void onFailure(OwtError owtError) {
                    LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
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
            P2PHelper.getClient().send(P2PHelper.peerId, jsonString, new P2PHelper.FailureCallBack<Void>() {
                @Override
                public void onFailure(OwtError owtError) {
                    LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString);
                }
            });
        }
    }

    private void checkMediaCodecSupportTypes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            boolean bHEVCEncoder = false;
            boolean bHEVCDecoder = false;
            boolean bH264Encoder = false;
            boolean bH264Decoder = false;
            for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) {
                String mediaCodecName = mediaCodecInfo.getName().toLowerCase(Locale.ROOT);
                String TYPE_MEDIA_HEVC = "hevc";
                String TYPE_MEDIA_ENCODER = "encoder";
                if (mediaCodecName.contains(TYPE_MEDIA_HEVC)
                        && mediaCodecName.contains(TYPE_MEDIA_ENCODER)) {
                    bHEVCEncoder = true;
                }
                String TYPE_MEDIA_DECODER = "decoder";
                if (mediaCodecName.contains(TYPE_MEDIA_HEVC)
                        && mediaCodecName.contains(TYPE_MEDIA_DECODER)) {
                    bHEVCDecoder = true;
                }
                String TYPE_MEDIA_H264 = "h264";
                if (mediaCodecName.contains(TYPE_MEDIA_H264)
                        && mediaCodecName.contains(TYPE_MEDIA_ENCODER)) {
                    bH264Encoder = true;
                }
                if (mediaCodecName.contains(TYPE_MEDIA_H264)
                        && mediaCodecName.contains(TYPE_MEDIA_DECODER)) {
                    bH264Decoder = true;
                }
            }

            if (!(bHEVCEncoder && bHEVCDecoder)) {
                Toast.makeText(this, R.string.no_hevc,
                        Toast.LENGTH_LONG).show();
            }

            if (!(bH264Encoder && bH264Decoder)) {
                Toast.makeText(this, R.string.no_h264,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    public static class GameHandler extends Handler {
        private final WeakReference<PlayGameRtcActivity> activity;

        public GameHandler(PlayGameRtcActivity act) {
            activity = new WeakReference<>(act);
        }

        @Override
        public void handleMessage(@NonNull @NotNull Message msg) {
            super.handleMessage(msg);
            PlayGameRtcActivity actPlay = activity.get();
            String RESULT_MSG = "resultMsg";
            switch (msg.what) {
                case AppConst.MSG_QUIT:
                    LogEx.i("Exit Result" + msg.arg1);
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
            }
        }
    }

    class DynamicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: This method is called when the BroadcastReceiver is receiving
            // an Intent broadcast.
            String action = intent.getAction();
            if (action.equals("com.intel.gamepad.sendfiletoaic")) {
                String uri = intent.getStringExtra("uri");
                Log.e("MyReceiver", "To aic uri = " + uri);
                sendFiletoAIC(uri);
            } else if (action.equals("com.intel.gamepad.sendfiletoapp")) {
                String uri = intent.getStringExtra("uri");
                Log.e("MyReceiver", "To app uri = " + uri);
                sendFiletoApp(uri);
            }
        }

        private void sendFiletoAIC(String uri) {
            File file = new File(uri);
            if (!file.exists()) {
                runOnUiThread(() -> Toast.makeText(PlayGameRtcActivity.this, "There is no file: " + uri, Toast.LENGTH_LONG).show());
                return;
            }
            long file_length = file.length();
            runOnUiThread(() -> Toast.makeText(PlayGameRtcActivity.this, "File: " + file.getName() + " Size: " + file_length + " start transfer", Toast.LENGTH_LONG).show());
            controller.sendFile(uri);
        }

        private void sendFiletoApp(String uri) {
            controller.sendFileNameToStreamer(uri);
        }
    }
}
