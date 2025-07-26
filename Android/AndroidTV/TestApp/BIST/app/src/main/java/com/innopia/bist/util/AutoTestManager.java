package com.innopia.bist.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.innopia.bist.test.*; // Import all test classes
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

public class AutoTestManager {

	private static final String TAG = "AutoTestManager";
	private static final String CONFIG_FILE = "config.json";

	private final Context context;
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private final Queue<TestType> testQueue = new LinkedList<>();
	private final AutoTestListener listener;

	// [NEW] A map to hold instances of our test models.
	private final Map<TestType, Test> testModelMap;

	public interface AutoTestListener {
		void onTestStatusChanged(TestType type, TestStatus status, String message);
		void onAllTestsCompleted();
		void onAutoTestError(String errorMessage);
	}

	public AutoTestManager(Context context, AutoTestListener listener) {
		this.context = context;
		this.listener = listener;
		this.testModelMap = new EnumMap<>(TestType.class);
		// [NEW] Initialize all test models. A TestFactory would be a good improvement here.
		initializeTestModels();
	}

	private void initializeTestModels() {
		testModelMap.put(TestType.WIFI, new WifiTest());
		testModelMap.put(TestType.BLUETOOTH, new BluetoothTest());
		testModelMap.put(TestType.ETHERNET, new EthernetTest());
		testModelMap.put(TestType.CPU, new CpuTest());
		testModelMap.put(TestType.MEMORY, new MemoryTest());
		testModelMap.put(TestType.VIDEO, new VideoTest());
		testModelMap.put(TestType.HDMI, new HdmiTest());
		testModelMap.put(TestType.USB, new UsbTest());
		testModelMap.put(TestType.RCU, new RcuTest());
	}


	public void startAutoTestFromRawResource(Context context, int rawResId) {
		try (InputStream is = context.getResources().openRawResource(rawResId)) {
			byte[] buffer = new byte[is.available()];
			int len = is.read(buffer);
			if (len != buffer.length) {
				String msg = "Could not read full resource (expected " + buffer.length + ", got " + len+")";
				Log.e(TAG, msg);
				if (listener != null) listener.onAutoTestError(msg);
				return;
			}
			String json = new String(buffer, StandardCharsets.UTF_8);
			processConfigJson(json);
		} catch (Exception e) {
			String msg = "Error reading test_config.json: " + e.getMessage();
			Log.e(TAG, msg, e);
			if (listener != null) listener.onAutoTestError(msg);
		}
	}

	private void processConfigJson(String json) {
		try {
			JSONObject config = new JSONObject(json);
			if ("auto".equalsIgnoreCase(config.optString("mode"))) {
				Log.d(TAG, "Auto test mode detected. Parsing test sequence.");
				JSONArray tests = config.getJSONArray("tests");
				testQueue.clear();
				for (int i = 0; i < tests.length(); i++) {
					TestType type = TestType.valueOf(tests.getString(i).toUpperCase());
					testQueue.add(type);
					if (listener != null)
						listener.onTestStatusChanged(type, TestStatus.PENDING, null);
				}
				processNextTest();
			} else {
				String errorMsg = "Mode is not 'auto' in config";
				Log.w(TAG, errorMsg);
				if (listener != null) listener.onAutoTestError(errorMsg);
			}
		} catch (Exception e) {
			String msg = "Error parsing config JSON: " + e.getMessage();
			Log.e(TAG, msg, e);
			if (listener != null) listener.onAutoTestError(msg);
		}
	}

	public void startAutoTestFromUsb(String usbPath) {
		File configFile = new File(usbPath, CONFIG_FILE);
		if (!configFile.exists()) {
			String errorMsg = "config.json not found in: " + usbPath;
			Log.e(TAG, errorMsg);
			if (listener != null) listener.onAutoTestError(errorMsg);
			return;
		}

		try (FileInputStream fis = new FileInputStream(configFile)) {
			byte[] buffer = new byte[(int) configFile.length()];
			fis.read(buffer);
			String json = new String(buffer, StandardCharsets.UTF_8);
			JSONObject config = new JSONObject(json);

			if ("auto".equalsIgnoreCase(config.optString("mode"))) {
				Log.d(TAG, "Auto test mode detected. Parsing test sequence.");
				JSONArray tests = config.getJSONArray("tests");
				testQueue.clear();
				for (int i = 0; i < tests.length(); i++) {
					TestType type = TestType.valueOf(tests.getString(i).toUpperCase());
					testQueue.add(type);
					// Notify listener to set initial state to PENDING
					if (listener != null) listener.onTestStatusChanged(type, TestStatus.PENDING, null);
				}
				processNextTest();
			} else {
				String errorMsg = "Mode is not 'auto' in config.json";
				Log.w(TAG, errorMsg);
				if (listener != null) listener.onAutoTestError(errorMsg);
			}
		} catch (Exception e) {
			String errorMsg = "Error reading or parsing config.json";
			Log.e(TAG, errorMsg, e);
			if (listener != null) listener.onAutoTestError(errorMsg + ": " + e.getMessage());
		}
	}

	private void processNextTest() {
		if (testQueue.isEmpty()) {
			Log.d(TAG, "All tests in queue are completed.");
			if (listener != null) listener.onAllTestsCompleted();
			return;
		}

		TestType currentTestType = testQueue.poll();
		if (listener != null) listener.onTestStatusChanged(currentTestType, TestStatus.RUNNING, null);
		runTestAsync(currentTestType);
	}

	// [MODIFIED] This method now executes the real test logic.
	private void runTestAsync(final TestType testType) {
		Test testModel = testModelMap.get(testType);
		if (testModel == null) {
			Log.e(TAG, "No test model found for type: " + testType);
			mainHandler.post(() -> {
				if (listener != null) listener.onTestStatusChanged(testType, TestStatus.FAILED, "Test implementation not found.");
				processNextTest(); // Continue with the next test
			});
			return;
		}

		// The callback that the test model will invoke upon completion.
		Consumer<TestResult> callback = result -> {
			mainHandler.post(() -> {
				if (listener != null) listener.onTestStatusChanged(testType, result.getStatus(), result.getMessage());

				if (result.getStatus() != TestStatus.WAITING_FOR_USER) {
					processNextTest();
				}
			});
		};

		// Execute the test on a background thread.
		new Thread(() -> {
			Log.d(TAG, "Running auto test for: " + testType.name());
			Map<String, Object> params = new HashMap<>();
			params.put("context", context);
			// The Test model is now responsible for its own logic, including user interaction prompts.
			testModel.runAutoTest(params, callback);
		}).start();
	}

	public void resumeTestAfterUserAction() {
		Log.d(TAG, "User confirmed action. Resuming test process.");
		// The logic for what to do after user confirmation should ideally be inside the
		// specific test model. For now, we assume it means we can proceed.
		// A more advanced implementation might re-run the check.
		processNextTest();
	}
}
