package com.susmita.parkicompanion.exercise.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.exercise.ui.ExerciseAlarmRingingActivity;

public class ExerciseAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "parky_exercise_alarm_channel";
    private static final String CHANNEL_NAME = "Parky Exercise Alarms";

    @Override
    public void onReceive(Context context, Intent intent) {
        String label = intent.getStringExtra("alarm_label");
        String timeStr = intent.getStringExtra("alarm_time");
        if (label == null || label.isEmpty()) {
            label = "Daily Parkinson's Exercise";
        }

        // 1. Launch Full-Screen Alarm Ringing Activity
        Intent ringingIntent = new Intent(context, ExerciseAlarmRingingActivity.class);
        ringingIntent.putExtra("alarm_label", label);
        ringingIntent.putExtra("alarm_time", timeStr);
        ringingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(ringingIntent);

        // 2. Also post high-priority Heads-up Notification
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for scheduled Parkinson's exercise alarms");
            channel.enableVibration(true);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                ringingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("🏃 Exercise Time: " + label)
                .setContentText("Time for your Parkinson's exercise routine. Tap to start!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
