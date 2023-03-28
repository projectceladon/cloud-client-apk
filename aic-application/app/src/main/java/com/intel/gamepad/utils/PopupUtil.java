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
