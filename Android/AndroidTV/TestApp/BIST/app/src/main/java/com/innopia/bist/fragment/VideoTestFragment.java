package com.innopia.bist.fragment;

import android.app.Application;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class VideoTestFragment extends Fragment {
	private static final String TAG = "VideoTestButtonFragment";
	private VideoTestViewModel videoTestViewModel;
	private MainViewModel mainViewModel;

	private PlayerView playerView;
	private ExoPlayer player;
	private TextView tvVideoInfo;
	private LinearLayout layoutButtons;

	// [추가] 재사용할 플레이어 리스너
	private Player.Listener playerListener;

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
		setupButtonLayout();
		observeViewModel();
		return root;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initializePlayer();
	}

	private void initializePlayer() {
		player = new ExoPlayer.Builder(requireContext()).build();
		playerView.setPlayer(player);
		playerView.setControllerShowTimeoutMs(1000);

		// [수정] 리스너를 한 번만 초기화하고 플레이어에 등록합니다.
		initializePlayerListener();
		player.addListener(playerListener);
	}

	// [추가] 플레이어 리스너의 동작을 정의하는 메서드
	private void initializePlayerListener() {
		this.playerListener = new Player.Listener() {
			@Override
			public void onPlaybackStateChanged(int playbackState) {
				if (playbackState == Player.STATE_ENDED) {
					// 재생이 끝났을 때 현재 MediaItem에서 태그를 가져옵니다.
					MediaItem mediaItem = player.getCurrentMediaItem();
					if (mediaItem != null && mediaItem.localConfiguration != null) {
						VideoTestViewModel.VideoSample completedSample = (VideoTestViewModel.VideoSample) mediaItem.localConfiguration.tag;
						if (completedSample != null) {
							mainViewModel.appendLog(TAG, completedSample.getDisplayName() + " completed");
							videoTestViewModel.onVideoCompleted(completedSample.getFileName());
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
						String errorMsg = "Video play error: " + error.getMessage();
						tvVideoInfo.setText(errorMsg);
						mainViewModel.appendLog(TAG, errorMsg);
						videoTestViewModel.onVideoFailed(failedSample.getFileName());
					}
				}
			}
		};
	}

	private void playSample(VideoTestViewModel.VideoSample sample) {
		if (player == null) return;

		try {
			int resId = getResources().getIdentifier(sample.getFileName(), "raw", requireContext().getPackageName());
			Uri videoUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + resId);

			// [수정] MediaItem을 만들 때 VideoSample 객체를 태그로 설정합니다.
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
			videoTestViewModel.onVideoFailed(sample.getFileName());
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		// [수정] Fragment가 소멸될 때 등록했던 리스너를 제거하고 플레이어를 해제합니다.
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
