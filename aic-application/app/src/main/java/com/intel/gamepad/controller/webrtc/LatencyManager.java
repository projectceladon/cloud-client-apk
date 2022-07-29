package com.intel.gamepad.controller.webrtc;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.intel.gamepad.app.AppConst;

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
import java.util.Locale;

import E2ELatency.LatencyMsgOuterClass.LatencyMsg;

public class LatencyManager implements EglRenderer.RenderCallback {
    private static final int MSG_FRAME_DROPPED = 1;
    private static final int MSG_FRAME_RENDERED = 2;
    private static final String TAG = "LatencyManager";
    private final SurfaceViewRenderer mRender;
    private final Context mContext;
    private final Handler mainHandler;
    private HandlerThread handlerThread;
    private Handler mHandler;
    private FileOutputStream outputStream;
    private Callback mCallback;
    private long timeStampEnable;


    public LatencyManager(Context context, SurfaceViewRenderer render, Handler handler) {
        mRender = render;
        mContext = context;
        mainHandler = handler;
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
                mCallback = new Callback(outputStream, mainHandler);
                mHandler = new Handler(handlerThread.getLooper(), mCallback);
            }
            mCallback.resetLatency();
            Log.d(TAG, "setEnable " + timeStampEnable);
        } else {
            mRender.registerRenderCallback(null);
            LatencyLogger.getInstance().clear();
            Log.d(TAG, "disable");
        }
    }

    private void initFilePath() {
        boolean exists = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        if (!exists) {
            Log.e(TAG, "initFilePath but sd not exits, no write");
        }
        File sdCardFile = mContext.getExternalFilesDir(null);
        File fileDir = new File(sdCardFile, "e2eLatency");
        if (!fileDir.exists()) {
            if (!fileDir.mkdir()) {
                outputStream = null;
                Log.e(TAG, "initFilePath mkdir fail");
                return;
            }
        }

        SimpleDateFormat timesdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        String fileName = timesdf.format(new Date());//获取系统时间
        File file = new File(fileDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            Log.d(TAG, "initFilePath " + file.getAbsolutePath());
            outputStream = fos;
        } catch (Exception e) {
            outputStream = null;
            Log.e(TAG, "initFilePath except", e);
        }
    }

    public void onStreamExit() {
        setEnable(false);
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mCallback.onExit();
        }
        if (handlerThread != null) {  //exit
            handlerThread.quit();
        }

        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onFrameDroped(long timestampNs, long receiveTime) {
        Log.d(TAG, "onFrameDropped timestampNs " + timestampNs + " - " + receiveTime);
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(MSG_FRAME_DROPPED);
            Bundle bundle = new Bundle();
            bundle.putLong("receiveTime", receiveTime);
            bundle.putLong("timestampNs", timestampNs);
            msg.obj = bundle;
            mHandler.sendMessage(msg);
        }
    }

    @Override
    public void onFrameDrawed(long timestampNs, long receiveTime, long drawStartTime, long drawEndTime, boolean draw) {
        Log.e(TAG, "onFrameDrawn timestampNs " + timestampNs + " - " + receiveTime + " - " + drawStartTime + " - " + drawEndTime + " - " + draw);
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

    static class Callback implements Handler.Callback {
        FileOutputStream outputStream;
        Handler mHandler;
        private int tick = 0;
        private long avgLatencyServer = 0;
        private long avgLatencyE2E = 0;

        Callback(FileOutputStream stream, Handler handler) {
            outputStream = stream;
            mHandler = handler;
        }

        void onExit() {
            outputStream = null;
        }

        void resetLatency() {
            tick = 0;
            avgLatencyServer = 0;
            avgLatencyE2E = 0;
        }

        private void updateAverageLatency(long latency, boolean sever) {
            if (sever) {
                avgLatencyServer = (tick * avgLatencyServer + latency) / (tick + 1);
            } else {
                avgLatencyE2E = (tick * avgLatencyE2E + latency) / (tick + 1);
            }
        }

        private void onFrameDropped(Bundle bundle) {
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
                    sb.append("Client: RenderFrame FrameDropped: protobuf message: ").append(latencyMsg).append("\n").
                            append("Client input timestamp:").append(latencyMsg.getClientInputTime()).
                            append("received timestamp:").append(latencyMsg.getClientReceivedTime()).append("\n").
                            append("Client E2E Latency, ms:").append((latencyMsg.getClientReceivedTime() -
                                    latencyMsg.getClientInputTime()) / (1000 * 1000)).append(", render, ms:").append("-1\n");
                    Log.d(TAG, "onFrameDropped " + sb);
                    if (outputStream != null) {
                        FileOutputStream stream = outputStream;
                        stream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                    }
                } catch (InvalidProtocolBufferException e) {
                    Log.e(TAG, "onFrameDropped InvalidProtocolBufferException ", e);
                } catch (IOException e) {
                    Log.e(TAG, "onFrameDropped write except", e);
                }
                LatencyLogger.getInstance().removeMessage(timestampNs);
            } else {
                Log.e(TAG, "onFrameDropped find no receive info");
            }
        }

        private void onFrameDrawn(Bundle bundle) {
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
                    Log.e(TAG, "onFrameDrawn latencyJson" + latencyJson);
                    JsonFormat.parser().ignoringUnknownFields().merge(latencyJson, builder);
                    builder.setClientReceivedTime(receiveTime);
                    builder.setClientRenderTime((drawEndTime - drawStartTime) / (1000 * 1000));  // ms
                    builder.setClientDecodeTime(msg.getDecodeTime());
                    LatencyMsg latencyMsg = builder.build();
                    StringBuilder sb = new StringBuilder();
                    sb.append("Client: RenderFrame onFrameDrawn: protobuf message: \n").append(latencyMsg).append("\n").
                            append("Client input timestamp:").append(latencyMsg.getClientInputTime()).
                            append("received timestamp:").append(latencyMsg.getClientReceivedTime()).append("\n").
                            append("Client E2E Latency, ms:").append((latencyMsg.getClientReceivedTime() -
                                    latencyMsg.getClientInputTime()) / (1000 * 1000)).append(", render, ms:").
                            append(latencyMsg.getClientRenderTime()).append(", draw: ").append(draw).append("\n\n");
                    Log.d(TAG, "onFrameDrawn " + sb);
                    if (outputStream != null) {
                        FileOutputStream stream = outputStream;
                        stream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    long latencyServer = (latencyMsg.getServerSendTime() - latencyMsg.getServerReceivedTime()) / (1000 * 1000);
                    long latencyE2E = (latencyMsg.getClientReceivedTime() - latencyMsg.getClientInputTime()) / (1000 * 1000);
                    updateAverageLatency(latencyServer, true);
                    updateAverageLatency(latencyE2E, false);
                    tick++;
                    mHandler.obtainMessage(AppConst.MSG_LATENCY_UPDATED,
                            "serverLatency:" + latencyServer + "\nE2Elatency:" + latencyE2E +
                                    "\navgServerLatency:" + avgLatencyServer + "\navgE2ELatency:" +
                                    avgLatencyE2E).sendToTarget();
                } catch (InvalidProtocolBufferException e) {
                    Log.e(TAG, "onFrameDrawn InvalidProtocolBufferException ", e);
                } catch (IOException e) {
                    Log.e(TAG, "onFrameDrawn write except", e);
                }
                LatencyLogger.getInstance().removeMessage(timestampNs);
            } else {
                Log.i(TAG, "onFrameDrawn find no receive info " + timestampNs);
            }

        }

        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_FRAME_DROPPED:
                    onFrameDropped((Bundle) msg.obj);
                    return true;
                case MSG_FRAME_RENDERED:
                    onFrameDrawn((Bundle) msg.obj);
                    return true;
            }
            return false;
        }
    }
}
