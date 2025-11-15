package com.innopia.bist.ver1.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;

import com.innopia.bist.ver1.test.MemoryTest;
import com.innopia.bist.ver1.util.TestType;

public class MemoryTestViewModel extends BaseTestViewModel {
    private static final String TAG = "MemoryTestViewModel";

    public MemoryTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new MemoryTest(), mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected TestType getTestType() {
        return TestType.MEMORY;
    }
}
