package com.intel.gamepad.utils;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;

import java.util.Locale;

public class DeviceMnager {

    private static DeviceMnager mInstance;

    private DeviceMnager() {
    }

    public static synchronized DeviceMnager getInstance() {
        if (mInstance == null) {
            mInstance = new DeviceMnager();
        }
        return mInstance;
    }

    public boolean checkMediaCodecSupportTypes(Dodec codec) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            boolean boolEncoder = false;
            boolean boolDecoder = false;
            String TYPE_MEDIA_ENCODER = "encoder";
            String TYPE_MEDIA_DECODER = "decoder";
            String TYPE_MEDIA = "hevc";
            switch (codec) {
                case h264:
                    TYPE_MEDIA = "h264";
                    break;
                case hevc:
                    TYPE_MEDIA = "hevc";
                    break;
                case vp9:
                    TYPE_MEDIA = "vp9";
                    break;
            }

            for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) {
                String mediaCodecName = mediaCodecInfo.getName().toLowerCase(Locale.ROOT);
                if (mediaCodecName.contains(TYPE_MEDIA)
                        && mediaCodecName.contains(TYPE_MEDIA_ENCODER)) {
                    boolEncoder = true;
                }
                if (mediaCodecName.contains(TYPE_MEDIA)
                        && mediaCodecName.contains(TYPE_MEDIA_DECODER)) {
                    boolDecoder = true;
                }
            }

            if (!(boolEncoder && boolDecoder)) {
                return false;
            } else {
                return true;
            }

        }
        return false;
    }

    public enum Dodec {
        hevc,
        h264,
        vp9
    }

}
