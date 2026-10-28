package com.susmita.parkicompanion.exercise.model;

import java.io.Serializable;

public class ExerciseCategory implements Serializable {

    private String id;
    private String title;
    private String icon;
    private String description;
    private String videoUrl;
    private boolean isCompleted;
    private boolean isAlarmButton;

    public ExerciseCategory(String id, String title, String icon, String description, String videoUrl) {
        this(id, title, icon, description, videoUrl, false);
    }

    public ExerciseCategory(String id, String title, String icon, String description, String videoUrl, boolean isAlarmButton) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.description = description;
        this.videoUrl = videoUrl;
        this.isCompleted = false;
        this.isAlarmButton = isAlarmButton;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getYoutubeVideoId() {
        return extractYoutubeVideoId(videoUrl);
    }

    public String getThumbnailUrl() {
        String videoId = getYoutubeVideoId();
        if (videoId != null && !videoId.isEmpty()) {
            return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
        }
        return null;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isAlarmButton() {
        return isAlarmButton;
    }

    public static String extractYoutubeVideoId(String url) {
        if (url == null || url.trim().isEmpty()) return null;
        String id = null;
        try {
            if (url.contains("youtu.be/")) {
                int start = url.indexOf("youtu.be/") + 9;
                int end = url.indexOf("?", start);
                if (end == -1) end = url.indexOf("&", start);
                if (end == -1) end = url.length();
                id = url.substring(start, end);
            } else if (url.contains("v=")) {
                int start = url.indexOf("v=") + 2;
                int end = url.indexOf("&", start);
                if (end == -1) end = url.length();
                id = url.substring(start, end);
            } else if (url.contains("embed/")) {
                int start = url.indexOf("embed/") + 6;
                int end = url.indexOf("?", start);
                if (end == -1) end = url.length();
                id = url.substring(start, end);
            }
        } catch (Exception ignored) {
        }
        return (id != null && !id.trim().isEmpty()) ? id.trim() : null;
    }
}
