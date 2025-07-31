package com.innopia.bist.test;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MemoryTest implements Test {
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private static final long PASS_THRESHOLD_MEMORY_MB = 500;
	private static final double PASS_THRESHOLD_SPEED_MBps = 100.0;

	@Override
	public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
//        executeTest(params, callback);
		memoryTest(params, callback);
	}

	@Override
	public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
//        executeTest(params, callback);
		memoryTest(params, callback);
	}

	private void executeTest(Map<String, Object> params, Consumer<TestResult> callback) {
		executor.execute(() -> {
			Context context = (Context) params.get("context");
			if (context == null) {
				callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null"));
				return;
			}
			callback.accept(new TestResult(TestStatus.PASSED, "Memory Test pass"));
		});
	}

	private void memoryTest(Map<String, Object> params, Consumer<TestResult> callback) {
		executor.execute(() -> {
			Context context = (Context) params.get("context");
			if (context == null) {
				callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null"));
				return;
			}

			try {
				// 1. Get Device Model info
				String modelInfo = getDeviceModel();

				// 2. Get Memory Usage info
				String usageInfo = getMemoryUsageInfo(context);

				// 3. Get Storage Read/Write Speed
				String speedInfo = runStorageSpeedTest(context);

				// 4. Build the final result string
				StringBuilder resultBuilder = new StringBuilder();
				resultBuilder.append(modelInfo).append("\n");
				resultBuilder.append(usageInfo).append("\n");
				resultBuilder.append(speedInfo);

				// 5. If all information is gathered, the test is considered PASSED
				callback.accept(new TestResult(TestStatus.PASSED, "Memory Test Pass\n"+resultBuilder));

			} catch (Exception e) {
				// If any error occurs during info retrieval, the test is FAILED
				callback.accept(new TestResult(TestStatus.FAILED, "Memory Test Fail\n" + e.getMessage()));
			}
		});
	}
	private String getSystemProperty(String propName) {
		try {
			Process process = Runtime.getRuntime().exec("getprop " + propName);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = reader.readLine();
			reader.close();
			return (line != null && !line.isEmpty()) ? line : null;
		} catch (Exception e) {
			Log.w("HardwareInfo", "Failed to read system property: " + propName, e);
			return null;
		}
	}

	private String getDeviceModel() {
		String ddrType = getSystemProperty("ro.runtime.innopia.ddr");
		return ddrType;
	}

	/**
	 * Retrieves current memory usage (Available / Total).
	 */
	private String getMemoryUsageInfo(Context context) {
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
		if (activityManager != null) {
			activityManager.getMemoryInfo(memInfo);
			double totalMemMB = memInfo.totalMem / (1024.0 * 1024.0 );
			double availMemMB = memInfo.availMem / (1024.0 * 1024.0 );
			return String.format(Locale.US, "Usage: %d MB / %d MB Available", (int)availMemMB, (int)totalMemMB);
		}
		return "Usage: Information not available";
	}

	private String runStorageSpeedTest(Context context) throws Exception {
		File tempFile = new File(context.getCacheDir(), "speed_test.tmp");
		byte[] data = new byte[10 * 1024 * 1024]; // 10 MB test file
		double writeSpeed, readSpeed;

		// Write Test
		long startTime = System.nanoTime();
		try (FileOutputStream fos = new FileOutputStream(tempFile)) {
			fos.write(data);
		}
		long endTime = System.nanoTime();
		double writeDurationSeconds = (endTime - startTime) / 1_000_000_000.0;
		writeSpeed = data.length / (1024.0 * 1024.0) / writeDurationSeconds; // MB/s

		// Read Test
		startTime = System.nanoTime();
		try (FileInputStream fis = new FileInputStream(tempFile)) {
			while (fis.read(data) != -1) {
				// Reading data
			}
		}
		endTime = System.nanoTime();
		double readDurationSeconds = (endTime - startTime) / 1_000_000_000.0;
		readSpeed = data.length / (1024.0 * 1024.0) / readDurationSeconds; // MB/s

		tempFile.delete(); // Clean up the test file

		return String.format(Locale.US, "Read/Write Speed: Read %d MB/s, Write %d MB/s", (int)readSpeed, (int)writeSpeed);
	}

//	private ActivityManager.MemoryInfo getMemoryInfo(Context context) {
//		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//		ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
//		activityManager.getMemoryInfo(memoryInfo);
//		return memoryInfo;
//	}

//	private String checkMemoryUsage(Context context) {
//		ActivityManager.MemoryInfo mi = getMemoryInfo(context);
//		long availableMegs = mi.availMem / 1048576L; // 1024*1024
//		long totalMegs = mi.totalMem / 1048576L; // 이게 0이면  Fail
//		return String.format("Memory Usage: %d MB / %d MB", (totalMegs - availableMegs), totalMegs);
//	}

	private String runSpeedTest() {
		try {
			int size = 50 * 1024 * 1024;
			byte[] data = new byte[size];

			long startTime = System.nanoTime();
			for (int i = 0; i < size; i++) {
				data[i] = (byte) i;
			}
			long endTime = System.nanoTime();
			long writeDuration = endTime - startTime;
			double writeSpeed = (double) size / (1024*1024) / (writeDuration / 1_000_000_000.0);

			startTime = System.nanoTime();
			int temp = 0;
			for (int i = 0; i < size; i++) {
				temp += data[i];
			}
			endTime = System.nanoTime();
			long readDuration = endTime - startTime;
			double readSpeed = (double) size / (1024*1024) / (readDuration / 1_000_000_000.0);

			return String.format("Memory Speed: Write %.2f MB/s, Read %.2f MB/s", writeSpeed, readSpeed); // 둘다 0보다 크면 pass

		} catch (OutOfMemoryError e) {
			return "Memory Speed: Test Failed (Out of Memory)";
		}
	}
}
