package com.innopia.bist.tests.wifi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.R;
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.WifiTestViewModel;

public class WifiTestFragment extends Fragment {

    private WifiTestViewModel wifiTestViewModel;
    private MainViewModel mainViewModel; // Activity와 공유
    private TextView textWifiInfo;

    public static WifiTestFragment newInstance() {
        return new WifiTestFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wifi_test, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Fragment 자신의 ViewModel
        wifiTestViewModel = new ViewModelProvider(this).get(WifiTestViewModel.class);
        // Activity 범위의 ViewModel
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        textWifiInfo = view.findViewById(R.id.text_wifi_info);
        Button btnScan = view.findViewById(R.id.btn_wifi_scan);
        Button btnTest = view.findViewById(R.id.btn_wifi_test);

        // 버튼 클릭 시 ViewModel의 함수 호출
        btnScan.setOnClickListener(v -> {
            wifiTestViewModel.startScan(mainViewModel::appendLog);
        });

        btnTest.setOnClickListener(v -> {
            wifiTestViewModel.startTest(mainViewModel::appendLog, mainViewModel::updateWifiStatus);
        });

        // ViewModel의 LiveData를 관찰하여 UI 업데이트
        wifiTestViewModel.testInfo.observe(getViewLifecycleOwner(), info -> {
            textWifiInfo.setText(info);
        });
    }
}
