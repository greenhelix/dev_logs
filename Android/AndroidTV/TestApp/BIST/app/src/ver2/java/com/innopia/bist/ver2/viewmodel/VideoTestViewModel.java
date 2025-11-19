package com.innopia.bist.ver2.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.ver2.data.repository.VideoTestRepository;

/**
 * Video Test ViewModel
 */
public class VideoTestViewModel extends AndroidViewModel {

    private static final String TAG = "VideoTestViewModel";
    private final VideoTestRepository repository;

    // LiveData
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<VideoTestRepository.VideoTestResult> testResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // 비디오 URL
    private String currentVideoUrl = "";

    public VideoTestViewModel(@NonNull Application application) {
        super(application);
        repository = new VideoTestRepository(application.getApplicationContext());
        repository.setUseSampleData(true); // 기본값: 샘플 데이터
        statusMessage.setValue("Ready to test video");
    }

    // Getters
    public LiveData<Integer> getProgress() {
        return progress;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<VideoTestRepository.VideoTestResult> getTestResult() {
        return testResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * 비디오 테스트 시작
     */
    public void startVideoTest(String videoUrl) {
        this.currentVideoUrl = videoUrl;
        isLoading.postValue(true);
        progress.postValue(0);
        statusMessage.postValue("Starting video test...");

        repository.startVideoTest(videoUrl, new VideoTestRepository.VideoTestCallback() {
            @Override
            public void onTestProgress(int prog, String message) {
                progress.postValue(prog);
                statusMessage.postValue(message);
            }

            @Override
            public void onVideoTestCompleted(VideoTestRepository.VideoTestResult result) {
                Log.d(TAG, "Video test completed: " + result.quality);
                testResult.postValue(result);
                progress.postValue(100);
                statusMessage.postValue("Video test completed");
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Video test error: " + error);
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
