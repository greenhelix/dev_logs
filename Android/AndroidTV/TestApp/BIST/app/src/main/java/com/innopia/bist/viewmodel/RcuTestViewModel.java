package com.innopia.bist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;

import com.innopia.bist.model.test.rcu.RcuTest;

public class RcuTestViewModel extends BaseTestViewModel {
    private static final String TAG = "RcuTestViewModel";

    public RcuTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new RcuTest(), mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
