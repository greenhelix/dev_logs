package com.innopia.bist.ver2.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.ver2.data.repository.Test3Repository;

import java.util.List;

/**
 * Test3 ViewModel - 네트워크 속도 측정
 */
public class Test3ViewModel extends AndroidViewModel {

    private static final String TAG = "Test3ViewModel";
    private final Test3Repository repository;

    // LiveData
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<List<Float>> chartData = new MutableLiveData<>();
    private final MutableLiveData<Test3Repository.DownloadTestResult> downloadResult = new MutableLiveData<>();
    private final MutableLiveData<Test3Repository.UploadTestResult> uploadResult = new MutableLiveData<>();
    private final MutableLiveData<Test3Repository.PingTestResult> pingResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public Test3ViewModel(@NonNull Application application) {
        super(application);
        repository = new Test3Repository(application.getApplicationContext());
        statusMessage.setValue("Ready to start network test");
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

    public LiveData<Test3Repository.DownloadTestResult> getDownloadResult() {
        return downloadResult;
    }

    public LiveData<Test3Repository.UploadTestResult> getUploadResult() {
        return uploadResult;
    }

    public LiveData<Test3Repository.PingTestResult> getPingResult() {
        return pingResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * 다운로드 속도 테스트
     */
    public void startDownloadTest() {
        isLoading.postValue(true);
        progress.postValue(0);
        statusMessage.postValue("Starting download test...");
        chartData.postValue(null);

        repository.testDownloadSpeed(new Test3Repository.Test3Callback() {
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
            public void onDownloadTestCompleted(Test3Repository.DownloadTestResult result) {
                Log.d(TAG, "Download test completed: " + result.downloadSpeed + " Mbps");
                downloadResult.postValue(result);
                chartData.postValue(result.speedData);
                progress.postValue(100);
                statusMessage.postValue("Download test completed");
                isLoading.postValue(false);
            }

            @Override
            public void onUploadTestCompleted(Test3Repository.UploadTestResult result) {
                // Not used
            }

            @Override
            public void onPingTestCompleted(Test3Repository.PingTestResult result) {
                // Not used
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Download test error: " + error);
                errorMessage.postValue(error);
                statusMessage.postValue("Test failed");
                isLoading.postValue(false);
            }
        });
    }

    /**
     * 업로드 속도 테스트
     */
    public void startUploadTest() {
        isLoading.postValue(true);
        progress.postValue(0);
        statusMessage.postValue("Starting upload test...");
        chartData.postValue(null);

        repository.testUploadSpeed(new Test3Repository.Test3Callback() {
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
            public void onDownloadTestCompleted(Test3Repository.DownloadTestResult result) {
                // Not used
            }

            @Override
            public void onUploadTestCompleted(Test3Repository.UploadTestResult result) {
                Log.d(TAG, "Upload test completed: " + result.uploadSpeed + " Mbps");
                uploadResult.postValue(result);
                chartData.postValue(result.speedData);
                progress.postValue(100);
                statusMessage.postValue("Upload test completed");
                isLoading.postValue(false);
            }

            @Override
            public void onPingTestCompleted(Test3Repository.PingTestResult result) {
                // Not used
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Upload test error: " + error);
                errorMessage.postValue(error);
                statusMessage.postValue("Test failed");
                isLoading.postValue(false);
            }
        });
    }

    /**
     * Ping 테스트
     */
    public void startPingTest() {
        isLoading.postValue(true);
        progress.postValue(0);
        statusMessage.postValue("Starting ping test...");
        chartData.postValue(null);

        repository.testPing(new Test3Repository.Test3Callback() {
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
            public void onDownloadTestCompleted(Test3Repository.DownloadTestResult result) {
                // Not used
            }

            @Override
            public void onUploadTestCompleted(Test3Repository.UploadTestResult result) {
                // Not used
            }

            @Override
            public void onPingTestCompleted(Test3Repository.PingTestResult result) {
                Log.d(TAG, "Ping test completed: " + result.averagePing + " ms");
                pingResult.postValue(result);
                chartData.postValue(result.pingData);
                progress.postValue(100);
                statusMessage.postValue("Ping test completed");
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Ping test error: " + error);
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
