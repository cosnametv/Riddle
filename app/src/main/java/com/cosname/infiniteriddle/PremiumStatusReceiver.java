package com.cosname.infiniteriddle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Broadcast receiver to handle premium status changes across the app
 */
public class PremiumStatusReceiver extends BroadcastReceiver {
    
    private static final String TAG = "PremiumStatusReceiver";
    public static final String ACTION_PREMIUM_STATUS_CHANGED = "com.cosname.infiniteriddle.PREMIUM_STATUS_CHANGED";
    
    private PremiumStatusListener listener;
    
    public interface PremiumStatusListener {
        void onPremiumStatusChanged(boolean isPremium);
    }
    
    public PremiumStatusReceiver(PremiumStatusListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_PREMIUM_STATUS_CHANGED.equals(intent.getAction())) {
            boolean isPremium = intent.getBooleanExtra("premium_activated", false);
            
            if (listener != null) {
                listener.onPremiumStatusChanged(isPremium);
            }
        }
    }
}
