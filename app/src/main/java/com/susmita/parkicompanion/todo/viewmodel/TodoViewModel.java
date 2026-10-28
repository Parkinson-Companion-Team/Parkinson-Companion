package com.susmita.parkicompanion.todo.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.susmita.parkicompanion.todo.model.Todo;
import com.susmita.parkicompanion.todo.repository.TodoRepository;

import java.util.List;

public class TodoViewModel extends AndroidViewModel {

    private final TodoRepository repository;
    private final LiveData<List<Todo>> allTodos;

    public TodoViewModel(@NonNull Application application) {
        super(application);
        repository = new TodoRepository(application);
        allTodos = repository.getAllTodos();
    }

    public LiveData<List<Todo>> getAllTodos() {
        return allTodos;
    }

    public void insert(Todo todo, TodoRepository.OnTodoInsertedListener listener) {
        repository.insert(todo, listener);
    }

    public void update(Todo todo) {
        repository.update(todo);
    }

    public void delete(Todo todo) {
        repository.delete(todo);
    }
}
