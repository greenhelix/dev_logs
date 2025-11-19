package com.innopia.bist.ver2.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.ver2.data.repository.StorageTestRepository;

/**
 * Storage Test ViewModel
 */
public class StorageTestViewModel extends AndroidViewModel {

    private static final String TAG = "StorageTestViewModel";
    private final StorageTestRepository repository;

    // LiveData
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<StorageTestRepository.StorageTestResult> testResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public StorageTestViewModel(@NonNull Application application) {
        super(application);
        repository = new StorageTestRepository(application.getApplicationContext());
        repository.setUseSampleData(false); // 실제 데이터 사용
        statusMessage.setValue("Ready to check storage");
    }

    // Getters
    public LiveData<Integer> getProgress() {
        return progress;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<StorageTestRepository.StorageTestResult> getTestResult() {
        return testResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * 저장공간 테스트 시작
     */
    public void startStorageTest() {
        isLoading.postValue(true);
        progress.postValue(0);
        statusMessage.postValue("Checking storage...");

        repository.testStorage(new StorageTestRepository.StorageTestCallback() {
            @Override
            public void onTestProgress(int prog, String message) {
                progress.postValue(prog);
                statusMessage.postValue(message);
            }

            @Override
            public void onStorageTestCompleted(StorageTestRepository.StorageTestResult result) {
                Log.d(TAG, "Storage test completed: " + result.status);
                testResult.postValue(result);
                progress.postValue(100);
                statusMessage.postValue("Storage check completed");
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Storage test error: " + error);
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
