package com.susmita.parkicompanion.appointment.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.appointment.model.Appointment;
import com.susmita.parkicompanion.appointment.util.AlarmScheduler;
import com.susmita.parkicompanion.appointment.viewmodel.AppointmentViewModel;

public class AppointmentListActivity extends AppCompatActivity implements AppointmentAdapter.OnAppointmentClickListener {

    private AppointmentViewModel viewModel;
    private AppointmentAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_list);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnAddAppointment = findViewById(R.id.btnAddAppointment);
        RecyclerView rvAppointments = findViewById(R.id.rvAppointments);
        tvEmpty = findViewById(R.id.tvEmpty);

        btnBack.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        adapter = new AppointmentAdapter(this);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvAppointments.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AppointmentViewModel.class);
        viewModel.getAllAppointments().observe(this, appointments -> {
            if (appointments == null || appointments.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvAppointments.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvAppointments.setVisibility(View.VISIBLE);
                adapter.submitList(appointments);
            }
        });

        btnAddAppointment.setOnClickListener(v -> {
            Intent intent = new Intent(AppointmentListActivity.this, AddEditAppointmentActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onItemClick(Appointment appointment) {
        Intent intent = new Intent(this, AddEditAppointmentActivity.class);
        intent.putExtra(AddEditAppointmentActivity.EXTRA_APPOINTMENT, appointment);
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Appointment appointment) {
        AlarmScheduler.cancelAlarm(this, appointment);
        viewModel.delete(appointment);
        Toast.makeText(this, "Appointment removed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAlarmToggle(Appointment appointment, boolean isChecked) {
        appointment.setAlarmSet(isChecked);
        viewModel.update(appointment);

        if (isChecked) {
            AlarmScheduler.scheduleAlarm(this, appointment);
            Toast.makeText(this, "Alarm enabled", Toast.LENGTH_SHORT).show();
        } else {
            AlarmScheduler.cancelAlarm(this, appointment);
            Toast.makeText(this, "Alarm disabled", Toast.LENGTH_SHORT).show();
        }
    }
}