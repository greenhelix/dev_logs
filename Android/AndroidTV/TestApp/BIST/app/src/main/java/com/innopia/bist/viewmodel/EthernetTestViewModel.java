package com.innopia.bist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;

import com.innopia.bist.test.EthernetTest;
import com.innopia.bist.util.TestType;

public class EthernetTestViewModel extends BaseTestViewModel{

    private static final String TAG = "EthernetTestViewModel";

    public EthernetTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new EthernetTest(), mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected TestType getTestType() {
        return TestType.ETHERNET;
    }
}
