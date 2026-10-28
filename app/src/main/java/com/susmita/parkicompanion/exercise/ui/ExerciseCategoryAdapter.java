package com.susmita.parkicompanion.exercise.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.exercise.model.ExerciseCategory;

import java.util.List;

public class ExerciseCategoryAdapter extends RecyclerView.Adapter<ExerciseCategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(ExerciseCategory category, int position);
    }

    private final Context context;
    private final List<ExerciseCategory> categories;
    private final OnCategoryClickListener listener;

    public ExerciseCategoryAdapter(Context context, List<ExerciseCategory> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_exercise_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExerciseCategory cat = categories.get(position);

        holder.tvTitle.setText(cat.getTitle());
        holder.tvIcon.setText(cat.getIcon());

        if (cat.isCompleted()) {
            // Completed state: Green tick mark visible + Faded layout
            holder.ivCheckMark.setVisibility(View.VISIBLE);
            holder.layoutContent.setAlpha(0.45f);
            holder.cardCategory.setCardBackgroundColor(0xFFF1F5F9);
        } else {
            // Normal state
            holder.ivCheckMark.setVisibility(View.GONE);
            holder.layoutContent.setAlpha(1.0f);
            holder.cardCategory.setCardBackgroundColor(0xFFFFFFFF);
        }

        if (cat.isAlarmButton()) {
            holder.cardCategory.setCardBackgroundColor(0xFFEFF6FF);
            holder.tvTitle.setTextColor(0xFF1D4ED8);
        } else {
            holder.tvTitle.setTextColor(0xFF0F172A);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(cat, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardCategory;
        RelativeLayout layoutContent;
        ImageView ivCheckMark;
        TextView tvIcon, tvTitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCategory = itemView.findViewById(R.id.cardCategory);
            layoutContent = itemView.findViewById(R.id.layoutContent);
            ivCheckMark = itemView.findViewById(R.id.ivCheckMark);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }
}
