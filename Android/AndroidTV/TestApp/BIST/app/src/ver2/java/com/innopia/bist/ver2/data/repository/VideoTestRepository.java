package com.innopia.bist.ver2.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.innopia.bist.ver2.util.TestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 비디오 테스트 Repository
 * ExoPlayer를 통한 비디오 재생 및 성능 측정
 */
public class VideoTestRepository implements Test {

    private static final String TAG = "VideoTestRepository";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TestStatus currentStatus = TestStatus.IDLE;
    private boolean isTestRunning = false;

    // 샘플 데이터 사용 여부
    private boolean useSampleData = true;

    // 테스트 비디오 URL (샘플)
    private static final String[] SAMPLE_VIDEO_URLS = {
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
    };

    public VideoTestRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setUseSampleData(boolean useSampleData) {
        this.useSampleData = useSampleData;
    }

    /**
     * 비디오 재생 테스트 시작
     */
    public void startVideoTest(String videoUrl, VideoTestCallback callback) {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;

        new Thread(() -> {
            try {
                handler.post(() -> callback.onTestProgress(10, "Preparing video test..."));

                if (useSampleData) {
                    simulateVideoTest(callback);
                } else {
                    // 실제 비디오 테스트는 ExoPlayer 통합 후 구현
                    handler.post(() -> callback.onError("Real video test not yet implemented"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Video test error", e);
                currentStatus = TestStatus.ERROR;
                handler.post(() -> callback.onError("Video test failed: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * 비디오 테스트 시뮬레이션 (샘플 데이터)
     */
    private void simulateVideoTest(VideoTestCallback callback) {
        VideoTestResult result = new VideoTestResult();
        Random random = new Random();

        handler.post(() -> callback.onTestProgress(20, "Loading video..."));
        sleep(500);

        result.videoUrl = SAMPLE_VIDEO_URLS[0];
        result.videoCodec = "H.264";
        result.resolution = "1920x1080";
        result.videoBitrate = 2500; // kbps
        result.audioCodec = "AAC";
        result.audioBitrate = 128; // kbps
        result.duration = 596; // seconds

        handler.post(() -> callback.onTestProgress(40, "Starting playback..."));
        sleep(500);

        // 프레임 드롭 시뮬레이션
        List<Float> frameDropData = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            float drops = random.nextFloat() * 5; // 0-5 drops per second
            frameDropData.add(drops);
        }
        result.frameDropData = frameDropData;

        handler.post(() -> callback.onTestProgress(60, "Monitoring performance..."));
        sleep(500);

        // 버퍼링 이벤트 시뮬레이션
        List<Float> bufferingData = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            float buffering = random.nextFloat() < 0.1 ? random.nextFloat() * 2 : 0; // 10% chance of buffering
            bufferingData.add(buffering);
        }
        result.bufferingData = bufferingData;

        // 비트레이트 변화 시뮬레이션
        List<Float> bitrateData = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            float bitrate = 2500 + (random.nextFloat() * 500 - 250); // 2250-2750 kbps
            bitrateData.add(bitrate);
        }
        result.bitrateData = bitrateData;

        handler.post(() -> callback.onTestProgress(80, "Analyzing results..."));
        sleep(500);

        // 통계 계산
        result.averageFrameDrops = calculateAverage(frameDropData);
        result.totalBufferingEvents = countBufferingEvents(bufferingData);
        result.averageBitrate = calculateAverage(bitrateData);

        // 품질 평가
        if (result.averageFrameDrops < 1 && result.totalBufferingEvents < 3) {
            result.quality = "EXCELLENT";
            result.qualityColor = "#4CAF50";
        } else if (result.averageFrameDrops < 3 && result.totalBufferingEvents < 5) {
            result.quality = "GOOD";
            result.qualityColor = "#8BC34A";
        } else if (result.averageFrameDrops < 5 && result.totalBufferingEvents < 8) {
            result.quality = "FAIR";
            result.qualityColor = "#FFC107";
        } else {
            result.quality = "POOR";
            result.qualityColor = "#FF5252";
        }

        handler.post(() -> callback.onTestProgress(100, "Test completed"));

        currentStatus = TestStatus.COMPLETED;
        handler.post(() -> callback.onVideoTestCompleted(result));
    }

    /**
     * 평균 계산
     */
    private float calculateAverage(List<Float> data) {
        if (data.isEmpty()) return 0;

        float sum = 0;
        for (float value : data) {
            sum += value;
        }
        return sum / data.size();
    }

    /**
     * 버퍼링 이벤트 카운트
     */
    private int countBufferingEvents(List<Float> data) {
        int count = 0;
        for (float value : data) {
            if (value > 0) count++;
        }
        return count;
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

    public interface VideoTestCallback {
        void onTestProgress(int progress, String message);
        void onVideoTestCompleted(VideoTestResult result);
        void onError(String error);
    }

    public static class VideoTestResult {
        // 비디오 정보
        public String videoUrl;
        public String videoCodec;
        public String resolution;
        public int videoBitrate; // kbps
        public String audioCodec;
        public int audioBitrate; // kbps
        public int duration; // seconds

        // 성능 데이터
        public List<Float> frameDropData = new ArrayList<>();
        public List<Float> bufferingData = new ArrayList<>();
        public List<Float> bitrateData = new ArrayList<>();

        // 통계
        public float averageFrameDrops;
        public int totalBufferingEvents;
        public float averageBitrate;

        // 품질 평가
        public String quality; // EXCELLENT, GOOD, FAIR, POOR
        public String qualityColor;
    }
}
