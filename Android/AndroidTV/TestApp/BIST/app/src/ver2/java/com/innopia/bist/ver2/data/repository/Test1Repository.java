package com.innopia.bist.ver2.data.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.innopia.bist.ver2.util.TestResult;
import com.innopia.bist.ver2.util.TestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Test1 데이터 생성 및 관리를 담당하는 Repository
 * Test 인터페이스를 구현하여 표준화된 테스트 구조 제공
 */
public class Test1Repository implements Test {

    private static final String TAG = "Test1Repository";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TestStatus currentStatus = TestStatus.IDLE;

    /**
     * 랜덤 데이터 생성
     */
    public void generateRandomData(Test1Callback callback) {
        handler.postDelayed(() -> {
            try {
                List<Float> data = new ArrayList<>();
                Random random = new Random();

                for (int i = 0; i < 12; i++) {
                    data.add(random.nextFloat() * 100);
                }

                String info = "Random data generated with " + data.size() + " data points";
                callback.onDataGenerated(data, info);

            } catch (Exception e) {
                Log.e(TAG, "Error generating random data", e);
                callback.onError("Failed to generate random data: " + e.getMessage());
            }
        }, 500);
    }

    /**
     * 매출 데이터 생성
     */
    public void generateSalesData(Test1Callback callback) {
        handler.postDelayed(() -> {
            try {
                List<Float> data = new ArrayList<>();
                float[] salesData = {45.5f, 52.3f, 61.8f, 58.2f, 67.4f, 73.1f,
                        68.9f, 75.6f, 82.3f, 78.5f, 85.2f, 91.7f};

                for (float value : salesData) {
                    data.add(value);
                }

                String info = "Sales data loaded: 12 months of sales performance";
                callback.onDataGenerated(data, info);

            } catch (Exception e) {
                Log.e(TAG, "Error generating sales data", e);
                callback.onError("Failed to generate sales data: " + e.getMessage());
            }
        }, 500);
    }

    @Override
    public void startTest(TestCallback callback) {
        currentStatus = TestStatus.RUNNING;
        callback.onTestStarted();
    }

    @Override
    public void stopTest() {
        currentStatus = TestStatus.IDLE;
    }

    @Override
    public void pauseTest() {
        currentStatus = TestStatus.PAUSED;
    }

    @Override
    public void resumeTest() {
        currentStatus = TestStatus.RUNNING;
    }

    @Override
    public TestStatus getTestStatus() {
        return currentStatus;
    }

    /**
     * Test1 전용 콜백 인터페이스
     */
    public interface Test1Callback {
        void onDataGenerated(List<Float> data, String info);
        void onError(String error);
    }

    /**
     * 통계 데이터 클래스
     */
    public static class DataStats {
        public float average;
        public float max;
        public float min;

        public DataStats(List<Float> data) {
            if (data == null || data.isEmpty()) {
                return;
            }

            float sum = 0;
            max = Float.MIN_VALUE;
            min = Float.MAX_VALUE;

            for (float value : data) {
                sum += value;
                if (value > max) max = value;
                if (value < min) min = value;
            }

            average = sum / data.size();
        }
    }
}
