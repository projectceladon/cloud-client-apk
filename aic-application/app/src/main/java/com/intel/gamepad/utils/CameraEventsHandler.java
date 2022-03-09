package com.intel.gamepad.utils;

import com.commonlibrary.utils.LogEx;

import org.webrtc.CameraVideoCapturer;

public class CameraEventsHandler implements CameraVideoCapturer.CameraEventsHandler {

    // Camera error handler - invoked when camera can not be opened
    // or any camera exception happens on camera thread.
    @Override
    public void onCameraError(String errorDescription) {
        LogEx.d("CameraEventsHandler.onCameraError: errorDescription " + errorDescription);
    }

    // Called when camera is disconnected.
    @Override
    public void onCameraDisconnected() {
        LogEx.d("CameraEventsHandler.onCameraDisconnected");
    }

    // Invoked when camera stops receiving frames
    @Override
    public void onCameraFreezed(String errorDescription) {
        LogEx.d("CameraEventsHandler.onCameraFreezed: errorDescription " + errorDescription);
    }

    // Callback invoked when camera is opening.
    @Override
    public void onCameraOpening(String cameraName) {
        LogEx.d("CameraEventsHandler.onCameraOpening: cameraName = " + cameraName);
    }

    // Callback invoked when first camera frame is available after camera is opened.
    @Override
    public void onFirstFrameAvailable() {
        LogEx.d("CameraEventsHandler.onFirstFrameAvailable");
    }

    // Callback invoked when camera closed.
    @Override
    public void onCameraClosed() {
        LogEx.d("CameraEventsHandler.onCameraClosed()");
    }
}
