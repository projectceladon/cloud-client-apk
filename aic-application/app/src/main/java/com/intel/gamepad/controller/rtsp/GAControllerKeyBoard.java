
package com.intel.gamepad.controller.rtsp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.intel.gamepad.R;
import com.intel.gamepad.controller.impl.DeviceSwitchListtener;
import com.mycommonlibrary.utils.LogEx;

public class GAControllerKeyBoard extends GAController implements OnClickListener {
    public static final String NAME = "KeyBoard";
    public static final String DESC = "虚拟键盘";
    private DeviceSwitchListtener devSwitch;
    private ViewGroup viewPad;
    private CheckBox chkShift;

    public GAControllerKeyBoard(Context c, DeviceSwitchListtener devSwitch) {
        super(c);
        this.devSwitch = devSwitch;
    }

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return DESC;
    }

    @Override
    public void onDimensionChange(int width, int height) {
        if (width < 0 || height < 0)
            return;
        // must be called first
        super.setMouseVisibility(false);
        super.onDimensionChange(width, height);

        viewPad = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.game_pad_keyboard, null, false);
        placeView(viewPad, 0, 0, width, height);
        chkShift = viewPad.findViewById(R.id.btnShift);
        viewPad.findViewById(R.id.btnEsc).setOnClickListener(this);
        viewPad.findViewById(R.id.btnShift).setOnClickListener(this);

        setKeyButtonTouch(R.id.layoutRow1);
        setKeyButtonTouch(R.id.layoutRow2);
        setKeyButtonTouch(R.id.layoutRow3);
        setKeyButtonTouch(R.id.layoutRow4);
        switchShift1();
        switchShift2();
        switchShift3();
        switchShift4();

        viewPad.findViewById(R.id.btnEnter).setOnTouchListener(this);
        viewPad.findViewById(R.id.btnBack).setOnTouchListener(this);
        viewPad.findViewById(R.id.btnSpace).setOnTouchListener(this);
        viewPad.findViewById(R.id.btnLeft).setOnTouchListener(this);
        viewPad.findViewById(R.id.btnRight).setOnTouchListener(this);
        viewPad.findViewById(R.id.btnUp).setOnTouchListener(this);
        viewPad.findViewById(R.id.btnDown).setOnTouchListener(this);

    }

    private void setKeyButtonTouch(int resLayout) {
        ViewGroup vg = viewPad.findViewById(resLayout);
        for (int i = 0; i < vg.getChildCount(); i++) {
            vg.getChildAt(i).setOnTouchListener(this);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        GAController.lastTouchMillis = System.currentTimeMillis();
        super.onTouch(v, evt);
        int action = evt.getActionMasked();
        switch (v.getId()) {
            case R.id.btnBack:
                return handleButtonTouch(action, SDL2.Scancode.BACKSPACE, SDL2.Keycode.BACKSPACE, 0, 0);
            case R.id.btnEnter:
                return handleButtonTouch(action, SDL2.Scancode.RETURN, SDL2.Keycode.RETURN, 0, 0);
            case R.id.btnLeft:
                return handleButtonTouch(action, SDL2.Scancode.LEFT, SDL2.Keycode.LEFT, 0, 0);
            case R.id.btnRight:
                return handleButtonTouch(action, SDL2.Scancode.RIGHT, SDL2.Keycode.RIGHT, 0, 0);
            case R.id.btnUp:
                return handleButtonTouch(action, SDL2.Scancode.UP, SDL2.Keycode.UP, 0, 0);
            case R.id.btnDown:
                return handleButtonTouch(action, SDL2.Scancode.DOWN, SDL2.Keycode.DOWN, 0, 0);
            case R.id.btnSpace:
                return handleButtonTouch(action, SDL2.Scancode.SPACE, SDL2.Keycode.SPACE, 0, 0);
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
                    return handleButtonTouch(action, SDL2.Scancode.A, SDL2.Keycode.a, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "b":
                    return handleButtonTouch(action, SDL2.Scancode.B, SDL2.Keycode.b, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "c":
                    return handleButtonTouch(action, SDL2.Scancode.C, SDL2.Keycode.c, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "d":
                    return handleButtonTouch(action, SDL2.Scancode.D, SDL2.Keycode.d, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "e":
                    return handleButtonTouch(action, SDL2.Scancode.E, SDL2.Keycode.e, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "f":
                    return handleButtonTouch(action, SDL2.Scancode.F, SDL2.Keycode.f, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "g":
                    return handleButtonTouch(action, SDL2.Scancode.G, SDL2.Keycode.g, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "h":
                    return handleButtonTouch(action, SDL2.Scancode.H, SDL2.Keycode.h, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "i":
                    return handleButtonTouch(action, SDL2.Scancode.I, SDL2.Keycode.i, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "j":
                    return handleButtonTouch(action, SDL2.Scancode.J, SDL2.Keycode.j, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "l":
                    return handleButtonTouch(action, SDL2.Scancode.L, SDL2.Keycode.l, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "k":
                    return handleButtonTouch(action, SDL2.Scancode.K, SDL2.Keycode.k, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "m":
                    return handleButtonTouch(action, SDL2.Scancode.M, SDL2.Keycode.m, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "n":
                    return handleButtonTouch(action, SDL2.Scancode.N, SDL2.Keycode.n, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "o":
                    return handleButtonTouch(action, SDL2.Scancode.O, SDL2.Keycode.o, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "p":
                    return handleButtonTouch(action, SDL2.Scancode.P, SDL2.Keycode.p, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "q":
                    return handleButtonTouch(action, SDL2.Scancode.Q, SDL2.Keycode.q, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "r":
                    return handleButtonTouch(action, SDL2.Scancode.R, SDL2.Keycode.r, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "s":
                    return handleButtonTouch(action, SDL2.Scancode.S, SDL2.Keycode.s, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "t":
                    return handleButtonTouch(action, SDL2.Scancode.T, SDL2.Keycode.t, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "u":
                    return handleButtonTouch(action, SDL2.Scancode.U, SDL2.Keycode.u, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "v":
                    return handleButtonTouch(action, SDL2.Scancode.V, SDL2.Keycode.v, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "w":
                    return handleButtonTouch(action, SDL2.Scancode.W, SDL2.Keycode.w, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "x":
                    return handleButtonTouch(action, SDL2.Scancode.X, SDL2.Keycode.x, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "y":
                    return handleButtonTouch(action, SDL2.Scancode.Y, SDL2.Keycode.y, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
                case "z":
                    return handleButtonTouch(action, SDL2.Scancode.Z, SDL2.Keycode.z, chkShift.isChecked() ? SDL2.Keymod.SHIFT : 0, 0);
            }
        }
        return false;
    }

    private boolean onTouchNumKey(View v, int action) {
        if (v instanceof TextView) {
            TextView btn = (TextView) v;
            switch (btn.getText().toString()) {
                case "0":
                    return handleButtonTouch(action, SDL2.Scancode._0, SDL2.Keycode._0, 0, 0);
                case "1":
                    return handleButtonTouch(action, SDL2.Scancode._1, SDL2.Keycode._1, 0, 0);
                case "2":
                    return handleButtonTouch(action, SDL2.Scancode._2, SDL2.Keycode._2, 0, 0);
                case "3":
                    return handleButtonTouch(action, SDL2.Scancode._3, SDL2.Keycode._3, 0, 0);
                case "4":
                    return handleButtonTouch(action, SDL2.Scancode._4, SDL2.Keycode._4, 0, 0);
                case "5":
                    return handleButtonTouch(action, SDL2.Scancode._5, SDL2.Keycode._5, 0, 0);
                case "6":
                    return handleButtonTouch(action, SDL2.Scancode._6, SDL2.Keycode._6, 0, 0);
                case "7":
                    return handleButtonTouch(action, SDL2.Scancode._7, SDL2.Keycode._7, 0, 0);
                case "8":
                    return handleButtonTouch(action, SDL2.Scancode._8, SDL2.Keycode._8, 0, 0);
                case "9":
                    return handleButtonTouch(action, SDL2.Scancode._9, SDL2.Keycode._9, 0, 0);
            }
        }
        return false;
    }

    private boolean onTouchOtherKey(View v, int action) {
        if (v instanceof TextView) {
            Button btn = (Button) v;
            switch (btn.getText().toString()) {
                case "\\":
                    return handleButtonTouch(action, SDL2.Scancode.BACKSLASH, SDL2.Keycode.BACKSLASH, 0, 0);
                case "[":
                    return handleButtonTouch(action, SDL2.Scancode.LEFTBRACKET, SDL2.Keycode.LEFTBRACKET, 0, 0);
                case "]":
                    return handleButtonTouch(action, SDL2.Scancode.RIGHTBRACKET, SDL2.Keycode.RIGHTBRACKET, 0, 0);
                case "(":
                    return handleButtonTouch(action, SDL2.Scancode.KP_LEFTPAREN, SDL2.Keycode.KP_LEFTPAREN, 0, 0);
                case ")":
                    return handleButtonTouch(action, SDL2.Scancode.KP_RIGHTPAREN, SDL2.Keycode.KP_RIGHTPAREN, 0, 0);
                case "%":
                    return handleButtonTouch(action, SDL2.Scancode.KP_PERCENT, SDL2.Keycode.KP_PERCENT, 0, 0);
                case "+":
                    return handleButtonTouch(action, SDL2.Scancode.KP_PLUS, SDL2.Keycode.KP_PLUS, 0, 0);
                case "-":
                    return handleButtonTouch(action, SDL2.Scancode.KP_MINUS, SDL2.Keycode.MINUS, 0, 0);
                case "*":
                    return handleButtonTouch(action, SDL2.Scancode.KP_MULTIPLY, SDL2.Keycode.KP_MULTIPLY, 0, 0);
                case "/":
                    return handleButtonTouch(action, SDL2.Scancode.KP_DIVIDE, SDL2.Keycode.KP_DIVIDE, 0, 0);
                case "=":
                    return handleButtonTouch(action, SDL2.Scancode.EQUALS, SDL2.Keycode.EQUALS, 0, 0);
                case ".":
                    return handleButtonTouch(action, SDL2.Scancode.KP_PERIOD, SDL2.Keycode.KP_PERIOD, 0, 0);
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
        ViewGroup vg1 = viewPad.findViewById(R.id.layoutRow1);
        for (int i = 0; i < vg1.getChildCount(); i++) {
            Button btn = (Button) vg1.getChildAt(i);
            btn.setText("" + (chkShift.isChecked() ? arrUp[i] : arrdown[i]));
        }
    }

    private void switchShift2() {
        char[] arrUp = {'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '{', '}', '|'};
        char[] arrDown = {'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']', '\\'};
        ViewGroup vg = viewPad.findViewById(R.id.layoutRow2);
        for (int i = 0; i < vg.getChildCount(); i++) {
            Button btn = (Button) vg.getChildAt(i);
            btn.setText("" + (chkShift.isChecked() ? arrUp[i] : arrDown[i]));
        }
    }

    private void switchShift3() {
        char[] arrUp = {'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ':', '\"'};
        char[] arrDown = {'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\''};
        ViewGroup vg = viewPad.findViewById(R.id.layoutRow3);
        for (int i = 0; i < vg.getChildCount(); i++) {
            Button btn = (Button) vg.getChildAt(i);
            LogEx.i(">>>>>" + i);
            btn.setText("" + (chkShift.isChecked() ? arrUp[i] : arrDown[i]));
        }
    }

    private void switchShift4() {
        char[] arrUp = {'Z', 'X', 'C', 'V', 'B', 'N', 'M', '<', '>', '?'};
        char[] arrDown = {'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/'};
        ViewGroup vg = viewPad.findViewById(R.id.layoutRow4);
        for (int i = 0; i < vg.getChildCount(); i++) {
            Button btn = (Button) vg.getChildAt(i);
            btn.setText("" + (chkShift.isChecked() ? arrUp[i] : arrDown[i]));
        }
    }
}
