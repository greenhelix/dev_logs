package com.innopia.bist.test;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
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
import java.util.function.Consumer;

public class VideoTest implements Test {
	private static final String TAG = "BIST_VideoTest";
	private static final long FIRST_FRAME_TIMEOUT_SECONDS = 10;
	private static final long EXTRA_PLAY_DURATION_MS = 0L;
	@Override
	public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
		// 수동 테스트는 프래그먼트에서 UI와 함께 진행되므로 PENDING 상태로 시작
		callback.accept(new TestResult(TestStatus.PENDING, "Manual test started. Please test each video via UI."));
	}

	@OptIn(markerClass = UnstableApi.class)
	@Override
	public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
		Context context = (Context) params.get("context");
		MainViewModel mainViewModel = (MainViewModel) params.get("mainViewModel"); // ViewModel을 AutoTestManager에서 전달받는다고 가정

		if (context == null) {
			callback.accept(new TestResult(TestStatus.ERROR, "Context is null for VideoTest."));
			return;
		}

		// ExoPlayer는 Looper가 있는 스레드에서 생성/사용되어야 합니다.
		HandlerThread handlerThread = new HandlerThread("ExoPlayerThread");
		handlerThread.start();
		Looper playerLooper = handlerThread.getLooper();
		Handler playerHandler = new Handler(playerLooper);

		playerHandler.post(() -> {
			List<VideoTestViewModel.VideoSample> samples = VideoTestViewModel.getVideoSamples();
			boolean allTestsPassed = true;

			for (VideoTestViewModel.VideoSample sample : samples) {
				final CountDownLatch latch = new CountDownLatch(1);
				final AtomicBoolean wasSuccessful = new AtomicBoolean(false);
				ExoPlayer player = null;

				try {
					player = new ExoPlayer.Builder(context).setLooper(playerLooper).build();

					int resId = context.getResources().getIdentifier(sample.getFileName(), "raw", context.getPackageName());
					Uri videoUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + resId);
					MediaItem mediaItem = MediaItem.fromUri(videoUri);

					final ExoPlayer finalPlayer = player;
					player.addListener(new Player.Listener() {
						@Override
						public void onRenderedFirstFrame() {
							String logMsg = "I-Frame detected for: " + sample.getDisplayName();
							Log.d(TAG, logMsg);
							if (mainViewModel != null) {
								mainViewModel.appendLog(TAG, logMsg);
							}
							finalPlayer.stop();
							wasSuccessful.set(true);
							try {
								if (EXTRA_PLAY_DURATION_MS > 0 )  {
									Thread.sleep(EXTRA_PLAY_DURATION_MS);
								}
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}finally {
								finalPlayer.stop();
								latch.countDown();
							}
						}

						@Override
						public void onPlayerError(PlaybackException error) {
							String errorMsg = "Player error for " + sample.getDisplayName() + ": " + error.getMessage();
							Log.e(TAG, errorMsg);
							if (mainViewModel != null) {
								mainViewModel.appendLog(TAG, errorMsg);
							}
							latch.countDown();
						}
					});

					player.setMediaItem(mediaItem);
					player.prepare();
					player.play(); // 재생을 시작해야 프레임을 렌더링합니다.

					// 지정된 시간 동안 첫 프레임 렌더링 또는 에러를 기다립니다.
					if (!latch.await(FIRST_FRAME_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
						String timeoutMsg = "Timeout: First frame not rendered within " + FIRST_FRAME_TIMEOUT_SECONDS + "s for " + sample.getDisplayName();
						Log.e(TAG, timeoutMsg);
						if (mainViewModel != null) {
							mainViewModel.appendLog(TAG, timeoutMsg);
						}
						// 타임아웃도 실패로 간주
						allTestsPassed = false;
						break; // 다음 비디오 테스트 중지
					}

					if (!wasSuccessful.get()) {
						// 에러가 발생했거나 타임아웃된 경우
						allTestsPassed = false;
						break; // 다음 비디오 테스트 중지
					}
					// 성공적으로 첫 프레임이 렌더링되면 다음 비디오로 넘어갑니다.

				} catch (Exception e) {
					String exceptionMsg = "Exception during video test for " + sample.getDisplayName() + ": " + e.getMessage();
					Log.e(TAG, exceptionMsg, e);
					if (mainViewModel != null) {
						mainViewModel.appendLog(TAG, exceptionMsg);
					}
					allTestsPassed = false;
					break;
				} finally {
					if (player != null) {
						player.release();
					}
				}
			}

			// 모든 테스트가 끝난 후 최종 결과를 콜백으로 전달
			if (allTestsPassed) {
				callback.accept(new TestResult(TestStatus.PASSED, "All video I-Frames rendered successfully."));
			} else {
				callback.accept(new TestResult(TestStatus.FAILED, "One or more videos failed the I-Frame check."));
			}

			// 스레드를 정리합니다.
			playerLooper.quit();
		});
	}
}
