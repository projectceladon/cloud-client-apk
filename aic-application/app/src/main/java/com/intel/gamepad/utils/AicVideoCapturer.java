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


import android.util.Log;

import org.webrtc.Camera1Capturer;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import owt.base.Stream;
import owt.base.VideoCapturer;

public final class AicVideoCapturer extends Camera1Capturer implements VideoCapturer {
    private static final String TAG = "AicVideoCapturer";
    public static String cameraId;
    private int width, height, fps;

    private AicVideoCapturer(String deviceName, CameraEventsHandler eventHandler,
                             boolean captureToTexture) {
        super(deviceName, eventHandler, captureToTexture);
    }

    public static AicVideoCapturer create(int width, int height, CameraEventsHandler eventHandler) {
        String deviceName = getDeviceName(true);
        AicVideoCapturer capturer;
        String cameraLatencyDebugEnabled;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop camera.latency.debug");
            input = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8), 1024);
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
            capturer = new AicVideoCapturer(deviceName, eventHandler, false);
        } else {
            capturer = new AicVideoCapturer(deviceName, eventHandler, true);
        }
        capturer.width = width;
        capturer.height = height;
        capturer.fps = 30;
        Log.d(TAG, "capturer: " + capturer);
        return capturer;
    }

    private static String getDeviceName(boolean captureToTexture) {
        String deviceName = null;
        int numOfCamerasAvailable = 0;
        int camId = Integer.parseInt(cameraId);

        CameraEnumerator enumerator = new Camera1Enumerator(captureToTexture);
        for (String device : enumerator.getDeviceNames()) {
            numOfCamerasAvailable++;
        }
        Log.d(TAG, "Number of cameras available in the client = " + numOfCamerasAvailable);

        if (numOfCamerasAvailable >= 2) {
            // Switch between cameras based on user request by using corresponding camera Id.
            deviceName = enumerator.getDeviceNames()[camId];
        } else if (numOfCamerasAvailable == 1) {
            Log.d(TAG, "Only One camera HW is available in the client device");
            deviceName = enumerator.getDeviceNames()[0];
        } else {
            Log.e(TAG, "[error] No camera HW is available in the client device");
        }

        Log.d(TAG, "deviceName = " + deviceName);
        return deviceName;
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
}
