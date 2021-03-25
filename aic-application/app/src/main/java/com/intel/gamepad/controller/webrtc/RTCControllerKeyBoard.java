
package com.intel.gamepad.controller.webrtc;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.intel.gamepad.R;
import com.intel.gamepad.activity.PlayGameRtcActivity;
import com.intel.gamepad.app.KeyConst;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;

public class RTCControllerKeyBoard extends BaseController implements OnClickListener {
    public static final String NAME = "KeyBoard";
    public static final String DESC = "虚拟键盘";
    private ViewGroup layoutGamePad;
    private CheckBox chkShift;

    public RTCControllerKeyBoard(PlayGameRtcActivity act, Handler handler, DeviceSwitchListtener devSwitch) {
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
        layoutGamePad = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.game_pad_keyboard, null, false);
        addControllerView(layoutGamePad);
        layoutGamePad.findViewById(R.id.ibtnGamePad).setOnClickListener(view -> devSwitch.switchMapperPad());
        chkShift = layoutGamePad.findViewById(R.id.btnShift);
        layoutGamePad.findViewById(R.id.btnEsc).setOnClickListener(this);
        layoutGamePad.findViewById(R.id.btnShift).setOnClickListener(this);

        setKeyButtonTouch(R.id.layoutRow1);
        setKeyButtonTouch(R.id.layoutRow2);
        setKeyButtonTouch(R.id.layoutRow3);
        setKeyButtonTouch(R.id.layoutRow4);
        switchShift1();
        switchShift2();
        switchShift3();
        switchShift4();

