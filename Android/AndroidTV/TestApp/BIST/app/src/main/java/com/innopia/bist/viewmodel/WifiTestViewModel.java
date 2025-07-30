package com.innopia.bist.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import com.innopia.bist.test.WifiTest;
import com.innopia.bist.util.TestType;

public class WifiTestViewModel extends BaseTestViewModel {
	private static final String TAG = "BIST_WIFI_VM";

	public WifiTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
		super(application, new WifiTest(), mainViewModel);
	}

	@Override
	protected String getTag() { return TAG; }

	@Override
	protected TestType getTestType() { return TestType.WIFI; }
}
