package com.susmita.parkicompanion.todo.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.susmita.parkicompanion.todo.db.TodoDao;
import com.susmita.parkicompanion.todo.db.TodoDatabase;
import com.susmita.parkicompanion.todo.model.Todo;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TodoRepository {

    private final TodoDao todoDao;
    private final LiveData<List<Todo>> allTodos;
    private final ExecutorService executorService;

    public interface OnTodoInsertedListener {
        void onInserted(long id);
    }

    public TodoRepository(Application application) {
        TodoDatabase database = TodoDatabase.getInstance(application);
        todoDao = database.todoDao();
        allTodos = todoDao.getAllTodos();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Todo>> getAllTodos() {
        return allTodos;
    }

    public void insert(Todo todo, OnTodoInsertedListener listener) {
        executorService.execute(() -> {
            long id = todoDao.insert(todo);
            todo.setId((int) id);
            if (listener != null) {
                listener.onInserted(id);
            }
        });
    }

    public void update(Todo todo) {
        executorService.execute(() -> todoDao.update(todo));
    }

    public void delete(Todo todo) {
        executorService.execute(() -> todoDao.delete(todo));
    }
}
