package com.susmita.parkicompanion.appointment.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.susmita.parkicompanion.appointment.model.Appointment;

@Database(entities = {Appointment.class}, version = 2, exportSchema = false)
public abstract class AppointmentDatabase extends RoomDatabase {

    private static volatile AppointmentDatabase INSTANCE;

    public abstract AppointmentDao appointmentDao();

    public static AppointmentDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppointmentDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppointmentDatabase.class,
                            "appointment_database"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}