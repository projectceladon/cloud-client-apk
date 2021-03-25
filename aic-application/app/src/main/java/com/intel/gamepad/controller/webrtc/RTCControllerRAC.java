
package com.intel.gamepad.controller.webrtc;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.intel.gamepad.R;
import com.intel.gamepad.activity.PlayGameRtcActivity;
import com.intel.gamepad.app.KeyConst;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.intel.gamepad.controller.impl.MouseMotionEventListener;

public class RTCControllerRAC extends BaseController implements MouseMotionEventListener {
    public static final String NAME = "RAC";
    public static final String DESC = "赛车类游戏控制器";
    private ViewGroup layoutGamePad;
    private Button btnEsc = null;
    private Button btnSelect = null;
    private Button btnStart = null;
    private Button btnL = null;
    private Button btnR = null;
    private ImageButton btnLeft = null;
    private ImageButton btnRight = null;
    private Button btnGas = null;
    private Button btnBrake = null;

    public RTCControllerRAC(PlayGameRtcActivity act, Handler handler, DeviceSwitchListtener devSwitch) {
        super(act, handler, devSwitch);
    }

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return DESC;
    }

    public View getView() {
        if (layoutGamePad == null) initView();
        return layoutGamePad;
    }

    private void initView() {
        this.layoutGamePad = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.game_pad_rac, null, false);
        addControllerView(layoutGamePad);
        initBackButton(layoutGamePad.findViewById(R.id.btnBack));
        initSwitchDeviceButton(layoutGamePad.findViewById(R.id.ibtnShowKeyBoard));

        btnEsc = layoutGamePad.findViewById(R.id.btnEsc);
        btnEsc.setOnTouchListener(this);
        //
        btnSelect = layoutGamePad.findViewById(R.id.btnSelect);
        btnSelect.setOnTouchListener(this);
        //
        btnStart = layoutGamePad.findViewById(R.id.btnStart);
        btnStart.setOnTouchListener(this);
        //
        btnL = layoutGamePad.findViewById(R.id.btnL);
        btnL.setOnTouchListener(this);
        //
        btnR = layoutGamePad.findViewById(R.id.btnR);
        btnR.setOnTouchListener(this);

        btnLeft = layoutGamePad.findViewById(R.id.btnLeft);
        btnLeft.setOnTouchListener(this);

        btnRight = layoutGamePad.findViewById(R.id.btnRight);
        btnRight.setOnTouchListener(this);

        btnGas = layoutGamePad.findViewById(R.id.btnGas);
        btnGas.setOnTouchListener(this);

        btnBrake = layoutGamePad.findViewById(R.id.btnBrake);
        btnBrake.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        updateLastTouchEvent();
        int count = evt.getPointerCount(); // 屏幕的触点数
        int action = evt.getActionMasked();
        float x = evt.getX();
        float y = evt.getY();
        //
        if (v == btnL)
            return handleButtonTouch(action, KeyConst.VK_L);
        if (v == btnR)
            return handleButtonTouch(action, KeyConst.VK_R);
        if (v == btnGas)
            return handleButtonTouch(action, KeyConst.VK_A);
        if (v == btnBrake)
            return handleButtonTouch(action, KeyConst.VK_B);
        if (v == btnSelect)
            return handleButtonTouch(action, KeyConst.VK_SPACE);
        if (v == btnStart)
            return handleButtonTouch(action, KeyConst.VK_ENTER);
        if (v == btnEsc)
            return handleButtonTouch(action, KeyConst.VK_ESCAPE);
        if (v == btnLeft)
            emulateArrowKeys(action, 9);
        if (v == btnRight)
            emulateArrowKeys(action, 3);
        return false;
    }

    @Override
    public void onMouseMotion(View v, int x, int y, int dx, int dy) {
        sendMouseMotionF(x, y, dx, dy);
    }

    @Override
    public void onMouseDown(View v, int x, int y) {

    }

    @Override
    public void onMouseUp(View v, int x, int y) {

    }
}
