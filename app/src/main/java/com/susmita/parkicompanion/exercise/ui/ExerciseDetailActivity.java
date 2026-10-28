package com.susmita.parkicompanion.exercise.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.exercise.model.ExerciseCategory;
import com.susmita.parkicompanion.exercise.repository.ExerciseDataRepository;
import com.susmita.parkicompanion.exercise.util.ImageDownloader;

public class ExerciseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EXERCISE = "extra_exercise";
    private static final String PREFS_NAME = "ParkyExercisePrefs";

    private View btnBack;
    private Button btnComplete;
    private TextView tvHeaderTitle, tvErrorFallback;
    private CardView cardVideoArea;
    private ImageView ivThumbnail, ivPlayOverlay;
    private ImageButton btnPlayVideo, btnFavorite;
    private ProgressBar pbLoading;

    private ExerciseCategory exerciseCategory;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_detail);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_EXERCISE)) {
            exerciseCategory = (ExerciseCategory) getIntent().getSerializableExtra(EXTRA_EXERCISE);
        }

        if (exerciseCategory == null) {
            // Default Fallback: Leg Exercise
            exerciseCategory = new ExerciseCategory("leg", "Leg Exercise", "🦵",
                    "Leg extensions, seated marches, and ankle pumps to improve gait, stability, and lower body strength for Parkinson's care.",
                    "https://youtu.be/hgnd1KfTVEA?si=bCR51iKU6FvCrMsV");
        }

        initViews();
        setupData();

        btnBack.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvErrorFallback = findViewById(R.id.tvErrorFallback);
        cardVideoArea = findViewById(R.id.cardVideoArea);
        ivThumbnail = findViewById(R.id.ivThumbnail);
        ivPlayOverlay = findViewById(R.id.ivPlayOverlay);
        btnPlayVideo = findViewById(R.id.btnPlayVideo);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnComplete = findViewById(R.id.btnComplete);
        pbLoading = findViewById(R.id.pbLoading);
    }

    private void setupData() {
        String rawTitle = exerciseCategory.getTitle().replace("\n", " ");
        if (!rawTitle.toLowerCase().contains("exercise")) {
            rawTitle += " Exercise";
        }
        final String titleText = rawTitle;
        tvHeaderTitle.setText(titleText);

        // Favorite State
        updateFavoriteUI();

        // Completion State
        updateCompletionUI();

        // Load YouTube Thumbnail
        String thumbnailUrl = exerciseCategory.getThumbnailUrl();
        ImageDownloader.loadThumbnail(thumbnailUrl, ivThumbnail, pbLoading, tvErrorFallback);

        // Click listeners to open YouTube video
        View.OnClickListener playListener = v -> openYoutubeVideo();
        cardVideoArea.setOnClickListener(playListener);
        ivPlayOverlay.setOnClickListener(playListener);
        btnPlayVideo.setOnClickListener(playListener);

        // Toggle Favorite
        btnFavorite.setOnClickListener(v -> {
            boolean isNowFav = ExerciseDataRepository.toggleFavorite(this, exerciseCategory.getId());
            this.isFavorite = isNowFav;
            updateFavoriteUI();
            if (isNowFav) {
                Toast.makeText(this, "Added " + titleText + " to Loved Videos ♡", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Removed " + titleText + " from Loved Videos", Toast.LENGTH_SHORT).show();
            }
        });

        // Toggle Completion
        btnComplete.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean isDone = prefs.getBoolean("completed_" + exerciseCategory.getId(), false);
            boolean newStatus = !isDone;

            prefs.edit().putBoolean("completed_" + exerciseCategory.getId(), newStatus).apply();
            exerciseCategory.setCompleted(newStatus);
            updateCompletionUI();

            if (newStatus) {
                Toast.makeText(this, "Awesome! " + titleText + " marked as completed ✔", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, titleText + " marked as incomplete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFavoriteUI() {
        isFavorite = ExerciseDataRepository.isFavorite(this, exerciseCategory.getId());
        if (isFavorite) {
            btnFavorite.setImageResource(R.drawable.ic_heart_filled);
        } else {
            btnFavorite.setImageResource(R.drawable.ic_heart_outline);
        }
    }

    private void updateCompletionUI() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDone = prefs.getBoolean("completed_" + exerciseCategory.getId(), false);
        exerciseCategory.setCompleted(isDone);

        if (isDone) {
            btnComplete.setText("Mark as Incomplete");
            btnComplete.setBackgroundTintList(ColorStateList.valueOf(0xFF64748B));
        } else {
            btnComplete.setText("Mark as Completed ✔");
            btnComplete.setBackgroundTintList(ColorStateList.valueOf(0xFF315C55));
        }
    }

    private void openYoutubeVideo() {
        String videoId = exerciseCategory.getYoutubeVideoId();
        if (videoId == null || videoId.isEmpty()) {
            Toast.makeText(this, "No video available for this exercise.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoId));
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + videoId));

        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            try {
                startActivity(webIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open YouTube video link.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
