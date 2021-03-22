package com.intel.gamepad.utils;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

public class AudioHelper {
    private AudioHelper(Context context) {
        initAudioManager(context);
    }

    public synchronized static AudioHelper getInstance(Context context) {
        if (INSTANCE == null)
            INSTANCE = new AudioHelper(context);
        return INSTANCE;
    }

    private static AudioHelper INSTANCE;
    private Context context;
    private AudioManager audioManager;

    /**
     * 初始化音频管理器
     */
    private void initAudioManager(Context context) {
        this.context = context.getApplicationContext();
        audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } else {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
        }
        audioManager.setSpeakerphoneOn(true);//默认为扬声器播放
    }

    /**
     * 设置是否使用扬声器
     *
     * @param speakerMode true为使用扬声器,false为使用耳机
     */
    public void speakerhoneSwitch(boolean speakerMode) {
        audioManager.setSpeakerphoneOn(speakerMode);
    }

    /**
     * 获取音频流的最大音量
     *
     * @param streamType 音频流类型
     */
    public int getMaxVolume(int streamType) {
        return audioManager.getStreamMaxVolume(streamType);
    }

    /**
     * 获取音频流的当前音量
     *
     * @param streamType 音频流类型
     */
    public int getCurrentVolume(int streamType) {
        return audioManager.getStreamVolume(streamType);
    }

    /**
     * 获取播放音乐时的最大音响
     */
    public int getMusicMaxVolume() {
        return getMaxVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * 获取铃声的最大音响
     */
    public int getRingMaxVolume() {
        return getMaxVolume(AudioManager.STREAM_RING);
    }

    /**
     * 获取提示音的最大音响
     */
    public int getAlamMaxVolume() {
        return getMaxVolume(AudioManager.STREAM_ALARM);
    }

    /**
     * 获取通知的最大音响
     */
    public int getNotificationMaxVolume() {
        return getMaxVolume(AudioManager.STREAM_NOTIFICATION);
    }

    /**
     * 获取系统音的最大音响
     */
    public int getSystemMaxVolume() {
        return getMaxVolume(AudioManager.STREAM_SYSTEM);
    }

    /**
     * 获取通话的最大音响
     */
    public int getVoiceCallMaxVolume() {
        return getMaxVolume(AudioManager.STREAM_VOICE_CALL);
    }

    /**
     * 获取双音频的最大音响
     */
    public int getDTMFMaxVolume() {
        return getMaxVolume(AudioManager.STREAM_DTMF);
    }

    /**
     * 直接设置音响大小
     *
     * @param streamType 音频类类型
     * @param volume     音频大小
     */
    public void setVolume(int streamType, int volume) {
        volume = (volume > getMaxVolume(streamType)) ? getMaxVolume(streamType) : volume;
        audioManager.setStreamVolume(streamType,
                volume,
                AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
    }

    /**
     * 调大音量
     *
     * @param streamType 音频类类型
     */
    public void raiseVolume(int streamType) {
        int currentVolume = getCurrentVolume(streamType);
        if (currentVolume < getMaxVolume(streamType)) {
            audioManager.adjustStreamVolume(streamType,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
        }
    }

    /**
     * 调小音量
     */
    public void lowerVolume(int streamType) {
        int currentVolume = getCurrentVolume(streamType);
        if (currentVolume > 0) {
            audioManager.adjustStreamVolume(streamType,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
        }
    }

    public void raiseMusicVolume() {
        raiseVolume(AudioManager.STREAM_MUSIC);
    }

    public void lowerMusicVolume() {
        lowerVolume(AudioManager.STREAM_MUSIC);
    }

}
