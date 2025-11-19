package com.innopia.bist.ver2.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.ver2.data.repository.WifiTestRepository;

/**
 * WiFi Test ViewModel
 */
public class WifiTestViewModel extends AndroidViewModel {

    private static final String TAG = "WifiTestViewModel";
    private final WifiTestRepository repository;

    // LiveData
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<WifiTestRepository.WifiTestResult> testResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public WifiTestViewModel(@NonNull Application application) {
        super(application);
        repository = new WifiTestRepository(application.getApplicationContext());
        repository.setUseSampleData(true); // 기본값: 샘플 데이터
        statusMessage.setValue("Ready to test WiFi");
    }

    // Getters
    public LiveData<Integer> getProgress() {
        return progress;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<WifiTestRepository.WifiTestResult> getTestResult() {
        return testResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * WiFi 테스트 시작
     */
    public void startWifiTest() {
        isLoading.postValue(true);
        progress.postValue(0);
        statusMessage.postValue("Testing WiFi...");

        repository.testWifi(new WifiTestRepository.WifiTestCallback() {
            @Override
            public void onTestProgress(int prog, String message) {
                progress.postValue(prog);
                statusMessage.postValue(message);
            }

            @Override
            public void onWifiTestCompleted(WifiTestRepository.WifiTestResult result) {
                Log.d(TAG, "WiFi test completed");
                testResult.postValue(result);
                progress.postValue(100);
                statusMessage.postValue("WiFi test completed");
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "WiFi test error: " + error);
                errorMessage.postValue(error);
                statusMessage.postValue("Test failed");
                isLoading.postValue(false);
            }
        });
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
