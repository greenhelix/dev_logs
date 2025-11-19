package com.innopia.bist.ver2.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.ver2.data.repository.TemperatureTestRepository;

/**
 * Temperature Test ViewModel
 */
public class TemperatureTestViewModel extends AndroidViewModel {

    private static final String TAG = "TemperatureTestViewModel";
    private final TemperatureTestRepository repository;

    // LiveData
    private final MutableLiveData<TemperatureTestRepository.TemperatureTestResult> temperatureResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isMonitoring = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public TemperatureTestViewModel(@NonNull Application application) {
        super(application);
        repository = new TemperatureTestRepository(application.getApplicationContext());
        repository.setUseSampleData(true); // 기본값: 샘플 데이터
        statusMessage.setValue("Ready to monitor temperature");
    }

    // Getters
    public LiveData<TemperatureTestRepository.TemperatureTestResult> getTemperatureResult() {
        return temperatureResult;
    }

    public LiveData<Boolean> getIsMonitoring() {
        return isMonitoring;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * 온도 모니터링 시작
     */
    public void startMonitoring() {
        isMonitoring.postValue(true);
        statusMessage.postValue("Monitoring temperature...");

        repository.startTemperatureMonitoring(new TemperatureTestRepository.TemperatureTestCallback() {
            @Override
            public void onMonitoringStarted() {
                Log.d(TAG, "Temperature monitoring started");
            }

            @Override
            public void onTemperatureUpdated(TemperatureTestRepository.TemperatureTestResult result) {
                temperatureResult.postValue(result);
            }

            @Override
            public void onMonitoringStopped() {
                Log.d(TAG, "Temperature monitoring stopped");
                isMonitoring.postValue(false);
                statusMessage.postValue("Monitoring stopped");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Temperature monitoring error: " + error);
                errorMessage.postValue(error);
                isMonitoring.postValue(false);
                statusMessage.postValue("Monitoring failed");
            }
        });
    }

    /**
     * 온도 모니터링 중지
     */
    public void stopMonitoring() {
        repository.stopTest();
    }

    /**
     * 샘플 데이터 모드 설정
     */
    public void setUseSampleData(boolean useSampleData) {
        repository.setUseSampleData(useSampleData);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.stopTest();
    }
}
