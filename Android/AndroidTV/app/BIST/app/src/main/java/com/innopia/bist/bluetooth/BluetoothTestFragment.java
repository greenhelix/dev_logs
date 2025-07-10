package com.innopia.bist.bluetooth;

import android.app.Fragment;
import android.content.Intent;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.innopia.bist.ILogger;
import com.innopia.bist.MainActivity;
import com.innopia.bist.R;
import com.innopia.bist.wifi.WifiTest;


public class BluetoothTestFragment extends Fragment {

    private static final String TAG = "BIST_BT_FRAGMENT";
    private BluetoothTest bluetoothTest;
    private TextView tvBluetoothInfo;
    private Button btnBluetoothScan;
    private Button btnBluetoothTest;
    private ILogger logger;
    private Network currentNetwork; // 현재 연결된 네트워크 객체 저장

    public static BluetoothTestFragment newInstance(ILogger logger) {
        BluetoothTestFragment fragment = new com.innopia.bist.bluetooth.BluetoothTestFragment();
        fragment.setLogger(logger);
        return fragment;
    }

    public void setLogger(ILogger logger) {
        this.logger = logger;
    }

    private void log(String message) {
        if (logger != null) {
            logger.log(TAG, message);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bluetooth_test, container, false);
        log("onCreateView called. Initializing Wi-Fi Test Fragment.");

        // MainActivity로부터 BluetoothTest 인스턴스 가져오기
        if (getActivity() instanceof MainActivity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                this.bluetoothTest = ((MainActivity) getActivity()).getBluetoothTest();
            }
        }

        tvBluetoothInfo = rootView.findViewById(R.id.text_bluetooth_info);
        btnBluetoothScan = rootView.findViewById(R.id.btn_bluetooth_scan);
        btnBluetoothTest = rootView.findViewById(R.id.btn_bluetooth_test);

        // BIST_RENEWAL 1: 'Bluetooth Scan' 버튼 -> 시스템 Wi-Fi 설정 화면 열기
        btnBluetoothScan.setOnClickListener(v -> {
            log("Scan button clicked. Opening system Bluetoothi settings...");
            Toast.makeText(getActivity(), "Opening Bluetooth Settings...", Toast.LENGTH_SHORT).show();
            // 안드로이드 시스템의 Bluetooth 설정 화면을 여는 Intent
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        });

        // BIST_RENEWAL 3: 'Bluetooth Test' 버튼 -> 현재 연결된 Bluetooth 에 Ping 테스트 실행
        btnBluetoothTest.setOnClickListener(v -> {
            if (currentNetwork != null) {
                log("Bluetooth Test button clicked. Starting BT test...");
//                btnBluetoothTest.runBtTest();
            } else {
                log("Bluetooth Test button clicked, but not connected to Bluetooth.");
                Toast.makeText(getActivity(), "Please connect to a Bluetoothv network first.", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    /**
     * BIST_RENEWAL 2: 프래그먼트가 화면에 다시 나타날 때마다 호출 (핵심 로직)
     * 사용자가 bluetooth 설정에서 연결을 마치고 돌아오면 이 메서드가 실행됩니다.
     */
    @Override
    public void onResume() {
        super.onResume();
        log("Fragment resumed. Checking for bluetooth connection status update.");
        updateConnectionStatus();
    }

    /**
     * BIST_RENEWAL 4: Wi-Fi 연결 상태를 확인하고 UI를 업데이트하는 메서드
     */
    private void updateConnectionStatus() {
        if (bluetoothTest == null) {
            log("WifiTest helper is null. Cannot update status.");
            return;
        }

        tvBluetoothInfo.setText("Checking connection status...");

        bluetoothTest.checkCurrentConnection((info, network, isConnected) -> {
            // 콜백을 통해 UI 업데이트
            tvBluetoothInfo.setText(info);
            currentNetwork = network;

            // 메인 액티비티의 상단 아이콘 업데이트
            if (getActivity() instanceof MainActivity) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ((MainActivity) getActivity()).updateWifiIcon(isConnected);
                }
            }
        });
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 화면 로드 시 'Wi-Fi Scan' 버튼에 기본 포커스 설정
        if (btnBluetoothScan != null) {
            btnBluetoothScan.requestFocus();
        }
    }
}
