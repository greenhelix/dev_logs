package com.innopia.bist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;

import com.innopia.bist.model.tests.video.VideoTest;

public class VideoTestViewModel extends BaseTestViewModel {
    private static final String TAG = "VideoTestViewModel";

    public VideoTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new VideoTest(), mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
