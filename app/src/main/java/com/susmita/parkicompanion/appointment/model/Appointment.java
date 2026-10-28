package com.susmita.parkicompanion.appointment.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "appointments")
public class Appointment implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String doctorName;
    private String location;
    private String phone;
    private String prescriptionUri;
    private String notes;
    private long timeInMillis;
    private int reminderOffsetMinutes;
    private boolean isAlarmSet;

    public Appointment(String title, String doctorName, String location, String phone, String prescriptionUri, String notes, long timeInMillis, int reminderOffsetMinutes, boolean isAlarmSet) {
        this.title = title;
        this.doctorName = doctorName;
        this.location = location;
        this.phone = phone;
        this.prescriptionUri = prescriptionUri;
        this.notes = notes;
        this.timeInMillis = timeInMillis;
        this.reminderOffsetMinutes = reminderOffsetMinutes;
        this.isAlarmSet = isAlarmSet;
    }

    @Ignore
    public Appointment(String title, String doctorName, String location, String notes, long timeInMillis, int reminderOffsetMinutes, boolean isAlarmSet) {
        this(title, doctorName, location, "", "", notes, timeInMillis, reminderOffsetMinutes, isAlarmSet);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPrescriptionUri() { return prescriptionUri; }
    public void setPrescriptionUri(String prescriptionUri) { this.prescriptionUri = prescriptionUri; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getTimeInMillis() { return timeInMillis; }
    public void setTimeInMillis(long timeInMillis) { this.timeInMillis = timeInMillis; }

    public int getReminderOffsetMinutes() { return reminderOffsetMinutes; }
    public void setReminderOffsetMinutes(int reminderOffsetMinutes) { this.reminderOffsetMinutes = reminderOffsetMinutes; }

    public boolean isAlarmSet() { return isAlarmSet; }
    public void setAlarmSet(boolean alarmSet) { isAlarmSet = alarmSet; }
}