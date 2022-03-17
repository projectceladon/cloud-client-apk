package com.intel.gamepad.utils;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.commonlibrary.utils.DensityUtils;

public class PopupUtil {

    public static PopupWindow createPopup(View parent, View popView, int xPos) {
        PopupWindow mPop = new PopupWindow(popView, xPos == -1 ? DensityUtils.dp2px(284f) : xPos, LinearLayout.LayoutParams.WRAP_CONTENT);
        mPop.setFocusable(false);
        int[] location = new int[2];
        parent.getLocationOnScreen(location);
        mPop.showAsDropDown(parent, 0, 0);
        mPop.setOutsideTouchable(false);
        mPop.setTouchable(true);
        mPop.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mPop.getContentView().setSystemUiVisibility(Const.FULL_SCREEN_FLAG);
        mPop.update();
        return mPop;
    }

}
