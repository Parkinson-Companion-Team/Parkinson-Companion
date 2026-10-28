package com.susmita.parkicompanion.appointment.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.appointment.ui.AlarmActivity;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "appointment_reminder_channel";
    public static final String EXTRA_APPOINTMENT_ID = "extra_appointment_id";
    public static final String EXTRA_APPOINTMENT_TITLE = "extra_appointment_title";
    public static final String EXTRA_DOCTOR_NAME = "extra_doctor_name";
    public static final String EXTRA_LOCATION = "extra_location";
    public static final String EXTRA_NOTES = "extra_notes";

    @Override
    public void onReceive(Context context, Intent intent) {
        int appointmentId = intent.getIntExtra(EXTRA_APPOINTMENT_ID, -1);
        String title = intent.getStringExtra(EXTRA_APPOINTMENT_TITLE);
        if (title == null) title = "Appointment Reminder";
        String doctorName = intent.getStringExtra(EXTRA_DOCTOR_NAME);
        String location = intent.getStringExtra(EXTRA_LOCATION);
        String notes = intent.getStringExtra(EXTRA_NOTES);

        // Intent for full screen AlarmActivity
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.putExtra(EXTRA_APPOINTMENT_ID, appointmentId);
        alarmIntent.putExtra(EXTRA_APPOINTMENT_TITLE, title);
        alarmIntent.putExtra(EXTRA_DOCTOR_NAME, doctorName);
        alarmIntent.putExtra(EXTRA_LOCATION, location);
        alarmIntent.putExtra(EXTRA_NOTES, notes);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        context.startActivity(alarmIntent);

        // Also trigger Notification
        showNotification(context, appointmentId, title, doctorName, alarmIntent);
    }

    private void showNotification(Context context, int notificationId, String title, String doctorName, Intent fullScreenIntent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Appointment Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for upcoming medical appointments");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(doctorName != null ? "Doctor: " + doctorName : "Upcoming Appointment")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }
}