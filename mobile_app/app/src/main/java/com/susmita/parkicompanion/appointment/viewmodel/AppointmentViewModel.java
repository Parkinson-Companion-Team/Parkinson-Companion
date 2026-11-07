package com.susmita.parkicompanion.appointment.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.susmita.parkicompanion.appointment.model.Appointment;
import com.susmita.parkicompanion.appointment.repository.AppointmentRepository;

import java.util.List;

public class AppointmentViewModel extends AndroidViewModel {

    private final AppointmentRepository repository;
    private final LiveData<List<Appointment>> allAppointments;

    public AppointmentViewModel(@NonNull Application application) {
        super(application);
        repository = new AppointmentRepository(application);
        allAppointments = repository.getAllAppointments();
    }

    public LiveData<List<Appointment>> getAllAppointments() {
        return allAppointments;
    }

    public void insert(Appointment appointment, AppointmentRepository.OnInsertListener listener) {
        repository.insert(appointment, listener);
    }

    public void update(Appointment appointment) {
        repository.update(appointment);
    }

    public void delete(Appointment appointment) {
        repository.delete(appointment);
    }
}