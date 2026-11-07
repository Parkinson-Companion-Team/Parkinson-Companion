package com.susmita.parkicompanion.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.network.ApiClient;
import com.susmita.parkicompanion.personalize.PersonalizeCareActivity;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class SignUpActivity extends AppCompatActivity {

    public static final String EXTRA_USER_NAME = "extra_user_name";

    private EditText etEmail;
    private EditText etPassword;
    private CheckBox cbTerms;
    private Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbTerms = findViewById(R.id.cbTerms);

        Button btnBack = findViewById(R.id.btnBack);
        btnSignUp = findViewById(R.id.btnSignUp);
        Button btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp);
        TextView tvLoginRedirect = findViewById(R.id.tvLoginRedirect);

        btnBack.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        btnSignUp.setOnClickListener(v -> performSignUp());

        btnGoogleSignUp.setOnClickListener(v -> {
            Toast.makeText(this, "🎉 Google Sign-up successful!", Toast.LENGTH_SHORT).show();
            navigateToFeatureDashboard();
        });

        tvLoginRedirect.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void performSignUp() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please accept the Terms of Service to continue", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSignUp.setEnabled(false);
        Request.Builder requestBuilder = ApiClient.authenticatedRequest("/register/", ApiClient.getToken(this))
                .post(new FormBody.Builder()
                        .add("email", email)
                        .add("password", password)
                        .build());
        ApiClient.client().newCall(requestBuilder.build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnSignUp.setEnabled(true);
                    Toast.makeText(SignUpActivity.this, "Can't reach server", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response ignored = response) {
                    if (response.code() == 409) {
                        runOnUiThread(() -> {
                            btnSignUp.setEnabled(true);
                            Toast.makeText(SignUpActivity.this,
                                    "Account already exists - please log in", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                    if (response.code() == 400) {
                        runOnUiThread(() -> {
                            btnSignUp.setEnabled(true);
                            Toast.makeText(SignUpActivity.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        runOnUiThread(() -> {
                            btnSignUp.setEnabled(true);
                            Toast.makeText(SignUpActivity.this, "Can't reach server", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    String token = JsonParser.parseString(response.body().string())
                            .getAsJsonObject().get("token").getAsString();
                    ApiClient.saveToken(SignUpActivity.this, token);
                    runOnUiThread(() -> {
                        Toast.makeText(SignUpActivity.this,
                                "🎉 Account created successfully! Welcome to Parkinson's Companion.",
                                Toast.LENGTH_LONG).show();
                        navigateToFeatureDashboard();
                    });
                }
            }
        });
    }

    private void navigateToFeatureDashboard() {
        Intent intent = new Intent(SignUpActivity.this, PersonalizeCareActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}