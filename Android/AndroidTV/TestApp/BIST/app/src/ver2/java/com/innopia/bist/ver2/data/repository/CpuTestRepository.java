package com.innopia.bist.ver2.data.repository;

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
 * CPU 성능 측정 Repository
 */
public class CpuTestRepository implements Test {

    private static final String TAG = "CpuTestRepository";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TestStatus currentStatus = TestStatus.IDLE;
    private boolean isTestRunning = false;

    public CpuTestRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * CPU 성능 테스트
     */
    public void testCpuPerformance(CpuTestCallback callback) {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;

        new Thread(() -> {
            try {
                handler.post(() -> callback.onTestProgress(10, "Starting CPU test..."));

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
                        performanceData.add((float) (currentOps / 1000));

                        final int currentIteration = i;
                        int progress = 10 + (currentIteration * 40 / totalIterations);
                        handler.post(() -> {
                            callback.onTestProgress(progress,
                                    "CPU test: " + currentIteration + "/" + totalIterations);
                            callback.onChartDataUpdate(new ArrayList<>(performanceData));
                        });
                    }
                }

                long endTime = System.nanoTime();
                double executionTime = (endTime - startTime) / 1_000_000.0;
                double operationsPerSecond = (totalIterations / executionTime) * 1000;

                handler.post(() -> callback.onTestProgress(50, "CPU test completed"));

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
     * CPU 사용률 가져오기
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

    public interface CpuTestCallback {
        void onTestProgress(int progress, String message);
        void onChartDataUpdate(List<Float> data);
        void onCpuTestCompleted(CpuTestResult result);
        void onError(String error);
    }

    public static class CpuTestResult {
        public double executionTime;
        public double operationsPerSecond;
        public int coreCount;
        public float cpuUsage;
        public double calculationResult;
        public List<Float> performanceData;
    }
}
