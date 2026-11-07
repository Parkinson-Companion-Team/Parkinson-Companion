package com.susmita.parkicompanion.todo.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.todo.model.Todo;
import com.susmita.parkicompanion.todo.util.TodoAlarmScheduler;
import com.susmita.parkicompanion.todo.viewmodel.TodoViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddEditTodoActivity extends AppCompatActivity {

    public static final String EXTRA_TODO = "extra_todo";

    private EditText etTaskTitle;
    private ImageButton btnVoiceInput;
    private SwitchMaterial switchReminder;
    private LinearLayout layoutAlarmOptions, layoutDatePicker, layoutCustomDays;
    private RadioGroup rgRepeatType;
    private RadioButton rbSpecificDate, rbEveryday, rbCustomDays;
    private Button btnPickDate, btnPickTime, btnSaveTodo;
    private CheckBox cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;
    private TextView tvSelectedDateTime, tvHeaderTitle;

    private TodoViewModel viewModel;
    private Todo existingTodo;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    private ActivityResultLauncher<Intent> speechResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_todo);

        initViews();

        viewModel = new ViewModelProvider(this).get(TodoViewModel.class);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        // Speech Recognizer Launcher
        speechResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenText = matches.get(0);
                            String currentText = etTaskTitle.getText().toString().trim();
                            if (currentText.isEmpty()) {
                                etTaskTitle.setText(spokenText);
                            } else {
                                etTaskTitle.setText(currentText + " " + spokenText);
                            }
                            etTaskTitle.setSelection(etTaskTitle.getText().length());
                        }
                    }
                }
        );

        btnVoiceInput.setOnClickListener(v -> startVoiceInput());

        // Default time: next hour
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (getIntent().hasExtra(EXTRA_TODO)) {
            existingTodo = (Todo) getIntent().getSerializableExtra(EXTRA_TODO);
        }

        if (existingTodo != null) {
            tvHeaderTitle.setText("Edit Task");
            etTaskTitle.setText(existingTodo.getTitle());
            switchReminder.setChecked(existingTodo.isReminderSet());

            if (existingTodo.isReminderSet()) {
                String type = existingTodo.getRepeatType();
                if ("EVERYDAY".equals(type)) {
                    rbEveryday.setChecked(true);
                } else if ("CUSTOM_DAYS".equals(type)) {
                    rbCustomDays.setChecked(true);
                    setSelectedRepeatDays(existingTodo.getRepeatDays());
                } else {
                    rbSpecificDate.setChecked(true);
                    if (existingTodo.getReminderTime() > 0) {
                        calendar.setTimeInMillis(existingTodo.getReminderTime());
                    }
                }

                calendar.set(Calendar.HOUR_OF_DAY, existingTodo.getHour());
                calendar.set(Calendar.MINUTE, existingTodo.getMinute());
            }
        } else {
            tvHeaderTitle.setText("Add Task");
        }

        updateAlarmUIState();
        updateDateTimeLabel();

        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> updateAlarmUIState());

        rgRepeatType.setOnCheckedChangeListener((group, checkedId) -> {
            updateAlarmUIState();
            updateDateTimeLabel();
        });

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickTime.setOnClickListener(v -> showTimePicker());

        btnSaveTodo.setOnClickListener(v -> saveTask());
    }

    private void initViews() {
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        etTaskTitle = findViewById(R.id.etTaskTitle);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        switchReminder = findViewById(R.id.switchReminder);
        layoutAlarmOptions = findViewById(R.id.layoutAlarmOptions);
        layoutDatePicker = findViewById(R.id.layoutDatePicker);
        layoutCustomDays = findViewById(R.id.layoutCustomDays);
        rgRepeatType = findViewById(R.id.rgRepeatType);
        rbSpecificDate = findViewById(R.id.rbSpecificDate);
        rbEveryday = findViewById(R.id.rbEveryday);
        rbCustomDays = findViewById(R.id.rbCustomDays);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);
        btnSaveTodo = findViewById(R.id.btnSaveTodo);
        tvSelectedDateTime = findViewById(R.id.tvSelectedDateTime);

        cbMon = findViewById(R.id.cbMon);
        cbTue = findViewById(R.id.cbTue);
        cbWed = findViewById(R.id.cbWed);
        cbThu = findViewById(R.id.cbThu);
        cbFri = findViewById(R.id.cbFri);
        cbSat = findViewById(R.id.cbSat);
        cbSun = findViewById(R.id.cbSun);
    }

    private void updateAlarmUIState() {
        boolean isReminder = switchReminder.isChecked();
        layoutAlarmOptions.setVisibility(isReminder ? View.VISIBLE : View.GONE);

        if (isReminder) {
            if (rbSpecificDate.isChecked()) {
                layoutDatePicker.setVisibility(View.VISIBLE);
                layoutCustomDays.setVisibility(View.GONE);
            } else if (rbCustomDays.isChecked()) {
                layoutDatePicker.setVisibility(View.GONE);
                layoutCustomDays.setVisibility(View.VISIBLE);
            } else { // Everyday
                layoutDatePicker.setVisibility(View.GONE);
                layoutCustomDays.setVisibility(View.GONE);
            }
        }
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your task...");

        try {
            speechResultLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Voice recognition is not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePicker() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year1);
                    calendar.set(Calendar.MONTH, month1);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeLabel();
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute1) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute1);
                    updateDateTimeLabel();
                },
                hour, minute, false
        );
        timePickerDialog.show();
    }

    private void updateDateTimeLabel() {
        String formattedTime = timeFormat.format(calendar.getTime());
        if (rbSpecificDate.isChecked()) {
            String formattedDate = dateFormat.format(calendar.getTime());
            tvSelectedDateTime.setText("Alarm set for:\n" + formattedDate + " at " + formattedTime);
        } else if (rbEveryday.isChecked()) {
            tvSelectedDateTime.setText("Alarm set for:\nEveryday at " + formattedTime);
        } else if (rbCustomDays.isChecked()) {
            String days = getSelectedRepeatDays();
            tvSelectedDateTime.setText("Alarm set for:\n" + (days.isEmpty() ? "Selected days" : days) + " at " + formattedTime);
        }
    }

    private String getSelectedRepeatDays() {
        List<String> days = new ArrayList<>();
        if (cbMon.isChecked()) days.add("Mon");
        if (cbTue.isChecked()) days.add("Tue");
        if (cbWed.isChecked()) days.add("Wed");
        if (cbThu.isChecked()) days.add("Thu");
        if (cbFri.isChecked()) days.add("Fri");
        if (cbSat.isChecked()) days.add("Sat");
        if (cbSun.isChecked()) days.add("Sun");
        return String.join(", ", days);
    }

    private void setSelectedRepeatDays(String daysStr) {
        if (daysStr == null) return;
        cbMon.setChecked(daysStr.contains("Mon"));
        cbTue.setChecked(daysStr.contains("Tue"));
        cbWed.setChecked(daysStr.contains("Wed"));
        cbThu.setChecked(daysStr.contains("Thu"));
        cbFri.setChecked(daysStr.contains("Fri"));
        cbSat.setChecked(daysStr.contains("Sat"));
        cbSun.setChecked(daysStr.contains("Sun"));
    }

    private void saveTask() {
        String title = etTaskTitle.getText().toString().trim();
        if (title.isEmpty()) {
            etTaskTitle.setError("Please enter task details");
            etTaskTitle.requestFocus();
            return;
        }

        boolean isReminderSet = switchReminder.isChecked();
        String repeatType = "SPECIFIC_DATE";
        String repeatDays = "";
        long reminderTime = 0L;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        if (isReminderSet) {
            if (rbEveryday.isChecked()) {
                repeatType = "EVERYDAY";
            } else if (rbCustomDays.isChecked()) {
                repeatType = "CUSTOM_DAYS";
                repeatDays = getSelectedRepeatDays();
                if (repeatDays.isEmpty()) {
                    Toast.makeText(this, "Please select at least one day for custom alarm!", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                repeatType = "SPECIFIC_DATE";
                reminderTime = calendar.getTimeInMillis();
                if (reminderTime <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Reminder date & time must be in the future!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        if (existingTodo != null) {
            existingTodo.setTitle(title);
            existingTodo.setReminderSet(isReminderSet);
            existingTodo.setRepeatType(repeatType);
            existingTodo.setRepeatDays(repeatDays);
            existingTodo.setReminderTime(reminderTime);
            existingTodo.setHour(hour);
            existingTodo.setMinute(minute);

            viewModel.update(existingTodo);

            if (isReminderSet) {
                TodoAlarmScheduler.scheduleAlarm(this, existingTodo);
            } else {
                TodoAlarmScheduler.cancelAlarm(this, existingTodo);
            }

            Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Todo newTodo = new Todo(title, false, isReminderSet, repeatType, repeatDays, reminderTime, hour, minute);
            viewModel.insert(newTodo, id -> {
                newTodo.setId((int) id);
                if (isReminderSet) {
                    TodoAlarmScheduler.scheduleAlarm(AddEditTodoActivity.this, newTodo);
                }
                runOnUiThread(() -> {
                    Toast.makeText(AddEditTodoActivity.this, "Task saved", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        }
    }
}
