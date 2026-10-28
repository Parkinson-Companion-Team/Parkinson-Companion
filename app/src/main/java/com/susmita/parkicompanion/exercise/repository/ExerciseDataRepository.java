package com.susmita.parkicompanion.exercise.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.susmita.parkicompanion.exercise.model.ExerciseCategory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExerciseDataRepository {

    private static final String PREFS_NAME = "ParkyExercisePrefs";
    private static final String PREF_FAVORITES_SET = "favorite_exercise_ids";
    private static final String PREF_DOWNLOADS_SET = "downloaded_exercise_ids";

    public static List<ExerciseCategory> getDefaultCategories() {
        List<ExerciseCategory> list = new ArrayList<>();
        // User provided video URL for Leg Exercise: https://youtu.be/hgnd1KfTVEA?si=bCR51iKU6FvCrMsV
        list.add(new ExerciseCategory("leg", "Leg", "🦵", "Leg extensions, seated marches, and ankle pumps to improve gait & balance.", "https://youtu.be/hgnd1KfTVEA?si=bCR51iKU6FvCrMsV"));
        list.add(new ExerciseCategory("core", "Core &\nTrunk", "🧘", "Seated trunk rotations, side bends, and core alignment routines.", "https://www.youtube.com/watch?v=4Y2ZdHCOXok"));
        list.add(new ExerciseCategory("hand", "Hand", "✋", "Finger taps, hand squeezes, and wrist rotations for fine motor control.", "https://www.youtube.com/watch?v=r_2_3A5L1nE"));
        list.add(new ExerciseCategory("speech", "Speech &\nVoice", "🗣️", "Vocal loud exercises, sustained vowels, and facial articulation.", "https://www.youtube.com/watch?v=91Ie4C_k_9c"));
        list.add(new ExerciseCategory("balance", "Balance", "⚖️", "Single-leg stands, weight shifts, and heel-to-toe walking drills.", "https://www.youtube.com/watch?v=Y8H3BfXN6Q8"));
        list.add(new ExerciseCategory("facial", "Facial", "😊", "Smile stretches, eyebrow raises, and lip puckering routines.", "https://www.youtube.com/watch?v=3J8Q1N2mO4E"));
        list.add(new ExerciseCategory("bed", "Bed\nExercise", "🛏️", "In-bed bridge lifts, leg slides, and gentle morning mobility.", "https://www.youtube.com/watch?v=vV7Yg5_x5vE"));
        list.add(new ExerciseCategory("brain", "Brain\nExercise", "🧠", "Dual-task coordination, cross-body movements, and rhythm drills.", "https://www.youtube.com/watch?v=H3E3F_Y0wE8"));
        list.add(new ExerciseCategory("breathing", "Breathing\nExercise", "🫁", "Deep diaphragmatic breathing and chest expansion routines.", "https://www.youtube.com/watch?v=5Dq_L_16aDA"));
        list.add(new ExerciseCategory("swimming", "Swimming", "🏊", "Low-impact water therapy routines and aquatic gait practice.", "https://www.youtube.com/watch?v=gh0E6WJ00k8"));
        list.add(new ExerciseCategory("walking", "Walking", "🚶", "Rhythmic auditory walking, high-knee strides, and turning drills.", "https://www.youtube.com/watch?v=5aY3f2zJ0_0"));
        list.add(new ExerciseCategory("alarm", "Set Alarm\nfor Exercise", "⏰", "Schedule daily reminders for exercise routines.", "", true));
        return list;
    }

    public static boolean isFavorite(Context context, String exerciseId) {
        if (exerciseId == null || exerciseId.isEmpty()) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> favs = prefs.getStringSet(PREF_FAVORITES_SET, new HashSet<>());
        return favs.contains(exerciseId);
    }

    public static boolean toggleFavorite(Context context, String exerciseId) {
        if (exerciseId == null || exerciseId.isEmpty()) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> favs = new HashSet<>(prefs.getStringSet(PREF_FAVORITES_SET, new HashSet<>()));
        boolean isNowFavorite;
        if (favs.contains(exerciseId)) {
            favs.remove(exerciseId);
            isNowFavorite = false;
        } else {
            favs.add(exerciseId);
            isNowFavorite = true;
        }
        prefs.edit().putStringSet(PREF_FAVORITES_SET, favs).apply();
        return isNowFavorite;
    }

    public static List<ExerciseCategory> getFavoriteCategories(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> favs = prefs.getStringSet(PREF_FAVORITES_SET, new HashSet<>());
        List<ExerciseCategory> all = getDefaultCategories();
        List<ExerciseCategory> result = new ArrayList<>();
        for (ExerciseCategory cat : all) {
            if (favs.contains(cat.getId())) {
                result.add(cat);
            }
        }
        return result;
    }

    public static boolean isDownloaded(Context context, String exerciseId) {
        if (exerciseId == null || exerciseId.isEmpty()) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> downloads = prefs.getStringSet(PREF_DOWNLOADS_SET, new HashSet<>());
        return downloads.contains(exerciseId);
    }

    public static boolean toggleDownloaded(Context context, String exerciseId) {
        if (exerciseId == null || exerciseId.isEmpty()) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> downloads = new HashSet<>(prefs.getStringSet(PREF_DOWNLOADS_SET, new HashSet<>()));
        boolean isNowDownloaded;
        if (downloads.contains(exerciseId)) {
            downloads.remove(exerciseId);
            isNowDownloaded = false;
        } else {
            downloads.add(exerciseId);
            isNowDownloaded = true;
        }
        prefs.edit().putStringSet(PREF_DOWNLOADS_SET, downloads).apply();
        return isNowDownloaded;
    }

    public static List<ExerciseCategory> getDownloadedCategories(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> downloads = prefs.getStringSet(PREF_DOWNLOADS_SET, new HashSet<>());
        List<ExerciseCategory> all = getDefaultCategories();
        List<ExerciseCategory> result = new ArrayList<>();
        for (ExerciseCategory cat : all) {
            if (downloads.contains(cat.getId())) {
                result.add(cat);
            }
        }
        return result;
    }
}
