package com.intel.gamepad.utils;

import com.mycommonlibrary.utils.LogEx;

import org.webrtc.Camera1Capturer;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;

import owt.base.Stream;
import owt.base.VideoCapturer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class AicVideoCapturer extends Camera1Capturer implements VideoCapturer {
    private int width, height, fps;

    private AicVideoCapturer(String deviceName, boolean captureToTexture) {
        super(deviceName, null, captureToTexture);
    }

    public static AicVideoCapturer create(int width, int height) {
        String deviceName = getDeviceName(true);
        AicVideoCapturer capturer = null;
        String cameraLatencyDebugEnabled = null;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop camera.latency.debug");
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            cameraLatencyDebugEnabled = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (cameraLatencyDebugEnabled != null && cameraLatencyDebugEnabled.equals("true")) {
            capturer = new AicVideoCapturer(deviceName, false);
        } else {
            capturer = new AicVideoCapturer(deviceName, true);
        }
        capturer.width = width;
        capturer.height = height;
        capturer.fps = 30;
        LogEx.d("capturer: " + capturer);
        return capturer;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getFps() {
        return fps;
    }

    @Override
    public Stream.StreamSourceInfo.VideoSourceInfo getVideoSource() {
        return Stream.StreamSourceInfo.VideoSourceInfo.CAMERA;
    }

    public void switchCamera() {
        super.switchCamera(null);
    }

    public void dispose() {
        super.dispose();
    }

    private static String getDeviceName(boolean captureToTexture) {
        CameraEnumerator enumerator = new Camera1Enumerator(captureToTexture);

        String deviceName = null;
        for (String device : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(device)) {
                deviceName = device;
                break;
            }
        }

        return deviceName == null ? enumerator.getDeviceNames()[0] : deviceName;
    }
}
