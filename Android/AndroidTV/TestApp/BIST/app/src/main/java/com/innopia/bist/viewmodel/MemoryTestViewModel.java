package com.innopia.bist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;

import com.innopia.bist.model.tests.memory.MemoryTest;

public class MemoryTestViewModel extends BaseTestViewModel {
    private static final String TAG = "MemoryTestViewModel";

    public MemoryTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new MemoryTest(), mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
