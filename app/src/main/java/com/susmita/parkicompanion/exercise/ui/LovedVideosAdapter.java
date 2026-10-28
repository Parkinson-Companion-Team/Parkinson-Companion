package com.susmita.parkicompanion.exercise.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.exercise.model.ExerciseCategory;
import com.susmita.parkicompanion.exercise.util.ImageDownloader;

import java.util.List;

public class LovedVideosAdapter extends RecyclerView.Adapter<LovedVideosAdapter.ViewHolder> {

    public interface OnLovedVideoClickListener {
        void onItemClick(ExerciseCategory category);
        void onRemoveFavorite(ExerciseCategory category, int position);
    }

    private final Context context;
    private final List<ExerciseCategory> lovedList;
    private final OnLovedVideoClickListener listener;

    public LovedVideosAdapter(Context context, List<ExerciseCategory> lovedList, OnLovedVideoClickListener listener) {
        this.context = context;
        this.lovedList = lovedList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_loved_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExerciseCategory cat = lovedList.get(position);

        String titleText = cat.getTitle().replace("\n", " ");
        if (!titleText.toLowerCase().contains("exercise")) {
            titleText += " Exercise";
        }
        holder.tvTitle.setText(cat.getIcon() + " " + titleText);
        holder.tvDescription.setText(cat.getDescription());

        // Load Thumbnail
        ImageDownloader.loadThumbnail(cat.getThumbnailUrl(), holder.ivThumbnail, null, null);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(cat);
        });

        holder.btnRemoveFavorite.setOnClickListener(v -> {
            if (listener != null) listener.onRemoveFavorite(cat, position);
        });
    }

    @Override
    public int getItemCount() {
        return lovedList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvTitle, tvDescription;
        ImageButton btnRemoveFavorite;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            btnRemoveFavorite = itemView.findViewById(R.id.btnRemoveFavorite);
        }
    }
}
