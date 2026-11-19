package com.susmita.parkicompanion.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.susmita.parkicompanion.MainActivity;
import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.appointment.ui.AppointmentListActivity;
import com.susmita.parkicompanion.diet.ui.DietTrackerActivity;
import com.susmita.parkicompanion.medicine.ui.MedicineListActivity;
import com.susmita.parkicompanion.todo.ui.TodoSplashActivity;
import com.susmita.parkicompanion.assessment.HandwritingTestActivity;
import com.susmita.parkicompanion.assessment.VoiceTestActivity;

public class FeatureDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_dashboard);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> navigateToMain());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToMain();
            }
        });

        setupFeatureCards();
    }

    private void setupFeatureCards() {
        // Row 1: Diet & Calorie Tracker | Exercise
        View cardDiet = findViewById(R.id.cardDiet);
        if (cardDiet != null) {
            cardDiet.setOnClickListener(v -> {
                Intent intent = new Intent(FeatureDashboardActivity.this, DietTrackerActivity.class);
                startActivity(intent);
            });
        }

        View cardExercise = findViewById(R.id.cardExercise);
        if (cardExercise != null) {
            cardExercise.setOnClickListener(v -> {
                Intent intent = new Intent(FeatureDashboardActivity.this, com.susmita.parkicompanion.exercise.ui.ExerciseActivity.class);
                startActivity(intent);
            });
        }

        // Row 2: Medicine Reminder | Appointment Reminder
        View cardMedicine = findViewById(R.id.cardMedicine);
        if (cardMedicine != null) {
            cardMedicine.setOnClickListener(v -> {
                Intent intent = new Intent(FeatureDashboardActivity.this, MedicineListActivity.class);
                startActivity(intent);
            });
        }

        View cardAppointment = findViewById(R.id.cardAppointment);
        if (cardAppointment != null) {
            cardAppointment.setOnClickListener(v -> {
                Intent intent = new Intent(FeatureDashboardActivity.this, AppointmentListActivity.class);
                startActivity(intent);
            });
        }

        // Row 3: To-Do | Handwriting Test
        View cardToDo = findViewById(R.id.cardToDo);
        if (cardToDo != null) {
            cardToDo.setOnClickListener(v -> {
                Intent intent = new Intent(FeatureDashboardActivity.this, TodoSplashActivity.class);
                startActivity(intent);
            });
        }

        View cardHandwriting = findViewById(R.id.cardHandwriting);
        if (cardHandwriting != null) {
            cardHandwriting.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Handwriting Test")
                    .setItems(new String[]{"Spiral", "Meander"}, (dialog, which) -> {
                        Intent intent = new Intent(this, HandwritingTestActivity.class);
                        intent.putExtra("test_type", which == 0 ? "spiral" : "meander");
                        startActivity(intent);
                    })
                    .setNegativeButton("Close", null)
                    .show());
        }

        // Row 4: Voice Test | Tremor Test
        View cardVoice = findViewById(R.id.cardVoice);
        if (cardVoice != null) {
            cardVoice.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Voice Test")
                    .setItems(new String[]{"Sustained vowel", "Pa-ta-ka", "Reading"}, (dialog, which) -> {
                        Intent intent = new Intent(this, VoiceTestActivity.class);
                        intent.putExtra("task_type", which == 0 ? "vowel" : which == 1 ? "ddk" : "reading");
                        startActivity(intent);
                    })
                    .setNegativeButton("Close", null)
                    .show());
        }
        setupCard(R.id.cardTremor, "🤲 Tremor Test", "Accelerometer-based tremor and motor fluctuation detection.");

        // Row 5: Progress Tracking | AI Assistant
        setupCard(R.id.cardProgress, "📈 Progress Tracking", "Monitor your symptom progression over time with visual charts.");
        setupCard(R.id.cardAiAssistant, "🤖 AI Assistant", "AI-powered assistance and answers for Parkinson's disease management.");
    }

    private void setupCard(int viewId, String title, String description) {
        View card = findViewById(viewId);
        if (card != null) {
            card.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(title);
                builder.setMessage(description + "\n\nWould you like to open this feature?");
                builder.setPositiveButton("Open Feature", (dialog, which) ->
                        Toast.makeText(this, title + " launched!", Toast.LENGTH_SHORT).show()
                );
                builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
                builder.show();
            });
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(FeatureDashboardActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}