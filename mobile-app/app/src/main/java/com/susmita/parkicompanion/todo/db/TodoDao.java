package com.susmita.parkicompanion.todo.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.susmita.parkicompanion.todo.model.Todo;

import java.util.List;

@Dao
public interface TodoDao {

    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, id DESC")
    LiveData<List<Todo>> getAllTodos();

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    Todo getTodoById(int id);

    @Query("SELECT * FROM todos WHERE isReminderSet = 1 AND isCompleted = 0")
    List<Todo> getActiveTodosWithReminderSync();

    @Insert
    long insert(Todo todo);

    @Update
    void update(Todo todo);

    @Delete
    void delete(Todo todo);
}
