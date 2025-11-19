package com.innopia.bist.ver2.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.innopia.bist.ver2.R;

public class OsdOverlayService extends Service {

    private static final String TAG = "OsdOverlayService";
    private static final String CHANNEL_ID = "OSD_SERVICE_CHANNEL";
    private static final int NOTIFICATION_ID = 1001;

    // ⭐ Stop 액션을 위한 Intent Action 정의
    public static final String ACTION_STOP_OSD = "com.innopia.bist.ver2.action.STOP_OSD";

    private WindowManager windowManager;
    private View overlayView;
    private Handler handler = new Handler(Looper.getMainLooper());

    private TextView cpuUsageText;
    private TextView memoryUsageText;
    private TextView temperatureText;

    private static final int UPDATE_INTERVAL = 1000;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "OSD Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // ⭐ Stop 액션 처리
        if (intent != null && ACTION_STOP_OSD.equals(intent.getAction())) {
            Log.d(TAG, "Stop action received. Stopping service.");
            stopSelf(); // 서비스 종료
            return START_NOT_STICKY;
        }

        // 서비스 시작 시 Foreground로 전환
        startForeground(NOTIFICATION_ID, createNotification());
        createOverlayView();
        startPeriodicUpdate();

        return START_STICKY;
    }

    private Notification createNotification() {
        createNotificationChannel();

        // ⭐ Stop 액션에 대한 PendingIntent 생성
        Intent stopIntent = new Intent(this, OsdOverlayService.class);
        stopIntent.setAction(ACTION_STOP_OSD);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // ⭐ Notification에 "Stop" 액션 버튼 추가
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("OSD Monitor")
                .setContentText("System monitoring active")
                .setSmallIcon(R.drawable.ic_launcher) // 아이콘을 확인하세요
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "OSD Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("System monitoring service");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void createOverlayView() {
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.overlay_osd, null);

        cpuUsageText = overlayView.findViewById(R.id.cpu_usage_text);
        memoryUsageText = overlayView.findViewById(R.id.memory_usage_text);
        temperatureText = overlayView.findViewById(R.id.temperature_text);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 20;
        params.y = 20;

        try {
            windowManager.addView(overlayView, params);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add overlay view", e);
        }
    }

    private void startPeriodicUpdate() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateOsdInfo();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        }, UPDATE_INTERVAL);
    }

    private void updateOsdInfo() {
        try {
            float cpuUsage = getCpuUsage();
            if (cpuUsageText != null) {
                cpuUsageText.setText(String.format("CPU: %.1f%%", cpuUsage));
            }

            long memoryUsage = getMemoryUsage();
            if (memoryUsageText != null) {
                memoryUsageText.setText(String.format("MEM: %d MB", memoryUsage));
            }

            float temperature = getTemperature();
            if (temperatureText != null) {
                temperatureText.setText(String.format("TEMP: %.1f°C", temperature));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating OSD info", e);
        }
    }

    private float getCpuUsage() {
        return 45.5f;
    }

    private long getMemoryUsage() {
        try {
            android.app.ActivityManager activityManager =
                    (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

            if (activityManager != null) {
                android.app.ActivityManager.MemoryInfo memInfo =
                        new android.app.ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memInfo);

                long usedMemory = (memInfo.totalMem - memInfo.availMem) / (1024 * 1024);
                return usedMemory;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting memory usage", e);
        }

        return 0;
    }

    private float getTemperature() {
        return 52.3f;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "OSD Service destroyed");

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                Log.e(TAG, "Error removing overlay view", e);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
