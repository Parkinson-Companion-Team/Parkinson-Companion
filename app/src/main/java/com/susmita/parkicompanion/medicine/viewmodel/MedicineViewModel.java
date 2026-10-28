package com.susmita.parkicompanion.medicine.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.susmita.parkicompanion.medicine.model.Medicine;
import com.susmita.parkicompanion.medicine.repository.MedicineRepository;

import java.util.List;

public class MedicineViewModel extends AndroidViewModel {

    private final MedicineRepository repository;
    private final LiveData<List<Medicine>> allMedicines;

    public MedicineViewModel(@NonNull Application application) {
        super(application);
        repository = new MedicineRepository(application);
        allMedicines = repository.getAllMedicines();
    }

    public LiveData<List<Medicine>> getAllMedicines() {
        return allMedicines;
    }

    public void insert(Medicine medicine, MedicineRepository.OnMedicineInsertedListener listener) {
        repository.insert(medicine, listener);
    }

    public void update(Medicine medicine) {
        repository.update(medicine);
    }

    public void delete(Medicine medicine) {
        repository.delete(medicine);
    }
}
