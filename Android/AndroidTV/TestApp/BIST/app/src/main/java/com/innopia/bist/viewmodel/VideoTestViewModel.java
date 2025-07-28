package com.innopia.bist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.test.VideoTest;
import com.innopia.bist.util.SingleLiveEvent;
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.util.TestType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoTestViewModel extends BaseTestViewModel {
	public final MutableLiveData<String> videoInfo = new MutableLiveData<>("Select a video to play.");
	private static final String TAG = "BIST_VIDEO_VM";

	private final MutableLiveData<Map<String, TestStatus>> _videoStatuses = new MutableLiveData<>(new HashMap<>());
	public final LiveData<Map<String, TestStatus>> videoStatuses = _videoStatuses;
	public final SingleLiveEvent<VideoSample> videoToPlay = new SingleLiveEvent<>();
	private int autoTestIndex = -1;

	public VideoTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
		super(application, new VideoTest(), mainViewModel);
		initializeVideoStatuses();
	}

	private void initializeVideoStatuses() {
		Map<String, TestStatus> statusMap = new HashMap<>();
		for (VideoSample sample : getVideoSamples()) {
			statusMap.put(sample.getFileName(), TestStatus.PENDING);
		}
		_videoStatuses.setValue(statusMap);
	}

	private void updateVideoStatus(String fileName, TestStatus newStatus) {
		Map<String, TestStatus> currentStatuses = _videoStatuses.getValue();
		if (currentStatuses == null) currentStatuses = new HashMap<>();

		currentStatuses.put(fileName, newStatus);
		_videoStatuses.setValue(currentStatuses);

		// Check if all manual tests have passed.
		checkOverallManualTestStatus(currentStatuses);
	}

	public void onVideoCompleted(String videoFileName) {
		updateVideoStatus(videoFileName, TestStatus.PASSED);
		videoInfo.postValue(videoFileName + " playback PASSED.");
		if (autoTestIndex != -1) {
			playNextVideoInSequence();
		}
	}

	public void onVideoFailed(String videoFileName) {
		updateVideoStatus(videoFileName, TestStatus.FAILED);
		videoInfo.postValue(videoFileName + " playback FAILED.");
		if (autoTestIndex != -1) {
			mainViewModel.updateTestResult(getTestType(), TestStatus.FAILED);
			autoTestIndex = -1; // 자동 테스트 종료
		}
	}

	public void startAutoPlaybackSequence() {
		if (autoTestIndex == -1) { // 중복 실행 방지
			autoTestIndex = 0;
			playNextVideoInSequence();
		}
	}

	private void playNextVideoInSequence() {
		List<VideoSample> samples = getVideoSamples();
		if (autoTestIndex >= 0 && autoTestIndex < samples.size()) {
			// 재생할 비디오를 LiveEvent를 통해 Fragment에 전달
			videoToPlay.postValue(samples.get(autoTestIndex));
			autoTestIndex++;
		} else {
			// 모든 비디오 재생 완료. 전체 테스트를 PASSED 처리
			mainViewModel.updateTestResult(getTestType(), TestStatus.PASSED);
			autoTestIndex = -1; // 자동 테스트 종료
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

	public List<VideoSample> getVideoSamples() {
		return Arrays.asList(
				new VideoSample("AV1/1080p", "bunny_1080_10s_5mb_av1", "H264 1080p 10s"),
				new VideoSample("H264/1080p", "bunny_1080_10s_5mb_h264", "H265 1080p 10s"),
				new VideoSample("VP9/1280p", "sample_video_1280x720_1mb", "VP9 1280p 12s"),
				new VideoSample("AVC/4k", "driving_mountain_4k", "AVC 4k")
		);
	}

	@Override
	protected String getTag() { return TAG; }

	@Override
	protected TestType getTestType() { return TestType.VIDEO; }


}
