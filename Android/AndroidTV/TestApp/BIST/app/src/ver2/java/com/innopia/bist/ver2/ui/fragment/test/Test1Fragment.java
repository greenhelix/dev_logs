package com.innopia.bist.ver2.ui.fragment.test;

import androidx.lifecycle.ViewModelProvider;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import com.innopia.bist.ver2.R;
import com.innopia.bist.ver2.viewmodel.Test1ViewModel;

import java.util.ArrayList;
import java.util.List;

public class Test1Fragment extends Fragment {

    private static final String TAG = "Test1Fragment";

    private Test1ViewModel viewModel;

    // UI 컴포넌트
    private TextView titleText;
    private TextView infoText;
    private TextView statsAverage;
    private TextView statsMax;
    private TextView statsMin;
    private LineChart lineChart;
    private Button buttonRandom;
    private Button buttonSales;
    private ProgressBar progressBar;

    public static Test1Fragment newInstance() {
        return new Test1Fragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_test1, container, false);

        // UI 초기화
        initViews(root);

        // 차트 설정
        setupChart();

        // ⭐ 초기 포커스 설정 (약간의 딜레이 후)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                requestInitialFocus();
            }
        }, 100);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel 초기화
        viewModel = new ViewModelProvider(this).get(Test1ViewModel.class);

        // 옵저버 설정
        setupObservers();

        // 버튼 리스너 설정
        setupButtons();
    }

    private void initViews(View root) {
        titleText = root.findViewById(R.id.title_text);
        infoText = root.findViewById(R.id.info_text);
        statsAverage = root.findViewById(R.id.stats_average);
        statsMax = root.findViewById(R.id.stats_max);
        statsMin = root.findViewById(R.id.stats_min);
        lineChart = root.findViewById(R.id.line_chart);
        buttonRandom = root.findViewById(R.id.button_random);
        buttonSales = root.findViewById(R.id.button_sales);
        progressBar = root.findViewById(R.id.progress_bar);
    }

    /**
     * ⭐ 초기 포커스 요청 (Android TV용)
     */
    private void requestInitialFocus() {
        if (buttonRandom != null) {
            buttonRandom.requestFocus();
            Log.d(TAG, "Initial focus requested on button_random");
        }
    }

    private void setupChart() {
        // 차트 기본 설정
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        // Description 설정
        Description description = new Description();
        description.setText("Monthly Data");
        description.setTextSize(12f);
        lineChart.setDescription(description);

        // X축 설정
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < months.length) {
                    return months[index];
                }
                return "";
            }
        });

        // 왼쪽 Y축 설정
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextSize(10f);
        leftAxis.setDrawGridLines(true);

        // 오른쪽 Y축 비활성화
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        // 범례 설정
        lineChart.getLegend().setEnabled(true);
        lineChart.getLegend().setTextSize(12f);
    }

    private void setupObservers() {
        // 차트 데이터 observe
        viewModel.getChartData().observe(getViewLifecycleOwner(), data -> {
            if (data != null && !data.isEmpty()) {
                Log.d(TAG, "Chart data updated: " + data.size() + " points");
                updateChart(data);
            }
        });

        // 정보 텍스트 observe
        viewModel.getInfoText().observe(getViewLifecycleOwner(), text -> {
            if (text != null) {
                infoText.setText(text);
            }
        });

        // 통계 데이터 observe
        viewModel.getStatsData().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                statsAverage.setText(String.format("%.1f", stats.average));
                statsMax.setText(String.format("%.1f", stats.max));
                statsMin.setText(String.format("%.1f", stats.min));
            }
        });

        // 로딩 상태 observe
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            buttonRandom.setEnabled(!isLoading);
            buttonSales.setEnabled(!isLoading);
        });

        // 에러 메시지 observe
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupButtons() {
        buttonRandom.setOnClickListener(v -> {
            Log.d(TAG, "Random button clicked");
            viewModel.loadRandomData();
        });

        buttonSales.setOnClickListener(v -> {
            Log.d(TAG, "Sales button clicked");
            viewModel.loadSalesData();
        });

        // ⭐ 포커스 변경 리스너 (디버깅용)
        buttonRandom.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "button_random focus: " + hasFocus);
        });

        buttonSales.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "button_sales focus: " + hasFocus);
        });
    }

    private void updateChart(List<Float> data) {
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            entries.add(new Entry(i, data.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Data Values");

        // 라인 스타일 설정
        dataSet.setColor(Color.rgb(33, 150, 243)); // 파란색
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleColor(Color.rgb(33, 150, 243));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.rgb(33, 150, 243));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate(); // 차트 새로고침

        Log.d(TAG, "Chart updated with " + entries.size() + " entries");
    }
}
