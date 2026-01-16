package com.innopia.bist.ver2.ui.fragment.test;

import android.Manifest;
import android.content.pm.PackageManager;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.innopia.bist.ver2.R;
import com.innopia.bist.ver2.data.repository.MicTestRepository;
import com.innopia.bist.ver2.viewmodel.MicTestViewModel;

import java.util.ArrayList;
import java.util.List;

public class MicTestFragment extends Fragment {
    private static final String TAG = "MicTestFragment";
    private MicTestViewModel viewModel;

    // UI elements
    private LineChart volumeChart;
    private Button buttonMicTest;
    private Button buttonStopTest;
    private Button buttonFakeTest;
    // 4 Mics UI Components
    private ProgressBar[] micProgressBars;
    private TextView[] micDbTexts;
    private final int[] MIC_COLORS = {
            Color.parseColor("#FF5252"), // Red
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#448AFF"), // Blue
            Color.parseColor("#FFC107")  // Yellow
    };

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startMicTest();
                else Toast.makeText(getContext(), "Permission required", Toast.LENGTH_SHORT).show();
            });

    public static MicTestFragment newInstance() {
        return new MicTestFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mic_test, container, false);
        initViews(root);
        setupChart();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MicTestViewModel.class);
        setupObservers();

        buttonMicTest.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startMicTest();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        });

        buttonStopTest.setOnClickListener(v -> viewModel.stopTest());

        buttonFakeTest.setOnClickListener(v -> {
            // 가짜 테스트는 권한 필요 없음
            volumeChart.clear();
            // UI 초기화
            for(ProgressBar pb : micProgressBars) pb.setProgress(0);
            for(TextView tv : micDbTexts) tv.setText("0 dB");

            viewModel.startFakeMicTest();
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            buttonMicTest.setEnabled(!isLoading);
            buttonFakeTest.setEnabled(!isLoading); // 시뮬레이션 버튼도 제어
            buttonStopTest.setEnabled(isLoading);
        });
    }

    private void initViews(View root) {
        volumeChart = root.findViewById(R.id.volume_chart);
        buttonMicTest = root.findViewById(R.id.button_mic_test);
        buttonStopTest = root.findViewById(R.id.button_stop_test);
        buttonFakeTest = root.findViewById(R.id.button_fake_test);

        // Bind 4 Mics UI
        micProgressBars = new ProgressBar[4];
        micDbTexts = new TextView[4];

        micProgressBars[0] = root.findViewById(R.id.progress_mic_1);
        micProgressBars[1] = root.findViewById(R.id.progress_mic_2);
        micProgressBars[2] = root.findViewById(R.id.progress_mic_3);
        micProgressBars[3] = root.findViewById(R.id.progress_mic_4);

        micDbTexts[0] = root.findViewById(R.id.text_db_mic_1);
        micDbTexts[1] = root.findViewById(R.id.text_db_mic_2);
        micDbTexts[2] = root.findViewById(R.id.text_db_mic_3);
        micDbTexts[3] = root.findViewById(R.id.text_db_mic_4);
    }

    private void setupChart() {
        volumeChart.setTouchEnabled(false);
        volumeChart.setDescription(null);
        volumeChart.getLegend().setTextColor(Color.WHITE);

        XAxis xAxis = volumeChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#333333"));

        YAxis leftAxis = volumeChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f); // dB range 0~100

        volumeChart.getAxisRight().setEnabled(false);
    }

    private void setupObservers() {
        viewModel.getChartData().observe(getViewLifecycleOwner(), data -> {
            if (data != null && !data.isEmpty()) {
                updateChart(data);       // 라인 차트 업데이트 (이력)
                updateVolumeMeters(data); // 실시간 막대바 업데이트 (현재값)
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            buttonMicTest.setEnabled(!isLoading);
            buttonStopTest.setEnabled(isLoading);
        });
    }

    private void startMicTest() {
        volumeChart.clear();
        // Reset Bars
        for(ProgressBar pb : micProgressBars) pb.setProgress(0);
        for(TextView tv : micDbTexts) tv.setText("0 dB");
        viewModel.startMicTest();
    }

    // ★ 실시간 막대바 업데이트 로직
    private void updateVolumeMeters(List<List<Float>> allMicsData) {
        for (int i = 0; i < 4; i++) {
            if (i < allMicsData.size()) {
                List<Float> history = allMicsData.get(i);
                if (!history.isEmpty()) {
                    // 가장 최근 데이터(마지막 값)을 가져옴
                    float currentDb = history.get(history.size() - 1);

                    // ProgressBar 업데이트
                    micProgressBars[i].setProgress((int) currentDb);

                    // 텍스트 업데이트
                    micDbTexts[i].setText(String.format("%d dB", (int) currentDb));

                    // 활성화 감지 효과 (소리가 작으면 회색, 크면 색상)
                    if (currentDb > 10) { // Noise threshold
                        micDbTexts[i].setTextColor(MIC_COLORS[i]);
                    } else {
                        micDbTexts[i].setTextColor(Color.GRAY);
                    }
                }
            }
        }
    }

    private void updateChart(List<List<Float>> allMicsData) {
        LineData lineData = new LineData();
        for (int i = 0; i < allMicsData.size(); i++) {
            List<Float> micData = allMicsData.get(i);
            if (micData == null || micData.isEmpty()) continue;

            List<Entry> entries = new ArrayList<>();
            // 최근 50개 데이터만 보여주기 (그래프가 너무 빽빽해지지 않도록)
            int start = Math.max(0, micData.size() - 50);
            for (int j = start; j < micData.size(); j++) {
                entries.add(new Entry(j - start, micData.get(j)));
            }

            LineDataSet dataSet = new LineDataSet(entries, "Mic " + (i + 1));
            dataSet.setColor(MIC_COLORS[i]);
            dataSet.setDrawCircles(false); // 점 제거 (선만 깔끔하게)
            dataSet.setLineWidth(2f);
            dataSet.setDrawValues(false);
            lineData.addDataSet(dataSet);
        }
        volumeChart.setData(lineData);
        volumeChart.invalidate();
    }
}
