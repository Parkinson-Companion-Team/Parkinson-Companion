package com.susmita.parkicompanion.diet.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.susmita.parkicompanion.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DietFoodLogActivity extends AppCompatActivity {

    public static final String EXTRA_MEAL_TYPE = "extra_meal_type"; // e.g. "Breakfast", "Lunch", "Dinner", "Snacks"
    public static final String EXTRA_DATE_KEY = "extra_date_key";
    private static final String PREFS_NAME = "diet_tracker_prefs";

    private String mealType = "Breakfast";
    private String dateKey = "default_date";

    private TextView tvHeaderTitle;
    private EditText etFoodSearch;
    private LinearLayout layoutFoodCategoriesContainer;
    private TextView tvTotalMealCalories;
    private Button btnSaveMeal;

    private int currentMealCalories = 0;

    private static class StapleCategory {
        final String emoji;
        final String name;
        final int defaultCalories;

        StapleCategory(String emoji, String name, int defaultCalories) {
            this.emoji = emoji;
            this.name = name;
            this.defaultCalories = defaultCalories;
        }
    }

    private final List<StapleCategory> categories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_food_log);

        if (getIntent().hasExtra(EXTRA_MEAL_TYPE)) {
            mealType = getIntent().getStringExtra(EXTRA_MEAL_TYPE);
        }
        if (getIntent().hasExtra(EXTRA_DATE_KEY)) {
            dateKey = getIntent().getStringExtra(EXTRA_DATE_KEY);
        }

        initCategoriesData();
        initViews();
        setupNavigation();
        loadSavedMealData();
        populateCategoriesList("");
        setupSearchListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedMealData();
    }

    private void initCategoriesData() {
        categories.clear();
        categories.add(new StapleCategory("🫘", "Beans & Peas", 150));
        categories.add(new StapleCategory("🥛", "Beverages", 100));
        categories.add(new StapleCategory("🍞", "Breads & Rolls", 120));
        categories.add(new StapleCategory("🧀", "Cheese", 110));
        categories.add(new StapleCategory("🧴", "Condiments, Dressings & Sauces", 50));
        categories.add(new StapleCategory("🥚", "Dairy, Dairy Substitutes & Egg", 140));
        categories.add(new StapleCategory("🥑", "Fats & Oils", 90));
        categories.add(new StapleCategory("🐟", "Fish & Seafood", 180));
        categories.add(new StapleCategory("🧃", "Fruit - canned, dried, or juice", 100));
        categories.add(new StapleCategory("🍎", "Fruit - raw or frozen", 80));
        categories.add(new StapleCategory("🌾", "Grains & Grain Products", 160));
        categories.add(new StapleCategory("🥩", "Meats", 220));
        categories.add(new StapleCategory("🥜", "Nuts & Seeds", 170));
        categories.add(new StapleCategory("🍗", "Poultry", 190));
        categories.add(new StapleCategory("🧂", "Spices & Herbs", 10));
        categories.add(new StapleCategory("🍲", "Stocks and Gravy", 40));
        categories.add(new StapleCategory("🍫", "Sweets & Sweeteners", 130));
        categories.add(new StapleCategory("🧃", "Vegetables - canned, dried, or juice", 60));
        categories.add(new StapleCategory("🥗", "Vegetables - raw, frozen, or cooked", 50));
    }

    private void initViews() {
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        etFoodSearch = findViewById(R.id.etFoodSearch);
        layoutFoodCategoriesContainer = findViewById(R.id.layoutFoodCategoriesContainer);
        tvTotalMealCalories = findViewById(R.id.tvTotalMealCalories);
        btnSaveMeal = findViewById(R.id.btnSaveMeal);

        tvHeaderTitle.setText("Log " + mealType);
        btnSaveMeal.setText("Save to " + mealType);

        btnSaveMeal.setOnClickListener(v -> saveMealData());
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

    private String getMealPrefKey() {
        return dateKey + "_" + mealType.toLowerCase(Locale.US) + "_cal";
    }

    private void loadSavedMealData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentMealCalories = prefs.getInt(getMealPrefKey(), 0);
        updateTotalDisplay();
    }

    private void updateTotalDisplay() {
        tvTotalMealCalories.setText(String.format(Locale.US, "Total %s Calories: %d kcal", mealType, currentMealCalories));
    }

    private void populateCategoriesList(String filter) {
        layoutFoodCategoriesContainer.removeAllViews();

        // Custom Calorie Entry Button / Card
        LinearLayout customCard = new LinearLayout(this);
        customCard.setOrientation(LinearLayout.HORIZONTAL);
        customCard.setGravity(Gravity.CENTER_VERTICAL);
        customCard.setBackgroundResource(R.drawable.box_primary_bg);
        customCard.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));

        LinearLayout.LayoutParams customParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        customParams.setMargins(0, 0, 0, dpToPx(16));
        customCard.setLayoutParams(customParams);

        TextView tvCustomIcon = new TextView(this);
        tvCustomIcon.setText("✏️");
        tvCustomIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        TextView tvCustomTitle = new TextView(this);
        tvCustomTitle.setText("Enter Custom Calories for " + mealType);
        tvCustomTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvCustomTitle.setTypeface(null, Typeface.BOLD);
        tvCustomTitle.setTextColor(Color.parseColor("#244C43"));

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        titleParams.setMargins(dpToPx(10), 0, 0, 0);
        tvCustomTitle.setLayoutParams(titleParams);

        customCard.addView(tvCustomIcon);
        customCard.addView(tvCustomTitle);

        customCard.setOnClickListener(v -> showCustomCalorieDialog());
        layoutFoodCategoriesContainer.addView(customCard);

        // Section Title
        TextView tvSection = new TextView(this);
        tvSection.setText("Select Staples & Food Categories:");
        tvSection.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvSection.setTypeface(null, Typeface.BOLD);
        tvSection.setTextColor(Color.parseColor("#244C43"));
        tvSection.setPadding(0, 0, 0, dpToPx(10));
        layoutFoodCategoriesContainer.addView(tvSection);

        String query = filter.trim().toLowerCase(Locale.US);

        for (StapleCategory cat : categories) {
            if (!query.isEmpty() && !cat.name.toLowerCase(Locale.US).contains(query)) {
                continue;
            }

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.edit_text_border);
            row.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, dpToPx(10));
            row.setLayoutParams(params);

            TextView tvEmoji = new TextView(this);
            tvEmoji.setText(cat.emoji);
            tvEmoji.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);

            TextView tvName = new TextView(this);
            tvName.setText(cat.name);
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tvName.setTextColor(Color.parseColor("#244C43"));

            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            nameParams.setMargins(dpToPx(12), 0, 0, 0);
            tvName.setLayoutParams(nameParams);

            row.addView(tvEmoji);
            row.addView(tvName);

            row.setOnClickListener(v -> {
                Intent intent = new Intent(DietFoodLogActivity.this, DietFoodItemsActivity.class);
                intent.putExtra(DietFoodItemsActivity.EXTRA_CATEGORY_NAME, cat.name);
                intent.putExtra(DietFoodItemsActivity.EXTRA_MEAL_TYPE, mealType);
                intent.putExtra(DietFoodItemsActivity.EXTRA_DATE_KEY, dateKey);
                startActivity(intent);
            });

            layoutFoodCategoriesContainer.addView(row);
        }
    }

    private void setupSearchListener() {
        etFoodSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                populateCategoriesList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showCustomCalorieDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Total " + mealType + " Calories");
        builder.setMessage("Enter total calories consumed for " + mealType + ":");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (currentMealCalories > 0) {
            input.setText(String.valueOf(currentMealCalories));
            input.setSelection(input.getText().length());
        } else {
            input.setHint("e.g., 350");
        }
        builder.setView(input);

        builder.setPositiveButton("Set", (dialog, which) -> {
            String valStr = input.getText().toString().trim();
            int setCal = 0;
            if (!valStr.isEmpty()) {
                try {
                    setCal = Integer.parseInt(valStr);
                } catch (NumberFormatException ignored) {}
            }
            currentMealCalories = setCal;
            updateTotalDisplay();
            Toast.makeText(this, String.format(Locale.US, "%s calories set to %d kcal", mealType, currentMealCalories), Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void saveMealData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(getMealPrefKey(), currentMealCalories).apply();

        Toast.makeText(this, String.format(Locale.US, "%s updated to %d kcal!", mealType, currentMealCalories), Toast.LENGTH_SHORT).show();
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