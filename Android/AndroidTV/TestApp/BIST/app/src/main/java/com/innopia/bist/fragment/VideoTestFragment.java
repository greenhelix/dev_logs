package com.innopia.bist.fragment;

import android.app.Application;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.R;
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.VideoTestViewModel;

import java.util.List;
import java.util.Map;

public class VideoTestFragment extends Fragment {
	private static final String TAG = "VideoTestButtonFragment";
	private VideoTestViewModel videoTestViewModel;
	private MainViewModel mainViewModel;
	private VideoView videoView;
	private TextView tvVideoInfo;
	private LinearLayout layoutButtons;

	public static VideoTestFragment newInstance() {
		return new VideoTestFragment();
	}

	public static class VideoTestViewModelFactory implements androidx.lifecycle.ViewModelProvider.Factory {
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
		videoView = root.findViewById(R.id.video_view);

		setupButtonLayout();
		observeViewModel();

		return root;
	}

	private void setupButtonLayout() {
		layoutButtons.removeAllViews();
		List<VideoTestViewModel.VideoSample> samples = videoTestViewModel.getVideoSamples();
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
		// 비디오 정보 텍스트 업데이트
		videoTestViewModel.videoInfo.observe(getViewLifecycleOwner(), info -> tvVideoInfo.setText(info));

		// 각 비디오 버튼의 상태(PASSED/FAILED)에 따라 UI 변경
		videoTestViewModel.videoStatuses.observe(getViewLifecycleOwner(), statuses -> {
			int backgroundColor = 0;
			for (Map.Entry<String, com.innopia.bist.util.TestStatus> entry : statuses.entrySet()) {
				Button buttonToUpdate = layoutButtons.findViewWithTag(entry.getKey());
				GradientDrawable defaultDrawable = new GradientDrawable();
				defaultDrawable.setShape(GradientDrawable.RECTANGLE);
				defaultDrawable.setCornerRadius(4 * getResources().getDisplayMetrics().density); // 4dp
				defaultDrawable.setStroke((int) (3 * getResources().getDisplayMetrics().density), Color.BLACK);
				if (buttonToUpdate != null) {
					switch (entry.getValue()) {
						case PASSED:
							// MODIFIED: setBackgroundColor를 setBackgroundResource로 수정해야 Drawable이 적용됩니다.
//							buttonToUpdate.setBackgroundColor(getResources().getColor(R.color.green, requireContext().getTheme()));
							backgroundColor = ContextCompat.getColor(requireContext(), R.color.green);
							defaultDrawable.setColor(backgroundColor);
							break;
						case FAILED:
//							buttonToUpdate.setBackgroundColor(getResources().getColor(R.color.red, requireContext().getTheme()));
							backgroundColor = ContextCompat.getColor(requireContext(), R.color.red);
							defaultDrawable.setColor(backgroundColor);
							break;

						default:
							//buttonToUpdate.setBackgroundResource(R.drawable.button_focus_selector);
							backgroundColor = ContextCompat.getColor(requireContext(), R.color.normal);
							defaultDrawable.setColor(backgroundColor);
							break;
					}
				}
				Drawable[] focusLayers = new Drawable[2];
				focusLayers[0] = defaultDrawable; // 아래층: 색상이 적용된 배경
				focusLayers[1] = ContextCompat.getDrawable(requireContext(), R.drawable.button_state_selector); // 위층: 노란 테두리
				LayerDrawable focusDrawable = new LayerDrawable(focusLayers);

				// 4. StateListDrawable을 만들어 상태별 드로어블을 지정
				StateListDrawable stateListDrawable = new StateListDrawable();
				// 포커스 상태일 때는 LayerDrawable을 사용
				stateListDrawable.addState(new int[]{android.R.attr.state_focused}, focusDrawable);
				// 기본 상태일 때는 색상만 있는 드로어블을 사용
				stateListDrawable.addState(new int[]{}, defaultDrawable);
				buttonToUpdate.setBackground(stateListDrawable);
			}
		});

		// 자동 테스트 진행 상태 관찰
		mainViewModel.isAutoTestRunning.observe(getViewLifecycleOwner(), isRunning -> {
			if (isRunning) {
				// 자동 테스트 중에는 수동 버튼을 숨깁니다.
				layoutButtons.setVisibility(View.INVISIBLE);
				tvVideoInfo.setText("Auto-test in progress...");
				// ViewModel에 자동 테스트 시퀀스 시작을 알립니다.
				videoTestViewModel.startAutoPlaybackSequence();
			} else {
				layoutButtons.setVisibility(View.VISIBLE);
			}
		});

		// NEW: ViewModel로부터 자동 재생할 비디오를 받아 재생합니다.
		videoTestViewModel.videoToPlay.observe(getViewLifecycleOwner(), this::playSample);
	}

	private void playSample(VideoTestViewModel.VideoSample sample) {
		try {
			int resId = getResources().getIdentifier(sample.getFileName(), "raw", requireContext().getPackageName());
			Uri videoUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + resId);
			videoView.setVideoURI(videoUri);
			tvVideoInfo.setText(sample.getMetaInfo());

			videoView.setOnPreparedListener(MediaPlayer::start);
			videoView.setOnErrorListener((mp, what, extra) -> {
				String errorMsg = "Video play error: " + what + "," + extra;
				tvVideoInfo.setText(errorMsg);
				mainViewModel.appendLog(TAG, errorMsg);
				return true;
			});

			videoView.setOnCompletionListener(mp -> {
				mainViewModel.appendLog(TAG, sample.getDisplayName() + " completed");
				videoTestViewModel.onVideoCompleted(sample.getFileName());
			});

			videoView.setOnPreparedListener(mp -> mp.start());

		} catch (Exception e) {
			String errorMsg = "Can't play sample: " + e.getMessage();
			mainViewModel.appendLog(TAG, errorMsg);
			videoTestViewModel.onVideoFailed(sample.getFileName());
		}
	}
}
