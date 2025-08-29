package com.innopia.bist.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class SysInfo {

	private static final String TAG = "BIST_SYS_INFO";

	private static String getSystemProperty(String propName) {
		try {
			Process process = Runtime.getRuntime().exec("getprop " + propName);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = reader.readLine();
			reader.close();
			return line != null ? line.trim() : "N/A";
		} catch (IOException e) {
			Log.e(TAG, "Failed to read system property " + propName, e);
			return "Error";
		}
	}

	public static String getSystemInfo() {
		String model = getSystemProperty("ro.product.model");
		String fwVer = getSystemProperty("ro.build.version.incremental");
		String androidVer = getSystemProperty("ro.build.version.release");
		String buildDate = getSystemProperty("ro.build.date");
		String serialNo = "This info will be shown in system app"; 
		Log.d(TAG, getSystemProperty("ro.serialno"));

		String line1 = String.format("Model: %-15s  FW Ver: %-15s", model, fwVer);
		String line2 = String.format("Build Date: %-15s Android Ver: %s",buildDate, androidVer);
		String line3 = "Serial No: "+serialNo;
		return line1 + "\n" + line2 + "\n" + line3;
	}
}