        layoutGamePad.findViewById(R.id.btnEnter).setOnTouchListener(this);
        layoutGamePad.findViewById(R.id.btnBack).setOnTouchListener(this);
        layoutGamePad.findViewById(R.id.btnSpace).setOnTouchListener(this);
        layoutGamePad.findViewById(R.id.btnLeft).setOnTouchListener(this);
        layoutGamePad.findViewById(R.id.btnRight).setOnTouchListener(this);
        layoutGamePad.findViewById(R.id.btnUp).setOnTouchListener(this);
        layoutGamePad.findViewById(R.id.btnDown).setOnTouchListener(this);
    }

    private void setKeyButtonTouch(int resLayout) {
        ViewGroup vg = layoutGamePad.findViewById(resLayout);
        for (int i = 0; i < vg.getChildCount(); i++) {
            vg.getChildAt(i).setOnTouchListener(this);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        updateLastTouchEvent();
        int action = evt.getActionMasked();
        switch (v.getId()) {
            case R.id.btnBack:
                return handleButtonTouch(action, KeyConst.VK_BACK_SPACE, v);
            case R.id.btnEnter:
                return handleButtonTouch(action, KeyConst.VK_ENTER, v);
            case R.id.btnLeft:
                return handleButtonTouch(action, KeyConst.VK_LEFT, v);
            case R.id.btnRight:
                return handleButtonTouch(action, KeyConst.VK_RIGHT, v);
            case R.id.btnUp:
                return handleButtonTouch(action, KeyConst.VK_UP, v);
            case R.id.btnDown:
                return handleButtonTouch(action, KeyConst.VK_DOWN, v);
            case R.id.btnSpace:
                return handleButtonTouch(action, KeyConst.VK_SPACE, v);
        }
        onTouchNumKey(v, action);
        onTouchCharKey(v, action);
        onTouchOtherKey(v, action);
        return false;
    }

    private boolean onTouchCharKey(View v, int action) {
        if (v instanceof TextView) {
            TextView btn = (TextView) v;
            switch (btn.getText().toString().toLowerCase()) {
                case "a":
                    return handleButtonTouch(action, KeyConst.VK_A, v);
                case "b":
                    return handleButtonTouch(action, KeyConst.VK_B, v);
                case "c":
                    return handleButtonTouch(action, KeyConst.VK_C, v);
                case "d":
                    return handleButtonTouch(action, KeyConst.VK_D, v);
                case "e":
                    return handleButtonTouch(action, KeyConst.VK_E, v);
                case "f":
                    return handleButtonTouch(action, KeyConst.VK_F, v);
                case "g":
                    return handleButtonTouch(action, KeyConst.VK_G, v);
                case "h":
                    return handleButtonTouch(action, KeyConst.VK_H, v);
                case "i":
                    return handleButtonTouch(action, KeyConst.VK_I, v);
                case "j":
                    return handleButtonTouch(action, KeyConst.VK_J, v);
                case "l":
                    return handleButtonTouch(action, KeyConst.VK_L, v);
                case "k":
                    return handleButtonTouch(action, KeyConst.VK_K, v);
                case "m":
                    return handleButtonTouch(action, KeyConst.VK_M, v);
                case "n":
                    return handleButtonTouch(action, KeyConst.VK_N, v);
                case "o":
                    return handleButtonTouch(action, KeyConst.VK_O, v);
                case "p":
                    return handleButtonTouch(action, KeyConst.VK_P, v);
                case "q":
                    return handleButtonTouch(action, KeyConst.VK_Q, v);
                case "r":
                    return handleButtonTouch(action, KeyConst.VK_R, v);
                case "s":
                    return handleButtonTouch(action, KeyConst.VK_S, v);
                case "t":
                    return handleButtonTouch(action, KeyConst.VK_T, v);
                case "u":
                    return handleButtonTouch(action, KeyConst.VK_U, v);
                case "v":
                    return handleButtonTouch(action, KeyConst.VK_V, v);
                case "w":
                    return handleButtonTouch(action, KeyConst.VK_W, v);
                case "x":
                    return handleButtonTouch(action, KeyConst.VK_X, v);
                case "y":
                    return handleButtonTouch(action, KeyConst.VK_Y, v);
                case "z":
                    return handleButtonTouch(action, KeyConst.VK_Z, v);
            }
        }
        return false;
    }

    private boolean onTouchNumKey(View v, int action) {
        if (v instanceof TextView) {
            TextView btn = (TextView) v;
            switch (btn.getText().toString()) {
                case "0":
                    return handleButtonTouch(action, KeyConst.VK_0, v);
                case "1":
                    return handleButtonTouch(action, KeyConst.VK_1, v);
                case "2":
                    return handleButtonTouch(action, KeyConst.VK_2, v);
                case "3":
                    return handleButtonTouch(action, KeyConst.VK_3, v);
                case "4":
                    return handleButtonTouch(action, KeyConst.VK_4, v);
                case "5":
                    return handleButtonTouch(action, KeyConst.VK_5, v);
                case "6":
                    return handleButtonTouch(action, KeyConst.VK_6, v);
                case "7":
                    return handleButtonTouch(action, KeyConst.VK_7, v);
                case "8":
                    return handleButtonTouch(action, KeyConst.VK_8, v);
                case "9":
                    return handleButtonTouch(action, KeyConst.VK_9, v);
            }
        }
        return false;
    }

    private boolean onTouchOtherKey(View v, int action) {
        if (v instanceof TextView) {
            Button btn = (Button) v;
            switch (btn.getText().toString()) {
                case "\\":
                    return handleButtonTouch(action, KeyConst.VK_BACK_SLASH, v);
                case "[":
                    return handleButtonTouch(action, KeyConst.VK_OPEN_BRACKET, v);
                case "]":
                    return handleButtonTouch(action, KeyConst.VK_CLOSE_BRACKET, v);
                case "(":
                    return handleButtonTouch(action, KeyConst.VK_LEFT_PARENTHESIS, v);
                case ")":
                    return handleButtonTouch(action, KeyConst.VK_RIGHT_PARENTHESIS, v);
                case "+":
                    return handleButtonTouch(action, KeyConst.VK_PLUS, v);
                case "-":
                    return handleButtonTouch(action, KeyConst.VK_UNDERSCORE, v);
                case "*":
                    return handleButtonTouch(action, KeyConst.VK_MULTIPLY, v);
                case "/":
                    return handleButtonTouch(action, KeyConst.VK_SLASH, v);
                case "=":
                    return handleButtonTouch(action, KeyConst.VK_EQUALS, v);
                case ";":
                    return handleButtonTouch(action, KeyConst.VK_SEMICOLON, v);
                case ".":
                    return handleButtonTouch(action, KeyConst.VK_PERIOD, v);
                case ",":
                    return handleButtonTouch(action, KeyConst.VK_COMMA, v);
                case "@":
                    return handleButtonTouch(action, KeyConst.VK_AT, v);
                case ":":
                    return handleButtonTouch(action, KeyConst.VK_COLON, v);
                case "^":
                    return handleButtonTouch(action, KeyConst.VK_CIRCUMFLEX, v);
                case "$":
                    return handleButtonTouch(action, KeyConst.VK_DOLLAR, v);
                case "!":
                    return handleButtonTouch(action, KeyConst.VK_EXCLAMATION_MARK, v);
                case "#":
                    return handleButtonTouch(action, KeyConst.VK_NUMBER_SIGN, v);
            }
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnEsc)
            devSwitch.switchMapperPad();
        if (v.getId() == R.id.btnShift) {
            switchShift1();
            switchShift2();
            switchShift3();
            switchShift4();
        }
    }

    private void switchShift1() {
        char[] arrUp = {'~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+'};
        char[] arrdown = {'`', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '='};
        ViewGroup vg1 = layoutGamePad.findViewById(R.id.layoutRow1);
        for (int i = 0; i < vg1.getChildCount(); i++) {
            Button btn = (Button) vg1.getChildAt(i);
            btn.setText("" + (chkShift.isChecked() ? arrUp[i] : arrdown[i]));
        }
    }

    private void switchShift2() {
        char[] arrUp = {'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '{', '}', '|'};
        char[] arrDown = {'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']', '\\'};
        ViewGroup vg = layoutGamePad.findViewById(R.id.layoutRow2);
        for (int i = 0; i < vg.getChildCount(); i++) {
            Button btn = (Button) vg.getChildAt(i);
            btn.setText("" + (chkShift.isChecked() ? arrUp[i] : arrDown[i]));
        }
    }

    private void switchShift3() {
        char[] arrUp = {'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ':', '\"'};
        char[] arrDown = {'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\''};
        ViewGroup vg = layoutGamePad.findViewById(R.id.layoutRow3);
        for (int i = 0; i < vg.getChildCount(); i++) {
            Button btn = (Button) vg.getChildAt(i);
            btn.setText("" + (chkShift.isChecked() ? arrUp[i] : arrDown[i]));
        }
    }

    private void switchShift4() {
        char[] arrUp = {'Z', 'X', 'C', 'V', 'B', 'N', 'M', '<', '>', '?'};
        char[] arrDown = {'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/'};
        ViewGroup vg = layoutGamePad.findViewById(R.id.layoutRow4);
        for (int i = 0; i < vg.getChildCount(); i++) {
            Button btn = (Button) vg.getChildAt(i);
            btn.setText("" + (chkShift.isChecked() ? arrUp[i] : arrDown[i]));
        }
    }
}
