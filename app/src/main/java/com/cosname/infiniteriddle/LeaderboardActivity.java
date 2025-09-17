package com.cosname.infiniteriddle;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LeaderboardActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private LeaderboardAdapter adapter;
    private List<ScoreEntry> scoreList = new ArrayList<>();
    private FirebaseLeaderboardHelper firebaseHelper;
    private SharedPreferences cachePrefs;
    
    // User details views
    private TextView userPositionText;
    private TextView userNameText;
    private TextView userScoreText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        firebaseHelper = new FirebaseLeaderboardHelper();
        cachePrefs = getSharedPreferences("LeaderboardCache", MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.leaderboardRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        // Initialize user details views
        userPositionText = findViewById(R.id.userPositionText);
        userNameText = findViewById(R.id.userNameText);
        userScoreText = findViewById(R.id.userScoreText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(scoreList);
        adapter.setCurrentUsername(this);
        recyclerView.setAdapter(adapter);

        // Update user details immediately
        updateUserDetails();
        
        loadScores();
    }

    private void loadScores() {
        progressBar.setVisibility(View.VISIBLE);
        if (isOnline()) {
            firebaseHelper.fetchLeaderboard(new FirebaseLeaderboardHelper.FetchCallback() {
                @Override
                public void onResult(List<ScoreEntry> scores) {
                    scoreList.clear();
                    if (scores != null && !scores.isEmpty()) {
                        scoreList.addAll(scores);
                        cacheLeaderboard(scores);
                    }
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    updateEmptyView();
                    updateUserDetails();
                }
                @Override
                public void onError(Exception e) {
                    loadCachedLeaderboard();
                }
            });
        } else {
            loadCachedLeaderboard();
        }
    }

    private void cacheLeaderboard(List<ScoreEntry> scores) {
        JSONArray arr = new JSONArray();
        for (ScoreEntry entry : scores) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", entry.name);
                obj.put("score", entry.score);
                arr.put(obj);
            } catch (JSONException e) {
                // Ignore
            }
        }
        cachePrefs.edit().putString("cached_leaderboard", arr.toString()).apply();
    }

    private void loadCachedLeaderboard() {
        scoreList.clear();
        String json = cachePrefs.getString("cached_leaderboard", null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String name = obj.getString("name");
                    int score = obj.getInt("score");
                    scoreList.add(new ScoreEntry(name, score));
                }
            } catch (JSONException e) {
                // Ignore
            }
        }
        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
        updateEmptyView();
        updateUserDetails();
    }

    private void updateEmptyView() {
        if (scoreList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }
        return false;
    }

    private void updateUserDetails() {
        SharedPreferences appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences riddlePrefs = getSharedPreferences("RiddlePrefs", MODE_PRIVATE);
        
        String currentUsername = appPrefs.getString("username", "Unknown User");
        int currentUserScore = riddlePrefs.getInt("score", 0);
        
        // Find user's position in the leaderboard
        int userPosition = findUserPosition(currentUsername, currentUserScore);
        
        // Update the UI
        if (userPosition > 0) {
            userPositionText.setText(String.valueOf(userPosition));
        } else {
            userPositionText.setText("--");
        }
        
        userNameText.setText(currentUsername);
        userScoreText.setText(String.valueOf(currentUserScore));
    }
    
    private int findUserPosition(String username, int userScore) {
        if (scoreList == null || scoreList.isEmpty()) {
            return -1;
        }
        List<ScoreEntry> sortedList = new ArrayList<>(scoreList);
        sortedList.sort((a, b) -> Integer.compare(b.score, a.score));
        for (int i = 0; i < sortedList.size(); i++) {
            ScoreEntry entry = sortedList.get(i);
            if (entry.name.equals(username)) {
                return calculatePositionForUser(sortedList, i);
            }
        }
        for (int i = 0; i < sortedList.size(); i++) {
            if (userScore >= sortedList.get(i).score) {
                return calculatePositionForUser(sortedList, i);
            }
        }
        
        return sortedList.size() + 1;
    }
    private int calculatePositionForUser(List<ScoreEntry> sortedList, int userIndex) {
        if (userIndex < 0 || userIndex >= sortedList.size()) {
            return userIndex + 1;
        }
        int userScore = sortedList.get(userIndex).score;
        int position = 1;

        for (int i = 0; i < userIndex; i++) {
            if (sortedList.get(i).score != userScore) {
                position = i + 2;
            }
        }

        return position;
    }
} 