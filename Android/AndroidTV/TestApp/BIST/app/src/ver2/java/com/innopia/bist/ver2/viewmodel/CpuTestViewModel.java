package com.innopia.bist.ver2.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.ver2.data.repository.CpuTestRepository;

import java.util.List;

/**
 * CPU Test ViewModel
 */
public class CpuTestViewModel extends AndroidViewModel {

    private static final String TAG = "CpuTestViewModel";
    private final CpuTestRepository repository;

    // LiveData
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<List<Float>> chartData = new MutableLiveData<>();
    private final MutableLiveData<CpuTestRepository.CpuTestResult> testResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public CpuTestViewModel(@NonNull Application application) {
        super(application);
        repository = new CpuTestRepository(application.getApplicationContext());
        statusMessage.setValue("Ready to start CPU test");
    }

    // Getters
    public LiveData<Integer> getProgress() {
        return progress;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<List<Float>> getChartData() {
        return chartData;
    }

    public LiveData<CpuTestRepository.CpuTestResult> getTestResult() {
        return testResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * CPU 테스트 시작
     */
    public void startCpuTest() {
        isLoading.postValue(true);
        progress.postValue(0);
        statusMessage.postValue("Starting CPU test...");
        chartData.postValue(null);

        repository.testCpuPerformance(new CpuTestRepository.CpuTestCallback() {
            @Override
            public void onTestProgress(int prog, String message) {
                progress.postValue(prog);
                statusMessage.postValue(message);
            }

            @Override
            public void onChartDataUpdate(List<Float> data) {
                chartData.postValue(data);
            }

            @Override
            public void onCpuTestCompleted(CpuTestRepository.CpuTestResult result) {
                Log.d(TAG, "CPU test completed: " + result.operationsPerSecond + " ops/s");
                testResult.postValue(result);
                chartData.postValue(result.performanceData);
                progress.postValue(100);
                statusMessage.postValue("CPU test completed");
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "CPU test error: " + error);
                errorMessage.postValue(error);
                statusMessage.postValue("Test failed");
                isLoading.postValue(false);
            }
        });
    }

    /**
     * 테스트 중지
     */
    public void stopTest() {
        repository.stopTest();
        isLoading.postValue(false);
        statusMessage.postValue("Test stopped");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.stopTest();
    }
}
