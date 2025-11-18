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
import com.innopia.bist.ver2.data.repository.Test3Repository;
import com.innopia.bist.ver2.viewmodel.Test3ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Test3Fragment - 네트워크 속도 측정 (그래프 포함)
 */
public class Test3Fragment extends Fragment {

    private static final String TAG = "Test3Fragment";
    private Test3ViewModel viewModel;

    // UI 컴포넌트
    private TextView titleText;
    private TextView statusText;
    private ProgressBar progressBar;
    private TextView progressText;
    private LineChart speedChart;

    // 다운로드 테스트 UI
    private Button buttonDownloadTest;
    private TextView downloadSpeedValue;
    private TextView downloadBytesValue;
    private TextView downloadTimeValue;

    // 업로드 테스트 UI
    private Button buttonUploadTest;
    private TextView uploadSpeedValue;
    private TextView uploadBytesValue;
    private TextView uploadTimeValue;

    // Ping 테스트 UI
    private Button buttonPingTest;
    private TextView pingAverageValue;
    private TextView pingSuccessValue;
    private TextView pingAttemptsValue;

    // 연결 정보
    private TextView connectionTypeValue;

    // 전체 테스트 버튼
    private Button buttonStopTest;

    public static Test3Fragment newInstance() {
        return new Test3Fragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_test3, container, false);

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
        viewModel = new ViewModelProvider(this).get(Test3ViewModel.class);

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
        speedChart = root.findViewById(R.id.speed_chart);

        // 다운로드 테스트 UI
        buttonDownloadTest = root.findViewById(R.id.button_download_test);
        downloadSpeedValue = root.findViewById(R.id.download_speed_value);
        downloadBytesValue = root.findViewById(R.id.download_bytes_value);
        downloadTimeValue = root.findViewById(R.id.download_time_value);

        // 업로드 테스트 UI
        buttonUploadTest = root.findViewById(R.id.button_upload_test);
        uploadSpeedValue = root.findViewById(R.id.upload_speed_value);
        uploadBytesValue = root.findViewById(R.id.upload_bytes_value);
        uploadTimeValue = root.findViewById(R.id.upload_time_value);

        // Ping 테스트 UI
        buttonPingTest = root.findViewById(R.id.button_ping_test);
        pingAverageValue = root.findViewById(R.id.ping_average_value);
        pingSuccessValue = root.findViewById(R.id.ping_success_value);
        pingAttemptsValue = root.findViewById(R.id.ping_attempts_value);

