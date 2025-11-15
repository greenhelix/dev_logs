package com.innopia.bist.ver1.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;

import com.innopia.bist.ver1.test.CpuTest;
import com.innopia.bist.ver1.util.TestType;

public class CpuTestViewModel extends BaseTestViewModel {
	private static final String TAG = "CpuTestViewModel";

	public CpuTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
		super(application, new CpuTest(), mainViewModel);
	}

	@Override
	protected String getTag() {
		return TAG;
	}

	@Override
	protected TestType getTestType() {
		return TestType.CPU;
	}
}
