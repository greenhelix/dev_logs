package com.innopia.bist.fragment;

import android.app.Application;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.R;
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.VideoTestViewModel;

import java.util.List;

public class VideoTestFragment extends Fragment {
    private static final String TAG = "VideoTestButtonFragment";
    private VideoTestViewModel videoTestViewModel;
    private MainViewModel mainViewModel;
    private VideoView videoView;
    private TextView tvVideoInfo;
    private LinearLayout layoutButtons;

    public static VideoTestFragment newInstance() {
        return new VideoTestFragment();
    }

    public static class VideoTestViewModelFactory implements androidx.lifecycle.ViewModelProvider.Factory {
        private final Application application;
        private final MainViewModel mainViewModel;

        public VideoTestViewModelFactory(Application application, MainViewModel mainViewModel) {
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_video_test, container, false);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        VideoTestViewModelFactory factory = new VideoTestViewModelFactory(
                requireActivity().getApplication(),
                mainViewModel
        );
        videoTestViewModel = new ViewModelProvider(this, factory).get(VideoTestViewModel.class);

        tvVideoInfo = root.findViewById(R.id.tv_video_info_button);
        layoutButtons = root.findViewById(R.id.layout_video_buttons);
        videoView = root.findViewById(R.id.video_view);

        List<VideoTestViewModel.VideoSample> samples = videoTestViewModel.getVideoSamples();
        for (VideoTestViewModel.VideoSample sample : samples) {
            Button btnSample = new Button(getContext());
            btnSample.setText(sample.getDisplayName());
            btnSample.setBackgroundResource(R.drawable.button_focus_selector);
            btnSample.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            btnSample.setOnClickListener(v -> playSample(sample));
            layoutButtons.addView(btnSample);
        }

        videoTestViewModel.videoInfo.observe(getViewLifecycleOwner(), info -> tvVideoInfo.setText(info));

        mainViewModel.isAutoTestRunning.observe(getViewLifecycleOwner(), isRunning -> {
            if (isRunning) {
                layoutButtons.setVisibility(View.INVISIBLE);
                tvVideoInfo.setText("Auto-test in progress...");
            } else {
                layoutButtons.setVisibility(View.VISIBLE);
            }
        });
        return root;
    }

    private void playSample(VideoTestViewModel.VideoSample sample) {
        try {
            int resId = getResources().getIdentifier(sample.getFileName(), "raw", requireContext().getPackageName());
            Uri videoUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + resId);
            videoView.setVideoURI(videoUri);
            tvVideoInfo.setText(sample.getMetaInfo());

            videoView.setOnPreparedListener(MediaPlayer::start);
            videoView.setOnErrorListener((mp, what, extra) -> {
                String errorMsg = "Video play error: " + what + "," + extra;
                tvVideoInfo.setText(errorMsg);
                mainViewModel.appendLog(TAG, errorMsg);
                return true;
            });
            videoView.setOnCompletionListener(mp -> {
                mainViewModel.appendLog(TAG, sample.getDisplayName() + " completed");
            });

            videoView.start();
        } catch (Exception e) {
            String errorMsg = "Can't play sample: " + e.getMessage();
            tvVideoInfo.setText(errorMsg);
            mainViewModel.appendLog(TAG, errorMsg);
            Log.e(TAG, errorMsg, e);
        }
    }
}
