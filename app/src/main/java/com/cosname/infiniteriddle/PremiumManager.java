package com.cosname.infiniteriddle;

import android.content.Context;
import android.content.SharedPreferences;

public class PremiumManager {
    private static final String PREFS_NAME = "AppSettings";
    private static final String ADS_REMOVED = "ads_removed";
    private static final String PREMIUM_PURCHASED = "premium_purchased";

    /**
     * Check if ads have been removed (either through premium purchase or manual setting)
     * @param context The application context
     * @return true if ads should be removed, false otherwise
     */
    public static boolean areAdsRemoved(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(ADS_REMOVED, false) || prefs.getBoolean(PREMIUM_PURCHASED, false);
    }

    /**
     * Set whether ads should be removed
     * @param context The application context
     * @param removed true to remove ads, false to show ads
     */
    public static void setAdsRemoved(Context context, boolean removed) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(ADS_REMOVED, removed).apply();
    }

    /**
     * Check if premium has been purchased
     * @param context The application context
     * @return true if premium is purchased, false otherwise
     */
    public static boolean isPremiumPurchased(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREMIUM_PURCHASED, false);
    }

    /**
     * Set premium purchase status
     * @param context The application context
     * @param purchased true if premium is purchased, false otherwise
     */
    public static void setPremiumPurchased(Context context, boolean purchased) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREMIUM_PURCHASED, purchased).apply();
    }

    /**
     * Update premium status from billing verification
     * @param context The application context
     * @param isPremium true if premium is verified, false otherwise
     */
    public static void updatePremiumStatusFromBilling(Context context, boolean isPremium) {
        setPremiumPurchased(context, isPremium);
        // Also update ads removal status based on premium status
        setAdsRemoved(context, isPremium);
    }
}
