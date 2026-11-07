package com.susmita.parkicompanion.network;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class ApiClient {

    // The Android emulator reaches the development server through 10.0.2.2.
    // For a real phone, use the laptop's LAN IP and keep both devices on the same Wi-Fi.
    public static final String BASE_URL = "http://10.0.2.2:8000";
    private static final String AUTH_FILE = "auth";
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
    private ApiClient() {}

    public interface TokenCallback {
        void onToken(String token);
        void onFailure(IOException error);
    }

    public static OkHttpClient client() {
        return CLIENT;
    }

    public static void saveToken(Context context, String token) {
        preferences(context).edit().putString("token", token).apply();
    }

    public static String getToken(Context context) {
        return preferences(context).getString("token", null);
    }

    public static void clearToken(Context context) {
        preferences(context).edit().remove("token").apply();
    }

    public static Request.Builder authenticatedRequest(String path, String token) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + path);
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Token " + token);
        }
        return builder;
    }

    public static void ensureToken(Context context, TokenCallback callback) {
        String savedToken = getToken(context);
        if (savedToken != null && !savedToken.isEmpty()) {
            callback.onToken(savedToken);
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/guest/")
                .post(new okhttp3.FormBody.Builder().build())
                .build();
        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try (Response ignored = response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onFailure(new IOException("Guest token request failed: " + response.code()));
                        return;
                    }
                    try {
                        JsonObject body = JsonParser.parseString(response.body().string()).getAsJsonObject();
                        String token = body.get("token").getAsString();
                        saveToken(context, token);
                        callback.onToken(token);
                    } catch (RuntimeException error) {
                        callback.onFailure(new IOException("Invalid guest token response", error));
                    }
                }
            }

            @Override
            public void onFailure(okhttp3.Call call, IOException error) {
                callback.onFailure(error);
            }
        });
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(AUTH_FILE, Context.MODE_PRIVATE);
    }
}
