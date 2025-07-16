package com.innopia.bist.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbDetachReceiver extends BroadcastReceiver {
    private static final String TAG = "UsbDetachReceiver";
    public static final String ACTION_USB_DETACHED_APP = "com.innopia.bist.ACTION_USB_DETACHED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
            Log.d(TAG, "USB device detached.");
            // MainActivity로 브로드캐스트를 보내 UI 변경이나 로직 처리를 요청
            Intent detachIntent = new Intent(ACTION_USB_DETACHED_APP);
            context.sendBroadcast(detachIntent);
        }
    }
}
