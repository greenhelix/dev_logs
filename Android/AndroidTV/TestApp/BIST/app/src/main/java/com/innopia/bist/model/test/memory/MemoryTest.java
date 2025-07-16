package com.innopia.bist.model.test.memory;

import android.app.ActivityManager;
import android.content.Context;
import com.innopia.bist.model.Test;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MemoryTest implements Test {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // 기준점: 남은 메모리가 500MB 이상이고, 쓰기 속도가 100 MB/s 이상이면 통과
    private static final long PASS_THRESHOLD_MEMORY_MB = 500;
    private static final double PASS_THRESHOLD_SPEED_MBps = 100.0;

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<String> callback) {
        executor.execute(() -> {
            Context context = (Context) params.get("context");
            if (context == null) {
                callback.accept("Error: Context is null");
                return;
            }

            String memoryUsage = checkMemoryUsage(context);
            String speedTestResult = runSpeedTest();

            // 양호/불량 판정 로직 추가
            ActivityManager.MemoryInfo memInfo = getMemoryInfo(context);
            double writeSpeed = parseSpeed(speedTestResult); // speedTestResult에서 속도 값 파싱
            boolean isMemoryOk = (memInfo.availMem / 1024 / 1024) > PASS_THRESHOLD_MEMORY_MB;
            boolean isSpeedOk = writeSpeed > PASS_THRESHOLD_SPEED_MBps;

            String finalStatus = (isMemoryOk && isSpeedOk) ? "Result: PASS" : "Result: FAIL";

            String result = "== Memory Test Result ==\n" + memoryUsage + "\n" + speedTestResult + "\n\n" + finalStatus;
            callback.accept(result);
        });
    }

    @Override
    public void runAutoTest(Map<String, Object> params, Consumer<String> callback) {
        runManualTest(params, callback); // 자동 테스트는 수동 테스트와 동일하게 동작
    }

    private ActivityManager.MemoryInfo getMemoryInfo(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    private String checkMemoryUsage(Context context) {
        ActivityManager.MemoryInfo mi = getMemoryInfo(context);
        long availableMegs = mi.availMem / 1048576L; // 1024*1024
        long totalMegs = mi.totalMem / 1048576L;
        return String.format("Memory Usage: %d MB / %d MB", (totalMegs - availableMegs), totalMegs);
    }

    private String runSpeedTest() {
        try {
            // 50MB 크기의 데이터로 속도 측정
            int size = 50 * 1024 * 1024;
            byte[] data = new byte[size];

            // 쓰기 속도 측정
            long startTime = System.nanoTime();
            for (int i = 0; i < size; i++) {
                data[i] = (byte) i;
            }
            long endTime = System.nanoTime();
            long writeDuration = endTime - startTime;
            double writeSpeed = (double) size / (1024*1024) / (writeDuration / 1_000_000_000.0);

            // 읽기 속도 측정
            startTime = System.nanoTime();
            int temp = 0;
            for (int i = 0; i < size; i++) {
                temp += data[i];
            }
            endTime = System.nanoTime();
            long readDuration = endTime - startTime;
            double readSpeed = (double) size / (1024*1024) / (readDuration / 1_000_000_000.0);

            return String.format("Memory Speed: Write %.2f MB/s, Read %.2f MB/s", writeSpeed, readSpeed);

        } catch (OutOfMemoryError e) {
            return "Memory Speed: Test Failed (Out of Memory)";
        }
    }

    // speedTestResult 문자열에서 쓰기 속도 값을 파싱하는 헬퍼 함수
    private double parseSpeed(String speedResult) {
        if (!speedResult.contains("Write")) return 0.0;
        try {
            String[] parts = speedResult.split(" ");
            return Double.parseDouble(parts[2]);
        } catch(Exception e) {
            return 0.0;
        }
    }
}
