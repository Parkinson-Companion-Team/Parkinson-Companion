package com.susmita.parkicompanion.assessment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.network.ApiClient;
import com.susmita.parkicompanion.util.WavRecorder;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.Response;

public class VoiceTestActivity extends AppCompatActivity {

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Button recordButton;
    private ProgressBar progressBar;
    private TextView timerText;
    private TextView resultText;
    private WavRecorder recorder;
    private long recordingStartedAt;
    private String taskType;
    private ActivityResultLauncher<String> permissionLauncher;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (recorder != null) {
                updateTimer();
                timerHandler.postDelayed(this, 200);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_test);

        taskType = getIntent().getStringExtra("task_type");
        if (!"ddk".equals(taskType) && !"reading".equals(taskType)) {
            taskType = "vowel";
        }
        recordButton = findViewById(R.id.btnRecordVoice);
        progressBar = findViewById(R.id.progressVoice);
        timerText = findViewById(R.id.tvVoiceTimer);
        resultText = findViewById(R.id.tvVoiceResult);
        ((TextView) findViewById(R.id.tvVoiceInstruction)).setText(instructionFor(taskType));
        ((TextView) findViewById(R.id.tvVoicePassage)).setVisibility(
                "reading".equals(taskType) ? View.VISIBLE : View.GONE);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startRecording();
                    } else {
                        resultText.setText("Microphone permission is required for this test.");
                    }
                });
        recordButton.setOnClickListener(v -> {
            if (recorder == null) {
                checkPermissionAndStart();
            } else {
                stopRecording();
            }
        });
    }

    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startRecording() {
        File recordingFile = new File(getCacheDir(), "voice.wav");
        recorder = new WavRecorder(recordingFile);
        try {
            recorder.start();
            recordingStartedAt = System.currentTimeMillis();
            recordButton.setText("Stop");
            resultText.setText("Recording...");
            timerHandler.post(timerRunnable);
        } catch (IOException | IllegalStateException error) {
            recorder = null;
            resultText.setText("Could not start recording.");
        }
    }

    private void stopRecording() {
        WavRecorder currentRecorder = recorder;
        recorder = null;
        timerHandler.removeCallbacks(timerRunnable);
        recordButton.setEnabled(false);
        recordButton.setText("Record");
        long durationSeconds = (System.currentTimeMillis() - recordingStartedAt) / 1000;
        new Thread(() -> {
            File recordingFile;
            try {
                recordingFile = currentRecorder.stop();
            } catch (IOException error) {
                runOnUiThread(() -> {
                    recordButton.setEnabled(true);
                    resultText.setText("Could not save recording.");
                });
                return;
            }
            if (durationSeconds < 3) {
                recordingFile.delete();
                runOnUiThread(() -> {
                    recordButton.setEnabled(true);
                    timerText.setText("0:00");
                    resultText.setText("Please record at least 3 seconds of audio.");
                });
                return;
            }
            runOnUiThread(() -> {
                progressBar.setVisibility(View.VISIBLE);
                resultText.setText("Uploading...");
            });
            ApiClient.ensureToken(this, new ApiClient.TokenCallback() {
                @Override
                public void onToken(String token) {
                    uploadRecording(recordingFile, token);
                }

                @Override
                public void onFailure(IOException error) {
                    recordingFile.delete();
                    showError("Network error. Please try again.");
                }
            });
        }, "voice-upload-preparation").start();
    }

    private void uploadRecording(File recordingFile, String token) {
        Request request = ApiClient.authenticatedRequest("/voice-test/upload/", token)
                .post(new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("task_type", taskType)
                        .addFormDataPart("audio", "voice.wav",
                                okhttp3.RequestBody.create(MediaType.parse("audio/wav"), recordingFile))
                        .build())
                .build();
        ApiClient.client().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException error) {
                recordingFile.delete();
                showError("Network error. Please try again.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response ignored = response) {
                    String body = response.body() == null ? "" : response.body().string();
                    recordingFile.delete();
                    if (response.code() == 503) {
                        showError("Voice analysis is not available yet");
                    } else if (response.code() == 400) {
                        showError("The recording could not be analyzed. Please try again.");
                    } else if (!response.isSuccessful()) {
                        showError("Upload failed. Please try again.");
                    } else {
                        try {
                            showResult(parseResult(body));
                        } catch (RuntimeException error) {
                            showError("The server returned an invalid result.");
                        }
                    }
                }
            }
        });
    }

    private String parseResult(String body) {
        JsonObject data = JsonParser.parseString(body).getAsJsonObject();
        if (data.has("baseline_created") && data.get("baseline_created").getAsBoolean()) {
            return "Baseline saved - take the test again next time";
        }
        if (!data.has("stability_score")) {
            throw new IllegalStateException("Missing stability score");
        }
        return "Stability score: " + data.get("stability_score").getAsString();
    }

    private void updateTimer() {
        long seconds = (System.currentTimeMillis() - recordingStartedAt) / 1000;
        timerText.setText(String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60));
    }

    private String instructionFor(String type) {
        if ("ddk".equals(type)) {
            return "Repeat 'pa-ta-ka' quickly for 10 seconds";
        }
        if ("reading".equals(type)) {
            return "Read the passage shown";
        }
        return "Say 'aaah' steadily for 5-10 seconds";
    }

    private void showResult(String message) {
        runOnUiThread(() -> {
            recordButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            resultText.setText(message);
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            recordButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            resultText.setText(message);
        });
    }

    @Override
    protected void onDestroy() {
        timerHandler.removeCallbacks(timerRunnable);
        super.onDestroy();
    }
}
