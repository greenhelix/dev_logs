package com.innopia.bist.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.innopia.bist.util.LogRepository;
import com.innopia.bist.util.Status;
import com.innopia.bist.util.TestType;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainViewModel extends ViewModel {
    private final LogRepository logRepository = LogRepository.getInstance();

    private final MutableLiveData<List<String>> _logLiveData = new MutableLiveData<>();
    public final LiveData<List<String>> logLiveData = _logLiveData;

    private final MutableLiveData<Map<TestType, Status>> _testStatusesLiveData = new MutableLiveData<>(new EnumMap<>(TestType.class));
    public final LiveData<Map<TestType, Status>> testStatusesLiveData = _testStatusesLiveData;

    private final MutableLiveData<String> _deviceInfoLiveData = new MutableLiveData<>();
    public final LiveData<String> deviceInfoLiveData = _deviceInfoLiveData;

    public MainViewModel() {
        Map<TestType, Status> initialStatuses = new EnumMap<>(TestType.class);
        for (TestType type : TestType.values()) {
            initialStatuses.put(type, Status.OFF);
        }
        _testStatusesLiveData.setValue(initialStatuses); // 앱 시작 시 초기값 설정은 메인 스레드이므로 setValue 사용 가능
    }

    public void setDeviceInfo(String info) {
        _deviceInfoLiveData.postValue(info);
    }

    /**
     * 로그를 추가하고 LiveData를 업데이트합니다.
     * 백그라운드 스레드에서 호출될 수 있으므로 postValue()를 사용합니다.
     */
    public void appendLog(String tag, String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
        String logMessage = String.format("[%s] %s: %s", timestamp, tag, message);

        logRepository.addLog(logMessage);
        _logLiveData.postValue(logRepository.getLogs()); // setValue -> postValue 변경
    }

    /**
     * 테스트 상태를 업데이트합니다.
     * 백그라운드 스레드에서 호출될 수 있으므로 postValue()를 사용합니다.
     */
    public void updateTestStatus(TestType test, Status status) {
        Map<TestType, Status> currentStatuses = _testStatusesLiveData.getValue();
        if (currentStatuses != null) {
            currentStatuses.put(test, status);
            _testStatusesLiveData.postValue(currentStatuses); // setValue -> postValue 변경
        }
    }

    public void saveLogsToFile() {
        // 이 작업은 UI 스레드에서 시작될 수 있지만, 로그 추가는 스레드 안전하게 처리
        appendLog("MainViewModel", "Logs saved to file.");
        logRepository.saveToFile();
    }

    public void resetAllTests() {
        logRepository.clearLogs();
        _logLiveData.postValue(logRepository.getLogs());
        appendLog("MainViewModel", "All tests and logs have been reset.");
    }
}
