package com.innopia.bist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.test.VideoTest;
import com.innopia.bist.util.TestType;

import java.util.Arrays;
import java.util.List;

public class VideoTestViewModel extends BaseTestViewModel {
    public final MutableLiveData<String> videoInfo = new MutableLiveData<>("wait test start");
    private static final String TAG = "BIST_VIDEO_VM";

    public VideoTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new VideoTest(), mainViewModel);
    }

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

    public List<VideoSample> getVideoSamples() {
        return Arrays.asList(
                new VideoSample("AV1/1080p", "bunny_1080_10s_5mb_av1", "H264 1080p 10s"),
                new VideoSample("H264/1080p", "bunny_1080_10s_5mb_h264", "H265 1080p 10s"),
                new VideoSample("VP9/1280p", "sample_video_1280x720_1mb", "VP9 1280p 12s"),
                new VideoSample("AVC/4k", "driving_mountain_4k", "AVC 4k")
        );
    }

    @Override
    protected String getTag() { return TAG; }

    @Override
    protected TestType getTestType() { return TestType.VIDEO; }

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
