package com.innopia.bist.viewmodel;

import android.app.Application;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.innopia.bist.test.RcuTest;
import com.innopia.bist.util.SingleLiveEvent;
import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.util.TestType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RcuTestViewModel extends BaseTestViewModel{

	private static final String TAG = "BIST_RcuTestViewModel";

	private final SingleLiveEvent<Void> _testCompletedEvent = new SingleLiveEvent<>();
	public final LiveData<Void> testCompletedEvent = _testCompletedEvent;

	public RcuTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
		super(application,new RcuTest(), mainViewModel);
	}

	@Override
	public void startTest() {
		mainViewModel.appendLog(getTag(), "RCU test started. Waiting for user input.");
		Map<String, Object> params = new HashMap<>();
		params.put("context", getApplication().getApplicationContext());

		// This callback will be invoked multiple times by the RcuTest model.
		Consumer<TestResult> intermediateCallback = result -> {
			_testResultLiveData.postValue(result);

			mainViewModel.appendLog(getTag(), "RCU status: " + result.getMessage());

			if (result.getStatus() == TestStatus.PASSED || result.getStatus() == TestStatus.FAILED) {
				mainViewModel.updateTestResult(getTestType(), result.getStatus());
				if (result.getStatus() == TestStatus.PASSED) {
					Log.d(TAG, "RCU Test Status PASS ====================================");
					_testCompletedEvent.postValue(null);
				}
			}
		};
		testModel.runManualTest(params, intermediateCallback);
	}

	/**
	 * Called from the RcuTestFragment when a key is pressed.
	 * @param event The key event to process.
	 */
	public void onKeyEvent(KeyEvent event) {
		// The testModel is an instance of RcuTest, so we can cast it.
		if (testModel instanceof RcuTest) {
			((RcuTest) testModel).onKeyEvent(event);
		}
	}

	@Override
	protected String getTag() {
		return TAG;
	}

	@Override
	protected TestType getTestType() {
		return TestType.RCU;
	}
}
