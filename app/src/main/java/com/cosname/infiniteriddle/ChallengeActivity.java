package com.cosname.infiniteriddle;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Queue;
import java.util.ArrayDeque;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.material.appbar.MaterialToolbar;

import com.google.android.gms.games.PlayGames;



public class ChallengeActivity extends AppCompatActivity implements PremiumStatusReceiver.PremiumStatusListener {

    private TextView riddleText, answerText, bonusText, totalText, errorText, timerText, scoreText,hintText, finalScoreText;
    private EditText answerInput;
    private Button submitButton, playAgainButton;
    private LinearLayout challengeLayout, resultLayout;

    private CountDownTimer countDownTimer;
    private int currentRiddleIndex = 0;
    private int score = 0;
    private final int MAX_RIDDLES = 5;
    private final int TIME_LIMIT = 50000;

    private List<Riddle> riddlesList;
    private boolean allCorrect = true;
    private static final String TAG = "NativeAdLoad";
    private RewardedAd rewardedAd;
    private View loadingOverlay;
    private boolean timeUp = false;
    private int adLoadAttempts = 0;
    private static final int SCORE_PER_CORRECT = 30;
    private static final int MAX_SCORE = 200;
    private static final int HIGH_SCORE_THRESHOLD = 150;

    private FirebaseLeaderboardHelper firebaseHelper;
    private boolean lastSyncOnline = false;
    private View view;
    private PremiumStatusReceiver premiumStatusReceiver;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

        AdManager.getInstance(this);
        MusicManager.initialize(this);
        firebaseHelper = new FirebaseLeaderboardHelper();

        // Challenge layout views
        challengeLayout = findViewById(R.id.challengeLayout);
        riddleText = findViewById(R.id.riddleText);
        answerText = findViewById(R.id.answerText);
        errorText = findViewById(R.id.errorText);
        totalText = findViewById(R.id.totalText);
        hintText = findViewById(R.id.hintText);
        bonusText = findViewById(R.id.bonusText);
        timerText = findViewById(R.id.timerText);
        scoreText = findViewById(R.id.scoreText);
        answerInput = findViewById(R.id.answerInput);
        submitButton = findViewById(R.id.submitButton);

        loadingOverlay = findViewById(R.id.loadingOverlay);
        view = findViewById(R.id.view);

        resultLayout = findViewById(R.id.resultLayout);
        finalScoreText = findViewById(R.id.finalScoreText);
        playAgainButton = findViewById(R.id.playAgainButton);

        riddlesList = RiddleRepository.getRandomDifficultyFiveRiddles(this, MAX_RIDDLES);

        SharedPreferences prefs = getSharedPreferences("ChallengePrefs", MODE_PRIVATE);
        String lastPlayedDate = prefs.getString("lastPlayedDate", "");
        String today = getCurrentDateString();

        initializeRewardedAd();
        restartChallenge();

