package com.innopia.bist.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.test.WifiTest;
import com.innopia.bist.util.TestConfig;
import com.innopia.bist.util.TestType;

public class WifiTestViewModel extends BaseTestViewModel {
	private static final String TAG = "BIST_WIFI_VM";
	private final WifiTest wifiTest;

	public WifiTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
		super(application, new WifiTest(), mainViewModel);
		this.wifiTest = new WifiTest();
	}

	@Override
	protected String getTag() { return TAG; }

	@Override
	protected TestType getTestType() { return TestType.WIFI; }
}
