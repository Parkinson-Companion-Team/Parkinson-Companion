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
import com.susmita.parkicompanion.personalize.PersonalizeCareActivity;

public class SignUpActivity extends AppCompatActivity {

    public static final String EXTRA_USER_NAME = "extra_user_name";

    private EditText etEmail;
    private EditText etPassword;
    private CheckBox cbTerms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbTerms = findViewById(R.id.cbTerms);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnSignUp = findViewById(R.id.btnSignUp);
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

        Toast.makeText(this, "🎉 Account created successfully! Welcome to Parkinson's Companion.", Toast.LENGTH_LONG).show();
        navigateToFeatureDashboard();
    }

    private void navigateToFeatureDashboard() {
        Intent intent = new Intent(SignUpActivity.this, PersonalizeCareActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}