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

	// 수동 테스트 시 Fragment가 재생할 영상을 전달하기 위한 LiveData
	public final SingleLiveEvent<VideoSample> videoToPlay = new SingleLiveEvent<>();

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

	// 이 메서드는 이제 수동 테스트에서만 의미를 가집니다.
	public void onVideoStatusChanged(String fileName, TestStatus status) {
		Map<String, TestStatus> currentStatuses = _videoStatuses.getValue();
		if (currentStatuses == null) {
			currentStatuses = new HashMap<>();
		}

		// 상태가 변경된 경우에만 업데이트
		if (currentStatuses.get(fileName) == status) {
			return;
		}

		currentStatuses.put(fileName, status);
		_videoStatuses.setValue(currentStatuses);

		String statusMessage = "playback " + status.name();
		videoInfo.postValue(fileName + " " + statusMessage);

		// 수동 테스트 시 모든 영상이 통과했는지 확인
		checkOverallManualTestStatus(currentStatuses);
	}

	// 수동 테스트 시 모든 영상이 통과했는지 확인하는 로직
	private void checkOverallManualTestStatus(Map<String, TestStatus> statuses) {
		// 자동 테스트가 아닐 때만 전체 통과 여부를 검사
		if (mainViewModel.isAutoTestRunning.getValue() != null && mainViewModel.isAutoTestRunning.getValue()) {
			return;
		}

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
}
