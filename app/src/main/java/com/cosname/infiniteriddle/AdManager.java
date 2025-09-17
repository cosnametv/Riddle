package com.cosname.infiniteriddle;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.lang.ref.WeakReference;

public class AdManager {
    private static AdManager instance;
    private RewardedAd rewardedAd;
    private boolean isLoading = false;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;
    private static final String AD_UNIT_ID = "ca-app-pub-6979842665203661/3050341104";
    private static final String PREFS_NAME = "AppSettings";
    private static final String ADS_REMOVED = "ads_removed";

    private AdManager(Context context) {
        loadRewardedAd(context);
    }

    public static synchronized AdManager getInstance(Context context) {
        if (instance == null) {
            instance = new AdManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadRewardedAd(Context context) {
        if (isLoading) return;

        isLoading = true;
        RewardedAd.load(context, AD_UNIT_ID, new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isLoading = false;
                        rewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        isLoading = false;
                        rewardedAd = ad;
                        retryCount = 0; // Reset retry counter on success
                    }
                });
    }

    public static boolean areAdsRemoved(Context context) {
        return PremiumManager.areAdsRemoved(context);
    }

    public void showRewardedAd(Activity activity, AdCallback callback) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);

        // Check if ads are removed
        if (areAdsRemoved(activity)) {
            // If ads are removed, immediately give the reward
            callback.onRewardEarned(1);
            return;
        }

        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Activity act = activityRef.get();
                    if (act != null && !act.isFinishing()) {
                        callback.onAdDismissed(true);
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    Activity act = activityRef.get();
                    if (act != null && !act.isFinishing()) {
                        callback.onAdDismissed(false);
                    }
                }
            });

            Activity act = activityRef.get();
            if (act != null && !act.isFinishing()) {
                rewardedAd.show(act, rewardItem -> {
                    if (activityRef.get() != null && !activityRef.get().isFinishing()) {
                        callback.onRewardEarned(rewardItem.getAmount());
                    }
                });
            }
        } else {
            callback.onAdDismissed(false);
        }
    }
    public interface AdCallback {
        void onRewardEarned(int rewardAmount);
        void onAdDismissed(boolean success);

        void onAdFailedToLoad();
    }
}