package com.susmita.parkicompanion.appointment.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.susmita.parkicompanion.appointment.model.Appointment;

import java.util.List;

@Dao
public interface AppointmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Appointment appointment);

    @Update
    void update(Appointment appointment);

    @Delete
    void delete(Appointment appointment);

    @Query("SELECT * FROM appointments ORDER BY timeInMillis ASC")
    LiveData<List<Appointment>> getAllAppointments();

    @Query("SELECT * FROM appointments WHERE isAlarmSet = 1")
    List<Appointment> getActiveAlarmAppointmentsSync();

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    Appointment getAppointmentByIdSync(int id);
}