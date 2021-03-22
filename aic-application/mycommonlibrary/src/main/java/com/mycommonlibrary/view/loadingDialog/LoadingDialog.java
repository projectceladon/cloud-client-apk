package com.mycommonlibrary.view.loadingDialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.mycommonlibrary.R;

/**
 * Created by Wood on 2016/4/12.
 */
public class LoadingDialog extends Dialog {
    public static Dialog show(Context context, String info) {
        LoadingDialog dlg = new LoadingDialog(context);
        dlg.setInfo(info);
        dlg.setCancelable(true);
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
        return dlg;
    }

    public static Dialog show(Context context) {
        return LoadingDialog.show(context, "加载中...");
    }

    private View contentView;
    private LVCircularRing circle;

    public LoadingDialog(Context context) {
        super(context, R.style.loading_dialog);
        contentView = LayoutInflater.from(context).inflate(R.layout.dialog_layout_progress, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(contentView);
        circle = findViewById(R.id.pg_loading);
    }

    public void setInfo(String info) {
        if (!TextUtils.isEmpty(info) && info.length() <= 6) {
            contentView.findViewById(R.id.tv_info).setVisibility(View.VISIBLE);
            ((TextView) contentView.findViewById(R.id.tv_info)).setText(info);
        } else {
            contentView.findViewById(R.id.tv_info).setVisibility(View.GONE);
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