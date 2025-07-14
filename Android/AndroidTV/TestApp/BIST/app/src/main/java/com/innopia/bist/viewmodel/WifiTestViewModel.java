package com.innopia.bist.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import com.innopia.bist.model.tests.wifi.WifiTest;

public class WifiTestViewModel extends BaseTestViewModel {
    private static final String TAG = "WifiTestViewModel";

    public WifiTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new WifiTest(), mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
