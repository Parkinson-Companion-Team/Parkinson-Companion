package com.susmita.parkicompanion.todo.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "todos")
public class Todo implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private boolean isCompleted;
    private boolean isReminderSet;
    private String repeatType; // "SPECIFIC_DATE", "EVERYDAY", "CUSTOM_DAYS"
    private String repeatDays; // e.g., "Mon, Wed, Fri"
    private long reminderTime; // Epoch millisecond for SPECIFIC_DATE
    private int hour;          // 0-23
    private int minute;        // 0-59

    public Todo(String title, boolean isCompleted, boolean isReminderSet, String repeatType, String repeatDays, long reminderTime, int hour, int minute) {
        this.title = title;
        this.isCompleted = isCompleted;
        this.isReminderSet = isReminderSet;
        this.repeatType = repeatType;
        this.repeatDays = repeatDays;
        this.reminderTime = reminderTime;
        this.hour = hour;
        this.minute = minute;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isReminderSet() {
        return isReminderSet;
    }

    public void setReminderSet(boolean reminderSet) {
        isReminderSet = reminderSet;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public String getRepeatDays() {
        return repeatDays;
    }

    public void setRepeatDays(String repeatDays) {
        this.repeatDays = repeatDays;
    }

    public long getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(long reminderTime) {
        this.reminderTime = reminderTime;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }
}
