package com.susmita.parkicompanion.exercise.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageDownloader {

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> memoryCache = new LruCache<>((int) (Runtime.getRuntime().maxMemory() / 1024 / 8));

    public static void loadThumbnail(String urlString, ImageView imageView, ProgressBar progressBar, View errorView) {
        if (urlString == null || urlString.isEmpty()) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (errorView != null) errorView.setVisibility(View.VISIBLE);
            return;
        }

        Bitmap cached = memoryCache.get(urlString);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (errorView != null) errorView.setVisibility(View.GONE);
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (errorView != null) errorView.setVisibility(View.GONE);

        executor.execute(() -> {
            Bitmap bitmap = null;
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setInstanceFollowRedirects(true);
                conn.connect();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream input = conn.getInputStream();
                    bitmap = BitmapFactory.decodeStream(input);
                    input.close();
                }
                conn.disconnect();
            } catch (Exception ignored) {
            }

            final Bitmap resultBitmap = bitmap;
            mainHandler.post(() -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (resultBitmap != null) {
                    memoryCache.put(urlString, resultBitmap);
                    imageView.setImageBitmap(resultBitmap);
                    if (errorView != null) errorView.setVisibility(View.GONE);
                } else {
                    if (errorView != null) errorView.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}
