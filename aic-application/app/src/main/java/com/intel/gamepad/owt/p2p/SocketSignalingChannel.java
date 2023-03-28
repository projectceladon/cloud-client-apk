/* Copyright (C) 2021 Intel Corporation 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *   
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * SPDX-License-Identifier: Apache-2.0
 */


package com.intel.gamepad.owt.p2p;

import static owt.p2p.OwtP2PError.P2P_CLIENT_ILLEGAL_ARGUMENT;
import static owt.p2p.OwtP2PError.P2P_CONN_SERVER_UNKNOWN;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter.Listener;
import okhttp3.OkHttpClient;
import owt.base.ActionCallback;
import owt.base.OwtError;
import owt.p2p.OwtP2PError;
import owt.p2p.SignalingChannelInterface;

/**
 * Socket.IO implementation of P2P signaling channel.
 */
public class SocketSignalingChannel implements SignalingChannelInterface {
    private static final String TAG = "OWT-SocketClient";
    static SSLContext sslContext;
    static HostnameVerifier hostnameVerifier;
    private final static String CLIENT_CHAT_TYPE = "owt-message";
    private final static int MAX_RECONNECT_ATTEMPTS = 5;
    private int reconnectAttempts = 0;
    private final Listener onReconnectingCallback = args -> reconnectAttempts++;
    private Socket socketIOClient;
    private List<SignalingChannelObserver> signalingChannelObservers;
    private final Listener onDisconnectCallback = args -> {
        for (SignalingChannelObserver observer : signalingChannelObservers) {
            observer.onServerDisconnected();
        }
    };
    private final Listener onForceDisconnectCallback = args -> {
        if (socketIOClient != null) {
            socketIOClient.on(Socket.EVENT_DISCONNECT, onDisconnectCallback);
            socketIOClient.io().reconnection(false);
        }
    };
    private final Listener onMessageCallback = args -> {
        JSONObject argumentJsonObject = (JSONObject) args[0];
        for (SignalingChannelObserver observer : signalingChannelObservers) {
            try {
                observer.onMessage(argumentJsonObject.getString("from"),
                        argumentJsonObject.getString("data"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
    private ActionCallback<String> connectCallback;
    // Socket.IO events.
    private final Listener onConnectErrorCallback = args -> {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            if (connectCallback != null) {
                connectCallback.onFailure(
                        new OwtError(P2P_CONN_SERVER_UNKNOWN.value, "connect failed"));
                connectCallback = null;
            } else {
                for (SignalingChannelObserver observer : signalingChannelObservers) {
                    observer.onServerDisconnected();
                }
            }
            reconnectAttempts = 0;
        }
    };
    private final Listener onErrorCallback = args -> {
        if (connectCallback != null) {
            Pattern pattern = Pattern.compile("[0-9]*");
            if (pattern.matcher(args[0].toString()).matches()) {
                connectCallback.onFailure(
                        new OwtError(OwtP2PError.get(Integer.parseInt((String) args[0])).value,
                                "Server error"));
            } else {
                connectCallback.onFailure(new OwtError(args[0].toString()));
            }
        }
    };
    // P2P server events.
    private final Listener onServerAuthenticatedCallback = args -> {
        if (connectCallback != null) {
            connectCallback.onSuccess(args[0].toString());
            connectCallback = null;
        }
    };

    public SocketSignalingChannel() {
        this.signalingChannelObservers = new ArrayList<>();
    }

    @Override
    public void addObserver(SignalingChannelObserver observer) {
        this.signalingChannelObservers.add(observer);
    }

    @Override
    public void removeObserver(SignalingChannelObserver observer) {
        this.signalingChannelObservers.remove(observer);
    }

    @Override
    public void connect(String userInfo, ActionCallback<String> callback) {
        JSONObject loginObject;
        String token;
        String url;
        try {
            connectCallback = callback;
            loginObject = new JSONObject(userInfo);
            token = URLEncoder.encode(loginObject.getString("token"), "UTF-8");
            url = loginObject.getString("host");
            String CLIENT_TYPE_VALUE = "Android";
            String CLIENT_VERSION = "&clientVersion=";
            //Const.CLIENT_VERSION;
            String CLIENT_VERSION_VALUE = "4.2";
            String CLIENT_TYPE = "&clientType=";
            url += "?token=" + token + CLIENT_TYPE + CLIENT_TYPE_VALUE + CLIENT_VERSION
                    + CLIENT_VERSION_VALUE;
            if (!isValid(url)) {
                callback.onFailure(new OwtError(P2P_CLIENT_ILLEGAL_ARGUMENT.value, "Invalid URL"));
                return;
            }
            IO.Options opt = new IO.Options();
            opt.forceNew = true;
            opt.reconnection = true;
            opt.reconnectionAttempts = MAX_RECONNECT_ATTEMPTS;
            if (url.contains("https")) {
                Log.d(TAG, "url: " + url);
                opt.secure = true;
                if (socketIOClient != null) {
                    Log.d(TAG, "stop reconnecting the former url");
                    socketIOClient.disconnect();
                }
                OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
                hostnameVerifier = (hostname, session) -> hostname != null && session != null;

                TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                        checkTrusted(chain, authType);
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                        checkTrusted(chain, authType);
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }};

                try {
                    sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, trustManagers, null);
                } catch (Exception e) {
                    if (callback != null) {
                        callback.onFailure(new OwtError(P2P_CLIENT_ILLEGAL_ARGUMENT.value, e.getMessage()));
                    }
                }

                if (sslContext != null) {
                    clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0]);
                }
                if (hostnameVerifier != null) {
                    clientBuilder.hostnameVerifier(hostnameVerifier);
                }
                OkHttpClient httpClient = clientBuilder.build();
                opt.callFactory = httpClient;
                opt.webSocketFactory = httpClient;
            }
            socketIOClient = IO.socket(url, opt);

            String FORCE_DISCONNECT = "server-disconnect";
            String SERVER_AUTHENTICATED = "server-authenticated";
            socketIOClient.on(Socket.EVENT_CONNECT_ERROR, onConnectErrorCallback)
                    .on(Socket.EVENT_ERROR, onErrorCallback)
                    .on(Socket.EVENT_RECONNECTING, onReconnectingCallback)
                    .on(CLIENT_CHAT_TYPE, onMessageCallback)
                    .on(SERVER_AUTHENTICATED, onServerAuthenticatedCallback)
                    .on(FORCE_DISCONNECT, onForceDisconnectCallback);

            socketIOClient.connect();
        } catch (JSONException | URISyntaxException | UnsupportedEncodingException e) {
            if (callback != null) {
                callback.onFailure(new OwtError(P2P_CLIENT_ILLEGAL_ARGUMENT.value, e.getMessage()));
            }
        }
    }

    private void checkTrusted(X509Certificate[] chain, String authType){
        if (chain == null || chain.length<=0 || authType==null) {
            Log.v(TAG,"checkServerTrusted failed");
        }
    }

    private boolean isValid(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getPort() <= 65535;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public void disconnect() {
        if (socketIOClient != null) {
            Log.d(TAG, "Socket IO Disconnect.");
            socketIOClient.on(Socket.EVENT_DISCONNECT, onDisconnectCallback);
            socketIOClient.disconnect();
            socketIOClient = null;
        }
    }

    @Override
    public void sendMessage(String peerId, String message, final ActionCallback<Void> callback) {
        if (socketIOClient == null) {
            Log.d(TAG, "socketIOClient is not established.");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("to", peerId);
            jsonObject.put("data", message);
            socketIOClient.emit(CLIENT_CHAT_TYPE, jsonObject, (Ack) args -> {
                if (args == null || args.length != 0) {
                    if (callback != null) {
                        callback.onFailure(new OwtError("Failed to send message."));
                    }
                } else {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
