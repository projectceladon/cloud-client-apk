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

package com.intel.gamepad.utils.sink;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

public class BweStatsVideoSink implements VideoSink {
    private final Object layoutLock = new Object();
    private BweStatsEvents bweStatsEvents;

    @Override
    public void onFrame(VideoFrame videoFrame) {
        UpdateBweStats(videoFrame);
    }

    public void setBweStatsEvent(BweStatsEvents events) {
        synchronized (layoutLock) {
            bweStatsEvents = events;
        }
    }

    private void UpdateBweStats(VideoFrame frame) {
        synchronized (layoutLock) {
            double delay = frame.bweStats.lastDuration - frame.bweStats.startDuration;
            int iDelay = (int) Math.round(delay);
            if (iDelay > 0) {
                if (bweStatsEvents != null) {
                    bweStatsEvents.onBweStats(iDelay, frame.bweStats.frameSize, frame.bweStats.packetsLost);
                }
            }
        }
    }

    public interface BweStatsEvents {
        void onBweStats(int frameDelay, int frameSize, int packetsLost);
    }
}
