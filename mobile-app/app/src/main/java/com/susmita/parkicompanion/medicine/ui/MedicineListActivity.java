package com.susmita.parkicompanion.medicine.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.medicine.model.Medicine;
import com.susmita.parkicompanion.medicine.receiver.MedicineAlarmReceiver;
import com.susmita.parkicompanion.medicine.viewmodel.MedicineViewModel;

public class MedicineListActivity extends AppCompatActivity {

    public static final String EXTRA_MEDICINE = "extra_medicine";

    private MedicineViewModel viewModel;
    private MedicineAdapter adapter;
    private View layoutEmptyState;

    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_list);

        setupNotificationPermissionLauncher();
        checkAndRequestNotificationPermission();

        Button btnBack = findViewById(R.id.btnMedicineListBack);
        btnBack.setOnClickListener(v -> finish());

        layoutEmptyState = findViewById(R.id.layoutEmptyMedicineState);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewMedicines);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddMedicine);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MedicineAdapter(new MedicineAdapter.OnMedicineClickListener() {
            @Override
            public void onItemClick(Medicine medicine) {
                Intent intent = new Intent(MedicineListActivity.this, AddEditMedicineActivity.class);
                intent.putExtra(EXTRA_MEDICINE, medicine);
                startActivity(intent);
            }

            @Override
            public void onToggleEnable(Medicine medicine, boolean isEnabled) {
                medicine.setEnabled(isEnabled);
                viewModel.update(medicine);
                if (isEnabled) {
                    MedicineAlarmReceiver.scheduleMedicineAlarms(MedicineListActivity.this, medicine);
                }
                String msg = isEnabled ? "Reminder turned ON for " + medicine.getName() : "Reminder turned OFF for " + medicine.getName();
                Toast.makeText(MedicineListActivity.this, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(Medicine medicine) {
                new AlertDialog.Builder(MedicineListActivity.this)
                        .setTitle("Delete Medicine")
                        .setMessage("Are you sure you want to delete " + medicine.getName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            viewModel.delete(medicine);
                            Toast.makeText(MedicineListActivity.this, medicine.getName() + " deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(MedicineViewModel.class);
        viewModel.getAllMedicines().observe(this, medicines -> {
            if (medicines == null || medicines.isEmpty()) {
                layoutEmptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                layoutEmptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setMedicines(medicines);
            }
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MedicineListActivity.this, AddEditMedicineActivity.class);
            startActivity(intent);
        });
    }

    private void setupNotificationPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(this, "Notification permission is needed for medicine alarms.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}