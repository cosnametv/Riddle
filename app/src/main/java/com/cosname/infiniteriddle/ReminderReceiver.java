package com.cosname.infiniteriddle;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;

public class ReminderReceiver extends BroadcastReceiver {

    // Channels
    private static final String CHANNEL_DAILY = "channel_daily_reminders";
    private static final String CHANNEL_ALERTS = "channel_special_alerts";

    // Message pools
    private static final String[] MORNING_MESSAGES = {
            "🌞 Good morning! Start your day with a riddle and sharpen your mind!",
            "Your coffee is ready ☕ — now wake up your brain with today’s puzzle!",
            "A fresh mind is a sharp mind. Crack today’s riddle!"
    };

    private static final String[] EVENING_MESSAGES = {
            "🌙 Relax your mind tonight with a brain teaser.",
            "End your day on a smart note — a new riddle awaits!",
            "Unwind and challenge yourself — tonight’s puzzle is ready!"
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        // Get notification type from intent
        String type = intent.getStringExtra("notification_type");
        boolean isDaily = "morning".equals(type) || "evening".equals(type);

        // Choose channel ID based on type
        String channelId = isDaily ? CHANNEL_DAILY : CHANNEL_ALERTS;

        // Create channels if needed
        createNotificationChannels(context);

        // Permission check for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return; // no permission → skip
        }

        // Avoid duplicate sends (within 6 hours)
        if (isTooSoon(context, type)) return;

        // Pick message
        String title;
        String message;

        if ("morning".equals(type)) {
            message = MORNING_MESSAGES[new Random().nextInt(MORNING_MESSAGES.length)];
            title = "🧠 Morning Puzzle Time!";
            sendNotification(context, channelId, title, message);

        } else if ("evening".equals(type)) {
            message = EVENING_MESSAGES[new Random().nextInt(EVENING_MESSAGES.length)];
            title = "🧩 Evening Brain Teaser!";
            sendNotification(context, channelId, title, message);

        } else {
            title = "📢 Special Alert!";
            message = "A special puzzle challenge is waiting for you!";

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Notification");
            String finalTitle = title;
            String finalMessage = message;

            ref.get().addOnCompleteListener(task -> {
                String fetchedTitle = finalTitle;
                String fetchedMessage = finalMessage;

                if (task.isSuccessful() && task.getResult() != null) {
                    String firebaseTitle = task.getResult().child("title").getValue(String.class);
                    String firebaseMessage = task.getResult().child("message").getValue(String.class);

                    if (firebaseTitle != null && !firebaseTitle.trim().isEmpty()) {
                        fetchedTitle = firebaseTitle;
                    }
                    if (firebaseMessage != null && !firebaseMessage.trim().isEmpty()) {
                        fetchedMessage = firebaseMessage;
                    }
                }
                sendNotification(context, channelId, fetchedTitle, fetchedMessage);
            });

            return; // async call handles sending
        }

        // Save last sent time
        saveLastNotificationTime(context, type);
    }

    private void sendNotification(Context context, String channelId, String title, String message) {
        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_hint)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(new Random().nextInt(9999), builder.build());
        } catch (SecurityException ignored) {
        }
    }

    private void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel dailyChannel = new NotificationChannel(
                    CHANNEL_DAILY,
                    "Daily Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            dailyChannel.setDescription("Daily morning and evening riddle reminders");

            NotificationChannel alertChannel = new NotificationChannel(
                    CHANNEL_ALERTS,
                    "Special Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("Occasional important updates and challenges");

            if (manager != null) {
                manager.createNotificationChannel(dailyChannel);
                manager.createNotificationChannel(alertChannel);
            }
        }
    }

    private boolean isTooSoon(Context context, String type) {
        SharedPreferences prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
        long lastTime = prefs.getLong("last_" + type, 0);
        long now = System.currentTimeMillis();

        return now - lastTime < 6 * 60 * 60 * 1000; // 6 hours
    }

    private void saveLastNotificationTime(Context context, String type) {
        SharedPreferences prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
        prefs.edit().putLong("last_" + type, System.currentTimeMillis()).apply();
    }
}
