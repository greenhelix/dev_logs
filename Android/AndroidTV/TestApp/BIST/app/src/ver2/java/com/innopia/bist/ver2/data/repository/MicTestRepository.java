package com.innopia.bist.ver2.data.repository;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.List;

public class MicTestRepository {
    private static final String TAG = "MicTestRepository";
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isTestRunning = false;
    private List<AudioRecord> activeMics = new ArrayList<>();
    // Stores history for up to 4 mics: index 0 = Mic1, 1 = Mic2, etc.
    private List<List<Float>> allMicVolumeData = new ArrayList<>();

    public MicTestRepository(Context context) {
        this.context = context.getApplicationContext();
        // Initialize lists for 4 potential mics
        for (int i = 0; i < 4; i++) {
            allMicVolumeData.add(new ArrayList<>());
        }
    }

    public void testMicPerformance(Test3Callback callback) {
        isTestRunning = true;
        // Clear previous data
        for (List<Float> list : allMicVolumeData) {
            list.clear();
        }

        handler.post(() -> callback.onTestProgress(10, "Starting Mic test..."));

        new Thread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    handler.post(() -> callback.onError("Permission denied"));
                    return;
                }

                // 1. Initialize Mics
                // Note: Standard Android devices often map different physical mics to the same AudioSource
                // depending on configuration. Here we attempt to open multiple instances to simulate
                // or capture from available channels if the hardware supports concurrent access.
                // In a strict hardware test, you might iterate different AudioSources (CAMCORDER, MIC, etc.)
                int maxMics = 4;
                activeMics.clear();

                for (int i = 0; i < maxMics; i++) {
                    try {
                        // For demonstration, we are creating multiple instances.
                        // Real hardware might require specific AudioSource flags for distinct physical mics.
                        AudioRecord mic = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);

                        if (mic.getState() == AudioRecord.STATE_INITIALIZED) {
                            mic.startRecording();
                            activeMics.add(mic);
                        } else {
                            mic.release();
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Mic " + i + " init failed", e);
                    }
                }

                int finalActiveCount = activeMics.size();
                handler.post(() -> callback.onTestProgress(30,
                        "Found " + finalActiveCount + " active mics"));

                // 2. Real-time Monitoring Loop
                long startTime = System.currentTimeMillis();
                while (isTestRunning && (System.currentTimeMillis() - startTime) < 10000) { // 10s test

                    // Capture data for each active mic
                    for (int i = 0; i < activeMics.size(); i++) {
                        AudioRecord mic = activeMics.get(i);
                        short[] buffer = new short[BUFFER_SIZE / 2];
                        int read = mic.read(buffer, 0, buffer.length);

                        float volumeDb = 0;
                        if (read > 0) {
                            double sum = 0;
                            for (short sample : buffer) {
                                sum += sample * sample;
                            }
                            double rms = Math.sqrt(sum / read);
                            // Avoid log(0)
                            if (rms > 0) {
                                volumeDb = (float) (20 * Math.log10(rms / 32768.0));
                                volumeDb = Math.max(0, volumeDb + 90); // Normalize to ~0-90 dB range for display
                            }
                        }

                        // Add to specific mic's history
                        if (i < allMicVolumeData.size()) {
                            allMicVolumeData.get(i).add(volumeDb);
                        }
                    }

                    // Pad inactive mics with 0 for graph consistency (optional)
                    for (int i = activeMics.size(); i < 4; i++) {
                        allMicVolumeData.get(i).add(0f);
                    }

                    // Send snapshot of all data for drawing
                    // We create a deep copy to avoid ConcurrentModificationException during drawing
                    List<List<Float>> snapshot = new ArrayList<>();
                    for (List<Float> list : allMicVolumeData) {
                        snapshot.add(new ArrayList<>(list));
                    }

                    handler.post(() -> callback.onChartDataUpdate(snapshot));

                    Thread.sleep(100); // 10Hz refresh rate
                }

