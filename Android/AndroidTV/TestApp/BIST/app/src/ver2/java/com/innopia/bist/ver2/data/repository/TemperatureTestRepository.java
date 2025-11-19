package com.innopia.bist.ver2.data.repository;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.innopia.bist.ver2.util.TestStatus;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 온도 테스트 Repository
 * CPU, 배터리 등의 온도 모니터링
 */
public class TemperatureTestRepository implements Test {

    private static final String TAG = "TemperatureTestRepository";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TestStatus currentStatus = TestStatus.IDLE;
    private boolean isMonitoring = false;

    // 샘플 데이터 사용 여부
    private boolean useSampleData = true;

    // CPU 온도 파일 경로들
    private static final String[] CPU_TEMP_PATHS = {
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/devices/platform/omap/omap_temp_sensor.0/temperature"
    };

    public TemperatureTestRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setUseSampleData(boolean useSampleData) {
        this.useSampleData = useSampleData;
    }

    /**
     * 온도 모니터링 시작
     */
    public void startTemperatureMonitoring(TemperatureTestCallback callback) {
        currentStatus = TestStatus.RUNNING;
        isMonitoring = true;

        new Thread(() -> {
            try {
                handler.post(() -> callback.onMonitoringStarted());

                List<Float> cpuTempHistory = new ArrayList<>();
                List<Float> batteryTempHistory = new ArrayList<>();

                while (isMonitoring) {
                    TemperatureTestResult result;

                    if (useSampleData) {
                        result = getSampleTemperatureData();
                    } else {
                        result = getRealTemperatureData();
                    }

                    // 온도 히스토리에 추가
                    cpuTempHistory.add(result.cpuTemperature);
                    batteryTempHistory.add(result.batteryTemperature);

                    // 최근 30개만 유지
                    if (cpuTempHistory.size() > 30) {
                        cpuTempHistory.remove(0);
                        batteryTempHistory.remove(0);
                    }

                    result.cpuTempHistory = new ArrayList<>(cpuTempHistory);
                    result.batteryTempHistory = new ArrayList<>(batteryTempHistory);

                    handler.post(() -> callback.onTemperatureUpdated(result));

                    Thread.sleep(1000); // 1초마다 업데이트
                }

                currentStatus = TestStatus.IDLE;
                handler.post(() -> callback.onMonitoringStopped());

            } catch (Exception e) {
                Log.e(TAG, "Temperature monitoring error", e);
                currentStatus = TestStatus.ERROR;
                handler.post(() -> callback.onError("Monitoring failed: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * 실제 온도 데이터 가져오기
     */
    private TemperatureTestResult getRealTemperatureData() {
        TemperatureTestResult result = new TemperatureTestResult();

        // CPU 온도 읽기
        result.cpuTemperature = readCpuTemperature();

        // 배터리 온도 읽기
        result.batteryTemperature = readBatteryTemperature();

        // 상태 평가
        evaluateTemperatureStatus(result);

        return result;
    }

    /**
     * CPU 온도 읽기
     */
    private float readCpuTemperature() {
        for (String path : CPU_TEMP_PATHS) {
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                String line = reader.readLine();
                if (line != null) {
                    float temp = Float.parseFloat(line.trim());
                    // 일부 시스템은 밀리도로 반환하므로 변환
                    if (temp > 1000) {
                        temp = temp / 1000f;
                    }
                    return temp;
                }
            } catch (Exception e) {
                // 다음 경로 시도
            }
        }

        return 0f; // 읽기 실패
    }

    /**
     * 배터리 온도 읽기
     */
    private float readBatteryTemperature() {
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, filter);

            if (batteryStatus != null) {
                int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                return temp / 10f; // 온도는 10배수로 반환됨
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading battery temperature", e);
        }

        return 0f;
    }

    /**
     * 샘플 온도 데이터
     */
    private TemperatureTestResult getSampleTemperatureData() {
        TemperatureTestResult result = new TemperatureTestResult();
        Random random = new Random();

        // CPU 온도: 40~65도 사이
        result.cpuTemperature = 45 + random.nextFloat() * 20;

        // 배터리 온도: 30~45도 사이
        result.batteryTemperature = 32 + random.nextFloat() * 13;

        // 상태 평가
        evaluateTemperatureStatus(result);

        return result;
    }

    /**
     * 온도 상태 평가
     */
    private void evaluateTemperatureStatus(TemperatureTestResult result) {
        // CPU 온도 상태
        if (result.cpuTemperature < 50) {
            result.cpuStatus = "NORMAL";
            result.cpuStatusColor = "#4CAF50";
        } else if (result.cpuTemperature < 70) {
            result.cpuStatus = "WARM";
            result.cpuStatusColor = "#FFC107";
        } else if (result.cpuTemperature < 85) {
            result.cpuStatus = "HOT";
            result.cpuStatusColor = "#FF9800";
        } else {
            result.cpuStatus = "CRITICAL";
            result.cpuStatusColor = "#FF5252";
        }

        // 배터리 온도 상태
        if (result.batteryTemperature < 40) {
            result.batteryStatus = "NORMAL";
            result.batteryStatusColor = "#4CAF50";
        } else if (result.batteryTemperature < 45) {
            result.batteryStatus = "WARM";
            result.batteryStatusColor = "#FFC107";
        } else {
            result.batteryStatus = "HOT";
            result.batteryStatusColor = "#FF5252";
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
        isMonitoring = false;
    }

    @Override
    public void pauseTest() {
        currentStatus = TestStatus.PAUSED;
        isMonitoring = false;
    }

    @Override
    public void resumeTest() {
        currentStatus = TestStatus.RUNNING;
        isMonitoring = true;
    }

    @Override
    public TestStatus getTestStatus() {
        return currentStatus;
    }

    public interface TemperatureTestCallback {
        void onMonitoringStarted();
        void onTemperatureUpdated(TemperatureTestResult result);
        void onMonitoringStopped();
        void onError(String error);
    }

    public static class TemperatureTestResult {
        // 현재 온도
        public float cpuTemperature; // °C
        public float batteryTemperature; // °C

        // 온도 히스토리
        public List<Float> cpuTempHistory = new ArrayList<>();
        public List<Float> batteryTempHistory = new ArrayList<>();

        // 상태
        public String cpuStatus; // NORMAL, WARM, HOT, CRITICAL
        public String cpuStatusColor;
        public String batteryStatus; // NORMAL, WARM, HOT
        public String batteryStatusColor;
    }
}
