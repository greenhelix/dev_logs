package com.innopia.bist.ver1.util;

import android.util.Log;

public class LogManager {

	private static final boolean DEBUG = true;
	private static final boolean INFO = true;

	public static void d(String tag, String log) {
		if (DEBUG) {
			Log.d(tag, log);
		}
	}

	public static void e(String tag, String log) {
		Log.e(tag, log);
	}

	public static void e(String tag, String log, Exception e) {
		Log.e(tag, log, e);
	}

	public static void i(String tag, String log) {
		if (INFO) {
			Log.i(tag, log);	
		}
	}

	public static void w(String tag, String log) {
		Log.w(tag, log);
	}
}
