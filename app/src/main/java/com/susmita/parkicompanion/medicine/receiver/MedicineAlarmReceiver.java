package com.susmita.parkicompanion.medicine.receiver;

import android.app.AlarmManager;
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
import com.susmita.parkicompanion.medicine.db.MedicineDatabase;
import com.susmita.parkicompanion.medicine.model.Medicine;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MedicineAlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "medicine_reminder_channel";
    public static final String EXTRA_MEDICINE_ID = "extra_medicine_id";
    public static final String EXTRA_MEDICINE_NAME = "extra_medicine_name";
    public static final String EXTRA_MEDICINE_DOSAGE = "extra_medicine_dosage";
    public static final String EXTRA_MEDICINE_INSTRUCTION = "extra_medicine_instruction";
    public static final String EXTRA_MEDICINE_NOTES = "extra_medicine_notes";
    public static final String EXTRA_MEDICINE_IMAGE_URI = "extra_medicine_image_uri";

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicineId = intent.getIntExtra(EXTRA_MEDICINE_ID, -1);
        String medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME);
        if (medicineName == null) medicineName = "Medicine Reminder";
        String dosage = intent.getStringExtra(EXTRA_MEDICINE_DOSAGE);
        if (dosage == null) dosage = "Take your scheduled medicine";
        String instruction = intent.getStringExtra(EXTRA_MEDICINE_INSTRUCTION);
        String notes = intent.getStringExtra(EXTRA_MEDICINE_NOTES);
        String imageUri = intent.getStringExtra(EXTRA_MEDICINE_IMAGE_URI);

        // 1. Launch AlarmActivity so the full-screen alarm screen turns on and rings!
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.putExtra(AlarmReceiver.EXTRA_APPOINTMENT_ID, medicineId);
        alarmIntent.putExtra(AlarmReceiver.EXTRA_APPOINTMENT_TITLE, "💊 Medicine Reminder: " + medicineName);
        
        String doseDetail = "Dose: " + dosage;
        if (instruction != null && !instruction.trim().isEmpty()) {
            doseDetail += " (" + instruction + ")";
        }
        alarmIntent.putExtra(AlarmReceiver.EXTRA_DOCTOR_NAME, doseDetail);
        alarmIntent.putExtra(AlarmReceiver.EXTRA_LOCATION, instruction != null ? "Instruction: " + instruction : "");
        alarmIntent.putExtra(AlarmReceiver.EXTRA_NOTES, notes != null ? notes : "");
        alarmIntent.putExtra("extra_medicine_image_uri", imageUri);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            context.startActivity(alarmIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Also trigger a high priority Notification with alarm sound
        showNotification(context, medicineId, medicineName, dosage, instruction, alarmIntent);

        // 3. Reschedule repeating alarm for next cycle
        rescheduleNextAlarm(context, medicineId);
    }

    private void showNotification(Context context, int notificationId, String medicineName, String dosage, String instruction, Intent fullScreenIntent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (soundUri == null) {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Medicine Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("High priority notifications for medicine schedule alerts");
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

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId > 0 ? notificationId : (int) System.currentTimeMillis(),
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String subText = dosage + (instruction != null && !instruction.isEmpty() ? " • " + instruction : "");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("💊 Medicine Reminder: " + medicineName)
                .setContentText(subText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Time to take " + medicineName + "\n" + subText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(soundUri)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify(notificationId > 0 ? notificationId : 9090, builder.build());
        }
    }

    private void rescheduleNextAlarm(Context context, int medicineId) {
        if (medicineId <= 0) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                MedicineDatabase db = MedicineDatabase.getInstance(context);
                Medicine medicine = db.medicineDao().getMedicineById(medicineId);
                if (medicine != null && medicine.isEnabled()) {
                    scheduleMedicineAlarms(context, medicine);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void scheduleMedicineAlarms(Context context, Medicine medicine) {
        if (!medicine.isEnabled()) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        String timesStr = medicine.getTimes();
        if (timesStr == null || timesStr.trim().isEmpty()) return;

        String[] timesArray = timesStr.split(", ");
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);

        for (int i = 0; i < timesArray.length; i++) {
            String timeStr = timesArray[i].trim();
            try {
                Calendar parsedTime = Calendar.getInstance();
                parsedTime.setTime(sdf.parse(timeStr));

                Calendar alarmCal = Calendar.getInstance();
                alarmCal.set(Calendar.HOUR_OF_DAY, parsedTime.get(Calendar.HOUR_OF_DAY));
                alarmCal.set(Calendar.MINUTE, parsedTime.get(Calendar.MINUTE));
                alarmCal.set(Calendar.SECOND, 0);

                if (alarmCal.getTimeInMillis() <= System.currentTimeMillis()) {
                    alarmCal.add(Calendar.DAY_OF_YEAR, 1);
                }

                Intent intent = new Intent(context, MedicineAlarmReceiver.class);
                intent.putExtra(EXTRA_MEDICINE_ID, medicine.getId());
                intent.putExtra(EXTRA_MEDICINE_NAME, medicine.getName());
                intent.putExtra(EXTRA_MEDICINE_DOSAGE, medicine.getDosage());
                intent.putExtra(EXTRA_MEDICINE_INSTRUCTION, medicine.getInstruction());
                intent.putExtra(EXTRA_MEDICINE_NOTES, medicine.getNotes());
                intent.putExtra(EXTRA_MEDICINE_IMAGE_URI, medicine.getImageUri());

                int requestCode = medicine.getId() * 100 + i;
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pendingIntent);
                        } else {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pendingIntent);
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pendingIntent);
                    }
                } catch (SecurityException se) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pendingIntent);
                }
            } catch (Exception ignored) {
            }
        }
    }
}