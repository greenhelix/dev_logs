package com.innopia.bist.test;

import android.app.ActivityManager;
import android.content.Context;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

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
				callback.accept(new TestResult(TestStatus.ERROR,"Error: Context is null"));
				return;
			}

			String memoryUsage = checkMemoryUsage(context);
			String speedTestResult = runSpeedTest();

			ActivityManager.MemoryInfo memInfo = getMemoryInfo(context);
			double writeSpeed = parseSpeed(speedTestResult);
			boolean isMemoryOk = (memInfo.availMem / 1024 / 1024) > PASS_THRESHOLD_MEMORY_MB;
			boolean isSpeedOk = writeSpeed > PASS_THRESHOLD_SPEED_MBps;
			String finalStatus = (isMemoryOk && isSpeedOk) ? "Result: PASS" : "Result: FAIL";
			String result = "== Memory Test Result ==\n" + memoryUsage + "\n" + speedTestResult + "\n\n" + finalStatus;

			if (result.contains("PASS")) {
				callback.accept(new TestResult(TestStatus.PASSED, "Memory Test pass \n"+result));
			} else {
				callback.accept(new TestResult(TestStatus.FAILED, "Memory Test fail \n"+result));
			}
		});
	}

	private ActivityManager.MemoryInfo getMemoryInfo(Context context) {
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(memoryInfo);
		return memoryInfo;
	}

	private String checkMemoryUsage(Context context) {
		ActivityManager.MemoryInfo mi = getMemoryInfo(context); // null 체크 하고 null이면 fail
		long availableMegs = mi.availMem / 1048576L; // 1024*1024
		long totalMegs = mi.totalMem / 1048576L; // 이게 0이면  Fail
		return String.format("Memory Usage: %d MB / %d MB", (totalMegs - availableMegs), totalMegs);
	}

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

	private double parseSpeed(String speedResult) {
		if (!speedResult.contains("Write")) return 0.0;
		try {
			String[] parts = speedResult.split(" ");
			return Double.parseDouble(parts[2]);
		} catch(Exception e) {
			return 0.0;
		}
	}
}
