package com.ik.innopia.hubist.main.wifi;
//package com.innopia.bist;

import android.app.Fragment;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.ik.innopia.hubist.R;
//import com.innopia.bist.R;

//public class WifiTestFragment extends Fragment {
public class WifiTestFragment3 extends Fragment implements WifiScanDialog3.WifiConnectionListener{

    private static final String DIALOG_TAG = "wifi_scan_dialog";
    private WifiTest3 wifiTest;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_wifi_test3, container, false);

        Button btnScan = rootView.findViewById(R.id.btn_wifi_scan);
        Button btnTest = rootView.findViewById(R.id.btn_wifi_test);

        wifiTest = new WifiTest3(getActivity());

        btnScan.setOnClickListener(v -> {
            wifiTest.startWifiScan();
            WifiScanDialog3 dialog = WifiScanDialog3.newInstance(wifiTest.wifiList);
            dialog.setWifiConnectionListener(this);
            dialog.show(getFragmentManager(), DIALOG_TAG);
        });

        btnTest.setOnClickListener(v -> {
        });

        return rootView;
    }

    @Override
    public void onConnectAttempt(ScanResult scanResult, String password) {
        Toast.makeText(getActivity(), scanResult.SSID + "에 연결합니다...", Toast.LENGTH_SHORT).show();

        wifiTest.connectToWifi(scanResult, password, new WifiTest3.ConnectionResultListener() {
            @Override
            public void onConnectionSuccess() {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), "연결 성공!", Toast.LENGTH_LONG).show();
                    dismissDialog();
                });
            }

            @Override
            public void onConnectionFailure(String error) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), "연결 실패: "+ error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void dismissDialog() {
        WifiScanDialog3 dialog = (WifiScanDialog3) getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if(dialog != null) {
            dialog.dismiss();
        }
    }
}
