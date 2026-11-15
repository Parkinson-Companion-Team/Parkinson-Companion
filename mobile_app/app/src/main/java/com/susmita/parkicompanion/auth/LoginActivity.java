package com.susmita.parkicompanion.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
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

import okhttp3.FormBody;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        CheckBox cbShowPassword = findViewById(R.id.cbShowPassword);

        Button btnBack = findViewById(R.id.btnBack);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnLogIn = findViewById(R.id.btnLogIn);
        TextView tvSignUp = findViewById(R.id.tvSignUp);

        btnBack.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
            startActivity(intent);
        });

        btnLogIn.setOnClickListener(v -> performLogin());

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        btnLogIn.setEnabled(false);
        Request request = new Request.Builder()
                .url(ApiClient.BASE_URL + "/api-token-auth/")
                .post(new FormBody.Builder()
                        .add("username", email)
                        .add("password", password)
                        .build())
                .build();
        ApiClient.client().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnLogIn.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Can't reach server", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response ignored = response) {
                    if (response.code() == 400) {
                        runOnUiThread(() -> {
                            btnLogIn.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "Wrong email or password", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        runOnUiThread(() -> {
                            btnLogIn.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "Can't reach server", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    String token = JsonParser.parseString(response.body().string())
                            .getAsJsonObject().get("token").getAsString();
                    ApiClient.saveToken(LoginActivity.this, token);
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "🎉 Welcome back!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, PersonalizeCareActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    });
                }
            }
        });
    }
}