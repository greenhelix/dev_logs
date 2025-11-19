package com.innopia.bist.ver2.ui.fragment.test;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import com.innopia.bist.ver2.R;
import com.innopia.bist.ver2.data.repository.RcuButtonTestRepository;
import com.innopia.bist.ver2.viewmodel.RcuButtonTestViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RCU Button Test Fragment
 */
public class RcuButtonTestFragment extends Fragment {

    private static final String TAG = "RcuButtonTestFragment";
    private RcuButtonTestViewModel viewModel;

    private TextView titleText;
    private TextView statusText;
    private BarChart buttonChart;
    private Button buttonStartTest;
    private Button buttonStopTest;

    private TextView lastButtonValue;
    private TextView lastKeyCodeValue;
    private TextView responseTimeValue;
    private TextView totalPressesValue;
    private TextView avgResponseTimeValue;
    private TextView buttonHistoryValue;

    private long buttonPressStartTime = 0;

    public static RcuButtonTestFragment newInstance() {
        return new RcuButtonTestFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_rcu_button_test, container, false);

        initViews(root);
        setupChart();

        // 키 이벤트 리스너 설정
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                buttonPressStartTime = System.currentTimeMillis();
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                long responseTime = System.currentTimeMillis() - buttonPressStartTime;
                viewModel.recordButtonPress(keyCode, responseTime);
                return true;
            }
            return false;
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> requestInitialFocus(), 100);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RcuButtonTestViewModel.class);
        setupObservers();
        setupButtons();
    }

    private void initViews(View root) {
        titleText = root.findViewById(R.id.title_text);
        statusText = root.findViewById(R.id.status_text);
        buttonChart = root.findViewById(R.id.button_chart);
        buttonStartTest = root.findViewById(R.id.button_start_test);
        buttonStopTest = root.findViewById(R.id.button_stop_test);

        lastButtonValue = root.findViewById(R.id.last_button_value);
        lastKeyCodeValue = root.findViewById(R.id.last_keycode_value);
        responseTimeValue = root.findViewById(R.id.response_time_value);
        totalPressesValue = root.findViewById(R.id.total_presses_value);
        avgResponseTimeValue = root.findViewById(R.id.avg_response_time_value);
        buttonHistoryValue = root.findViewById(R.id.button_history_value);
    }

    private void setupChart() {
        buttonChart.setTouchEnabled(true);
        buttonChart.setDrawBarShadow(false);
        buttonChart.setDrawValueAboveBar(true);
        buttonChart.setBackgroundColor(Color.parseColor("#1a1a1a"));
        buttonChart.setDrawGridBackground(false);

        Description desc = new Description();
        desc.setText("Button Press Count");
        desc.setTextColor(Color.WHITE);
        desc.setTextSize(12f);
        buttonChart.setDescription(desc);

        buttonChart.getLegend().setEnabled(false);

        XAxis xAxis = buttonChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = buttonChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#333333"));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);

        buttonChart.getAxisRight().setEnabled(false);
        buttonChart.setData(new BarData());
        buttonChart.invalidate();
    }

    private void requestInitialFocus() {
        if (getView() != null) {
            getView().requestFocus();
        }
    }

    private void setupObservers() {
        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                statusText.setText(message);
            }
        });

        viewModel.getLastButtonEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                lastButtonValue.setText(event.buttonName);
                lastKeyCodeValue.setText(String.valueOf(event.keyCode));
                responseTimeValue.setText(String.format("%d ms", event.responseTime));
            }
        });

        viewModel.getTestResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                updateResults(result);
                updateChart(result);
            }
        });

        viewModel.getIsTestRunning().observe(getViewLifecycleOwner(), isRunning -> {
            buttonStartTest.setEnabled(!isRunning);
            buttonStopTest.setEnabled(isRunning);
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
            buttonChart.clear();
            viewModel.startButtonTest();
            if (getView() != null) {
                getView().requestFocus();
            }
        });

        buttonStopTest.setOnClickListener(v -> {
            viewModel.stopTest();
        });
    }

    private void updateChart(RcuButtonTestRepository.RcuButtonTestResult result) {
        if (result.buttonCountMap == null || result.buttonCountMap.isEmpty()) {
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Integer> entry : result.buttonCountMap.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Button Presses");
        dataSet.setColor(Color.parseColor("#9C27B0"));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.8f);

        buttonChart.setData(data);
        buttonChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        buttonChart.notifyDataSetChanged();
        buttonChart.invalidate();
    }

    private void updateResults(RcuButtonTestRepository.RcuButtonTestResult result) {
        totalPressesValue.setText(String.valueOf(result.buttonHistory.size()));
        avgResponseTimeValue.setText(String.format("%.1f ms", result.averageResponseTime));

        // 최근 5개 버튼 히스토리
        StringBuilder history = new StringBuilder();
        int start = Math.max(0, result.buttonHistory.size() - 5);
        for (int i = start; i < result.buttonHistory.size(); i++) {
            RcuButtonTestRepository.ButtonEvent event = result.buttonHistory.get(i);
            history.append(event.buttonName).append(" (").append(event.responseTime).append("ms)\n");
        }
        buttonHistoryValue.setText(history.toString().trim());
    }

    private void resetResults() {
        lastButtonValue.setText("--");
        lastKeyCodeValue.setText("--");
        responseTimeValue.setText("--");
        totalPressesValue.setText("0");
        avgResponseTimeValue.setText("--");
        buttonHistoryValue.setText("--");
    }
}
