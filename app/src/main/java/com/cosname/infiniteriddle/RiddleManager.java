package com.cosname.infiniteriddle;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;

public class RiddleManager {
    private List<Riddle> riddles;
    private int currentIndex = 0;
    private RiddleDatabaseHelper dbHelper;
    private SharedPreferences prefs;

    public RiddleManager(Context context) {
        dbHelper = new RiddleDatabaseHelper(context);
        riddles = dbHelper.getAllRiddles();
        prefs = context.getSharedPreferences("RiddlePrefs", Context.MODE_PRIVATE);
        int savedIndex = prefs.getInt("riddleIndex", 0);
        currentIndex = (savedIndex >= 0 && savedIndex < riddles.size()) ? savedIndex : 0;
    }

    public Riddle getNextRiddle() {
        if (riddles == null || riddles.isEmpty()) return null;
        if (currentIndex >= riddles.size()) currentIndex = 0;
        return riddles.get(currentIndex++);
    }

    public void setCurrentLevel(int savedIndex) {
        currentIndex = (savedIndex >= 0 && savedIndex < riddles.size()) ? savedIndex : 0;
    }

    public int getCurrentLevel() {
        return currentIndex;
    }

    public int getTotalRiddles() {
        return riddles != null ? riddles.size() : 0;
    }

    public void saveProgress() {
        prefs.edit().putInt("riddleIndex", currentIndex).apply();
    }
}
