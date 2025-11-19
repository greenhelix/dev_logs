package com.innopia.bist.ver2.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.ver2.data.repository.RcuButtonTestRepository;

/**
 * RCU Button Test ViewModel
 */
public class RcuButtonTestViewModel extends AndroidViewModel {

    private static final String TAG = "RcuButtonTestViewModel";
    private final RcuButtonTestRepository repository;

    // LiveData
    private final MutableLiveData<RcuButtonTestRepository.ButtonEvent> lastButtonEvent = new MutableLiveData<>();
    private final MutableLiveData<RcuButtonTestRepository.RcuButtonTestResult> testResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isTestRunning = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public RcuButtonTestViewModel(@NonNull Application application) {
        super(application);
        repository = new RcuButtonTestRepository(application.getApplicationContext());
        statusMessage.setValue("Ready to test remote buttons");

        // 콜백 설정
        repository.setCallback(new RcuButtonTestRepository.RcuButtonTestCallback() {
            @Override
            public void onTestStarted(String message) {
                statusMessage.postValue(message);
            }

            @Override
            public void onButtonPressed(RcuButtonTestRepository.ButtonEvent event) {
                lastButtonEvent.postValue(event);
                statusMessage.postValue("Button pressed: " + event.buttonName);
            }

            @Override
            public void onTestResultUpdated(RcuButtonTestRepository.RcuButtonTestResult result) {
                testResult.postValue(result);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    // Getters
    public LiveData<RcuButtonTestRepository.ButtonEvent> getLastButtonEvent() {
        return lastButtonEvent;
    }

    public LiveData<RcuButtonTestRepository.RcuButtonTestResult> getTestResult() {
        return testResult;
    }

    public LiveData<Boolean> getIsTestRunning() {
        return isTestRunning;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * RCU 버튼 테스트 시작
     */
    public void startButtonTest() {
        isTestRunning.postValue(true);
        repository.startButtonTest(null); // 콜백은 이미 설정됨
    }

    /**
     * 버튼 입력 기록
     */
    public void recordButtonPress(int keyCode, long responseTime) {
        if (Boolean.TRUE.equals(isTestRunning.getValue())) {
            repository.recordButtonPress(keyCode, responseTime);
        }
    }

    /**
     * 테스트 중지
     */
    public void stopTest() {
        repository.stopTest();
        isTestRunning.postValue(false);
        statusMessage.postValue("Test stopped");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.stopTest();
    }
}
