package com.susmita.parkicompanion.exercise.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.exercise.model.ExerciseCategory;
import com.susmita.parkicompanion.exercise.repository.ExerciseDataRepository;

import java.util.ArrayList;
import java.util.List;

public class LovedVideosActivity extends AppCompatActivity {

    private View btnBack;
    private RecyclerView rvLovedVideos;
    private LinearLayout layoutEmptyState;

    private List<ExerciseCategory> lovedList = new ArrayList<>();
    private LovedVideosAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loved_videos);

        initViews();

        btnBack.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLovedVideos();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        rvLovedVideos = findViewById(R.id.rvLovedVideos);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
    }

    private void setupRecyclerView() {
        rvLovedVideos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LovedVideosAdapter(this, lovedList, new LovedVideosAdapter.OnLovedVideoClickListener() {
            @Override
            public void onItemClick(ExerciseCategory category) {
                Intent intent = new Intent(LovedVideosActivity.this, ExerciseDetailActivity.class);
                intent.putExtra(ExerciseDetailActivity.EXTRA_EXERCISE, category);
                startActivity(intent);
            }

            @Override
            public void onRemoveFavorite(ExerciseCategory category, int position) {
                ExerciseDataRepository.toggleFavorite(LovedVideosActivity.this, category.getId());
                lovedList.remove(position);
                adapter.notifyItemRemoved(position);
                updateEmptyState();
                Toast.makeText(LovedVideosActivity.this, "Removed from Loved Videos", Toast.LENGTH_SHORT).show();
            }
        });
        rvLovedVideos.setAdapter(adapter);
    }

    private void loadLovedVideos() {
        lovedList.clear();
        lovedList.addAll(ExerciseDataRepository.getFavoriteCategories(this));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (lovedList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvLovedVideos.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvLovedVideos.setVisibility(View.VISIBLE);
        }
    }
}
