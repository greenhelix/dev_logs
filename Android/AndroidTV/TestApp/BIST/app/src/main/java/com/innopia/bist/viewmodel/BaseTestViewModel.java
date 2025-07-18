package com.innopia.bist.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.innopia.bist.test.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class BaseTestViewModel extends AndroidViewModel {

    protected final Test testModel;
    protected final MainViewModel mainViewModel;
    protected final MutableLiveData<String> _testResultLiveData = new MutableLiveData<>();
    public final LiveData<String> testResultLiveData = _testResultLiveData;

    public BaseTestViewModel(@NonNull Application application, Test testModel, MainViewModel mainViewModel) {
        super(application);
        this.testModel = testModel;
        this.mainViewModel = mainViewModel;
    }

    public void startManualTest() {
        mainViewModel.appendLog(getTag(), "Manual test started.");
        Consumer<String> callback = result -> {
            _testResultLiveData.postValue(result);
            mainViewModel.appendLog(getTag(), "Manual test finished.");
        };
        Map<String, Object> params = new HashMap<>();
        params.put("context", getApplication().getApplicationContext());
        testModel.runManualTest(params, callback);
    }

//    public void startAutoTest() {
//        mainViewModel.appendLog(getTag(), "Auto test started.");
//        Consumer<String> callback = result -> {
//            _testResultLiveData.postValue("AUTO TEST: " + result);
//            mainViewModel.appendLog(getTag(), "Auto test finished.");
//        };
//        Map<String, Object> params = new HashMap<>();
//        params.put("context", getApplication().getApplicationContext());
//        testModel.runAutoTest(params, callback);
//    }
    protected abstract String getTag();

    public Test getTestModel() {
        return testModel;
    }

    public MutableLiveData<String> getTestResult() {
        return _testResultLiveData;
    }
}
