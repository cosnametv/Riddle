package com.cosname.infiniteriddle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment implements RemoveAdsBottomSheet.RemoveAdsCallback {

    private static final String PREFS_NAME = "AppSettings";
    private static final String MUSIC_ON = "music_on";
    private static final String SOUNDS_ON = "sounds_on";
    private static final String ADS_REMOVED = "ads_removed";

    private SharedPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Activity activity = requireActivity();
        preferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        TextView usernameText = view.findViewById(R.id.currentUsernameText);
        SharedPreferences appPrefs = activity.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String username = appPrefs.getString("username", "---");
        usernameText.setText("Username: " + username);

        TextView appVersionText = view.findViewById(R.id.appVersionText);
        String versionName = "?";
        try {
            versionName = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (Exception ignored) {}
        appVersionText.setText("App Version: " + versionName);

        View toolbar = view.findViewById(R.id.settingsToolbar);
        if (toolbar != null) {
            toolbar.setOnClickListener(v -> requireActivity().onBackPressed());
            try {
                com.google.android.material.appbar.MaterialToolbar mt = (com.google.android.material.appbar.MaterialToolbar) toolbar;
                mt.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
            } catch (ClassCastException ignored) {}
        }

        setupMusicSwitch(view);
        setupSoundEffectSwitch(view);
        setupActionRows(view);
        setupHelp(view);
        setupRemoveAds(view);
    }

    private void setupMusicSwitch(View root) {
        Switch musicSwitch = root.findViewById(R.id.musicSwitch);
        musicSwitch.setChecked(preferences.getBoolean(MUSIC_ON, true));
        musicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            MusicManager.updateMusicState(requireContext(), isChecked);
            preferences.edit().putBoolean(MUSIC_ON, isChecked).apply();
        });
    }

    private void setupSoundEffectSwitch(View root) {
        Switch soundEffectSwitch = root.findViewById(R.id.soundEffectSwitch);
        soundEffectSwitch.setChecked(preferences.getBoolean(SOUNDS_ON, true));
        soundEffectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            MusicManager.updateSoundState(requireContext(), isChecked);
        });
    }

    private void setupActionRows(View root) {
        // Change username bottom sheet
        View rowChangeUsername = root.findViewById(R.id.rowChangeUsername);
        rowChangeUsername.setOnClickListener(v -> {
            ChangeUsernameBottomSheet sheet = new ChangeUsernameBottomSheet();
            sheet.show(getParentFragmentManager(), "ChangeUsernameBottomSheet");
        });

        // Share
        View rowShare = root.findViewById(R.id.rowShare);
        rowShare.setOnClickListener(v -> {
            String packageName = requireContext().getPackageName();
            String playStoreUrl = "https://play.google.com/store/apps/details?id=" + packageName;
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out Riddle Puzzle!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Try Riddle Puzzle - Word Puzzle Game: " + playStoreUrl);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        // Rate
        View rowRate = root.findViewById(R.id.rowRate);
        rowRate.setOnClickListener(v -> openPlayStorePage());

        // Privacy
        View rowPrivacy = root.findViewById(R.id.rowPrivacy);
        rowPrivacy.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PrivacyPolicyActivity.class);
            startActivity(intent);
        });
    }

    private void openPlayStorePage() {
        String packageName = requireContext().getPackageName();
        boolean opened = false;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            opened = true;
        } catch (ActivityNotFoundException e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                opened = true;
            } catch (Exception ignored) {}
        }
        if (!opened) {
            Toast.makeText(requireContext(), "Unable to open Play Store. Please rate us manually!", Toast.LENGTH_LONG).show();
        }
    }

    private void setupHelp(View root) {
        View rowHelp = root.findViewById(R.id.rowHelp);
        if (rowHelp != null) {
            rowHelp.setOnClickListener(v -> showHowToPlayDialog());
        }
    }

    @android.annotation.SuppressLint("SetTextI18n")
    private void showHowToPlayDialog() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
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

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        ImageView closeButton = dialogView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setupRemoveAds(View root) {
        View rowRemoveAds = root.findViewById(R.id.rowRemoveAds);
        TextView removeAdsStatus = root.findViewById(R.id.removeAdsStatus);

        boolean adsRemoved = PremiumManager.areAdsRemoved(requireContext());
        if (adsRemoved) {
            removeAdsStatus.setVisibility(View.VISIBLE);
        }
        
        rowRemoveAds.setOnClickListener(v -> {
            if (adsRemoved) {
                Toast.makeText(requireContext(), "You already have premium! Ads are removed.", Toast.LENGTH_SHORT).show();
            } else {
                RemoveAdsBottomSheet bottomSheet = new RemoveAdsBottomSheet();
                bottomSheet.show(getParentFragmentManager(), "RemoveAdsBottomSheet");
            }
        });
    }

    @Override
    public void onAdsRemoved() {
        // Update the UI to show premium status
        View view = getView();
        if (view != null) {
            TextView removeAdsStatus = view.findViewById(R.id.removeAdsStatus);
            removeAdsStatus.setVisibility(View.VISIBLE);
        }
        
        // Show success message
        Toast.makeText(requireContext(), "Premium activated! All ads removed!", Toast.LENGTH_LONG).show();
    }
}


