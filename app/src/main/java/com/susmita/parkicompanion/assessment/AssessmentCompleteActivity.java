package com.susmita.parkicompanion.assessment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.susmita.parkicompanion.MainActivity;
import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.auth.LoginActivity;
import com.susmita.parkicompanion.auth.SignUpActivity;
import com.susmita.parkicompanion.dashboard.FeatureDashboardActivity;

public class AssessmentCompleteActivity extends AppCompatActivity {

    public static final String EXTRA_USER_NAME = "extra_user_name";
    public static final String EXTRA_UPDRS_SCORE = "extra_updrs_score";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assessment_complete);

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        TextView tvUpdrsScore = findViewById(R.id.tvUpdrsScore);
        TextView tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);
        TextView tvLogIn = findViewById(R.id.tvLogIn);
        Button btnContinueGuest = findViewById(R.id.btnContinueGuest);

        String userName = getIntent().getStringExtra(EXTRA_USER_NAME);
        if (userName == null || userName.trim().isEmpty()) {
            userName = "Susmita";
        }
        tvGreeting.setText("Hi, " + userName + "!");

        int score = getIntent().getIntExtra(EXTRA_UPDRS_SCORE, 15);
        tvUpdrsScore.setText("UPDRS Score: " + score);

        String status;
        if (score <= 32) {
            status = "Current Status: Mild Symptoms";
        } else if (score <= 58) {
            status = "Current Status: Moderate Symptoms";
        } else {
            status = "Current Status: Severe Symptoms";
        }
        tvCurrentStatus.setText(status);

        final String finalUserName = userName;
        btnCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(AssessmentCompleteActivity.this, SignUpActivity.class);
            intent.putExtra(SignUpActivity.EXTRA_USER_NAME, finalUserName);
            startActivity(intent);
        });

        tvLogIn.setOnClickListener(v -> {
            Intent intent = new Intent(AssessmentCompleteActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnContinueGuest.setOnClickListener(v -> {
            Toast.makeText(this, "Welcome, continuing as Guest!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AssessmentCompleteActivity.this, FeatureDashboardActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(AssessmentCompleteActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}