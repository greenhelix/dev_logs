package com.innopia.bist.ver1.test;

import android.content.Context;
import android.view.KeyEvent;

import com.innopia.bist.ver1.util.TestResult;
import com.innopia.bist.ver1.util.TestStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class RcuTest implements Test {

	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private List<Integer> keySequence;
	private int currentIndex;
	private Consumer<TestResult> callback;

	@Override
	public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
//        executeTest(params, callback);
		rcuTest(params, callback);
	}

	@Override
	public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
//        executeTest(params, callback);
		rcuTest(params, callback);
	}

	private void executeTest(Map<String, Object> params, Consumer<TestResult> callback) {
		executor.execute(() -> {
			Context context = (Context) params.get("context");
			if (context == null) {
				callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null"));
				return;
			}
			callback.accept(new TestResult(TestStatus.PASSED, "RCU Test pass"));
		});
	}

	private void rcuTest(Map<String, Object> params, Consumer<TestResult> callback) {
		this.callback = callback;
		keySequence = generateRandomKeys();
		currentIndex = 0;
		showNextKey();
	}

	private List<Integer> generateRandomKeys() {
		List<Integer> allKeys = Arrays.asList(
				KeyEvent.KEYCODE_DPAD_UP,
				KeyEvent.KEYCODE_DPAD_DOWN,
				KeyEvent.KEYCODE_DPAD_LEFT,
				KeyEvent.KEYCODE_DPAD_RIGHT
		);
		List<Integer> result = new ArrayList<>(allKeys);
		Collections.shuffle(result);
		return result;
	}

	private void showNextKey() {
		if (callback == null) return;

		if (currentIndex < keySequence.size()) {
			String keyName = KeyEvent.keyCodeToString(keySequence.get(currentIndex));
			callback.accept(new TestResult(TestStatus.RUNNING,"Press: " + keyName + " (" + (currentIndex + 1) + "/4)"));
		} else {
			callback.accept(new TestResult(TestStatus.PASSED,"Test Passed! All keys matched."));
		}
	}

	public void onKeyEvent(KeyEvent event) {
		if (keySequence == null || event.getAction() != KeyEvent.ACTION_DOWN) {
			return;
		}

		if (currentIndex >= keySequence.size()) {
			return;
		}

		int expectedKey = keySequence.get(currentIndex);
		int receivedKey = event.getKeyCode();

		if (receivedKey == expectedKey) {
			currentIndex++;
			showNextKey();
		} else if (receivedKey == KeyEvent.KEYCODE_BACK) {
			callback.accept(new TestResult(TestStatus.FAILED, "Test Exited. BACK key pressed."));
		} else {
			String expectedKeyName = KeyEvent.keyCodeToString(expectedKey);
			String receivedKeyName = KeyEvent.keyCodeToString(receivedKey);
			callback.accept(new TestResult(TestStatus.RUNNING, "Wrong key! Pressed " + receivedKeyName + ". Please press: " + expectedKeyName));
		}
	}
}
