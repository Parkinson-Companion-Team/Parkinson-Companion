package com.susmita.parkicompanion.diet.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.susmita.parkicompanion.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DietDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_AGE = "extra_age";
    public static final String EXTRA_SEX_MALE = "extra_sex_male";
    public static final String EXTRA_HEIGHT_CM = "extra_height_cm";
    public static final String EXTRA_WEIGHT_KG = "extra_weight_kg";

    private static final String PREFS_NAME = "diet_tracker_prefs";

    private int userAge = 30;
    private boolean userIsMale = true;
    private double userHeightCm = 170.0;
    private double userWeightKg = 70.0;
    private double userRee = 1550.0;

    private TextView tvEnergyRequirementValue;
    private TextView tvCalorieSummary;

    private TextView tvExerciseValue;
    private TextView tvStepsValue;
    private TextView tvReeValue;

    private TextView tvBreakfastValue;
    private TextView tvLunchValue;
    private TextView tvDinnerValue;
    private TextView tvSnacksValue;
    private TextView tvWaterValue;

    private LinearLayout layoutDatesContainer;
    private Button btnMonthlyReport;

    private final List<Date> dateList = new ArrayList<>();
    private int selectedDateIndex = 0;
    private final List<LinearLayout> dateCardViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_dashboard);

        loadUserProfile();
        initViews();
        setupNavigation();
        setupDatesList();
        setupClickListeners();

        loadDateData(getSelectedDateKey());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDateData(getSelectedDateKey());
    }

    private void loadUserProfile() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        userAge = getIntent().getIntExtra(EXTRA_AGE, prefs.getInt("user_age", 30));
        userIsMale = getIntent().getBooleanExtra(EXTRA_SEX_MALE, prefs.getBoolean("user_sex_male", true));
        userHeightCm = getIntent().getDoubleExtra(EXTRA_HEIGHT_CM, (double) prefs.getFloat("user_height_cm", 170.0f));
        userWeightKg = getIntent().getDoubleExtra(EXTRA_WEIGHT_KG, (double) prefs.getFloat("user_weight_kg", 70.0f));

        // Calculate REE
        // Male = 10 * weight + 6.25 * height - 5 * age + 5
        // Female = 10 * weight + 6.25 * height - 5 * age - 161
        if (userIsMale) {
            userRee = (10 * userWeightKg) + (6.25 * userHeightCm) - (5 * userAge) + 5;
        } else {
            userRee = (10 * userWeightKg) + (6.25 * userHeightCm) - (5 * userAge) - 161;
        }

        // Save profile in prefs
        prefs.edit()
                .putInt("user_age", userAge)
                .putBoolean("user_sex_male", userIsMale)
                .putFloat("user_height_cm", (float) userHeightCm)
                .putFloat("user_weight_kg", (float) userWeightKg)
                .apply();
    }

    private void initViews() {
        tvEnergyRequirementValue = findViewById(R.id.tvEnergyRequirementValue);
        tvCalorieSummary = findViewById(R.id.tvCalorieSummary);

        tvExerciseValue = findViewById(R.id.tvExerciseValue);
        tvStepsValue = findViewById(R.id.tvStepsValue);
        tvReeValue = findViewById(R.id.tvReeValue);

        tvBreakfastValue = findViewById(R.id.tvBreakfastValue);
        tvLunchValue = findViewById(R.id.tvLunchValue);
        tvDinnerValue = findViewById(R.id.tvDinnerValue);
        tvSnacksValue = findViewById(R.id.tvSnacksValue);
        tvWaterValue = findViewById(R.id.tvWaterValue);

        layoutDatesContainer = findViewById(R.id.layoutDatesContainer);
        btnMonthlyReport = findViewById(R.id.btnMonthlyReport);
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

    private void setupDatesList() {
        dateList.clear();
        dateCardViews.clear();
        layoutDatesContainer.removeAllViews();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6); // Show past 6 days up to next 7 days

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.US); // e.g. Sa, Sun, Mo
        SimpleDateFormat numFormat = new SimpleDateFormat("d", Locale.US);   // e.g. 2, 3, 4

        int todayIndex = 6; // index of today in 14 days range

        for (int i = 0; i < 14; i++) {
            Date date = cal.getTime();
            dateList.add(date);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);

            int paddingPx = dpToPx(10);
            card.setPadding(paddingPx, dpToPx(8), paddingPx, dpToPx(8));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(56),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            card.setLayoutParams(params);

            TextView tvDay = new TextView(this);
            String dayStr = dayFormat.format(date);
            if (dayStr.length() > 3) dayStr = dayStr.substring(0, 3);
            tvDay.setText(dayStr);
            tvDay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvDay.setGravity(Gravity.CENTER);

            TextView tvNum = new TextView(this);
            tvNum.setText(numFormat.format(date));
            tvNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tvNum.setTypeface(null, android.graphics.Typeface.BOLD);
            tvNum.setGravity(Gravity.CENTER);

            card.addView(tvDay);
            card.addView(tvNum);

            final int index = i;
            card.setOnClickListener(v -> selectDateIndex(index));

            dateCardViews.add(card);
            layoutDatesContainer.addView(card);

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        selectDateIndex(todayIndex);
    }

    private void selectDateIndex(int index) {
        selectedDateIndex = index;
        for (int i = 0; i < dateCardViews.size(); i++) {
            LinearLayout card = dateCardViews.get(i);
            TextView tvDay = (TextView) card.getChildAt(0);
            TextView tvNum = (TextView) card.getChildAt(1);

            if (i == index) {
                card.setBackgroundResource(R.drawable.date_item_selected);
                tvDay.setTextColor(Color.WHITE);
                tvNum.setTextColor(Color.WHITE);
            } else {
                card.setBackgroundResource(R.drawable.date_item_unselected);
                tvDay.setTextColor(Color.parseColor("#606B66"));
                tvNum.setTextColor(Color.parseColor("#244C43"));
            }
        }

        loadDateData(getSelectedDateKey());
    }

    private String getSelectedDateKey() {
        if (selectedDateIndex >= 0 && selectedDateIndex < dateList.size()) {
            SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy_MM_dd", Locale.US);
            return keyFormat.format(dateList.get(selectedDateIndex));
        }
        return "default_date";
    }

    private void setupClickListeners() {
        // Energy Requirement Box Click -> Show Formula Dialog
        View cardEnergyRequirement = findViewById(R.id.cardEnergyRequirement);
        if (cardEnergyRequirement != null) {
            cardEnergyRequirement.setOnClickListener(v -> showFormulaDialog());
        }

        // Left Column items
        View cardExercise = findViewById(R.id.cardExercise);
        if (cardExercise != null) {
            cardExercise.setOnClickListener(v -> {
                Intent intent = new Intent(DietDashboardActivity.this, DietExerciseLogActivity.class);
                intent.putExtra(DietExerciseLogActivity.EXTRA_DATE_KEY, getSelectedDateKey());
                startActivity(intent);
            });
        }

        View cardSteps = findViewById(R.id.cardSteps);
        if (cardSteps != null) {
            cardSteps.setOnClickListener(v -> {
                Intent intent = new Intent(DietDashboardActivity.this, DietStepsLogActivity.class);
                intent.putExtra(DietStepsLogActivity.EXTRA_DATE_KEY, getSelectedDateKey());
                startActivity(intent);
            });
        }

        View cardRee = findViewById(R.id.cardRee);
        if (cardRee != null) {
            cardRee.setOnClickListener(v -> showReeInfoDialog());
        }

        // Right Column items (Meals)
        View cardBreakfast = findViewById(R.id.cardBreakfast);
        if (cardBreakfast != null) {
            cardBreakfast.setOnClickListener(v -> openFoodLog("Breakfast"));
        }

        View cardLunch = findViewById(R.id.cardLunch);
        if (cardLunch != null) {
            cardLunch.setOnClickListener(v -> openFoodLog("Lunch"));
        }

        View cardDinner = findViewById(R.id.cardDinner);
        if (cardDinner != null) {
            cardDinner.setOnClickListener(v -> openFoodLog("Dinner"));
        }

        View cardSnacks = findViewById(R.id.cardSnacks);
        if (cardSnacks != null) {
            cardSnacks.setOnClickListener(v -> openFoodLog("Snacks"));
        }

        View cardWater = findViewById(R.id.cardWater);
        if (cardWater != null) {
            cardWater.setOnClickListener(v -> showInputDialog("Water Intake", "Enter Water Consumed in ml (e.g., 250 ml = 1 glass):", "water_ml", "ml"));
        }

        // Monthly Nutrition Report
        btnMonthlyReport.setOnClickListener(v -> showMonthlyReportDialog());
    }

    private void openFoodLog(String mealType) {
        Intent intent = new Intent(DietDashboardActivity.this, DietFoodLogActivity.class);
        intent.putExtra(DietFoodLogActivity.EXTRA_MEAL_TYPE, mealType);
        intent.putExtra(DietFoodLogActivity.EXTRA_DATE_KEY, getSelectedDateKey());
        startActivity(intent);
    }

    private void loadDateData(String dateKey) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int exerciseCal = prefs.getInt(dateKey + "_exercise_cal", 0);
        int stepsCal = prefs.getInt(dateKey + "_steps_cal", 0);

        int breakfastCal = prefs.getInt(dateKey + "_breakfast_cal", 0);
        int lunchCal = prefs.getInt(dateKey + "_lunch_cal", 0);
        int dinnerCal = prefs.getInt(dateKey + "_dinner_cal", 0);
        int snacksCal = prefs.getInt(dateKey + "_snacks_cal", 0);
        int waterMl = prefs.getInt(dateKey + "_water_ml", 0);

        // Update Item UI
        tvExerciseValue.setText(String.format(Locale.US, "%d cal", exerciseCal));
        tvStepsValue.setText(String.format(Locale.US, "%d cal", stepsCal));
        tvReeValue.setText(String.format(Locale.US, "%.0f cal", userRee));

        tvBreakfastValue.setText(String.format(Locale.US, "%d cal", breakfastCal));
        tvLunchValue.setText(String.format(Locale.US, "%d cal", lunchCal));
        tvDinnerValue.setText(String.format(Locale.US, "%d cal", dinnerCal));
        tvSnacksValue.setText(String.format(Locale.US, "%d cal", snacksCal));
        tvWaterValue.setText(String.format(Locale.US, "%d ml", waterMl));

        // Calculations
        // Base Energy Requirement = REE + Daily Movement (150cal) + Thermic Effect of Food (150cal)
        double baseRequirement = userRee + 150.0 + 150.0;

        // Exercise & Steps add up to Energy Requirement
        double addedActivity = exerciseCal + stepsCal;

        // Meals & Snacks subtract from Energy Requirement
        int totalConsumed = breakfastCal + lunchCal + dinnerCal + snacksCal;

        double currentEnergyRequirement = baseRequirement + addedActivity - totalConsumed;

        tvEnergyRequirementValue.setText(String.format(Locale.US, "%.0f cal", currentEnergyRequirement));
        tvCalorieSummary.setText(String.format(Locale.US, "Consumed: %d cal | Remaining: %.0f cal", totalConsumed, currentEnergyRequirement));
    }

    private void showInputDialog(String title, String message, String keySuffix, String unit) {
        String dateKey = getSelectedDateKey();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentVal = prefs.getInt(dateKey + "_" + keySuffix, 0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (currentVal > 0) {
            input.setText(String.valueOf(currentVal));
            input.setSelection(input.getText().length());
        } else {
            input.setHint("e.g., 250");
        }
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String valStr = input.getText().toString().trim();
            int newVal = 0;
            if (!valStr.isEmpty()) {
                try {
                    newVal = Integer.parseInt(valStr);
                } catch (NumberFormatException ignored) {}
            }
            prefs.edit().putInt(dateKey + "_" + keySuffix, newVal).apply();
            loadDateData(dateKey);
            Toast.makeText(this, title + " updated to " + newVal + " " + unit, Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showStepsDialog() {
        String dateKey = getSelectedDateKey();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentCal = prefs.getInt(dateKey + "_steps_cal", 0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Steps");
        builder.setMessage("Enter step count (approx. 25 steps = 1 cal) OR enter burned calories directly:");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10));

        final EditText inputSteps = new EditText(this);
        inputSteps.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputSteps.setHint("Steps count (e.g. 5000)");

        final EditText inputCal = new EditText(this);
        inputCal.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputCal.setHint("Direct calories (e.g. 200 cal)");
        if (currentCal > 0) {
            inputCal.setText(String.valueOf(currentCal));
        }

        layout.addView(inputSteps);
        layout.addView(inputCal);
        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            int stepsCal = 0;
            String calStr = inputCal.getText().toString().trim();
            String stepsStr = inputSteps.getText().toString().trim();

            if (!calStr.isEmpty()) {
                try {
                    stepsCal = Integer.parseInt(calStr);
                } catch (NumberFormatException ignored) {}
            } else if (!stepsStr.isEmpty()) {
                try {
                    int steps = Integer.parseInt(stepsStr);
                    stepsCal = (int) Math.round(steps * 0.04); // 25 steps = ~1 cal
                } catch (NumberFormatException ignored) {}
            }

            prefs.edit().putInt(dateKey + "_steps_cal", stepsCal).apply();
            loadDateData(dateKey);
            Toast.makeText(this, "Steps updated (" + stepsCal + " cal)", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showFormulaDialog() {
        String dateKey = getSelectedDateKey();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int exerciseCal = prefs.getInt(dateKey + "_exercise_cal", 0);
        int stepsCal = prefs.getInt(dateKey + "_steps_cal", 0);
        int breakfastCal = prefs.getInt(dateKey + "_breakfast_cal", 0);
        int lunchCal = prefs.getInt(dateKey + "_lunch_cal", 0);
        int dinnerCal = prefs.getInt(dateKey + "_dinner_cal", 0);
        int snacksCal = prefs.getInt(dateKey + "_snacks_cal", 0);

        double baseRequirement = userRee + 150.0 + 150.0;
        int totalConsumed = breakfastCal + lunchCal + dinnerCal + snacksCal;
        int totalAddedActivity = exerciseCal + stepsCal;
        double netRemaining = baseRequirement + totalAddedActivity - totalConsumed;

        String formulaText = String.format(
                Locale.US,
                "📐 ENERGY REQUIREMENT FORMULA:\n\n" +
                        "1️⃣ Base Energy Requirement = REE + Daily Movement + Thermic Effect of Food\n" +
                        "• REE (Resting Energy Expenditure): %.0f cal\n" +
                        "   Formula (%s):\n" +
                        "   REE = (10 × %.1f kg) + (6.25 × %.1f cm) - (5 × %d yrs) %s\n" +
                        "• Daily Movement: +150 cal\n" +
                        "• Thermic Effect of Food: +150 cal\n" +
                        "➜ Base Target = %.0f cal\n\n" +
                        "2️⃣ Daily Activity Adjustments:\n" +
                        "• Added Exercise: +%d cal\n" +
                        "• Added Steps: +%d cal\n\n" +
                        "3️⃣ Meals Consumed:\n" +
                        "• Breakfast: -%d cal\n" +
                        "• Lunch: -%d cal\n" +
                        "• Dinner: -%d cal\n" +
                        "• Snacks: -%d cal\n" +
                        "➜ Total Consumed = -%d cal\n\n" +
                        "===============================\n" +
                        "⚡ Current Energy Requirement = %.0f cal",
                userRee,
                userIsMale ? "Male" : "Female",
                userWeightKg, userHeightCm, userAge,
                userIsMale ? "+ 5" : "- 161",
                baseRequirement,
                exerciseCal, stepsCal,
                breakfastCal, lunchCal, dinnerCal, snacksCal, totalConsumed,
                netRemaining
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚡ Energy Requirement Formula");
        builder.setMessage(formulaText);
        builder.setPositiveButton("Got It!", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showReeInfoDialog() {
        String reeFormula = userIsMale ?
                "(10 × weight in kg) + (6.25 × height in cm) - (5 × age in years) + 5" :
                "(10 × weight in kg) + (6.25 × height in cm) - (5 × age in years) - 161";

        String msg = String.format(
                Locale.US,
                "Resting Energy Expenditure (REE) is the baseline calories your body needs at complete rest.\n\n" +
                        "Formula (%s):\n%s\n\n" +
                        "Your Current Parameters:\n" +
                        "• Gender: %s\n" +
                        "• Age: %d years\n" +
                        "• Height: %.1f cm\n" +
                        "• Weight: %.1f kg\n\n" +
                        "Calculated REE = %.0f cal / day",
                userIsMale ? "Male" : "Female",
                reeFormula,
                userIsMale ? "Male" : "Female",
                userAge, userHeightCm, userWeightKg, userRee
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("REE (Resting Energy Expenditure)");
        builder.setMessage(msg);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showMonthlyReportDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int totalDaysLogged = 0;
        int totalCaloriesConsumed = 0;
        int totalExerciseCal = 0;
        int totalWaterMl = 0;

        // Iterate date keys
        for (Date date : dateList) {
            SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy_MM_dd", Locale.US);
            String dateKey = keyFormat.format(date);

            int b = prefs.getInt(dateKey + "_breakfast_cal", 0);
            int l = prefs.getInt(dateKey + "_lunch_cal", 0);
            int d = prefs.getInt(dateKey + "_dinner_cal", 0);
            int s = prefs.getInt(dateKey + "_snacks_cal", 0);
            int ex = prefs.getInt(dateKey + "_exercise_cal", 0);
            int w = prefs.getInt(dateKey + "_water_ml", 0);

            int dayConsumed = b + l + d + s;
            if (dayConsumed > 0 || ex > 0 || w > 0) {
                totalDaysLogged++;
                totalCaloriesConsumed += dayConsumed;
                totalExerciseCal += ex;
                totalWaterMl += w;
            }
        }

        int avgCalories = totalDaysLogged > 0 ? totalCaloriesConsumed / totalDaysLogged : 0;
        double avgWaterLiters = totalDaysLogged > 0 ? (totalWaterMl / 1000.0) / totalDaysLogged : 0.0;

        String reportMsg = String.format(
                Locale.US,
                "📅 MONTHLY NUTRITION REPORT\n\n" +
                        "• Total Active Logging Days: %d days\n" +
                        "• Baseline Daily REE Target: %.0f cal\n" +
                        "• Average Daily Consumption: %d cal / day\n" +
                        "• Total Active Exercise Burned: %d cal\n" +
                        "• Average Daily Hydration: %.1f Liters / day\n\n" +
                        "💡 Parkinson's Health Insights:\n" +
                        "• Consistent calorie intake supports stable motor control and energy levels.\n" +
                        "• Remember to schedule protein intake at least 1 hour away from Levodopa medications.\n" +
                        "• Keep daily hydration above 2.0L to prevent constipation and fatigue.",
                totalDaysLogged, userRee, avgCalories, totalExerciseCal, avgWaterLiters
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📊 Monthly Nutrition Report");
        builder.setMessage(reportMsg);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}