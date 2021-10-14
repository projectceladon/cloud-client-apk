package com.intel.gamepad.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.android.material.button.MaterialButton;
import com.intel.gamepad.R;
import com.intel.gamepad.bean.GameListBean;
import com.intel.gamepad.controller.webrtc.RTCControllerAndroid;
import com.intel.gamepad.owt.p2p.P2PHelper;
import com.intel.gamepad.utils.IPUtils;
import com.commonlibrary.utils.StatusBarUtil;
import com.commonlibrary.view.loadingDialog.LoadingDialog;

public class GameDetailActivity extends BaseActvitiy {
    private static final String PARAM_BEAN = "param_bean";
    private GameListBean bean = null;
    private CheckBox chkAndroid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.setTranslucentStatus(this);
        setContentView(R.layout.activity_game_detail);
        initView();
        loadData();
        chkAndroid.setChecked(true);
        EditText etServerIP = findViewById(R.id.etServerIP);
        EditText etCoturnIP = findViewById(R.id.etCoturnIP);
        EditText etPeerID = findViewById(R.id.etPeerID);
        EditText etClientID = findViewById(R.id.etClientID);

        etServerIP.setText(IPUtils.loadIP());
        P2PHelper.serverIP = IPUtils.loadIP();
        etServerIP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null) {
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
                if (s != null) {
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
                if (s != null) {
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
                if (s != null) {
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

    private void initView() {
        initBackButton(R.id.ibtnBack);
        chkAndroid = findViewById(R.id.chkAndroid);
        MaterialButton btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(v -> requestStartGame());
        btnPlay.requestFocus();
    }

    private void requestStartGame() {
        LoadingDialog dlg = (LoadingDialog) LoadingDialog.show(this);
        gotoGamePlay();
        dlg.dismiss();
    }

    private void gotoGamePlay() {
        if (bean != null) {
            if (chkAndroid.isChecked()) {
                bean.setAddurl("android");
            }
            PlayGameRtcActivity.actionStart(this, RTCControllerAndroid.NAME, bean.getIid(), bean.getConf());
        }
    }

    private void loadData() {
        bean = getIntent().getParcelableExtra(PARAM_BEAN);
        if (bean == null) {
            bean = new GameListBean();
            bean.setIid(1);
            bean.setConf("rts");
        }
    }
}
