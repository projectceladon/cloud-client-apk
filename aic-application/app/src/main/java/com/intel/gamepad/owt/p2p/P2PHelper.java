package com.intel.gamepad.owt.p2p;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;

import com.intel.gamepad.app.MyApp;

import org.webrtc.EglBase;
import org.webrtc.PeerConnection;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import owt.base.ActionCallback;
import owt.base.ContextInitialization;
import owt.p2p.P2PClient;
import owt.p2p.P2PClientConfiguration;

public class P2PHelper {
    private static final String TAG = "P2PHelper";
    public static String serverIP = "http://153.35.78.77:8095/";
    public static String peerId = "s0";
    public static String clientId = "c0";
    public static String stunAddress = "stun:153.35.78.77:3478";
    public static String turnAddressTCP = "turn:153.35.78.77:3478?transport=tcp";
    public static String turnAddressUDP = "turn:153.35.78.77:3478?transport=udp";
    public static String strIP = "153.35.78.77";
    public static String strCoturn = "153.35.78.77";
    private static VideoEncoderFactory mVideoEncoderFactory;
    private static VideoDecoderFactory mVideoDecoderFactory;
    private P2PClientConfiguration p2pConfig;
    private P2PClient client;
    private EglBase rootEglBase;
    private boolean initialized = false;

    private P2PHelper() {
        initP2PClientConfig();
    }

    public static P2PHelper getInst() {
        return Inner.inst;
    }

    public static void init(AppCompatActivity activity, P2PClient.P2PClientObserver observer) {
        getInst().initP2PClientConfig();
        getInst().initP2PClient(activity, observer);
    }

    public static P2PClient getClient() {
        return getInst().client;
    }

    public static void closeP2PClient() {
        getInst().client.stop(peerId);
        getInst().client.removeAllowedRemotePeer(peerId);
        getInst().client.disconnect();
    }

    public static void updateP2PServerIP() {
        Pattern pIp = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+):(\\d+)");
        Matcher mIp = pIp.matcher(serverIP);
        if (mIp.find()) {
            strIP = mIp.group(1);
            stunAddress = "stun:" + strIP + ":3478";
            turnAddressTCP = "turn:" + strIP + ":3478?transport=tcp";
            turnAddressUDP = "turn:" + strIP + ":3478?transport=udp";
        }

    }

    public static void updateP2PCoturnIP() {
        stunAddress = "stun:" + strCoturn + ":3478";
        turnAddressTCP = "turn:" + strCoturn + ":3478?transport=tcp";
        turnAddressUDP = "turn:" + strCoturn + ":3478?transport=udp";
    }

    public static VideoEncoderFactory getVideoEncoderFactory() {
        return mVideoEncoderFactory;
    }

    public static VideoDecoderFactory getVideoDecoderFactory() {
        return mVideoDecoderFactory;
    }

    private void initP2PClientConfig() {
        if (!initialized) {
            rootEglBase = EglBase.create();
            ContextInitialization mContextInitialization = ContextInitialization.create()
                    .setApplicationContext(MyApp.context)
                    .setVideoHardwareAccelerationOptions(
                            rootEglBase.getEglBaseContext(),
                            rootEglBase.getEglBaseContext());
            mContextInitialization.initialize();
            mVideoDecoderFactory = mContextInitialization.getVideoDecoderFactory();
            mVideoEncoderFactory = mContextInitialization.getVideoEncoderFactory();
            initialized = true;
        }
        updateP2PCoturnIP();
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        urls.add(stunAddress);
        urls.add(turnAddressTCP);
        urls.add(turnAddressUDP);
        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder(urls).setUsername("username").setPassword("password").createIceServer();
        iceServers.add(iceServer);
        PeerConnection.RTCConfiguration rtcConf = new PeerConnection.RTCConfiguration(iceServers);

        Log.i(TAG, "Configuring p2pClient with default codecs.");
        p2pConfig = P2PClientConfiguration.builder()
                .setRTCConfiguration(rtcConf)
                .build();
    }

    private void initP2PClient(AppCompatActivity activity, P2PClient.P2PClientObserver observer) {
        client = new P2PClient(p2pConfig, new SocketSignalingChannel());
        client.addObserver(observer);
        activity.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            if(event == Lifecycle.Event.ON_DESTROY){
                client.removeObserver(observer);
                client.stop(P2PHelper.peerId);
                client.disconnect();
                client.removeAllowedRemotePeer(P2PHelper.peerId);
            }
        });
    }

    public EglBase getRootEglBase() {
        return rootEglBase;
    }

    public P2PClientConfiguration getConfig() {
        return p2pConfig;
    }

    private static class Inner {
        private static final P2PHelper inst = new P2PHelper();
    }

    public abstract static class FailureCallBack<T> implements ActionCallback<T> {
        @Override
        public void onSuccess(T t) {
        }
    }
}
