package com.intel.gamepad.owt.p2p;

import static owt.base.MediaCodecs.AudioCodec.OPUS;
import static owt.base.MediaCodecs.VideoCodec.H264;
import static owt.base.MediaCodecs.VideoCodec.H265;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.intel.gamepad.app.MyApp;

import org.webrtc.EglBase;
import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import owt.base.ActionCallback;
import owt.base.AudioCodecParameters;
import owt.base.AudioEncodingParameters;
import owt.base.ContextInitialization;
import owt.base.VideoEncodingParameters;
import owt.p2p.P2PClient;
import owt.p2p.P2PClientConfiguration;

public class P2PHelper {
    public static String serverIP = "http://153.35.78.77:8095/";
    public static String peerId = "s0";
    public static String clientId = "c0";
    public static String stunAddr = "stun:153.35.78.77:3478";
    public static String turnAddrTCP = "turn:153.35.78.77:3478?transport=tcp";
    public static String turnAddrUDP = "turn:153.35.78.77:3478?transport=udp";
    public static String strIP = "153.35.78.77";
    public static String strCoturn = "153.35.78.77";

    private P2PClientConfiguration p2pConfig;
    private P2PClient client;
    private EglBase rootEglBase;
    private boolean inited = false;

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
        getInst().client.disconnect();
    }

    public static void updateP2PServerIP() {
        Pattern pIp = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+):(\\d+)");
        Matcher mIp = pIp.matcher(serverIP);
        if (mIp.find()) {
            strIP = mIp.group(1);
            stunAddr = "stun:" + strIP + ":3478";
            turnAddrTCP = "turn:" + strIP + ":3478?transport=tcp";
            turnAddrUDP = "turn:" + strIP + ":3478?transport=udp";
        }

    }

    public static void updateP2PCoturnIP() {
        stunAddr = "stun:" + strCoturn + ":3478";
        turnAddrTCP = "turn:" + strCoturn + ":3478?transport=tcp";
        turnAddrUDP = "turn:" + strCoturn + ":3478?transport=udp";
    }

    private void initP2PClientConfig() {
        if (!inited) {
            rootEglBase = EglBase.create();
            ContextInitialization.create()
                    .setApplicationContext(MyApp.context)
                    .setVideoHardwareAccelerationOptions(
                            rootEglBase.getEglBaseContext(),
                            rootEglBase.getEglBaseContext())
                    .initialize();
            inited = true;
        }
        VideoEncodingParameters h264 = new VideoEncodingParameters(H264);
        VideoEncodingParameters h265 = new VideoEncodingParameters(H265);
        // VideoEncodingParameters vp8 = new VideoEncodingParameters(VP8);
        // VideoEncodingParameters vp9 = new VideoEncodingParameters(VP9);
        AudioCodecParameters opusCodec = new AudioCodecParameters(OPUS);
        AudioEncodingParameters opus = new AudioEncodingParameters(opusCodec);
        updateP2PCoturnIP();
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        urls.add(stunAddr);
        urls.add(turnAddrTCP);
        urls.add(turnAddrUDP);
        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder(urls).setUsername("username").setPassword("password").createIceServer();
        iceServers.add(iceServer);
        PeerConnection.RTCConfiguration rtcConf = new PeerConnection.RTCConfiguration(iceServers);
        p2pConfig = P2PClientConfiguration.builder()
                // .addVideoParameters(vp8)
                // .addVideoParameters(vp9)
                .addVideoParameters(h264)
                .addVideoParameters(h265)
                .addAudioParameters(opus)
                .setRTCConfiguration(rtcConf)
                .build();
    }

    private void initP2PClient(AppCompatActivity activity, P2PClient.P2PClientObserver observer) {
        client = new P2PClient(p2pConfig, new SocketSignalingChannel());
        client.addObserver(observer);
        activity.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void onDestory() {
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
