package com.susmita.parkicompanion.todo.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.susmita.parkicompanion.todo.model.Todo;

@Database(entities = {Todo.class}, version = 2, exportSchema = false)
public abstract class TodoDatabase extends RoomDatabase {

    private static volatile TodoDatabase INSTANCE;

    public abstract TodoDao todoDao();

    public static TodoDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TodoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            TodoDatabase.class,
                            "todo_database"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
