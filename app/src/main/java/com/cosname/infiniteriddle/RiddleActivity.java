package com.cosname.infiniteriddle;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;


public class RiddleActivity extends AppCompatActivity implements PremiumStatusReceiver.PremiumStatusListener {

    private AdView mAdView;
    private MaterialButton backButton, hintButton, resetButton, undoButton, nextRiddleButton;
    private TextView riddleText, riddleNumberText, difficultyText, scoreText;
    private LinearLayout feedbackContainer;
    private ImageView feedbackIcon;
    private TextView feedbackText;

    private RiddleManager riddleManager;
    private Riddle currentRiddle;

    private TextView timerText, levelText;
    private CountDownTimer countDownTimer;
    private static final long RIDDLE_DURATION_MS = 60_000;
    private long remainingTimeMillis = RIDDLE_DURATION_MS;

    private int score = 0;
    private Animation bounce;
    private Handler handler = new Handler();
    private boolean hintUsedForCurrentRiddle = false;
    int[] levelThresholds = {299, 749, 1499, 1999, 2500, 3000, 5000, 7500, 10000, 15000, 20000, 25000, 30000, 35000, 40000, 45000, 50000, 55000, 60000, 65000, 70000, 75000, 80000, 85000, 90000, 95000, 100000};
    private int highestAchievedLevel = 1;
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "AppSettings";
    private GridLayout answerContainer;
    private GridLayout lettersContainer;
    private ImageButton settingsButton;
    private static final String MUSIC_ON = "music_on";
    private static final String SOUND_ON = "sound_on";
    private Handler firebaseSyncHandler = new Handler();
    private Runnable firebaseSyncRunnable;
    private static final long SYNC_INTERVAL_MS = 300_000; // 5 minutes instead of 1 minute
    private FirebaseLeaderboardHelper firebaseHelper;
    private boolean lastSyncOnline = true;
    private static final String TAG = "RateUs";
    private final Handler timeoutHandler = new Handler();
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 101;
    private RewardedAd rewardedAd;
    private View loadingOverlay;
    private View accuracyBar;
    private LinearLayout letterFeedbackContainer;
    private ReviewManager reviewManager;
    private static final long DELAY_MILLIS = 240000;
    private PremiumStatusReceiver premiumStatusReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riddle);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        firebaseHelper = new FirebaseLeaderboardHelper();

        // Restore score, riddle index, and highest achieved level
        score = getSharedPreferences("RiddlePrefs", MODE_PRIVATE).getInt("score", 0);
        int savedRiddleProgressionIndex = getSharedPreferences("RiddlePrefs", MODE_PRIVATE).getInt("riddleIndex", 0);
        highestAchievedLevel = getSharedPreferences("RiddlePrefs", MODE_PRIVATE).getInt("highestAchievedLevel", 1);


        MobileAds.initialize(this, initializationStatus -> {});
        mAdView = findViewById(R.id.adView);

        // Only load banner ads if ads are not removed
        if (!AdManager.areAdsRemoved(this)) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        } else {
            // Hide the banner ad if ads are removed
            mAdView.setVisibility(View.GONE);
        }

        loadingOverlay = findViewById(R.id.loadingOverlay);
        reviewManager = ReviewManagerFactory.create(this);


        requestNotificationPermission();
        scheduleDailyNotification();
        updateNextButtonIcon();
        updateHintButtonIcon();

        // Bind views
        backButton = findViewById(R.id.backButton);
        nextRiddleButton = findViewById(R.id.nextRiddleButton);
        hintButton = findViewById(R.id.hintButton);
        resetButton = findViewById(R.id.resetButton);
        undoButton = findViewById(R.id.undoButton);

        riddleText = findViewById(R.id.riddleText);
        riddleNumberText = findViewById(R.id.riddleNumberText);
        difficultyText = findViewById(R.id.difficultyText);
        scoreText = findViewById(R.id.scoreText);
        feedbackContainer = findViewById(R.id.feedbackContainer);
        feedbackIcon = findViewById(R.id.feedbackIcon);
        feedbackText = findViewById(R.id.feedbackText);
        timerText = findViewById(R.id.timerText);
        levelText = findViewById(R.id.levelText);

        letterFeedbackContainer = findViewById(R.id.letterFeedbackContainer);
        accuracyBar = findViewById(R.id.accuracyBar);

        lettersContainer = findViewById(R.id.lettersContainer);
        answerContainer = findViewById(R.id.answerContainer);
        settingsButton = findViewById(R.id.settingsButton);

        riddleManager = new RiddleManager(this);
        riddleManager.setCurrentLevel(savedRiddleProgressionIndex);

        bounce = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);

        settingsButton.setOnClickListener(v -> showSettingsDialog());

        backButton.setOnClickListener(v -> {
            // Mark MainActivity to behave like first time again
            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isFirstTime", true)
                    .apply();

            // Start MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        hintButton.setOnClickListener(v -> showHint());

        resetButton.setOnClickListener(v -> {
            for (int i = 0; i < answerContainer.getChildCount(); i++) {
                View view = answerContainer.getChildAt(i);
                if (view instanceof TextView) {
                    TextView slot = (TextView) view;
                    Object tag = slot.getTag();
                    if (tag instanceof Button) {
                        ((Button) tag).setEnabled(true);
                        slot.setTag(null);
                    }
                    slot.setText("_");
                }
            }
        });

        undoButton.setOnClickListener(v -> {
            for (int i = answerContainer.getChildCount() - 1; i >= 0; i--) {
                View view = answerContainer.getChildAt(i);
                if (view instanceof TextView) {
                    TextView slot = (TextView) view;
                    if (!slot.getText().equals("_")) {
                        Object tag = slot.getTag();
                        if (tag instanceof Button) {
                            ((Button) tag).setEnabled(true);
                            slot.setTag(null);
                        }
                        slot.setText("_");
                        break; // only undo one
                    }
                }
            }
        });

        nextRiddleButton.setOnClickListener(v -> {
            if (score >= 20 || AdManager.areAdsRemoved(this)) {
                if (countDownTimer != null) countDownTimer.cancel();
                saveProgress();
                if (!AdManager.areAdsRemoved(this)) {
                    score -= 20;
                    updateHeaderUI();
                    showShakePoints("-20");
                } else {
                    // Premium users get free next riddle
                    Toast.makeText(this, "Premium: Free next riddle!", Toast.LENGTH_SHORT).show();
                }
                loadNextRiddle();
            } else {
                // Check if ads are removed
                if (AdManager.areAdsRemoved(this)) {
                    // If ads are removed, give points immediately
                    score += 20;
                    updateHeaderUI();
                    showShakePoints("+20");
                    Toast.makeText(this, "Premium bonus: +20 points!", Toast.LENGTH_SHORT).show();
                    return;
                }

                initializeRewardedAd();
                AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                        .setTitle("Not Enough Points")
                        .setMessage("You need 20 points to move to the Next Riddle. Watch an ad to earn 20 points?")
                        .setPositiveButton("Watch Ad", (d, which) -> {
                            showLoadingOverlay();
                            showRewardedAd();
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                dialog.setCancelable(false);

                dialog.show();
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (positiveButton != null) {
                    positiveButton.setTextColor(Color.WHITE);
                }
                if (negativeButton != null) {
                    negativeButton.setTextColor(Color.WHITE);
                }
            }
            updateNextButtonIcon();
        });


        ImageView copyRiddle = findViewById(R.id.copyRiddle);
        ImageView shareRiddle = findViewById(R.id.shareRiddle);

        copyRiddle.setOnClickListener(v -> {
            String riddleTextString = riddleText.getText().toString().trim();
            String answerTextString = currentRiddle != null ? currentRiddle.getAnswer().trim() : "";
            String appName = getString(R.string.app_name);
            String packageName = getPackageName();
            String appUrl = "https://play.google.com/store/apps/details?id=" + packageName;

            String shareText = riddleTextString + "\n\n"
                    + "Answer: " + answerTextString + "\n\n"
                    + "Enjoyed this riddle? Download " + appName + " here:\n"
                    + appUrl;

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Riddle Text", shareText);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        shareRiddle.setOnClickListener(v -> {
            String riddleTextString = riddleText.getText().toString().trim();
            String answerTextString = currentRiddle != null ? currentRiddle.getAnswer().trim() : "";
            String appName = getString(R.string.app_name);
            String packageName = getPackageName();
            String appUrl = "https://play.google.com/store/apps/details?id=" + packageName;

            String shareText = riddleTextString + "\n\n"
                    + "Enjoyed this riddle? Download " + appName + " here:\n"
                    + appUrl;

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        loadNextRiddle();
        startPeriodicFirebaseSync();
        initializeRewardedAd();
        handler.postDelayed(this::requestReview, DELAY_MILLIS);
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
        // Refresh the riddle activity to remove ads
        runOnUiThread(() -> {
            if (isPremium) {
                // Hide banner ads
                if (mAdView != null) {
                    mAdView.setVisibility(View.GONE);
                }

                // Update button icons to show premium status
                updateNextButtonIcon();
                updateHintButtonIcon();

                Toast.makeText(this, "Premium activated! Ads removed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestReview() {
        Task<ReviewInfo> request = reviewManager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = reviewManager.launchReviewFlow(this, reviewInfo);
                flow.addOnCompleteListener(completeTask -> {
                });
            } else {
                task.getException().printStackTrace();
            }
        });
    }
    private void updateNextButtonIcon() {
        MaterialButton nextButton = findViewById(R.id.nextRiddleButton);
        if (score >= 20 || AdManager.areAdsRemoved(this)) {
            nextButton.setIconResource(R.drawable.ic_next);
            nextButton.setText("");
        } else {
            nextButton.setIconResource(R.drawable.ic_ad);
            nextButton.setText("Ad");
        }
    }
    private void updateHintButtonIcon() {
        MaterialButton hintButton = findViewById(R.id.hintButton);
        if (score >= 3 || hintUsedForCurrentRiddle || AdManager.areAdsRemoved(this)) {
            hintButton.setIconResource(R.drawable.ic_hint);
            hintButton.setText("");
        } else {
            hintButton.setIconResource(R.drawable.ic_ad);
            hintButton.setText("Ad");
        }
    }
    private void initializeRewardedAd() {
        RewardedAd.load(this, "ca-app-pub-6979842665203661/3050341104", new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                        hideLoadingOverlay();
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        hideLoadingOverlay();
                    }
                });
    }
    private void showRewardedAd() {
        // Check if ads are removed
        if (AdManager.areAdsRemoved(this)) {
            // If ads are removed, immediately give the reward
            int rewardAmount = 1;
            score += rewardAmount;
            updateHeaderUI();
            showShakePoints("+" + rewardAmount);
            loadNextRiddle();
            updateNextButtonIcon();
            updateHintButtonIcon();
            return;
        }

        showLoadingOverlay();
        if (rewardedAd != null && !isFinishing() && !isDestroyed()) {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    hideLoadingOverlay();
                    initializeRewardedAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    hideLoadingOverlay();
                    Toast.makeText(RiddleActivity.this, "Ad failed to show", Toast.LENGTH_SHORT).show();
                    initializeRewardedAd();
                }
            });

            rewardedAd.show(RiddleActivity.this, rewardItem -> {
                int rewardAmount = rewardItem.getAmount();
                score += rewardAmount;
                updateHeaderUI();
                showShakePoints("+" + rewardAmount);
                loadNextRiddle();
                updateNextButtonIcon();
                updateHintButtonIcon();
            });
        } else {
            hideLoadingOverlay();
            if (isFinishing() || isDestroyed()) {
                return;
            }
            Toast.makeText(this, "Ad not ready yet. Please try again.", Toast.LENGTH_SHORT).show();
            initializeRewardedAd();
        }
    }
    private void showLoadingOverlay() {
        runOnUiThread(() -> {
            loadingOverlay.setVisibility(View.VISIBLE);
            nextRiddleButton.setEnabled(false);
        });
    }
    private void hideLoadingOverlay() {
        runOnUiThread(() -> {
            loadingOverlay.setVisibility(View.GONE);
            nextRiddleButton.setEnabled(true);
        });
    }

    private void showHint() {
        if (currentRiddle == null) return;

        if (score >= 3 || hintUsedForCurrentRiddle) {
            Toast.makeText(this, currentRiddle.getHint(), Toast.LENGTH_LONG).show();

            if (!hintUsedForCurrentRiddle) {
                score -= 3;
                updateHeaderUI();
                showShakePoints("-3");
                showFeedback(false, "-3 points for hint");
                hintUsedForCurrentRiddle = true;
            }

        } else {
            // Check if ads are removed
            if (AdManager.areAdsRemoved(this)) {
                // If ads are removed, give points immediately
                score += 20;
                updateHeaderUI();
                showShakePoints("+20");
                Toast.makeText(this, "Premium bonus: +20 points!", Toast.LENGTH_SHORT).show();
                return;
            }

            initializeRewardedAd();
            AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                    .setTitle("Not Enough Points")
                    .setMessage("You need 3 points to show Riddle Hint. Watch an ad to earn 20 points?")
                    .setPositiveButton("Watch Ad", (d, which) -> {
                        showLoadingOverlay();
                        showRewardedAd();
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.setCancelable(false);

            dialog.show();
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(Color.WHITE);
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(Color.WHITE);
            }
        }
        updateHintButtonIcon();
    }

    private void startPeriodicFirebaseSync() {
        firebaseSyncRunnable = new Runnable() {
            @Override
            public void run() {
                syncScoreToFirebase();
                firebaseSyncHandler.postDelayed(this, SYNC_INTERVAL_MS);
            }
        };
        firebaseSyncHandler.post(firebaseSyncRunnable);
    }
    private void stopPeriodicFirebaseSync() {
        firebaseSyncHandler.removeCallbacks(firebaseSyncRunnable);
    }
    private void syncScoreToFirebase() {
        SharedPreferences appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences riddlePrefs = getSharedPreferences("RiddlePrefs", MODE_PRIVATE);
        String username = appPrefs.getString("username", "");
        int userScore = riddlePrefs.getInt("score", 0);
        if (isOnline()) {
            // If there is unsynced data, sync it first
            if (appPrefs.getBoolean("hasUnsyncedScore", false)) {
                int unsyncedScore = appPrefs.getInt("unsyncedScore", userScore);
                firebaseHelper.uploadUserScore(username, unsyncedScore, new FirebaseLeaderboardHelper.UploadCallback() {
                    @Override
                    public void onSuccess() {
                        appPrefs.edit().remove("hasUnsyncedScore").remove("unsyncedScore").apply();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        // Keep unsynced
                    }
                });
            }
            // Sync current score
            firebaseHelper.uploadUserScore(username, userScore, new FirebaseLeaderboardHelper.UploadCallback() {
                @Override
                public void onSuccess() {
                    // Success, nothing to do
                }
                @Override
                public void onFailure(Exception e) {
                    // Save for later
                    appPrefs.edit().putBoolean("hasUnsyncedScore", true).putInt("unsyncedScore", userScore).apply();
                }
            });
            lastSyncOnline = true;
        } else {
            // Save for later
            appPrefs.edit().putBoolean("hasUnsyncedScore", true).putInt("unsyncedScore", userScore).apply();
            lastSyncOnline = false;
        }
    }
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }
        return false;
    }
    private void startTimer(long durationMillis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        remainingTimeMillis = durationMillis;

        countDownTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeMillis = millisUntilFinished;
                updateTimerDisplay(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                timerText.setText("00.00");

                // Disable letter picking
                for (int i = 0; i < lettersContainer.getChildCount(); i++) {
                    lettersContainer.getChildAt(i).setEnabled(false);
                }
                for (int i = 0; i < answerContainer.getChildCount(); i++) {
                    answerContainer.getChildAt(i).setEnabled(false);
                }

                hintButton.setEnabled(false);
                showFeedback(false, "Time’s up!");

                // Show correct answer
                TextView riddleAns = findViewById(R.id.riddleAns);
                if (currentRiddle != null) {
                    SpannableStringBuilder styledText = new SpannableStringBuilder();
                    String prefix = "Correct Answer: ";
                    String answer = currentRiddle.getAnswer();
                    styledText.append(prefix);
                    int start = styledText.length();
                    styledText.append(answer);
                    int end = styledText.length();
                    styledText.setSpan(
                            new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                            start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    riddleAns.setText(styledText);
                    riddleAns.setVisibility(View.VISIBLE);
                }

                handler.postDelayed(() -> {
                    feedbackContainer.setVisibility(View.GONE);
                    riddleAns.setVisibility(View.GONE);
                    loadNextRiddle();
                }, 5000);
            }
        }.start();
    }
    private void updateTimerDisplay(long millis) {
        int minutes = (int) (millis / 1000) / 60;
        int seconds = (int) (millis / 1000) % 60;
        // Format as MM.SS
        String timeString = String.format("%02d.%02d", minutes, seconds);
        timerText.setText(timeString);
    }

    @SuppressLint("SetTextI18n")
    private void loadNextRiddle() {
        updateNextButtonIcon();
        updateHintButtonIcon();

        currentRiddle = riddleManager.getNextRiddle();
        if (currentRiddle == null) {
            riddleText.setText("You've completed all riddles!");
            riddleNumberText.setText("");
            difficultyText.setText("");
            hintButton.setEnabled(false);
            accuracyBar.setVisibility(View.GONE);
            return;
        }

        // 3. Set up riddle display
        riddleText.setText(currentRiddle.getQuestion());
        riddleText.startAnimation(bounce);
        riddleNumberText.setText("Random Riddle");
        difficultyText.setText(currentRiddle.getDifficultyStars());
        feedbackContainer.setVisibility(View.GONE);
        hintButton.setEnabled(true);
        hintUsedForCurrentRiddle = false;

        // 4. Clear previous game elements
        lettersContainer.removeAllViews();
        answerContainer.removeAllViews();

        // 5. Initialize accuracy system
        accuracyBar.setVisibility(View.VISIBLE);
        accuracyBar.setBackgroundColor(ContextCompat.getColor(this, R.color.gray));
        ViewGroup.LayoutParams params = accuracyBar.getLayoutParams();
        params.width = 0; // Start with zero width
        accuracyBar.setLayoutParams(params);

        // 6. Create answer slots
        String cleanAnswer = currentRiddle.getAnswer().replaceAll("\\s+", "");
        int answerLength = cleanAnswer.length();
        boolean[] correctPositions = new boolean[answerLength];
        Arrays.fill(correctPositions, false);

        for (int i = 0; i < answerLength; i++) {
            TextView slot = new TextView(this);
            slot.setText("_");
            slot.setTextSize(24);
            slot.setGravity(Gravity.CENTER);
            slot.setBackgroundResource(R.drawable.answer_slot_background);
            slot.setPadding(24, 10, 24, 10);
            slot.setTextColor(Color.WHITE);

            LinearLayout.LayoutParams slotParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            slotParams.setMargins(5, 5, 5, 5);
            slot.setLayoutParams(slotParams);
            answerContainer.addView(slot);
        }

        // 7. Create letter buttons
        List<Character> scrambled = scramble(currentRiddle.getAnswer());
        for (char c : scrambled) {
            MaterialButton letterButton = createLetterButton(c);

            letterButton.setOnClickListener(v -> handleLetterClick(letterButton, correctPositions));

            lettersContainer.addView(letterButton);
        }

        updateHeaderUI();
        startTimer(getTimerDurationForDifficulty(currentRiddle.getDifficulty()));
    }
    private MaterialButton createLetterButton(char letter) {
        MaterialButton button = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);

        button.setText(String.valueOf(letter));
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.accent_color)));
        button.setTextColor(ContextCompat.getColor(this, R.color.white));
        button.setStrokeWidth(1);
        button.setStrokeColor(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.white)));
        button.setCornerRadius(8);
        button.setPadding(2, 0, 2, 0);

        int buttonSizeInDp = 40;
        int pixelSize = (int) (buttonSizeInDp * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(pixelSize, pixelSize);
        params.setMargins(2, 0, 3, 0);
        button.setLayoutParams(params);

        return button;
    }
    private void handleLetterClick(MaterialButton letterButton, boolean[] correctPositions) {
        for (int i = 0; i < answerContainer.getChildCount(); i++) {
            View view = answerContainer.getChildAt(i);
            if (view instanceof TextView) {
                TextView slot = (TextView) view;
                if (slot.getText().equals("_")) {
                    String buttonLetter = letterButton.getText().toString();
                    slot.setText(buttonLetter);
                    slot.setTag(letterButton);
                    letterButton.setEnabled(false);

                    // Check accuracy
                    String correctAnswer = currentRiddle.getAnswer().replaceAll("\\s+", "");
                    boolean isCorrect = i < correctAnswer.length() &&
                            buttonLetter.equalsIgnoreCase(String.valueOf(correctAnswer.charAt(i)));

                    if (isCorrect) {
                        correctPositions[i] = true;
                    }

                    updateAccuracyDisplay(correctPositions);
                    break;
                }
            }
        }
        checkIfAnswerComplete();
    }
    private void updateAccuracyDisplay(boolean[] correctPositions) {
        int totalSlots = correctPositions.length;
        int correctCount = 0;

        for (boolean isCorrect : correctPositions) {
            if (isCorrect) correctCount++;
        }

        float accuracy = (float) correctCount / totalSlots;
        View parent = (View) accuracyBar.getParent();
        int maxWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
        int newWidth = (int) (maxWidth * accuracy);

        // Animate width change
        ValueAnimator widthAnimator = ValueAnimator.ofInt(accuracyBar.getWidth(), newWidth);
        widthAnimator.setDuration(300);
        widthAnimator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = accuracyBar.getLayoutParams();
            params.width = (int) animation.getAnimatedValue();
            accuracyBar.setLayoutParams(params);
        });

        // Set color based on accuracy
        int colorRes;
        if (accuracy >= 0.75f) {
            colorRes = R.color.correct_green;
        } else if (accuracy >= 0.5f) {
            colorRes = R.color.partial_yellow;
        } else if (accuracy >= 0.25f) {
            colorRes = R.color.partial_yellow;
        } else {
            colorRes = R.color.incorrect_red;
        }

        // Animate color change
        int finalColor = ContextCompat.getColor(this, colorRes);
        ValueAnimator colorAnimator = ValueAnimator.ofArgb(
                ((ColorDrawable) accuracyBar.getBackground()).getColor(),
                finalColor
        );
        colorAnimator.setDuration(300);
        colorAnimator.addUpdateListener(animation -> {
            accuracyBar.setBackgroundColor((int) animation.getAnimatedValue());
        });

        widthAnimator.start();
        colorAnimator.start();
    }
    private void checkIfAnswerComplete() {
        for (int i = 0; i < answerContainer.getChildCount(); i++) {
            View view = answerContainer.getChildAt(i);
            if (view instanceof TextView && ((TextView) view).getText().equals("_")) {
                return;
            }
        }
        checkAnswer();
    }
    private List<Character> scramble(String word) {
        List<Character> chars = new ArrayList<>();

        String cleanWord = word.replaceAll("\\s+", "");
        for (char c : cleanWord.toCharArray()) {
            chars.add(c);
        }

        // Add extra random letters
        int extraLetters = 1; // increase for more challenge
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();

        for (int i = 0; i < extraLetters; i++) {
            char extra;
            do {
                extra = alphabet.charAt(random.nextInt(alphabet.length()));
            } while (cleanWord.indexOf(extra) >= 0);

            chars.add(extra);
        }

        do {
            Collections.shuffle(chars);
        } while (chars.equals(Arrays.asList(cleanWord.toCharArray())) && chars.size() > 1);

        return chars;
    }
    private void checkAnswer() {
        StringBuilder userAnswerBuilder = new StringBuilder();
        for (int i = 0; i < answerContainer.getChildCount(); i++) {
            View view = answerContainer.getChildAt(i);
            if (view instanceof TextView) {
                String letter = ((TextView) view).getText().toString();
                if (!letter.equals("_")) {
                    userAnswerBuilder.append(letter);
                }
            }
        }

        String userAnswer = userAnswerBuilder.toString().toLowerCase().trim();
        String actualAnswer = currentRiddle.getAnswer().toLowerCase().trim();

        // Normalize (remove special chars, extra spaces)
        userAnswer = userAnswer.replaceAll("[^a-z0-9 ]", " ");
        actualAnswer = actualAnswer.replaceAll("[^a-z0-9 ]", " ");
        userAnswer = userAnswer.replaceAll("\\s+", " ").trim();
        actualAnswer = actualAnswer.replaceAll("\\s+", " ").trim();

        boolean isCorrect = false;
        String regex = "\\b" + Pattern.quote(actualAnswer) + "\\b";

        if (userAnswer.equals(actualAnswer)) {
            isCorrect = true;
        } else if (userAnswer.matches(".*" + regex + ".*")) {
            isCorrect = true;
        }

        if (isCorrect) {
            if (countDownTimer != null) countDownTimer.cancel();
            showShakePoints("+10");

            score += 10; // Update score
            checkLevelUp(); // Check for level up after score update

            SoundManager.playCorrect(this);
            showFeedback(true, "+10 points");

            updateHeaderUI();

            handler.postDelayed(() -> {
                feedbackContainer.setVisibility(View.GONE);
                saveProgress();
                loadNextRiddle();
            }, 1000);
        } else {
            SoundManager.playWrong(this);
            showFeedback(false, "Incorrect, try again!");
            handler.postDelayed(() -> {
                feedbackContainer.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> feedbackContainer.setVisibility(View.GONE))
                        .start();
            }, 2000);
        }
    }
    private void showFeedback(boolean isPositive, String message) {
        feedbackText.setText(message);

        if (isPositive) {
            feedbackIcon.setImageResource(R.drawable.ic_correct);
            feedbackContainer.setBackgroundResource(R.drawable.feedback_bg_positive);
        } else {
            feedbackIcon.setImageResource(R.drawable.ic_incorrect);
            feedbackContainer.setBackgroundResource(R.drawable.feedback_bg_negative);
        }

        feedbackContainer.setVisibility(View.VISIBLE);

        feedbackContainer.setAlpha(0f);
        feedbackContainer.animate().alpha(1f).setDuration(200).start();

        handler.postDelayed(() -> {
            feedbackContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> feedbackContainer.setVisibility(View.GONE))
                    .start();
        }, 1300);
    }
    private long getTimerDurationForDifficulty(int difficulty) {
        switch (difficulty) {
            case 1: return 70_000;
            case 2: return 85_000;
            case 3: return 110_000;
            case 4: return 135_000;
            case 5: return 160_000;
            default: return 80_000;
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateHeaderUI() {
        scoreText.setText(String.valueOf(score));
        // Display the highest level the user has achieved
        levelText.setText(String.valueOf(highestAchievedLevel));
    }
    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pause();
        if (isFinishing()) {
            MusicManager.pause();
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        saveProgress();
        saveHighestLevelProgress();
        stopPeriodicFirebaseSync();
    }
    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        // Clean up handlers
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (firebaseSyncHandler != null) {
            firebaseSyncHandler.removeCallbacksAndMessages(null);
        }
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
        stopPeriodicFirebaseSync();
        SoundManager.release();
        MusicManager.release();
        super.onDestroy();
        if (premiumStatusReceiver != null) {
            unregisterReceiver(premiumStatusReceiver);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (preferences.getBoolean("music_on", true)) {
            MusicManager.start(this);
        }
        if (currentRiddle != null && remainingTimeMillis > 0) {
            startTimer(remainingTimeMillis); // Resume with time left
        }
        // If coming online, sync immediately
        if (isOnline()) {
            syncScoreToFirebase();
        }
    }
    @Override
    public void onBackPressed() {
        saveProgress();
        saveHighestLevelProgress();

        setResult(RESULT_OK);
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
    private void saveProgress() {
        getSharedPreferences("RiddlePrefs", MODE_PRIVATE)
                .edit()
                .putInt("riddleIndex", riddleManager.getCurrentLevel())
                .putInt("score", score)
                .apply();
    }
    private void saveHighestLevelProgress() {
        getSharedPreferences("RiddlePrefs", MODE_PRIVATE)
                .edit()
                .putInt("highestAchievedLevel", highestAchievedLevel)
                .apply();
    }
    private void showShakePoints(String text) {
        TextView transaction = findViewById(R.id.transaction);
        transaction.setText(text);
        transaction.setVisibility(View.VISIBLE);

        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        transaction.startAnimation(shake);

        // Hide after animation
        new Handler().postDelayed(() -> transaction.setVisibility(View.GONE), 1000);
    }
    private int getCalculatedLevelFromScore() {
        int level = 1;
        for (int i = 0; i < levelThresholds.length; i++) {
            if (score <= levelThresholds[i]) {
                return level;
            }
            level++;
        }
        return level; // Return the last level if score exceeds all thresholds
    }
    private void checkLevelUp() {
        int calculatedCurrentLevel = getCalculatedLevelFromScore();

        if (calculatedCurrentLevel > highestAchievedLevel) {
            highestAchievedLevel = calculatedCurrentLevel;
            showLevelUpDialog(highestAchievedLevel);
            saveHighestLevelProgress();
        } else {
        }
    }
    private void showLevelUpDialog(int newLevel) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_level_up, null);

        TextView levelUpText = dialogView.findViewById(R.id.levelUpMessage);
        levelUpText.setText("🏆 Achievement Unlocked! \nYou've Reached Level " + newLevel + "!");

        // Animate the text as before
        levelUpText.setScaleX(0f);
        levelUpText.setScaleY(0f);
        levelUpText.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator())
                .start();

        MaterialButton levelUpOkButton = dialogView.findViewById(R.id.levelUpOkButton);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        levelUpOkButton.setOnClickListener(v -> dialog.dismiss());

        // Play the sound
        SoundManager.playLevelUp(this);
        dialog.show();
    }

    private void showSettingsDialog() {
        pauseTimer();
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                )
                .replace(R.id.settings_container, new SettingsFragment(), "SettingsFragment")
                .addToBackStack("SettingsFragment")
                .commit();

        View container = findViewById(R.id.settings_container);
        if (container != null) container.setVisibility(View.VISIBLE);

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean hasSettings = getSupportFragmentManager().findFragmentByTag("SettingsFragment") != null;
            View c = findViewById(R.id.settings_container);
            if (c != null) c.setVisibility(hasSettings ? View.VISIBLE : View.GONE);
            if (!hasSettings) {
                resumeTimerOnDismiss();
            }
        });
    }
    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }
    private void resumeTimerOnDismiss() {
        if (currentRiddle != null && remainingTimeMillis > 0) {
            startTimer(remainingTimeMillis);
        }
    }

    public void updateUsernameInFirebase(String newUsername) {

        SharedPreferences riddlePrefs = getSharedPreferences("RiddlePrefs", MODE_PRIVATE);
        SharedPreferences appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int currentScore = riddlePrefs.getInt("score", 0);
        String oldUsername = appPrefs.getString("oldUsername", "");

        firebaseHelper.updateUsername(oldUsername, newUsername, currentScore, new FirebaseLeaderboardHelper.UpdateCallback() {
            @Override
            public void onSuccess() {
                appPrefs.edit().putString("oldUsername", newUsername).apply();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(RiddleActivity.this, "Failed to update username online", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scheduleDailyNotification();
            } else {
                // Permission denied
                Toast.makeText(this, "Notifications are disabled. You can enable them in settings.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS
                );
            }
        }
    }
    private void scheduleDailyNotification() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // --- Morning ---
        Calendar morning = Calendar.getInstance();
        morning.set(Calendar.HOUR_OF_DAY, 6);
        morning.set(Calendar.MINUTE, 0);
        morning.set(Calendar.SECOND, 0);
        if (morning.before(Calendar.getInstance())) {
            morning.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent morningIntent = new Intent(this, ReminderReceiver.class);
        morningIntent.putExtra("notification_type", "morning");
        PendingIntent morningPending = PendingIntent.getBroadcast(
                this,
                1001,
                morningIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                morning.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                morningPending
        );

        // --- Evening ---
        Calendar evening = Calendar.getInstance();
        evening.set(Calendar.HOUR_OF_DAY, 20);
        evening.set(Calendar.MINUTE, 0);
        evening.set(Calendar.SECOND, 0);
        if (evening.before(Calendar.getInstance())) {
            evening.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent eveningIntent = new Intent(this, ReminderReceiver.class);
        eveningIntent.putExtra("notification_type", "evening");
        PendingIntent eveningPending = PendingIntent.getBroadcast(
                this,
                1002,
                eveningIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                evening.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                eveningPending
        );

        // --- Special Alert ---
        Calendar special = Calendar.getInstance();
        special.set(Calendar.HOUR_OF_DAY, 12);
        special.set(Calendar.MINUTE, 30);
        special.set(Calendar.SECOND, 0);
        if (special.before(Calendar.getInstance())) {
            special.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent specialIntent = new Intent(this, ReminderReceiver.class);
        specialIntent.putExtra("notification_type", "special");
        PendingIntent specialPending = PendingIntent.getBroadcast(
                this,
                1003,
                specialIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                special.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                specialPending
        );
    }

}