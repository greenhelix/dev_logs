package com.innopia.bist.ver2.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.innopia.bist.ver2.service.OsdOverlayService;

/**
 * OSD 오버레이 매니저
 */
public class OsdManager {

    private static final String TAG = "OsdManager";

    /**
     * OSD 서비스 시작
     */
    public static void startOsdService(Context context) {
        if (!canDrawOverlays(context)) {
            Log.w(TAG, "Cannot start OSD: overlay permission not granted");
            return;
        }

        try {
            Intent serviceIntent = new Intent(context, OsdOverlayService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            Log.d(TAG, "OSD service started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start OSD service", e);
        }
    }

    /**
     * OSD 서비스 중지
     */
    public static void stopOsdService(Context context) {
        try {
            Intent serviceIntent = new Intent(context, OsdOverlayService.class);
            context.stopService(serviceIntent);
            Log.d(TAG, "OSD service stopped");
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop OSD service", e);
        }
    }

    /**
     * OSD 오버레이 권한 확인
     */
    public static boolean canDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true; // API 23 미만은 자동으로 권한 허용
    }

    /**
     * OSD 권한 설정 화면으로 이동
     */
    public static Intent getOverlayPermissionIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
        return intent;
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
