package com.cosname.infiniteriddle;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseLeaderboardHelper {
    private static final String LEADERBOARD_PATH = "leaderboard";
    private final DatabaseReference leaderboardRef;

    public FirebaseLeaderboardHelper() {
        leaderboardRef = FirebaseDatabase.getInstance().getReference(LEADERBOARD_PATH);
    }

    public interface UploadCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface FetchCallback {
        void onResult(List<ScoreEntry> scores);
        void onError(Exception e);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Upload or update the user's score (by name as key)
    public void uploadUserScore(String name, int score, UploadCallback callback) {
        if (name == null || name.trim().isEmpty()) {
            if (callback != null) callback.onFailure(new IllegalArgumentException("Name is empty"));
            return;
        }
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("score", score);
        leaderboardRef.child(name).setValue(userData)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onFailure(e);
            });
    }

    // Update username in Firebase (this will update ALL existing entries with the old username)
    public void updateUsername(String oldUsername, String newUsername, int score, UpdateCallback callback) {
        if (newUsername == null || newUsername.trim().isEmpty()) {
            if (callback != null) callback.onFailure(new IllegalArgumentException("Username is empty"));
            return;
        }
        
        // Find ALL existing entries and update them
        if (oldUsername != null && !oldUsername.trim().isEmpty()) {
            // First, check if there's an entry with the old username as the key
            leaderboardRef.child(oldUsername).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Found entry with old username as key, update it
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", newUsername);
                        userData.put("score", score);
                        
                        // Remove the old entry and create new one with new username as key
                        leaderboardRef.child(oldUsername).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                // Create new entry with new username as key
                                leaderboardRef.child(newUsername).setValue(userData)
                                    .addOnSuccessListener(aVoid2 -> {
                                        if (callback != null) callback.onSuccess();
                                    })
                                    .addOnFailureListener(e -> {
                                        if (callback != null) callback.onFailure(e);
                                    });
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onFailure(e);
                            });
                    } else {
                        // No entry with old username as key, search for entries with old username in name field
                        searchAndUpdateByName(oldUsername, newUsername, score, callback);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // If query fails, search by name field
                    searchAndUpdateByName(oldUsername, newUsername, score, callback);
                }
            });
        } else {
            // No old username, just add new entry
            addNewEntry(newUsername, score, callback);
        }
    }
    
    private void searchAndUpdateByName(String oldUsername, String newUsername, int score, UpdateCallback callback) {
        // Find ALL entries with the old username in the name field
        leaderboardRef.orderByChild("name").equalTo(oldUsername).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Found entries, update ALL of them
                    int totalEntries = (int) snapshot.getChildrenCount();
                    final int[] updatedCount = {0};
                    final boolean[] hasError = {false};
                    
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String key = child.getKey();
                        if (key != null) {
                            // Update the existing entry with new username
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", newUsername);
                            userData.put("score", score);
                            
                            leaderboardRef.child(key).setValue(userData)
                                .addOnSuccessListener(aVoid -> {
                                    updatedCount[0]++;
                                    if (updatedCount[0] == totalEntries) {
                                        // All entries updated successfully
                                        if (callback != null) callback.onSuccess();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    hasError[0] = true;
                                    if (callback != null) callback.onFailure(e);
                                });
                        }
                    }
                    
                    // If no entries were found to update, create new entry
                    if (totalEntries == 0) {
                        addNewEntry(newUsername, score, callback);
                    }
                } else {
                    // If not found, create new entry
                    addNewEntry(newUsername, score, callback);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // If query fails, create new entry
                addNewEntry(newUsername, score, callback);
            }
        });
    }
    
    private void addNewEntry(String username, int score, UpdateCallback callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", username);
        userData.put("score", score);
        
        leaderboardRef.child(username).setValue(userData)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onFailure(e);
            });
    }

    // Fetch all leaderboard entries, sorted by score descending
    public void fetchLeaderboard(FetchCallback callback) {
        leaderboardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ScoreEntry> scores = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = "";
                    int score = 0;
                    if (child.child("name").getValue() != null) {
                        name = child.child("name").getValue(String.class);
                    }
                    if (child.child("score").getValue() != null) {
                        score = child.child("score").getValue(Integer.class);
                    }
                    if (!name.isEmpty()) {
                        scores.add(new ScoreEntry(name, score));
                    }
                }
                // Sort by score descending
                Collections.sort(scores, new Comparator<ScoreEntry>() {
                    @Override
                    public int compare(ScoreEntry o1, ScoreEntry o2) {
                        return Integer.compare(o2.score, o1.score);
                    }
                });
                if (callback != null) callback.onResult(scores);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onError(error.toException());
            }
        });
    }
}