                // 3. Cleanup & Result
                MicTestResult result = new MicTestResult();
                result.activeMicCount = activeMics.size();
                result.status = activeMics.size() > 0 ? "Success" : "No Signal";
                // Just pass the first mic's data as summary, or average
                if (!allMicVolumeData.get(0).isEmpty()) {
                    result.volumeData = new ArrayList<>(allMicVolumeData.get(0));
                } else {
                    result.volumeData = new ArrayList<>();
                }

                stopTest(); // Close mics

                handler.post(() -> {
                    callback.onTestProgress(100, "Mic test completed");
                    callback.onMicTestCompleted(result);
                });

            } catch (Exception e) {
                Log.e(TAG, "Mic test error", e);
                stopTest();
                handler.post(() -> callback.onError("Test failed: " + e.getMessage()));
            }
        }).start();
    }

    public void stopTest() {
        isTestRunning = false;
        for (AudioRecord mic : activeMics) {
            try {
                if (mic.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    mic.stop();
                }
                mic.release();
            } catch (Exception e) {
                Log.w(TAG, "Error releasing mic", e);
            }
        }
        activeMics.clear();
    }

    public interface Test3Callback {
        void onTestProgress(int progress, String message);
        // Changed to List of Lists to support multiple mics
        void onChartDataUpdate(List<List<Float>> data);
        void onMicTestCompleted(MicTestResult result);
        void onError(String error);
    }

    public static class MicTestResult {
        public int activeMicCount;
        public String status;
        public List<Float> volumeData;
    }


    public void startFakeTest(Test3Callback callback) {
        isTestRunning = true;
        // 데이터 초기화
        for (List<Float> list : allMicVolumeData) list.clear();

        handler.post(() -> callback.onTestProgress(10, "Starting Simulation..."));

        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();

                // 4개 마이크 활성화 상태로 가정
                MicTestResult result = new MicTestResult();
                result.activeMicCount = 4;
                result.status = "Simulation Running";

                while (isTestRunning && (System.currentTimeMillis() - startTime) < 10000) { // 10초 테스트
                    float timeSec = (System.currentTimeMillis() - startTime) / 1000f;

                    // --- [Fake Data Generation] ---
                    // Mic 1: 사인파 (부드럽게 오르내림)
                    float val1 = (float) (50 + 40 * Math.sin(timeSec * 2));

                    // Mic 2: 랜덤 노이즈 (말하는 것처럼 불규칙)
                    float val2 = 30f + (float)(Math.random() * 60);

                    // Mic 3: 펄스 (2초마다 켜졌다 꺼짐)
                    float val3 = (timeSec % 2 < 1.0) ? 80f : 10f;

                    // Mic 4: 톱니파 (점점 커지다가 뚝 떨어짐)
                    float val4 = (timeSec * 20) % 100;

                    // 데이터 저장
                    if (allMicVolumeData.size() >= 4) {
                        allMicVolumeData.get(0).add(Math.abs(val1));
                        allMicVolumeData.get(1).add(Math.abs(val2));
                        allMicVolumeData.get(2).add(Math.abs(val3));
                        allMicVolumeData.get(3).add(Math.abs(val4));
                    }

                    // UI 업데이트 전송 (리스트 복사해서 전송)
                    List<List<Float>> snapshot = new ArrayList<>();
                    for (List<Float> list : allMicVolumeData) {
                        snapshot.add(new ArrayList<>(list));
                    }

                    handler.post(() -> callback.onChartDataUpdate(snapshot));
                    Thread.sleep(100); // 0.1초 간격
                }

                // 테스트 종료 처리
                handler.post(() -> {
                    callback.onTestProgress(100, "Simulation Completed");
                    result.status = "Simulation Done";
                    if(!allMicVolumeData.isEmpty() && !allMicVolumeData.get(0).isEmpty()) {
                        result.volumeData = new ArrayList<>(allMicVolumeData.get(0));
                    }
                    callback.onMicTestCompleted(result);
                });

            } catch (Exception e) {
                Log.e(TAG, "Simulation error", e);
            }
            isTestRunning = false;
        }).start();
    }
}
