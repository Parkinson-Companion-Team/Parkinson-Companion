package com.susmita.parkicompanion.todo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.susmita.parkicompanion.R;

public class TodoSplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 3000L; // 3 seconds
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_splash);

        handler = new Handler(Looper.getMainLooper());
        runnable = () -> {
            Intent intent = new Intent(TodoSplashActivity.this, TodoListActivity.class);
            startActivity(intent);
            finish();
        };

        handler.postDelayed(runnable, SPLASH_DELAY_MS);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (handler != null && runnable != null) {
                    handler.removeCallbacks(runnable);
                }
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        super.onDestroy();
    }
}
