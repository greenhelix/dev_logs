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
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.ui.PlayerView;
import androidx.media3.common.util.UnstableApi;

import com.innopia.bist.R;
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.VideoTestViewModel;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VideoTestFragment extends Fragment {
	private static final String TAG = "BIST_VideoTestFragment";
	private VideoTestViewModel videoTestViewModel;
	private MainViewModel mainViewModel;
	private PlayerView playerView;
	private ExoPlayer player;
	private TextView tvVideoInfo;
	private LinearLayout layoutButtons;
	private TextView debugTextView;
	private final Handler playbackHandler = new Handler(Looper.getMainLooper());
	private Player.Listener playerListener;
	private static final long PLAY_DURATION_MS = 3000L;
	private AnalyticsListener analyticsListener;
	private final Handler debugUpdateHandler = new Handler(Looper.getMainLooper());
	private Runnable debugUpdateRunnable;
	private String videoFormatString = "N/A";
	private String videoDecoderName = "N/A";
	private int droppedFrames = 0;

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
		VideoTestViewModelFactory factory = new VideoTestViewModelFactory(
				requireActivity().getApplication(),
				mainViewModel
		);
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
		Log.d(TAG, "onViewCreated: Fragment view has been created. Initializing player.");
		initializePlayer();
		debugUpdateHandler.post(debugUpdateRunnable);
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume: Fragment is now active. Notifying ViewModel.");
		videoTestViewModel.onFragmentReady();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		playbackHandler.removeCallbacksAndMessages(null);
		debugUpdateHandler.removeCallbacksAndMessages(null);

		if (player != null) {
			if (analyticsListener != null) {
				player.removeAnalyticsListener(analyticsListener);
			}
			player.release();
			player = null;
		}
	}

	private void setupButtonLayout() {
		layoutButtons.removeAllViews();
		List<VideoTestViewModel.VideoSample> samples = VideoTestViewModel.getVideoSamples();
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1);
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
			} else {
				layoutButtons.setVisibility(View.VISIBLE);
				tvVideoInfo.setText("Select a video to play.");
			}
		});
		mainViewModel.startVideoPlaybackSequence.observe(getViewLifecycleOwner(), aVoid -> {
			mainViewModel.appendLog(TAG, "startVideoPlaybackSequence event received. Starting playback sequence in ViewModel.");
			videoTestViewModel.startAutoPlaybackSequence();
		});

		videoTestViewModel.videoToPlay.observe(getViewLifecycleOwner(), this::playSample);
	}

	private void playSample(VideoTestViewModel.VideoSample sample) {
		if (player == null) {
			Log.e(TAG, "playSample: Player is null!");
			return;
		}

		Log.d(TAG, "playSample: Attempting to play " + sample.getDisplayName());
		playbackHandler.removeCallbacksAndMessages(null);

		try {
			int resId = getResources().getIdentifier(sample.getFileName(), "raw", requireContext().getPackageName());
			if (resId == 0) {
				Log.e(TAG, "Video resource not found for: " + sample.getFileName());
				videoTestViewModel.onVideoStatusChanged(sample.getFileName(), TestStatus.ERROR);
				return;
			}

			Uri videoUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + resId);
			MediaItem mediaItem = new MediaItem.Builder().setUri(videoUri).setTag(sample).build();
			player.setMediaItem(mediaItem);
			player.prepare();
			player.play();
			tvVideoInfo.setText("Playing: " + sample.getMetaInfo());
		} catch (Exception e) {
			Log.e(TAG, "Exception during playSample.", e);
			videoTestViewModel.onVideoStatusChanged(sample.getFileName(), TestStatus.ERROR);
		}
	}

	private void initializePlayer() {
		player = new ExoPlayer.Builder(requireContext()).build();
		playerView.setPlayer(player);
		playerView.setUseController(false);

		initializeAnalyticsListener();
		initializePlayerListener();
		player.addAnalyticsListener(analyticsListener);
		player.addListener(playerListener);

		this.debugUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				if (player != null && debugTextView != null) {
					StringBuilder debugInfo = new StringBuilder();
					debugInfo.append(String.format(Locale.US, "playWhenReady:%s playbackState:%s\n",
					player.getPlayWhenReady(), getPlayerStateString(player.getPlaybackState())));

					debugInfo.append("video_info:")
					.append("\n- format: ").append(videoFormatString)
					.append("\n- decoder: ").append(videoDecoderName)
					.append("\n- dropped: ").append(droppedFrames);

					debugInfo.append(String.format(Locale.US, "\nbuffer: %d%%", player.getBufferedPercentage()));

					debugTextView.setText(debugInfo.toString());
					debugUpdateHandler.postDelayed(this, 1000);
				}
			}
		};
	}

	private void initializePlayerListener() {
		this.playerListener = new Player.Listener() {
			@Override
			public void onRenderedFirstFrame() {
				Log.d(TAG, "onRenderedFirstFrame: First frame has been rendered!");
				MediaItem mediaItem = player.getCurrentMediaItem();
				if (mediaItem == null || mediaItem.playbackProperties == null) return;
				VideoTestViewModel.VideoSample sample = (VideoTestViewModel.VideoSample) mediaItem.playbackProperties.tag;
				if (sample == null) return;

				Map<String, TestStatus> currentStatuses = videoTestViewModel.videoStatuses.getValue();
				if (currentStatuses != null && currentStatuses.get(sample.getFileName()) == TestStatus.PASSED) {
					Log.w(TAG, "onRenderedFirstFrame: " + sample.getFileName() + " already marked as PASSED. Ignoring.");
					return;
				}

				mainViewModel.appendLog(TAG, "I-Frame detected for " + sample.getDisplayName() + ". Waiting " + PLAY_DURATION_MS + "ms to confirm.");

				playbackHandler.postDelayed(() -> {
					mainViewModel.appendLog(TAG, sample.getDisplayName() + " playback finished.");
					videoTestViewModel.onVideoStatusChanged(sample.getFileName(), TestStatus.PASSED);
				}, PLAY_DURATION_MS);
			}

			@Override
			public void onPlayerError(@NonNull PlaybackException error) {
				Log.e(TAG, "onPlayerError: Critical player error.", error);
				MediaItem mediaItem = player.getCurrentMediaItem();
				if (mediaItem == null || mediaItem.playbackProperties == null) return;

				VideoTestViewModel.VideoSample failedSample = (VideoTestViewModel.VideoSample) mediaItem.playbackProperties.tag;
				if (failedSample != null) {
					String errorMsg = "Video play error for " + failedSample.getDisplayName() + ": " + error.getMessage();
					mainViewModel.appendLog(TAG, errorMsg);
					videoTestViewModel.onVideoStatusChanged(failedSample.getFileName(), TestStatus.FAILED);
				}
			}
		};
	}

	@OptIn(markerClass = UnstableApi.class)
	private void initializeAnalyticsListener() {
		this.analyticsListener = new AnalyticsListener() {
			@Override
			public void onTracksChanged(EventTime eventTime, Tracks tracks) {
				for (Tracks.Group trackGroupInfo : tracks.getGroups()) {
					if (trackGroupInfo.isSelected()) {
						for (int i = 0; i < trackGroupInfo.length; i++) {
							if(trackGroupInfo.isTrackSelected(i)) {
								Format videoFormat = trackGroupInfo.getTrackFormat(i);
								videoFormatString = String.format(Locale.US, "\n    W x H: %dx%d\n    MIME: %s", videoFormat.width, videoFormat.height, videoFormat.sampleMimeType);
								return;
							}
						}
					}
				}
			}

			@Override
			public void onDroppedVideoFrames(@NonNull AnalyticsListener.EventTime eventTime, int droppedFrames, long elapsedMs) {
				VideoTestFragment.this.droppedFrames += droppedFrames;
			}

			@Override
			public void onPlaybackStateChanged(@NonNull EventTime eventTime, int state) {
				if (state == Player.STATE_IDLE || state == Player.STATE_ENDED) {
					droppedFrames = 0;
				}
			}
		};
	}

	private String getPlayerStateString(int state) {
		switch (state) {
			case Player.STATE_IDLE: return "IDLE";
			case Player.STATE_BUFFERING: return "BUFFERING";
			case Player.STATE_READY: return "READY";
			case Player.STATE_ENDED: return "ENDED";
			default: return "UNKNOWN";
		}
	}

	private void updateButtonBackground(Button button, TestStatus status) {
		GradientDrawable defaultDrawable = new GradientDrawable();
		defaultDrawable.setShape(GradientDrawable.RECTANGLE);
		defaultDrawable.setCornerRadius(4 * getResources().getDisplayMetrics().density);
		defaultDrawable.setStroke((int) (3 * getResources().getDisplayMetrics().density), Color.BLACK);
		int backgroundColor;

		switch (status) {
			case PASSED: backgroundColor = ContextCompat.getColor(requireContext(), R.color.green); break;
			case FAILED: backgroundColor = ContextCompat.getColor(requireContext(), R.color.red); break;
			default: backgroundColor = ContextCompat.getColor(requireContext(), R.color.normal); break;
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
