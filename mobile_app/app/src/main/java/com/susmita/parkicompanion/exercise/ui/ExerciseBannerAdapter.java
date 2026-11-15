package com.susmita.parkicompanion.exercise.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.susmita.parkicompanion.R;

import java.util.List;

public class ExerciseBannerAdapter extends RecyclerView.Adapter<ExerciseBannerAdapter.ViewHolder> {

    public static class BannerItem {
        private String title;
        private String badge;
        private String subtitle;
        private int progress;
        private int maxProgress;

        public BannerItem(String title, String badge, String subtitle, int progress, int maxProgress) {
            this.title = title;
            this.badge = badge;
            this.subtitle = subtitle;
            this.progress = progress;
            this.maxProgress = maxProgress;
        }

        public String getTitle() {
            return title;
        }

        public String getBadge() {
            return badge;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public int getProgress() {
            return progress;
        }

        public int getMaxProgress() {
            return maxProgress;
        }
    }

    private final Context context;
    private final List<BannerItem> bannerItems;

    public ExerciseBannerAdapter(Context context, List<BannerItem> bannerItems) {
        this.context = context;
        this.bannerItems = bannerItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_banner_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BannerItem item = bannerItems.get(position);
        holder.tvBannerTitle.setText(item.getTitle());
        holder.tvBannerBadge.setText(item.getBadge());
        holder.tvBannerSubtitle.setText(item.getSubtitle());
        holder.pbBannerProgress.setMax(item.getMaxProgress());
        holder.pbBannerProgress.setProgress(item.getProgress());
    }

    @Override
    public int getItemCount() {
        return bannerItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBannerTitle, tvBannerBadge, tvBannerSubtitle;
        ProgressBar pbBannerProgress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBannerTitle = itemView.findViewById(R.id.tvBannerTitle);
            tvBannerBadge = itemView.findViewById(R.id.tvBannerBadge);
            tvBannerSubtitle = itemView.findViewById(R.id.tvBannerSubtitle);
            pbBannerProgress = itemView.findViewById(R.id.pbBannerProgress);
        }
    }
}
