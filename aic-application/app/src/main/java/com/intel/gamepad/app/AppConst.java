package com.intel.gamepad.app;

public class AppConst {
    // 消息事件常量
    public static final int MSG_QUIT = 1;
    public static final int MSG_SHOW_CONTROLLER = 2;
    public static final int MSG_UPDATE_CONTROLLER = 3;
    public static final int MSG_NO_STREAM_ADDED = 4;
    public static final int MSG_UNRECOVERABLE = 5;
    public static final int MSG_LATENCY_UPDATED = 6;
    public static final int EXIT_NORMAL = 0;
    public static final int EXIT_DISCONNECT = -1;

    public static final String H264 = "video/avc";
    public static final String CODEC_WHITELIST_FILENAME = "mediaCodec.xml";
}
