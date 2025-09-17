package com.cosname.infiniteriddle;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RiddleRepository {

    public static List<Riddle> getRandomDifficultyFiveRiddles(Context context, int count) {
        List<Riddle> allRiddles = loadRiddlesFromAssets(context);
        List<Riddle> filtered = new ArrayList<>();

        for (Riddle r : allRiddles) {
            if (r.getDifficulty() == 5) {
                filtered.add(r);
            }
        }

        Collections.shuffle(filtered);
        if (filtered.size() > count) {
            return filtered.subList(0, count);
        } else {
            return filtered;
        }
    }

    private static List<Riddle> loadRiddlesFromAssets(Context context) {
        try {
            InputStream is = context.getAssets().open("challenges.json");
            InputStreamReader reader = new InputStreamReader(is);
            Type listType = new TypeToken<List<Riddle>>() {}.getType();
            return new Gson().fromJson(reader, listType);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
