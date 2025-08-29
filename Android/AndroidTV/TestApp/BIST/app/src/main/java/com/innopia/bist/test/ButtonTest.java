package com.innopia.bist.test;

import android.content.Context;
import android.view.KeyEvent;

//import com.innopia.bist.util.FileUtils;
import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ButtonTest implements Test {

	private static final String TAG = "ButtonTest";

	//private static final String SYSFS_MIC_STATUS = "/sys/class/leds/mic_led/mic_gpio";
	//private static final String MIC_MUTE = "0";
	//private static final String MIC_UNMUTE = "1";

	//private final Uri HOTWORD_MIC_URI = Uri.parse("content://atv.hotwordmic/togglestate");

	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private List<Integer> keySequence;
	private int currentIndex;
	private Consumer<TestResult> callback;

	@Override
	public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
		buttonTest(params, callback);
	}

	@Override
	public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
		buttonTest(params, callback);
	}

	private void buttonTest(Map<String, Object> params, Consumer<TestResult> callback) {
		this.callback = callback;
		//boolean isMuted = MIC_MUTE.equals(FileUtils.readFromFile(SYSFS_MIC_STATUS));
		//keySequence = generateRandomKeys(isMuted);
		keySequence = generateRandomKeys();
		currentIndex = 0;
		showNextKey();
	}

	//private List<Integer> generateRandomKeys(boolean isMuted) {
	private List<Integer> generateRandomKeys() {
		List<Integer> result = new ArrayList<>(Arrays.asList(
				KeyEvent.KEYCODE_BUTTON_10, // pin
				KeyEvent.KEYCODE_BUTTON_13, // mute
				KeyEvent.KEYCODE_BUTTON_14 // unmute
		));
		Collections.shuffle(result);
		//int keyToSwap;
		//int targetKey;
		//if (isMuted) {
		//	keyToSwap = KeyEvent.KEYCODE_BUTTON_14;
		//	targetKey = KeyEvent.KEYCODE_BUTTON_13;
		//} else {
		//	keyToSwap = KeyEvent.KEYCODE_BUTTON_13;
		//	targetKey = KeyEvent.KEYCODE_BUTTON_14;
		//}
		//int indexOfKeyToSwap = result.indexOf(keyToSwap);
		//int indexOfTargetKey = result.indexOf(targetKey);
		//if (indexOfKeyToSwap != 0 && indexOfTargetKey != 0) {
		//	if (indexOfKeyToSwap > indexOfTargetKey) {
		//		Collections.swap(result, indexOfKeyToSwap, indexOfTargetKey);
		//	}
		//}
		return result;
	}

	private void showNextKey() {
		if (callback == null) return;

		if (currentIndex < keySequence.size()) {
			String keyName = getKeyName(keySequence.get(currentIndex));
			callback.accept(new TestResult(TestStatus.RUNNING, "Press: " + keyName + " on STB (" + (currentIndex + 1) + "/3)"));
		} else {
			callback.accept(new TestResult(TestStatus.PASSED, "Test Passed! All keys matched."));
		}
	}

	private String getKeyName(int keyCode) {
		String name = KeyEvent.keyCodeToString(keyCode);
		if (keyCode == KeyEvent.KEYCODE_BUTTON_10) {
			name = "RESET_BUTTON";
		} else if (keyCode == KeyEvent.KEYCODE_BUTTON_13) {
			name = "MIC_MUTE_BUTTON";
		} else if (keyCode == KeyEvent.KEYCODE_BUTTON_14) {
			name = "MIC_UNMUTE_BUTTON";
		}
		return name;
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
			String expectedKeyName = getKeyName(expectedKey);
			String receivedKeyName = getKeyName(receivedKey);
			callback.accept(new TestResult(TestStatus.RUNNING, "Wrong key! Pressed " + receivedKeyName + ". Please press: " + expectedKeyName + " on STB."));
		}
	}
}
