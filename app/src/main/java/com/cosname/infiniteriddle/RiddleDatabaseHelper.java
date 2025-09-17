package com.cosname.infiniteriddle;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class RiddleDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "riddles.db";
    private static final int DATABASE_VERSION =4;
    private static final String TABLE_NAME = "riddles";

    private static final String COL_ID = "id";
    private static final String COL_QUESTION = "question";
    private static final String COL_ANSWER = "answer";
    private static final String COL_HINT = "hint";
    private static final String COL_DIFFICULTY = "difficulty";
    private final Context context;


    public RiddleDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_QUESTION + " TEXT NOT NULL, " +
                COL_ANSWER + " TEXT NOT NULL, " +
                COL_HINT + " TEXT, " +
                COL_DIFFICULTY + " INTEGER DEFAULT 1" +
                ");";
        db.execSQL(createTable);
        insertInitialRiddles(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop and recreate table to ensure all riddles are present
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    private void insertInitialRiddles(SQLiteDatabase db) {
        try {
            InputStream is = context.getAssets().open("riddles.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");

            JSONArray riddlesArray = new JSONArray(jsonStr);
            for (int i = 0; i < riddlesArray.length(); i++) {
                JSONObject riddle = riddlesArray.getJSONObject(i);
                String question = riddle.getString("question");
                String answer = riddle.getString("answer");
                String hint = riddle.getString("hint");
                int difficulty = riddle.getInt("difficulty");

                insertRiddle(db, question, answer, hint, difficulty);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void insertRiddle(SQLiteDatabase db, String question, String answer, String hint, int difficulty) {
        ContentValues values = new ContentValues();
        values.put(COL_QUESTION, question);
        values.put(COL_ANSWER, answer);
        values.put(COL_HINT, hint);
        values.put(COL_DIFFICULTY, difficulty);
        db.insert(TABLE_NAME, null, values);
    }

    public List<Riddle> getAllRiddles() {
        List<Riddle> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME,
                new String[]{COL_QUESTION, COL_ANSWER, COL_HINT, COL_DIFFICULTY},
                null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String q = cursor.getString(cursor.getColumnIndexOrThrow(COL_QUESTION));
                String a = cursor.getString(cursor.getColumnIndexOrThrow(COL_ANSWER));
                String h = cursor.getString(cursor.getColumnIndexOrThrow(COL_HINT));
                int d = cursor.getInt(cursor.getColumnIndexOrThrow(COL_DIFFICULTY));
                list.add(new Riddle(q, a, h, d));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}
