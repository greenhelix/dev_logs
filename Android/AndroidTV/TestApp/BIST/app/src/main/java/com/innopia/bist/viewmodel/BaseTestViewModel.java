package com.innopia.bist.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.innopia.bist.model.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class BaseTestViewModel extends AndroidViewModel {

    protected final Test testModel;
    protected final MainViewModel mainViewModel;

    private final MutableLiveData<String> _testResultLiveData = new MutableLiveData<>();
    public final LiveData<String> testResultLiveData = _testResultLiveData;

    public BaseTestViewModel(@NonNull Application application, Test testModel, MainViewModel mainViewModel) {
        super(application);
        this.testModel = testModel;
        this.mainViewModel = mainViewModel;
    }

    public void startManualTest() {
        mainViewModel.appendLog(getTag(), "Manual test started.");

        // Consumer를 사용하여 Model의 비동기 결과를 처리
        Consumer<String> callback = result -> {
            // Model에서 받은 결과는 항상 메인 스레드에서 LiveData에 설정해야 함
            _testResultLiveData.postValue(result);
            mainViewModel.appendLog(getTag(), "Manual test finished.");
        };

        Map<String, Object> params = new HashMap<>();
        params.put("context", getApplication().getApplicationContext());

        testModel.runManualTest(params, callback);
    }

    // 자동 테스트 시작
    public void startAutoTest() {
        mainViewModel.appendLog(getTag(), "Auto test started.");

        Consumer<String> callback = result -> {
            // 자동 테스트 결과 처리 로직 (예: 상태 업데이트)
            _testResultLiveData.postValue("AUTO TEST: " + result);
            mainViewModel.appendLog(getTag(), "Auto test finished.");
        };

        Map<String, Object> params = new HashMap<>();
        params.put("context", getApplication().getApplicationContext());

        testModel.runAutoTest(params, callback);
    }

    protected abstract String getTag();
}
