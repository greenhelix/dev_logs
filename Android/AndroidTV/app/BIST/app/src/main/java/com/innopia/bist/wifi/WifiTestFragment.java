package com.innopia.bist.wifi;

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

public class WifiTestFragment extends Fragment {

    private static final String TAG = "BIST_WIFI_FRAGMENT";
    private WifiTest wifiTest;
    private TextView tvWifiInfo;
    private Button btnWifiScan;
    private Button btnWifiTest;
    private ILogger logger;
    private Network currentNetwork; // 현재 연결된 네트워크 객체 저장

    public static WifiTestFragment newInstance(ILogger logger) {
        WifiTestFragment fragment = new WifiTestFragment();
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
        View rootView = inflater.inflate(R.layout.fragment_wifi_test, container, false);
        log("onCreateView called. Initializing Wi-Fi Test Fragment.");

        // MainActivity로부터 WifiTest 인스턴스 가져오기
        if (getActivity() instanceof MainActivity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                this.wifiTest = ((MainActivity) getActivity()).getWifiTest();
            }
        }

        tvWifiInfo = rootView.findViewById(R.id.text_wifi_info);
        btnWifiScan = rootView.findViewById(R.id.btn_wifi_scan);
        btnWifiTest = rootView.findViewById(R.id.btn_wifi_test);

        // BIST_RENEWAL 1: 'Wi-Fi Scan' 버튼 -> 시스템 Wi-Fi 설정 화면 열기
        btnWifiScan.setOnClickListener(v -> {
            log("Scan button clicked. Opening system Wi-Fi settings...");
            Toast.makeText(getActivity(), "Opening Wi-Fi Settings...", Toast.LENGTH_SHORT).show();
            // 안드로이드 시스템의 Wi-Fi 설정 화면을 여는 Intent
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        });

        // BIST_RENEWAL 3: 'Wi-Fi Test' 버튼 -> 현재 연결된 Wi-Fi에 Ping 테스트 실행
        btnWifiTest.setOnClickListener(v -> {
            if (currentNetwork != null) {
                log("Wi-Fi Test button clicked. Starting ping test...");
                tvWifiInfo.setText("Running Ping Test...");
                wifiTest.runPingTest(currentNetwork, new WifiTest.PingResultListener() {
                    @Override
                    public void onPingLog(String logMessage) {
                        log(logMessage); // 핑 로그를 메인 로그창에 출력
                    }

                    @Override
                    public void onPingFinished(String summary) {
                        log("Ping Test Finished: " + summary);
                        Toast.makeText(getActivity(), summary, Toast.LENGTH_LONG).show();
                        // 테스트가 끝나면 다시 현재 상태를 표시
                        updateConnectionStatus();
                    }
                });
            } else {
                log("Wi-Fi Test button clicked, but not connected to Wi-Fi.");
                Toast.makeText(getActivity(), "Please connect to a Wi-Fi network first.", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    /**
     * BIST_RENEWAL 2: 프래그먼트가 화면에 다시 나타날 때마다 호출 (핵심 로직)
     * 사용자가 Wi-Fi 설정에서 연결을 마치고 돌아오면 이 메서드가 실행됩니다.
     */
    @Override
    public void onResume() {
        super.onResume();
        log("Fragment resumed. Checking for Wi-Fi connection status update.");
        updateConnectionStatus();
    }

    /**
     * BIST_RENEWAL 4: Wi-Fi 연결 상태를 확인하고 UI를 업데이트하는 메서드
     */
    private void updateConnectionStatus() {
        if (wifiTest == null) {
            log("WifiTest helper is null. Cannot update status.");
            return;
        }

        tvWifiInfo.setText("Checking connection status...");

        wifiTest.checkCurrentConnection((info, network, isConnected) -> {
            // 콜백을 통해 UI 업데이트
            tvWifiInfo.setText(info);
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
        if (btnWifiScan != null) {
            btnWifiScan.requestFocus();
        }
    }
}
