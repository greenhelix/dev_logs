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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.innopia.bist.ver2.R;
import com.innopia.bist.ver2.data.repository.TemperatureTestRepository;
import com.innopia.bist.ver2.viewmodel.TemperatureTestViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Temperature Test Fragment
 * CPU 및 배터리 온도 모니터링
 */
public class TemperatureTestFragment extends Fragment {

    private static final String TAG = "TemperatureTestFragment";
    private TemperatureTestViewModel viewModel;

    private TextView titleText;
    private TextView statusText;
    private LineChart temperatureChart;
    private Button buttonStartMonitoring;
    private Button buttonStopMonitoring;

    // CPU 온도
    private TextView cpuTempValue;
    private TextView cpuStatusValue;

    // 배터리 온도
    private TextView batteryTempValue;
    private TextView batteryStatusValue;

    public static TemperatureTestFragment newInstance() {
        return new TemperatureTestFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_temperature_test, container, false);

        initViews(root);
        setupChart();

        new Handler(Looper.getMainLooper()).postDelayed(() -> requestInitialFocus(), 100);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TemperatureTestViewModel.class);
        setupObservers();
        setupButtons();
    }

    private void initViews(View root) {
        titleText = root.findViewById(R.id.title_text);
        statusText = root.findViewById(R.id.status_text);
        temperatureChart = root.findViewById(R.id.temperature_chart);
        buttonStartMonitoring = root.findViewById(R.id.button_start_monitoring);
        buttonStopMonitoring = root.findViewById(R.id.button_stop_monitoring);

        cpuTempValue = root.findViewById(R.id.cpu_temp_value);
        cpuStatusValue = root.findViewById(R.id.cpu_status_value);
        batteryTempValue = root.findViewById(R.id.battery_temp_value);
        batteryStatusValue = root.findViewById(R.id.battery_status_value);
    }

    private void setupChart() {
        temperatureChart.setTouchEnabled(true);
        temperatureChart.setDragEnabled(true);
        temperatureChart.setScaleEnabled(true);
        temperatureChart.setPinchZoom(true);
        temperatureChart.setDrawGridBackground(false);
        temperatureChart.setBackgroundColor(Color.parseColor("#1a1a1a"));

        Description desc = new Description();
        desc.setText("Temperature (°C)");
        desc.setTextColor(Color.WHITE);
        desc.setTextSize(12f);
        temperatureChart.setDescription(desc);

        temperatureChart.getLegend().setEnabled(true);
        temperatureChart.getLegend().setTextColor(Color.WHITE);
        temperatureChart.getLegend().setTextSize(12f);

        XAxis xAxis = temperatureChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#333333"));

        YAxis leftAxis = temperatureChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#333333"));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);

        temperatureChart.getAxisRight().setEnabled(false);
        temperatureChart.setData(new LineData());
        temperatureChart.invalidate();
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

        viewModel.getTemperatureResult().observe(getViewLifecycleOwner(), result -> {
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
            temperatureChart.clear();
            viewModel.startMonitoring();
        });

        buttonStopMonitoring.setOnClickListener(v -> {
            viewModel.stopMonitoring();
        });
    }

    private void updateChart(TemperatureTestRepository.TemperatureTestResult result) {
        if (result.cpuTempHistory == null || result.cpuTempHistory.isEmpty()) {
            return;
        }

        List<Entry> cpuEntries = new ArrayList<>();
        List<Entry> batteryEntries = new ArrayList<>();

        for (int i = 0; i < result.cpuTempHistory.size(); i++) {
            cpuEntries.add(new Entry(i, result.cpuTempHistory.get(i)));
        }

        for (int i = 0; i < result.batteryTempHistory.size(); i++) {
            batteryEntries.add(new Entry(i, result.batteryTempHistory.get(i)));
        }

        LineDataSet cpuDataSet = new LineDataSet(cpuEntries, "CPU Temp");
        cpuDataSet.setColor(Color.parseColor("#FF5252"));
        cpuDataSet.setLineWidth(2f);
        cpuDataSet.setCircleColor(Color.parseColor("#FF5252"));
        cpuDataSet.setCircleRadius(3f);
        cpuDataSet.setDrawCircleHole(false);
        cpuDataSet.setValueTextColor(Color.WHITE);
        cpuDataSet.setDrawValues(false);
        cpuDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineDataSet batteryDataSet = new LineDataSet(batteryEntries, "Battery Temp");
        batteryDataSet.setColor(Color.parseColor("#4CAF50"));
        batteryDataSet.setLineWidth(2f);
        batteryDataSet.setCircleColor(Color.parseColor("#4CAF50"));
        batteryDataSet.setCircleRadius(3f);
        batteryDataSet.setDrawCircleHole(false);
        batteryDataSet.setValueTextColor(Color.WHITE);
        batteryDataSet.setDrawValues(false);
        batteryDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(cpuDataSet, batteryDataSet);
        temperatureChart.setData(lineData);
        temperatureChart.notifyDataSetChanged();
        temperatureChart.invalidate();
    }

    private void updateResults(TemperatureTestRepository.TemperatureTestResult result) {
        cpuTempValue.setText(String.format("%.1f°C", result.cpuTemperature));
        cpuStatusValue.setText(result.cpuStatus);
        cpuStatusValue.setTextColor(Color.parseColor(result.cpuStatusColor));

        batteryTempValue.setText(String.format("%.1f°C", result.batteryTemperature));
        batteryStatusValue.setText(result.batteryStatus);
        batteryStatusValue.setTextColor(Color.parseColor(result.batteryStatusColor));
    }

    private void resetResults() {
        cpuTempValue.setText("--");
        cpuStatusValue.setText("--");
        cpuStatusValue.setTextColor(Color.WHITE);
        batteryTempValue.setText("--");
        batteryStatusValue.setText("--");
        batteryStatusValue.setTextColor(Color.WHITE);
    }
}
