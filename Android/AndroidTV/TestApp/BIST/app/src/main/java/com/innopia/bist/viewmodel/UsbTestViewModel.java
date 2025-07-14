package com.innopia.bist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;

import com.innopia.bist.model.tests.usb.UsbTest;

public class UsbTestViewModel extends BaseTestViewModel {
    private static final String TAG = "UsbTestViewModel";

    public UsbTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new UsbTest(), mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
