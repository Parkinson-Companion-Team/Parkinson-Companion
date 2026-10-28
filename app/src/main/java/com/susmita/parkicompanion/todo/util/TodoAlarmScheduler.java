package com.susmita.parkicompanion.todo.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.susmita.parkicompanion.todo.model.Todo;
import com.susmita.parkicompanion.todo.receiver.TodoAlarmReceiver;

import java.util.Calendar;

public class TodoAlarmScheduler {

    public static void scheduleAlarm(Context context, Todo todo) {
        if (!todo.isReminderSet()) {
            return;
        }

        long triggerTime = calculateNextTriggerTime(todo);
        if (triggerTime <= System.currentTimeMillis()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, TodoAlarmReceiver.class);
        intent.putExtra(TodoAlarmReceiver.EXTRA_TODO_ID, todo.getId());
        intent.putExtra(TodoAlarmReceiver.EXTRA_TODO_TITLE, todo.getTitle());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                todo.getId(),
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

    public static long calculateNextTriggerTime(Todo todo) {
        String type = todo.getRepeatType();
        if ("SPECIFIC_DATE".equals(type)) {
            return todo.getReminderTime();
        }

        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, todo.getHour());
        next.set(Calendar.MINUTE, todo.getMinute());
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        if ("EVERYDAY".equals(type)) {
            if (next.getTimeInMillis() <= now.getTimeInMillis()) {
                next.add(Calendar.DAY_OF_YEAR, 1);
            }
            return next.getTimeInMillis();
        }

        if ("CUSTOM_DAYS".equals(type)) {
            String days = todo.getRepeatDays();
            if (days == null || days.trim().isEmpty()) {
                if (next.getTimeInMillis() <= now.getTimeInMillis()) {
                    next.add(Calendar.DAY_OF_YEAR, 1);
                }
                return next.getTimeInMillis();
            }

            for (int i = 0; i < 7; i++) {
                if (next.getTimeInMillis() > now.getTimeInMillis() && isDaySelected(next, days)) {
                    return next.getTimeInMillis();
                }
                next.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        return next.getTimeInMillis();
    }

    private static boolean isDaySelected(Calendar cal, String repeatDays) {
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String dayName = "";
        switch (dayOfWeek) {
            case Calendar.SUNDAY: dayName = "Sun"; break;
            case Calendar.MONDAY: dayName = "Mon"; break;
            case Calendar.TUESDAY: dayName = "Tue"; break;
            case Calendar.WEDNESDAY: dayName = "Wed"; break;
            case Calendar.THURSDAY: dayName = "Thu"; break;
            case Calendar.FRIDAY: dayName = "Fri"; break;
            case Calendar.SATURDAY: dayName = "Sat"; break;
        }
        return repeatDays.contains(dayName);
    }

    public static void cancelAlarm(Context context, Todo todo) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, TodoAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                todo.getId(),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }
}