        if (riddlesList.isEmpty()) {
            Toast.makeText(this, "No riddles with difficulty 5 found!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Only load native ads if ads are not removed
        if (!AdManager.areAdsRemoved(this)) {
            loadNativeAd();
        } else {
            // Hide the native ad view if ads are removed
            View nativeAdView = findViewById(R.id.nativeAdView);
            if (nativeAdView != null) {
                nativeAdView.setVisibility(View.GONE);
            }
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        currentRiddleIndex = 0; score = 0; allCorrect = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        builder.setTitle("Challenge Instructions");
        builder.setMessage(
                "• Each riddle is worth 30 marks.\n" +
                        "• Read the Riddle, HINT Below RIDDLE and type your answer\n" +
                        "• You have 40 Seconds to complete one riddle.\n" +
                        "• Type your answer in the input box below each riddle.\n" +
                        "• Press the 'SUBMIT ANSWER' button to submit your answer.\n" +
                        "• You can’t retry the challenge on the same day.\n" +
                        "• Max points for this challenge is 150 points + 50 Bonus Points.\n" +
                        "• Try your best and have fun!"
        );
        builder.setCancelable(false);
        builder.setPositiveButton("Start Challenge", (dialog, which) -> {
            if (!today.equals(lastPlayedDate)) {
                startTimer();
                loadNextRiddle(true);
            }
        });
        builder.setNegativeButton("Exit", (dialog, which) -> {
            getOnBackPressedDispatcher().onBackPressed();
        });
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.game_bg_gradient);
        }
        dialog.show();
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(Color.WHITE);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.RED);
        }

        submitButton.setOnClickListener(v -> checkAnswer());
        playAgainButton.setOnClickListener(v -> restartChallenge());

        setupAnswerInput();
        currentRiddleIndex = 0;
        registerPremiumStatusReceiver();
    }
    
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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
        runOnUiThread(() -> {
            if (isPremium) {
                // Hide native ads
                View nativeAdView = findViewById(R.id.nativeAdView);
                if (nativeAdView != null) {
                    nativeAdView.setVisibility(View.GONE);
                }
                
                // Stop ad rotation
                if (nativeAdRotateRunnable != null) {
                    nativeAdHandler.removeCallbacks(nativeAdRotateRunnable);
                    nativeAdRotateRunnable = null;
                }
                
                // Clear the ad queue and destroy current ad
                if (currentNativeAd != null) {
                    currentNativeAd.destroy();
                    currentNativeAd = null;
                }
                nativeAdQueue.clear();
                
                Toast.makeText(this, "Premium activated! Ads removed!", Toast.LENGTH_SHORT).show();
            }
        });
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
    private void showLoadingOverlay() {
        loadingOverlay.setVisibility(View.VISIBLE);
    }
    private void hideLoadingOverlay() {
        loadingOverlay.setVisibility(View.GONE);
    }
    private void loadNextRiddle(boolean startTimerNow) {
        errorText.setVisibility(View.GONE);

        // Guard against invalid index
        if (riddlesList == null || currentRiddleIndex < 0 || currentRiddleIndex >= riddlesList.size()) {
            endChallenge();
            return;
        }

        // Safely load current riddle
        Riddle currentRiddle = riddlesList.get(currentRiddleIndex);
        riddleText.setText(currentRiddle.getQuestion());
        hintText.setText(currentRiddle.getHint());
        answerText.setText(currentRiddle.getAnswer());
        scoreText.setText(String.valueOf(score));

        answerInput.setEnabled(true);
        submitButton.setEnabled(true);

        answerInput.setText("");
        errorText.setText("");
        timeUp = false;

        if (startTimerNow) {
            startTimer();
        }
    }
    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timeUp = false;

        countDownTimer = new CountDownTimer(TIME_LIMIT, 1000) {
            @SuppressLint("SetTextI18n")
            public void onTick(long millisUntilFinished) {
                timerText.setText((millisUntilFinished / 1000) + "s");
            }

            @SuppressLint("SetTextI18n")
            public void onFinish() {
                timeUp = true;
                SoundManager.playWrong(ChallengeActivity.this);
                errorText.setVisibility(View.VISIBLE);
                errorText.setText("Time's up! Moving to next riddle. Ans = " + riddlesList.get(currentRiddleIndex).getAnswer());
                timerText.postDelayed(() -> {
                    allCorrect = false;
                    nextRiddle();
                }, 2500);
            }
        }.start();
    }
    private void setupAnswerInput() {
        answerInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        answerInput.setSingleLine(true);

        answerInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_NEXT
                    || (event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                checkAnswer();
                return true;
            }
            return false;
        });
    }

    private void checkAnswer() {
        submitButton.setEnabled(false);

        String userAnswer = answerInput.getText().toString().trim().toLowerCase();
        String correctAnswer = riddlesList.get(currentRiddleIndex).getAnswer().toLowerCase();

        if (userAnswer.equals(correctAnswer)) {
            score += 30;
            SoundManager.playCorrect(this);
            timerText.postDelayed(this::nextRiddle, 1000);
        } else {
            allCorrect = false;
            SoundManager.playWrong(this);
            if (timeUp) {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText("Incorrect! Moving to next riddle.");
                timerText.postDelayed(this::nextRiddle, 1500);
            } else {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText("Incorrect Answer! Try again.");
                submitButton.setEnabled(true);
            }
        }
    }
    private void nextRiddle() {
        errorText.setVisibility(View.GONE);

        currentRiddleIndex++;

        if (riddlesList == null || currentRiddleIndex >= riddlesList.size()) {
            endChallenge();
            return;
        }

        loadNextRiddle(true);
        submitButton.setEnabled(true);
    }
    private void endChallenge() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        timerText.removeCallbacks(null);
        answerInput.removeCallbacks(null);
        submitButton.removeCallbacks(null);

        if (currentRiddleIndex == 0 && score == 0) {
            return;
        }

        if (currentNativeAd != null) {
            currentNativeAd.destroy();
            currentNativeAd = null;
        }

        NativeAdView adView = findViewById(R.id.nativeAdView);
        if (adView != null) {
            adView.setVisibility(View.GONE);
        }

        ImageView fallbackImage = findViewById(R.id.fallback_media);
        if (fallbackImage != null) {
            fallbackImage.setImageDrawable(null);
            fallbackImage.setVisibility(View.GONE);
        }

        hideKeyboard();

        int correctCount = score / SCORE_PER_CORRECT;
        int bonus = Math.min(Math.max(correctCount, 0), 5) * 10;
        int totalScore = score + bonus;
        saveScore(totalScore);

        SharedPreferences prefs = getSharedPreferences("ChallengePrefs", MODE_PRIVATE);
        prefs.edit().putString("lastPlayedDate", getCurrentDateString()).apply();

        challengeLayout.setVisibility(View.GONE);
        view.setVisibility(View.GONE);
        resultLayout.setVisibility(View.VISIBLE);
        displayScores(score, bonus, totalScore);

        if (totalScore == MAX_SCORE) {
            MusicManager.playSoundEffect(this, MusicManager.SOUND_LEVEL_UP);
            showPerfectScoreAnimation();
        } else if (totalScore >= HIGH_SCORE_THRESHOLD) {
            MusicManager.playSoundEffect(this, MusicManager.SOUND_LEVEL_UP);
        } else {
            MusicManager.playSoundEffect(this, MusicManager.SOUND_LEVEL_UP);
        }
    }

    private void performCleanRestart() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        score = 0;
        currentRiddleIndex = 0;
        allCorrect = true;
        timeUp = false;

        // Reload riddles
        riddlesList = RiddleRepository.getRandomDifficultyFiveRiddles(this, MAX_RIDDLES);

        // Reset UI
        runOnUiThread(() -> {
            resultLayout.setVisibility(View.GONE);
            challengeLayout.setVisibility(View.VISIBLE);

            answerInput.setEnabled(true);
            submitButton.setEnabled(true);
            
            loadNativeAd();
            playAgainButton.setEnabled(true);

            answerInput.setText("");
            errorText.setText("");
            timerText.setText("");

            loadNextRiddle(true);
        });
    }

    private void displayScores(int score, int bonus, int totalScore) {
        SpannableString scoreText = new SpannableString(String.format("SCORE\n%,d", score));
        scoreText.setSpan(new StyleSpan(Typeface.BOLD), 6, scoreText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        scoreText.setSpan(new RelativeSizeSpan(1.2f), 6, scoreText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        finalScoreText.setText(scoreText);
        finalScoreText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

        SpannableString bonusDisplay = new SpannableString(String.format("BONUS\n+%,d", bonus));
        bonusDisplay.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 6, bonusDisplay.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        bonusDisplay.setSpan(new StyleSpan(Typeface.BOLD), 6, bonusDisplay.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        bonusText.setText(bonusDisplay);
        bonusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        SpannableString totalDisplay = new SpannableString(String.format("TOTAL\n%,d", totalScore));
        totalDisplay.setSpan(new ForegroundColorSpan(Color.parseColor("#FFC107")), 6, totalDisplay.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        totalDisplay.setSpan(new StyleSpan(Typeface.BOLD), 6, totalDisplay.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        totalDisplay.setSpan(new RelativeSizeSpan(1.4f), 6, totalDisplay.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        totalText.setText(totalDisplay);
        totalText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        if (totalScore == 200) {
            SpannableString perfectText = new SpannableString("PERFECT SCORE!\n");
            perfectText.setSpan(new ForegroundColorSpan(Color.parseColor("#FF5722")), 0, perfectText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            perfectText.setSpan(new RelativeSizeSpan(1.3f), 0, perfectText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            SpannableStringBuilder builder = new SpannableStringBuilder(perfectText);
            builder.append(totalDisplay);
            totalText.setText(builder);

            ConfettiView confettiView = findViewById(R.id.confettiView);
            confettiView.setVisibility(View.VISIBLE);
            confettiView.startConfetti();
        }
    }
    private String getCurrentDateString() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd");
        return sdf.format(new java.util.Date());
    }
    private void showPerfectScoreAnimation() {
        // Confetti animation
        ConfettiView confettiView = findViewById(R.id.confettiView);
        confettiView.setVisibility(View.VISIBLE);
        confettiView.startConfetti();

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(totalText, "scaleX", 1f, 1.2f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(totalText, "scaleY", 1f, 1.2f);
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(totalText, "scaleX", 1.2f, 1f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(totalText, "scaleY", 1.2f, 1f);

        scaleUpX.setDuration(150);
        scaleUpY.setDuration(150);
        scaleDownX.setDuration(150);
        scaleDownY.setDuration(150);

        AnimatorSet pulseAnim = new AnimatorSet();
        pulseAnim.play(scaleUpX).with(scaleUpY);
        pulseAnim.play(scaleDownX).with(scaleDownY).after(scaleUpX);
        pulseAnim.start();
        pulseAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                pulseAnim.start();
            }
        });
    }
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private void saveScore(int challengeScore) {
        SharedPreferences prefs = getSharedPreferences("RiddlePrefs", MODE_PRIVATE);
        int previousScore = prefs.getInt("score", 0);
        int newTotalScore = previousScore + challengeScore;

        prefs.edit().putInt("score", newTotalScore).apply();

        syncScoreToFirebase();
        syncScoreToPlayStore(newTotalScore);
    }
    private void syncScoreToFirebase() {
        if (firebaseHelper == null) {
            firebaseHelper = new FirebaseLeaderboardHelper();
        }
        SharedPreferences appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences riddlePrefs = getSharedPreferences("RiddlePrefs", MODE_PRIVATE);
        String username = appPrefs.getString("username", "");
        int userScore = riddlePrefs.getInt("score", 0);

        if (isOnline()) {
            if (appPrefs.getBoolean("hasUnsyncedScore", false)) {
                int unsyncedScore = appPrefs.getInt("unsyncedScore", userScore);
                firebaseHelper.uploadUserScore(username, unsyncedScore, new FirebaseLeaderboardHelper.UploadCallback() {
                    @Override
                    public void onSuccess() {
                        appPrefs.edit()
                                .remove("hasUnsyncedScore")
                                .remove("unsyncedScore")
                                .apply();
                    }

                    @Override
                    public void onFailure(Exception e) {
                    }
                });
            }

            firebaseHelper.uploadUserScore(username, userScore, new FirebaseLeaderboardHelper.UploadCallback() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(Exception e) {
                    appPrefs.edit()
                            .putBoolean("hasUnsyncedScore", true)
                            .putInt("unsyncedScore", userScore)
                            .apply();
                }
            });
            lastSyncOnline = true;
        } else {
            appPrefs.edit()
                    .putBoolean("hasUnsyncedScore", true)
                    .putInt("unsyncedScore", userScore)
                    .apply();
            lastSyncOnline = false;
        }
    }
    private void syncScoreToPlayStore(int score) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient(this);
            leaderboardsClient.submitScore(getString(R.string.leaderboard_id), score);
        } else {
            Log.d("GPGS", "User not signed in, cannot sync score to Play Store");
        }
    }

    private void restartChallenge() {
        SharedPreferences prefs = getSharedPreferences("ChallengePrefs", MODE_PRIVATE);
        String lastPlayedDate = prefs.getString("lastPlayedDate", "");
        String today = getCurrentDateString();

        if (today.equals(lastPlayedDate)) {
            // Check if ads are removed - if so, allow unlimited play
            if (AdManager.areAdsRemoved(this)) {
                // Premium users can play unlimited times
                performCleanRestart();
                return;
            }
            showWatchAdDialog();
            return;
        }
        performCleanRestart();
    }
    private void showWatchAdDialog() {
        AdManager adManager = AdManager.getInstance(this);
        final WeakReference<ChallengeActivity> activityRef = new WeakReference<>(this);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                .setTitle("Already Played Today")
                .setMessage("You can normally play once per day, but you can watch an ad to play now!")
                .setPositiveButton("Watch Ad", (d, w) -> {
                    ChallengeActivity activity = activityRef.get();
                    if (activity == null || activity.isFinishing()) return;

                    if (!isNetworkAvailable()) {
                        showOfflineDialog(activityRef);
                        return;
                    }

                    activity.showLoadingOverlay();

                    adManager.showRewardedAd(activity, new AdManager.AdCallback() {
                        @Override
                        public void onRewardEarned(int rewardAmount) {
                            ChallengeActivity act = activityRef.get();
                            if (act == null || act.isFinishing()) return;

                            act.hideLoadingOverlay();
                            SharedPreferences prefs = act.getSharedPreferences("ChallengePrefs", MODE_PRIVATE);
                            prefs.edit().remove("lastPlayedDate").apply();
                            act.performCleanRestart();
                            act.loadNativeAd();
                            startTimer();
                            Toast.makeText(act, "Access granted! Enjoy your game", Toast.LENGTH_SHORT).show();
                            adLoadAttempts = 0;
                        }

                        @Override
                        public void onAdDismissed(boolean success) {
                            ChallengeActivity act = activityRef.get();
                            if (act == null || act.isFinishing()) return;

                            act.hideLoadingOverlay();
                            if (!success) {
                                handleAdFailure(activityRef);
                            } else {
                                adLoadAttempts = 0;
                            }
                        }

                        @Override
                        public void onAdFailedToLoad() {
                            handleAdFailure(activityRef);
                        }
                    });
                })
                .setNegativeButton("Exit", (d, w) -> {
                    ChallengeActivity activity = activityRef.get();
                    if (activity != null && !activity.isFinishing()) {
                        activity.finish();
                    }
                    adLoadAttempts = 0;
                })
                .setCancelable(false)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED);
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        return false;
    }
    private void showOfflineDialog(WeakReference<ChallengeActivity> activityRef) {
        ChallengeActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) return;

        activity.runOnUiThread(() -> {
            AlertDialog dialog = new AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme)
                    .setTitle("No Internet Connection")
                    .setMessage("Please check your internet connection and try again.")
                    .setPositiveButton("Retry", (d, w) -> {
                        if (isNetworkAvailable()) {
                            activity.showWatchAdDialog();
                        } else {
                            showOfflineDialog(activityRef);
                        }
                    })
                    .setNegativeButton("Exit", (d, w) -> activity.finish())
                    .setCancelable(false)
                    .show();

            // Make the dialog buttons white
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED);
        });
    }
    private void handleAdFailure(WeakReference<ChallengeActivity> activityRef) {
        ChallengeActivity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) return;

        activity.hideLoadingOverlay();
        adLoadAttempts++;

        if (adLoadAttempts >= 2) {
            activity.runOnUiThread(() -> {
                AlertDialog dialog = new AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme)
                        .setTitle("Ad Not Available")
                        .setMessage("Please try again later.")
                        .setPositiveButton("OK", (d, w) -> activity.finish())
                        .setCancelable(false)
                        .show();

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
            });
            adLoadAttempts = 0;
        } else {
            activity.runOnUiThread(() -> {
                AlertDialog dialog = new AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme)
                        .setTitle("Ad Failed to Load")
                        .setMessage("Would you like to try again?")
                        .setPositiveButton("Retry", (d, w) -> {
                            if (isNetworkAvailable()) {
                                activity.showWatchAdDialog();
                            } else {
                                showOfflineDialog(activityRef);
                            }
                        })
                        .setNegativeButton("Exit", (d, w) -> activity.finish())
                        .setCancelable(false)
                        .show();

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED);
            });
        }
    }
    private NativeAd currentNativeAd;
    private final Queue<NativeAd> nativeAdQueue = new ArrayDeque<>();
    private static final int NATIVE_AD_PREFETCH_COUNT = 3;
    private final Handler nativeAdHandler = new Handler(Looper.getMainLooper());
    private Runnable nativeAdRotateRunnable;
    private static final long STATIC_NATIVE_AD_ROTATE_MS = 65000L; // Respect AdMob 60s+ guideline

    private void loadNativeAd() {
        // Maintain method name but switch to prefetch strategy
        preloadNativeAds(NATIVE_AD_PREFETCH_COUNT);
    }

    private void preloadNativeAds(int count) {
        // Check if ads are removed before preloading
        if (AdManager.areAdsRemoved(this)) {
            return;
        }

        AdLoader adLoader = new AdLoader.Builder(this, "ca-app-pub-6979842665203661/5149002782")
                .forNativeAd(nativeAd -> {
                    nativeAdQueue.offer(nativeAd);
                    if (currentNativeAd == null) {
                        showNextNativeAd();
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setVideoOptions(new VideoOptions.Builder()
                                .setStartMuted(true)
                                .build())
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build())
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        if (currentNativeAd == null && nativeAdQueue.isEmpty()) {
                            showFallbackAd();
                        }
                    }
                })
                .build();

        adLoader.loadAds(new AdRequest.Builder().build(), Math.max(1, count));
    }

    private void showNextNativeAd() {
        // Check if ads are removed before showing native ad
        if (AdManager.areAdsRemoved(this)) {
            // Hide native ad view if ads are removed
            View nativeAdView = findViewById(R.id.nativeAdView);
            if (nativeAdView != null) {
                nativeAdView.setVisibility(View.GONE);
            }
            return;
        }

        NativeAd next = nativeAdQueue.poll();
        if (next == null) {
            preloadNativeAds(NATIVE_AD_PREFETCH_COUNT);
            return;
        }

        if (nativeAdRotateRunnable != null) {
            nativeAdHandler.removeCallbacks(nativeAdRotateRunnable);
            nativeAdRotateRunnable = null;
        }

                    if (currentNativeAd != null) {
                        currentNativeAd.destroy();
                    }
        currentNativeAd = next;

                    NativeAdView adView = findViewById(R.id.nativeAdView);
                    if (adView == null) return;
                    
                    adView.setVisibility(View.VISIBLE);

                    MediaView mediaView = adView.findViewById(R.id.ad_media);
                    ImageView fallbackImage = adView.findViewById(R.id.fallback_media);
                    TextView headlineView = adView.findViewById(R.id.ad_headline);
                    TextView bodyView = adView.findViewById(R.id.ad_body);
                    Button ctaView = adView.findViewById(R.id.ad_call_to_action);

                    fallbackImage.setVisibility(View.GONE);
                    mediaView.setVisibility(View.VISIBLE);

                    adView.setMediaView(mediaView);
                    adView.setHeadlineView(headlineView);
                    adView.setBodyView(bodyView);
                    adView.setCallToActionView(ctaView);

        headlineView.setText(next.getHeadline());

        if (next.getMediaContent() != null) {
            mediaView.setMediaContent(next.getMediaContent());
                    }

        if (next.getBody() == null) {
                        bodyView.setVisibility(View.INVISIBLE);
                    } else {
                        bodyView.setVisibility(View.VISIBLE);
            bodyView.setText(next.getBody());
                    }

        if (next.getCallToAction() == null) {
                        ctaView.setVisibility(View.INVISIBLE);
                    } else {
                        ctaView.setVisibility(View.VISIBLE);
            ctaView.setText(next.getCallToAction());
        }

        adView.setNativeAd(next);

        scheduleNativeAdRotation(next, adView);

        if (nativeAdQueue.size() < NATIVE_AD_PREFETCH_COUNT - 1) {
            preloadNativeAds(NATIVE_AD_PREFETCH_COUNT - nativeAdQueue.size());
        }
    }

    private void scheduleNativeAdRotation(NativeAd ad, NativeAdView adView) {
        if (ad == null || adView == null) return;

        boolean hasVideo = ad.getMediaContent() != null && ad.getMediaContent().hasVideoContent();
        if (hasVideo) {
            try {
                com.google.android.gms.ads.VideoController controller = ad.getMediaContent().getVideoController();
                if (controller != null) {
                    controller.setVideoLifecycleCallbacks(new com.google.android.gms.ads.VideoController.VideoLifecycleCallbacks() {
                        @Override
                        public void onVideoEnd() {
                            // Advance to next ad when video completes
                            nativeAdHandler.postDelayed(() -> showNextNativeAd(), 1000);
                        }
                    });
                }
            } catch (Exception ignored) {}
        } else {
            nativeAdRotateRunnable = () -> {
                if (adView.getVisibility() == View.VISIBLE) {
                    showNextNativeAd();
                }
            };
            nativeAdHandler.postDelayed(nativeAdRotateRunnable, STATIC_NATIVE_AD_ROTATE_MS);
        }
    }
    @SuppressLint("SetTextI18n")
    private void showFallbackAd() {
        NativeAdView adView = findViewById(R.id.nativeAdView);
        if (adView == null) return;

        adView.setVisibility(View.VISIBLE);

        MediaView mediaView = adView.findViewById(R.id.ad_media);
        ImageView fallbackImage = adView.findViewById(R.id.fallback_media);
        TextView headlineView = adView.findViewById(R.id.ad_headline);
        TextView bodyView = adView.findViewById(R.id.ad_body);
        Button ctaView = adView.findViewById(R.id.ad_call_to_action);

        mediaView.setVisibility(View.GONE);
        fallbackImage.setVisibility(View.VISIBLE);
        fallbackImage.setImageResource(R.drawable.status);

        headlineView.setText("Never Lose a WhatsApp Status Again");
        bodyView.setVisibility(View.VISIBLE);
        bodyView.setText("View, save, and share WhatsApp statuses before they disappear.");

        ctaView.setVisibility(View.VISIBLE);
        ctaView.setText("Download");
        ctaView.setOnClickListener(v -> {
            String url = "https://play.google.com/store/apps/details?id=com.cosname.statussaver";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (totalText != null) {
            totalText.clearAnimation();
        }
        MusicManager.pause();
        if (nativeAdRotateRunnable != null) {
            nativeAdHandler.removeCallbacks(nativeAdRotateRunnable);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (challengeLayout.getVisibility() == View.VISIBLE && !timeUp) {
            startTimer();
        }
        MusicManager.start(this);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                .setTitle("Exit Challenge")
                .setMessage("Are you sure you want to exit? Your progress will be lost.")
                .setPositiveButton("Exit", (dialogInterface, which) -> finish())
                .setNegativeButton("Cancel", null)
                .show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    @Override
    protected void onDestroy() {
        currentRiddleIndex = 0;
        score = 0;
        allCorrect = true;
        timeUp = false;
        if (premiumStatusReceiver != null) {
            unregisterReceiver(premiumStatusReceiver);
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (rewardedAd != null) {
            rewardedAd = null;
        }

        if (currentNativeAd != null) {
            currentNativeAd.destroy();
            currentNativeAd = null;
        }
        if (nativeAdRotateRunnable != null) {
            nativeAdHandler.removeCallbacks(nativeAdRotateRunnable);
            nativeAdRotateRunnable = null;
        }
        super.onDestroy();
    }
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }
        return false;
    }
}
