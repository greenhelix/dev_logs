package com.ik.innopia.hubist.main.wifi;

import android.app.Fragment;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ik.innopia.hubist.R;
public class WifiTestFragment3 extends Fragment {

    private WifiTest3 wifiTest;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_wifi_test3, container, false);

        Button btnScan = rootView.findViewById(R.id.btn_wifi_scan);
        Button btnTest = rootView.findViewById(R.id.btn_wifi_test);

        wifiTest = new WifiTest3(getActivity());

        btnScan.setOnClickListener(v -> {
            // Wi-Fi 스캔 시작
            wifiTest.startWifiScan();

            // 스캔 결과 다이얼로그 띄우기
            WifiScanDialog3 dialog = WifiScanDialog3.newInstance(wifiTest.wifiList);
            dialog.show(getFragmentManager(), "wifi_scan_dialog");
        });

        btnTest.setOnClickListener(v -> {
            // Wi-Fi 테스트 로직 호출 (필요시 구현)
            // 예: wifiTest.startWifiTest();
        });

        return rootView;
    }
}
