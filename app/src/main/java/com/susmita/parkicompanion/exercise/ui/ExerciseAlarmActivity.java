package com.susmita.parkicompanion.exercise.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.exercise.model.ExerciseAlarm;
import com.susmita.parkicompanion.exercise.receiver.ExerciseAlarmReceiver;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class ExerciseAlarmActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ParkyExerciseAlarmPrefs";
    private static final String PREF_ALARMS_JSON = "exercise_alarms_list";

    private View btnBack;
    private Button btnAddAlarm;
    private RecyclerView rvAlarms;
    private LinearLayout layoutEmptyState;

    private List<ExerciseAlarm> alarmList = new ArrayList<>();
    private ExerciseAlarmAdapter adapter;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_alarm);

        gson = new Gson();

        initViews();
        loadAlarms();
        setupRecyclerView();

        btnBack.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        btnAddAlarm.setOnClickListener(v -> openClockTimePicker(null, -1));
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnAddAlarm = findViewById(R.id.btnAddAlarm);
        rvAlarms = findViewById(R.id.rvAlarms);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
    }

    private void setupRecyclerView() {
        rvAlarms.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExerciseAlarmAdapter(this, alarmList, new ExerciseAlarmAdapter.OnAlarmActionListener() {
            @Override
            public void onToggleAlarm(ExerciseAlarm alarm, boolean isEnabled) {
                if (isEnabled) {
                    scheduleAlarmSystem(alarm);
                    Toast.makeText(ExerciseAlarmActivity.this, "Alarm " + alarm.getFormattedTime() + " Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    cancelAlarmSystem(alarm);
                    Toast.makeText(ExerciseAlarmActivity.this, "Alarm " + alarm.getFormattedTime() + " Disabled", Toast.LENGTH_SHORT).show();
                }
                saveAlarms();
            }

            @Override
            public void onDeleteAlarm(ExerciseAlarm alarm, int position) {
                cancelAlarmSystem(alarm);
                alarmList.remove(position);
                adapter.notifyItemRemoved(position);
                saveAlarms();
                updateEmptyState();
                Toast.makeText(ExerciseAlarmActivity.this, "Alarm Deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEditAlarm(ExerciseAlarm alarm, int position) {
                openClockTimePicker(alarm, position);
            }
        });
        rvAlarms.setAdapter(adapter);
    }

    private void openClockTimePicker(ExerciseAlarm existingAlarm, int position) {
        Calendar now = Calendar.getInstance();
        int initialHour = existingAlarm != null ? existingAlarm.getHour() : now.get(Calendar.HOUR_OF_DAY);
        int initialMinute = existingAlarm != null ? existingAlarm.getMinute() : now.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> openCustomDaysDialog(existingAlarm, position, hourOfDay, minute),
                initialHour,
                initialMinute,
                false // 12-hour format with AM/PM clock selector
        );
        timePickerDialog.setTitle(existingAlarm != null ? "✏️ Edit Exercise Alarm Time" : "⏰ Choose Exercise Alarm Time");
        timePickerDialog.show();
    }

    private void openCustomDaysDialog(ExerciseAlarm existingAlarm, int position, int hour, int minute) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(existingAlarm != null ? "✏️ Edit Days & Alarm Name" : "📅 Customize Days & Alarm Name");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 16, 32, 16);

        final EditText etLabel = new EditText(this);
        etLabel.setHint("Exercise Label (e.g. Leg Exercise)");
        etLabel.setText(existingAlarm != null ? existingAlarm.getLabel() : "Parkinson's Exercise");
        container.addView(etLabel);

        final String[] dayNames = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        final boolean[] checkedDays = existingAlarm != null && existingAlarm.getRepeatDays() != null ?
                existingAlarm.getRepeatDays().clone() :
                new boolean[]{true, true, true, true, true, true, true};

        builder.setView(container);

        builder.setMultiChoiceItems(dayNames, checkedDays, (dialog, which, isChecked) -> {
            checkedDays[which] = isChecked;
        });

        String buttonText = existingAlarm != null ? "Save Changes" : "Set Alarm";

        builder.setPositiveButton(buttonText, (dialog, which) -> {
            String label = etLabel.getText().toString().trim();
            if (label.isEmpty()) label = "Parkinson's Exercise";

            if (existingAlarm != null && position >= 0) {
                // Cancel old alarm schedule
                cancelAlarmSystem(existingAlarm);

                existingAlarm.setHour(hour);
                existingAlarm.setMinute(minute);
                existingAlarm.setLabel(label);
                existingAlarm.setRepeatDays(checkedDays.clone());

                scheduleAlarmSystem(existingAlarm);
                adapter.notifyItemChanged(position);
                Toast.makeText(this, "✏️ Alarm updated for " + existingAlarm.getFormattedTime() + " (" + existingAlarm.getFormattedDays() + ")", Toast.LENGTH_LONG).show();
            } else {
                String id = UUID.randomUUID().toString();
                ExerciseAlarm newAlarm = new ExerciseAlarm(id, hour, minute, label, checkedDays.clone());

                alarmList.add(newAlarm);
                scheduleAlarmSystem(newAlarm);
                adapter.notifyItemInserted(alarmList.size() - 1);
                Toast.makeText(this, "⏰ Exercise Alarm set for " + newAlarm.getFormattedTime() + " (" + newAlarm.getFormattedDays() + ")", Toast.LENGTH_LONG).show();
            }

            saveAlarms();
            updateEmptyState();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void loadAlarms() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(PREF_ALARMS_JSON, null);
        alarmList.clear();
        if (json != null && !json.isEmpty()) {
            try {
                Type type = new TypeToken<ArrayList<ExerciseAlarm>>() {}.getType();
                List<ExerciseAlarm> saved = gson.fromJson(json, type);
                if (saved != null) {
                    alarmList.addAll(saved);
                }
            } catch (Exception ignored) {
            }
        }
        updateEmptyState();
    }

    private void saveAlarms() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = gson.toJson(alarmList);
        prefs.edit().putString(PREF_ALARMS_JSON, json).apply();
    }

    private void updateEmptyState() {
        if (alarmList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvAlarms.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvAlarms.setVisibility(View.VISIBLE);
        }
    }

    private void scheduleAlarmSystem(ExerciseAlarm alarm) {
        if (!alarm.isEnabled()) return;

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        target.set(Calendar.MINUTE, alarm.getMinute());
        target.set(Calendar.SECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(this, ExerciseAlarmReceiver.class);
        intent.putExtra("alarm_id", alarm.getId());
        intent.putExtra("alarm_label", alarm.getLabel());
        intent.putExtra("alarm_time", alarm.getFormattedTime());

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                alarm.getId().hashCode(),
                intent,
                pendingFlags
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pendingIntent);
            }
        } catch (SecurityException e) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pendingIntent);
        }
    }

    private void cancelAlarmSystem(ExerciseAlarm alarm) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, ExerciseAlarmReceiver.class);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                alarm.getId().hashCode(),
                intent,
                pendingFlags
        );

        alarmManager.cancel(pendingIntent);
    }
}
