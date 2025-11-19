package com.innopia.bist.ver2.data.repository;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;

import com.innopia.bist.ver2.util.TestResult;
import com.innopia.bist.ver2.util.TestStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 저장공간 테스트 Repository
 */
public class StorageTestRepository implements Test {

    private static final String TAG = "StorageTestRepository";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TestStatus currentStatus = TestStatus.IDLE;
    private boolean isTestRunning = false;

    // 샘플 데이터 사용 여부
    private boolean useSampleData = false;

    public StorageTestRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 샘플 데이터 모드 설정
     */
    public void setUseSampleData(boolean useSampleData) {
        this.useSampleData = useSampleData;
    }

    /**
     * 저장공간 테스트
     */
    public void testStorage(StorageTestCallback callback) {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;

        new Thread(() -> {
            try {
                handler.post(() -> callback.onTestProgress(10, "Checking storage..."));

                StorageTestResult result;

                if (useSampleData) {
                    result = getSampleStorageData();
                } else {
                    result = getRealStorageData();
                }

                handler.post(() -> callback.onTestProgress(50, "Analyzing storage..."));

                // 저장공간 상태 판단
                float usagePercent = (result.usedSpace * 100.0f) / result.totalSpace;
                if (usagePercent > 90) {
                    result.status = "BUSY";
                    result.statusColor = "#FF5252";
                } else if (usagePercent > 70) {
                    result.status = "NORMAL";
                    result.statusColor = "#FFC107";
                } else {
                    result.status = "GOOD";
                    result.statusColor = "#4CAF50";
                }

                handler.post(() -> callback.onTestProgress(100, "Storage check completed"));

                currentStatus = TestStatus.COMPLETED;
                handler.post(() -> callback.onStorageTestCompleted(result));

            } catch (Exception e) {
                Log.e(TAG, "Storage test error", e);
                currentStatus = TestStatus.ERROR;
                handler.post(() -> callback.onError("Storage test failed: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * 실제 저장공간 데이터 가져오기
     */
    private StorageTestResult getRealStorageData() {
        StorageTestResult result = new StorageTestResult();

        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());

            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocksLong();

            result.totalSpace = (totalBlocks * blockSize) / (1024 * 1024 * 1024); // GB
            result.freeSpace = (availableBlocks * blockSize) / (1024 * 1024 * 1024); // GB
            result.usedSpace = result.totalSpace - result.freeSpace;

            // 외부 저장소 (SD 카드 등)
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File externalPath = Environment.getExternalStorageDirectory();
                StatFs externalStat = new StatFs(externalPath.getPath());

                long externalBlockSize = externalStat.getBlockSizeLong();
                long externalTotalBlocks = externalStat.getBlockCountLong();
                long externalAvailableBlocks = externalStat.getAvailableBlocksLong();

                result.externalTotalSpace = (externalTotalBlocks * externalBlockSize) / (1024 * 1024 * 1024);
                result.externalFreeSpace = (externalAvailableBlocks * externalBlockSize) / (1024 * 1024 * 1024);
                result.externalUsedSpace = result.externalTotalSpace - result.externalFreeSpace;
                result.hasExternalStorage = true;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting real storage data", e);
        }

        return result;
    }

    /**
     * 샘플 저장공간 데이터 (권한 없을 때)
     */
    private StorageTestResult getSampleStorageData() {
        StorageTestResult result = new StorageTestResult();

        result.totalSpace = 64; // 64GB
        result.usedSpace = 45; // 45GB
        result.freeSpace = 19; // 19GB

        result.hasExternalStorage = true;
        result.externalTotalSpace = 128; // 128GB
        result.externalUsedSpace = 32; // 32GB
        result.externalFreeSpace = 96; // 96GB

        return result;
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

    public interface StorageTestCallback {
        void onTestProgress(int progress, String message);
        void onStorageTestCompleted(StorageTestResult result);
        void onError(String error);
    }

    public static class StorageTestResult {
        // 내부 저장소
        public long totalSpace; // GB
        public long usedSpace; // GB
        public long freeSpace; // GB

        // 외부 저장소
        public boolean hasExternalStorage;
        public long externalTotalSpace; // GB
        public long externalUsedSpace; // GB
        public long externalFreeSpace; // GB

        // 상태
        public String status; // GOOD, NORMAL, BUSY
        public String statusColor;
    }
}
