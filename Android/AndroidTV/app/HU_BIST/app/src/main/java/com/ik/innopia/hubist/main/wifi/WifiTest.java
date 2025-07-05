package com.ik.innopia.hubist.main.wifi;
//package com.innopia.bist;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.ScanResult;

import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class WifiTest {

    private static final String TAG = "BIST";

    private final WifiManager mWifiManager;
    public Context mContext;
    public List<ScanResult> wifiList = new ArrayList<>();
    public interface ConnectionResultListener {
        void onConnectionSuccess();
        void onConnectionFailure(String error);
    }

    public WifiTest(Context context) {
        mContext = context;
        mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
    }

    /**
     * Wi-Fi has a permission check
     * @return having permission is true, or not false
     */
    public boolean checkWifiPermission() {
        boolean hasPermission = ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!hasPermission) {
            Log.d(TAG, "Wi-Fi no persmission. ACCESS_FINE_LOCATION needed");
        }
        return hasPermission;
    }

    /**
     * Wi-Fi list scanning is started
     * if not has permisison, this function not work
     */
    public void startWifiScan() {
        Log.d(TAG,"Wi-Fi scan start");
        if(checkWifiPermission()){
            wifiList = mWifiManager.getScanResults();
            Log.d(TAG,"Wi-Fi scan results: " + wifiList.size());
            checkWifiScanList(true);
        } else {
            Log.d(TAG,"Wi-Fi scanning wasnt start: no permission.");
        }
    }

    /**
     * Wi-Fi 스캔 목록을 로그로 확인 합니다.
     * @param on_off true면 로그를 출력하고, false면 출력하지 않습니다.
     */
    public void checkWifiScanList(boolean on_off){
        if(on_off) {
            Log.d(TAG,"Wi-Fi list =======================");
            if (wifiList.isEmpty()) {
                Log.d(TAG,"wifi list was empty, no results");
            } else {
                for (int i = 0; i < wifiList.size(); i++) {
                    if(wifiList.get(i).SSID.isEmpty()){
                        wifiList.remove(i);
                    }
                    Log.d(TAG,(i + 1) + " : " + wifiList.get(i).SSID);
                }
            }
            Log.d(TAG,"Wi-Fi list end =======================");
        }
    }

    public void connectToWifi(ScanResult scanResult, String password, ConnectionResultListener listener) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(scanResult.SSID)
                    .setWpa2Passphrase(password)
                    .build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier)
                    .build();

            ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    connectivityManager.bindProcessToNetwork(network);
                    Log.d(TAG, "Wi-Fi onAvailable: connection success");
                    if (listener != null) listener.onConnectionSuccess();
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    Log.d(TAG, "Wi-Fi onUnavailable: connection fail");
                    if (listener != null) listener.onConnectionFailure("wrong password, or fail connection");
                }

                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                    Log.e(TAG, "[WifiTest] Wi-Fi onLost: network was disconnected, SSID: " + scanResult.SSID);
                    // 필요 시 실패 처리
                }
            });
        } else {
            // Q 이전 버전에 대한 연결 로직 (필요 시 구현)
            Log.w(TAG, "[WifiTest] under Android Q, this conn not supported");
            if (listener != null) listener.onConnectionFailure("not supported os version");
        }
    }
}
