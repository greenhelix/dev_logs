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
import com.github.mikephil.charting.formatter.ValueFormatter;

import com.innopia.bist.ver2.R;
import com.innopia.bist.ver2.data.repository.MemoryTestRepository;
import com.innopia.bist.ver2.viewmodel.MemoryTestViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Memory Test Fragment
 */
public class MemoryTestFragment extends Fragment {

    private static final String TAG = "MemoryTestFragment";
    private MemoryTestViewModel viewModel;

    // UI 컴포넌트
    private TextView titleText;
    private TextView statusText;
    private ProgressBar progressBar;
    private TextView progressText;
    private LineChart performanceChart;
    private Button buttonStartTest;
    private Button buttonStopTest;

    // 결과 표시
    private TextView memorySpeedValue;
    private TextView memoryTotalValue;
    private TextView memoryUsedValue;
    private TextView memoryAvailableValue;
    private TextView memoryUsageValue;

    public static MemoryTestFragment newInstance() {
        return new MemoryTestFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_memory_test, container, false);

        initViews(root);
        setupChart();

        new Handler(Looper.getMainLooper()).postDelayed(() -> requestInitialFocus(), 100);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MemoryTestViewModel.class);
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

        memorySpeedValue = root.findViewById(R.id.memory_speed_value);
        memoryTotalValue = root.findViewById(R.id.memory_total_value);
        memoryUsedValue = root.findViewById(R.id.memory_used_value);
        memoryAvailableValue = root.findViewById(R.id.memory_available_value);
        memoryUsageValue = root.findViewById(R.id.memory_usage_value);
    }

    private void setupChart() {
        performanceChart.setTouchEnabled(true);
        performanceChart.setDragEnabled(true);
        performanceChart.setScaleEnabled(true);
        performanceChart.setPinchZoom(true);
        performanceChart.setDrawGridBackground(false);
        performanceChart.setBackgroundColor(Color.parseColor("#1a1a1a"));

        Description desc = new Description();
        desc.setText("Memory Speed (MB/s)");
        desc.setTextColor(Color.WHITE);
        desc.setTextSize(12f);
        performanceChart.setDescription(desc);

        performanceChart.getLegend().setEnabled(true);
        performanceChart.getLegend().setTextColor(Color.WHITE);
        performanceChart.getLegend().setTextSize(12f);

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

        viewModel.getChartData().observe(getViewLifecycleOwner(), data -> {
            if (data != null && !data.isEmpty()) {
                updateChart(data);
            }
        });

        viewModel.getTestResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                updateResults(result);
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
            viewModel.startMemoryTest();
        });

        buttonStopTest.setOnClickListener(v -> {
            viewModel.stopTest();
        });
    }

    private void updateChart(List<Float> data) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new Entry(i, data.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Memory Speed");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        performanceChart.setData(lineData);
        performanceChart.notifyDataSetChanged();
        performanceChart.invalidate();
    }

    private void updateResults(MemoryTestRepository.MemoryTestResult result) {
        memorySpeedValue.setText(String.format("%.2f MB/s", result.memorySpeed));
        memoryTotalValue.setText(String.format("%d MB", result.totalMemory));
        memoryUsedValue.setText(String.format("%d MB", result.usedMemory));
        memoryAvailableValue.setText(String.format("%d MB", result.availableMemory));
        memoryUsageValue.setText(String.format("%.1f%%", result.memoryUsagePercent));

        if (result.memoryUsagePercent > 80) {
            memoryUsageValue.setTextColor(Color.RED);
        } else if (result.memoryUsagePercent > 60) {
            memoryUsageValue.setTextColor(Color.YELLOW);
        } else {
            memoryUsageValue.setTextColor(Color.GREEN);
        }
    }

    private void resetResults() {
        memorySpeedValue.setText("--");
        memoryTotalValue.setText("--");
        memoryUsedValue.setText("--");
        memoryAvailableValue.setText("--");
        memoryUsageValue.setText("--");
        memoryUsageValue.setTextColor(Color.WHITE);
    }
}
