package com.innopia.bist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;

import com.innopia.bist.test.UsbTest;
import com.innopia.bist.util.TestType;

public class UsbTestViewModel extends BaseTestViewModel {
    private static final String TAG = "UsbTestViewModel";

    public UsbTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new UsbTest(), mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected TestType getTestType() {
        return TestType.USB;
    }
}
