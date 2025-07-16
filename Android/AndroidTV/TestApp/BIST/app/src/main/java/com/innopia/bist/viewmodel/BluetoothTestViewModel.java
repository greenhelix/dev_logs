package com.innopia.bist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;

import com.innopia.bist.model.test.bluetooth.BluetoothTest;

public class BluetoothTestViewModel extends BaseTestViewModel {
    private static final String TAG = "BluetoothTestViewModel";

    public BluetoothTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new BluetoothTest(), mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
