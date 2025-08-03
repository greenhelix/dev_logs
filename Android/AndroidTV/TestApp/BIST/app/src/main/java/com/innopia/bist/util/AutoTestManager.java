package com.innopia.bist.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.innopia.bist.test.*;
import com.innopia.bist.util.TestConfig;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class AutoTestManager {

	private static final String TAG = "BIST_AutoTestManager";
	private static final String CONFIG_FILE = "config.json";

	private final Context context;
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private final Queue<TestType> testQueue = new LinkedList<>();
	private final AutoTestListener listener;

	private final Map<TestType, Test> testModelMap;

	private TestType currentTestType;
	private Map<String, Object> currentParams;
	private static final long TEST_TIMEOUT_MS = 15000;
	private final Map<TestType, Long> customTimeoutMap;
	private static final long UI_TRANSITION_DELAY_MS = 1000;

	public interface AutoTestListener {
		void onTestStatusChanged(TestType type, TestStatus status, String message);
		void onAllTestsCompleted();
		void onAutoTestError(String errorMessage);
		void onTestTimeout(TestType type);
	}

	public AutoTestManager(Context context, AutoTestListener listener) {
		this.context = context;
		this.listener = listener;
		this.testModelMap = new EnumMap<>(TestType.class);
		this.customTimeoutMap = new EnumMap<>(TestType.class);
		initializeTestModels();
		initializeCustomeTimeouts();
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

	private void initializeCustomeTimeouts() {
		customTimeoutMap.put(TestType.VIDEO, 60000L);
//		customTimeoutMap.put(TestType.blash, xxxL);
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
//			processConfigJson(json);
			procConfigAndStartTest(json);
		} catch (Exception e) {
			String msg = "Error reading test_config.json: " + e.getMessage();
			Log.e(TAG, msg, e);
			if (listener != null) listener.onAutoTestError(msg);
		}
	}

	private TestConfig testConfig;

	public void procConfigAndStartTest(String json) {
		try {
			JSONObject configJson = new JSONObject(json);
			if (!"auto".equalsIgnoreCase(configJson.optString("mode"))) {
				Log.d(TAG, "!!!! Config Not Mentioned Mode is AutoTest.\nPlease Check config.json File.");
				return;
			}

			this.testConfig = new TestConfig(json);

			JSONObject testObject = configJson.getJSONObject("tests");
			JSONArray testOrder = testObject.getJSONArray("order");

			testQueue.clear();

			for (int i = 0; i < testOrder.length(); i++) {
				TestType type = TestType.valueOf(testOrder.getString(i).toUpperCase());
				testQueue.add(type);
				if (listener != null) {
					listener.onTestStatusChanged(type, TestStatus.PENDING, null);
				}
			}

			processNextTest();

		} catch (Exception e) {
			String msg = "Error parsing config JSON: " + e.getMessage();
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
			currentTestType = null;
			return;
		}

		currentTestType = testQueue.poll();
		if (listener != null) listener.onTestStatusChanged(currentTestType, TestStatus.RUNNING, null);

		currentParams = new HashMap<>();
		currentParams.put("context", context);
		currentParams.put("isResume", false);

		if (testConfig != null) {
			switch (currentTestType) {
				case WIFI:
					if (testConfig.wifiConfig != null ) {
						currentParams.put("config", testConfig.wifiConfig);
					}
					break;
				case ETHERNET:
					if (testConfig.ethernetConfig != null ) {
						currentParams.put("config", testConfig.ethernetConfig);
					}
					break;
				case BLUETOOTH:
					if (testConfig.bluetoothConfig != null ) {
						currentParams.put("config", testConfig.bluetoothConfig);
					}
					break;
			}
		}

		runTestAsync(currentTestType, currentParams);
	}

	private void concludeAndProceed(TestType type, TestResult result) {
		// 모든 작업은 UI 스레드에서 순차적으로 실행되도록 보장합니다.
		mainHandler.post(() -> {
			// 1. 리스너를 통해 UI 상태를 최종적으로 업데이트합니다.
			if (listener != null) {
				// 타임아웃으로 인한 실패인 경우, 전용 콜백을 먼저 호출
				if (result.getStatus() == TestStatus.FAILED && result.getMessage().contains("timed out")) {
					listener.onTestTimeout(type);
				}
				listener.onTestStatusChanged(type, result.getStatus(), result.getMessage());
			}

			// 2. WAITING_FOR_USER 상태가 아니면, UI가 업데이트될 시간을 기다린 후 다음 테스트로 진행합니다.
			if (result.getStatus() != TestStatus.WAITING_FOR_USER) {
				Log.d(TAG, "Test " + type.name() + " concluded with " + result.getStatus() + ". Proceeding to next test after " + UI_TRANSITION_DELAY_MS + "ms delay.");
				mainHandler.postDelayed(this::processNextTest, 1000);
			} else {
				Log.d(TAG, "Test " + type.name() + " is paused, waiting for user action.");
			}
		});
	}

	private void runTestAsync(final TestType testType, final Map<String, Object> params) {

		final long timeoutMs = customTimeoutMap.getOrDefault(testType, TEST_TIMEOUT_MS);

		Test testModel = testModelMap.get(testType);

		if (testModel == null) {
			Log.e(TAG, "No test model found for type: " + testType);
			TestResult missingResult = new TestResult(TestStatus.FAILED, "Test implementation not found.");

			mainHandler.postDelayed(() ->  concludeAndProceed(testType, missingResult), timeoutMs);
				//if (listener != null) listener.onTestStatusChanged(testType, TestStatus.FAILED, "Test implementation not found.");
				//processNextTest();
//			}, TEST_TIMEOUT_MS);
			return;
		}

		final AtomicBoolean isCompleted = new AtomicBoolean(false);

		final Runnable timeoutRunnable = () -> {
			if (isCompleted.compareAndSet(false, true)) {
				Log.d(TAG, "Test " + testType.name() + " timed out after "+TEST_TIMEOUT_MS +" ms.");
				TestResult timeoutResult = new TestResult(TestStatus.FAILED, "Test timed out (" + (timeoutMs/1000)+"s)");
				concludeAndProceed(testType, timeoutResult);
//				mainHandler.post(() -> {
//					if (listener != null) {
//						listener.onTestTimeout(testType);
//						listener.onTestStatusChanged(testType, timeoutResult.getStatus(), timeoutResult.getMessage());
//					}
//					mainHandler.postDelayed(this::processNextTest, 500);
////					processNextTest();
//				});
			}
		};

		Consumer<TestResult> callback = result -> {
			if (isCompleted.compareAndSet(false, true)) {
				mainHandler.removeCallbacks(timeoutRunnable);
				concludeAndProceed(testType, result);
//				mainHandler.post(() -> {
//					if (listener != null)
//						listener.onTestStatusChanged(testType, result.getStatus(), result.getMessage());
//
//					if (result.getStatus() != TestStatus.WAITING_FOR_USER) {
//						mainHandler.postDelayed(this::processNextTest, 1000);
////						processNextTest();
//					} else {
//						Log.d(TAG, "Test " + testType + " is paused, waiting for user action.");
//					}
//				});
			}
		};

		new Thread(() -> {
			Log.d(TAG, "Running auto test for: " + testType.name() + " with a " + (TEST_TIMEOUT_MS/1000) + "s timeout.");
			try {
				mainHandler.postDelayed(timeoutRunnable, TEST_TIMEOUT_MS);
				testModel.runAutoTest(params, callback);
			} catch (Exception e) {
				Log.d(TAG, "Unhandled exception in " + testType.name() + " auto-test.", e);
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
