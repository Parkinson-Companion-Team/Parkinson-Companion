package com.susmita.parkicompanion;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.susmita.parkicompanion.auth.LoginActivity;
import com.susmita.parkicompanion.personalize.PersonalizeCareActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#F7F5F0"));

        Button btnGetStarted = findViewById(R.id.btnGetStarted);
        TextView tvLogin = findViewById(R.id.tvLogin);

        btnGetStarted.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    PersonalizeCareActivity.class
            );
            startActivity(intent);
        });

        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }
}