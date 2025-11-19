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
import com.innopia.bist.ver2.data.repository.CpuTestRepository;
import com.innopia.bist.ver2.viewmodel.CpuTestViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * CPU Test Fragment
 */
public class CpuTestFragment extends Fragment {

    private static final String TAG = "CpuTestFragment";
    private CpuTestViewModel viewModel;

    private TextView titleText;
    private TextView statusText;
    private ProgressBar progressBar;
    private TextView progressText;
    private LineChart performanceChart;
    private Button buttonStartTest;
    private Button buttonStopTest;

    private TextView cpuTimeValue;
    private TextView cpuOpsValue;
    private TextView cpuCoresValue;
    private TextView cpuUsageValue;

    public static CpuTestFragment newInstance() {
        return new CpuTestFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cpu_test, container, false);

        initViews(root);
        setupChart();

        new Handler(Looper.getMainLooper()).postDelayed(() -> requestInitialFocus(), 100);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CpuTestViewModel.class);
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

        cpuTimeValue = root.findViewById(R.id.cpu_time_value);
        cpuOpsValue = root.findViewById(R.id.cpu_ops_value);
        cpuCoresValue = root.findViewById(R.id.cpu_cores_value);
        cpuUsageValue = root.findViewById(R.id.cpu_usage_value);
    }

    private void setupChart() {
        performanceChart.setTouchEnabled(true);
        performanceChart.setDragEnabled(true);
        performanceChart.setScaleEnabled(true);
        performanceChart.setPinchZoom(true);
        performanceChart.setDrawGridBackground(false);
        performanceChart.setBackgroundColor(Color.parseColor("#1a1a1a"));

        Description desc = new Description();
        desc.setText("CPU Performance (K ops/s)");
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
            viewModel.startCpuTest();
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

        LineDataSet dataSet = new LineDataSet(entries, "CPU Performance");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#2196F3"));
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

    private void updateResults(CpuTestRepository.CpuTestResult result) {
        cpuTimeValue.setText(String.format("%.2f ms", result.executionTime));
        cpuOpsValue.setText(String.format("%.0f ops/s", result.operationsPerSecond));
        cpuCoresValue.setText(String.valueOf(result.coreCount));
        cpuUsageValue.setText(String.format("%.1f%%", result.cpuUsage));

        if (result.cpuUsage > 80) {
            cpuUsageValue.setTextColor(Color.RED);
        } else if (result.cpuUsage > 60) {
            cpuUsageValue.setTextColor(Color.YELLOW);
        } else {
            cpuUsageValue.setTextColor(Color.GREEN);
        }
    }

    private void resetResults() {
        cpuTimeValue.setText("--");
        cpuOpsValue.setText("--");
        cpuCoresValue.setText("--");
        cpuUsageValue.setText("--");
        cpuUsageValue.setTextColor(Color.WHITE);
    }
}
