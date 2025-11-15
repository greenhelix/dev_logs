package com.innopia.bist.ver1.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;

import com.innopia.bist.ver1.test.HdmiTest;
import com.innopia.bist.ver1.util.TestType;

/**
 * ViewModel for the HdmiTestFragment.
 * It extends BaseTestViewModel to inherit common test functionalities.
 * This ViewModel creates and manages an instance of HdmiTest.
 */
public class HdmiTestViewModel extends BaseTestViewModel {

    private static final String TAG = "HdmiTestViewModel";

    /**
     * Constructor for HdmiTestViewModel.
     * @param application The application instance.
     * @param mainViewModel The shared MainViewModel instance.
     */
    public HdmiTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new HdmiTest(), mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected TestType getTestType() {
        return TestType.HDMI;
    }
}
