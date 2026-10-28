package com.susmita.parkicompanion.medicine.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.susmita.parkicompanion.medicine.model.Medicine;

@Database(entities = {Medicine.class}, version = 2, exportSchema = false)
public abstract class MedicineDatabase extends RoomDatabase {

    private static volatile MedicineDatabase INSTANCE;

    public abstract MedicineDao medicineDao();

    public static MedicineDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MedicineDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            MedicineDatabase.class,
                            "medicine_database"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
