package com.susmita.parkicompanion.personalize;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.assessment.AssessmentCompleteActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class PersonalizeCareActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etDob;
    private Spinner spinnerGender;
    private Spinner spinnerAccessibility;

    private SpeechRecognizer speechRecognizer;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personalize_care);

        etName = findViewById(R.id.etName);
        ImageButton btnMic = findViewById(R.id.btnMic);
        etDob = findViewById(R.id.etDob);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerAccessibility = findViewById(R.id.spinnerAccessibility);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnFinish = findViewById(R.id.btnFinish);

        setupPermissionLauncher();
        setupSpinners();
        setupDatePicker();

        btnMic.setOnClickListener(v -> checkPermissionAndStartVoice());

        btnBack.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        btnFinish.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            int score = calculateUpdrsScore();

            Intent intent = new Intent(PersonalizeCareActivity.this, AssessmentCompleteActivity.class);
            intent.putExtra(AssessmentCompleteActivity.EXTRA_USER_NAME, name);
            intent.putExtra(AssessmentCompleteActivity.EXTRA_UPDRS_SCORE, score);
            startActivity(intent);
        });
    }

    private int calculateUpdrsScore() {
        int totalScore = 0;
        int[] radioGroupIds = {
                R.id.rgQ18, R.id.rgQ19, R.id.rgQ20, R.id.rgQ21, R.id.rgQ22,
                R.id.rgQ23, R.id.rgQ24, R.id.rgQ25, R.id.rgQ26, R.id.rgQ27,
                R.id.rgQ28, R.id.rgQ29, R.id.rgQ30, R.id.rgQ31
        };

        for (int groupId : radioGroupIds) {
            RadioGroup rg = findViewById(groupId);
            if (rg != null) {
                int checkedId = rg.getCheckedRadioButtonId();
                if (checkedId != -1) {
                    RadioButton rb = findViewById(checkedId);
                    if (rb != null) {
                        String text = rb.getText().toString();
                        if (text.startsWith("0")) totalScore += 0;
                        else if (text.startsWith("1")) totalScore += 1;
                        else if (text.startsWith("2")) totalScore += 2;
                        else if (text.startsWith("3")) totalScore += 3;
                        else if (text.startsWith("4")) totalScore += 4;
                    }
                }
            }
        }
        return totalScore;
    }

    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startVoiceRecognition();
                    } else {
                        showVoiceInputDialog("Audio permission not granted. Enter your name:");
                    }
                }
        );
    }

    private void checkPermissionAndStartVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecognition();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showVoiceInputDialog("Speak or enter your name:");
            return;
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        }

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(PersonalizeCareActivity.this, "🎙️ Listening... Speak now", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                showVoiceInputDialog("Microphone listening... Enter or confirm your name:");
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    etName.setText(spokenText);
                    etName.setSelection(spokenText.length());
                    Toast.makeText(PersonalizeCareActivity.this, "Voice recognized: " + spokenText, Toast.LENGTH_SHORT).show();
                } else {
                    showVoiceInputDialog("Speak or enter your name:");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your name...");

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            showVoiceInputDialog("Speak or enter your name:");
        }
    }

    private void showVoiceInputDialog(String prompt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎙️ Voice Input (Microphone)");
        builder.setMessage(prompt);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        if (!etName.getText().toString().isEmpty()) {
            input.setText(etName.getText().toString());
        } else {
            input.setHint("e.g. Asim Ranjan Mandal");
        }
        builder.setView(input);

        builder.setPositiveButton("Set Name", (dialog, which) -> {
            String spokenText = input.getText().toString().trim();
            if (!spokenText.isEmpty()) {
                etName.setText(spokenText);
                etName.setSelection(spokenText.length());
                Toast.makeText(this, "Name set: " + spokenText, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, dialogWhich) -> dialog.cancel());

        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    private void setupSpinners() {
        String[] genderOptions = {"Select Gender", "Male", "Female", "Non-binary", "Other", "Prefer not to say"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genderOptions);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        String[] accessOptions = {
                "Select Accessibility Feature",
                "Large Text",
                "High Contrast",
                "Voice Navigation",
                "Bigger Buttons",
                "Haptic Feedback",
                "All Features Enabled"
        };
        ArrayAdapter<String> accessAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, accessOptions);
        accessAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAccessibility.setAdapter(accessAdapter);
    }

    private void setupDatePicker() {
        etDob.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR) - 50;
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    PersonalizeCareActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String dateStr = String.format(Locale.US, "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                        etDob.setText(dateStr);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });
    }
}