package com.susmita.parkicompanion.exercise.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.exercise.model.ExerciseCategory;
import com.susmita.parkicompanion.exercise.repository.ExerciseDataRepository;

import java.util.ArrayList;
import java.util.List;

public class ExerciseActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ParkyExercisePrefs";
    private static final String PREF_STREAK_DAYS = "exercise_streak_days";

    private View btnBack, cardSetAlarm;
    private ImageButton btnLovedVideos;
    private ViewPager2 vpBanners;
    private LinearLayout layoutDots;
    private RecyclerView rvCategories;
    private TextView tvProgressSummary;

    private List<ExerciseCategory> categoryList;
    private ExerciseCategoryAdapter categoryAdapter;
    private List<ExerciseBannerAdapter.BannerItem> bannerList;
    private ExerciseBannerAdapter bannerAdapter;

    private int streakDays = 5;
    private int completedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);

        initViews();
        loadPreferences();
        setupHeaderActions();
        setupBanners();
        setupCategories();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        // Show Streak notice when exercise feature is opened first
        showInitialStreakNotice();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCategoryStates();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnLovedVideos = findViewById(R.id.btnLovedVideos);
        vpBanners = findViewById(R.id.vpBanners);
        layoutDots = findViewById(R.id.layoutDots);
        rvCategories = findViewById(R.id.rvCategories);
        tvProgressSummary = findViewById(R.id.tvProgressSummary);
        cardSetAlarm = findViewById(R.id.cardSetAlarm);
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        streakDays = prefs.getInt(PREF_STREAK_DAYS, 5);
    }

    private void setupHeaderActions() {
        btnBack.setOnClickListener(v -> finish());

        // Loved Video Collection Button (Opens LovedVideosActivity)
        btnLovedVideos.setOnClickListener(v -> {
            Intent intent = new Intent(ExerciseActivity.this, LovedVideosActivity.class);
            startActivity(intent);
        });

        // Set Alarm for Exercise Button
        if (cardSetAlarm != null) {
            cardSetAlarm.setOnClickListener(v -> {
                Intent intent = new Intent(ExerciseActivity.this, ExerciseAlarmActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupBanners() {
        bannerList = new ArrayList<>();

        // Banner 1: 30 Days Challenge
        int daysLeft = Math.max(0, 30 - completedCount);
        bannerList.add(new ExerciseBannerAdapter.BannerItem(
                "30 Days Challenge 🏆",
                daysLeft + " Days Left",
                "Complete daily exercise goals to boost Parkinson's mobility & balance.",
                completedCount,
                30
        ));

        // Banner 2: Daily Streak (shown first on launch)
        bannerList.add(new ExerciseBannerAdapter.BannerItem(
                "Daily Streak 🔥",
                streakDays + " Days Active",
                "You are taking care of yourself for " + streakDays + " days! Keep it up!",
                streakDays,
                30
        ));

        // Banner 3: Mobility & Balance
        bannerList.add(new ExerciseBannerAdapter.BannerItem(
                "Balance & Posture 🧘",
                "Daily Goal",
                "Guided routines for freezing of gait & postural stability.",
                10,
                30
        ));

        // Banner 4: Caregiver Safety
        bannerList.add(new ExerciseBannerAdapter.BannerItem(
                "Safety & Assistance 🏡",
                "Guidelines",
                "Safe, supported movement guidelines for caregivers and family.",
                15,
                30
        ));

        bannerAdapter = new ExerciseBannerAdapter(this, bannerList);
        vpBanners.setAdapter(bannerAdapter);

        setupDotsIndicator(bannerList.size());

        vpBanners.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
        });
    }

    private void setupDotsIndicator(int count) {
        layoutDots.removeAllViews();
        for (int i = 0; i < count; i++) {
            TextView dot = new TextView(this);
            dot.setText("● ");
            dot.setTextSize(14);
            dot.setTextColor(i == 0 ? 0xFFFFFFFF : 0x80FFFFFF);
            layoutDots.addView(dot);
        }
    }

    private void updateDots(int activePosition) {
        for (int i = 0; i < layoutDots.getChildCount(); i++) {
            TextView dot = (TextView) layoutDots.getChildAt(i);
            if (dot != null) {
                dot.setTextColor(i == activePosition ? 0xFFFFFFFF : 0x66FFFFFF);
            }
        }
    }

    private void showInitialStreakNotice() {
        if (vpBanners != null && bannerList.size() > 1) {
            vpBanners.setCurrentItem(1, false);
        }
        Toast.makeText(this, "🔥 Daily Streak: You are taking care of yourself for " + streakDays + " days!", Toast.LENGTH_LONG).show();
    }

    private void setupCategories() {
        categoryList = new ArrayList<>();
        for (ExerciseCategory cat : ExerciseDataRepository.getDefaultCategories()) {
            if (!cat.isAlarmButton()) {
                categoryList.add(cat);
            }
        }

        rvCategories.setLayoutManager(new GridLayoutManager(this, 3));
        categoryAdapter = new ExerciseCategoryAdapter(this, categoryList, (category, position) -> {
            Intent intent = new Intent(ExerciseActivity.this, ExerciseDetailActivity.class);
            intent.putExtra(ExerciseDetailActivity.EXTRA_EXERCISE, category);
            startActivity(intent);
        });
        rvCategories.setAdapter(categoryAdapter);

        refreshCategoryStates();
    }

    private void refreshCategoryStates() {
        if (categoryList == null) return;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        completedCount = 0;
        for (ExerciseCategory cat : categoryList) {
            boolean isDone = prefs.getBoolean("completed_" + cat.getId(), false);
            cat.setCompleted(isDone);
            if (isDone) completedCount++;
        }

        if (categoryAdapter != null) {
            categoryAdapter.notifyDataSetChanged();
        }

        updateProgressSummary();
        setupBanners();
    }

    private void updateProgressSummary() {
        int total = categoryList != null ? categoryList.size() : 11;
        tvProgressSummary.setText(completedCount + "/" + total + " Done");
    }
}
