package com.susmita.parkicompanion.appointment.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.susmita.parkicompanion.appointment.model.Appointment;
import com.susmita.parkicompanion.appointment.receiver.AlarmReceiver;

public class AlarmScheduler {

    public static void scheduleAlarm(Context context, Appointment appointment) {
        long triggerTime = appointment.getTimeInMillis() - (appointment.getReminderOffsetMinutes() * 60 * 1000L);
        if (triggerTime <= System.currentTimeMillis()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_APPOINTMENT_ID, appointment.getId());
        intent.putExtra(AlarmReceiver.EXTRA_APPOINTMENT_TITLE, appointment.getTitle());
        intent.putExtra(AlarmReceiver.EXTRA_DOCTOR_NAME, appointment.getDoctorName());
        intent.putExtra(AlarmReceiver.EXTRA_LOCATION, appointment.getLocation());
        intent.putExtra(AlarmReceiver.EXTRA_NOTES, appointment.getNotes());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                appointment.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    public static void cancelAlarm(Context context, Appointment appointment) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                appointment.getId(),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }
}