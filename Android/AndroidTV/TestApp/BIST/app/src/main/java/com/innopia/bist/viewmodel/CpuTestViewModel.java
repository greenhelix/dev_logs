package com.innopia.bist.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import com.innopia.bist.test.CpuTest;

public class CpuTestViewModel extends BaseTestViewModel {
    private static final String TAG = "CpuTestViewModel";

    public CpuTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new CpuTest(), mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
