package com.innopia.bist.ver2.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.ver2.data.repository.Test2Repository;

import java.util.List;

/**
 * Test2 ViewModel - 메모리 및 CPU 성능 측정
 */
public class Test2ViewModel extends AndroidViewModel {

    private static final String TAG = "Test2ViewModel";
    private final Test2Repository repository;

    // LiveData
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<List<Float>> chartData = new MutableLiveData<>();
    private final MutableLiveData<Test2Repository.MemoryTestResult> memoryResult = new MutableLiveData<>();
    private final MutableLiveData<Test2Repository.CpuTestResult> cpuResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public Test2ViewModel(@NonNull Application application) {
        super(application);
        repository = new Test2Repository(application.getApplicationContext());
        statusMessage.setValue("Ready to start performance test");
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

    public LiveData<Test2Repository.MemoryTestResult> getMemoryResult() {
        return memoryResult;
    }

    public LiveData<Test2Repository.CpuTestResult> getCpuResult() {
        return cpuResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * 메모리 테스트 시작
     */
    public void startMemoryTest() {
        isLoading.postValue(true);
        progress.postValue(0);
        statusMessage.postValue("Starting memory test...");
        chartData.postValue(null);

        repository.testMemoryPerformance(new Test2Repository.Test2Callback() {
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
            public void onMemoryTestCompleted(Test2Repository.MemoryTestResult result) {
                Log.d(TAG, "Memory test completed: " + result.memorySpeed + " MB/s");
                memoryResult.postValue(result);
                chartData.postValue(result.speedData);
                progress.postValue(100);
                statusMessage.postValue("Memory test completed");
                isLoading.postValue(false);
            }

            @Override
            public void onCpuTestCompleted(Test2Repository.CpuTestResult result) {
                // Not used in memory test
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Memory test error: " + error);
                errorMessage.postValue(error);
                statusMessage.postValue("Test failed");
                isLoading.postValue(false);
            }
        });
    }

    /**
     * CPU 테스트 시작
     */
    public void startCpuTest() {
        isLoading.postValue(true);
        progress.postValue(0);
        statusMessage.postValue("Starting CPU test...");
        chartData.postValue(null);

        repository.testCpuPerformance(new Test2Repository.Test2Callback() {
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
            public void onMemoryTestCompleted(Test2Repository.MemoryTestResult result) {
                // Not used in CPU test
            }

            @Override
            public void onCpuTestCompleted(Test2Repository.CpuTestResult result) {
                Log.d(TAG, "CPU test completed: " + result.operationsPerSecond + " ops/s");
                cpuResult.postValue(result);
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
