package com.innopia.bist.ver2.ui.fragment.test;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.innopia.bist.ver2.R;
import com.innopia.bist.ver2.data.repository.VideoTestRepository;
import com.innopia.bist.ver2.viewmodel.VideoTestViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Video Test Fragment
 * 비디오 재생 성능 테스트
 */
public class VideoTestFragment extends Fragment {

    private static final String TAG = "VideoTestFragment";
    private VideoTestViewModel viewModel;

    private TextView titleText;
    private TextView statusText;
    private ProgressBar progressBar;
    private TextView progressText;
    private LineChart performanceChart;
    private Button buttonStartTest;
    private Button buttonStopTest;

    // 비디오 정보
    private TextView videoCodecValue;
    private TextView resolutionValue;
    private TextView videoBitrateValue;
    private TextView audioCodecValue;

    // 성능 지표
    private TextView avgFrameDropsValue;
    private TextView bufferingEventsValue;
    private TextView avgBitrateValue;
    private TextView qualityValue;

    // 샘플 비디오 URL
    private static final String SAMPLE_VIDEO_URL =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";

    public static VideoTestFragment newInstance() {
        return new VideoTestFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_video_test, container, false);

        initViews(root);
        setupChart();

        new Handler(Looper.getMainLooper()).postDelayed(() -> requestInitialFocus(), 100);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(VideoTestViewModel.class);
        setupObservers();
        setupButtons();
    }

    private void initViews(View root) {
        titleText = root.findViewById(R.id.title_text);
        statusText = root.findViewById(R.id.status_text);
        progressBar = root.findViewById(R.id.progress_bar);
        progressText = root.findViewById(R.id.progress_text);
        performanceChart = root.findViewById(R.id.performance_chart);
        buttonStartTest = root.findViewById(R.id.button_start_test);
        buttonStopTest = root.findViewById(R.id.button_stop_test);

        videoCodecValue = root.findViewById(R.id.video_codec_value);
        resolutionValue = root.findViewById(R.id.resolution_value);
        videoBitrateValue = root.findViewById(R.id.video_bitrate_value);
        audioCodecValue = root.findViewById(R.id.audio_codec_value);

        avgFrameDropsValue = root.findViewById(R.id.avg_frame_drops_value);
        bufferingEventsValue = root.findViewById(R.id.buffering_events_value);
        avgBitrateValue = root.findViewById(R.id.avg_bitrate_value);
        qualityValue = root.findViewById(R.id.quality_value);
    }

    private void setupChart() {
        performanceChart.setTouchEnabled(true);
        performanceChart.setDragEnabled(true);
        performanceChart.setScaleEnabled(true);
        performanceChart.setPinchZoom(true);
        performanceChart.setDrawGridBackground(false);
        performanceChart.setBackgroundColor(Color.parseColor("#1a1a1a"));

        Description desc = new Description();
        desc.setText("Frame Drops / Buffering");
        desc.setTextColor(Color.WHITE);
        desc.setTextSize(12f);
        performanceChart.setDescription(desc);

        performanceChart.getLegend().setEnabled(true);
        performanceChart.getLegend().setTextColor(Color.WHITE);

        XAxis xAxis = performanceChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#333333"));

        YAxis leftAxis = performanceChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#333333"));
        leftAxis.setAxisMinimum(0f);

        performanceChart.getAxisRight().setEnabled(false);
        performanceChart.setData(new LineData());
        performanceChart.invalidate();
    }

    private void requestInitialFocus() {
        if (buttonStartTest != null) {
            buttonStartTest.requestFocus();
        }
    }

    private void setupObservers() {
        viewModel.getProgress().observe(getViewLifecycleOwner(), progress -> {
            if (progress != null) {
                progressBar.setProgress(progress);
                progressText.setText(progress + "%");
            }
        });

        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                statusText.setText(message);
            }
        });

        viewModel.getTestResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                updateResults(result);
                if (result.frameDropData != null && !result.frameDropData.isEmpty()) {
                    updateChart(result);
                }
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            buttonStartTest.setEnabled(!isLoading);
            buttonStopTest.setEnabled(isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
            progressText.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupButtons() {
        buttonStartTest.setOnClickListener(v -> {
            resetResults();
            performanceChart.clear();
            viewModel.startVideoTest(SAMPLE_VIDEO_URL);
        });

        buttonStopTest.setOnClickListener(v -> {
            viewModel.stopTest();
        });
    }

    private void updateChart(VideoTestRepository.VideoTestResult result) {
        List<Entry> frameDropEntries = new ArrayList<>();
        List<Entry> bufferingEntries = new ArrayList<>();

        for (int i = 0; i < result.frameDropData.size(); i++) {
            frameDropEntries.add(new Entry(i, result.frameDropData.get(i)));
        }

        for (int i = 0; i < result.bufferingData.size(); i++) {
            bufferingEntries.add(new Entry(i, result.bufferingData.get(i)));
        }

        LineDataSet frameDropDataSet = new LineDataSet(frameDropEntries, "Frame Drops");
        frameDropDataSet.setColor(Color.parseColor("#FF5252"));
        frameDropDataSet.setLineWidth(2f);
        frameDropDataSet.setDrawCircles(false);
        frameDropDataSet.setDrawValues(false);

        LineDataSet bufferingDataSet = new LineDataSet(bufferingEntries, "Buffering");
        bufferingDataSet.setColor(Color.parseColor("#FFC107"));
        bufferingDataSet.setLineWidth(2f);
        bufferingDataSet.setDrawCircles(false);
        bufferingDataSet.setDrawValues(false);

        LineData lineData = new LineData(frameDropDataSet, bufferingDataSet);
        performanceChart.setData(lineData);
        performanceChart.notifyDataSetChanged();
        performanceChart.invalidate();
    }

    private void updateResults(VideoTestRepository.VideoTestResult result) {
        videoCodecValue.setText(result.videoCodec);
        resolutionValue.setText(result.resolution);
        videoBitrateValue.setText(String.format("%d kbps", result.videoBitrate));
        audioCodecValue.setText(result.audioCodec);

        avgFrameDropsValue.setText(String.format("%.2f /s", result.averageFrameDrops));
        bufferingEventsValue.setText(String.valueOf(result.totalBufferingEvents));
        avgBitrateValue.setText(String.format("%.0f kbps", result.averageBitrate));
        qualityValue.setText(result.quality);
        qualityValue.setTextColor(Color.parseColor(result.qualityColor));
    }

    private void resetResults() {
        videoCodecValue.setText("--");
        resolutionValue.setText("--");
        videoBitrateValue.setText("--");
        audioCodecValue.setText("--");
        avgFrameDropsValue.setText("--");
        bufferingEventsValue.setText("--");
        avgBitrateValue.setText("--");
        qualityValue.setText("--");
        qualityValue.setTextColor(Color.WHITE);
    }
}
