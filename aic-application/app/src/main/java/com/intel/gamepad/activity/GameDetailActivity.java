package com.intel.gamepad.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.commonlibrary.utils.DensityUtils;
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
import com.intel.gamepad.utils.PopupUtil;

import java.util.List;

public class GameDetailActivity extends BaseActvitiy {
    private static final String PARAM_BEAN = "param_bean";
    private GameListBean bean = null;
    private MaterialButton btnPlay;
    private PopupWindow popupOrient;
    private PopupWindow popupCodec;

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
        TextView orient = findViewById(R.id.orient);

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


        orient.setOnClickListener(v -> {
            if (popupOrient != null) {
                popupOrient.dismiss();
                popupOrient = null;
            } else {
                showPopupOrientation(orient);
            }
        });

        checkMediaCodecSupport();

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

        if (!result[0] && !result[1]) {
            btnPlay.setClickable(false);
            btnPlay.setBackgroundResource(R.color.gray67);
        } else if (result[0] && !result[1]) {
            IPUtils.saveMediaCodec(AppConst.H264);
        } else {
            IPUtils.saveMediaCodec(AppConst.HEVC);
            TextView codec = findViewById(R.id.codec);
            codec.setVisibility(View.VISIBLE);
            codec.setOnClickListener(v -> {
                if (popupCodec != null) {
                    popupCodec.dismiss();
                    popupCodec = null;
                } else {
                    showPopupCodec(codec);
                }
            });
        }

    }

    public void showPopupOrientation(View parent) {
        View popView = View.inflate(this, R.layout.popup_orientation_window, null);
        popupOrient = PopupUtil.createPopup(parent, popView, DensityUtils.dp2px(150f));
        CheckBox chkLandscape = popView.findViewById(R.id.chk_landscape);
        CheckBox chkPortrait = popView.findViewById(R.id.chk_portrait);
        popView.findViewById(R.id.close).setVisibility(View.GONE);

        if (IPUtils.loadPortrait()) {
            chkLandscape.setChecked(false);
            chkPortrait.setChecked(true);
            chkPortrait.setClickable(false);
        } else {
            chkLandscape.setChecked(true);
            chkPortrait.setChecked(false);
            chkLandscape.setClickable(false);
        }
        chkLandscape.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                IPUtils.savePortrait(false);
                chkPortrait.setChecked(false);
                chkPortrait.setClickable(true);
            } else {
                IPUtils.savePortrait(true);
                chkPortrait.setChecked(true);
                chkPortrait.setClickable(false);
            }
        });
        chkPortrait.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                chkLandscape.setChecked(false);
                chkLandscape.setClickable(true);
            } else {
                chkLandscape.setChecked(true);
                chkLandscape.setClickable(false);
            }
        });
    }

    public void showPopupCodec(View parent) {
        View popView = View.inflate(this, R.layout.popup_orientation_window, null);
        popupCodec = PopupUtil.createPopup(parent, popView, DensityUtils.dp2px(150f));
        CheckBox HECV = popView.findViewById(R.id.chk_landscape);
        HECV.setText(R.string.hevc);
        CheckBox H264 = popView.findViewById(R.id.chk_portrait);
        H264.setText(R.string.h264);
        popView.findViewById(R.id.close).setVisibility(View.GONE);
        if (AppConst.HEVC.equals(IPUtils.loadMediaCodec())) {
            H264.setChecked(false);
            HECV.setChecked(true);
            HECV.setClickable(false);
        } else {
            H264.setChecked(true);
            HECV.setChecked(false);
            H264.setClickable(false);
        }
        H264.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                IPUtils.saveMediaCodec(AppConst.H264);
                HECV.setChecked(false);
                HECV.setClickable(true);
            } else {
                IPUtils.saveMediaCodec(AppConst.HEVC);
                HECV.setChecked(true);
                HECV.setClickable(false);
            }
        });
        HECV.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                H264.setChecked(false);
                H264.setClickable(true);
            } else {
                H264.setChecked(true);
                H264.setClickable(false);
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
