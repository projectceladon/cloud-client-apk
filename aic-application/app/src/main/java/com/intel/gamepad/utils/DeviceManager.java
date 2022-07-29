package com.intel.gamepad.utils;

import static com.intel.gamepad.app.AppConst.EXYNOS_PREFIX;
import static com.intel.gamepad.app.AppConst.INTEL_PREFIX;
import static com.intel.gamepad.app.AppConst.MTK_PREFIX;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;

import com.intel.gamepad.app.AppConst;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DeviceManager {

    private static DeviceManager mInstance;

    private DeviceManager() {
    }

    public static synchronized DeviceManager getInstance() {
        if (mInstance == null) {
            mInstance = new DeviceManager();
        }
        return mInstance;
    }

    private boolean checkMediaCodecSupportTypes(String codec) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            boolean boolEncoder = false;
            boolean boolDecoder = false;
            String TYPE_MEDIA = "hevc";
            switch (codec) {
                case AppConst.H264:
                    TYPE_MEDIA = "h264";
                    break;
                case AppConst.HEVC:
                    TYPE_MEDIA = "hevc";
                    break;
                case AppConst.VP9:
                    TYPE_MEDIA = "vp9";
                    break;
            }

            for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) {
                String mediaCodecName = mediaCodecInfo.getName().toLowerCase(Locale.ROOT);
                boolean isEncoder = mediaCodecInfo.isEncoder();
                if (mediaCodecName.contains(TYPE_MEDIA)
                        && isEncoder) {
                    boolEncoder = true;
                }
                if (mediaCodecName.contains(TYPE_MEDIA)
                        && !isEncoder) {
                    boolDecoder = true;
                }
            }
            if (boolEncoder && boolDecoder) {
                return true;
            } else {
                return isMediaCodecHardwareSupported(codec, mediaCodecList);
            }
        }
        return false;
    }

    private boolean isMediaCodecHardwareSupported(String codecType, MediaCodecList mediaCodecList) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            List<Codec> codecs = new ArrayList<>();
            for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) {
                if (isMediaCodecHardwareSupportType(codecType, mediaCodecInfo)) {
                    String mediaCodecName = mediaCodecInfo.getName().toLowerCase(Locale.ROOT);
                    boolean isEncoder = mediaCodecInfo.isEncoder();
                    if (mediaCodecName.startsWith(EXYNOS_PREFIX)) {
                        putMediaCodecHardwareData(EXYNOS_PREFIX, codecs, isEncoder);
                    } else if (mediaCodecName.startsWith(INTEL_PREFIX)) {
                        putMediaCodecHardwareData(INTEL_PREFIX, codecs, isEncoder);
                    } else if (mediaCodecName.startsWith(MTK_PREFIX)) {
                        putMediaCodecHardwareData(MTK_PREFIX, codecs, isEncoder);
                    }
                }
            }
            for (Codec codec : codecs) {
                if (codec.isSupport()) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }


    private void putMediaCodecHardwareData(String codecType, List<Codec> codecs, boolean isEncoder) {
        for (Codec codec : codecs) {
            if (codecType.equals(codec.codecType)) {
                if (isEncoder) {
                    codec.encoder = true;
                } else {
                    codec.decoder = true;
                }
            }
        }
        Codec codec = new Codec();
        codec.codecType = codecType;
        if (isEncoder) {
            codec.encoder = true;
        } else {
            codec.decoder = true;
        }
        codecs.add(codec);
    }

    private boolean isMediaCodecHardwareSupportType(String codecType, MediaCodecInfo mediaCodecInfo) {
        String[] types = mediaCodecInfo.getSupportedTypes();
        for (String type : types) {
            if (codecType.equals(type)) {
                return true;
            }
        }
        return false;
    }


    public boolean[] checkMediaCodecSupport(String[] dodecs) {
        boolean[] result = new boolean[dodecs.length];
        for (int i = 0; i < dodecs.length; i++) {
            result[i] = checkMediaCodecSupportTypes(dodecs[i]);
        }
        return result;
    }


    static class Codec {
        String codecType;
        boolean encoder;
        boolean decoder;

        boolean isSupport() {
            return encoder && decoder;
        }
    }


}
