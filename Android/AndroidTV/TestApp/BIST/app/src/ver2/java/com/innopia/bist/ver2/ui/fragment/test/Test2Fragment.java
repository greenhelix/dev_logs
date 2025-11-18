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
import com.innopia.bist.ver2.data.repository.Test2Repository;
import com.innopia.bist.ver2.viewmodel.Test2ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Test2Fragment - 메모리 및 CPU 성능 측정 (그래프 포함)
 */
public class Test2Fragment extends Fragment {

    private static final String TAG = "Test2Fragment";
    private Test2ViewModel viewModel;

    // UI 컴포넌트
    private TextView titleText;
    private TextView statusText;
    private ProgressBar progressBar;
    private TextView progressText;
    private LineChart performanceChart;

    // 메모리 테스트 UI
    private Button buttonMemoryTest;
    private TextView memorySpeedValue;
    private TextView memoryTotalValue;
    private TextView memoryUsedValue;
    private TextView memoryAvailableValue;
    private TextView memoryUsageValue;

    // CPU 테스트 UI
    private Button buttonCpuTest;
    private TextView cpuTimeValue;
    private TextView cpuOpsValue;
    private TextView cpuCoresValue;
    private TextView cpuUsageValue;

    // 전체 테스트 버튼
    private Button buttonStopTest;

    public static Test2Fragment newInstance() {
        return new Test2Fragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_test2, container, false);

        // UI 초기화
        initViews(root);

        // 차트 초기화
        setupChart();

        // 초기 포커스 설정
        new Handler(Looper.getMainLooper()).postDelayed(() -> requestInitialFocus(), 100);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel 초기화
        viewModel = new ViewModelProvider(this).get(Test2ViewModel.class);

        // 옵저버 설정
        setupObservers();

