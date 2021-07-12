package com.intel.gamepad.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.intel.gamepad.R;
import com.intel.gamepad.bean.GameListBean;
import com.intel.gamepad.controller.webrtc.RTCControllerACT;
import com.intel.gamepad.controller.webrtc.RTCControllerAndroid;
import com.intel.gamepad.controller.webrtc.RTCControllerFPS;
import com.intel.gamepad.controller.webrtc.RTCControllerMouse;
import com.intel.gamepad.controller.webrtc.RTCControllerRAC;
import com.intel.gamepad.controller.webrtc.RTCControllerXBox;
import com.intel.gamepad.owt.p2p.P2PHelper;
import com.intel.gamepad.utils.IPUtils;
import com.mycommonlibrary.utils.StatusBarUtil;
import com.mycommonlibrary.view.loadingDialog.LoadingDialog;

import org.jetbrains.annotations.NotNull;

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.CoroutineScope;

public class GameDetailActivity extends BaseActvitiy implements CoroutineScope {
    private boolean useWebRTC =  true;
    private GameListBean bean = null;
    private CheckBox chkAndroid;
    private EditText etServerIP;
    private EditText etCoturnIP;
    private EditText etPeerID;
    private EditText etClientID;
    private static final String PARAM_BEAN = "param_bean";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.setTranslucentStatus(this);
        setContentView(R.layout.activity_game_detail);
        initView();
        loadData();
        chkAndroid.setChecked(true);
        etServerIP = findViewById(R.id.etServerIP);
        etCoturnIP = findViewById(R.id.etCoturnIP);
        etPeerID = findViewById(R.id.etPeerID);
        etClientID = findViewById(R.id.etClientID);

        etServerIP.setText(IPUtils.loadIP());
        P2PHelper.serverIP = IPUtils.loadIP();
        etServerIP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s != null) {
                    IPUtils.saveip(s.toString());
                    P2PHelper.serverIP = IPUtils.loadIP();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        etCoturnIP.setText(IPUtils.loadCoturnIP());
        P2PHelper.strCoturn = IPUtils.loadCoturnIP();
        etCoturnIP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s != null) {
                    IPUtils.saveCoturn(s.toString());
                    P2PHelper.strCoturn = IPUtils.loadCoturnIP();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        etPeerID.setText(IPUtils.loadPeerID());
        P2PHelper.peerId = IPUtils.loadPeerID();
        etPeerID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s != null) {
                    IPUtils.savepeerid(s.toString());
                    P2PHelper.peerId = IPUtils.loadPeerID();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        etClientID.setText(IPUtils.loadTokenID());
        P2PHelper.clientId = IPUtils.loadTokenID();
        etClientID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s != null) {
                    IPUtils.savetoken(s.toString());
                    P2PHelper.clientId = IPUtils.loadTokenID();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initView(){
        initBackButton(R.id.ibtnBack);
        chkAndroid = findViewById(R.id.chkAndroid);
        MaterialButton btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestStartGame(false);
            }
        });
        btnPlay.requestFocus();
    }

    private  void requestStartGame(Boolean isCreateRoom) {
        LoadingDialog dlg = (LoadingDialog) LoadingDialog.show(this);
        if(isCreateRoom) {
            Toast.makeText(this, "room is not supported", Toast.LENGTH_LONG);
        } else {
            gotoGamePlay();
            dlg.dismiss();
        }
    }

    private void gotoGamePlay() {
        if(bean != null) {
            if(useWebRTC) {
                bean.setAddurl("fps");
                if(chkAndroid.isChecked()) {
                    bean.setAddurl("android");
                }
                String ctrlName;
                switch (bean.getAddurl()) {
                    case "fps":
                        ctrlName = RTCControllerFPS.NAME;
                        break;
                    case "rts":
                        ctrlName = RTCControllerMouse.NAME;
                        break;
                    case "rac":
                        ctrlName = RTCControllerRAC.NAME;
                        break;
                    case "act":
                        ctrlName = RTCControllerACT.NAME;
                        break;
                    case "android":
                        ctrlName = RTCControllerAndroid.NAME;
                        break;
                    default:
                        ctrlName = RTCControllerXBox.NAME;
                }
                PlayGameRtcActivity.actionStart(this, ctrlName, bean.getIid(), bean.getConf());
            } else {
                //PlayGameRtcActivity.actionStart(this, bean.getIid(), bean.getIp(), bean.getPort());
            }
        }
    }
    private void loadData() {
        bean = getIntent().getParcelableExtra(PARAM_BEAN);
        if(bean == null) {
            bean = new GameListBean();
            bean.setIid(1);
            bean.setConf("rts");
        }
    }

    @NotNull
    @Override
    public CoroutineContext getCoroutineContext() {
        return null;
    }
}
