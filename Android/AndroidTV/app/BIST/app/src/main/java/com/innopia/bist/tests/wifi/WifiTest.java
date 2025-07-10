package com.innopia.bist.tests.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import com.innopia.bist.tests.AutoTest;
import com.innopia.bist.tests.TestResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

// Wi-Fi 관련 수동/자동 테스트 로직을 모두 담당하는 클래스
public class WifiTest implements AutoTest {
    private static final String TAG = "BIST_WIFI_TEST";
    Executor executor;
    Handler mainThreadHandler;

    // 1. 핵심 Ping 로직을 담고 있는 private 메서드
    //    실시간 로그 콜백과 최종 결과 콜백을 모두 파라미터로 받습니다.
    private void performPing(Consumer<String> onLog, Consumer<TestResult> onFinished) {
        executor.execute(() -> {
            try {
                Process process = Runtime.getRuntime().exec("ping -c 10 8.8.8.8");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    // onLog 콜백이 제공된 경우에만 (즉, 수동 테스트 시에만) 실시간 로그를 전달합니다.
                    if (onLog != null) {
                        final String finalLine = line;
                        mainThreadHandler.post(() -> onLog.accept(finalLine));
                    }
                }

                process.waitFor();
                reader.close();

                boolean isSuccess = (process.exitValue() == 0);
                String message = isSuccess ? "Ping test successful." : "Ping test failed. Host might be unreachable.";

                // 최종 결과는 항상 onFinished 콜백으로 전달합니다.
                mainThreadHandler.post(() -> onFinished.accept(new TestResult(isSuccess, message)));

            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Ping test failed", e);
                mainThreadHandler.post(() -> onFinished.accept(new TestResult(false, "Ping Test Error: " + e.getMessage())));
            }
        });
    }

    // 2. 수동 테스트(UI)용 public 메서드 - PingResultListener 사용
    /**
     * Ping 테스트를 실행하고, 모든 과정을 실시간으로 로깅합니다.
     * @param listener 실시간 로그 및 최종 결과를 전달받을 리스너
     */
    public void runPingTest(PingResultListener listener) {
        if (listener == null) return;

        // 핵심 로직 호출: 람다식을 사용하여 listener의 메서드를 onLog와 onFinished에 연결합니다.
        performPing(
                log -> listener.onPingLog(log),
                result -> listener.onPingFinished(result.message, result.isSuccess)
        );
    }

    // 3. 자동 테스트용 public 메서드 - Consumer<TestResult> 사용
    /**
     * Ping 테스트를 실행하고, 최종 결과만 반환합니다.
     * @param onResult 최종 결과를 전달받을 Consumer 콜백
     */
    public void runPingTest(Consumer<TestResult> onResult) {
        if (onResult == null) return;

        // 핵심 로직 호출: 실시간 로그(onLog)는 필요 없으므로 null을 전달합니다.
        performPing(null, onResult);
    }

    /**
     * IAutoTest 인터페이스 구현 메서드
     * 자동 테스트에 필요한 `runPingTest(Consumer<TestResult>)` 버전을 호출합니다.
     */
    @Override
    public void runTest(Consumer<TestResult> onResult) {
        runPingTest(onResult);
    }

    /**
     * 요구사항 반영: 현재 연결된 Wi-Fi의 상세 정보를 가져옵니다.
     * @param context Context 객체
     * @return 상세 정보가 담긴 WifiDetails 객체
     */
    public WifiDetails getCurrentWifiInfo(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (networkInfo != null && networkInfo.isConnected()) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            String ssid = wifiInfo.getSSID().replace("\"", "");
            String bssid = wifiInfo.getBSSID();
            String macAddress = wifiInfo.getMacAddress(); // Android 6.0 이상에서는 "02:00:00:00:00:00"을 반환할 수 있음
            int rssi = wifiInfo.getRssi();
            int linkSpeed = wifiInfo.getLinkSpeed(); // Mbps 단위

            return new WifiDetails(ssid, bssid, macAddress, rssi, linkSpeed);
        } else {
            // Wi-Fi가 연결되지 않은 경우
            return new WifiDetails();
        }
    }

    public interface PingResultListener {
        /**
         * Ping 과정에서 한 줄의 로그가 발생할 때마다 호출됩니다.
         */
        void onPingLog(String log);

        /**
         * Ping 테스트가 완전히 종료되었을 때 호출됩니다.
         * @param resultMessage 최종 결과 메시지
         * @param isSuccess 테스트 성공 여부
         */
        void onPingFinished(String resultMessage, boolean isSuccess);
    }

}
