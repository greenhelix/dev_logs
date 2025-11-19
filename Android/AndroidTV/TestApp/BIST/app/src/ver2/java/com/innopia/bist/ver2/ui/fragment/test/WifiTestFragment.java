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
import com.innopia.bist.ver2.data.repository.WifiTestRepository;
import com.innopia.bist.ver2.viewmodel.WifiTestViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * WiFi Test Fragment
 */
public class WifiTestFragment extends Fragment {

    private static final String TAG = "WifiTestFragment";
    private WifiTestViewModel viewModel;

    private TextView titleText;
    private TextView statusText;
    private ProgressBar progressBar;
    private TextView progressText;
    private LineChart signalChart;
    private Button buttonStartTest;
    private Button buttonStopTest;

    private TextView wifiStatusValue;
    private TextView wifiEnabledValue;
    private TextView ssidValue;
    private TextView linkSpeedValue;
    private TextView rssiValue;
    private TextView frequencyValue;
    private TextView signalLevelValue;

    public static WifiTestFragment newInstance() {
        return new WifiTestFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_wifi_test, container, false);

        initViews(root);
        setupChart();

        new Handler(Looper.getMainLooper()).postDelayed(() -> requestInitialFocus(), 100);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(WifiTestViewModel.class);
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

        wifiStatusValue = root.findViewById(R.id.wifi_status_value);
        wifiEnabledValue = root.findViewById(R.id.wifi_enabled_value);
        ssidValue = root.findViewById(R.id.ssid_value);
        linkSpeedValue = root.findViewById(R.id.link_speed_value);
        rssiValue = root.findViewById(R.id.rssi_value);
        frequencyValue = root.findViewById(R.id.frequency_value);
        signalLevelValue = root.findViewById(R.id.signal_level_value);
    }

    private void setupChart() {
        signalChart.setTouchEnabled(true);
        signalChart.setDragEnabled(true);
        signalChart.setScaleEnabled(true);
        signalChart.setPinchZoom(true);
        signalChart.setDrawGridBackground(false);
        signalChart.setBackgroundColor(Color.parseColor("#1a1a1a"));

        Description desc = new Description();
        desc.setText("WiFi Signal (dBm)");
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
            viewModel.startWifiTest();
        });

        buttonStopTest.setOnClickListener(v -> {
            // WiFi test는 빠르게 완료
        });
    }

    private void updateChart(List<Float> data) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new Entry(i, data.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Signal Strength");
        dataSet.setColor(Color.parseColor("#00BCD4"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#00BCD4"));
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

    private void updateResults(WifiTestRepository.WifiTestResult result) {
        wifiStatusValue.setText(result.isWifiAvailable ? "Available" : "Not Available");
        wifiStatusValue.setTextColor(result.isWifiAvailable ? Color.GREEN : Color.RED);

        wifiEnabledValue.setText(result.isWifiEnabled ? "Enabled" : "Disabled");
        wifiEnabledValue.setTextColor(result.isWifiEnabled ? Color.GREEN : Color.RED);

        if (result.isConnected) {
            ssidValue.setText(result.ssid);
            linkSpeedValue.setText(String.format("%d Mbps", result.linkSpeed));
            rssiValue.setText(String.format("%d dBm", result.rssi));
            frequencyValue.setText(String.format("%d MHz", result.frequency));
            signalLevelValue.setText(String.format("%d/4", result.signalLevel));
        } else {
            ssidValue.setText("Not connected");
            linkSpeedValue.setText("--");
            rssiValue.setText("--");
            frequencyValue.setText("--");
            signalLevelValue.setText("--");
        }

        if (result.errorMessage != null && !result.errorMessage.isEmpty()) {
            Toast.makeText(getContext(), result.errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void resetResults() {
        wifiStatusValue.setText("--");
        wifiStatusValue.setTextColor(Color.WHITE);
        wifiEnabledValue.setText("--");
        wifiEnabledValue.setTextColor(Color.WHITE);
        ssidValue.setText("--");
        linkSpeedValue.setText("--");
        rssiValue.setText("--");
        frequencyValue.setText("--");
        signalLevelValue.setText("--");
    }
}
