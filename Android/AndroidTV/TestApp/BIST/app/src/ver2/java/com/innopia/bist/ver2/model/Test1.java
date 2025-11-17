package com.innopia.bist.ver2.model;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Test1 비즈니스 로직
 * - 랜덤 데이터 생성 테스트
 * - 콜백을 통해 결과 전달
 */
public class Test1 {

    private static final String TAG = "Test1";

    public interface Test1Callback {
        void onDataGenerated(List<Float> data, String info);
        void onError(String errorMessage);
    }

    /**
     * 랜덤 데이터 생성 (비동기 시뮬레이션)
     */
    public void generateRandomData(final Test1Callback callback) {
        // 백그라운드 스레드에서 데이터 생성 시뮬레이션
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1초 지연 (서버 통신 시뮬레이션)
                    Thread.sleep(1000);

                    // 랜덤 데이터 생성
                    List<Float> data = new ArrayList<>();
                    Random random = new Random();

                    for (int i = 0; i < 12; i++) { // 12개월 데이터
                        float value = 50 + random.nextFloat() * 50; // 50~100 사이 값
                        data.add(value);
                    }

                    // 메인 스레드에서 콜백 실행
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            String info = "Generated " + data.size() + " data points";
                            callback.onDataGenerated(data, info);
                        }
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Failed to generate data: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 특정 월별 매출 데이터 생성 (예시)
     */
    public void generateSalesData(final Test1Callback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);

                    // 월별 매출 시뮬레이션 (단위: 만원)
                    List<Float> salesData = new ArrayList<>();
                    float[] monthlySales = {
                            120f, 135f, 150f, 145f, 160f, 175f,
                            190f, 185f, 200f, 220f, 215f, 240f
                    };

                    for (float sale : monthlySales) {
                        salesData.add(sale);
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            String info = "Monthly Sales Data (12 months)";
                            callback.onDataGenerated(salesData, info);
                        }
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Failed to load sales data");
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 데이터 통계 계산
     */
    public static class DataStats {
        public float average;
        public float max;
        public float min;
        public float total;

        public DataStats(List<Float> data) {
            if (data == null || data.isEmpty()) {
                return;
            }

            float sum = 0;
            float maxVal = Float.MIN_VALUE;
            float minVal = Float.MAX_VALUE;

            for (Float value : data) {
                sum += value;
                maxVal = Math.max(maxVal, value);
                minVal = Math.min(minVal, value);
            }

            this.total = sum;
            this.average = sum / data.size();
            this.max = maxVal;
            this.min = minVal;
        }
    }
}
