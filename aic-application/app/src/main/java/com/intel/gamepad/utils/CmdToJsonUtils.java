package com.intel.gamepad.utils;

import org.json.JSONObject;

import java.util.HashMap;

public class CmdToJsonUtils {
    public static final int HANDSHAKE = 0x8001;
    public static final int ENUMARATE = 0x8002;
    public static final int PLAY = 0x8003;
    public static final int CLOSE = 0x8004;
    public static final int RESET = 0x8005;
    public static final int NODEINFO = 0x8006;

    public static String toJson(String cmd, int reqId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("magic", 0x55AA55AA+"");
        map.put("msgHdr", reqId+"");
        map.put("payloadLen", cmd.length() + "");
        map.put("payload", cmd);

        String json = new JSONObject(map).toString();
        System.out.println(json);
        return json;
    }

    public static String getHandShakeJson(String cmd) {
        return toJson(cmd, HANDSHAKE);
    }

    public static String getPlayJson(String cmd) {
        return toJson(cmd, PLAY);
    }

    public static String getCloseJson(String cmd) {
        return toJson(cmd, CLOSE);
    }

    public static String getResetJson(String cmd) {
        return toJson(cmd, RESET);
    }

    public static String getNodeInfoJson(String cmd) {
        return toJson(cmd, NODEINFO);
    }
}
