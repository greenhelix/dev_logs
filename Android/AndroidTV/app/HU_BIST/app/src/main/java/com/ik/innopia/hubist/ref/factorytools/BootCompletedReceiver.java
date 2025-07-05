package com.innopia.factorytools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.os.Handler;

import java.io.FileNotFoundException;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
	private final String TAG = "InnoFactory.BootCompletedReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			if(SystemProperties.get("ro.boot.inno_boot_mode").equals("factory")){

				executeCommand("logcat -G 30m;");
				executeCommand("rm -rf /data/vendor/wifi/wpa/p2p_supplicant.conf");
				executeCommand("rm -rf /data/vendor/wifi/wpa/wpa_supplicant.conf");
				executeCommand("sync");
			}
		}
	}


	private void executeCommand (String cmd) {
		try {
			Process process = Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			e.printStackTrace();
			Log.d(TAG, e.toString());
		}
	}

}































