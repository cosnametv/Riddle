package com.cosname.infiniteriddle;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;

public class SoundManager {
    private static MediaPlayer mediaPlayer;

    public static void playCorrect(Context context) {
        playSound(context, R.raw.correct);
    }

    public static void playWrong(Context context) {
        playSound(context, R.raw.wrong);
    }

    public static void playLevelUp(Context context) {
        playSound(context, R.raw.level_up_sound);
    }

    private static void playSound(Context context, int resId) {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean soundOn = prefs.getBoolean("sounds_on", true);
        if (!soundOn) return;

        release();
        try {
            mediaPlayer = MediaPlayer.create(context, resId);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> release());
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    release();
                    return true;
                });
                mediaPlayer.start();
            }
        } catch (Exception e) {
            // Log error and release resources
            release();
        }
    }

    public static void updateSoundState(Context context, boolean isOn) {
        SharedPreferences.Editor editor = context.getSharedPreferences("AppSettings", MODE_PRIVATE).edit();
        editor.putBoolean("sounds_on", isOn);
        editor.apply();
        if (!isOn) {
            release();
        }
    }

    public static void release() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
} 