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

package com.commonlibrary.view.loadingDialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.commonlibrary.R;

/**
 * Created by Wood on 2016/4/12.
 */
public class LoadingDialog extends Dialog {
    private final View contentView;
    private LVCircularRing circle;

    public LoadingDialog(Context context) {
        super(context, R.style.loading_dialog);
        contentView = LayoutInflater.from(context).inflate(R.layout.dialog_layout_progress, null);
    }

    public static Dialog show(Context context, String info) {
        LoadingDialog dlg = new LoadingDialog(context);
        dlg.setInfo(info);
        dlg.setCancelable(true);
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
        return dlg;
    }

    public static Dialog show(Context context) {
        return LoadingDialog.show(context, context.getString(R.string.loading));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(contentView);
        circle = findViewById(R.id.pg_loading);
    }

    public void setInfo(String info) {
        View tvInfo=contentView.findViewById(R.id.tv_info);
        if(tvInfo!=null){
            if (!TextUtils.isEmpty(info) && info.length() <= 6) {
                tvInfo.setVisibility(View.VISIBLE);
                ((TextView) tvInfo).setText(info);
            } else {
                tvInfo.setVisibility(View.GONE);
            }
        }
    }


    @Override
    public void show() {
        super.show();
        if (circle != null) {
            circle.startAnim();
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (circle != null) {
            circle.stopAnim();
        }
    }
}
