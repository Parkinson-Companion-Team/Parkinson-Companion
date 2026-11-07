package com.susmita.parkicompanion.medicine.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "medicines")
public class Medicine implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String dosage;
    private String times; // e.g., "08:00 AM, 02:00 PM, 08:00 PM"
    private String repeatDays; // e.g., "Mon, Wed, Fri" or "Everyday"
    private String specificDate; // e.g., "25 Oct 2025" or empty
    private boolean isEnabled;
    private String notes;
    private String instruction; // e.g. "After Meal", "Before Meal", "With Meal"
    private String imageUri; // path or URI for medicine picture

    public Medicine(String name, String dosage, String times, String repeatDays, String specificDate, boolean isEnabled, String notes, String instruction, String imageUri) {
        this.name = name;
        this.dosage = dosage;
        this.times = times;
        this.repeatDays = repeatDays;
        this.specificDate = specificDate;
        this.isEnabled = isEnabled;
        this.notes = notes;
        this.instruction = instruction;
        this.imageUri = imageUri;
    }

    @Ignore
    public Medicine(String name, String dosage, String times, String repeatDays, String specificDate, boolean isEnabled, String notes) {
        this(name, dosage, times, repeatDays, specificDate, isEnabled, notes, "After Meal", "");
    }

    @Ignore
    public Medicine(String name, String dosage, String times, String repeatDays, boolean isEnabled) {
        this(name, dosage, times, repeatDays, "", isEnabled, "", "After Meal", "");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getTimes() {
        return times;
    }

    public void setTimes(String times) {
        this.times = times;
    }

    public String getRepeatDays() {
        return repeatDays;
    }

    public void setRepeatDays(String repeatDays) {
        this.repeatDays = repeatDays;
    }

    public String getSpecificDate() {
        return specificDate;
    }

    public void setSpecificDate(String specificDate) {
        this.specificDate = specificDate;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
}
