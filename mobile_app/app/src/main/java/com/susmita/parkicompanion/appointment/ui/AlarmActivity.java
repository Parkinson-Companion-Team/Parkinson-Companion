package com.susmita.parkicompanion.appointment.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.appointment.receiver.AlarmReceiver;

import java.io.File;

public class AlarmActivity extends AppCompatActivity {

    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        setContentView(R.layout.activity_alarm);

        ImageView ivAlarmMedicinePic = findViewById(R.id.ivAlarmMedicinePic);
        TextView tvAlarmTitle = findViewById(R.id.tvAlarmTitle);
        TextView tvAlarmDoctor = findViewById(R.id.tvAlarmDoctor);
        TextView tvAlarmLocation = findViewById(R.id.tvAlarmLocation);
        TextView tvAlarmNotes = findViewById(R.id.tvAlarmNotes);
        Button btnSnooze = findViewById(R.id.btnSnooze);
        Button btnDismiss = findViewById(R.id.btnDismiss);

        Intent intent = getIntent();
        int appointmentId = intent.getIntExtra(AlarmReceiver.EXTRA_APPOINTMENT_ID, -1);
        String title = intent.getStringExtra(AlarmReceiver.EXTRA_APPOINTMENT_TITLE);
        String doctorName = intent.getStringExtra(AlarmReceiver.EXTRA_DOCTOR_NAME);
        String location = intent.getStringExtra(AlarmReceiver.EXTRA_LOCATION);
        String notes = intent.getStringExtra(AlarmReceiver.EXTRA_NOTES);
        String imageUriStr = intent.getStringExtra("extra_medicine_image_uri");

        if (title != null) tvAlarmTitle.setText(title);

        boolean isMedicine = title != null && title.contains("Medicine");
        boolean isTodo = title != null && (title.contains("To-Do") || title.contains("Task"));

        if (isMedicine || isTodo) {
            if (doctorName != null && !doctorName.trim().isEmpty()) {
                tvAlarmDoctor.setText(doctorName);
                tvAlarmDoctor.setVisibility(View.VISIBLE);
            } else {
                tvAlarmDoctor.setVisibility(View.GONE);
            }

            if (location != null && !location.trim().isEmpty()) {
                tvAlarmLocation.setText(location);
                tvAlarmLocation.setVisibility(View.VISIBLE);
            } else {
                tvAlarmLocation.setVisibility(View.GONE);
            }
        } else {
            tvAlarmDoctor.setText(doctorName != null && !doctorName.trim().isEmpty() ? "Doctor: " + doctorName : "Doctor: N/A");
            tvAlarmLocation.setText(location != null && !location.trim().isEmpty() ? "Location: " + location : "Location: N/A");
            tvAlarmDoctor.setVisibility(View.VISIBLE);
            tvAlarmLocation.setVisibility(View.VISIBLE);
        }

        if (notes != null && !notes.trim().isEmpty()) {
            tvAlarmNotes.setText("Notes: " + notes);
            tvAlarmNotes.setVisibility(View.VISIBLE);
        } else {
            tvAlarmNotes.setVisibility(View.GONE);
        }

        if (imageUriStr != null && !imageUriStr.trim().isEmpty()) {
            try {
                File imgFile = new File(imageUriStr);
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    if (bitmap != null) {
                        ivAlarmMedicinePic.setImageBitmap(bitmap);
                        ivAlarmMedicinePic.setVisibility(View.VISIBLE);
                    }
                } else {
                    Uri uri = Uri.parse(imageUriStr);
                    ivAlarmMedicinePic.setImageURI(uri);
                    ivAlarmMedicinePic.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                ivAlarmMedicinePic.setVisibility(View.GONE);
            }
        } else {
            ivAlarmMedicinePic.setVisibility(View.GONE);
        }

        playRingtone();

        btnSnooze.setOnClickListener(v -> {
            snoozeAlarm(appointmentId, title, doctorName, location, notes);
            stopRingtoneAndFinish();
        });

        btnDismiss.setOnClickListener(v -> stopRingtoneAndFinish());
    }

    private void playRingtone() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
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

    private void snoozeAlarm(int appointmentId, String title, String doctorName, String location, String notes) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent snoozeIntent = new Intent(this, AlarmReceiver.class);
        snoozeIntent.putExtra(AlarmReceiver.EXTRA_APPOINTMENT_ID, appointmentId);
        snoozeIntent.putExtra(AlarmReceiver.EXTRA_APPOINTMENT_TITLE, title);
        snoozeIntent.putExtra(AlarmReceiver.EXTRA_DOCTOR_NAME, doctorName);
        snoozeIntent.putExtra(AlarmReceiver.EXTRA_LOCATION, location);
        snoozeIntent.putExtra(AlarmReceiver.EXTRA_NOTES, notes);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                appointmentId > 0 ? appointmentId + 9999 : 9999,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerAtMillis = System.currentTimeMillis() + (5 * 60 * 1000L); // 5 minutes

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            Toast.makeText(this, "Alarm snoozed for 5 minutes", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            Toast.makeText(this, "Alarm snoozed for 5 minutes", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRingtoneAndFinish() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }
}