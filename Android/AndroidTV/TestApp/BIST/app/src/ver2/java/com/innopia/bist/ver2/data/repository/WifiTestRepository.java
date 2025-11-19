package com.innopia.bist.ver2.data.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.innopia.bist.ver2.util.TestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * WiFi 테스트 Repository
 */
public class WifiTestRepository implements Test {

    private static final String TAG = "WifiTestRepository";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TestStatus currentStatus = TestStatus.IDLE;
    private boolean isTestRunning = false;

    // 샘플 데이터 사용 여부
    private boolean useSampleData = true;

    public WifiTestRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setUseSampleData(boolean useSampleData) {
        this.useSampleData = useSampleData;
    }

    /**
     * WiFi 테스트
     */
    public void testWifi(WifiTestCallback callback) {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;

        new Thread(() -> {
            try {
                handler.post(() -> callback.onTestProgress(10, "Checking WiFi..."));

                WifiTestResult result;

                if (useSampleData) {
                    result = getSampleWifiData(callback);
                } else {
                    result = getRealWifiData(callback);
                }

                currentStatus = TestStatus.COMPLETED;
                handler.post(() -> callback.onWifiTestCompleted(result));

            } catch (Exception e) {
                Log.e(TAG, "WiFi test error", e);
                currentStatus = TestStatus.ERROR;
                handler.post(() -> callback.onError("WiFi test failed: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * 실제 WiFi 데이터 가져오기
     */
    private WifiTestResult getRealWifiData(WifiTestCallback callback) {
        WifiTestResult result = new WifiTestResult();

        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);

            if (wifiManager == null) {
                result.isWifiAvailable = false;
                result.errorMessage = "WiFi not available";
                return result;
            }

            result.isWifiAvailable = true;
            result.isWifiEnabled = wifiManager.isWifiEnabled();

            if (!result.isWifiEnabled) {
                result.errorMessage = "WiFi is disabled";
                return result;
            }

            handler.post(() -> callback.onTestProgress(30, "Getting WiFi info..."));

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if (wifiInfo != null) {
                result.ssid = wifiInfo.getSSID().replace("\"", "");
                result.linkSpeed = wifiInfo.getLinkSpeed(); // Mbps
                result.rssi = wifiInfo.getRssi(); // dBm
                result.frequency = wifiInfo.getFrequency(); // MHz
                result.isConnected = true;

                // 신호 강도를 퍼센트로 변환
                result.signalLevel = WifiManager.calculateSignalLevel(result.rssi, 5);
            }

            handler.post(() -> callback.onTestProgress(60, "Monitoring signal..."));

            // 신호 강도 모니터링
            result.signalStrengthData = monitorWifiSignal(wifiManager);
            result.linkSpeedData = monitorLinkSpeed(wifiInfo);

        } catch (SecurityException e) {
            result.errorMessage = "WiFi permission denied";
            Log.e(TAG, "WiFi permission error", e);
        } catch (Exception e) {
            result.errorMessage = "WiFi test failed: " + e.getMessage();
            Log.e(TAG, "WiFi test error", e);
        }

        return result;
    }

    /**
     * 샘플 WiFi 데이터
     */
    private WifiTestResult getSampleWifiData(WifiTestCallback callback) {
        WifiTestResult result = new WifiTestResult();

        handler.post(() -> callback.onTestProgress(30, "Getting WiFi info..."));
        sleep(300);

        result.isWifiAvailable = true;
        result.isWifiEnabled = true;
        result.isConnected = true;
        result.ssid = "Sample_WiFi_5G";
        result.linkSpeed = 866; // Mbps
        result.rssi = -45; // dBm
        result.frequency = 5180; // MHz (5GHz)
        result.signalLevel = 4; // 5단계 중 4

        handler.post(() -> callback.onTestProgress(60, "Monitoring signal..."));
        sleep(300);

        result.signalStrengthData = generateSignalData();
        result.linkSpeedData = generateLinkSpeedData();

        handler.post(() -> callback.onTestProgress(100, "Test completed"));

        return result;
    }

    /**
     * WiFi 신호 강도 모니터링
     */
    private List<Float> monitorWifiSignal(WifiManager wifiManager) {
        List<Float> data = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            if (!isTestRunning) break;

            try {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    data.add((float) wifiInfo.getRssi());
                }
                Thread.sleep(100);
            } catch (Exception e) {
                Log.e(TAG, "Error monitoring signal", e);
            }
        }

        return data;
    }

    /**
     * 링크 속도 모니터링
     */
    private List<Float> monitorLinkSpeed(WifiInfo wifiInfo) {
        List<Float> data = new ArrayList<>();

        if (wifiInfo != null) {
            float baseSpeed = wifiInfo.getLinkSpeed();
            Random random = new Random();

            for (int i = 0; i < 20; i++) {
                float variation = random.nextFloat() * 20 - 10; // ±10 Mbps
                data.add(baseSpeed + variation);
            }
        }

        return data;
    }

    /**
     * 샘플 신호 강도 데이터 생성
     */
    private List<Float> generateSignalData() {
        List<Float> data = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 20; i++) {
            float signal = -45 - random.nextFloat() * 15; // -45 ~ -60 dBm
            data.add(signal);
        }

        return data;
    }

    /**
     * 샘플 링크 속도 데이터 생성
     */
    private List<Float> generateLinkSpeedData() {
        List<Float> data = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 20; i++) {
            float speed = 866 - random.nextFloat() * 30; // 836~866 Mbps
            data.add(speed);
        }

        return data;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void startTest(TestCallback callback) {
        currentStatus = TestStatus.RUNNING;
        callback.onTestStarted();
    }

    @Override
    public void stopTest() {
        currentStatus = TestStatus.IDLE;
        isTestRunning = false;
    }

    @Override
    public void pauseTest() {
        currentStatus = TestStatus.PAUSED;
        isTestRunning = false;
    }

    @Override
    public void resumeTest() {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;
    }

    @Override
    public TestStatus getTestStatus() {
        return currentStatus;
    }

    public interface WifiTestCallback {
        void onTestProgress(int progress, String message);
        void onWifiTestCompleted(WifiTestResult result);
        void onError(String error);
    }

    public static class WifiTestResult {
        public boolean isWifiAvailable;
        public boolean isWifiEnabled;
        public boolean isConnected;
        public String ssid;
        public int linkSpeed; // Mbps
        public int rssi; // dBm
        public int frequency; // MHz
        public int signalLevel; // 0-4
        public List<Float> signalStrengthData = new ArrayList<>();
        public List<Float> linkSpeedData = new ArrayList<>();
        public String errorMessage;
    }
}
