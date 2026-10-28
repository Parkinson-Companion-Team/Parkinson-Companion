package com.susmita.parkicompanion.medicine.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.medicine.model.Medicine;
import com.susmita.parkicompanion.medicine.receiver.MedicineAlarmReceiver;
import com.susmita.parkicompanion.medicine.viewmodel.MedicineViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddEditMedicineActivity extends AppCompatActivity {

    private EditText etMedicineName, etDosage, etNotes;
    private RadioGroup rgInstruction;
    private RadioButton rbBeforeMeal, rbAfterMeal, rbWithMeal;
    private ImageView ivMedicinePicPreview;
    private Button btnTakeCameraPhoto, btnPickGalleryPhoto, btnRemovePhoto;

    private ChipGroup chipGroupTimes;
    private Button btnAddTime, btnSelectEveryday, btnPickSpecificDate, btnClearDate, btnSaveMedicine;
    private CheckBox cbSun, cbMon, cbTue, cbWed, cbThu, cbFri, cbSat;
    private TextView tvTitle;

    private final List<String> selectedTimesList = new ArrayList<>();
    private String selectedSpecificDate = "";
    private String currentPhotoPath = "";
    private Medicine existingMedicine = null;

    private MedicineViewModel viewModel;

    // Launchers
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    private ActivityResultLauncher<String> audioPermissionLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private EditText activeVoiceTarget = null;
    private String activeFieldName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_medicine);

        viewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        initViews();
        setupPhotoLaunchers();
        setupVoiceLauncher();
        setupNotificationPermissionLauncher();
        setupListeners();

        if (getIntent().hasExtra(MedicineListActivity.EXTRA_MEDICINE)) {
            existingMedicine = (Medicine) getIntent().getSerializableExtra(MedicineListActivity.EXTRA_MEDICINE);
            if (existingMedicine != null) {
                populateFieldsForEdit();
            }
        } else {
            addTimeChip("08:00 AM");
        }

        checkAndRequestNotificationPermission();
    }

    private void initViews() {
        Button btnBack = findViewById(R.id.btnAddEditBack);
        btnBack.setOnClickListener(v -> finish());

        tvTitle = findViewById(R.id.tvAddEditTitle);
        etMedicineName = findViewById(R.id.etMedicineName);
        etDosage = findViewById(R.id.etDosage);
        etNotes = findViewById(R.id.etNotes);

        rgInstruction = findViewById(R.id.rgInstruction);
        rbBeforeMeal = findViewById(R.id.rbBeforeMeal);
        rbAfterMeal = findViewById(R.id.rbAfterMeal);
        rbWithMeal = findViewById(R.id.rbWithMeal);

        ivMedicinePicPreview = findViewById(R.id.ivMedicinePicPreview);
        btnTakeCameraPhoto = findViewById(R.id.btnTakeCameraPhoto);
        btnPickGalleryPhoto = findViewById(R.id.btnPickGalleryPhoto);
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto);

        chipGroupTimes = findViewById(R.id.chipGroupTimes);
        btnAddTime = findViewById(R.id.btnAddTime);

        btnSelectEveryday = findViewById(R.id.btnSelectEveryday);
        cbSun = findViewById(R.id.cbSun);
        cbMon = findViewById(R.id.cbMon);
        cbTue = findViewById(R.id.cbTue);
        cbWed = findViewById(R.id.cbWed);
        cbThu = findViewById(R.id.cbThu);
        cbFri = findViewById(R.id.cbFri);
        cbSat = findViewById(R.id.cbSat);

        btnPickSpecificDate = findViewById(R.id.btnPickSpecificDate);
        btnClearDate = findViewById(R.id.btnClearDate);
        btnSaveMedicine = findViewById(R.id.btnSaveMedicine);

        ImageButton btnVoiceName = findViewById(R.id.btnVoiceName);
        ImageButton btnVoiceDosage = findViewById(R.id.btnVoiceDosage);
        ImageButton btnVoiceNotes = findViewById(R.id.btnVoiceNotes);

        btnVoiceName.setOnClickListener(v -> checkPermissionAndStartVoice(etMedicineName, "Medicine Name"));
        btnVoiceDosage.setOnClickListener(v -> checkPermissionAndStartVoice(etDosage, "Dosage"));
        btnVoiceNotes.setOnClickListener(v -> checkPermissionAndStartVoice(etNotes, "Notes"));
    }

    private void setupPhotoLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        String savedPath = saveBitmapToFile(bitmap);
                        if (savedPath != null) {
                            currentPhotoPath = savedPath;
                            displayPhotoPreview(currentPhotoPath);
                        }
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        String savedPath = copyUriToFile(uri);
                        if (savedPath != null) {
                            currentPhotoPath = savedPath;
                            displayPhotoPreview(currentPhotoPath);
                        }
                    }
                }
        );
    }

    private void displayPhotoPreview(String path) {
        if (path == null || path.trim().isEmpty()) {
            ivMedicinePicPreview.setImageResource(android.R.drawable.ic_menu_camera);
            btnRemovePhoto.setVisibility(View.GONE);
            return;
        }

        try {
            File imgFile = new File(path);
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (bitmap != null) {
                    ivMedicinePicPreview.setImageBitmap(bitmap);
                    btnRemovePhoto.setVisibility(View.VISIBLE);
                    return;
                }
            }
            Uri uri = Uri.parse(path);
            ivMedicinePicPreview.setImageURI(uri);
            btnRemovePhoto.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            ivMedicinePicPreview.setImageResource(android.R.drawable.ic_menu_camera);
            btnRemovePhoto.setVisibility(View.GONE);
        }
    }

    private String saveBitmapToFile(Bitmap bitmap) {
        try {
            File dir = new File(getFilesDir(), "medicine_photos");
            if (!dir.exists()) dir.mkdirs();
            File photoFile = new File(dir, "med_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(photoFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            return photoFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save photo.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private String copyUriToFile(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            if (bitmap != null) {
                return saveBitmapToFile(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setupNotificationPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(this, "Notification permission is needed for medicine alarms to ring.", Toast.LENGTH_LONG).show();
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

    private void setupVoiceLauncher() {
        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchNativeSpeechIntent();
                    } else {
                        Toast.makeText(this, "Microphone permission is required for voice input.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty() && activeVoiceTarget != null) {
                            String recognizedText = matches.get(0);
                            activeVoiceTarget.setText(recognizedText);
                            activeVoiceTarget.setSelection(recognizedText.length());
                            Toast.makeText(this, "Recognized: " + recognizedText, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "No speech recognized. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void checkPermissionAndStartVoice(EditText targetEditText, String fieldName) {
        activeVoiceTarget = targetEditText;
        activeFieldName = fieldName;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            launchNativeSpeechIntent();
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void launchNativeSpeechIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak " + activeFieldName + "...");

        try {
            speechRecognizerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to start speech recognition.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        btnTakeCameraPhoto.setOnClickListener(v -> cameraLauncher.launch(null));
        btnPickGalleryPhoto.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnRemovePhoto.setOnClickListener(v -> {
            currentPhotoPath = "";
            displayPhotoPreview("");
        });

        btnAddTime.setOnClickListener(v -> showTimePickerDialog());

        btnSelectEveryday.setOnClickListener(v -> {
            boolean selectAll = !(cbSun.isChecked() && cbMon.isChecked() && cbTue.isChecked()
                    && cbWed.isChecked() && cbThu.isChecked() && cbFri.isChecked() && cbSat.isChecked());
            cbSun.setChecked(selectAll);
            cbMon.setChecked(selectAll);
            cbTue.setChecked(selectAll);
            cbWed.setChecked(selectAll);
            cbThu.setChecked(selectAll);
            cbFri.setChecked(selectAll);
            cbSat.setChecked(selectAll);
        });

        btnPickSpecificDate.setOnClickListener(v -> showDatePickerDialog());

        btnClearDate.setOnClickListener(v -> {
            selectedSpecificDate = "";
            btnPickSpecificDate.setText("📅 Select Specific Date");
            btnClearDate.setVisibility(View.GONE);
        });

        btnSaveMedicine.setOnClickListener(v -> saveMedicine());
    }

    private void showTimePickerDialog() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(this, (view, h, m) -> {
            Calendar timeCal = Calendar.getInstance();
            timeCal.set(Calendar.HOUR_OF_DAY, h);
            timeCal.set(Calendar.MINUTE, m);
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            String formattedTime = sdf.format(timeCal.getTime());
            addTimeChip(formattedTime);
        }, hour, minute, false);
        dialog.show();
    }

    private void addTimeChip(String timeStr) {
        if (selectedTimesList.contains(timeStr)) return;

        selectedTimesList.add(timeStr);

        Chip chip = new Chip(this);
        chip.setText(timeStr);
        chip.setCloseIconVisible(true);
        chip.setTextSize(16.0f);
        chip.setChipBackgroundColorResource(R.color.white);
        chip.setChipStrokeColorResource(android.R.color.darker_gray);
        chip.setChipStrokeWidth(2.0f);

        chip.setOnCloseIconClickListener(v -> {
            selectedTimesList.remove(timeStr);
            chipGroupTimes.removeView(chip);
        });

        chipGroupTimes.addView(chip);
    }

    private void showDatePickerDialog() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar dateCal = Calendar.getInstance();
            dateCal.set(Calendar.YEAR, year);
            dateCal.set(Calendar.MONTH, month);
            dateCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
            selectedSpecificDate = sdf.format(dateCal.getTime());
            btnPickSpecificDate.setText("📅 Date: " + selectedSpecificDate);
            btnClearDate.setVisibility(View.VISIBLE);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private String getSelectedInstruction() {
        int checkedId = rgInstruction.getCheckedRadioButtonId();
        if (checkedId == R.id.rbBeforeMeal) {
            return "Before Meal";
        } else if (checkedId == R.id.rbWithMeal) {
            return "With Meal";
        } else {
            return "After Meal";
        }
    }

    private void populateFieldsForEdit() {
        tvTitle.setText("Edit Medicine");
        btnSaveMedicine.setText("UPDATE MEDICINE REMINDER");

        etMedicineName.setText(existingMedicine.getName());
        etDosage.setText(existingMedicine.getDosage());
        etNotes.setText(existingMedicine.getNotes());

        String inst = existingMedicine.getInstruction();
        if ("Before Meal".equalsIgnoreCase(inst)) {
            rbBeforeMeal.setChecked(true);
        } else if ("With Meal".equalsIgnoreCase(inst)) {
            rbWithMeal.setChecked(true);
        } else {
            rbAfterMeal.setChecked(true);
        }

        if (existingMedicine.getImageUri() != null && !existingMedicine.getImageUri().trim().isEmpty()) {
            currentPhotoPath = existingMedicine.getImageUri();
            displayPhotoPreview(currentPhotoPath);
        }

        if (existingMedicine.getTimes() != null && !existingMedicine.getTimes().trim().isEmpty()) {
            String[] times = existingMedicine.getTimes().split(", ");
            for (String t : times) {
                if (!t.trim().isEmpty()) {
                    addTimeChip(t.trim());
                }
            }
        }

        String repeat = existingMedicine.getRepeatDays();
        if (repeat != null) {
            if (repeat.contains("Sun")) cbSun.setChecked(true);
            if (repeat.contains("Mon")) cbMon.setChecked(true);
            if (repeat.contains("Tue")) cbTue.setChecked(true);
            if (repeat.contains("Wed")) cbWed.setChecked(true);
            if (repeat.contains("Thu")) cbThu.setChecked(true);
            if (repeat.contains("Fri")) cbFri.setChecked(true);
            if (repeat.contains("Sat")) cbSat.setChecked(true);
        }

        if (existingMedicine.getSpecificDate() != null && !existingMedicine.getSpecificDate().trim().isEmpty()) {
            selectedSpecificDate = existingMedicine.getSpecificDate();
            btnPickSpecificDate.setText("📅 Date: " + selectedSpecificDate);
            btnClearDate.setVisibility(View.VISIBLE);
        }
    }

    private void saveMedicine() {
        String name = etMedicineName.getText().toString().trim();
        String dosage = etDosage.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        String instruction = getSelectedInstruction();

        if (TextUtils.isEmpty(name)) {
            etMedicineName.setError("Please enter medicine name");
            etMedicineName.requestFocus();
            return;
        }

        if (selectedTimesList.isEmpty()) {
            Toast.makeText(this, "Please add at least one time for the reminder.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder timesSb = new StringBuilder();
        for (int i = 0; i < selectedTimesList.size(); i++) {
            timesSb.append(selectedTimesList.get(i));
            if (i < selectedTimesList.size() - 1) {
                timesSb.append(", ");
            }
        }
        String timesJoined = timesSb.toString();

        List<String> days = new ArrayList<>();
        if (cbSun.isChecked()) days.add("Sun");
        if (cbMon.isChecked()) days.add("Mon");
        if (cbTue.isChecked()) days.add("Tue");
        if (cbWed.isChecked()) days.add("Wed");
        if (cbThu.isChecked()) days.add("Thu");
        if (cbFri.isChecked()) days.add("Fri");
        if (cbSat.isChecked()) days.add("Sat");

        String repeatDays;
        if (days.size() == 7) {
            repeatDays = "Everyday";
        } else if (days.isEmpty()) {
            repeatDays = "Everyday";
        } else {
            repeatDays = TextUtils.join(", ", days);
        }

        if (existingMedicine != null) {
            existingMedicine.setName(name);
            existingMedicine.setDosage(dosage);
            existingMedicine.setTimes(timesJoined);
            existingMedicine.setRepeatDays(repeatDays);
            existingMedicine.setSpecificDate(selectedSpecificDate);
            existingMedicine.setNotes(notes);
            existingMedicine.setInstruction(instruction);
            existingMedicine.setImageUri(currentPhotoPath);

            viewModel.update(existingMedicine);
            scheduleMedicineAlarms(existingMedicine);
            Toast.makeText(this, "Medicine reminder updated 💊", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Medicine newMed = new Medicine(name, dosage, timesJoined, repeatDays, selectedSpecificDate, true, notes, instruction, currentPhotoPath);
            viewModel.insert(newMed, newId -> {
                newMed.setId((int) newId);
                scheduleMedicineAlarms(newMed);
                runOnUiThread(() -> {
                    Toast.makeText(AddEditMedicineActivity.this, "Medicine reminder saved 💊", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        }
    }

    private void scheduleMedicineAlarms(Medicine medicine) {
        if (!medicine.isEnabled()) return;

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        String timesStr = medicine.getTimes();
        if (timesStr == null || timesStr.trim().isEmpty()) return;

        String[] timesArray = timesStr.split(", ");
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);

        for (int i = 0; i < timesArray.length; i++) {
            String timeStr = timesArray[i].trim();
            try {
                Calendar parsedTime = Calendar.getInstance();
                parsedTime.setTime(sdf.parse(timeStr));

                Calendar alarmCal = Calendar.getInstance();
                alarmCal.set(Calendar.HOUR_OF_DAY, parsedTime.get(Calendar.HOUR_OF_DAY));
                alarmCal.set(Calendar.MINUTE, parsedTime.get(Calendar.MINUTE));
                alarmCal.set(Calendar.SECOND, 0);

                if (alarmCal.getTimeInMillis() <= System.currentTimeMillis()) {
                    alarmCal.add(Calendar.DAY_OF_YEAR, 1);
                }

                Intent intent = new Intent(this, MedicineAlarmReceiver.class);
                intent.putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_ID, medicine.getId());
                intent.putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NAME, medicine.getName());
                intent.putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_DOSAGE, medicine.getDosage());
                intent.putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_INSTRUCTION, medicine.getInstruction());
                intent.putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_NOTES, medicine.getNotes());
                intent.putExtra(MedicineAlarmReceiver.EXTRA_MEDICINE_IMAGE_URI, medicine.getImageUri());

                int requestCode = medicine.getId() * 100 + i;
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pendingIntent);
                        } else {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pendingIntent);
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pendingIntent);
                    }
                } catch (SecurityException se) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pendingIntent);
                }
            } catch (Exception ignored) {
            }
        }
    }
}