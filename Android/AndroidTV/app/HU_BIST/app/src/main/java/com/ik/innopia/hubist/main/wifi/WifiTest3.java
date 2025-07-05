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

//public class WifiTest {
public class WifiTest3 {

    private static final String TAG = "BIST";

    private final WifiManager mWifiManager;
    public Context mContext;
    public List<ScanResult> wifiList = new ArrayList<>();
    public interface ConnectionResultListener {
        void onConnectionSuccess();
        void onConnectionFailure(String error);
    }

    // 생성자
    public WifiTest3(Context context) {
        mContext = context;
        mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        // WifiInfo wifiInfo = mWifiManager.getConnectionInfo(); // 이 줄은 현재 사용되지 않으므로 필요에 따라 제거하거나 사용하십시오.
    }

    /**
     * Wi-Fi 권한이 있는지 확인 합니다.
     * @return 권한이 있으면 true, 없으면 false
     */
    public boolean checkWifiPermission() {
        boolean hasPermission = ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!hasPermission) {
            Log.d(TAG, "Wi-Fi 권한이 없습니다. ACCESS_FINE_LOCATION 필요.");
        }
        return hasPermission;
    }

    /**
     * Wi-Fi 스캔을 시작 합니다.
     * 권한이 없으면 스캔을 시작하지 않습니다.
     */
    public void startWifiScan() {
        Log.d(TAG,"Wi-Fi 스캔 시작 요청.");
        if(checkWifiPermission()){
            wifiList = mWifiManager.getScanResults();
            Log.d(TAG,"Wi-Fi 스캔 결과 수: " + wifiList.size());
            checkWifiScanList(true);  // Wi-Fi 목록을 로그로 확인 (true: 켜기, false: 끄기)
        } else {
            Log.d(TAG,"Wi-Fi 스캔을 시작할 수 없습니다: 권한 없음.");
        }
    }

    /**
     * Wi-Fi 스캔 목록을 로그로 확인 합니다.
     * @param on_off true면 로그를 출력하고, false면 출력하지 않습니다.
     */
    public void checkWifiScanList(boolean on_off){
        if(on_off) {
            Log.d(TAG,"Wi-Fi 목록 =======================");
            if (wifiList.isEmpty()) {
                Log.d(TAG,"스캔된 Wi-Fi 네트워크가 없습니다.");
            } else {
                for (int i = 0; i < wifiList.size(); i++) {
                    if(wifiList.get(i).SSID.isEmpty()){
                        wifiList.remove(i);
                    }
                    Log.d(TAG,(i + 1) + " : " + wifiList.get(i).SSID);
                }
            }
            Log.d(TAG,"Wi-Fi 목록 끝 =======================");
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
                    Log.d(TAG, "Wi-Fi onAvailable: 연결 성공");
                    if (listener != null) listener.onConnectionSuccess();
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    Log.d(TAG, "Wi-Fi onUnavailable: 연결 실패");
                    if (listener != null) listener.onConnectionFailure("비밀번호가 틀리거나 연결할 수 없습니다.");
                }

                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                    Log.e(TAG, "[WifiTest3] Wi-Fi onLost: 네트워크 연결이 끊어졌습니다. SSID: " + scanResult.SSID);
                    // 필요 시 실패 처리
                }
            });
        } else {
            // Q 이전 버전에 대한 연결 로직 (필요 시 구현)
            Log.w(TAG, "[WifiTest3] Android Q 미만 버전에서는 지원되지 않는 연결 방식입니다.");
            if (listener != null) listener.onConnectionFailure("지원되지 않는 OS 버전입니다.");
        }
    }
}
