package com.intel.gamepad.utils;


import android.util.Log;

import org.webrtc.CameraVideoCapturer;

public class CameraEventsHandler implements CameraVideoCapturer.CameraEventsHandler {
    public static final Object cameraLock = new Object();
    private static final String TAG = "CameraEventsHandler";
    public static boolean isCameraSessionClosed = true;

    // Camera error handler - invoked when camera can not be opened
    // or any camera exception happens on camera thread.
    @Override
    public void onCameraError(String errorDescription) {
        Log.d(TAG, "CameraEventsHandler.onCameraError: errorDescription " + errorDescription);
    }

    // Called when camera is disconnected.
    @Override
    public void onCameraDisconnected() {
        Log.d(TAG, "CameraEventsHandler.onCameraDisconnected");
    }

    // Invoked when camera stops receiving frames
    @Override
    public void onCameraFreezed(String errorDescription) {
        Log.d(TAG, "CameraEventsHandler.onCameraFreezed: errorDescription " + errorDescription);
    }

    // Callback invoked when camera is opening.
    @Override
    public void onCameraOpening(String cameraName) {
        Log.d(TAG, "CameraEventsHandler.onCameraOpening: cameraName = " + cameraName);
        isCameraSessionClosed = false;
    }

    // Callback invoked when first camera frame is available after camera is opened.
    @Override
    public void onFirstFrameAvailable() {
        Log.d(TAG, "CameraEventsHandler.onFirstFrameAvailable");
    }

    // Callback invoked when camera closed.
    @Override
    public void onCameraClosed() {
        Log.d(TAG, "CameraEventsHandler.onCameraClosed()");
        synchronized (cameraLock) {
            isCameraSessionClosed = true;
            cameraLock.notify();
        }
    }
}
