package com.susmita.parkicompanion.diet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.susmita.parkicompanion.R;

import java.util.Locale;

public class DietTrackerActivity extends AppCompatActivity {

    private EditText etAge;
    private Spinner spinnerSex;

    private Spinner spinnerHeightUnit;
    private EditText etHeightCm;
    private LinearLayout layoutHeightFtIn;
    private EditText etHeightFt;
    private EditText etHeightIn;
    private EditText etHeightInches;
    private EditText etHeightMeters;
    private LinearLayout layoutHeightConversion;
    private TextView tvHeightConverted;
    private Button btnConvertHeight;

    private Spinner spinnerWeightUnit;
    private EditText etWeightKg;
    private EditText etWeightLbs;
    private EditText etWeightSt;
    private LinearLayout layoutWeightConversion;
    private TextView tvWeightConverted;
    private Button btnConvertWeight;

    private Spinner spinnerActivityLevel;
    private Button btnCalculate;

    private LinearLayout cardResults;
    private TextView tvCalories;
    private TextView tvProtein;
    private TextView tvCarbs;
    private TextView tvFats;
    private TextView tvWater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_tracker);

        initViews();
        setupNavigation();
        setupSpinners();
        setupUnitListeners();
        setupCalculation();
    }

    private void initViews() {
        etAge = findViewById(R.id.etAge);
        spinnerSex = findViewById(R.id.spinnerSex);

        spinnerHeightUnit = findViewById(R.id.spinnerHeightUnit);
        etHeightCm = findViewById(R.id.etHeightCm);
        layoutHeightFtIn = findViewById(R.id.layoutHeightFtIn);
        etHeightFt = findViewById(R.id.etHeightFt);
        etHeightIn = findViewById(R.id.etHeightIn);
        etHeightInches = findViewById(R.id.etHeightInches);
        etHeightMeters = findViewById(R.id.etHeightMeters);
        layoutHeightConversion = findViewById(R.id.layoutHeightConversion);
        tvHeightConverted = findViewById(R.id.tvHeightConverted);
        btnConvertHeight = findViewById(R.id.btnConvertHeight);

        spinnerWeightUnit = findViewById(R.id.spinnerWeightUnit);
        etWeightKg = findViewById(R.id.etWeightKg);
        etWeightLbs = findViewById(R.id.etWeightLbs);
        etWeightSt = findViewById(R.id.etWeightSt);
        layoutWeightConversion = findViewById(R.id.layoutWeightConversion);
        tvWeightConverted = findViewById(R.id.tvWeightConverted);
        btnConvertWeight = findViewById(R.id.btnConvertWeight);

        spinnerActivityLevel = findViewById(R.id.spinnerActivityLevel);
        btnCalculate = findViewById(R.id.btnCalculate);

        cardResults = findViewById(R.id.cardResults);
        tvCalories = findViewById(R.id.tvCalories);
        tvProtein = findViewById(R.id.tvProtein);
        tvCarbs = findViewById(R.id.tvCarbs);
        tvFats = findViewById(R.id.tvFats);
        tvWater = findViewById(R.id.tvWater);
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

    private void setupSpinners() {
        // Sex Options
        String[] sexOptions = {"Select an option", "Male", "Female", "Other"};
        ArrayAdapter<String> sexAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sexOptions);
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSex.setAdapter(sexAdapter);

        // Height Unit Options
        String[] heightUnits = {"cm", "ft & in", "inches", "meters"};
        ArrayAdapter<String> heightUnitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, heightUnits);
        heightUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHeightUnit.setAdapter(heightUnitAdapter);

        // Weight Unit Options
        String[] weightUnits = {"kg", "lbs", "st (stones)"};
        ArrayAdapter<String> weightUnitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, weightUnits);
        weightUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWeightUnit.setAdapter(weightUnitAdapter);

        // Activity Level Options
        String[] activityOptions = {
                "Tell us how physically active you are per week",
                "Sedentary (little or no exercise)",
                "Lightly Active (1-3 days/week)",
                "Moderately Active (3-5 days/week)",
                "Very Active (6-7 days/week)",
                "Extra Active (intense exercise / job)"
        };
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activityOptions);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivityLevel.setAdapter(activityAdapter);
    }

    private void setupUnitListeners() {
        // Height unit spinner selection
        spinnerHeightUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateHeightInputVisibility(position);
                updateHeightConvertedValue();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        TextWatcher heightWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged() {
                updateHeightConvertedValue();
            }
        };
        etHeightCm.addTextChangedListener(heightWatcher);
        etHeightFt.addTextChangedListener(heightWatcher);
        etHeightIn.addTextChangedListener(heightWatcher);
        etHeightInches.addTextChangedListener(heightWatcher);
        etHeightMeters.addTextChangedListener(heightWatcher);

        btnConvertHeight.setOnClickListener(v -> {
            double cm = calculateHeightInCm();
            if (cm > 0) {
                spinnerHeightUnit.setSelection(0); // switch to cm
                etHeightCm.setText(String.format(Locale.US, "%.1f", cm));
                Toast.makeText(this, String.format(Locale.US, "Converted height to %.1f cm", cm), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter a valid height to convert.", Toast.LENGTH_SHORT).show();
            }
        });

        // Weight unit spinner selection
        spinnerWeightUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateWeightInputVisibility(position);
                updateWeightConvertedValue();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        TextWatcher weightWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged() {
                updateWeightConvertedValue();
            }
        };
        etWeightKg.addTextChangedListener(weightWatcher);
        etWeightLbs.addTextChangedListener(weightWatcher);
        etWeightSt.addTextChangedListener(weightWatcher);

        btnConvertWeight.setOnClickListener(v -> {
            double kg = calculateWeightInKg();
            if (kg > 0) {
                spinnerWeightUnit.setSelection(0); // switch to kg
                etWeightKg.setText(String.format(Locale.US, "%.1f", kg));
                Toast.makeText(this, String.format(Locale.US, "Converted weight to %.1f kg", kg), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter a valid weight to convert.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateHeightInputVisibility(int unitPosition) {
        etHeightCm.setVisibility(unitPosition == 0 ? View.VISIBLE : View.GONE);
        layoutHeightFtIn.setVisibility(unitPosition == 1 ? View.VISIBLE : View.GONE);
        etHeightInches.setVisibility(unitPosition == 2 ? View.VISIBLE : View.GONE);
        etHeightMeters.setVisibility(unitPosition == 3 ? View.VISIBLE : View.GONE);

        layoutHeightConversion.setVisibility(unitPosition == 0 ? View.GONE : View.VISIBLE);
    }

    private void updateWeightInputVisibility(int unitPosition) {
        etWeightKg.setVisibility(unitPosition == 0 ? View.VISIBLE : View.GONE);
        etWeightLbs.setVisibility(unitPosition == 1 ? View.VISIBLE : View.GONE);
        etWeightSt.setVisibility(unitPosition == 2 ? View.VISIBLE : View.GONE);

        layoutWeightConversion.setVisibility(unitPosition == 0 ? View.GONE : View.VISIBLE);
    }

    private void updateHeightConvertedValue() {
        int unitPosition = spinnerHeightUnit.getSelectedItemPosition();
        if (unitPosition == 0) return;

        double cm = calculateHeightInCm();
        if (cm > 0) {
            tvHeightConverted.setText(String.format(Locale.US, "Converted: %.1f cm", cm));
        } else {
            tvHeightConverted.setText("Converted: -- cm");
        }
    }

    private void updateWeightConvertedValue() {
        int unitPosition = spinnerWeightUnit.getSelectedItemPosition();
        if (unitPosition == 0) return;

        double kg = calculateWeightInKg();
        if (kg > 0) {
            tvWeightConverted.setText(String.format(Locale.US, "Converted: %.1f kg", kg));
        } else {
            tvWeightConverted.setText("Converted: -- kg");
        }
    }

    private double calculateHeightInCm() {
        int unitPosition = spinnerHeightUnit.getSelectedItemPosition();
        try {
            if (unitPosition == 0) { // cm
                String val = etHeightCm.getText().toString().trim();
                return val.isEmpty() ? 0 : Double.parseDouble(val);
            } else if (unitPosition == 1) { // ft & in
                String ftStr = etHeightFt.getText().toString().trim();
                String inStr = etHeightIn.getText().toString().trim();
                double ft = ftStr.isEmpty() ? 0 : Double.parseDouble(ftStr);
                double in = inStr.isEmpty() ? 0 : Double.parseDouble(inStr);
                if (ft == 0 && in == 0) return 0;
                return (ft * 12 + in) * 2.54;
            } else if (unitPosition == 2) { // inches
                String val = etHeightInches.getText().toString().trim();
                return val.isEmpty() ? 0 : Double.parseDouble(val) * 2.54;
            } else if (unitPosition == 3) { // meters
                String val = etHeightMeters.getText().toString().trim();
                return val.isEmpty() ? 0 : Double.parseDouble(val) * 100.0;
            }
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    private double calculateWeightInKg() {
        int unitPosition = spinnerWeightUnit.getSelectedItemPosition();
        try {
            if (unitPosition == 0) { // kg
                String val = etWeightKg.getText().toString().trim();
                return val.isEmpty() ? 0 : Double.parseDouble(val);
            } else if (unitPosition == 1) { // lbs
                String val = etWeightLbs.getText().toString().trim();
                return val.isEmpty() ? 0 : Double.parseDouble(val) * 0.453592;
            } else if (unitPosition == 2) { // st (stones)
                String val = etWeightSt.getText().toString().trim();
                return val.isEmpty() ? 0 : Double.parseDouble(val) * 6.35029;
            }
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    private void setupCalculation() {
        btnCalculate.setOnClickListener(v -> calculateNutritionalNeeds());
    }

    private void calculateNutritionalNeeds() {
        String ageStr = etAge.getText().toString().trim();
        if (ageStr.isEmpty()) {
            etAge.setError("Please enter your age");
            etAge.requestFocus();
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age <= 0 || age > 120) {
                etAge.setError("Please enter a valid age");
                etAge.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etAge.setError("Invalid age format");
            return;
        }

        if (spinnerSex.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select an option for Sex", Toast.LENGTH_SHORT).show();
            return;
        }

        double heightCm = calculateHeightInCm();
        if (heightCm <= 0) {
            Toast.makeText(this, "Please enter a valid Height", Toast.LENGTH_SHORT).show();
            return;
        }

        double weightKg = calculateWeightInKg();
        if (weightKg <= 0) {
            Toast.makeText(this, "Please enter a valid Weight", Toast.LENGTH_SHORT).show();
            return;
        }

        int activityPos = spinnerActivityLevel.getSelectedItemPosition();
        if (activityPos == 0) {
            Toast.makeText(this, "Please select an Activity level option", Toast.LENGTH_SHORT).show();
            return;
        }

        // BMR Calculation (Mifflin-St Jeor)
        boolean isMale = spinnerSex.getSelectedItemPosition() == 1;

        Intent intent = new Intent(DietTrackerActivity.this, DietDashboardActivity.class);
        intent.putExtra(DietDashboardActivity.EXTRA_AGE, age);
        intent.putExtra(DietDashboardActivity.EXTRA_SEX_MALE, isMale);
        intent.putExtra(DietDashboardActivity.EXTRA_HEIGHT_CM, heightCm);
        intent.putExtra(DietDashboardActivity.EXTRA_WEIGHT_KG, weightKg);
        startActivity(intent);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        public abstract void onTextChanged();

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onTextChanged();
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}