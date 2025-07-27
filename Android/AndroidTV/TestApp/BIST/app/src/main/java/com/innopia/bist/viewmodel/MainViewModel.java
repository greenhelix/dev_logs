package com.innopia.bist.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.innopia.bist.R;
import com.innopia.bist.util.AutoTestManager;
import com.innopia.bist.util.LogRepository;
import com.innopia.bist.util.SingleLiveEvent;
import com.innopia.bist.util.Status;
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.util.TestType;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainViewModel extends AndroidViewModel implements AutoTestManager.AutoTestListener {

    private final LogRepository logRepository = LogRepository.getInstance();
    private final AutoTestManager autoTestManager;

    private final MutableLiveData<List<String>> _logLiveData = new MutableLiveData<>();
    public final LiveData<List<String>> logLiveData = _logLiveData;

    // This LiveData holds the test results (PASSED, FAILED, etc.) for each test type.
    // MainActivity observes this to change button colors.
    private final MutableLiveData<Map<TestType, TestStatus>> _testStatusesLiveData = new MutableLiveData<>(new EnumMap<>(TestType.class));
    public final LiveData<Map<TestType, TestStatus>> testStatusesLiveData = _testStatusesLiveData;

    // This LiveData holds the hardware statuses (ON, OFF) for the top status bar.
    private final MutableLiveData<Map<TestType, Status>> _hardwareStatusLiveData = new MutableLiveData<>(new EnumMap<>(TestType.class));
    public final LiveData<Map<TestType, Status>> hardwareStatusLiveData = _hardwareStatusLiveData;

    private final MutableLiveData<String> _sysInfoLiveData = new MutableLiveData<>();
    public final LiveData<String> sysInfoLiveData = _sysInfoLiveData;

    private final MutableLiveData<String> _hwInfoLiveData = new MutableLiveData<>();
    public final LiveData<String> hwInfoLiveData = _hwInfoLiveData;

    private final MutableLiveData<Boolean> _isAutoTestRunning = new MutableLiveData<>(false);
    public final LiveData<Boolean> isAutoTestRunning = _isAutoTestRunning;

    private final SingleLiveEvent<String> _userActionRequired = new SingleLiveEvent<>();
    public final LiveData<String> userActionRequired = _userActionRequired;

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.autoTestManager = new AutoTestManager(application.getApplicationContext(), this);
        resetAllTests();
    }

    public void setSysInfo(String info) { _sysInfoLiveData.postValue(info); }
    public void setHwInfo(String info) { _hwInfoLiveData.postValue(info); }

    public void appendLog(String tag, String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
        String logMessage = String.format("[%s] %s: %s", timestamp, tag, message);
        logRepository.addLog(logMessage);
        _logLiveData.postValue(logRepository.getLogs());
    }

    public void saveLogsToFile() {
        appendLog("MainViewModel", "Logs saved to file.");
        logRepository.saveToFile();
    }

    /**
     * [NEW] Called by individual TestViewModels (via BaseTestViewModel) after a manual test is completed.
     * This is the missing link that allows manual test results to update the main UI.
     * @param type The type of the test that was run (e.g., WIFI, CPU).
     * @param status The final result of the test (e.g., PASSED, FAILED).
     */
    public void updateTestResult(TestType type, TestStatus status) {
        // Get the current map of statuses from the LiveData.
        Map<TestType, TestStatus> currentStatuses = _testStatusesLiveData.getValue();

        // Ensure the map is not null.
        if (currentStatuses != null) {
            // Update the status for the given test type.
            currentStatuses.put(type, status);
            // Post the updated map back to the LiveData to trigger observers in MainActivity.
            _testStatusesLiveData.postValue(currentStatuses);
            appendLog("MainViewModel", "Manual test result updated for " + type.name() + ": " + status.name());
        }
    }

    /**
     * Updates the live hardware status (ON/OFF), mainly for the top status bar icons.
     * @param type The hardware type.
     * @param status The new status (ON or OFF).
     */
    public void updateHardwareStatus(TestType type, Status status) {
        Map<TestType, Status> currentStatuses = _hardwareStatusLiveData.getValue();
        if (currentStatuses != null) {
            currentStatuses.put(type, status);
            _hardwareStatusLiveData.postValue(currentStatuses);
        }
    }

    public void resetAllTests() {
        appendLog("MainViewModel", "All tests and logs have been reset.");
        Map<TestType, TestStatus> initialStatuses = new EnumMap<>(TestType.class);
        for (TestType type : TestType.values()) {
            initialStatuses.put(type, TestStatus.PENDING);
        }
        _testStatusesLiveData.setValue(initialStatuses);
        logRepository.clearLogs();
        _logLiveData.postValue(logRepository.getLogs());
    }

    public void startAutoTest(String usbPath) {
        resetAllTests();
        _isAutoTestRunning.setValue(true);
        appendLog("AutoTest", "Starting auto test sequence...");
        autoTestManager.startAutoTestFromUsb(usbPath);
    }

    public void startAutoTest(boolean test) {
        resetAllTests();
        _isAutoTestRunning.setValue(true);
        appendLog("AutoTest", "Starting auto test by button ...");
        autoTestManager.startAutoTestFromRawResource(getApplication().getApplicationContext(), R.raw.test_config);
    }

    public void userActionConfirmed() {
        autoTestManager.resumeTestAfterUserAction();
    }

    @Override
    public void onTestStatusChanged(TestType type, TestStatus status, String message) {
        // This method is for AUTO-TESTS. It updates the same LiveData as the manual test method.
        updateTestResult(type, status);

        if (status == TestStatus.WAITING_FOR_USER && message != null) {
            appendLog("AutoTest", "Waiting for user action: " + message);
            _userActionRequired.postValue(message);
        } else {
            appendLog("AutoTest", "Test " + type.name() + " is now " + status.name());
        }
    }

    @Override
    public void onAllTestsCompleted() {
        appendLog("AutoTest", "All tests completed.");
        _isAutoTestRunning.postValue(false);
        saveLogsToFile();
    }

    @Override
    public void onAutoTestError(String errorMessage) {
        appendLog("AutoTest", "Error during auto test: " + errorMessage);
        _isAutoTestRunning.postValue(false);
    }
}
