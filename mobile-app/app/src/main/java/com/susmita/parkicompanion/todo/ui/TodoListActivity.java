package com.susmita.parkicompanion.todo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.susmita.parkicompanion.R;
import com.susmita.parkicompanion.todo.model.Todo;
import com.susmita.parkicompanion.todo.util.TodoAlarmScheduler;
import com.susmita.parkicompanion.todo.viewmodel.TodoViewModel;

public class TodoListActivity extends AppCompatActivity implements TodoAdapter.OnTodoActionListener {

    private TodoViewModel viewModel;
    private TodoAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_list);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnAddTodo = findViewById(R.id.btnAddTodo);
        RecyclerView rvTodos = findViewById(R.id.rvTodos);
        tvEmpty = findViewById(R.id.tvEmpty);

        btnBack.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        adapter = new TodoAdapter(this);
        rvTodos.setLayoutManager(new LinearLayoutManager(this));
        rvTodos.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(TodoViewModel.class);
        viewModel.getAllTodos().observe(this, todos -> {
            if (todos == null || todos.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvTodos.setVisibility(View.GONE);
                adapter.submitList(null);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvTodos.setVisibility(View.VISIBLE);
                adapter.submitList(todos);
            }
        });

        btnAddTodo.setOnClickListener(v -> {
            Intent intent = new Intent(TodoListActivity.this, AddEditTodoActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onToggleCompleted(Todo todo, boolean isCompleted) {
        todo.setCompleted(isCompleted);
        viewModel.update(todo);
    }

    @Override
    public void onEditClick(Todo todo) {
        Intent intent = new Intent(this, AddEditTodoActivity.class);
        intent.putExtra(AddEditTodoActivity.EXTRA_TODO, todo);
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Todo todo) {
        TodoAlarmScheduler.cancelAlarm(this, todo);
        viewModel.delete(todo);
        Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
    }
}
