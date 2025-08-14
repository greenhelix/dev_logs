package com.innopia.bist.fragment;

import android.app.Application;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.R;
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.VideoTestViewModel;

import java.util.List;
import java.util.Map;

// ExoPlayer 관련 클래스 임포트
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.util.DebugTextViewHelper;
import androidx.media3.ui.PlayerView;
import androidx.media3.common.VideoSize;

public class VideoTestFragment extends Fragment {
	private static final String TAG = "BIST_VideoTestFragment";
	private VideoTestViewModel videoTestViewModel;
	private MainViewModel mainViewModel;

	private PlayerView playerView;
	private ExoPlayer player;
	private TextView tvVideoInfo;
	private LinearLayout layoutButtons;
	private TextView debugTextView;
	private DebugTextViewHelper debugViewHelper;
	private final Handler playbackHandler = new Handler(Looper.getMainLooper());
	private Player.Listener playerListener;
	private static final boolean PLAY_FULL_VIDEO_MANUAL = false;
	private static final long MANUAL_PLAY_DURATION_MS = 10000L;
	private final Handler debugUpdateHandler = new Handler(Looper.getMainLooper());
	private Runnable debugUpdateRunnable;

	public static VideoTestFragment newInstance() {
		return new VideoTestFragment();
	}

	public static class VideoTestViewModelFactory implements ViewModelProvider.Factory {
		private final Application application;
		private final MainViewModel mainViewModel;

		public VideoTestViewModelFactory(Application application, MainViewModel mainViewModel) {
			this.application = application;
			this.mainViewModel = mainViewModel;
		}

		@NonNull
		@Override
		public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
			if (modelClass.isAssignableFrom(VideoTestViewModel.class)) {
				return (T) new VideoTestViewModel(application, mainViewModel);
			}
			throw new IllegalArgumentException("Unknown ViewModel class");
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_video_test, container, false);
		mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
		VideoTestViewModelFactory factory = new VideoTestViewModelFactory(requireActivity().getApplication(), mainViewModel);
		videoTestViewModel = new ViewModelProvider(this, factory).get(VideoTestViewModel.class);
		tvVideoInfo = root.findViewById(R.id.tv_video_info_button);
		layoutButtons = root.findViewById(R.id.layout_video_buttons);
		playerView = root.findViewById(R.id.player_view);
		debugTextView = root.findViewById(R.id.debug_text_view);

