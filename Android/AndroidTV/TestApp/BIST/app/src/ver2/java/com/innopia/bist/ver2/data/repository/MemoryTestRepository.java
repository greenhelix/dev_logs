package com.innopia.bist.ver2.data.repository;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.innopia.bist.ver2.util.TestResult;
import com.innopia.bist.ver2.util.TestStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Memory 성능 측정 Repository
 */
public class MemoryTestRepository implements Test {

    private static final String TAG = "MemoryTestRepository";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TestStatus currentStatus = TestStatus.IDLE;
    private boolean isTestRunning = false;

    public MemoryTestRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 메모리 성능 테스트
     */
    public void testMemoryPerformance(MemoryTestCallback callback) {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;

        new Thread(() -> {
            try {
                handler.post(() -> callback.onTestProgress(10, "Starting memory test..."));

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
                            ((iterEndTime - iterStartTime) / 1_000_000_000.0) / (1024 * 1024);

                    speedData.add((float) iterSpeed);

                    if (i % 10 == 0) {
                        final int currentIteration = i;
                        int progress = 10 + (currentIteration * 40 / iterations);
                        handler.post(() -> {
                            callback.onTestProgress(progress,
                                    "Memory test: " + currentIteration + "/" + iterations);
                            callback.onChartDataUpdate(new ArrayList<>(speedData));
                        });
                    }
                }

                long endTime = System.nanoTime();
                double memorySpeed = (iterations * arraySize * 4.0) /
                        ((endTime - startTime) / 1_000_000_000.0) / (1024 * 1024);

                handler.post(() -> callback.onTestProgress(50, "Memory test completed"));

                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager)
                        context.getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager != null) {
                    activityManager.getMemoryInfo(memInfo);
                }

                MemoryTestResult result = new MemoryTestResult();
                result.memorySpeed = memorySpeed;
                result.totalMemory = memInfo.totalMem / (1024 * 1024);
                result.availableMemory = memInfo.availMem / (1024 * 1024);
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

    public interface MemoryTestCallback {
        void onTestProgress(int progress, String message);
        void onChartDataUpdate(List<Float> data);
        void onMemoryTestCompleted(MemoryTestResult result);
        void onError(String error);
    }

    public static class MemoryTestResult {
        public double memorySpeed;
        public long totalMemory;
        public long availableMemory;
        public long usedMemory;
        public float memoryUsagePercent;
        public List<Float> speedData;
    }
}
