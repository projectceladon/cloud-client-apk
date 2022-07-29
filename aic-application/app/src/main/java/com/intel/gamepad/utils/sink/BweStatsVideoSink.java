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
        bweStatsEvents = events;
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
