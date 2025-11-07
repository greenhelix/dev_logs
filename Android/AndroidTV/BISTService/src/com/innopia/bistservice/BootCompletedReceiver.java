package com.innopia.bistservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BIST";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "[BootReceiver] Boot completed event received.");
            Intent serviceIntent = new Intent(context, BISTService.class);
            serviceIntent.setAction(BISTService.ACTION_BOOT_CLEANUP);
            
            Log.i(TAG, "[BootReceiver] Starting BISTService with ACTION_BOOT_CLEANUP.");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
