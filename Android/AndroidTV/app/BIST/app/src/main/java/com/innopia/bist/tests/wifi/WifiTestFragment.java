package com.innopia.bist.tests.wifi;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.innopia.bist.R;
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.WifiTestViewModel;

public class WifiTestFragment extends Fragment {

    private WifiTestViewModel wifiTestViewModel;
    private MainViewModel mainViewModel;
    private TextView textWifiInfo;
    private ActivityResultLauncher<Intent> wifiSettingsLauncher;

    public static WifiTestFragment newInstance() {
        return new WifiTestFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wifiSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 설정 화면에서 돌아왔을 때 로그만 남깁니다.
                    // 실제 정보 업데이트는 onResume()에서 처리하여 일관성을 유지합니다.
                    mainViewModel.appendLog("Returned from Wi-Fi settings.");
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wifi_test, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        wifiTestViewModel = new ViewModelProvider(this).get(WifiTestViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        textWifiInfo = view.findViewById(R.id.text_wifi_info);
        Button btnScan = view.findViewById(R.id.btn_wifi_scan);
        Button btnTest = view.findViewById(R.id.btn_wifi_test);
        Button btnAutoTest = view.findViewById(R.id.btn_wifi_auto_test);

        btnScan.setOnClickListener(v -> {
            mainViewModel.appendLog("Opening Wi-Fi settings...");
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            wifiSettingsLauncher.launch(intent);
        });

        btnTest.setOnClickListener(v -> wifiTestViewModel.startPingTest(mainViewModel::appendLog, mainViewModel::updateWifiStatus));
        btnAutoTest.setOnClickListener(v -> wifiTestViewModel.startAutoTest(mainViewModel::appendLog, mainViewModel::updateWifiStatus));

        // ViewModel의 displayInfo LiveData를 관찰하여 TextView를 업데이트합니다.
//        wifiTestViewModel.displayInfo.observe(getViewLifecycleOwner(), info -> {
//            textWifiInfo.setText(info);
//        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // 요구사항의 핵심: 프래그먼트가 화면에 보일 때마다 Wi-Fi 정보를 갱신합니다.
        // 이렇게 하면 설정에서 돌아왔을 때, 또는 이미 연결된 상태에서 이 화면에 진입했을 때
        // 항상 최신 정보를 표시할 수 있습니다.
        if (getContext() != null) {
            wifiTestViewModel.updateWifiInfo(requireContext());
        }
    }
}