		setupButtonLayout();
		observeViewModel();
		return root;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initializePlayer();
		debugUpdateHandler.post(debugUpdateRunnable);
	}

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer() {
		player = new ExoPlayer.Builder(requireContext()).build();
		playerView.setPlayer(player);
		playerView.setUseController(false);
//		playerView.setControllerShowTimeoutMs(1000);

		debugViewHelper = new DebugTextViewHelper(player, debugTextView);
		debugViewHelper.start();

		initializePlayerListener();
		player.addListener(playerListener);
	}
	private String formatDebugString(String originalString) {
		if (originalString == null) return "";
		// " r:", " colr:" 등 특정 키워드 앞에 줄바꿈 문자를 추가하여 가독성을 높입니다.
		return originalString
			.replace(" r:", "\nr:")
			.replace(" colr:", "\ncolr:")
			.replace(" sib:", "\nsib:")
			.replace(" decoder:", "\ndecoder:");
	}
	private void initializePlayerListener() {
		this.playerListener = new Player.Listener() {

			@Override
			public void onVideoSizeChanged(VideoSize videoSize) {
				MediaItem mediaItem = player.getCurrentMediaItem();
				if (mediaItem != null && mediaItem.localConfiguration != null) {
					VideoTestViewModel.VideoSample sample = (VideoTestViewModel.VideoSample) mediaItem.localConfiguration.tag;
					if (sample != null) {
						String specInfo = "Spec: " + videoSize.width + "x" + videoSize.height;
						mainViewModel.appendLog(TAG, sample.getDisplayName() + " - " + specInfo);
					}
				}
			}

			@Override
			public void onRenderedFirstFrame() {
				MediaItem mediaItem = player.getCurrentMediaItem();
				if (mediaItem == null || mediaItem.localConfiguration == null) return;
				VideoTestViewModel.VideoSample sample = (VideoTestViewModel.VideoSample) mediaItem.localConfiguration.tag;
				if (sample == null) return;

				// 이미 통과된 항목은 중복 처리 방지
				Map<String, TestStatus> currentStatuses = videoTestViewModel.videoStatuses.getValue();
				if (currentStatuses != null && currentStatuses.get(sample.getFileName()) == TestStatus.PASSED) {
					return;
				}

				String logMsg = "I-Frame detected, PASSED: " + sample.getDisplayName();
				mainViewModel.appendLog(TAG, logMsg);
				videoTestViewModel.onVideoStatusChanged(sample.getFileName(), TestStatus.PASSED);

				// [수정] 설정 값에 따라 재생 정지 정책을 분기
				if (PLAY_FULL_VIDEO_MANUAL) {
					// 옵션 1: 전체 재생 (아무것도 하지 않으면 끝까지 재생됨)
					mainViewModel.appendLog(TAG, "Playing full video for " + sample.getDisplayName());
				} else {
					// 옵션 2: 지정된 시간(10초)만큼 추가 재생 후 정지
					mainViewModel.appendLog(TAG, "Playing for an extra " + (MANUAL_PLAY_DURATION_MS / 1000) + "s.");
					playbackHandler.postDelayed(() -> {
						if (player != null) {
							mainViewModel.appendLog(TAG, sample.getDisplayName() + " stopped after 3s.");
							player.stop();
						}
					}, MANUAL_PLAY_DURATION_MS);
				}
			}

			@Override
			public void onPlaybackStateChanged(int playbackState) {
				if (playbackState == Player.STATE_ENDED) {
					MediaItem mediaItem = player.getCurrentMediaItem();
					if (mediaItem != null && mediaItem.localConfiguration != null) {
						VideoTestViewModel.VideoSample completedSample = (VideoTestViewModel.VideoSample) mediaItem.localConfiguration.tag;
						if (completedSample != null) {
							mainViewModel.appendLog(TAG, completedSample.getDisplayName() + " playback completed.");
						}
					}
				}
			}

			@Override
			public void onPlayerError(@NonNull PlaybackException error) {
				// 에러가 발생했을 때 현재 MediaItem에서 태그를 가져옵니다.
				MediaItem mediaItem = player.getCurrentMediaItem();
				if (mediaItem != null && mediaItem.localConfiguration != null) {
					VideoTestViewModel.VideoSample failedSample = (VideoTestViewModel.VideoSample) mediaItem.localConfiguration.tag;
					if (failedSample != null) {
						String errorMsg = "Video play error for " + failedSample.getDisplayName() + ": " + error.getMessage();
						tvVideoInfo.setText(errorMsg);
						mainViewModel.appendLog(TAG, errorMsg);
						videoTestViewModel.onVideoStatusChanged(failedSample.getFileName(), TestStatus.FAILED);
					}
				}
			}
		};
	}

	private void playSample(VideoTestViewModel.VideoSample sample) {
		if (player == null) return;

		// 새 영상 재생 전, 예약된 'stop' 명령이 있다면 취소
		playbackHandler.removeCallbacksAndMessages(null);

		try {
			int resId = getResources().getIdentifier(sample.getFileName(), "raw", requireContext().getPackageName());
			Uri videoUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + resId);

			MediaItem mediaItem = new MediaItem.Builder()
				.setUri(videoUri)
				.setTag(sample) // 이 태그를 통해 리스너가 어떤 영상인지 식별합니다.
				.build();

			player.setMediaItem(mediaItem);
			player.prepare();
			player.play();

			tvVideoInfo.setText(sample.getMetaInfo());

		} catch (Exception e) {
			String errorMsg = "Can't play sample: " + e.getMessage();
			mainViewModel.appendLog(TAG, errorMsg);
			videoTestViewModel.onVideoStatusChanged(sample.getFileName(), TestStatus.ERROR);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		// Fragment 종료 시 핸들러 콜백 제거 및 디버그 헬퍼, 플레이어 자원 해제
		playbackHandler.removeCallbacksAndMessages(null);
		if (player != null) {
			if (playerListener != null) {
				player.removeListener(playerListener);
			}
			player.release();
			player = null;
		}
	}

	// --- 아래 코드는 변경 사항 없습니다. ---

	private void setupButtonLayout() {
		layoutButtons.removeAllViews();
		List<VideoTestViewModel.VideoSample> samples = videoTestViewModel.getVideoSamples();
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
		for (VideoTestViewModel.VideoSample sample : samples) {
			Button btnSample = new Button(getContext());
			btnSample.setText(sample.getDisplayName());
			btnSample.setTag(sample.getFileName());
			btnSample.setLayoutParams(params);
			btnSample.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
			btnSample.setOnClickListener(v -> playSample(sample));
			layoutButtons.addView(btnSample);
		}
	}

	private void observeViewModel() {
		videoTestViewModel.videoInfo.observe(getViewLifecycleOwner(), info -> tvVideoInfo.setText(info));
		videoTestViewModel.videoStatuses.observe(getViewLifecycleOwner(), statuses -> {
			for (Map.Entry<String, TestStatus> entry : statuses.entrySet()) {
				Button buttonToUpdate = layoutButtons.findViewWithTag(entry.getKey());
				if (buttonToUpdate != null) {
					updateButtonBackground(buttonToUpdate, entry.getValue());
				}
			}
		});
		mainViewModel.isAutoTestRunning.observe(getViewLifecycleOwner(), isRunning -> {
			if (isRunning) {
				layoutButtons.setVisibility(View.INVISIBLE);
				tvVideoInfo.setText("Auto-test in progress...");
				videoTestViewModel.startAutoPlaybackSequence();
			} else {
				layoutButtons.setVisibility(View.VISIBLE);
			}
		});
		videoTestViewModel.videoToPlay.observe(getViewLifecycleOwner(), this::playSample);
	}

	private void updateButtonBackground(Button button, TestStatus status) {
		GradientDrawable defaultDrawable = new GradientDrawable();
		defaultDrawable.setShape(GradientDrawable.RECTANGLE);
		defaultDrawable.setCornerRadius(4 * getResources().getDisplayMetrics().density);
		defaultDrawable.setStroke((int) (3 * getResources().getDisplayMetrics().density), Color.BLACK);
		int backgroundColor;
		switch (status) {
			case PASSED:
				backgroundColor = ContextCompat.getColor(requireContext(), R.color.green);
				break;
			case FAILED:
				backgroundColor = ContextCompat.getColor(requireContext(), R.color.red);
				break;
			default:
				backgroundColor = ContextCompat.getColor(requireContext(), R.color.normal);
				break;
		}
		defaultDrawable.setColor(backgroundColor);
		Drawable[] focusLayers = new Drawable[2];
		focusLayers[0] = defaultDrawable;
		focusLayers[1] = ContextCompat.getDrawable(requireContext(), R.drawable.button_state_selector);
		LayerDrawable focusDrawable = new LayerDrawable(focusLayers);
		StateListDrawable stateListDrawable = new StateListDrawable();
		stateListDrawable.addState(new int[]{android.R.attr.state_focused}, focusDrawable);
		stateListDrawable.addState(new int[]{}, defaultDrawable);
		button.setBackground(stateListDrawable);
	}
}
