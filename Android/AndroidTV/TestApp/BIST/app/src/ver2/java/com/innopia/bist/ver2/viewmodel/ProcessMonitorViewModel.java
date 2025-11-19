package com.innopia.bist.ver2.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.ver2.data.repository.ProcessMonitorRepository;

/**
 * Process Monitor ViewModel
 */
public class ProcessMonitorViewModel extends AndroidViewModel {

    private static final String TAG = "ProcessMonitorViewModel";
    private final ProcessMonitorRepository repository;

    // LiveData
    private final MutableLiveData<ProcessMonitorRepository.ProcessMonitorResult> monitorResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isMonitoring = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ProcessMonitorViewModel(@NonNull Application application) {
        super(application);
        repository = new ProcessMonitorRepository(application.getApplicationContext());
        repository.setUseSampleData(true); // 기본값: 샘플 데이터
        statusMessage.setValue("Ready to monitor processes");
    }

    // Getters
    public LiveData<ProcessMonitorRepository.ProcessMonitorResult> getMonitorResult() {
        return monitorResult;
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
     * 프로세스 모니터링 시작
     */
    public void startMonitoring() {
        isMonitoring.postValue(true);
        statusMessage.postValue("Monitoring processes...");

        repository.startMonitoring(new ProcessMonitorRepository.ProcessMonitorCallback() {
            @Override
            public void onMonitoringStarted() {
                Log.d(TAG, "Process monitoring started");
            }

            @Override
            public void onProcessDataUpdated(ProcessMonitorRepository.ProcessMonitorResult result) {
                monitorResult.postValue(result);
            }

            @Override
            public void onMonitoringStopped() {
                Log.d(TAG, "Process monitoring stopped");
                isMonitoring.postValue(false);
                statusMessage.postValue("Monitoring stopped");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Process monitoring error: " + error);
                errorMessage.postValue(error);
                isMonitoring.postValue(false);
                statusMessage.postValue("Monitoring failed");
            }
        });
    }

    /**
     * 프로세스 모니터링 중지
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
