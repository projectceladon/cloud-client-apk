package com.intel.gamepad.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;

import com.commonlibrary.utils.StatusBarUtil;
import com.commonlibrary.view.loadingDialog.LoadingDialog;
import com.google.android.material.button.MaterialButton;
import com.intel.gamepad.R;
import com.intel.gamepad.app.AppConst;
import com.intel.gamepad.bean.GameListBean;
import com.intel.gamepad.controller.webrtc.BaseController;
import com.intel.gamepad.controller.webrtc.RTCControllerAndroid;
import com.intel.gamepad.owt.p2p.P2PHelper;
import com.intel.gamepad.utils.DeviceManager;
import com.intel.gamepad.utils.IPUtils;

import java.util.ArrayList;
import java.util.List;

public class GameDetailActivity extends BaseActivity {
    private static final String PARAM_BEAN = "param_bean";
    private GameListBean bean = null;
    private MaterialButton btnPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.setTranslucentStatus(this);
        setContentView(R.layout.activity_game_detail);
        initView();
        loadData();
        EditText etServerIP = findViewById(R.id.etServerIP);
        EditText etCoturnIP = findViewById(R.id.etCoturnIP);
        EditText etPeerID = findViewById(R.id.etPeerID);
        EditText etClientID = findViewById(R.id.etClientID);
        CheckBox chkTest = findViewById(R.id.chkTest);

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
                    if (s.toString().startsWith("http") && s.toString().contains(":")) {
                        List<String> ipInfo = IPUtils.getIp(s.toString());
                        if (ipInfo.size() > 0) {
                            etCoturnIP.setText(ipInfo.get(0));
                        }
                    }

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

        chkTest.setChecked(IPUtils.loadTest());
        chkTest.setOnCheckedChangeListener((buttonView, isChecked) -> IPUtils.savetest(isChecked));

        checkOrientatin();
        checkMediaCodecSupport();

    }

    private void checkOrientatin() {
        Spinner orient_sp = findViewById(R.id.orient_sp);
        List<String> orientList = new ArrayList<>();
        orientList.add(getResources().getString(R.string.landscape));
        orientList.add(getResources().getString(R.string.portrait));
        ArrayAdapter<String> codecAdapter=new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,orientList);
        codecAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        orient_sp.setAdapter(codecAdapter);
        if (IPUtils.loadPortrait()) {
            orient_sp.setSelection(1);
        } else {
            orient_sp.setSelection(0);
        }
        orient_sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                IPUtils.savePortrait(position != 0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void checkMediaCodecSupport() {
        boolean[] result = DeviceManager.getInstance().checkMediaCodecSupport(new String[]{AppConst.H264, AppConst.HEVC, AppConst.VP9});
        if (!result[0]) {
            Toast.makeText(this, R.string.no_h264, Toast.LENGTH_LONG).show();
        }
        if (!result[1]) {
            Toast.makeText(this, R.string.no_hevc, Toast.LENGTH_LONG).show();
        }
        if (!result[2]) {
            Toast.makeText(this, R.string.no_vp9, Toast.LENGTH_LONG).show();
        }

        Spinner codec_sp = findViewById(R.id.codec_sp);
        List<String> codecList = new ArrayList<>();
        if (!result[0] && !result[1]) {
            btnPlay.setClickable(false);
            btnPlay.setBackgroundTintList(AppCompatResources.getColorStateList(this,R.color.gray_99));
            codecList.add(getResources().getString(R.string.none));
            codec_sp.setEnabled(false);
        } else if (result[0] && !result[1]) {
            IPUtils.saveMediaCodec(AppConst.H264);
            codecList.add(getResources().getString(R.string.h264));
            codec_sp.setEnabled(false);
        } else {
            IPUtils.saveMediaCodec(AppConst.H264);
            codecList.add(getResources().getString(R.string.h264));
            codecList.add(getResources().getString(R.string.hevc));
        }
        ArrayAdapter<String> codecAdapter=new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,codecList);
        codecAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        codec_sp.setAdapter(codecAdapter);
        codec_sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    IPUtils.saveMediaCodec(AppConst.H264);
                } else {
                    IPUtils.saveMediaCodec(AppConst.HEVC);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
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
        btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(v -> {
            BaseController.manuallyPressBackButton.set(false);
            requestStartGame();
        });
        btnPlay.requestFocus();
    }

    private void requestStartGame() {
        LoadingDialog dlg = (LoadingDialog) LoadingDialog.show(this);
        gotoGamePlay();
        dlg.dismiss();
    }

    private void gotoGamePlay() {
        if (bean != null) {
            bean.setAddurl("android");
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
