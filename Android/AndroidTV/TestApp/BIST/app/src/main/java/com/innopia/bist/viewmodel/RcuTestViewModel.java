package com.innopia.bist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;

import com.innopia.bist.test.RcuTest;
import com.innopia.bist.util.TestType;

public class RcuTestViewModel extends BaseTestViewModel{

    private static final String TAG = "RcuTestViewModel";

    public RcuTestViewModel(@NonNull Application application, RcuTest rcuTest, MainViewModel mainViewModel) {
        super(application, rcuTest, mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected TestType getTestType() {
        return TestType.RCU;
    }
}
