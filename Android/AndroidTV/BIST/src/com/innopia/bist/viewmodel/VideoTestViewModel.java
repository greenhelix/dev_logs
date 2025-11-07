package com.innopia.bist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.util.SingleLiveEvent;
import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.util.TestType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import android.util.Log;

public class VideoTestViewModel extends BaseTestViewModel {
	public final MutableLiveData<String> videoInfo = new MutableLiveData<>("Select a video to play.");
	private static final String TAG = "BIST_VIDEO_VM";

	private final MutableLiveData<Map<String, TestStatus>> _videoStatuses = new MutableLiveData<>(new HashMap<>());
	public final LiveData<Map<String, TestStatus>> videoStatuses = _videoStatuses;
	private final MutableLiveData<VideoSample> _videoToPlay = new MutableLiveData<>();
	public final LiveData<VideoSample> videoToPlay = _videoToPlay;
	private int autoTestIndex = -1;

	public VideoTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
		super(application, null, mainViewModel);
		initializeVideoStatuses();
	}

	private void initializeVideoStatuses() {
		Map<String, TestStatus> statusMap = new HashMap<>();
		for (VideoSample sample : getVideoSamples()) {
			statusMap.put(sample.getFileName(), TestStatus.PENDING);
		}
		_videoStatuses.postValue(statusMap);
	}

	public void onVideoStatusChanged(String fileName, TestStatus status) {
		Map<String, TestStatus> currentStatuses = _videoStatuses.getValue();
		if (currentStatuses == null) currentStatuses = new HashMap<>();

		if (currentStatuses.get(fileName) == status) return;
		Map<String, TestStatus> newStatuses = new HashMap<>(currentStatuses);
		newStatuses.put(fileName, status);
		_videoStatuses.postValue(newStatuses);
		videoInfo.postValue("Playback " + fileName + ": " + status.name());

		if (autoTestIndex != -1) { // 자동 테스트 모드
			if (status == TestStatus.PASSED) {
				playNextVideoInSequence();
			} else if (status == TestStatus.FAILED || status == TestStatus.ERROR) {
				mainViewModel.onVideoTestCompleted(new TestResult(TestStatus.FAILED, "Video " + fileName + " failed."));
				autoTestIndex = -1;
			}
		} else {
			checkOverallManualTestStatus(newStatuses);
		}
	}

	public void startAutoPlaybackSequence() {
		Log.d(TAG, "startAutoPlaybackSequence called. Starting from index 0."); 
		if (autoTestIndex == -1) {
			autoTestIndex = 0;
			initializeVideoStatuses();
			playNextVideoInSequence();
		}
	}

	private void playNextVideoInSequence() {
		List<VideoSample> samples = getVideoSamples();
		if (autoTestIndex >= 0 && autoTestIndex < samples.size()) {
			_videoToPlay.postValue(samples.get(autoTestIndex));
			autoTestIndex++;
		} else {
			mainViewModel.onVideoTestCompleted(new TestResult(TestStatus.PASSED, "All videos played successfully."));
			autoTestIndex = -1;
		}
	}

	private void checkOverallManualTestStatus(Map<String, TestStatus> statuses) {
		boolean allPassed = statuses.values().stream().allMatch(s -> s == TestStatus.PASSED);
		if (allPassed) {
			mainViewModel.appendLog(getTag(), "All manual video tests have passed.");
			mainViewModel.updateTestResult(getTestType(), TestStatus.PASSED);
		}
	}

	public static class VideoSample {
		private final String displayName;
		private final String fileName;
		private final String metaInfo;
		public VideoSample(String displayName, String fileName, String metaInfo) {
			this.displayName = displayName;
			this.fileName = fileName;
			this.metaInfo = metaInfo;
		}
		public String getDisplayName() { return displayName; }
		public String getFileName() { return fileName; }
		public String getMetaInfo() { return metaInfo; }
	}

	public static List<VideoSample> getVideoSamples() {
		return Arrays.asList(
				new VideoSample("AV1", "sample_bunny_av1_1080_10s_5mb", "AV1 1080p 10s"),
				new VideoSample("H264", "sample_anim_h264_1080_10s_1mb", "H264 1080p 10s"),
				new VideoSample("H265", "sample_jellyfish_h265_1080_10s_1mb", "H265 1080p 10s"),
				new VideoSample("VP9", "sample_vp9_1080_10s_1mb", "VP9 1080p 10s")
		);
	}

	@Override
	protected String getTag() { return TAG; }

	@Override
	protected TestType getTestType() { return TestType.VIDEO; }

	public void onFragmentReady() {
		Log.d(TAG, "onFragmentReady: Fragment is ready. Checking if there is a video to play.");
		if (_videoToPlay.getValue() != null) {
			_videoToPlay.setValue(_videoToPlay.getValue());
		}
	}

	public void consumePlaybackEvent() {
		_videoToPlay.postValue(null);
	}
}
