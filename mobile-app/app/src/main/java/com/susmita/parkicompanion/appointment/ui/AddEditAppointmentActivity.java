package com.susmita.parkicompanion.appointment.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.appointment.model.Appointment;
import com.susmita.parkicompanion.appointment.util.AlarmScheduler;
import com.susmita.parkicompanion.appointment.viewmodel.AppointmentViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddEditAppointmentActivity extends AppCompatActivity {

    public static final String EXTRA_APPOINTMENT = "extra_appointment";

    private EditText etTitle;
    private EditText etDoctor;
    private EditText etLocation;
    private EditText etPhone;
    private EditText etDate;
    private EditText etTime;
    private Spinner spinnerOffset;
    private EditText etNotes;
    private ImageView ivPrescriptionPreview;

    private Appointment existingAppointment;
    private final Calendar selectedCalendar = Calendar.getInstance();
    private boolean isDateSelected = false;
    private boolean isTimeSelected = false;

    private final List<String> attachedPrescriptionUris = new ArrayList<>();

    private AppointmentViewModel viewModel;
    private EditText activeVoiceTarget;
    private String activeFieldName = "";

    private ActivityResultLauncher<String> audioPermissionLauncher;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    private ActivityResultLauncher<String> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_appointment);

        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        etTitle = findViewById(R.id.etTitle);
        ImageButton btnTitleMic = findViewById(R.id.btnTitleMic);

        etDoctor = findViewById(R.id.etDoctor);
        ImageButton btnDoctorMic = findViewById(R.id.btnDoctorMic);

        etLocation = findViewById(R.id.etLocation);
        ImageButton btnLocationMic = findViewById(R.id.btnLocationMic);
        Button btnOpenMap = findViewById(R.id.btnOpenMap);

        etPhone = findViewById(R.id.etPhone);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        spinnerOffset = findViewById(R.id.spinnerOffset);

        etNotes = findViewById(R.id.etNotes);
        ImageButton btnNotesMic = findViewById(R.id.btnNotesMic);

        Button btnUploadPrescription = findViewById(R.id.btnUploadPrescription);
        ivPrescriptionPreview = findViewById(R.id.ivPrescriptionPreview);

        Button btnBookORS = findViewById(R.id.btnBookORS);
        Button btnBookPracto = findViewById(R.id.btnBookPracto);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnSaveAppointment = findViewById(R.id.btnSaveAppointment);

        viewModel = new ViewModelProvider(this).get(AppointmentViewModel.class);

        setupPermissionAndPickers();
        setupOffsetSpinner();

        btnBack.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        // Voice Input Buttons - each associated with its respective EditText
        btnTitleMic.setOnClickListener(v -> checkPermissionAndStartVoice(etTitle, "title"));
        btnDoctorMic.setOnClickListener(v -> checkPermissionAndStartVoice(etDoctor, "doctor's name"));
        btnLocationMic.setOnClickListener(v -> checkPermissionAndStartVoice(etLocation, "hospital location"));
        btnNotesMic.setOnClickListener(v -> checkPermissionAndStartVoice(etNotes, "prescription notes"));

        // Google Maps Button
        btnOpenMap.setOnClickListener(v -> openGoogleMaps());

        // Upload Prescription Image / PDF Button
        btnUploadPrescription.setOnClickListener(v -> filePickerLauncher.launch("*/*"));

        // Online Booking Portals
        btnBookORS.setOnClickListener(v -> openWebUrl("https://ors.gov.in"));
        btnBookPracto.setOnClickListener(v -> openWebUrl("https://www.practo.com"));

        // Check if Editing Existing Appointment
        existingAppointment = (Appointment) getIntent().getSerializableExtra(EXTRA_APPOINTMENT);
        if (existingAppointment != null) {
            tvHeaderTitle.setText("Edit Appointment");
            etTitle.setText(existingAppointment.getTitle());
            etDoctor.setText(existingAppointment.getDoctorName());
            etLocation.setText(existingAppointment.getLocation());
            etPhone.setText(existingAppointment.getPhone());
            etNotes.setText(existingAppointment.getNotes());

            if (existingAppointment.getPrescriptionUri() != null && !existingAppointment.getPrescriptionUri().trim().isEmpty()) {
                String[] savedUris = existingAppointment.getPrescriptionUri().split("\\|");
                for (String s : savedUris) {
                    if (!s.trim().isEmpty()) {
                        attachedPrescriptionUris.add(s.trim());
                    }
                }
                updatePrescriptionPreview();
            }

            selectedCalendar.setTimeInMillis(existingAppointment.getTimeInMillis());
            isDateSelected = true;
            isTimeSelected = true;
            updateDateLabel();
            updateTimeLabel();

            setSpinnerSelectionByOffset(existingAppointment.getReminderOffsetMinutes());
        }

        btnSaveAppointment.setOnClickListener(v -> saveAppointment());
    }

    private void setupPermissionAndPickers() {
        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchNativeSpeechIntent();
                    } else {
                        Toast.makeText(this, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show();
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
                    } else if (result.getResultCode() != RESULT_CANCELED) {
                        Toast.makeText(this, "Voice recognition canceled or failed.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        for (Uri u : uris) {
                            String internalPath = copyFileToInternalStorage(u);
                            attachedPrescriptionUris.add(internalPath);
                        }
                        updatePrescriptionPreview();
                        Toast.makeText(this, uris.size() + " prescription file(s) attached 📄", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        setupPickers();
    }

    private void checkPermissionAndStartVoice(EditText targetEditText, String fieldName) {
        activeVoiceTarget = targetEditText;
        activeFieldName = fieldName;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
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
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech recognition is not supported on this device.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to start speech recognition.", Toast.LENGTH_SHORT).show();
        }
    }

    private String copyFileToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return uri.toString();

            File presDir = new File(getFilesDir(), "prescriptions");
            if (!presDir.exists()) presDir.mkdirs();

            String extension = ".jpg";
            String mimeType = getContentResolver().getType(uri);
            if (mimeType != null && mimeType.contains("pdf")) {
                extension = ".pdf";
            }

            File destFile = new File(presDir, "prescription_" + System.currentTimeMillis() + extension);
            OutputStream outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return destFile.getAbsolutePath();
        } catch (Exception e) {
            return uri.toString();
        }
    }

    private void updatePrescriptionPreview() {
        if (attachedPrescriptionUris.isEmpty()) {
            ivPrescriptionPreview.setVisibility(View.GONE);
            return;
        }

        String firstPath = attachedPrescriptionUris.get(0);
        if (!firstPath.toLowerCase().endsWith(".pdf")) {
            try {
                Uri uri;
                if (firstPath.startsWith("content://") || firstPath.startsWith("file://")) {
                    uri = Uri.parse(firstPath);
                } else {
                    uri = Uri.fromFile(new File(firstPath));
                }
                ivPrescriptionPreview.setImageURI(uri);
                ivPrescriptionPreview.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                ivPrescriptionPreview.setVisibility(View.GONE);
            }
        } else {
            ivPrescriptionPreview.setVisibility(View.GONE);
        }
    }

    private void openGoogleMaps() {
        String loc = etLocation.getText().toString().trim();
        if (TextUtils.isEmpty(loc)) {
            Toast.makeText(this, "Please enter a location first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(loc));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        } catch (Exception e) {
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(loc)));
            startActivity(mapIntent);
        }
    }

    private void openWebUrl(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open web link", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupOffsetSpinner() {
        String[] offsetOptions = {
                "At time of appointment",
                "15 minutes before",
                "30 minutes before",
                "1 hour before",
                "1 day before"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, offsetOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOffset.setAdapter(adapter);
    }

    private int getOffsetMinutes() {
        int position = spinnerOffset.getSelectedItemPosition();
        switch (position) {
            case 1: return 15;
            case 2: return 30;
            case 3: return 60;
            case 4: return 1440;
            default: return 0;
        }
    }

    private void setSpinnerSelectionByOffset(int minutes) {
        switch (minutes) {
            case 15: spinnerOffset.setSelection(1); break;
            case 30: spinnerOffset.setSelection(2); break;
            case 60: spinnerOffset.setSelection(3); break;
            case 1440: spinnerOffset.setSelection(4); break;
            default: spinnerOffset.setSelection(0); break;
        }
    }

    private void setupPickers() {
        etDate.setOnClickListener(v -> {
            int year = selectedCalendar.get(Calendar.YEAR);
            int month = selectedCalendar.get(Calendar.MONTH);
            int day = selectedCalendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
                selectedCalendar.set(Calendar.YEAR, y);
                selectedCalendar.set(Calendar.MONTH, m);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, d);
                isDateSelected = true;
                updateDateLabel();
            }, year, month, day);
            dialog.show();
        });

        etTime.setOnClickListener(v -> {
            int hour = selectedCalendar.get(Calendar.HOUR_OF_DAY);
            int minute = selectedCalendar.get(Calendar.MINUTE);

            TimePickerDialog dialog = new TimePickerDialog(this, (view, h, m) -> {
                selectedCalendar.set(Calendar.HOUR_OF_DAY, h);
                selectedCalendar.set(Calendar.MINUTE, m);
                selectedCalendar.set(Calendar.SECOND, 0);
                isTimeSelected = true;
                updateTimeLabel();
            }, hour, minute, false);
            dialog.show();
        });
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        etDate.setText(sdf.format(selectedCalendar.getTime()));
    }

    private void updateTimeLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
        etTime.setText(sdf.format(selectedCalendar.getTime()));
    }

    private void saveAppointment() {
        String title = etTitle.getText().toString().trim();
        String doctor = etDoctor.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Appointment title is required");
            etTitle.requestFocus();
            return;
        }

        if (!isDateSelected || !isTimeSelected) {
            Toast.makeText(this, "Please select both date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        long appointmentTime = selectedCalendar.getTimeInMillis();
        int offset = getOffsetMinutes();

        StringBuilder prescriptionSb = new StringBuilder();
        for (int i = 0; i < attachedPrescriptionUris.size(); i++) {
            prescriptionSb.append(attachedPrescriptionUris.get(i));
            if (i < attachedPrescriptionUris.size() - 1) {
                prescriptionSb.append("|");
            }
        }
        String combinedPrescriptionStr = prescriptionSb.toString();

        if (existingAppointment != null) {
            existingAppointment.setTitle(title);
            existingAppointment.setDoctorName(doctor);
            existingAppointment.setLocation(location);
            existingAppointment.setPhone(phone);
            existingAppointment.setPrescriptionUri(combinedPrescriptionStr);
            existingAppointment.setNotes(notes);
            existingAppointment.setTimeInMillis(appointmentTime);
            existingAppointment.setReminderOffsetMinutes(offset);
            existingAppointment.setAlarmSet(true);

            viewModel.update(existingAppointment);
            AlarmScheduler.scheduleAlarm(this, existingAppointment);
            Toast.makeText(this, "Appointment updated & alarm set ⏰", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Appointment newAppt = new Appointment(title, doctor, location, phone, combinedPrescriptionStr, notes, appointmentTime, offset, true);
            viewModel.insert(newAppt, newId -> {
                newAppt.setId((int) newId);
                AlarmScheduler.scheduleAlarm(getApplicationContext(), newAppt);
            });
            Toast.makeText(this, "Appointment saved & alarm set ⏰", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}