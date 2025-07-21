package com.innopia.bist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.test.VideoTest;

import java.util.Arrays;
import java.util.List;

public class VideoTestViewModel extends BaseTestViewModel {
    public final MutableLiveData<String> videoInfo = new MutableLiveData<>("wait test start");

    public VideoTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new VideoTest(), mainViewModel);
    }

    // 샘플 영상 정보 정의
    public static class VideoSample {
        private final String displayName;
        private final String fileName;
        private final String metaInfo;
        public VideoSample(String displayName, String fileName, String metaInfo) {
            this.displayName = displayName;
            this.fileName = fileName;
            this.metaInfo = metaInfo;
        }
        public String getDisplayName() { return displayName; }
        public String getFileName() { return fileName; }
        public String getMetaInfo() { return metaInfo; }
    }

    // 코덱, 화질별 샘플 영상리스트. raw에 샘플 파일 사전 추가 필요
    public List<VideoSample> getVideoSamples() {
        return Arrays.asList(
                new VideoSample("H264/480p", "sample_h264_480p", "코덱: H264 / 해상도: 480p / 10s"),
                new VideoSample("H265/1080p", "sample_h265_1080p", "코덱: H265 / 해상도: 1080p / 15s"),
                new VideoSample("VP9/720p", "sample_vp9_720p", "코덱: VP9 / 해상도: 720p / 12s"),
                new VideoSample("AV1/360p", "sample_av1_360p", "코덱: AV1 / 해상도: 360p / 10s")
                // 파일명은 raw 폴더에 실제 파일명/확장자를 int 리소스명으로 추가 필요
        );
    }

    @Override
    protected String getTag() {
        return "VideoTestViewModel";
    }

    // 추후 signed 영상/키확인 등 확장시 메소드 추가
    // public void checkDRMVideo() { ... }
    public static class Factory implements androidx.lifecycle.ViewModelProvider.Factory {
        private final Application application;
        private final MainViewModel mainViewModel;

        public Factory(Application application, MainViewModel mainViewModel) {
            this.application = application;
            this.mainViewModel = mainViewModel;
        }

        @NonNull
        @Override
        public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(VideoTestViewModel.class)) {
                return (T) new VideoTestViewModel(application, mainViewModel);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
