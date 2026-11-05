package com.susmita.parkicompanion.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.susmita.parkicompanion.R;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etResetEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etResetEmail = findViewById(R.id.etResetEmail);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnSendReset = findViewById(R.id.btnSendReset);
        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnBack.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        btnSendReset.setOnClickListener(v -> sendResetLink());

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void sendResetLink() {
        String email = etResetEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etResetEmail.setError("Please enter a valid email address");
            etResetEmail.requestFocus();
            return;
        }

        Toast.makeText(this, "📧 Password reset link sent to " + email, Toast.LENGTH_LONG).show();

        // Return to Login
        finish();
    }
}