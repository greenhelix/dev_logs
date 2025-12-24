package com.innopia.bist.ver2.viewmodel;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.innopia.bist.ver2.data.repository.MicTestRepository;
import java.util.List;

public class MicTestViewModel extends AndroidViewModel {
    private static final String TAG = "MicTestViewModel";
    private final MicTestRepository repository;

    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<String> statusMessage = new MutableLiveData();
    // Changed to List<List<Float>> for multi-mic support
    private final MutableLiveData<List<List<Float>>> chartData = new MutableLiveData<>();
    private final MutableLiveData<MicTestRepository.MicTestResult> micResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MicTestViewModel(@NonNull Application application) {
        super(application);
        repository = new MicTestRepository(application.getApplicationContext());
        statusMessage.setValue("Ready to start Mic test");
    }

    public LiveData<Integer> getProgress() { return progress; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    // Getter updated
    public LiveData<List<List<Float>>> getChartData() { return chartData; }
    public LiveData<MicTestRepository.MicTestResult> getMicResult() { return micResult; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void startMicTest() {
        isLoading.postValue(true);
        progress.postValue(0);
        statusMessage.postValue("Starting Mic test...");
        chartData.postValue(null);

        repository.testMicPerformance(new MicTestRepository.Test3Callback() {
            @Override
            public void onTestProgress(int prog, String message) {
                progress.postValue(prog);
                statusMessage.postValue(message);
            }

            @Override
            public void onChartDataUpdate(List<List<Float>> data) {
                chartData.postValue(data);
            }

            @Override
            public void onMicTestCompleted(MicTestRepository.MicTestResult result) {
                Log.d(TAG, "Mic test completed: " + result.activeMicCount + " mics");
                micResult.postValue(result);
                progress.postValue(100);
                statusMessage.postValue("Mic test completed");
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Mic test error: " + error);
                errorMessage.postValue(error);
                statusMessage.postValue("Test failed");
                isLoading.postValue(false);
            }
        });
    }

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

    public void startFakeMicTest() {
        isLoading.postValue(true);
        progress.postValue(0);
        statusMessage.postValue("Generating synthetic data...");
        chartData.postValue(null);

        repository.startFakeTest(new MicTestRepository.Test3Callback() {
            @Override
            public void onTestProgress(int prog, String message) {
                progress.postValue(prog);
                statusMessage.postValue(message);
            }

            @Override
            public void onChartDataUpdate(List<List<Float>> data) {
                chartData.postValue(data);
            }

            @Override
            public void onMicTestCompleted(MicTestRepository.MicTestResult result) {
                micResult.postValue(result);
                progress.postValue(100);
                statusMessage.postValue("Simulation Finished");
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
                isLoading.postValue(false);
            }
        });
    }
}
