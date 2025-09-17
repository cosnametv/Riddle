package com.cosname.infiniteriddle;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicyActivity extends AppCompatActivity {
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        MusicManager.initialize(this);

        WebView webView = findViewById(R.id.privacyWebView);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);

        webView.loadUrl("https://cosname.web.app/status/riddle/privacy.html");
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (preferences.getBoolean("music_on", true)) {
            MusicManager.start(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pause();
        if (isFinishing()) {
            MusicManager.pause();
        }
    }
}
