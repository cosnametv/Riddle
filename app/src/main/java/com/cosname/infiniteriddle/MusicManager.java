package com.cosname.infiniteriddle;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;

public class MusicManager {
    private static MediaPlayer backgroundPlayer;
    private static MediaPlayer soundEffectPlayer;
    private static boolean shouldPlayBg = false;

    public static final int SOUND_LEVEL_UP = 1;

    public static void initialize(Context context) {
        if (backgroundPlayer == null) {
            try {
                backgroundPlayer = MediaPlayer.create(context, R.raw.main);
                if (backgroundPlayer != null) {
                    backgroundPlayer.setLooping(true);
                    backgroundPlayer.setOnErrorListener((mp, what, extra) -> {
                        release();
                        return true;
                    });
                }
            } catch (Exception e) {
                // Log error and release resources
                release();
            }
        }
    }

    public static void playSoundEffect(Context context, int soundType) {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean soundsOn = prefs.getBoolean("sounds_on", true);

        if (!soundsOn) return;

        int soundResId = 0;
        switch (soundType) {
            case SOUND_LEVEL_UP:
                soundResId = R.raw.level_up_sound;
                break;
        }

        if (soundResId != 0) {
            // Release previous sound if playing
            if (soundEffectPlayer != null) {
                soundEffectPlayer.release();
            }

            soundEffectPlayer = MediaPlayer.create(context, soundResId);
            soundEffectPlayer.setOnCompletionListener(mp -> {
                mp.release();
                soundEffectPlayer = null;
            });
            soundEffectPlayer.start();
        }
    }

    public static void start(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean musicOn = prefs.getBoolean("music_on", true);

        if (musicOn && backgroundPlayer != null && !backgroundPlayer.isPlaying()) {
            backgroundPlayer.start();
            shouldPlayBg = true;
        }
    }

    public static void pause() {
        if (backgroundPlayer != null && backgroundPlayer.isPlaying()) {
            backgroundPlayer.pause();
        }
        shouldPlayBg = false;
    }

    public static void release() {
        if (backgroundPlayer != null) {
            if (backgroundPlayer.isPlaying()) {
                backgroundPlayer.stop();
            }
            backgroundPlayer.release();
            backgroundPlayer = null;
        }

        if (soundEffectPlayer != null) {
            soundEffectPlayer.release();
            soundEffectPlayer = null;
        }

        shouldPlayBg = false;
    }

    public static void updateMusicState(Context context, boolean isOn) {
        SharedPreferences.Editor editor = context.getSharedPreferences("AppSettings", MODE_PRIVATE).edit();
        editor.putBoolean("music_on", isOn);
        editor.apply();

        if (isOn) {
            start(context);
        } else {
            pause();
        }
    }

    public static void updateSoundState(Context context, boolean isOn) {
        SharedPreferences.Editor editor = context.getSharedPreferences("AppSettings", MODE_PRIVATE).edit();
        editor.putBoolean("sounds_on", isOn);
        editor.apply();
    }

    public static boolean shouldPlay() {
        return shouldPlayBg;
    }
}