        connectionTypeValue = root.findViewById(R.id.connection_type_value);
        buttonStopTest = root.findViewById(R.id.button_stop_test);
    }

    private void setupChart() {
        // 차트 기본 설정
        speedChart.setTouchEnabled(true);
        speedChart.setDragEnabled(true);
        speedChart.setScaleEnabled(true);
        speedChart.setPinchZoom(true);
        speedChart.setDrawGridBackground(false);
        speedChart.setBackgroundColor(Color.parseColor("#1a1a1a"));

        // Description 설정
        Description desc = new Description();
        desc.setText("Network Speed Over Time");
        desc.setTextColor(Color.WHITE);
        desc.setTextSize(12f);
        speedChart.setDescription(desc);

        // Legend 설정
        speedChart.getLegend().setEnabled(true);
        speedChart.getLegend().setTextColor(Color.WHITE);
        speedChart.getLegend().setTextSize(12f);

        // X축 설정
        XAxis xAxis = speedChart.getXAxis();
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
        YAxis leftAxis = speedChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#333333"));
        leftAxis.setAxisMinimum(0f);

        // Y축 (오른쪽) 비활성화
        speedChart.getAxisRight().setEnabled(false);

        // 빈 데이터로 초기화
        speedChart.setData(new LineData());
        speedChart.invalidate();
    }

    private void requestInitialFocus() {
        if (buttonDownloadTest != null) {
            buttonDownloadTest.requestFocus();
            Log.d(TAG, "Initial focus requested on button_download_test");
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

        // 다운로드 테스트 결과 observe
        viewModel.getDownloadResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Log.d(TAG, "Download test result received");
                updateDownloadResults(result);
            }
        });

        // 업로드 테스트 결과 observe
        viewModel.getUploadResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Log.d(TAG, "Upload test result received");
                updateUploadResults(result);
            }
        });

        // Ping 테스트 결과 observe
        viewModel.getPingResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Log.d(TAG, "Ping test result received");
                updatePingResults(result);
            }
        });

        // 로딩 상태 observe
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            buttonDownloadTest.setEnabled(!isLoading);
            buttonUploadTest.setEnabled(!isLoading);
            buttonPingTest.setEnabled(!isLoading);
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
        buttonDownloadTest.setOnClickListener(v -> {
            Log.d(TAG, "Download test button clicked");
            resetDownloadResults();
            speedChart.clear();
            viewModel.startDownloadTest();
        });

        buttonUploadTest.setOnClickListener(v -> {
            Log.d(TAG, "Upload test button clicked");
            resetUploadResults();
            speedChart.clear();
            viewModel.startUploadTest();
        });

        buttonPingTest.setOnClickListener(v -> {
            Log.d(TAG, "Ping test button clicked");
            resetPingResults();
            speedChart.clear();
            viewModel.startPingTest();
        });

        buttonStopTest.setOnClickListener(v -> {
            Log.d(TAG, "Stop test button clicked");
            viewModel.stopTest();
        });

        // 포커스 변경 리스너
        buttonDownloadTest.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "button_download_test focus: " + hasFocus);
        });

        buttonUploadTest.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "button_upload_test focus: " + hasFocus);
        });

        buttonPingTest.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "button_ping_test focus: " + hasFocus);
        });
    }

    private void updateChart(List<Float> data) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new Entry(i, data.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Speed");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#2196F3"));
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        speedChart.setData(lineData);
        speedChart.notifyDataSetChanged();
        speedChart.invalidate();
    }

    private void updateDownloadResults(Test3Repository.DownloadTestResult result) {
        downloadSpeedValue.setText(String.format("%.2f Mbps", result.downloadSpeed));
        downloadBytesValue.setText(formatBytes(result.totalBytes));
        downloadTimeValue.setText(String.format("%.2f sec", result.totalTimeSeconds));
        connectionTypeValue.setText(result.connectionType);

        // 속도에 따른 색상 변경
        setSpeedColor(downloadSpeedValue, result.downloadSpeed);
    }

    private void updateUploadResults(Test3Repository.UploadTestResult result) {
        uploadSpeedValue.setText(String.format("%.2f Mbps", result.uploadSpeed));
        uploadBytesValue.setText(formatBytes(result.totalBytes));
        uploadTimeValue.setText(String.format("%.2f sec", result.totalTimeSeconds));
        connectionTypeValue.setText(result.connectionType);

        // 속도에 따른 색상 변경
        setSpeedColor(uploadSpeedValue, result.uploadSpeed);
    }

    private void updatePingResults(Test3Repository.PingTestResult result) {
        pingAverageValue.setText(String.format("%d ms", result.averagePing));
        pingSuccessValue.setText(String.valueOf(result.successCount));
        pingAttemptsValue.setText(String.valueOf(result.totalAttempts));
        connectionTypeValue.setText(result.connectionType);

        // Ping에 따른 색상 변경
        if (result.averagePing < 50) {
            pingAverageValue.setTextColor(Color.GREEN);
        } else if (result.averagePing < 100) {
            pingAverageValue.setTextColor(Color.YELLOW);
        } else {
            pingAverageValue.setTextColor(Color.RED);
        }
    }

    private void setSpeedColor(TextView textView, double speedMbps) {
        if (speedMbps > 50) {
            textView.setTextColor(Color.GREEN);
        } else if (speedMbps > 10) {
            textView.setTextColor(Color.YELLOW);
        } else {
            textView.setTextColor(Color.RED);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }

    private void resetDownloadResults() {
        downloadSpeedValue.setText("--");
        downloadBytesValue.setText("--");
        downloadTimeValue.setText("--");
        downloadSpeedValue.setTextColor(Color.WHITE);
    }

    private void resetUploadResults() {
        uploadSpeedValue.setText("--");
        uploadBytesValue.setText("--");
        uploadTimeValue.setText("--");
        uploadSpeedValue.setTextColor(Color.WHITE);
    }

    private void resetPingResults() {
        pingAverageValue.setText("--");
        pingSuccessValue.setText("--");
        pingAttemptsValue.setText("--");
        pingAverageValue.setTextColor(Color.WHITE);
    }
}
