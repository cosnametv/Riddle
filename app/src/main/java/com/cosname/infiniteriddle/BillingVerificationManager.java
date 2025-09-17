package com.cosname.infiniteriddle;

import android.content.Context;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.util.List;

public class BillingVerificationManager implements BillingClientStateListener, PurchasesResponseListener, PurchasesUpdatedListener {
    
    private static final String SKU_REMOVE_ADS = "remove_all_ads";
    
    private BillingClient billingClient;
    private Context context;
    private BillingVerificationCallback callback;
    
    public interface BillingVerificationCallback {
        void onBillingVerificationComplete(boolean isPremium);
        void onBillingVerificationError(String error);
    }
    
    public BillingVerificationManager(Context context, BillingVerificationCallback callback) {
        this.context = context;
        this.callback = callback;
    }
    
    public void startVerification() {
        billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        
        billingClient.startConnection(this);
    }
    
    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            queryExistingPurchases();
        } else {
            String error = "Billing setup failed: " + billingResult.getResponseCode();
            if (callback != null) {
                callback.onBillingVerificationError(error);
            }
        }
    }
    
    @Override
    public void onBillingServiceDisconnected() {
        if (callback != null) {
            callback.onBillingVerificationError("Billing service disconnected");
        }
    }
    
    private void queryExistingPurchases() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, this);
    }
    
    @Override
    public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            boolean hasValidPurchase = false;
            
            if (purchases != null) {
                for (Purchase purchase : purchases) {
                    if (purchase.getSkus().contains(SKU_REMOVE_ADS)) {
                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            hasValidPurchase = true;
                            PremiumManager.updatePremiumStatusFromBilling(context, true);
                            break;
                        }
                    }
                }
            }
            
            if (!hasValidPurchase) {
                PremiumManager.updatePremiumStatusFromBilling(context, false);
            }
            
            if (callback != null) {
                callback.onBillingVerificationComplete(hasValidPurchase);
            }
            
        } else {
            String error = "Failed to query purchases: " + billingResult.getResponseCode();
            if (callback != null) {
                callback.onBillingVerificationError(error);
            }
        }
        
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
    
    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
    }
    
}