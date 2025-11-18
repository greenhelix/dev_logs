package com.innopia.bist.ver2.data.repository;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.innopia.bist.ver2.util.TestResult;
import com.innopia.bist.ver2.util.TestStatus;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Test2 - 메모리 및 CPU 성능 측정 Repository
 */
public class Test2Repository implements Test {

    private static final String TAG = "Test2Repository";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TestStatus currentStatus = TestStatus.IDLE;
    private boolean isTestRunning = false;

    public Test2Repository(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 메모리 성능 테스트
     */
    public void testMemoryPerformance(Test2Callback callback) {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;

        new Thread(() -> {
            try {
                handler.post(() -> callback.onTestProgress(10, "Starting memory test..."));

                // 메모리 할당/해제 속도 측정을 위한 데이터 수집
                List<Float> speedData = new ArrayList<>();

                long startTime = System.nanoTime();
                int iterations = 100;
                int arraySize = 10000;

                for (int i = 0; i < iterations; i++) {
                    if (!isTestRunning) break;

                    long iterStartTime = System.nanoTime();

                    int[] testArray = new int[arraySize];
                    for (int j = 0; j < arraySize; j++) {
                        testArray[j] = j * 2;
                    }

                    long iterEndTime = System.nanoTime();
                    double iterSpeed = (arraySize * 4.0) /
                            ((iterEndTime - iterStartTime) / 1_000_000_000.0) / (1024 * 1024); // MB/s

                    speedData.add((float) iterSpeed);

                    if (i % 10 == 0) {
                        final int currentIteration = i;
                        int progress = 10 + (currentIteration * 40 / iterations);
                        handler.post(() -> {
                            callback.onTestProgress(progress,
                                    "Memory allocation test: " + currentIteration + "/" + iterations);
                            callback.onChartDataUpdate(new ArrayList<>(speedData));
                        });
                    }
                }

                long endTime = System.nanoTime();
                double memorySpeed = (iterations * arraySize * 4.0) /
                        ((endTime - startTime) / 1_000_000_000.0) / (1024 * 1024); // MB/s

                handler.post(() -> callback.onTestProgress(50, "Memory test completed"));

                // 메모리 정보 수집
                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager)
                        context.getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager != null) {
                    activityManager.getMemoryInfo(memInfo);
                }

                MemoryTestResult result = new MemoryTestResult();
                result.memorySpeed = memorySpeed;
                result.totalMemory = memInfo.totalMem / (1024 * 1024); // MB
                result.availableMemory = memInfo.availMem / (1024 * 1024); // MB
                result.usedMemory = result.totalMemory - result.availableMemory;
                result.memoryUsagePercent = (result.usedMemory * 100.0f) / result.totalMemory;
                result.speedData = speedData;

                currentStatus = TestStatus.COMPLETED;
                handler.post(() -> callback.onMemoryTestCompleted(result));

            } catch (Exception e) {
                Log.e(TAG, "Memory test error", e);
                currentStatus = TestStatus.ERROR;
                handler.post(() -> callback.onError("Memory test failed: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * CPU 성능 테스트
     */
    public void testCpuPerformance(Test2Callback callback) {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;

        new Thread(() -> {
            try {
                handler.post(() -> callback.onTestProgress(10, "Starting CPU test..."));

                // CPU 성능 데이터 수집
                List<Float> performanceData = new ArrayList<>();

                long startTime = System.nanoTime();
                int totalIterations = 1000000;
                int checkInterval = 50000;
                double result = 0;

                for (int i = 0; i < totalIterations; i++) {
                    if (!isTestRunning) break;

                    result += Math.sqrt(i) * Math.sin(i) * Math.cos(i);

                    if (i > 0 && i % checkInterval == 0) {
                        long currentTime = System.nanoTime();
                        double elapsedMs = (currentTime - startTime) / 1_000_000.0;
                        double currentOps = (i / elapsedMs) * 1000;
                        performanceData.add((float) (currentOps / 1000)); // K ops/s

                        final int currentIteration = i;
                        int progress = 10 + (currentIteration * 40 / totalIterations);
                        handler.post(() -> {
                            callback.onTestProgress(progress,
                                    "CPU calculation test: " + currentIteration + "/" + totalIterations);
                            callback.onChartDataUpdate(new ArrayList<>(performanceData));
                        });
                    }
                }

                long endTime = System.nanoTime();
                double executionTime = (endTime - startTime) / 1_000_000.0; // ms
                double operationsPerSecond = (totalIterations / executionTime) * 1000;

                handler.post(() -> callback.onTestProgress(50, "CPU test completed"));

                // CPU 정보 수집
                int coreCount = Runtime.getRuntime().availableProcessors();
                float cpuUsage = getCpuUsage();

                CpuTestResult cpuResult = new CpuTestResult();
                cpuResult.executionTime = executionTime;
                cpuResult.operationsPerSecond = operationsPerSecond;
                cpuResult.coreCount = coreCount;
                cpuResult.cpuUsage = cpuUsage;
                cpuResult.calculationResult = result;
                cpuResult.performanceData = performanceData;

                currentStatus = TestStatus.COMPLETED;
                handler.post(() -> callback.onCpuTestCompleted(cpuResult));

            } catch (Exception e) {
                Log.e(TAG, "CPU test error", e);
                currentStatus = TestStatus.ERROR;
                handler.post(() -> callback.onError("CPU test failed: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * CPU 사용률 가져오기 (근사값)
     */
    private float getCpuUsage() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();
            reader.close();

            String[] toks = load.split(" +");
            long idle1 = Long.parseLong(toks[4]);
            long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) +
                    Long.parseLong(toks[5]) + Long.parseLong(toks[6]) +
                    Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            Thread.sleep(360);

            reader = new RandomAccessFile("/proc/stat", "r");
            load = reader.readLine();
            reader.close();

            toks = load.split(" +");
            long idle2 = Long.parseLong(toks[4]);
            long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) +
                    Long.parseLong(toks[5]) + Long.parseLong(toks[6]) +
                    Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1)) * 100f;

        } catch (Exception e) {
            Log.e(TAG, "Error getting CPU usage", e);
            return 0f;
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

    /**
     * Test2 전용 콜백 인터페이스
     */
    public interface Test2Callback {
        void onTestProgress(int progress, String message);
        void onChartDataUpdate(List<Float> data);
        void onMemoryTestCompleted(MemoryTestResult result);
        void onCpuTestCompleted(CpuTestResult result);
        void onError(String error);
    }

    /**
     * 메모리 테스트 결과
     */
    public static class MemoryTestResult {
        public double memorySpeed; // MB/s
        public long totalMemory; // MB
        public long availableMemory; // MB
        public long usedMemory; // MB
        public float memoryUsagePercent;
        public List<Float> speedData; // 차트용 데이터
    }

    /**
     * CPU 테스트 결과
     */
    public static class CpuTestResult {
        public double executionTime; // ms
        public double operationsPerSecond;
        public int coreCount;
        public float cpuUsage; // %
        public double calculationResult;
        public List<Float> performanceData; // 차트용 데이터
    }
}
