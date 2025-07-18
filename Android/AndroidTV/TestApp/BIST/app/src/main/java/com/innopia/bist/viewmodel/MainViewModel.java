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
        _testStatusesLiveData.setValue(initialStatuses);
    }

    public void setDeviceInfo(String info) {
        _deviceInfoLiveData.postValue(info);
    }

    public void appendLog(String tag, String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
        String logMessage = String.format("[%s] %s: %s", timestamp, tag, message);
        logRepository.addLog(logMessage);
        _logLiveData.postValue(logRepository.getLogs());
    }

    public void updateTestStatus(TestType test, Status status) {
        Map<TestType, Status> currentStatuses = _testStatusesLiveData.getValue();
        if (currentStatuses != null) {
            currentStatuses.put(test, status);
            _testStatusesLiveData.postValue(currentStatuses);
        }
    }

    public void saveLogsToFile() {
        appendLog("MainViewModel", "Logs saved to file.");
        logRepository.saveToFile();
    }

    public void resetAllTests() {
        logRepository.clearLogs();
        _logLiveData.postValue(logRepository.getLogs());
        appendLog("MainViewModel", "All tests and logs have been reset.");
    }
}
