package com.susmita.parkicompanion.appointment.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.susmita.parkicompanion.appointment.db.AppointmentDatabase;
import com.susmita.parkicompanion.appointment.model.Appointment;
import com.susmita.parkicompanion.appointment.util.AlarmScheduler;
import com.susmita.parkicompanion.medicine.db.MedicineDatabase;
import com.susmita.parkicompanion.medicine.model.Medicine;
import com.susmita.parkicompanion.medicine.receiver.MedicineAlarmReceiver;

import java.util.List;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Executors.newSingleThreadExecutor().execute(() -> {
                // Reschedule Appointment Alarms
                try {
                    AppointmentDatabase db = AppointmentDatabase.getInstance(context);
                    List<Appointment> appointments = db.appointmentDao().getActiveAlarmAppointmentsSync();
                    long now = System.currentTimeMillis();

                    for (Appointment appointment : appointments) {
                        long triggerTime = appointment.getTimeInMillis() - (appointment.getReminderOffsetMinutes() * 60 * 1000L);
                        if (triggerTime > now) {
                            AlarmScheduler.scheduleAlarm(context, appointment);
                        } else {
                            appointment.setAlarmSet(false);
                            db.appointmentDao().update(appointment);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Reschedule Medicine Alarms
                try {
                    MedicineDatabase medDb = MedicineDatabase.getInstance(context);
                    List<Medicine> medicines = medDb.medicineDao().getEnabledMedicinesSync();
                    for (Medicine med : medicines) {
                        MedicineAlarmReceiver.scheduleMedicineAlarms(context, med);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Reschedule To-Do Alarms
                try {
                    com.susmita.parkicompanion.todo.db.TodoDatabase todoDb = com.susmita.parkicompanion.todo.db.TodoDatabase.getInstance(context);
                    List<com.susmita.parkicompanion.todo.model.Todo> todos = todoDb.todoDao().getActiveTodosWithReminderSync();
                    for (com.susmita.parkicompanion.todo.model.Todo todo : todos) {
                        com.susmita.parkicompanion.todo.util.TodoAlarmScheduler.scheduleAlarm(context, todo);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}