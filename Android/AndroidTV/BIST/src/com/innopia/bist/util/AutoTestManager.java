package com.innopia.bist.util;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.innopia.bist.test.BluetoothTest;
import com.innopia.bist.test.CpuTest;
import com.innopia.bist.test.EthernetTest;
import com.innopia.bist.test.HdmiTest;
import com.innopia.bist.test.MemoryTest;
import com.innopia.bist.test.RcuTest;
import com.innopia.bist.test.Test;
import com.innopia.bist.test.UsbTest;
import com.innopia.bist.test.VideoTest;
import com.innopia.bist.test.WifiTest;
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.VideoTestViewModel;

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

import org.json.JSONArray;
import org.json.JSONObject;

public class AutoTestManager {

	private static final String TAG = "BIST_AutoTestManager";
	private static final String CONFIG_FILE = "config.json";

	private final Context context;
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	//private final Queue<TestType> testQueue = new LinkedList<>();
	private final Queue<TestConfig> testQueue = new LinkedList<>();
	private final AutoTestListener listener;

	private final Map<TestType, Test> testModelMap;

	// Added state variable to handle paused tests waiting for user input.
	private TestType currentTestType;
	private Map<String, Object> currentParams;
	private final MainViewModel mainViewModel;

	public interface AutoTestListener {
		void onTestStatusChanged(TestType type, TestStatus status, String message);
		void onAllTestsCompleted();
		void onAutoTestError(String errorMessage);
	}

	public AutoTestManager(Context context, MainViewModel mainViewModel , AutoTestListener listener) {
		this.context = context;
		this.mainViewModel = mainViewModel;
		this.listener = listener;
		this.testModelMap = new EnumMap<>(TestType.class);
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

	public void proceedToNextTest() {
		Log.d(TAG, "Proceeding to next test externally.");
		// The main handler ensures this runs on the main thread,
		// which is safe for queue processing.
		mainHandler.post(this::processNextTest);
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
					//testQueue.add(type);
					testQueue.add(new TestConfig(type));
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
					//testQueue.add(type);
					testQueue.add(new TestConfig(type));
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

	public void startAutoTestFromConfig(Bundle config) {
		if (config == null) {
			Log.e(TAG, "config is null");
			return;
		}
		Log.d(TAG, "starting auto test, parsing test sequence...");
		testQueue.clear();
		testQueue.add(new TestConfig(TestType.ETHERNET));
		testQueue.add(new TestConfig(TestType.WIFI, config));
		testQueue.add(new TestConfig(TestType.BLUETOOTH));
		testQueue.add(new TestConfig(TestType.USB));
		testQueue.add(new TestConfig(TestType.HDMI));
		testQueue.add(new TestConfig(TestType.CPU));
		testQueue.add(new TestConfig(TestType.MEMORY));
		testQueue.add(new TestConfig(TestType.VIDEO));
		processNextTest();
	}

	private void processNextTest() {
		if (testQueue.isEmpty()) {
			Log.d(TAG, "All tests in queue are completed.");
			if (listener != null) listener.onAllTestsCompleted();
			currentTestType = null;
			return;
		}

		TestConfig config = testQueue.poll();
		currentTestType = config.getType();
		if (listener != null) listener.onTestStatusChanged(currentTestType, TestStatus.RUNNING, null);

		currentParams = new HashMap<>();
		currentParams.put("context", context);
		currentParams.put("isResume", false);
		currentParams.put("config", config.getConfig());

		runTestAsync(currentTestType, currentParams);
	}

	private void runTestAsync(final TestType testType, final Map<String, Object> params) {
		Test testModel = testModelMap.get(testType);
		if (testModel == null) {
			Log.e(TAG, "No test model found for type: " + testType);
			mainHandler.post(() -> {
				if (listener != null) listener.onTestStatusChanged(testType, TestStatus.FAILED, "Test implementation not found.");
				processNextTest();
			});
			return;
		}

		params.put("mainViewModel", mainViewModel);

		Consumer<TestResult> callback = result -> {
			mainHandler.post(() -> {
				if (listener != null) listener.onTestStatusChanged(testType, result.getStatus(), result.getMessage());

				if (testType != TestType.VIDEO && result.getStatus() != TestStatus.WAITING_FOR_USER) {
					processNextTest();
				} else if (testType == TestType.VIDEO) {
					Log.d(TAG, "VIDEO TESTING....");
				} else {
					Log.d(TAG, "Test "+ testType + " is paused, waiting for user action.");
				}
			});
		};

		new Thread(() -> {
			Log.d(TAG, "Running auto test for: " + testType.name());
			try {
				testModel.runAutoTest(params, callback);
			} catch (Exception e) {
				Log.d(TAG, "Unhanced exception in " + testType.name() + " auto-test.", e);
				TestResult errorResult = new TestResult(TestStatus.ERROR, e.getMessage());
				callback.accept(errorResult);
			}
		}).start();
	}

	/**
	 * @param userConfirmed The user's choice from the dialog (true for YES/OK, false for NO).
	 */
	public void resumeTestAfterUserAction(boolean userConfirmed) {
		if (currentTestType == null) {
			Log.w(TAG, "resumeTestAfterUserAction called but no test is paused.");
			return;
		}
		Log.d(TAG, "User action received: " + userConfirmed + " Resuming test: " + currentTestType.name());
		currentParams.put("isResume", true);
		currentParams.put("userChoice", userConfirmed);

		if ( listener != null) {
			listener.onTestStatusChanged(currentTestType, TestStatus.RUNNING, "Resuming after user action ...");
		}

		runTestAsync(currentTestType, currentParams);
	}
}
