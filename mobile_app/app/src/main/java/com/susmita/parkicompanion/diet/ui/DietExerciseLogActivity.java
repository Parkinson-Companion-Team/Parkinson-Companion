package com.susmita.parkicompanion.diet.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.susmita.parkicompanion.R;

import java.util.Locale;

public class DietExerciseLogActivity extends AppCompatActivity {

    public static final String EXTRA_DATE_KEY = "extra_date_key";
    private static final String PREFS_NAME = "diet_tracker_prefs";

    private String dateKey = "default_date";

    private CheckBox cbLeg, cbCore, cbHand, cbBalance, cbFacial, cbBed, cbSwimming, cbBreathing;
    private TextView tvTotalExerciseCalories;

    private static final int CAL_LEG = 65;
    private static final int CAL_CORE = 70;
    private static final int CAL_HAND = 40;
    private static final int CAL_BALANCE = 40;
    private static final int CAL_FACIAL = 20;
    private static final int CAL_BED = 70;
    private static final int CAL_SWIMMING = 90;
    private static final int CAL_BREATHING = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_exercise_log);

        if (getIntent().hasExtra(EXTRA_DATE_KEY)) {
            dateKey = getIntent().getStringExtra(EXTRA_DATE_KEY);
        }

        initViews();
        setupNavigation();
        loadSavedState();
        setupItemClickListeners();
        calculateTotalCalories();
    }

    private void initViews() {
        cbLeg = findViewById(R.id.cbLeg);
        cbCore = findViewById(R.id.cbCore);
        cbHand = findViewById(R.id.cbHand);
        cbBalance = findViewById(R.id.cbBalance);
        cbFacial = findViewById(R.id.cbFacial);
        cbBed = findViewById(R.id.cbBed);
        cbSwimming = findViewById(R.id.cbSwimming);
        cbBreathing = findViewById(R.id.cbBreathing);

        tvTotalExerciseCalories = findViewById(R.id.tvTotalExerciseCalories);
        Button btnSaveExercise = findViewById(R.id.btnSaveExercise);

        btnSaveExercise.setOnClickListener(v -> saveExerciseSelection());
    }

    private void setupNavigation() {
        Button btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void loadSavedState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        cbLeg.setChecked(prefs.getBoolean(dateKey + "_ex_leg", false));
        cbCore.setChecked(prefs.getBoolean(dateKey + "_ex_core", false));
        cbHand.setChecked(prefs.getBoolean(dateKey + "_ex_hand", false));
        cbBalance.setChecked(prefs.getBoolean(dateKey + "_ex_balance", false));
        cbFacial.setChecked(prefs.getBoolean(dateKey + "_ex_facial", false));
        cbBed.setChecked(prefs.getBoolean(dateKey + "_ex_bed", false));
        cbSwimming.setChecked(prefs.getBoolean(dateKey + "_ex_swimming", false));
        cbBreathing.setChecked(prefs.getBoolean(dateKey + "_ex_breathing", false));
    }

    private void setupItemClickListeners() {
        setupRowToggle(R.id.itemLeg, cbLeg);
        setupRowToggle(R.id.itemCore, cbCore);
        setupRowToggle(R.id.itemHand, cbHand);
        setupRowToggle(R.id.itemBalance, cbBalance);
        setupRowToggle(R.id.itemFacial, cbFacial);
        setupRowToggle(R.id.itemBed, cbBed);
        setupRowToggle(R.id.itemSwimming, cbSwimming);
        setupRowToggle(R.id.itemBreathing, cbBreathing);
    }

    private void setupRowToggle(int layoutResId, CheckBox checkBox) {
        View layout = findViewById(layoutResId);
        if (layout != null) {
            layout.setOnClickListener(v -> {
                checkBox.setChecked(!checkBox.isChecked());
                calculateTotalCalories();
            });
        }
    }

    private int calculateTotalCalories() {
        int total = 0;
        if (cbLeg.isChecked()) total += CAL_LEG;
        if (cbCore.isChecked()) total += CAL_CORE;
        if (cbHand.isChecked()) total += CAL_HAND;
        if (cbBalance.isChecked()) total += CAL_BALANCE;
        if (cbFacial.isChecked()) total += CAL_FACIAL;
        if (cbBed.isChecked()) total += CAL_BED;
        if (cbSwimming.isChecked()) total += CAL_SWIMMING;
        if (cbBreathing.isChecked()) total += CAL_BREATHING;

        tvTotalExerciseCalories.setText(String.format(Locale.US, "Total Burned: %d cals", total));
        return total;
    }

    private void saveExerciseSelection() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int total = calculateTotalCalories();

        prefs.edit()
                .putBoolean(dateKey + "_ex_leg", cbLeg.isChecked())
                .putBoolean(dateKey + "_ex_core", cbCore.isChecked())
                .putBoolean(dateKey + "_ex_hand", cbHand.isChecked())
                .putBoolean(dateKey + "_ex_balance", cbBalance.isChecked())
                .putBoolean(dateKey + "_ex_facial", cbFacial.isChecked())
                .putBoolean(dateKey + "_ex_bed", cbBed.isChecked())
                .putBoolean(dateKey + "_ex_swimming", cbSwimming.isChecked())
                .putBoolean(dateKey + "_ex_breathing", cbBreathing.isChecked())
                .putInt(dateKey + "_exercise_cal", total)
                .apply();

        Toast.makeText(this, String.format(Locale.US, "Exercise updated: %d cals added to Energy Requirement!", total), Toast.LENGTH_SHORT).show();
        finish();
    }
}