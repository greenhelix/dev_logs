package com.innopia.bist.fragment;

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

public class VideoTestButtonFragment extends Fragment {
    private static final String TAG = "VideoTestButtonFragment";
    private VideoTestViewModel videoTestViewModel;
    private MainViewModel mainViewModel;
    private VideoView videoView;
    private TextView tvVideoInfo;

    public static VideoTestButtonFragment newInstance() {
        return new VideoTestButtonFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_video_button, container, false);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        videoTestViewModel = new ViewModelProvider(this,
                new VideoTestViewModel.Factory(requireActivity().getApplication(), mainViewModel))
                .get(VideoTestViewModel.class);

        tvVideoInfo = root.findViewById(R.id.tv_video_info_button);
        LinearLayout layoutButtons = root.findViewById(R.id.layout_video_buttons);
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
