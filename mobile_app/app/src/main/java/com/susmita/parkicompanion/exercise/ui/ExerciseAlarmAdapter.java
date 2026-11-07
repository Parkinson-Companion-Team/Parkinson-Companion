package com.susmita.parkicompanion.exercise.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.exercise.model.ExerciseAlarm;

import java.util.List;

public class ExerciseAlarmAdapter extends RecyclerView.Adapter<ExerciseAlarmAdapter.ViewHolder> {

    public interface OnAlarmActionListener {
        void onToggleAlarm(ExerciseAlarm alarm, boolean isEnabled);
        void onDeleteAlarm(ExerciseAlarm alarm, int position);
        void onEditAlarm(ExerciseAlarm alarm, int position);
    }

    private final Context context;
    private final List<ExerciseAlarm> alarmList;
    private final OnAlarmActionListener listener;

    public ExerciseAlarmAdapter(Context context, List<ExerciseAlarm> alarmList, OnAlarmActionListener listener) {
        this.context = context;
        this.alarmList = alarmList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_exercise_alarm, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExerciseAlarm alarm = alarmList.get(position);

        holder.tvAlarmTime.setText(alarm.getFormattedTime());
        holder.tvAlarmLabel.setText(alarm.getLabel());
        holder.tvAlarmDays.setText(alarm.getFormattedDays());

        holder.switchAlarm.setOnCheckedChangeListener(null);
        holder.switchAlarm.setChecked(alarm.isEnabled());

        holder.switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            alarm.setEnabled(isChecked);
            if (listener != null) {
                listener.onToggleAlarm(alarm, isChecked);
            }
        });

        View.OnClickListener editListener = v -> {
            if (listener != null) {
                listener.onEditAlarm(alarm, position);
            }
        };

        holder.itemView.setOnClickListener(editListener);
        if (holder.btnEditAlarm != null) {
            holder.btnEditAlarm.setOnClickListener(editListener);
        }

        holder.btnDeleteAlarm.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteAlarm(alarm, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlarmTime, tvAlarmLabel, tvAlarmDays;
        SwitchCompat switchAlarm;
        ImageButton btnEditAlarm, btnDeleteAlarm;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAlarmTime = itemView.findViewById(R.id.tvAlarmTime);
            tvAlarmLabel = itemView.findViewById(R.id.tvAlarmLabel);
            tvAlarmDays = itemView.findViewById(R.id.tvAlarmDays);
            switchAlarm = itemView.findViewById(R.id.switchAlarm);
            btnEditAlarm = itemView.findViewById(R.id.btnEditAlarm);
            btnDeleteAlarm = itemView.findViewById(R.id.btnDeleteAlarm);
        }
    }
}
