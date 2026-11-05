package com.susmita.parkicompanion.diet.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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

public class DietFoodItemsActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_NAME = "extra_category_name";
    public static final String EXTRA_MEAL_TYPE = "extra_meal_type";
    public static final String EXTRA_DATE_KEY = "extra_date_key";
    private static final String PREFS_NAME = "diet_tracker_prefs";

    private String categoryName = "Beans & Peas";
    private String mealType = "Breakfast";
    private String dateKey = "default_date";

    private TextView tvHeaderTitle;
    private EditText etSearchItem;
    private LinearLayout layoutFoodItemsContainer;
    private TextView tvTotalItemCalories;

    private static class FoodItem {
        final String name;
        final int cals;
        final String serving;
        boolean isChecked;
        CheckBox checkBox;

        FoodItem(String name, int cals, String serving) {
            this.name = name;
            this.cals = cals;
            this.serving = serving;
            this.isChecked = false;
        }
    }

    private final List<FoodItem> foodItemsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_food_items);

        if (getIntent().hasExtra(EXTRA_CATEGORY_NAME)) {
            categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        }
        if (getIntent().hasExtra(EXTRA_MEAL_TYPE)) {
            mealType = getIntent().getStringExtra(EXTRA_MEAL_TYPE);
        }
        if (getIntent().hasExtra(EXTRA_DATE_KEY)) {
            dateKey = getIntent().getStringExtra(EXTRA_DATE_KEY);
        }

        initBeansAndPeasItems();
        initViews();
        setupNavigation();
        loadSavedItemState();
        populateItemsList("");
        setupSearchListener();
        calculateTotalCalories();
    }

    private void initBeansAndPeasItems() {
        foodItemsList.clear();
        foodItemsList.add(new FoodItem("Cannellini or white kidney beans canned no salt added, Generic", 100, "0.5 cup"));
        foodItemsList.add(new FoodItem("Chickpeas or garbanzo beans boiled with salt, Generic", 269, "cup"));
        foodItemsList.add(new FoodItem("Chickpeas or garbanzo beans boiled without salt, Generic", 269, "cup"));
        foodItemsList.add(new FoodItem("Chickpeas or garbanzo beans canned, Generic", 353, "can drained, rinsed"));
        foodItemsList.add(new FoodItem("Chickpeas or garbanzo beans canned no salt added, Generic", 181, "0.5 cup"));
        foodItemsList.add(new FoodItem("Chickpeas or garbanzo beans raw, Generic", 756, "cup"));
        foodItemsList.add(new FoodItem("Edamame dry roasted, Generic", 129, "0.33 cup"));
        foodItemsList.add(new FoodItem("Edamame frozen unprepared, Generic", 129, "cup"));
        foodItemsList.add(new FoodItem("Fava beans boiled without salt, Generic", 187, "cup"));
        foodItemsList.add(new FoodItem("Fava beans raw, Generic", 512, "cup"));
        foodItemsList.add(new FoodItem("Hummus, Generic", 583, "cup"));
        foodItemsList.add(new FoodItem("Kidney beans boiled without salt, Generic", 225, "cup"));
        foodItemsList.add(new FoodItem("Kidney beans canned, Generic", 330, "can drained solids"));
        foodItemsList.add(new FoodItem("Lentils French green raw, Generic", 676, "cup"));
        foodItemsList.add(new FoodItem("Lentils black beluga raw, Generic", 676, "cup"));
        foodItemsList.add(new FoodItem("Lentils boiled without salt, Generic", 230, "cup"));
        foodItemsList.add(new FoodItem("Lentils raw, Generic", 676, "cup"));
        foodItemsList.add(new FoodItem("Lima beans large boiled without salt, Generic", 216, "cup"));
        foodItemsList.add(new FoodItem("Lima beans large canned, Generic", 190, "cup"));
        foodItemsList.add(new FoodItem("Miso paste white, Generic", 545, "cup"));
        foodItemsList.add(new FoodItem("Mung beans sprouted canned, Generic", 15, "cup"));
        foodItemsList.add(new FoodItem("Natto, Generic", 369, "cup"));
        foodItemsList.add(new FoodItem("Pinto beans boiled without salt, Generic", 245, "cup"));
        foodItemsList.add(new FoodItem("Pinto beans canned, Generic", 316, "can drained solids"));
        foodItemsList.add(new FoodItem("Pinto beans raw, Generic", 670, "cup"));
        foodItemsList.add(new FoodItem("Refried beans traditional canned, Generic", 227, "cup"));
        foodItemsList.add(new FoodItem("Refried beans vegetarian canned, Generic", 191, "cup"));
        foodItemsList.add(new FoodItem("Split peas boiled without salt, Generic", 231, "cup"));
        foodItemsList.add(new FoodItem("Split peas raw, Generic", 717, "cup"));
        foodItemsList.add(new FoodItem("Tempeh cooked, Generic", 195, "serving"));
        foodItemsList.add(new FoodItem("Tempeh uncooked, Generic", 319, "cup"));
        foodItemsList.add(new FoodItem("Tofu extra firm made with nigari, Generic", 76, "0.2 block"));
        foodItemsList.add(new FoodItem("Tofu firm made with calcium sulfate, Generic", 181, "0.5 cup"));
        foodItemsList.add(new FoodItem("Tofu firm made with nigari, Generic", 98, "0.5 cup"));
        foodItemsList.add(new FoodItem("Tofu regular made with calcium sulfate, Generic", 94, "0.5 cup"));
        foodItemsList.add(new FoodItem("Tofu soft made with nigari, Generic", 151, "cup"));
        foodItemsList.add(new FoodItem("Tofu soft silken, Generic", 42, "0.2 block"));
    }

    private void initViews() {
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        etSearchItem = findViewById(R.id.etSearchItem);
        layoutFoodItemsContainer = findViewById(R.id.layoutFoodItemsContainer);
        tvTotalItemCalories = findViewById(R.id.tvTotalItemCalories);

        Button btnSaveSelectedItems = findViewById(R.id.btnSaveSelectedItems);

        tvHeaderTitle.setText(categoryName);
        btnSaveSelectedItems.setText("Add Selected Foods to " + mealType);

        btnSaveSelectedItems.setOnClickListener(v -> saveSelectedItems());
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

    private void loadSavedItemState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        for (int i = 0; i < foodItemsList.size(); i++) {
            FoodItem item = foodItemsList.get(i);
            item.isChecked = prefs.getBoolean(dateKey + "_" + mealType + "_item_" + i, false);
        }
    }

    private void populateItemsList(String filter) {
        layoutFoodItemsContainer.removeAllViews();

        String query = filter.trim().toLowerCase(Locale.US);

        for (FoodItem item : foodItemsList) {
            if (!query.isEmpty() && !item.name.toLowerCase(Locale.US).contains(query)) {
                continue;
            }

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
            cb.setChecked(item.isChecked);
            item.checkBox = cb;

            LinearLayout textLayout = new LinearLayout(this);
            textLayout.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            textParams.setMargins(dpToPx(10), 0, 0, 0);
            textLayout.setLayoutParams(textParams);

            TextView tvName = new TextView(this);
            tvName.setText(item.name);
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(Color.parseColor("#244C43"));

            TextView tvCal = new TextView(this);
            tvCal.setText(String.format(Locale.US, "%d cals / %s", item.cals, item.serving));
            tvCal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvCal.setTextColor(Color.parseColor("#2D8A68"));

            textLayout.addView(tvName);
            textLayout.addView(tvCal);

            row.addView(cb);
            row.addView(textLayout);

            row.setOnClickListener(v -> {
                item.isChecked = !item.isChecked;
                cb.setChecked(item.isChecked);
                calculateTotalCalories();
            });

            layoutFoodItemsContainer.addView(row);
        }
    }

    private void setupSearchListener() {
        etSearchItem.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                populateItemsList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private int calculateTotalCalories() {
        int selectedCount = 0;
        int totalCals = 0;

        for (FoodItem item : foodItemsList) {
            if (item.isChecked) {
                selectedCount++;
                totalCals += item.cals;
            }
        }

        tvTotalItemCalories.setText(String.format(Locale.US, "Selected: %d items | Total: %d cals", selectedCount, totalCals));
        return totalCals;
    }

    private void saveSelectedItems() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int selectedCals = calculateTotalCalories();

        for (int i = 0; i < foodItemsList.size(); i++) {
            FoodItem item = foodItemsList.get(i);
            editor.putBoolean(dateKey + "_" + mealType + "_item_" + i, item.isChecked);
        }

        // Add this item total to meal total
        String mealPrefKey = dateKey + "_" + mealType.toLowerCase(Locale.US) + "_cal";
        int previousMealCal = prefs.getInt(mealPrefKey, 0);
        int updatedMealCal = previousMealCal + selectedCals;

        editor.putInt(mealPrefKey, updatedMealCal);
        editor.apply();

        Toast.makeText(this, String.format(Locale.US, "Added %d cals from %s to %s!", selectedCals, categoryName, mealType), Toast.LENGTH_SHORT).show();
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