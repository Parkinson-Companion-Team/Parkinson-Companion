package com.susmita.parkicompanion.todo.ui;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.todo.model.Todo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    public interface OnTodoActionListener {
        void onToggleCompleted(Todo todo, boolean isCompleted);
        void onEditClick(Todo todo);
        void onDeleteClick(Todo todo);
    }

    private final List<Todo> todoList = new ArrayList<>();
    private final OnTodoActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public TodoAdapter(OnTodoActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Todo> todos) {
        this.todoList.clear();
        if (todos != null) {
            this.todoList.addAll(todos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todo, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        Todo todo = todoList.get(position);
        holder.bind(todo, listener, dateFormat, timeFormat);
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    static class TodoViewHolder extends RecyclerView.ViewHolder {

        private final CheckBox cbDone;
        private final TextView tvTitle;
        private final TextView tvReminderTime;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            cbDone = itemView.findViewById(R.id.cbDone);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvReminderTime = itemView.findViewById(R.id.tvReminderTime);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Todo todo, OnTodoActionListener listener, SimpleDateFormat dateFormat, SimpleDateFormat timeFormat) {
            tvTitle.setText(todo.getTitle());

            // Checkbox state
            cbDone.setOnCheckedChangeListener(null);
            cbDone.setChecked(todo.isCompleted());

            if (todo.isCompleted()) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                itemView.setAlpha(0.6f);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                itemView.setAlpha(1.0f);
            }

            cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggleCompleted(todo, isChecked);
                }
            });

            // Reminder time display
            if (todo.isReminderSet()) {
                tvReminderTime.setVisibility(View.VISIBLE);
                String repeatType = todo.getRepeatType();

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, todo.getHour());
                cal.set(Calendar.MINUTE, todo.getMinute());

                String formattedTimeStr = timeFormat.format(cal.getTime());

                if ("EVERYDAY".equals(repeatType)) {
                    tvReminderTime.setText("⏰ Everyday at " + formattedTimeStr);
                } else if ("CUSTOM_DAYS".equals(repeatType)) {
                    String days = todo.getRepeatDays() != null ? todo.getRepeatDays() : "Selected Days";
                    tvReminderTime.setText("⏰ " + days + " at " + formattedTimeStr);
                } else {
                    if (todo.getReminderTime() > 0) {
                        tvReminderTime.setText("⏰ " + dateFormat.format(new Date(todo.getReminderTime())));
                    } else {
                        tvReminderTime.setText("⏰ At " + formattedTimeStr);
                    }
                }
            } else {
                tvReminderTime.setVisibility(View.GONE);
            }

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(todo);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(todo);
                }
            });
        }
    }
}
