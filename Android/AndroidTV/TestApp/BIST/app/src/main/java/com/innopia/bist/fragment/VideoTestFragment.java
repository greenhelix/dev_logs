package com.innopia.bist.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.R;
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.VideoTestViewModel;

public class VideoTestFragment extends Fragment {

    private VideoTestViewModel videoTestViewModel;
    private MainViewModel mainViewModel;

    public static VideoTestFragment newInstance() {
        return new VideoTestFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_video_test, container, false);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        videoTestViewModel = new ViewModelProvider(this,
                new VideoTestViewModel.Factory(requireActivity().getApplication(), mainViewModel))
                .get(VideoTestViewModel.class);

        TextView tvVideoInfo = root.findViewById(R.id.text_video_info);
        Button btnVideoTestStart = root.findViewById(R.id.btn_video_manual_test);

        videoTestViewModel.videoInfo.observe(getViewLifecycleOwner(),
                s -> tvVideoInfo.setText(s != null ? s : "wait test start"));

        btnVideoTestStart.setOnClickListener(v -> {
            FragmentTransaction tx = requireActivity().getSupportFragmentManager().beginTransaction();
            tx.replace(R.id.fragment_container, VideoTestButtonFragment.newInstance());
            tx.addToBackStack(null);
            tx.commit();
        });

        return root;
    }
}
