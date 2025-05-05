package com.example.clockwallpaperapp;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new ClockEngine();
    }

    private class ClockEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable drawRunnable = this::drawFrame;
        private boolean visible = true;
        private SharedPreferences prefs;
        private Paint clockPaint;
        private int backgroundColor = Color.BLACK;
        private int clockColor = Color.WHITE;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            prefs.registerOnSharedPreferenceChangeListener(this);
            setupPaint();
            loadTheme(prefs.getString("theme_preference", "default"));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            prefs.unregisterOnSharedPreferenceChangeListener(this);
            handler.removeCallbacks(drawRunnable);
        }

        private void setupPaint() {
            clockPaint = new Paint();
            clockPaint.setTextSize(120f);
            clockPaint.setTextAlign(Paint.Align.CENTER);
            clockPaint.setAntiAlias(true);
        }

        private void loadTheme(String themeKey) {
            switch (themeKey) {
                case "blue":
                    backgroundColor = Color.parseColor("#0D47A1"); // Dark Blue
                    clockColor = Color.parseColor("#BBDEFB"); // Light Blue
                    break;
                case "green":
                    backgroundColor = Color.parseColor("#1B5E20"); // Dark Green
                    clockColor = Color.parseColor("#C8E6C9"); // Light Green
                    break;
                case "default":
                default:
                    backgroundColor = Color.BLACK;
                    clockColor = Color.WHITE;
                    break;
            }
            clockPaint.setColor(clockColor);
            // Force redraw when theme changes
            if (visible) {
                drawFrame();
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if ("theme_preference".equals(key)) {
                loadTheme(sharedPreferences.getString(key, "default"));
            }
        }

        private void drawFrame() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    // Draw background
                    canvas.drawColor(backgroundColor);

                    // Draw time using the configured paint
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    String currentTime = sdf.format(new Date());

                    float x = canvas.getWidth() / 2f;
                    float y = canvas.getHeight() / 2f;
                    Paint.FontMetrics fm = clockPaint.getFontMetrics();
                    y -= (fm.ascent + fm.descent) / 2f;

                    canvas.drawText(currentTime, x, y, clockPaint);
                }
            } finally {
                if (canvas != null)
                    holder.unlockCanvasAndPost(canvas);
            }

            handler.removeCallbacks(drawRunnable);
            if (visible) {
                handler.postDelayed(drawRunnable, 1000);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                loadTheme(prefs.getString("theme_preference", "default")); // Reload theme in case it changed while invisible
                drawFrame();
            } else {
                handler.removeCallbacks(drawRunnable);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            handler.removeCallbacks(drawRunnable);
        }
    }
}
