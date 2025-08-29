package com.innopia.bist.util;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class FileUtils {

	private static final String TAG = "FileUtils";

	public static Bundle getConfigValues(Context context, String filename) {
		// get usb storage path
		StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
		if (sm == null) {
			LogManager.d(TAG, "sm == null");
			return null;
		}
		String path = "";
		for (StorageVolume volume : sm.getStorageVolumes()) {
			if (volume.isRemovable() && Environment.MEDIA_MOUNTED.equals(volume.getState())) {
				File dir = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ? volume.getDirectory() : getLegacyVolumeDirectory(volume);
				if (dir != null) {
					path = dir.getAbsolutePath();
					break;
				}
			}
		}
		if (TextUtils.isEmpty(path)) {
			path = "/storage/emulated/0";
		}

		// read config
		File config = new File(path + "/" + filename);
		if (!config.exists()) {
			LogManager.d(TAG, "!config.exists()");
			return null;
		}
		if (!config.canRead()) {
			LogManager.d(TAG, "!config.canRead()");
			return null;
		}
		try {
			Bundle bundle = new Bundle();
			FileInputStream fis = new FileInputStream(config);
			XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
			xpp.setInput(fis, "UTF-8");

			int type = xpp.getEventType();
			String name = null;
			while (type != XmlPullParser.END_DOCUMENT) {
				switch (type) {
					case XmlPullParser.START_TAG:
						name = xpp.getName();
						break;
					case XmlPullParser.END_TAG:
						name = null;
						break;
					case XmlPullParser.TEXT:
						if (name != null) {
							String text = xpp.getText();
							bundle.putString(name, text);
						}
						break;
				}
				type = xpp.next();
			}
			fis.close();
			return bundle;
		} catch (Exception e) {
			LogManager.e(TAG, "config error", e);
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	private static File getLegacyVolumeDirectory(StorageVolume volume) {
		try {
			return (File) volume.getClass().getMethod("getPathFile").invoke(volume);
		} catch (Exception e) {
			LogManager.e(TAG, "Failed to get path for volume on older Android verision", e);
			return null;
		}
	}

	public static String readFromFile(String filePath) {
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader reader = null;
		try {
			File file = new File(filePath);
			if (file.exists() && file.canRead()) {
				reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
				}
			} else {
				// Handle cases where the file doesn't exist or isn't readable
				// e.g., log a warning, return null, or throw a specific exception
				System.err.println("File not found or not readable: " + filePath);
			}
		} catch (IOException e) {
			// Log the exception
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return stringBuilder.toString().trim(); // trim to remove any leading/trailing whitespace/newlines
	}
}
