package com.innopia.bist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;

import com.innopia.bist.test.HdmiTest;

public class HdmiTestViewModel extends BaseTestViewModel {
    private static final String TAG = "HdmiTestViewModel";

    public HdmiTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new HdmiTest(), mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
