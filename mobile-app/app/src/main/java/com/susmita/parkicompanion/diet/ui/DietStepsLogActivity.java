package com.susmita.parkicompanion.diet.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.susmita.parkicompanion.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DietStepsLogActivity extends AppCompatActivity {

    public static final String EXTRA_DATE_KEY = "extra_date_key";
    private static final String PREFS_NAME = "diet_tracker_prefs";

    private String dateKey = "default_date";

    private EditText etCustomSteps;
    private TextView tvCustomCalories;
    private LinearLayout layoutPresetStepsContainer;
    private TextView tvTotalStepsCalories;

    private static class StepOption {
        final int steps;
        final int kcal;
        boolean isSelected;
        CheckBox checkBox;

        StepOption(int steps, int kcal) {
            this.steps = steps;
            this.kcal = kcal;
            this.isSelected = false;
        }
    }

    private final List<StepOption> presetOptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_steps_log);

        if (getIntent().hasExtra(EXTRA_DATE_KEY)) {
            dateKey = getIntent().getStringExtra(EXTRA_DATE_KEY);
        }

        initPresetData();
        initViews();
        setupNavigation();
        loadSavedState();
        populatePresetViews();
        setupCustomInputListener();
        calculateTotalStepsAndCalories();
    }

    private void initPresetData() {
        presetOptions.clear();
        presetOptions.add(new StepOption(100, 3));
        presetOptions.add(new StepOption(200, 6));
        presetOptions.add(new StepOption(300, 9));
        presetOptions.add(new StepOption(400, 12));
        presetOptions.add(new StepOption(500, 15));
        presetOptions.add(new StepOption(600, 18));
        presetOptions.add(new StepOption(700, 21));
        presetOptions.add(new StepOption(800, 24));
        presetOptions.add(new StepOption(900, 27));
        presetOptions.add(new StepOption(1000, 30));
        presetOptions.add(new StepOption(2000, 60));
        presetOptions.add(new StepOption(3000, 90));
        presetOptions.add(new StepOption(5000, 150));
        presetOptions.add(new StepOption(8000, 240));
        presetOptions.add(new StepOption(10000, 300));
    }

    private void initViews() {
        etCustomSteps = findViewById(R.id.etCustomSteps);
        tvCustomCalories = findViewById(R.id.tvCustomCalories);
        layoutPresetStepsContainer = findViewById(R.id.layoutPresetStepsContainer);
        tvTotalStepsCalories = findViewById(R.id.tvTotalStepsCalories);

        Button btnSaveSteps = findViewById(R.id.btnSaveSteps);
        btnSaveSteps.setOnClickListener(v -> saveStepsSelection());
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

        int savedCustomSteps = prefs.getInt(dateKey + "_steps_custom", 0);
        if (savedCustomSteps > 0) {
            etCustomSteps.setText(String.valueOf(savedCustomSteps));
        }

        for (StepOption option : presetOptions) {
            option.isSelected = prefs.getBoolean(dateKey + "_step_preset_" + option.steps, false);
        }
    }

    private void populatePresetViews() {
        layoutPresetStepsContainer.removeAllViews();

        for (StepOption option : presetOptions) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.edit_text_border);
            row.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, dpToPx(10));
            row.setLayoutParams(params);

            CheckBox cb = new CheckBox(this);
            cb.setClickable(false);
            cb.setFocusable(false);
            cb.setChecked(option.isSelected);
            option.checkBox = cb;

            TextView tvSteps = new TextView(this);
            tvSteps.setText(String.format(Locale.US, "%,d steps", option.steps));
            tvSteps.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tvSteps.setTypeface(null, android.graphics.Typeface.BOLD);
            tvSteps.setTextColor(android.graphics.Color.parseColor("#244C43"));

            LinearLayout.LayoutParams stepsParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            stepsParams.setMargins(dpToPx(8), 0, 0, 0);
            tvSteps.setLayoutParams(stepsParams);

            TextView tvKcal = new TextView(this);
            tvKcal.setText(String.format(Locale.US, "%d kcal", option.kcal));
            tvKcal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tvKcal.setTypeface(null, android.graphics.Typeface.BOLD);
            tvKcal.setTextColor(android.graphics.Color.parseColor("#315C55"));

            row.addView(cb);
            row.addView(tvSteps);
            row.addView(tvKcal);

            row.setOnClickListener(v -> {
                option.isSelected = !option.isSelected;
                cb.setChecked(option.isSelected);
                calculateTotalStepsAndCalories();
            });

            layoutPresetStepsContainer.addView(row);
        }
    }

    private void setupCustomInputListener() {
        etCustomSteps.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String valStr = s.toString().trim();
                if (!valStr.isEmpty()) {
                    try {
                        int customSteps = Integer.parseInt(valStr);
                        int customKcal = (int) Math.round(customSteps * 0.03); // 100 steps = 3 kcal
                        tvCustomCalories.setText(String.format(Locale.US, "= %d kcal", customKcal));
                    } catch (NumberFormatException e) {
                        tvCustomCalories.setText("= 0 kcal");
                    }
                } else {
                    tvCustomCalories.setText("= 0 kcal");
                }
                calculateTotalStepsAndCalories();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void calculateTotalStepsAndCalories() {
        int totalSteps = 0;
        int totalCalories = 0;

        // Custom steps
        String customStr = etCustomSteps.getText().toString().trim();
        if (!customStr.isEmpty()) {
            try {
                int cSteps = Integer.parseInt(customStr);
                totalSteps += cSteps;
                totalCalories += (int) Math.round(cSteps * 0.03);
            } catch (NumberFormatException ignored) {}
        }

        // Preset options
        for (StepOption option : presetOptions) {
            if (option.isSelected) {
                totalSteps += option.steps;
                totalCalories += option.kcal;
            }
        }

        tvTotalStepsCalories.setText(String.format(Locale.US, "Total Burned: %d kcal (%,d steps)", totalCalories, totalSteps));
    }

    private void saveStepsSelection() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int totalSteps = 0;
        int totalCalories = 0;

        SharedPreferences.Editor editor = prefs.edit();

        // Custom steps
        int customSteps = 0;
        String customStr = etCustomSteps.getText().toString().trim();
        if (!customStr.isEmpty()) {
            try {
                customSteps = Integer.parseInt(customStr);
                totalSteps += customSteps;
                totalCalories += (int) Math.round(customSteps * 0.03);
            } catch (NumberFormatException ignored) {}
        }
        editor.putInt(dateKey + "_steps_custom", customSteps);

        // Presets
        for (StepOption option : presetOptions) {
            editor.putBoolean(dateKey + "_step_preset_" + option.steps, option.isSelected);
            if (option.isSelected) {
                totalSteps += option.steps;
                totalCalories += option.kcal;
            }
        }

        editor.putInt(dateKey + "_steps_count", totalSteps);
        editor.putInt(dateKey + "_steps_cal", totalCalories);
        editor.apply();

        Toast.makeText(this, String.format(Locale.US, "Steps updated: %d kcal added to Energy Requirement!", totalCalories), Toast.LENGTH_SHORT).show();
        finish();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}