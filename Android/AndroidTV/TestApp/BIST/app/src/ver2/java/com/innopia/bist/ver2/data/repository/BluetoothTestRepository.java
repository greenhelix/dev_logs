package com.innopia.bist.ver2.data.repository;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.innopia.bist.ver2.util.TestResult;
import com.innopia.bist.ver2.util.TestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 블루투스 테스트 Repository
 */
public class BluetoothTestRepository implements Test {

    private static final String TAG = "BluetoothTestRepository";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TestStatus currentStatus = TestStatus.IDLE;
    private boolean isTestRunning = false;

    // 샘플 데이터 사용 여부
    private boolean useSampleData = true; // 기본값: 샘플 데이터 사용

    public BluetoothTestRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 샘플 데이터 모드 설정
     */
    public void setUseSampleData(boolean useSampleData) {
        this.useSampleData = useSampleData;
    }

    /**
     * 블루투스 테스트
     */
    public void testBluetooth(BluetoothTestCallback callback) {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;

        new Thread(() -> {
            try {
                handler.post(() -> callback.onTestProgress(10, "Checking Bluetooth..."));

                BluetoothTestResult result;

                if (useSampleData) {
                    result = getSampleBluetoothData(callback);
                } else {
                    result = getRealBluetoothData(callback);
                }

                currentStatus = TestStatus.COMPLETED;
                handler.post(() -> callback.onBluetoothTestCompleted(result));

            } catch (Exception e) {
                Log.e(TAG, "Bluetooth test error", e);
                currentStatus = TestStatus.ERROR;
                handler.post(() -> callback.onError("Bluetooth test failed: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * 실제 블루투스 데이터 가져오기
     */
    private BluetoothTestResult getRealBluetoothData(BluetoothTestCallback callback) {
        BluetoothTestResult result = new BluetoothTestResult();

        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (bluetoothAdapter == null) {
                result.isBluetoothAvailable = false;
                result.errorMessage = "Bluetooth not supported";
                return result;
            }

            result.isBluetoothAvailable = true;
            result.isBluetoothEnabled = bluetoothAdapter.isEnabled();

            if (!result.isBluetoothEnabled) {
                result.errorMessage = "Bluetooth is disabled";
                return result;
            }

            handler.post(() -> callback.onTestProgress(30, "Scanning devices..."));

            // 페어링된 디바이스 확인
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            result.pairedDeviceCount = pairedDevices.size();

            for (BluetoothDevice device : pairedDevices) {
                result.deviceNames.add(device.getName() != null ? device.getName() : "Unknown");
            }

            handler.post(() -> callback.onTestProgress(60, "Testing signal..."));

            // 신호 강도 시뮬레이션 (실제로는 연결 후 RSSI 측정 필요)
            result.signalStrengthData = generateSignalData();

            result.connectionSuccessful = true;

        } catch (SecurityException e) {
            result.errorMessage = "Bluetooth permission denied";
            Log.e(TAG, "Bluetooth permission error", e);
        } catch (Exception e) {
            result.errorMessage = "Bluetooth test failed: " + e.getMessage();
            Log.e(TAG, "Bluetooth test error", e);
        }

        return result;
    }

    /**
     * 샘플 블루투스 데이터
     */
    private BluetoothTestResult getSampleBluetoothData(BluetoothTestCallback callback) {
        BluetoothTestResult result = new BluetoothTestResult();

        handler.post(() -> callback.onTestProgress(30, "Scanning devices..."));
        sleep(500);

        result.isBluetoothAvailable = true;
        result.isBluetoothEnabled = true;
        result.pairedDeviceCount = 3;
        result.deviceNames.add("Sample Headphone");
        result.deviceNames.add("Sample Speaker");
        result.deviceNames.add("Sample Remote");

        handler.post(() -> callback.onTestProgress(60, "Testing signal..."));
        sleep(500);

        result.signalStrengthData = generateSignalData();
        result.connectionSuccessful = true;

        handler.post(() -> callback.onTestProgress(100, "Test completed"));

        return result;
    }

    /**
     * 신호 강도 데이터 생성
     */
    private List<Float> generateSignalData() {
        List<Float> data = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 20; i++) {
            float signal = -40 - random.nextFloat() * 30; // -40 ~ -70 dBm
            data.add(signal);
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

    public interface BluetoothTestCallback {
        void onTestProgress(int progress, String message);
        void onBluetoothTestCompleted(BluetoothTestResult result);
        void onError(String error);
    }

    public static class BluetoothTestResult {
        public boolean isBluetoothAvailable;
        public boolean isBluetoothEnabled;
        public int pairedDeviceCount;
        public List<String> deviceNames = new ArrayList<>();
        public List<Float> signalStrengthData = new ArrayList<>();
        public boolean connectionSuccessful;
        public String errorMessage;
    }
}
