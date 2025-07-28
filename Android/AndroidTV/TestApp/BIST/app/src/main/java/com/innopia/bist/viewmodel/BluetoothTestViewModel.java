package com.innopia.bist.viewmodel;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.innopia.bist.test.BluetoothTest;
import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.util.TestType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BluetoothTestViewModel extends BaseTestViewModel {

    private static final String TAG = "BluetoothTestViewModel";
    private final BluetoothTest bluetoothTest;

    // LiveData for the currently selected device.
    private final MutableLiveData<BluetoothDevice> _selectedDevice = new MutableLiveData<>(null);
    public final LiveData<BluetoothDevice> selectedDevice = _selectedDevice;

    // ADDED: LiveData to hold the detailed information of the selected device.
    private final MutableLiveData<String> _deviceInfo = new MutableLiveData<>("Device Info: (Select a device to see details)");
    public final LiveData<String> deviceInfo = _deviceInfo;

    // LiveData to trigger showing the device selection dialog.
    private final MutableLiveData<List<BluetoothDevice>> _devicesForDialog = new MutableLiveData<>(null);
    public final LiveData<List<BluetoothDevice>> devicesForDialog = _devicesForDialog;

    // LiveData to trigger navigation to system settings.
    private final MutableLiveData<Boolean> _navigateToSettings = new MutableLiveData<>(false);
    public final LiveData<Boolean> navigateToSettings = _navigateToSettings;

    public BluetoothTestViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new BluetoothTest(), mainViewModel);
        this.bluetoothTest = (BluetoothTest) this.testModel;
        checkForConnectedDevicesOnStart();
    }

    public void onScanClicked() {
        String logMsg = "Scan requested. Finding connected devices...";
        mainViewModel.appendLog(getTag(), logMsg);
        Log.d(getTag(), logMsg);

        bluetoothTest.findConnectedDevices(getApplication(), devices -> {
            if (devices == null || devices.isEmpty()) {
                String emptyMsg = "No connected devices found. Requesting navigation to settings.";
                mainViewModel.appendLog(getTag(), emptyMsg);
                Log.d(getTag(), emptyMsg);
                _navigateToSettings.postValue(true);
            } else {
                String foundMsg = "Devices found. Requesting to show selection dialog.";
                mainViewModel.appendLog(getTag(), foundMsg);
                Log.d(getTag(), foundMsg);
                _devicesForDialog.postValue(devices);
            }
        });
    }

    private void checkForConnectedDevicesOnStart() {
        String logMsg = "ViewModel initialized. Checking for pre-connected devices...";
        mainViewModel.appendLog(getTag(), logMsg);
        Log.d(getTag(), logMsg);

        bluetoothTest.findConnectedDevices(getApplication(), devices -> {
            if (devices != null && !devices.isEmpty()) {
                if (devices.size() == 1) {
                    // If only one device is connected, select it automatically.
                    BluetoothDevice device = devices.get(0);
                    String autoSelectMsg = "Found one connected device. Auto-selecting: " + device.getName();
                    mainViewModel.appendLog(getTag(), autoSelectMsg);
                    Log.d(getTag(), autoSelectMsg);
                    // onDeviceSelected will handle updating LiveData for both device info and selection state.
                    onDeviceSelected(device);
                } else {
                    // If multiple devices are connected, show the selection dialog.
                    String multiDeviceMsg = "Found multiple connected devices. Prompting user for selection.";
                    mainViewModel.appendLog(getTag(), multiDeviceMsg);
                    Log.d(getTag(), multiDeviceMsg);
                    _devicesForDialog.postValue(devices);
                }
            } else {
                // If no devices are connected, do nothing. The UI will show the default message.
                String noDeviceMsg = "No pre-connected devices found.";
                mainViewModel.appendLog(getTag(), noDeviceMsg);
                Log.d(getTag(), noDeviceMsg);
            }
        });
    }

    @SuppressLint("MissingPermission")
    public void onDeviceSelected(BluetoothDevice device) {
        _selectedDevice.postValue(device);
        _testResultLiveData.postValue(new TestResult(TestStatus.PENDING, "Test Result: PENDING")); // Reset test result on new selection

        if (device != null) {

            String logMsg = "Device selected: " + device.getName() + ". Fetching details.";
            mainViewModel.appendLog(getTag(), logMsg);
            Log.d(getTag(), logMsg);
            bluetoothTest.getDeviceInfo(device, info -> _deviceInfo.postValue(info));
        } else {
            _deviceInfo.postValue("Device Info: (Select a device to see details)");
        }
    }

    public void onDialogShown() {
        _devicesForDialog.setValue(null);
    }

    public void onNavigatedToSettings() {
        _navigateToSettings.setValue(false);
    }

    @Override
    public void startTest() {
        String initialLog = "Manual test button clicked.";
        mainViewModel.appendLog(getTag(), initialLog);
        Log.d(getTag(), initialLog);

        BluetoothDevice device = _selectedDevice.getValue();
        if (device == null) {
            String noDeviceMsg = "Cannot start test: Please select a device first.";
            _testResultLiveData.postValue(new TestResult(TestStatus.ERROR, noDeviceMsg));
            mainViewModel.appendLog(getTag(), noDeviceMsg);
            Log.w(getTag(), noDeviceMsg);
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("context", getApplication().getApplicationContext());
        params.put("device", device);

        Consumer<TestResult> callback = result -> {
            _testResultLiveData.postValue(result);
            String finishMsg = "Manual test finished. Result: " + result;
            mainViewModel.appendLog(getTag(), finishMsg);
            Log.d(getTag(), finishMsg);
        };

        String startMsg = "Starting manual test for device: " + device.getName();
        mainViewModel.appendLog(getTag(), startMsg);
        Log.d(getTag(), startMsg);
        testModel.runManualTest(params, callback);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected TestType getTestType() {
        return TestType.BLUETOOTH;
    }
}
