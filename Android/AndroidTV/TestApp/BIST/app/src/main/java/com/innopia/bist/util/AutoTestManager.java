package com.innopia.bist.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.innopia.bist.util.TestStatus;
import com.innopia.bist.util.TestType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

// This class orchestrates the automatic testing process.
public class AutoTestManager {
	private static final String TAG = "AutoTestManager";
	private static final String CONFIG_FILE = "config.json";

	private final Context context;
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private final Queue<TestType> testQueue = new LinkedList<>();
	private AutoTestListener listener;

	public interface AutoTestListener {
		void onTestStatusChanged(TestType type, TestStatus status, String message);
		void onAllTestsCompleted();
		void onAutoTestError(String msg);
	}

	public AutoTestManager(Context context, AutoTestListener listener) {
		this.context = context;
		this.listener = listener;
	}

	// Attempts to find and start the auto test from a connected USB drive.
	public void startAutoTestFromUsb(String usbPath) {
		File configFile = new File(usbPath, CONFIG_FILE);
		if (!configFile.exists()) {
			Log.d(TAG, "config.json not found in: " + usbPath);
			return;
		}

		try (FileInputStream fis = new FileInputStream(configFile)) {
			byte[] buffer = new byte[(int) configFile.length()];
			fis.read(buffer);
			String json = new String(buffer, StandardCharsets.UTF_8);
			JSONObject config = new JSONObject(json);

			if ("auto".equalsIgnoreCase(config.optString("mode"))) {
				Log.d(TAG, "Auto test mode detected. Starting tests.");
				JSONArray tests = config.getJSONArray("tests");
				testQueue.clear();
				for (int i = 0; i < tests.length(); i++) {
					// Convert string from JSON to TestType enum
					TestType type = TestType.valueOf(tests.getString(i).toUpperCase());
					testQueue.add(type);
					// Notify listener to set initial state to PENDING
					listener.onTestStatusChanged(type, TestStatus.PENDING, null);
				}
				processNextTest();
			}
		} catch (Exception e) {
			Log.e(TAG, "Error reading or parsing config.json", e);
		}
	}

	// Processes the next test in the queue.
	private void processNextTest() {
		if (testQueue.isEmpty()) {
			Log.d(TAG, "All tests completed.");
			if (listener != null) {
				listener.onAllTestsCompleted();
			}
			return;
		}

		TestType currentTest = testQueue.poll();
		listener.onTestStatusChanged(currentTest, TestStatus.RUNNING, null);

		// Simulate running a test. Replace this with your actual test logic.
		// The test should run in a background thread.
		runTestAsync(currentTest);
	}

	// This is a placeholder for your actual test execution logic.
	private void runTestAsync(final TestType test) {
		new Thread(() -> {
			Log.d(TAG, "Running test: " + test.name());
			try {
				// Simulate test duration
				Thread.sleep(2000);

				// --- START of Example Test Logic ---
				TestStatus result;
				String message = null;

				// Example of a test that needs user action
				if (test == TestType.HDMI) {
					result = TestStatus.WAITING_FOR_USER;
					message = "HDMI 케이블을 연결하고 확인 버튼을 눌러주세요.";
				}
				// Example of a test that passes/fails randomly
				else {
					boolean success = Math.random() > 0.3; // 70% chance of success
					result = success ? TestStatus.PASSED : TestStatus.FAILED;
				}
				// --- END of Example Test Logic ---

				// Post result back to the main thread
				final TestStatus finalResult = result;
				final String finalMessage = message;
				mainHandler.post(() -> {
					listener.onTestStatusChanged(test, finalResult, finalMessage);
					// If the test didn't pause for user action, proceed to the next one.
					if (finalResult != TestStatus.WAITING_FOR_USER) {
						processNextTest();
					}
				});

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				Log.e(TAG, "Test thread interrupted for: " + test.name());
			}
		}).start();
	}

	// Called when the user confirms a dialog (e.g., "OK, I plugged in the cable").
	public void resumeTestAfterUserAction() {
		Log.d(TAG, "Resuming test process.");
		// Here you could add logic to re-check the state (e.g., verify HDMI signal)
		// For simplicity, we will just proceed to the next test.
		processNextTest();
	}
}

