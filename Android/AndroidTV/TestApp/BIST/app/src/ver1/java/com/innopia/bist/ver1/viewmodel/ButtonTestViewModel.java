package com.innopia.bist.ver1.viewmodel;

import android.app.Application;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.innopia.bist.ver1.test.ButtonTest;
import com.innopia.bist.ver1.util.SingleLiveEvent;
import com.innopia.bist.ver1.util.TestResult;
import com.innopia.bist.ver1.util.TestStatus;
import com.innopia.bist.ver1.util.TestType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ButtonTestViewModel extends BaseTestViewModel {

	private static final String TAG = "ButtonTestViewModel";

	private final SingleLiveEvent<Void> _testCompletedEvent = new SingleLiveEvent<>();
	public final LiveData<Void> testCompletedEvent = _testCompletedEvent;

	public ButtonTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
		super(application, new ButtonTest(), mainViewModel);
	}

	@Override
	public void startTest() {
		mainViewModel.appendLog(getTag(), "Button test started. Waiting for user input.");
		Map<String, Object> params = new HashMap<>();
		params.put("context", getApplication().getApplicationContext());

		// This callback will be invoked multiple times by the ButtonTest model.
		Consumer<TestResult> intermediateCallback = result -> {
			_testResultLiveData.postValue(result);

			mainViewModel.appendLog(getTag(), "Button status: " + result.getMessage());

			if (result.getStatus() == TestStatus.PASSED || result.getStatus() == TestStatus.FAILED) {
				mainViewModel.updateTestResult(getTestType(), result.getStatus());
				_testCompletedEvent.postValue(null);
			}
		};
		testModel.runManualTest(params, intermediateCallback);
	}

	/**
	 * Called from the ButtonTestFragment when a key is pressed.
	 * @param event The key event to process.
	 */
	public void onKeyEvent(KeyEvent event) {
		// The testModel is an instance of ButtonTest, so we can cast it.
		if (testModel instanceof ButtonTest) {
			((ButtonTest) testModel).onKeyEvent(event);
		}
	}

	@Override
	protected String getTag() {
		return TAG;
	}

	@Override
	protected TestType getTestType() {
		return TestType.BUTTON;
	}
}
