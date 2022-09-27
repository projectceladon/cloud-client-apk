package com.intel.gamepad.controller.webrtc;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.intel.gamepad.bean.ConnStatsBean;
import com.intel.gamepad.owt.p2p.P2PHelper;
import com.intel.gamepad.utils.TimingTaskUtil;

import org.webrtc.RTCStatsReport;

import owt.base.ActionCallback;
import owt.base.OwtError;
import owt.p2p.P2PClient;

public class StatsTextView extends AppCompatTextView  {
    private static final String TAG = "StatsTextView";
    private TimingTaskUtil statsTaskUtil;
    private final Object statsLock = new Object();
    private ConnStatsBean statsBean;

    final Runnable timingTaskRunnable = () -> {
        synchronized (statsLock) {
            P2PClient client = P2PHelper.getClient();
            if(client!=null){
                client.getStats(P2PHelper.peerId, new ActionCallback<>() {
                    @Override
                    public void onSuccess(RTCStatsReport rtcStatsReport) {
                        if(statsBean == null ){
                            statsBean = new ConnStatsBean();
                        }
                        String info = statsBean.parseReport(rtcStatsReport);
                        if(info !=null){
                            updateLatencyMsg(info);
                        }
                    }

                    @Override
                    public void onFailure(OwtError owtError) {

                    }
                });
            }

        }
    };


    public StatsTextView(Context context) {
        super(context);
    }

    public StatsTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    private Handler handler;
    public void open(){
        if(handler ==null){
            handler = new Handler(getContext().getMainLooper());
        }
        setText("");
        setVisibility(View.VISIBLE);
        if(statsTaskUtil==null){
            statsTaskUtil = new TimingTaskUtil(handler);
        }
        statsTaskUtil.startTask(timingTaskRunnable, 1000, true);
    }

    public void close(){
        if(handler!=null){
            handler.removeCallbacksAndMessages(null);
        }
        setVisibility(View.GONE);
        if(statsTaskUtil!=null){
            statsTaskUtil.endTask(timingTaskRunnable);
        }
    }

    public void updateLatencyMsg(final String msg) {
        if(getContext() instanceof Activity){
            ((Activity)getContext()).runOnUiThread(() -> setText(msg));
        }
    }



}
