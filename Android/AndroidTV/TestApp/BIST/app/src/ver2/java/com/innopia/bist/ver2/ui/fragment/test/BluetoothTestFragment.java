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
import com.innopia.bist.ver2.data.repository.BluetoothTestRepository;
import com.innopia.bist.ver2.viewmodel.BluetoothTestViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Bluetooth Test Fragment
 */
public class BluetoothTestFragment extends Fragment {

    private static final String TAG = "BluetoothTestFragment";
    private BluetoothTestViewModel viewModel;

    private TextView titleText;
    private TextView statusText;
    private ProgressBar progressBar;
    private TextView progressText;
    private LineChart signalChart;
    private Button buttonStartTest;
    private Button buttonStopTest;

    private TextView btStatusValue;
    private TextView btEnabledValue;
    private TextView pairedDevicesValue;
    private TextView deviceListValue;
    private TextView connectionStatusValue;

    public static BluetoothTestFragment newInstance() {
        return new BluetoothTestFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_bluetooth_test, container, false);

        initViews(root);
        setupChart();

        new Handler(Looper.getMainLooper()).postDelayed(() -> requestInitialFocus(), 100);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(BluetoothTestViewModel.class);
        setupObservers();
        setupButtons();
    }

    private void initViews(View root) {
        titleText = root.findViewById(R.id.title_text);
        statusText = root.findViewById(R.id.status_text);
        progressBar = root.findViewById(R.id.progress_bar);
        progressText = root.findViewById(R.id.progress_text);
        signalChart = root.findViewById(R.id.signal_chart);
        buttonStartTest = root.findViewById(R.id.button_start_test);
        buttonStopTest = root.findViewById(R.id.button_stop_test);

        btStatusValue = root.findViewById(R.id.bt_status_value);
        btEnabledValue = root.findViewById(R.id.bt_enabled_value);
        pairedDevicesValue = root.findViewById(R.id.paired_devices_value);
        deviceListValue = root.findViewById(R.id.device_list_value);
        connectionStatusValue = root.findViewById(R.id.connection_status_value);
    }

    private void setupChart() {
        signalChart.setTouchEnabled(true);
        signalChart.setDragEnabled(true);
        signalChart.setScaleEnabled(true);
        signalChart.setPinchZoom(true);
        signalChart.setDrawGridBackground(false);
        signalChart.setBackgroundColor(Color.parseColor("#1a1a1a"));

        Description desc = new Description();
        desc.setText("Signal Strength (dBm)");
        desc.setTextColor(Color.WHITE);
        desc.setTextSize(12f);
        signalChart.setDescription(desc);

        signalChart.getLegend().setEnabled(true);
        signalChart.getLegend().setTextColor(Color.WHITE);

        XAxis xAxis = signalChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#333333"));

        YAxis leftAxis = signalChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#333333"));

        signalChart.getAxisRight().setEnabled(false);
        signalChart.setData(new LineData());
        signalChart.invalidate();
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
                if (result.signalStrengthData != null && !result.signalStrengthData.isEmpty()) {
                    updateChart(result.signalStrengthData);
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
            signalChart.clear();
            viewModel.startBluetoothTest();
        });

        buttonStopTest.setOnClickListener(v -> {
            // Bluetooth test는 빠르게 완료되므로 stop 불필요
        });
    }

    private void updateChart(List<Float> data) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new Entry(i, data.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Signal Strength");
        dataSet.setColor(Color.parseColor("#03A9F4"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#03A9F4"));
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        signalChart.setData(lineData);
        signalChart.notifyDataSetChanged();
        signalChart.invalidate();
    }

    private void updateResults(BluetoothTestRepository.BluetoothTestResult result) {
        btStatusValue.setText(result.isBluetoothAvailable ? "Available" : "Not Available");
        btStatusValue.setTextColor(result.isBluetoothAvailable ? Color.GREEN : Color.RED);

        btEnabledValue.setText(result.isBluetoothEnabled ? "Enabled" : "Disabled");
        btEnabledValue.setTextColor(result.isBluetoothEnabled ? Color.GREEN : Color.RED);

        pairedDevicesValue.setText(String.valueOf(result.pairedDeviceCount));

        if (result.deviceNames != null && !result.deviceNames.isEmpty()) {
            StringBuilder devices = new StringBuilder();
            for (String name : result.deviceNames) {
                devices.append(name).append("\n");
            }
            deviceListValue.setText(devices.toString().trim());
        } else {
            deviceListValue.setText("No devices");
        }

        connectionStatusValue.setText(result.connectionSuccessful ? "Connected" : "Not Connected");
        connectionStatusValue.setTextColor(result.connectionSuccessful ? Color.GREEN : Color.RED);

        if (result.errorMessage != null && !result.errorMessage.isEmpty()) {
            Toast.makeText(getContext(), result.errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void resetResults() {
        btStatusValue.setText("--");
        btStatusValue.setTextColor(Color.WHITE);
        btEnabledValue.setText("--");
        btEnabledValue.setTextColor(Color.WHITE);
        pairedDevicesValue.setText("--");
        deviceListValue.setText("--");
        connectionStatusValue.setText("--");
        connectionStatusValue.setTextColor(Color.WHITE);
    }
}
