package com.susmita.parkicompanion.assessment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.network.ApiClient;
import com.susmita.parkicompanion.view.DrawView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.Response;

public class HandwritingTestActivity extends AppCompatActivity {

    private DrawView drawView;
    private Button submitButton;
    private ProgressBar progressBar;
    private TextView resultText;
    private String testType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handwriting_test);

        testType = getIntent().getStringExtra("test_type");
        if (!"meander".equals(testType)) {
            testType = "spiral";
        }
        drawView = findViewById(R.id.drawView);
        Button clearButton = findViewById(R.id.btnClearDrawing);
        submitButton = findViewById(R.id.btnSubmitDrawing);
        progressBar = findViewById(R.id.progressDrawing);
        resultText = findViewById(R.id.tvDrawingResult);

        clearButton.setOnClickListener(v -> {
            drawView.clear();
            resultText.setText("");
        });
        submitButton.setOnClickListener(v -> submitDrawing());
    }

    private void submitDrawing() {
        if (drawView.isEmpty()) {
            resultText.setText("Draw something before submitting.");
            return;
        }

        submitButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        resultText.setText("Uploading...");
        File drawingFile = new File(getCacheDir(), "drawing.png");
        try {
            Bitmap bitmap = drawView.export();
            try (FileOutputStream output = new FileOutputStream(drawingFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            }
            bitmap.recycle();
        } catch (IOException error) {
            showError("Could not save drawing.");
            return;
        }

        ApiClient.ensureToken(this, new ApiClient.TokenCallback() {
            @Override
            public void onToken(String token) {
                uploadDrawing(drawingFile, token);
            }

            @Override
            public void onFailure(IOException error) {
                drawingFile.delete();
                showError("Can't reach server");
            }
        });
    }

    private void uploadDrawing(File drawingFile, String token) {
        Request request = ApiClient.authenticatedRequest("/writing-test/analyze/", token)
                .post(new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("test_type", testType)
                        .addFormDataPart("uploaded_file", "drawing.png",
                                okhttp3.RequestBody.create(MediaType.parse("image/png"), drawingFile))
                        .build())
                .build();
        ApiClient.client().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException error) {
                drawingFile.delete();
                showError("Can't reach server");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response ignored = response) {
                    String body = response.body() == null ? "" : response.body().string();
                    drawingFile.delete();
                    if (!response.isSuccessful()) {
                        String message = "Upload failed";
                        if (!body.isEmpty()) {
                            JsonObject errorBody = JsonParser.parseString(body).getAsJsonObject();
                            if (errorBody.has("error")) {
                                message = errorBody.get("error").getAsString();
                            }
                        }
                        showError(message);
                        return;
                    }
                    JsonObject data = JsonParser.parseString(body).getAsJsonObject()
                            .getAsJsonObject("data");
                    showResult("Stability score: " + data.get("stability_score").getAsString());
                }
            }
        });
    }

    private void showResult(String message) {
        runOnUiThread(() -> {
            submitButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            resultText.setText(message);
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            submitButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            resultText.setText(message);
        });
    }
}