        // 버튼 리스너 설정
        setupButtons();
    }

    private void initViews(View root) {
        titleText = root.findViewById(R.id.title_text);
        statusText = root.findViewById(R.id.status_text);
        progressBar = root.findViewById(R.id.progress_bar);
        progressText = root.findViewById(R.id.progress_text);
        performanceChart = root.findViewById(R.id.performance_chart);

        // 메모리 테스트 UI
        buttonMemoryTest = root.findViewById(R.id.button_memory_test);
        memorySpeedValue = root.findViewById(R.id.memory_speed_value);
        memoryTotalValue = root.findViewById(R.id.memory_total_value);
        memoryUsedValue = root.findViewById(R.id.memory_used_value);
        memoryAvailableValue = root.findViewById(R.id.memory_available_value);
        memoryUsageValue = root.findViewById(R.id.memory_usage_value);

        // CPU 테스트 UI
        buttonCpuTest = root.findViewById(R.id.button_cpu_test);
        cpuTimeValue = root.findViewById(R.id.cpu_time_value);
        cpuOpsValue = root.findViewById(R.id.cpu_ops_value);
        cpuCoresValue = root.findViewById(R.id.cpu_cores_value);
        cpuUsageValue = root.findViewById(R.id.cpu_usage_value);

        buttonStopTest = root.findViewById(R.id.button_stop_test);
    }

    private void setupChart() {
        // 차트 기본 설정
        performanceChart.setTouchEnabled(true);
        performanceChart.setDragEnabled(true);
        performanceChart.setScaleEnabled(true);
        performanceChart.setPinchZoom(true);
        performanceChart.setDrawGridBackground(false);
        performanceChart.setBackgroundColor(Color.parseColor("#1a1a1a"));

        // Description 설정
        Description desc = new Description();
        desc.setText("Performance Over Time");
        desc.setTextColor(Color.WHITE);
        desc.setTextSize(12f);
        performanceChart.setDescription(desc);

        // Legend 설정
        performanceChart.getLegend().setEnabled(true);
        performanceChart.getLegend().setTextColor(Color.WHITE);
        performanceChart.getLegend().setTextSize(12f);

        // X축 설정
        XAxis xAxis = performanceChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#333333"));
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        // Y축 (왼쪽) 설정
        YAxis leftAxis = performanceChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#333333"));
        leftAxis.setAxisMinimum(0f);

        // Y축 (오른쪽) 비활성화
        performanceChart.getAxisRight().setEnabled(false);

        // 빈 데이터로 초기화
        performanceChart.setData(new LineData());
        performanceChart.invalidate();
    }

    private void requestInitialFocus() {
        if (buttonMemoryTest != null) {
            buttonMemoryTest.requestFocus();
            Log.d(TAG, "Initial focus requested on button_memory_test");
        }
    }

    private void setupObservers() {
        // 진행률 observe
        viewModel.getProgress().observe(getViewLifecycleOwner(), progress -> {
            if (progress != null) {
                progressBar.setProgress(progress);
                progressText.setText(progress + "%");
            }
        });

        // 상태 메시지 observe
        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                statusText.setText(message);
            }
        });

        // 차트 데이터 observe
        viewModel.getChartData().observe(getViewLifecycleOwner(), data -> {
            if (data != null && !data.isEmpty()) {
                updateChart(data);
            }
        });

        // 메모리 테스트 결과 observe
        viewModel.getMemoryResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Log.d(TAG, "Memory test result received");
                updateMemoryResults(result);
            }
        });

        // CPU 테스트 결과 observe
        viewModel.getCpuResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Log.d(TAG, "CPU test result received");
                updateCpuResults(result);
            }
        });

        // 로딩 상태 observe
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            buttonMemoryTest.setEnabled(!isLoading);
            buttonCpuTest.setEnabled(!isLoading);
            buttonStopTest.setEnabled(isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
            progressText.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        });

        // 에러 메시지 observe
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupButtons() {
        buttonMemoryTest.setOnClickListener(v -> {
            Log.d(TAG, "Memory test button clicked");
            resetMemoryResults();
            performanceChart.clear();
            viewModel.startMemoryTest();
        });

        buttonCpuTest.setOnClickListener(v -> {
            Log.d(TAG, "CPU test button clicked");
            resetCpuResults();
            performanceChart.clear();
            viewModel.startCpuTest();
        });

        buttonStopTest.setOnClickListener(v -> {
            Log.d(TAG, "Stop test button clicked");
            viewModel.stopTest();
        });

        // 포커스 변경 리스너
        buttonMemoryTest.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "button_memory_test focus: " + hasFocus);
        });

        buttonCpuTest.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "button_cpu_test focus: " + hasFocus);
        });
    }

    private void updateChart(List<Float> data) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new Entry(i, data.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Performance");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        performanceChart.setData(lineData);
        performanceChart.notifyDataSetChanged();
        performanceChart.invalidate();
    }

    private void updateMemoryResults(Test2Repository.MemoryTestResult result) {
        memorySpeedValue.setText(String.format("%.2f MB/s", result.memorySpeed));
        memoryTotalValue.setText(String.format("%d MB", result.totalMemory));
        memoryUsedValue.setText(String.format("%d MB", result.usedMemory));
        memoryAvailableValue.setText(String.format("%d MB", result.availableMemory));
        memoryUsageValue.setText(String.format("%.1f%%", result.memoryUsagePercent));

        // 메모리 사용률에 따른 색상 변경
        if (result.memoryUsagePercent > 80) {
            memoryUsageValue.setTextColor(Color.RED);
        } else if (result.memoryUsagePercent > 60) {
            memoryUsageValue.setTextColor(Color.YELLOW);
        } else {
            memoryUsageValue.setTextColor(Color.GREEN);
        }
    }

    private void updateCpuResults(Test2Repository.CpuTestResult result) {
        cpuTimeValue.setText(String.format("%.2f ms", result.executionTime));
        cpuOpsValue.setText(String.format("%.0f ops/s", result.operationsPerSecond));
        cpuCoresValue.setText(String.valueOf(result.coreCount));
        cpuUsageValue.setText(String.format("%.1f%%", result.cpuUsage));

        // CPU 사용률에 따른 색상 변경
        if (result.cpuUsage > 80) {
            cpuUsageValue.setTextColor(Color.RED);
        } else if (result.cpuUsage > 60) {
            cpuUsageValue.setTextColor(Color.YELLOW);
        } else {
            cpuUsageValue.setTextColor(Color.GREEN);
        }
    }

    private void resetMemoryResults() {
        memorySpeedValue.setText("--");
        memoryTotalValue.setText("--");
        memoryUsedValue.setText("--");
        memoryAvailableValue.setText("--");
        memoryUsageValue.setText("--");
        memoryUsageValue.setTextColor(Color.WHITE);
    }

    private void resetCpuResults() {
        cpuTimeValue.setText("--");
        cpuOpsValue.setText("--");
        cpuCoresValue.setText("--");
        cpuUsageValue.setText("--");
        cpuUsageValue.setTextColor(Color.WHITE);
    }
}
