package com.susmita.parkicompanion.appointment.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.susmita.parkicompanion.appointment.db.AppointmentDao;
import com.susmita.parkicompanion.appointment.db.AppointmentDatabase;
import com.susmita.parkicompanion.appointment.model.Appointment;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppointmentRepository {

    private final AppointmentDao appointmentDao;
    private final LiveData<List<Appointment>> allAppointments;
    private final ExecutorService executorService;

    public interface OnInsertListener {
        void onInserted(long newId);
    }

    public AppointmentRepository(Context context) {
        AppointmentDatabase db = AppointmentDatabase.getInstance(context);
        appointmentDao = db.appointmentDao();
        allAppointments = appointmentDao.getAllAppointments();
        executorService = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<Appointment>> getAllAppointments() {
        return allAppointments;
    }

    public void insert(Appointment appointment, OnInsertListener listener) {
        executorService.execute(() -> {
            long id = appointmentDao.insert(appointment);
            if (listener != null) {
                listener.onInserted(id);
            }
        });
    }

    public void update(Appointment appointment) {
        executorService.execute(() -> appointmentDao.update(appointment));
    }

    public void delete(Appointment appointment) {
        executorService.execute(() -> appointmentDao.delete(appointment));
    }
}