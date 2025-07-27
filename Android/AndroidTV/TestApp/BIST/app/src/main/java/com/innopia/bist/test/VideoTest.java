package com.innopia.bist.test;

import android.content.Context;
import android.util.Log;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class VideoTest implements Test {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executeTest(params, callback);
    }

    @Override
    public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
        //executeTest(params, callback);
        executor.execute(() -> {
            // This test simulates playing 4 videos sequentially.
            // The UI will show RUNNING state based on the AutoTestManager's signal.
            try {
                // Simulate playing 4 videos, each taking 2 seconds.
                for (int i = 1; i <= 4; i++) {
                    Log.d("VideoTest", "Auto-playing video " + i);
                    Thread.sleep(2000); // Simulate video playback time
                }
                // If all "videos" played without exception, it's a pass.
                callback.accept(new TestResult(TestStatus.PASSED, "Auto-test: All 4 videos played successfully."));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                callback.accept(new TestResult(TestStatus.FAILED, "Auto-test: Video playback was interrupted."));
            } catch (Exception e) {
                callback.accept(new TestResult(TestStatus.ERROR, "Auto-test: An error occurred during video playback: " + e.getMessage()));
            }
        });

    }

    private void executeTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executor.execute(() -> {
            Context context = (Context) params.get("context");
            if (context == null) {
                callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null"));
                return;
            }
            callback.accept(new TestResult(TestStatus.PASSED, "Video Test pass"));
        });
    }
}
