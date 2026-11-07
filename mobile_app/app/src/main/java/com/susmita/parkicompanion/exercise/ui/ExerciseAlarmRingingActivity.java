package com.susmita.parkicompanion.exercise.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.exercise.receiver.ExerciseAlarmReceiver;

public class ExerciseAlarmRingingActivity extends AppCompatActivity {

    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        setContentView(R.layout.activity_exercise_alarm_ringing);

        TextView tvRingingTime = findViewById(R.id.tvRingingTime);
        TextView tvRingingLabel = findViewById(R.id.tvRingingLabel);
        Button btnStartExercise = findViewById(R.id.btnStartExercise);
        Button btnSnooze = findViewById(R.id.btnSnooze);
        Button btnDismiss = findViewById(R.id.btnDismiss);

        Intent intent = getIntent();
        String label = intent.getStringExtra("alarm_label");
        String timeStr = intent.getStringExtra("alarm_time");

        if (label != null && !label.isEmpty()) {
            tvRingingLabel.setText("🏃 " + label);
        }
        if (timeStr != null && !timeStr.isEmpty()) {
            tvRingingTime.setText(timeStr);
        }

        playAlarmSound();

        btnStartExercise.setOnClickListener(v -> {
            stopAlarmSound();
            Intent openIntent = new Intent(this, ExerciseActivity.class);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(openIntent);
            finish();
        });

        btnSnooze.setOnClickListener(v -> {
            snoozeAlarm5Minutes(label != null ? label : "Parkinson's Exercise");
            finish();
        });

        btnDismiss.setOnClickListener(v -> {
            stopAlarmSound();
            finish();
        });
    }

    private void snoozeAlarm5Minutes(String label) {
        stopAlarmSound();

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent snoozeIntent = new Intent(this, ExerciseAlarmReceiver.class);
        snoozeIntent.putExtra("alarm_label", label + " (Snoozed)");

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) System.currentTimeMillis(),
                snoozeIntent,
                pendingFlags
        );

        long triggerAtMillis = System.currentTimeMillis() + (5 * 60 * 1000L); // 5 Minutes Snooze

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            Toast.makeText(this, "⏰ Exercise Alarm snoozed for 5 minutes", Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            Toast.makeText(this, "⏰ Exercise Alarm snoozed for 5 minutes", Toast.LENGTH_LONG).show();
        }
    }

    private void playAlarmSound() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtone = RingtoneManager.getRingtone(this, alarmUri);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAlarmSound() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    @Override
    protected void onDestroy() {
        stopAlarmSound();
        super.onDestroy();
    }
}
