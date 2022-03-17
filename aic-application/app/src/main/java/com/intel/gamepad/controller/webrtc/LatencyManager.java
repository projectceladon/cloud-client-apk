package com.intel.gamepad.controller.webrtc;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

import com.commonlibrary.utils.LogEx;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import org.webrtc.EglRenderer;
import org.webrtc.LatencyLogger;
import org.webrtc.LatencyNativeMessage;
import org.webrtc.SurfaceViewRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import E2ELatency.LatencyMsgOuterClass.LatencyMsg;

public class LatencyManager implements EglRenderer.RenderCallback {
    private static final String TAG = "LatencyManager";
    private boolean enable = false;
    private SurfaceViewRenderer mRender;
    private HandlerThread handlerThread;
    private Handler mHandler;
    private boolean writeToFile = true;
    private Context mContext;
    private FileOutputStream outputStream;
    private Callback mCallback;
    private long timeStampEnable;

    private static final int MSG_FRAME_DROPED = 1;
    private static final int MSG_FRAME_RENDERED = 2;

    public LatencyManager(Context context, SurfaceViewRenderer render){
        mRender = render;
        mContext = context;
    }

    public void setEnable(boolean enable) {
        if (enable) {
            mRender.registerRenderCallback(this);
            LatencyLogger.getInstance().clear();
            if (handlerThread == null) {
                timeStampEnable = System.nanoTime();
                initFilePath();
                handlerThread = new HandlerThread("LatencyManager");
                handlerThread.start();
                mCallback = new Callback(outputStream);
                mHandler = new Handler(handlerThread.getLooper(), mCallback);
            }
            LogEx.d("setEnable " + timeStampEnable);
        } else {
            mRender.registerRenderCallback(null);
            LatencyLogger.getInstance().clear();
            LogEx.d("disable");
        }
    }

    private void initFilePath() {
        if (!writeToFile) {
            outputStream = null;
            return;
        }
        boolean exists = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        if (!exists) {
            LogEx.e("initFilePath but sd not exits, no write");
        }
        File sdCardFile = mContext.getExternalFilesDir(null);
        File fileDir = new File(sdCardFile, "e2eLatency");
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        SimpleDateFormat timesdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String fileName =timesdf.format(new Date());//获取系统时间
        File file = new File(fileDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            LogEx.d("initFilePath " + file.getAbsolutePath());
            outputStream = fos;
        } catch (Exception e) {
            outputStream = null;
            LogEx.e("initFilePath excp", e);
        }
    }

    static class Callback implements Handler.Callback {
        FileOutputStream outputStream;
        Callback(FileOutputStream stream) {
            outputStream = stream;
        }
        void onExit() {
            outputStream = null;
        }

        private void onFrameDroped(Bundle bundle) {
            long receiveTime = bundle.getLong("receiveTime");
            long timestampNs = bundle.getLong("timestampNs");
            LatencyNativeMessage msg = LatencyLogger.getInstance().getMessage(timestampNs);
            if (msg != null) {
                try {
                    LatencyMsg.Builder builder = LatencyMsg.newBuilder();
                    String latencyJson = new String(msg.geLatencyBuffer());
                    JsonFormat.parser().ignoringUnknownFields().merge(latencyJson, builder);
                    builder.setClientReceivedTime(receiveTime);
                    LatencyMsg latencyMsg = builder.build();
                    StringBuilder sb = new StringBuilder();
                    sb.append("Client: RenderFrame FrameDroped: protobuf message: " + latencyMsg + "\n").
                            append("Client input timestamp:").append(latencyMsg.getClientInputTime()).
                            append("received timestamp:").append(latencyMsg.getClientReceivedTime()).append("\n").
                            append("Client E2E Latency, ms:").append((latencyMsg.getClientReceivedTime() -
                            latencyMsg.getClientInputTime()) / (1000 * 1000)).append(", render, ms:").append("-1\n");
                    LogEx.d("onFrameDroped " + sb);
                    if (outputStream != null) {
                        FileOutputStream stream = outputStream;
                        if (stream != null) {
                            stream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                        }
                    }

                } catch (InvalidProtocolBufferException e) {
                    LogEx.e("onFrameDroped InvalidProtocolBufferException ", e);
                } catch (IOException e) {
                    LogEx.e("onFrameDroped write excp", e);
                }
                LatencyLogger.getInstance().removeMessage(timestampNs);
            } else {
                LogEx.e("onFrameDroped find no receive info");
            }

        }

