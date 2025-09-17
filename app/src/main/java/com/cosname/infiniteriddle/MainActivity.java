package com.cosname.infiniteriddle;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity implements PremiumStatusReceiver.PremiumStatusListener, BillingVerificationManager.BillingVerificationCallback {

    private MaterialButton startGameButton;
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "AppSettings";
    private static final int APP_UPDATE_REQUEST_CODE = 1234;
    private AppUpdateManager appUpdateManager;
    private PremiumStatusReceiver premiumStatusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        MusicManager.initialize(this);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isNameSaved = prefs.getBoolean("isNameSaved", false);
        String username = prefs.getString("username", null);

        if (!isNameSaved || username == null || username.trim().isEmpty()) {
            // Show username dialog before proceeding
            showUsernameDialog(prefs);
            return;
        }

        checkForAppUpdate();
        
        if (isNetworkAvailable()) {
            checkBillingStatus();
        }

        boolean isFirstTime = prefs.getBoolean("isFirstTime", true);
        if (!isFirstTime) {
            startActivity(new Intent(MainActivity.this, RiddleActivity.class));
            finish();
            return;
        }

        prefs.edit().putBoolean("isFirstTime", false).apply();

        setupMainScreen();
        registerPremiumStatusReceiver();
    }

    private void registerPremiumStatusReceiver() {
        premiumStatusReceiver = new PremiumStatusReceiver(this);
        IntentFilter filter = new IntentFilter(PremiumStatusReceiver.ACTION_PREMIUM_STATUS_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(premiumStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(premiumStatusReceiver, filter);
        }
    }

    @Override
    public void onPremiumStatusChanged(boolean isPremium) {
        // Refresh the main screen to reflect premium status
        runOnUiThread(() -> {
            if (isPremium) {
                setupMainScreen();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (premiumStatusReceiver != null) {
            unregisterReceiver(premiumStatusReceiver);
        }
    }
    private void checkForAppUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            this,
                            APP_UPDATE_REQUEST_CODE);
                } catch (Exception e) {
                    // Optionally handle exception
                }
            }
        });
        // Listen for update state
        appUpdateManager.registerListener(state -> {
            if (state.installStatus() == com.google.android.play.core.install.model.InstallStatus.DOWNLOADED) {
                Snackbar.make(findViewById(android.R.id.content), "A new update is ready! Restart to update.", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Restart", v -> appUpdateManager.completeUpdate())
                        .show();
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        // If update was downloaded but not installed, prompt again
        if (appUpdateManager != null) {
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.installStatus() == com.google.android.play.core.install.model.InstallStatus.DOWNLOADED) {
                    Snackbar.make(findViewById(android.R.id.content), "A new update is ready! Restart to update.", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Restart", v -> appUpdateManager.completeUpdate())
                            .show();
                }
            });
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Update required to continue.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    private void setupMainScreen() {
        setContentView(R.layout.activity_main);

        // Initialize MusicManager
        MusicManager.initialize(this);

        startGameButton = findViewById(R.id.startGameButton);

        // Init SharedPreferences
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(MainActivity.this, RiddleActivity.class));
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    }
                }, 150);
            }
        });

        MaterialButton leaderboardButton = findViewById(R.id.leaderboardButton);
        leaderboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(MainActivity.this, LeaderboardActivity.class));
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    }
                }, 150);
            }
        });

        animateBackground();
        startGameButton.setRippleColor(getResources().getColorStateList(R.color.button_ripple));

        ImageView howToPlayImage = findViewById(R.id.howToPlayImage);
        MaterialButton startGame = findViewById(R.id.challengeButton);
        howToPlayImage.setOnClickListener(v -> showHowToPlayDialog());
        startGame.setOnClickListener(v -> startGameChallenge());
    }
    private void startGameChallenge() {
        Intent intent = new Intent(this, ChallengeActivity.class);
        startActivity(intent);
    }

    private void animateButtonPress(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start())
                .start();
    }
    private void animateBackground() {
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(0.1f, 0.3f);
        alphaAnimator.setDuration(3000);
        alphaAnimator.setRepeatMode(ValueAnimator.REVERSE);
        alphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
        alphaAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        alphaAnimator.start();
    }
    @SuppressLint("SetTextI18n")
    private void showHowToPlayDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.how_to_play_dialog, null);

        TextView message = dialogView.findViewById(R.id.howToPlayMessage);
        message.setText(
                "🎯 How to Play:\n" +
                "• Read the riddle and tap letters to build your answer\n" +
                "• Use HINT (-3pts), RESET, or UNDO if stuck\n" +
                "• Skip riddle (-20pts) if needed\n\n" +
                "⏰ Time Limits:\n" +
                "Easy: 70s | Medium: 85s | Hard: 110s | Tricky: 135s | Extreme: 160s\n\n" +
                "✅ Correct Answer = +10pts\n" +
                "Become the ultimate Riddle Master!"
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Set up close button
        ImageView closeButton = dialogView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
    private void showUsernameDialog(SharedPreferences prefs) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_username);
        dialog.setCancelable(false);

        EditText usernameEditText = dialog.findViewById(R.id.usernameEditText);
        usernameEditText.setHint(getString(R.string.dialog_enter_username_hint));
        dialog.findViewById(R.id.usernameDialogTitle);
        dialog.findViewById(R.id.usernameConfirmButton).setOnClickListener(v -> {
            String enteredName = usernameEditText.getText().toString().trim();
            if (enteredName.isEmpty()) {
                Toast.makeText(this, getString(R.string.dialog_enter_username_error), Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putString("username", enteredName)
                        .putString("oldUsername", enteredName)
                        .putBoolean("isNameSaved", true)
                        .putBoolean("isFirstTime", false)
                        .apply();
                dialog.dismiss();
                setupMainScreen();
            }
        });
        // Handle skip button
        dialog.findViewById(R.id.usernameSkipButton).setOnClickListener(v -> {
            long timestamp = System.currentTimeMillis();
            int randomNum = (int)(Math.random() * 1000000);

            String chars = "abcdefghijklmnopqrstuvwxyz";
            String randomSuffix = "";
            for (int i = 0; i < 2; i++) {
                randomSuffix += chars.charAt((int)(Math.random() * chars.length()));
            }

            String unique = "User" + (timestamp % 10000000) + randomNum + randomSuffix;
            prefs.edit().putString("username", unique)
                    .putString("oldUsername", unique) 
                    .putBoolean("isNameSaved", true)
                    .putBoolean("isFirstTime", false)
                    .apply();
            dialog.dismiss();
            setupMainScreen();
        });
        dialog.show();
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }
        return false;
    }
    
    private void checkBillingStatus() {
        BillingVerificationManager billingVerificationManager = new BillingVerificationManager(this, this);
        billingVerificationManager.startVerification();
    }
    
    @Override
    public void onBillingVerificationComplete(boolean isPremium) {
        runOnUiThread(() -> {
            if (isPremium) {
                PremiumManager.setAdsRemoved(this, true);
            }
        });
    }
    
    @Override
    public void onBillingVerificationError(String error) {
        runOnUiThread(() -> {
        });
    }
}