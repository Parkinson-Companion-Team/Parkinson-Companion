package com.susmita.parkicompanion.exercise.model;

import java.io.Serializable;
import java.util.Locale;

public class ExerciseAlarm implements Serializable {

    private String id;
    private int hour;
    private int minute;
    private String label;
    private boolean[] repeatDays; // Index 0 = Sun, 1 = Mon, ..., 6 = Sat
    private boolean isEnabled;

    public ExerciseAlarm(String id, int hour, int minute, String label, boolean[] repeatDays) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.label = label;
        this.repeatDays = repeatDays != null ? repeatDays : new boolean[]{true, true, true, true, true, true, true};
        this.isEnabled = true;
    }

    public String getId() {
        return id;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean[] getRepeatDays() {
        return repeatDays;
    }

    public void setRepeatDays(boolean[] repeatDays) {
        this.repeatDays = repeatDays;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public String getFormattedTime() {
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        String amPm = hour >= 12 ? "PM" : "AM";
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm);
    }

    public String getFormattedDays() {
        String[] days = new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < 7; i++) {
            if (repeatDays != null && i < repeatDays.length && repeatDays[i]) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(days[i]);
                count++;
            }
        }
        if (count == 7) return "Everyday";
        if (count == 0) return "No Days Selected";
        return sb.toString();
    }
}
