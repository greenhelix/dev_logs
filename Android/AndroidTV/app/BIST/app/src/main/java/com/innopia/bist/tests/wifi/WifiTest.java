package com.innopia.bist.tests.wifi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.innopia.bist.util.ILogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WifiTest {

    private static final String TAG = "BIST_WIFI_TEST";
    private final WifiManager mWifiManager;
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private final ILogger mLogger;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * BIST_RENEWAL: 현재 Wi-Fi 연결 상태 정보를 전달하기 위한 콜백 인터페이스
     */
    public interface ConnectionInfoListener {
        void onInfoUpdated(String info, Network network, boolean isConnected);
    }

    /**
     * BIST_RENEWAL: Ping 테스트 결과를 전달하기 위한 콜백 인터페이스
     */
    public interface PingResultListener {
        void onPingLog(String log);
        void onPingFinished(String summary);
    }

    public WifiTest(Context context, ILogger logger) {
        mContext = context;
        mLogger = logger;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private void log(String message) {
        Log.i(TAG, message);
        if (mLogger != null) {
            mLogger.log(TAG, message);
        }
    }

    /**
     * BIST_RENEWAL: 현재 Wi-Fi 연결 상태를 확인하고 정보를 반환하는 핵심 메서드
     * 이 메서드는 Fragment의 onResume 등에서 호출됩니다.
     */
    public void checkCurrentConnection(ConnectionInfoListener listener) {
        log("Checking current Wi-Fi connection status...");
        executor.execute(() -> {
            Network activeNetwork = mConnectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                mainThreadHandler.post(() -> listener.onInfoUpdated("Wi-Fi is not connected.", null, false));
                return;
            }

            NetworkCapabilities caps = mConnectivityManager.getNetworkCapabilities(activeNetwork);
            if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                mainThreadHandler.post(() -> listener.onInfoUpdated("Current active network is not Wi-Fi.", null, false));
                return;
            }

            // Wi-Fi 정보 가져오기
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID().replace("\"", "");
            String bssid = wifiInfo.getBSSID();
            int rssi = wifiInfo.getRssi();
            int linkSpeed = wifiInfo.getLinkSpeed();

            // 인터넷 유효성 검사
            boolean isValidated = isInternetAvailable(activeNetwork);

            // 결과 문자열 생성
            String info = "Status: " + (isValidated ? "Connected (Internet OK)" : "Connected (No Internet)") + "\n" +
                    "SSID: " + ssid + "\n" +
                    "BSSID: " + bssid + "\n" +
                    "Signal Strength (RSSI): " + rssi + " dBm\n" +
                    "Link Speed: " + linkSpeed + " Mbps";

            log("Connection check finished. SSID: " + ssid + ", Internet available: " + isValidated);
            mainThreadHandler.post(() -> listener.onInfoUpdated(info, activeNetwork, true));
        });
    }

    /**
     * BIST_RENEWAL: 인터넷 연결 유효성을 검사하는 private 헬퍼 메서드
     */
    private boolean isInternetAvailable(Network network) {
        NetworkCapabilities caps = mConnectivityManager.getNetworkCapabilities(network);
        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            log("Network is not validated. May have no internet or a captive portal.");
            return false;
        }

        try {
            URL url = new URL("https://clients3.google.com/generate_204");
            HttpURLConnection urlConnection = (HttpURLConnection) network.openConnection(url);
            urlConnection.setConnectTimeout(3000);
            urlConnection.connect();
            int responseCode = urlConnection.getResponseCode();
            urlConnection.disconnect();
            log("Internet validation check: server returned " + responseCode);
            return responseCode == 204;
        } catch (IOException e) {
            log("Internet validation check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * BIST_RENEWAL: 연결된 Wi-Fi에 Ping 테스트를 실행하는 메서드
     */
    public void runPingTest(Network network, PingResultListener listener) {
        if (network == null) {
            log("Ping Test Error: Not connected to any Wi-Fi network.");
            listener.onPingFinished("Ping Test Error: Not connected.");
            return;
        }

        executor.execute(() -> {
            log("Starting ping test to 8.8.8.8...");
            mainThreadHandler.post(() -> listener.onPingLog("Pinging 8.8.8.8..."));
            try {
                Process process = Runtime.getRuntime().exec("ping -c 5 8.8.8.8");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    final String finalLine = line;
                    mainThreadHandler.post(() -> listener.onPingLog(finalLine));
                }

                process.waitFor();
                reader.close();

                if (process.exitValue() == 0) {
                    mainThreadHandler.post(() -> listener.onPingFinished("Ping test successful."));
                } else {
                    mainThreadHandler.post(() -> listener.onPingFinished("Ping test failed. Host might be unreachable."));
                }

            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Ping test failed", e);
                mainThreadHandler.post(() -> listener.onPingFinished("Ping Test Error: " + e.getMessage()));
            }
        });
    }

    public boolean checkWifiPermission() {
        return ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
