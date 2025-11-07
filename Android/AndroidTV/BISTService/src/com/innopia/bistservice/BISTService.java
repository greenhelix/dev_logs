package com.innopia.bistservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class BISTService extends Service {

    private static final String TAG = "BIST";
    public static final String ACTION_BOOT_CLEANUP = "com.innopia.bistservice.ACTION_BOOT_CLEANUP";
    private static final String APK_NAME = "BIST.apk";
    private static final String BIST_PACKAGE_NAME = "com.innopia.bist";
    private static final String BIST_MAIN_ACTIVITY = BIST_PACKAGE_NAME + ".MainActivity";
    private static final String ACTION_INSTALL_COMPLETE = "com.innopia.bistservice.ACTION_INSTALL_COMPLETE";
    private static final String NOTIFICATION_CHANNEL_ID = "BISTServiceChannel";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    
    private boolean isSearching = false;
    private static final int MAX_USB_SCAN_RETRIES = 10;
    private static final long USB_SCAN_RETRY_DELAY_MS = 500;
    private int currentUsbScanRetries = 0;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "[Receiver] Received action: " + action);

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.i(TAG, "[Receiver] USB ATTACHED event detected.");
                if (!isSearching) {
                    isSearching = true;
                    currentUsbScanRetries = 0;
                    findAndInstallBistApkWithRetries(MAX_USB_SCAN_RETRIES);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.i(TAG, "[Receiver] USB DETACHED event detected.");
                uninstallPackage(BIST_PACKAGE_NAME);
                isSearching = false;
            } else if (ACTION_INSTALL_COMPLETE.equals(action)) {
                Log.i(TAG, "[Receiver] INSTALL_COMPLETE event received.");
                handleInstallComplete(context, intent);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "--- BISTService onCreate ---");
        createNotificationChannel();
        Notification notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("BIST Service").setContentText("Monitoring USB status.").setSmallIcon(R.mipmap.ic_launcher).build();
        startForeground(1, notification);
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_INSTALL_COMPLETE);
        registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);
		findAndInstallBistApkWithRetries(0);
        Log.d(TAG, "BroadcastReceiver registered.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "--- BISTService onStartCommand ---");
        if (intent != null && ACTION_BOOT_CLEANUP.equals(intent.getAction())) {
            Log.d(TAG, "[Boot] Handling boot cleanup action.");
            if (isPackageInstalled(this, BIST_PACKAGE_NAME)) {
                Log.i(TAG, "[Boot] BIST app is installed. Uninstalling as part of boot cleanup.");
                uninstallPackage(BIST_PACKAGE_NAME);
            }
        }
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        mainThreadHandler.removeCallbacksAndMessages(null);
        unregisterReceiver(mReceiver);
        Log.i(TAG, "--- BISTService onDestroy ---");
    }

    private void findAndInstallBistApkWithRetries(int retries) {
        executor.execute(() -> {
            Log.d(TAG, "[Install_Flow] 1. Starting USB scan (Retry " + currentUsbScanRetries + ")");
            String usbPath = getUsbStoragePath();
            if (usbPath != null) {
                Log.i(TAG, "[Install_Flow] 1. SUCCESS - USB storage found at: " + usbPath);
                File apkFile = new File(usbPath, APK_NAME);
                if (apkFile.exists()) {
                    Log.i(TAG, "[Install_Flow] 2. BIST.apk found. Proceeding with installation.");
                    installApk(apkFile.getAbsolutePath());
                } else {
                    Log.e(TAG, "[Install_Flow] 2. FAILURE - BIST.apk NOT FOUND at " + apkFile.getAbsolutePath());
                }
                isSearching = false;
            } else {
                Log.w(TAG, "[Install_Flow] 1. USB storage not found on this attempt.");
                if (currentUsbScanRetries < retries) {
                    currentUsbScanRetries++;
                    mainThreadHandler.postDelayed(() -> findAndInstallBistApkWithRetries(retries), USB_SCAN_RETRY_DELAY_MS);
                } else {
                    Log.e(TAG, "[Install_Flow] 1. FAILURE - Max retries reached. Could not find USB storage.");
                    isSearching = false;
                }
            }
        });
    }

    private void installApk(String apkPath) {
        executor.execute(() -> {
            try {
                PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(BIST_PACKAGE_NAME);
                int sessionId = packageInstaller.createSession(params);
                try (PackageInstaller.Session session = packageInstaller.openSession(sessionId)) {
                    try (InputStream in = new FileInputStream(apkPath); OutputStream out = session.openWrite("BISTInstaller", 0, -1)) {
                        byte[] buffer = new byte[65536]; int c; while ((c = in.read(buffer)) != -1) { out.write(buffer, 0, c); } session.fsync(out);
                    }
                    Intent intent = new Intent(ACTION_INSTALL_COMPLETE);
                    intent.setPackage(getPackageName());
                    intent.putExtra("OPERATION_TYPE", "INSTALL");
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            this, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    Log.i(TAG, "[Install_Flow] 8. Committing session. Waiting for callback...");
                    session.commit(pendingIntent.getIntentSender());
                }
            } catch (Exception e) {
                Log.e(TAG, "[Install_Flow] EXCEPTION in installApk!", e);
            }
        });
    }
    
    private void uninstallPackage(String packageName) {
        Log.w(TAG, "[Uninstall] Attempting to uninstall: " + packageName + ". THIS WILL KILL THE SERVICE PROCESS.");
        try {
            PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
            Intent intent = new Intent(ACTION_INSTALL_COMPLETE);
            intent.setPackage(getPackageName());
            intent.putExtra("OPERATION_TYPE", "UNINSTALL");
            PendingIntent sender = PendingIntent.getBroadcast(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            packageInstaller.uninstall(packageName, sender.getIntentSender());
            Log.i(TAG, "[Uninstall] Uninstallation command sent.");
        } catch (Exception e) {
            Log.e(TAG, "[Uninstall] EXCEPTION while sending uninstall command!", e);
        }
    }

    private void handleInstallComplete(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        String operationType = intent.getStringExtra("OPERATION_TYPE");
        Log.d(TAG, "[Result] handleInstallComplete: Type=" + operationType + ", Status=" + status + ", Msg=" + message);

        if ("INSTALL".equals(operationType)) {
            if (status == PackageInstaller.STATUS_SUCCESS) {
                Log.i(TAG, "[Launch_Flow] 9. Installation SUCCESS reported by callback. Scheduling app launch.");
                launchBistAppWithDelay(context, 1500);
            } else {
                Log.w(TAG, "[Launch_Flow] 9. Installation FAILED reported by callback. Starting verification loop...");
                verifyInstallationWithPolling(context);
            }
        } else if ("UNINSTALL".equals(operationType)) {
            Log.i(TAG, "[Result] Uninstallation " + (status == PackageInstaller.STATUS_SUCCESS ? "successful." : "failed."));
        }
    }

    private void verifyInstallationWithPolling(Context context) {
        final int MAX_VERIFICATION_ATTEMPTS = 10;
        final long VERIFICATION_INTERVAL_MS = 1000;
        final AtomicInteger currentAttempt = new AtomicInteger(0);
        
        Runnable verificationRunnable = new Runnable() {
            @Override
            public void run() {
                int attempt = currentAttempt.incrementAndGet();
                Log.d(TAG, "[Verification] Checking for package installation, attempt " + attempt + "/" + MAX_VERIFICATION_ATTEMPTS);
                
                if (isPackageInstalled(context, BIST_PACKAGE_NAME)) {
                    Log.i(TAG, "[Verification] SUCCESS! Package found. Treating as successful installation.");
                    launchBistAppWithDelay(context, 500);
                } else {
                    if (attempt < MAX_VERIFICATION_ATTEMPTS) {
                        mainThreadHandler.postDelayed(this, VERIFICATION_INTERVAL_MS);
                    } else {
                        Log.e(TAG, "[Verification] FAILED. Package was not found after " + MAX_VERIFICATION_ATTEMPTS + " attempts.");
                    }
                }
            }
        };
        mainThreadHandler.postDelayed(verificationRunnable, VERIFICATION_INTERVAL_MS);
    }

    private void launchBistAppWithDelay(Context context, long delayMillis) {
        Log.d(TAG, "[Launch_Flow] Scheduling app launch in " + delayMillis + "ms.");
        mainThreadHandler.postDelayed(() -> {
            Log.i(TAG, "[Launch_Flow] DELAY ENDED. Attempting to launch app NOW.");
            try {
                Log.d(TAG, "[Launch_Flow] Using hard-coded ComponentName to launch.");
                ComponentName comp = new ComponentName(BIST_PACKAGE_NAME, BIST_MAIN_ACTIVITY);
                Intent activityIntent = new Intent(Intent.ACTION_MAIN);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activityIntent.setComponent(comp);
                context.startActivity(activityIntent);
                Log.i(TAG, "[Launch_Flow] SUCCESS - startActivity() called for Component: " + comp.flattenToString());
            } catch (Exception e) {
                Log.e(TAG, "[Launch_Flow] CRITICAL FAILURE - EXCEPTION while calling startActivity()!", e);
            }
        }, delayMillis);
    }
    
    private boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "BIST Service Channel", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
    
    private String getUsbStoragePath() {
        StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) return null;
        for (StorageVolume volume : sm.getStorageVolumes()) {
            if (volume.isRemovable() && Environment.MEDIA_MOUNTED.equals(volume.getState())) {
                File dir = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ? volume.getDirectory() : getLegacyVolumeDirectory(volume);
                if (dir != null) return dir.getAbsolutePath();
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private File getLegacyVolumeDirectory(StorageVolume volume) {
        try {
            return (File) volume.getClass().getMethod("getPathFile").invoke(volume);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get path for volume on older Android version", e);
            return null;
        }
    }
}
