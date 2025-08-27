package com.innopia.bist.test;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.VideoTestViewModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class VideoTest implements Test {

	private static final String TAG = "BIST_VideoTest";
	private static final long FIRST_FRAME_TIMEOUT_SECONDS = 10;

	// 자동 테스트 시 첫 프레임 렌더링 후 추가 재생 시간 (ms 단위)
	private static final long EXTRA_PLAY_DURATION_MS = 3000L;

	@Override
	public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
		// 수동 테스트는 Fragment를 통해 UI에서 직접 제어되므로 여기서는 시작 신호만 보냅니다.
		callback.accept(new TestResult(TestStatus.RUNNING, "Manual test started. Please test each video via UI."));
	}

	@Override
	public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
		Context context = (Context) params.get("context");
		MainViewModel mainViewModel = (MainViewModel) params.get("mainViewModel");

		Log.d(TAG, "[AutoTest] VideoTest Started.");

		if (context == null) {
			Log.e(TAG, "[AutoTest] Context is null. Test aborted.");
			callback.accept(new TestResult(TestStatus.ERROR, "Context is null for VideoTest."));
			return;
		}

		List<VideoTestViewModel.VideoSample> samples = VideoTestViewModel.getVideoSamples();
		boolean allTestsPassed = true;

		// 각 비디오 샘플을 순회하며 독립적인 테스트 실행
		for (int i = 0; i < samples.size(); i++) {
			VideoTestViewModel.VideoSample sample = samples.get(i);
			Log.d(TAG, String.format("[AutoTest] Starting test for video %d/%d: %s", (i + 1), samples.size(), sample.getDisplayName()));

			// 각 테스트마다 새로운 스레드와 CountDownLatch를 생성
			HandlerThread handlerThread = new HandlerThread("ExoPlayerThread-" + sample.getFileName());
			handlerThread.start();
			Looper playerLooper = handlerThread.getLooper();
			Handler playerHandler = new Handler(playerLooper);

			final CountDownLatch latch = new CountDownLatch(1);
			final AtomicBoolean wasSuccessful = new AtomicBoolean(false);
			final AtomicReference<ExoPlayer> playerRef = new AtomicReference<>();

			// ExoPlayer 생성 및 테스트 로직을 ExoPlayerThread에서 실행
			playerHandler.post(() -> {
				Log.d(TAG, "[AutoTest] ExoPlayerThread: Initializing ExoPlayer for " + sample.getDisplayName());
				try {
					ExoPlayer player = new ExoPlayer.Builder(context).setLooper(playerLooper).build();
					playerRef.set(player);

					player.addListener(new Player.Listener() {
						@Override
						public void onRenderedFirstFrame() {
							Log.d(TAG, "[AutoTest] ExoPlayerThread: onRenderedFirstFrame for " + sample.getDisplayName());
							wasSuccessful.set(true);

							if (EXTRA_PLAY_DURATION_MS > 0) {
								Log.d(TAG, "[AutoTest] ExoPlayerThread: Playing for " + EXTRA_PLAY_DURATION_MS + "ms more.");
								playerHandler.postDelayed(() -> {
									Log.d(TAG, "[AutoTest] ExoPlayerThread: Extra play duration finished. Counting down latch for " + sample.getDisplayName());
									latch.countDown();
								}, EXTRA_PLAY_DURATION_MS);
							} else {
								Log.d(TAG, "[AutoTest] ExoPlayerThread: No extra play duration. Counting down latch for " + sample.getDisplayName());
								latch.countDown();
							}
						}

						@Override
						public void onPlayerError(@NonNull PlaybackException error) {
							Log.e(TAG, "[AutoTest] ExoPlayerThread: onPlayerError for " + sample.getDisplayName() + ": " + error.getMessage());
							if (mainViewModel != null) {
								mainViewModel.appendLog(TAG, "Player error for " + sample.getDisplayName() + ": " + error.getMessage());
							}
							wasSuccessful.set(false);
							latch.countDown();
						}
					});

					int resId = context.getResources().getIdentifier(sample.getFileName(), "raw", context.getPackageName());
					Uri videoUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + resId);
					MediaItem mediaItem = MediaItem.fromUri(videoUri);

					player.setMediaItem(mediaItem);
					player.prepare();
					player.play();
					Log.d(TAG, "[AutoTest] ExoPlayerThread: Player prepared and started for " + sample.getDisplayName());

				} catch (Exception e) {
					Log.e(TAG, "[AutoTest] ExoPlayerThread: Exception during player setup for " + sample.getDisplayName(), e);
					if (mainViewModel != null) {
						mainViewModel.appendLog(TAG, "Exception for " + sample.getDisplayName() + ": " + e.getMessage());
					}
					wasSuccessful.set(false);
					latch.countDown();
				}
			});

			try {
				// AutoTestManager의 워커 스레드에서 결과를 기다림
				long totalTimeoutMs = (FIRST_FRAME_TIMEOUT_SECONDS * 1000) + EXTRA_PLAY_DURATION_MS + 2000; // 2초의 여유시간 추가
				Log.d(TAG, String.format("[AutoTest] Main test thread: Waiting for %s latch. Timeout set to %d ms.", sample.getDisplayName(), totalTimeoutMs));

				if (!latch.await(totalTimeoutMs, TimeUnit.MILLISECONDS)) {
					Log.e(TAG, "[AutoTest] Main test thread: TIMEOUT for " + sample.getDisplayName());
					if (mainViewModel != null) {
						mainViewModel.appendLog(TAG, "Timeout: Test not completed for " + sample.getDisplayName());
					}
					allTestsPassed = false;
				} else {
					Log.d(TAG, "[AutoTest] Main test thread: Latch released for " + sample.getDisplayName() + ". Success: " + wasSuccessful.get());
					if (!wasSuccessful.get()) {
						allTestsPassed = false;
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				Log.e(TAG, "[AutoTest] Main test thread: Interrupted while waiting for " + sample.getDisplayName(), e);
				if (mainViewModel != null) {
					mainViewModel.appendLog(TAG, "Test interrupted for " + sample.getDisplayName());
				}
				allTestsPassed = false;
			} finally {
				// 리소스 해제를 ExoPlayerThread에 요청
				Log.d(TAG, "[AutoTest] Main test thread: Releasing resources for " + sample.getDisplayName());
				playerHandler.post(() -> {
					ExoPlayer player = playerRef.get();
					if (player != null) {
						player.stop();
						player.release();
						Log.d(TAG, "[AutoTest] ExoPlayerThread: Player released for " + sample.getDisplayName());
					}
					if (playerLooper != null) {
						playerLooper.quitSafely();
						Log.d(TAG, "[AutoTest] ExoPlayerThread: Looper quit for " + sample.getDisplayName());
					}
				});
			}

			// 하나의 영상이라도 실패하면 전체 테스트를 중단
			if (!allTestsPassed) {
				Log.w(TAG, "[AutoTest] A test failed. Aborting remaining video tests.");
				break;
			}
		}

		// 모든 영상 테스트가 끝난 후 최종 결과 전송
		Log.d(TAG, "[AutoTest] All video tests finished. Overall result: " + (allTestsPassed ? "PASSED" : "FAILED"));
		if (allTestsPassed) {
			callback.accept(new TestResult(TestStatus.PASSED, "All video tests completed successfully."));
		} else {
			callback.accept(new TestResult(TestStatus.FAILED, "One or more videos failed the test."));
		}
	}
}
