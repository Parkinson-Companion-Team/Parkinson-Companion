package com.susmita.parkicompanion.todo.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.appointment.receiver.AlarmReceiver;
import com.susmita.parkicompanion.appointment.ui.AlarmActivity;
import com.susmita.parkicompanion.todo.db.TodoDatabase;
import com.susmita.parkicompanion.todo.model.Todo;
import com.susmita.parkicompanion.todo.ui.TodoListActivity;
import com.susmita.parkicompanion.todo.util.TodoAlarmScheduler;

import java.util.concurrent.Executors;

public class TodoAlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "todo_alarm_channel";
    public static final String EXTRA_TODO_ID = "extra_todo_id";
    public static final String EXTRA_TODO_TITLE = "extra_todo_title";

    @Override
    public void onReceive(Context context, Intent intent) {
        int todoId = intent.getIntExtra(EXTRA_TODO_ID, -1);
        String title = intent.getStringExtra(EXTRA_TODO_TITLE);
        if (title == null || title.trim().isEmpty()) {
            title = "Task Reminder";
        }

        // 1. Launch full screen AlarmActivity so full alarm screen turns on and plays ringtone
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.putExtra(AlarmReceiver.EXTRA_APPOINTMENT_ID, todoId);
        alarmIntent.putExtra(AlarmReceiver.EXTRA_APPOINTMENT_TITLE, "⏰ To-Do Alarm: " + title);
        alarmIntent.putExtra(AlarmReceiver.EXTRA_DOCTOR_NAME, "Task: " + title);
        alarmIntent.putExtra(AlarmReceiver.EXTRA_LOCATION, "To-Do Task Reminder");
        alarmIntent.putExtra(AlarmReceiver.EXTRA_NOTES, "");
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            context.startActivity(alarmIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Also trigger a high-priority Notification with full alarm sound
        showNotification(context, todoId, title, alarmIntent);

        // 3. Reschedule next alarm for repeating tasks (EVERYDAY or CUSTOM_DAYS)
        if (todoId > 0) {
            Executors.newSingleThreadExecutor().execute(() -> {
                Todo todo = TodoDatabase.getInstance(context).todoDao().getTodoById(todoId);
                if (todo != null && todo.isReminderSet()) {
                    String type = todo.getRepeatType();
                    if ("EVERYDAY".equals(type) || "CUSTOM_DAYS".equals(type)) {
                        TodoAlarmScheduler.scheduleAlarm(context, todo);
                    }
                }
            });
        }
    }

    private void showNotification(Context context, int todoId, String title, Intent fullScreenIntent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (soundUri == null) {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "To-Do Task Alarms",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Full alarm sound and notifications for scheduled To-Do tasks");
            channel.enableVibration(true);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            channel.setSound(soundUri, audioAttributes);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent contentIntent = new Intent(context, TodoListActivity.class);
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                todoId > 0 ? todoId : (int) System.currentTimeMillis(),
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                todoId > 0 ? todoId + 20000 : (int) System.currentTimeMillis() + 20000,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("⏰ To-Do Alarm: " + title)
                .setContentText("Task reminder: " + title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Time for your task: " + title))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(soundUri)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify(todoId > 0 ? todoId : (int) System.currentTimeMillis(), builder.build());
        }
    }
}
