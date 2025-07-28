package com.innopia.bist.test;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.viewmodel.VideoTestViewModel;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class VideoTest implements Test {
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@Override
	public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
//        executeTest(params, callback);
		callback.accept(new TestResult(TestStatus.PENDING, "Manual test started. Please test each video."));
	}

	@Override
	public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
		//executeTest(params, callback);
		Log.d("VideoTest", "Handing over control to UI for visible auto-test.");
//        Context context = (Context) params.get("context");
//        if (context == null) {
//            callback.accept(new TestResult(TestStatus.ERROR, "Context is null for VideoTest."));
//            return;
//        }
//
//        VideoTestViewModel.VideoSample[] samples = new VideoTestViewModel.VideoSample[] {
//                new VideoTestViewModel.VideoSample("AV1/1080p", "bunny_1080_10s_5mb_av1", "H264 1080p 10s"),
//                new VideoTestViewModel.VideoSample("H264/1080p", "bunny_1080_10s_5mb_h264", "H265 1080p 10s"),
//                new VideoTestViewModel.VideoSample("VP9/1280p", "sample_video_1280x720_1mb", "VP9 1280p 12s"),
//                new VideoTestViewModel.VideoSample("AVC/4k", "driving_mountain_4k", "AVC 4k")
//        };
//
//        for (VideoTestViewModel.VideoSample sample : samples) {
//            final CountDownLatch latch = new CountDownLatch(1);
//            final boolean[] didErrorOccur = {false};
//
//            MediaPlayer mediaPlayer = new MediaPlayer();
//            try {
//                int resId = context.getResources().getIdentifier(sample.getFileName(), "raw", context.getPackageName());
//                Uri videoUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + resId);
//                mediaPlayer.setDataSource(context, videoUri);
//
//                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
//                    Log.e("VideoTestAuto", "Error playing " + sample.getFileName() + ": what=" + what + ", extra=" + extra);
//                    didErrorOccur[0] = true;
//                    latch.countDown();
//                    return true;
//                });
//
//                mediaPlayer.setOnCompletionListener(mp -> {
//                    Log.d("VideoTestAuto", sample.getFileName() + " completed successfully.");
//                    latch.countDown();
//                });
//
//                mediaPlayer.prepareAsync();
//
//                // Wait for completion or error, with a timeout (e.g., 30 seconds).
//                if (!latch.await(30, TimeUnit.SECONDS)) {
//                    // Timeout occurred
//                    callback.accept(new TestResult(TestStatus.FAILED, "Auto-test: Timeout playing " + sample.getFileName()));
//                    mediaPlayer.release();
//                    return;
//                }
//
//                if (didErrorOccur[0]) {
//                    callback.accept(new TestResult(TestStatus.FAILED, "Auto-test: Error playing " + sample.getFileName()));
//                    mediaPlayer.release();
//                    return;
//                }
//
//            } catch (Exception e) {
//                callback.accept(new TestResult(TestStatus.ERROR, "Auto-test: Exception setting up " + sample.getFileName() + ": " + e.getMessage()));
//                return;
//            } finally {
//                mediaPlayer.release();
//            }
//            callback.accept(new TestResult(TestStatus.PASSED, "Auto-test: All 4 videos played successfully."));
//        }
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
