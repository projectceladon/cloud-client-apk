package com.intel.gamepad.activity;

import android.Manifest;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

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
import com.intel.gamepad.utils.permission.PermissionsUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameDetailActivity extends BaseActivity {
    private static final String PARAM_BEAN = "param_bean";
    /* This file needs to be placed in "/sdcard/" folder. */
    private final File codecWhitelistXMLFile = new File(Environment.getExternalStorageDirectory().getAbsoluteFile(),
            AppConst.CODEC_WHITELIST_FILENAME);
    private final String TAG = GameDetailActivity.class.toString();
    private GameListBean bean = null;
    PermissionsUtils.IPermissionResult permissionsResult = new PermissionsUtils.IPermissionResult() {
        @Override
        public void passPermission(boolean history, String[] permissions) {
            BaseController.manuallyPressBackButton.set(false);
            updateMediacodecXmlFile();
            requestStartGame();
            if (!history) {
                for (String pass : permissions) {
                    Toast.makeText(GameDetailActivity.this, "The " + pass + " has been granted", Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        public void denyPermission(String[] permissions) {
            BaseController.manuallyPressBackButton.set(false);
            updateMediacodecXmlFile();
            requestStartGame();
            for (String deny : permissions) {
                Toast.makeText(GameDetailActivity.this, "The " + deny + " has been denied", Toast.LENGTH_LONG).show();
            }
        }
    };

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

        checkOrientation();
        checkMediaCodecSupport();

    }

    private void checkOrientation() {
        Spinner orient_sp = findViewById(R.id.orient_sp);
        List<String> orientList = new ArrayList<>();
        orientList.add(getResources().getString(R.string.landscape));
        orientList.add(getResources().getString(R.string.portrait));
        ArrayAdapter<String> codecAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, orientList);
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

        MaterialButton btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(v -> {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            PermissionsUtils.getInstance().checkPermissions(this, permissions, permissionsResult);
        });
        btnPlay.requestFocus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionsUtils.getInstance().onRequestPermissionsResult(GameDetailActivity.this, requestCode, permissions, grantResults);
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

    private void updateMediacodecXmlFile() {
        String fileContent;
        String referenceXmlStr = prepareCodecWhitelistXmlStr();

        // file does not exist, create it
        if (!codecWhitelistXMLFile.exists()) {
            Log.d(TAG, "file not found, creating: " + codecWhitelistXMLFile.getName());

            try {
                FileUtils.writeStringToFile(codecWhitelistXMLFile, referenceXmlStr, "UTF8");
            } catch (Exception e) {
                Log.d(TAG, "error creating file: " + codecWhitelistXMLFile.getName());
                e.printStackTrace();
            }
            return;
        }

        // file exists, compare and replace content if required
        try {
            fileContent = FileUtils.readFileToString(codecWhitelistXMLFile, "UTF8");
        } catch (IOException e) {
            // error reading file, probably
            Log.d(TAG, "cannot update file (" + codecWhitelistXMLFile.getAbsolutePath()
                    + "), error reading file: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        int match = fileContent.compareTo(referenceXmlStr);

        if (match != 0) {
            // file different than expected, replace content with expected one
            Log.d(TAG, "updating content of " + codecWhitelistXMLFile.getName());
            try {
                FileUtils.writeStringToFile(codecWhitelistXMLFile, referenceXmlStr, "UTF8");
            } catch (Exception e) {
                Log.d(TAG, "error updating file: " + codecWhitelistXMLFile.getName());
                e.printStackTrace();
            }
        } else {
            // do nothing, the expected file is already present.
            Log.d(TAG, "no need to update file : " + codecWhitelistXMLFile.getName());
        }
    }

    String prepareCodecWhitelistXmlStr() {
        String[] mime_types = {"video/avc", "video/hevc"};

        ArrayList<MediaCodecInfo> decoders = new ArrayList<>();
        ArrayList<MediaCodecInfo> encoders = new ArrayList<>();

        for (String mime_type : mime_types) {
            decoders.addAll(findMediaCodecs(mime_type, false));
        }

        for (String mime_type : mime_types) {
            encoders.addAll(findMediaCodecs(mime_type, true));
        }

        return CodeLite2XmlStr(encoders, decoders);
    }

    private ArrayList<MediaCodecInfo> findMediaCodecs(String mimeType, boolean isEncoder) {
        ArrayList<MediaCodecInfo> codecList = new ArrayList<>();
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (codecInfo.isEncoder() == isEncoder) {
                String[] types = codecInfo.getSupportedTypes();
                for (String type : types) {
                    if (type.equalsIgnoreCase(mimeType)) {
                        codecList.add(codecInfo);
                        break;
                    }
                }
            }
        }
        return codecList;
    }

    String CodeLite2XmlStr(ArrayList<MediaCodecInfo> encoderList, ArrayList<MediaCodecInfo> decoderList) {
        /* Creates mediacodec.xml content for list of codecs supplied. The XML format is based on format specified at
         * https://github.com/open-webrtc-toolkit/owt-client-android/blob/f2294c55f9d3bdc1de78ab84c9b7d018c3e3a04b/docs/index.md?plain=1#L71.
         */

        StringBuilder xmlStr = new StringBuilder();

        xmlStr.append("<pre>\n")
                .append("  <MediaCodecs>\n")
                .append("    <Encoders>\n");

        // add encoders
        for (int i = 0; i < encoderList.size(); i++) {
            Log.d(TAG, "Encoders = " + encoderList.get(i));
            xmlStr.append("      <MediaCodec name=\"")
                    .append(encoderList.get(i).getName())
                    .append("\" type=\"")
                    .append(encoderList.get(i).getSupportedTypes()[0])
                    .append("\"/>\n");
        }

        xmlStr.append("    </Encoders>\n")
                .append("    <Decoders>\n");

        // add decoders
        for (int i = 0; i < decoderList.size(); i++) {
            Log.d(TAG, "decoders = " + decoderList.get(i));
            xmlStr.append("      <MediaCodec name=\"")
                    .append(decoderList.get(i).getName())
                    .append("\" type=\"")
                    .append(decoderList.get(i).getSupportedTypes()[0])
                    .append("\"/>\n");
        }

        xmlStr.append("    </Decoders>\n")
                .append("  </MediaCodecs>\n")
                .append("</pre>\n");

        return xmlStr.toString();
    }

}