        private void onFrameDrawed(Bundle bundle) {
            long receiveTime = bundle.getLong("receiveTime");
            long timestampNs = bundle.getLong("timestampNs");
            long drawStartTime = bundle.getLong("drawStartTime");
            long drawEndTime = bundle.getLong("drawEndTime");
            boolean draw = bundle.getBoolean("draw");
            LatencyNativeMessage msg = LatencyLogger.getInstance().getMessage(timestampNs);
            if (msg != null) {
                try {
                    LatencyMsg.Builder builder = LatencyMsg.newBuilder();
                    String latencyJson = new String(msg.geLatencyBuffer());
                    LogEx.e("onFrameDrawed latencyJson" + latencyJson);
                    JsonFormat.parser().ignoringUnknownFields().merge(latencyJson, builder);
                    builder.setClientReceivedTime(receiveTime);
                    builder.setClientRenderTime((drawEndTime - drawStartTime) / (1000 * 1000));  // ms
                    builder.setClientDecodeTime(msg.getDecodeTime());
                    LatencyMsg latencyMsg = builder.build();
                    StringBuilder sb = new StringBuilder();
                    sb.append("Client: RenderFrame onFrameDrawed: protobuf message: \n" + latencyMsg + "\n").
                            append("Client input timestamp:").append(latencyMsg.getClientInputTime()).
                            append("received timestamp:").append(latencyMsg.getClientReceivedTime()).append("\n").
                            append("Client E2E Latency, ms:").append((latencyMsg.getClientReceivedTime() -
                            latencyMsg.getClientInputTime()) / (1000 * 1000)).append(", render, ms:").
                            append(latencyMsg.getClientRenderTime()).append(", draw: ").append(draw).append("\n\n");
                    LogEx.e("onFrameDrawed " + sb);
                    if (outputStream != null) {
                        FileOutputStream stream = outputStream;
                        if (stream != null) {
                            stream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                        }
                    }
                } catch (InvalidProtocolBufferException e) {
                    LogEx.e("onFrameDrawed InvalidProtocolBufferException ", e);
                } catch (IOException e) {
                    LogEx.e("onFrameDrawed write excp", e);
                }
                LatencyLogger.getInstance().removeMessage(timestampNs);
            } else {
                LogEx.e("onFrameDrawed find no receive info " + timestampNs);
            }

        }

        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_FRAME_DROPED:
                    onFrameDroped((Bundle) msg.obj);
                    return true;
                case MSG_FRAME_RENDERED:
                    onFrameDrawed((Bundle) msg.obj);
                    return true;
            }
            return false;
        }
    }

    public void onStreamExit() {
        setEnable(false);
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mCallback.onExit();
        }
        if (handlerThread!= null) {  //exit
            handlerThread.quit();
        }

        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (Exception exception) {

            }
        }

    }

    @Override
    public void onFrameDroped(long timestampNs, long receiveTime) {
        LogEx.d("onFrameDroped timestampNs " + timestampNs +" - " + receiveTime);
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(MSG_FRAME_DROPED);
            Bundle bundle = new Bundle();
            bundle.putLong("receiveTime", receiveTime);
            bundle.putLong("timestampNs", timestampNs);
            msg.obj = bundle;
            mHandler.sendMessage(msg);
        }
    }

    @Override
    public void onFrameDrawed(long timestampNs, long receiveTime, long drawStartTime, long drawEndTime, boolean draw) {
        LogEx.e("onFrameDrawed timestampNs " + timestampNs +" - " + receiveTime +" - "+ drawStartTime +" - "+ drawEndTime+" - " + draw);
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(MSG_FRAME_RENDERED);
            Bundle bundle = new Bundle();
            bundle.putLong("receiveTime", receiveTime);
            bundle.putLong("timestampNs", timestampNs);
            bundle.putLong("drawStartTime", drawStartTime);
            bundle.putLong("drawEndTime", drawEndTime);
            bundle.putBoolean("draw", draw);
            msg.obj = bundle;
            mHandler.sendMessage(msg);
        }
    }

}
