package com.cosname.infiniteriddle;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.List;

public class RemoveAdsBottomSheet extends BottomSheetDialogFragment implements PurchasesUpdatedListener, BillingClientStateListener {

    private static final String SKU_REMOVE_ADS = "remove_all_ads";
    private static final String PREFS_NAME = "AppSettings";
    private static final String ADS_REMOVED = "ads_removed";

    private BillingClient billingClient;
    private MaterialButton purchaseButton;
    private ProgressBar loadingIndicator;
    private TextView errorMessage;
    private TextView priceText;
    private boolean isPremium = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_remove_ads, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        purchaseButton = view.findViewById(R.id.purchaseButton);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        errorMessage = view.findViewById(R.id.errorMessage);
        priceText = view.findViewById(R.id.priceText);

        view.findViewById(R.id.closeButton).setOnClickListener(v -> dismiss());

        checkPremiumStatus();
        initializeBilling();

        purchaseButton.setOnClickListener(v -> {
            if (isPremium) {
                Toast.makeText(requireContext(), "You already have premium!", Toast.LENGTH_SHORT).show();
                dismiss();
            } else {
                initiatePurchase();
            }
        });
    }

    private void checkPremiumStatus() {
        isPremium = PremiumManager.areAdsRemoved(requireContext());
        
        if (isPremium) {
            purchaseButton.setText("Already Premium");
            purchaseButton.setEnabled(false);
            priceText.setText("Owned");
        }
    }

    private void initializeBilling() {
        billingClient = BillingClient.newBuilder(requireContext())
                .setListener(this)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(this);
    }

    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            querySkuDetails();
            queryPurchases();
        } else {
            String errorMessage = "Billing setup failed: " + billingResult.getResponseCode();
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                errorMessage = "Billing unavailable on this device";
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
                errorMessage = "Google Play services unavailable";
            }
            showError(errorMessage);
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        showError("Billing service disconnected. Please try again.");
    }

    private void querySkuDetails() {
        if (billingClient == null || !billingClient.isReady()) {
            priceText.setText("Loading...");
            return;
        }

        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder()
                .setSkusList(Arrays.asList(SKU_REMOVE_ADS))
                .setType(BillingClient.SkuType.INAPP);

        billingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null && !skuDetailsList.isEmpty()) {
                    for (SkuDetails skuDetails : skuDetailsList) {
                        if (SKU_REMOVE_ADS.equals(skuDetails.getSku())) {
                            priceText.setText(skuDetails.getPrice());
                            return;
                        }
                    }
                    priceText.setText("Price unavailable");
                } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && (skuDetailsList == null || skuDetailsList.isEmpty())) {
                    priceText.setText("SKU not configured");
                } else {
                    String errorMessage = "Error: " + billingResult.getResponseCode();
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                        errorMessage = "Billing unavailable";
                    } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
                        errorMessage = "Service unavailable";
                    }
                    priceText.setText(errorMessage);
                }
            }
        });
    }

    private void queryPurchases() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (Purchase purchase : purchases) {
                        if (purchase.getSkus().contains(SKU_REMOVE_ADS)) {
                            handlePurchase(purchase);
                            break;
                        }
                    }
                }
            }
        });
    }

    private void initiatePurchase() {
        if (billingClient == null || !billingClient.isReady()) {
            showError("Billing not ready. Please try again.");
            return;
        }

        showLoading();

        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder()
                .setSkusList(Arrays.asList(SKU_REMOVE_ADS))
                .setType(BillingClient.SkuType.INAPP);

        billingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null && !skuDetailsList.isEmpty()) {
                    for (SkuDetails skuDetails : skuDetailsList) {
                        if (SKU_REMOVE_ADS.equals(skuDetails.getSku())) {
                            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                    .setSkuDetails(skuDetails)
                                    .build();
                            billingClient.launchBillingFlow(requireActivity(), flowParams);
                            return;
                        }
                    }
                    hideLoading();
                    showError("Product not found in Play Store. Please contact support.");
                } else {
                    hideLoading();
                    showError("Unable to load product details. Please try again.");
                }
            }
        });
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getSkus().contains(SKU_REMOVE_ADS)) {
                    handlePurchase(purchase);
                }
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            hideLoading();
        } else {
            hideLoading();
            showError("Purchase failed. Please try again.");
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            setAdsRemoved(true);
                            showSuccess();
                        } else {
                            hideLoading();
                            showError("Failed to acknowledge purchase. Please contact support.");
                        }
                    }
                });
            } else {
                setAdsRemoved(true);
                showSuccess();
            }
        } else {
            hideLoading();
            showError("Purchase not completed. Please try again.");
        }
    }

    private void setAdsRemoved(boolean removed) {
        PremiumManager.setAdsRemoved(requireContext(), removed);
        isPremium = removed;
    }

    private void showSuccess() {
        hideLoading();
        purchaseButton.setText("Premium Activated!");
        purchaseButton.setEnabled(false);
        priceText.setText("Owned");
        Toast.makeText(requireContext(), "Premium activated! Ads removed forever!", Toast.LENGTH_LONG).show();
        
        if (getParentFragment() instanceof RemoveAdsCallback) {
            ((RemoveAdsCallback) getParentFragment()).onAdsRemoved();
        }
        
        broadcastPremiumStatusChange();
        navigateToRefreshedActivity();
        dismiss();
    }
    
    private void broadcastPremiumStatusChange() {
        android.content.Intent intent = new android.content.Intent("com.cosname.infiniteriddle.PREMIUM_STATUS_CHANGED");
        intent.putExtra("premium_activated", true);
        requireContext().sendBroadcast(intent);
    }
    
    private void navigateToRefreshedActivity() {
        Activity currentActivity = requireActivity();
        
        android.content.Intent refreshIntent = new android.content.Intent(currentActivity, currentActivity.getClass());
        refreshIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                              android.content.Intent.FLAG_ACTIVITY_NEW_TASK | 
                              android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        refreshIntent.putExtra("premium_refresh", true);
        
        requireContext().startActivity(refreshIntent);
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.GONE);
        purchaseButton.setEnabled(true);
    }

    private void showLoading() {
        loadingIndicator.setVisibility(View.VISIBLE);
        errorMessage.setVisibility(View.GONE);
        purchaseButton.setEnabled(false);
        purchaseButton.setText("Loading...");
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
        purchaseButton.setEnabled(true);
        purchaseButton.setText("Purchase Now");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }

    public interface RemoveAdsCallback {
        void onAdsRemoved();
    }
}