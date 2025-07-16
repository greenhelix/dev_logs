package com.innopia.bist.util;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

public class UsbBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "BISTService";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        switch (action) {
//            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
//                Log.d(TAG, "USB device attached.");
//                Intent serviceIntent = new Intent(context, BISTService.class);
//                context.startService(serviceIntent);
//                break;

            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                Log.d(TAG, "USB device detached.");
                // Activity에 USB 분리 이벤트를 알려 제거 여부를 묻도록 합니다.
                Intent detachIntent = new Intent("com.innopia.bistservice.ACTION_USB_DETACHED");
                context.sendBroadcast(detachIntent);
                break;

            case "com.innopia.bistservice.ACTION_INSTALL_COMPLETE":
                handleInstallComplete(context, intent);
                break;

            case "com.innopia.bistservice.ACTION_UNINSTALL_COMPLETE":
                Log.d(TAG, "Uninstall complete broadcast received.");
                // 필요 시 제거 완료 후 처리 로직 추가
                break;
        }
    }

    private void handleInstallComplete(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);

        Log.d(TAG, "Installation status: " + status + ", Message: " + message);

        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Log.d(TAG, "Requesting user confirmation for installation.");
                Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(confirmIntent);
                break;

            case PackageInstaller.STATUS_SUCCESS:
                Log.d(TAG, "Package " + packageName + " installation successful.");
                Toast.makeText(context, "BIST App installed successfully.", Toast.LENGTH_SHORT).show();

                // 설치 성공 후 앱 실행
                try {
                    Thread.sleep(1000); // 앱이 완전히 설치될 시간을 줍니다.
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(launchIntent);
                        Log.d(TAG, "BIST App launched.");
                    } else {
                        Log.e(TAG, "Could not get launch intent for " + packageName);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;

            default:
                Log.e(TAG, "Install failed with status: " + status + ", message: " + message);
                Toast.makeText(context, "BIST App installation failed.", Toast.LENGTH_LONG).show();
                break;
        }
    }
}
