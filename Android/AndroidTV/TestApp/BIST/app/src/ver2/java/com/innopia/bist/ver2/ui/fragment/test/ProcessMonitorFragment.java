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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import com.innopia.bist.ver2.R;
import com.innopia.bist.ver2.data.repository.ProcessMonitorRepository;
import com.innopia.bist.ver2.viewmodel.ProcessMonitorViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Process Monitor Fragment
 */
public class ProcessMonitorFragment extends Fragment {

    private static final String TAG = "ProcessMonitorFragment";
    private ProcessMonitorViewModel viewModel;

    private TextView titleText;
    private TextView statusText;
    private HorizontalBarChart processChart;
    private Button buttonStartMonitoring;
    private Button buttonStopMonitoring;

    private TextView totalMemoryValue;
    private TextView usedMemoryValue;
    private TextView availableMemoryValue;
    private TextView process1Name;
    private TextView process1Memory;
    private TextView process2Name;
    private TextView process2Memory;
    private TextView process3Name;
    private TextView process3Memory;
    private TextView process4Name;
    private TextView process4Memory;
    private TextView process5Name;
    private TextView process5Memory;

    public static ProcessMonitorFragment newInstance() {
        return new ProcessMonitorFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_process_monitor, container, false);

        initViews(root);
        setupChart();

        new Handler(Looper.getMainLooper()).postDelayed(() -> requestInitialFocus(), 100);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProcessMonitorViewModel.class);
        setupObservers();
        setupButtons();
    }

    private void initViews(View root) {
        titleText = root.findViewById(R.id.title_text);
        statusText = root.findViewById(R.id.status_text);
        processChart = root.findViewById(R.id.process_chart);
        buttonStartMonitoring = root.findViewById(R.id.button_start_monitoring);
        buttonStopMonitoring = root.findViewById(R.id.button_stop_monitoring);

        totalMemoryValue = root.findViewById(R.id.total_memory_value);
        usedMemoryValue = root.findViewById(R.id.used_memory_value);
        availableMemoryValue = root.findViewById(R.id.available_memory_value);

        process1Name = root.findViewById(R.id.process1_name);
        process1Memory = root.findViewById(R.id.process1_memory);
        process2Name = root.findViewById(R.id.process2_name);
        process2Memory = root.findViewById(R.id.process2_memory);
        process3Name = root.findViewById(R.id.process3_name);
        process3Memory = root.findViewById(R.id.process3_memory);
        process4Name = root.findViewById(R.id.process4_name);
        process4Memory = root.findViewById(R.id.process4_memory);
        process5Name = root.findViewById(R.id.process5_name);
        process5Memory = root.findViewById(R.id.process5_memory);
    }

    private void setupChart() {
        processChart.setTouchEnabled(true);
        processChart.setDrawBarShadow(false);
        processChart.setDrawValueAboveBar(true);
        processChart.setBackgroundColor(Color.parseColor("#1a1a1a"));
        processChart.setDrawGridBackground(false);

        Description desc = new Description();
        desc.setText("Memory Usage (MB)");
        desc.setTextColor(Color.WHITE);
        desc.setTextSize(12f);
        processChart.setDescription(desc);

        processChart.getLegend().setEnabled(false);

        XAxis xAxis = processChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = processChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#333333"));
        leftAxis.setAxisMinimum(0f);

        processChart.getAxisRight().setEnabled(false);
        processChart.setData(new BarData());
        processChart.invalidate();
    }

    private void requestInitialFocus() {
        if (buttonStartMonitoring != null) {
            buttonStartMonitoring.requestFocus();
        }
    }

    private void setupObservers() {
        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                statusText.setText(message);
            }
        });

        viewModel.getMonitorResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                updateResults(result);
                updateChart(result);
            }
        });

        viewModel.getIsMonitoring().observe(getViewLifecycleOwner(), isMonitoring -> {
            buttonStartMonitoring.setEnabled(!isMonitoring);
            buttonStopMonitoring.setEnabled(isMonitoring);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupButtons() {
        buttonStartMonitoring.setOnClickListener(v -> {
            resetResults();
            processChart.clear();
            viewModel.startMonitoring();
        });

        buttonStopMonitoring.setOnClickListener(v -> {
            viewModel.stopMonitoring();
        });
    }

    private void updateChart(ProcessMonitorRepository.ProcessMonitorResult result) {
        if (result.topProcesses == null || result.topProcesses.isEmpty()) {
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < result.topProcesses.size(); i++) {
            ProcessMonitorRepository.ProcessInfo process = result.topProcesses.get(i);
            entries.add(new BarEntry(i, process.memoryUsage));

            String shortName = process.processName;
            if (shortName.contains(".")) {
                String[] parts = shortName.split("\\.");
                shortName = parts[parts.length - 1];
            }
            labels.add(shortName);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Memory Usage");

        List<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#FF5252"));
        colors.add(Color.parseColor("#FF9800"));
        colors.add(Color.parseColor("#FFC107"));
        colors.add(Color.parseColor("#8BC34A"));
        colors.add(Color.parseColor("#4CAF50"));
        dataSet.setColors(colors);

        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.8f);

        processChart.setData(data);
        processChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        processChart.getXAxis().setGranularity(1f);
        processChart.notifyDataSetChanged();
        processChart.invalidate();
    }

    private void updateResults(ProcessMonitorRepository.ProcessMonitorResult result) {
        totalMemoryValue.setText(String.format("%d MB", result.totalMemory));
        usedMemoryValue.setText(String.format("%d MB", result.usedMemory));
        availableMemoryValue.setText(String.format("%d MB", result.availableMemory));

        if (result.topProcesses != null && !result.topProcesses.isEmpty()) {
            if (result.topProcesses.size() > 0) {
                ProcessMonitorRepository.ProcessInfo p1 = result.topProcesses.get(0);
                process1Name.setText(p1.processName);
                process1Memory.setText(String.format("%d MB", p1.memoryUsage));
            }
            if (result.topProcesses.size() > 1) {
                ProcessMonitorRepository.ProcessInfo p2 = result.topProcesses.get(1);
                process2Name.setText(p2.processName);
                process2Memory.setText(String.format("%d MB", p2.memoryUsage));
            }
            if (result.topProcesses.size() > 2) {
                ProcessMonitorRepository.ProcessInfo p3 = result.topProcesses.get(2);
                process3Name.setText(p3.processName);
                process3Memory.setText(String.format("%d MB", p3.memoryUsage));
            }
            if (result.topProcesses.size() > 3) {
                ProcessMonitorRepository.ProcessInfo p4 = result.topProcesses.get(3);
                process4Name.setText(p4.processName);
                process4Memory.setText(String.format("%d MB", p4.memoryUsage));
            }
            if (result.topProcesses.size() > 4) {
                ProcessMonitorRepository.ProcessInfo p5 = result.topProcesses.get(4);
                process5Name.setText(p5.processName);
                process5Memory.setText(String.format("%d MB", p5.memoryUsage));
            }
        }
    }

    private void resetResults() {
        totalMemoryValue.setText("--");
        usedMemoryValue.setText("--");
        availableMemoryValue.setText("--");
        process1Name.setText("--");
        process1Memory.setText("--");
        process2Name.setText("--");
        process2Memory.setText("--");
        process3Name.setText("--");
        process3Memory.setText("--");
        process4Name.setText("--");
        process4Memory.setText("--");
        process5Name.setText("--");
        process5Memory.setText("--");
    }
}
