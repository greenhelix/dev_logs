package com.innopia.bist.ver1.test;

import android.util.Log;

import com.innopia.bist.ver1.util.TestResult;
import com.innopia.bist.ver1.util.TestStatus;
import com.innopia.bist.ver1.viewmodel.MainViewModel;

import java.util.Map;
import java.util.function.Consumer;

public class VideoTest implements Test {

	private static final String TAG = "BIST_VideoTest";
	private static final long AUTO_TEST_TIMEOUT_SECONDS = 15;

	@Override
	public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
		callback.accept(new TestResult(TestStatus.RUNNING, "Manual test started. Please use UI."));
	}

	@Override
	public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
		MainViewModel mainViewModel = (MainViewModel) params.get("mainViewModel");

		if (mainViewModel == null) {
			Log.e(TAG, "Cannot run auto test: MainViewModel is null.");
			callback.accept(new TestResult(TestStatus.ERROR, "MainViewModel was not provided."));
			return;
		}

		mainViewModel.startVideoAutoTest(callback);
	}
}
