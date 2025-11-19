package com.innopia.bist.ver2.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.ver2.data.repository.BluetoothTestRepository;

/**
 * Bluetooth Test ViewModel
 */
public class BluetoothTestViewModel extends AndroidViewModel {

    private static final String TAG = "BluetoothTestViewModel";
    private final BluetoothTestRepository repository;

    // LiveData
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<BluetoothTestRepository.BluetoothTestResult> testResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public BluetoothTestViewModel(@NonNull Application application) {
        super(application);
        repository = new BluetoothTestRepository(application.getApplicationContext());
        repository.setUseSampleData(true); // 기본값: 샘플 데이터
        statusMessage.setValue("Ready to test Bluetooth");
    }

    // Getters
    public LiveData<Integer> getProgress() {
        return progress;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<BluetoothTestRepository.BluetoothTestResult> getTestResult() {
        return testResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * 블루투스 테스트 시작
     */
    public void startBluetoothTest() {
        isLoading.postValue(true);
        progress.postValue(0);
        statusMessage.postValue("Testing Bluetooth...");

        repository.testBluetooth(new BluetoothTestRepository.BluetoothTestCallback() {
            @Override
            public void onTestProgress(int prog, String message) {
                progress.postValue(prog);
                statusMessage.postValue(message);
            }

            @Override
            public void onBluetoothTestCompleted(BluetoothTestRepository.BluetoothTestResult result) {
                Log.d(TAG, "Bluetooth test completed");
                testResult.postValue(result);
                progress.postValue(100);
                statusMessage.postValue("Bluetooth test completed");
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Bluetooth test error: " + error);
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
