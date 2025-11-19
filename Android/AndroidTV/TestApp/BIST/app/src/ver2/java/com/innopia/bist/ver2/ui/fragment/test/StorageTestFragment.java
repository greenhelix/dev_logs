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

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.innopia.bist.ver2.R;
import com.innopia.bist.ver2.data.repository.StorageTestRepository;
import com.innopia.bist.ver2.viewmodel.StorageTestViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage Test Fragment
 */
public class StorageTestFragment extends Fragment {

    private static final String TAG = "StorageTestFragment";
    private StorageTestViewModel viewModel;

    private TextView titleText;
    private TextView statusText;
    private ProgressBar progressBar;
    private TextView progressText;
    private PieChart storageChart;
    private Button buttonStartTest;
    private Button buttonStopTest;

    private TextView storageTotalValue;
    private TextView storageUsedValue;
    private TextView storageFreeValue;
    private TextView storageStatusValue;
    private TextView externalTotalValue;
    private TextView externalUsedValue;
    private TextView externalFreeValue;

    public static StorageTestFragment newInstance() {
        return new StorageTestFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_storage_test, container, false);

        initViews(root);
        setupChart();

        new Handler(Looper.getMainLooper()).postDelayed(() -> requestInitialFocus(), 100);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StorageTestViewModel.class);
        setupObservers();
        setupButtons();
    }

    private void initViews(View root) {
        titleText = root.findViewById(R.id.title_text);
        statusText = root.findViewById(R.id.status_text);
        progressBar = root.findViewById(R.id.progress_bar);
        progressText = root.findViewById(R.id.progress_text);
        storageChart = root.findViewById(R.id.storage_chart);
        buttonStartTest = root.findViewById(R.id.button_start_test);
        buttonStopTest = root.findViewById(R.id.button_stop_test);

        storageTotalValue = root.findViewById(R.id.storage_total_value);
        storageUsedValue = root.findViewById(R.id.storage_used_value);
        storageFreeValue = root.findViewById(R.id.storage_free_value);
        storageStatusValue = root.findViewById(R.id.storage_status_value);
        externalTotalValue = root.findViewById(R.id.external_total_value);
        externalUsedValue = root.findViewById(R.id.external_used_value);
        externalFreeValue = root.findViewById(R.id.external_free_value);
    }

    private void setupChart() {
        storageChart.setUsePercentValues(true);
        storageChart.setDrawHoleEnabled(true);
        storageChart.setHoleColor(Color.parseColor("#1a1a1a"));
        storageChart.setTransparentCircleRadius(61f);
        storageChart.setDrawCenterText(true);
        storageChart.setCenterText("Internal\nStorage");
        storageChart.setCenterTextColor(Color.WHITE);
        storageChart.setRotationEnabled(true);

        Description desc = new Description();
        desc.setText("");
        storageChart.setDescription(desc);

        Legend legend = storageChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(Color.WHITE);
        legend.setTextSize(12f);
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
                updateChart(result);
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
            storageChart.clear();
            viewModel.startStorageTest();
        });

        buttonStopTest.setOnClickListener(v -> {
            // Storage test는 즉시 완료되므로 stop 불필요
        });
    }

    private void updateChart(StorageTestRepository.StorageTestResult result) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(result.usedSpace, "Used"));
        entries.add(new PieEntry(result.freeSpace, "Free"));

        PieDataSet dataSet = new PieDataSet(entries, "Storage Distribution");

        List<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#FF5252")); // Used - Red
        colors.add(Color.parseColor("#4CAF50")); // Free - Green
        dataSet.setColors(colors);

        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        storageChart.setData(data);
        storageChart.invalidate();
    }

    private void updateResults(StorageTestRepository.StorageTestResult result) {
        storageTotalValue.setText(String.format("%d GB", result.totalSpace));
        storageUsedValue.setText(String.format("%d GB", result.usedSpace));
        storageFreeValue.setText(String.format("%d GB", result.freeSpace));
        storageStatusValue.setText(result.status);
        storageStatusValue.setTextColor(Color.parseColor(result.statusColor));

        if (result.hasExternalStorage) {
            externalTotalValue.setText(String.format("%d GB", result.externalTotalSpace));
            externalUsedValue.setText(String.format("%d GB", result.externalUsedSpace));
            externalFreeValue.setText(String.format("%d GB", result.externalFreeSpace));
        } else {
            externalTotalValue.setText("N/A");
            externalUsedValue.setText("N/A");
            externalFreeValue.setText("N/A");
        }
    }

    private void resetResults() {
        storageTotalValue.setText("--");
        storageUsedValue.setText("--");
        storageFreeValue.setText("--");
        storageStatusValue.setText("--");
        storageStatusValue.setTextColor(Color.WHITE);
        externalTotalValue.setText("--");
        externalUsedValue.setText("--");
        externalFreeValue.setText("--");
    }
}
