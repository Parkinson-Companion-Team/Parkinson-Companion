package com.susmita.parkicompanion.medicine.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.susmita.parkicompanion.medicine.db.MedicineDao;
import com.susmita.parkicompanion.medicine.db.MedicineDatabase;
import com.susmita.parkicompanion.medicine.model.Medicine;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MedicineRepository {

    private final MedicineDao medicineDao;
    private final LiveData<List<Medicine>> allMedicines;
    private final ExecutorService executorService;

    public interface OnMedicineInsertedListener {
        void onInserted(long newId);
    }

    public MedicineRepository(Application application) {
        MedicineDatabase db = MedicineDatabase.getInstance(application);
        medicineDao = db.medicineDao();
        allMedicines = medicineDao.getAllMedicines();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Medicine>> getAllMedicines() {
        return allMedicines;
    }

    public void insert(Medicine medicine, OnMedicineInsertedListener listener) {
        executorService.execute(() -> {
            long id = medicineDao.insert(medicine);
            if (listener != null) {
                listener.onInserted(id);
            }
        });
    }

    public void update(Medicine medicine) {
        executorService.execute(() -> medicineDao.update(medicine));
    }

    public void delete(Medicine medicine) {
        executorService.execute(() -> medicineDao.delete(medicine));
    }
}
