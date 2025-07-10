package com.innopia.bist.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

// MainActivity의 UI 상태(로그, 전체 상태 아이콘)를 관리
public class MainViewModel extends ViewModel {

    private final MutableLiveData<String> _logOutput = new MutableLiveData<>("Logging Start...\n");
    public LiveData<String> logOutput = _logOutput;

    private final MutableLiveData<Boolean> _wifiStatus = new MutableLiveData<>(false);
    public LiveData<Boolean> wifiStatus = _wifiStatus;

    // ... (Bluetooth, Ethernet 상태 LiveData 추가 가능)

    public void appendLog(String message) {
        String currentLog = _logOutput.getValue() != null ? _logOutput.getValue() : "";
        _logOutput.postValue(currentLog + message + "\n");
    }

    public void updateWifiStatus(boolean isConnected) {
        _wifiStatus.postValue(isConnected);
    }
}
