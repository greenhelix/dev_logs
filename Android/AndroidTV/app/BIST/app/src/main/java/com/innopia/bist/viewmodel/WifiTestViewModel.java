package com.innopia.bist.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.innopia.bist.tests.wifi.WifiTest;

import java.util.function.Consumer;

// WifiTestFragment의 UI 상태와 테스트 로직을 연결
public class WifiTestViewModel extends ViewModel {

    private final WifiTest wifiTest;
    private final MutableLiveData<String> _testInfo = new MutableLiveData<>("Press 'Scan' or 'Test' to start.");
    public LiveData<String> testInfo = _testInfo;

    public WifiTestViewModel() {
        this.wifiTest = new WifiTest(); // 테스트 로직 클래스 인스턴스 생성
    }

    // Wi-Fi 스캔 시작
    public void startScan(Consumer<String> logConsumer) {
        logConsumer.accept("Starting Wi-Fi scan...");
        wifiTest.scan(result -> {
            _testInfo.postValue(result.message); // UI 업데이트
            logConsumer.accept("Scan result: " + result.message); // 로그 기록
        });
    }

    // Wi-Fi 테스트 시작
    public void startTest(Consumer<String> logConsumer, Consumer<Boolean> statusConsumer) {
        logConsumer.accept("Starting Wi-Fi test...");
        wifiTest.runTest(result -> {
            _testInfo.postValue(result.message); // UI 업데이트
            logConsumer.accept("Test result: " + result.message); // 로그 기록
            statusConsumer.accept(result.isSuccess); // MainActivity의 Wi-Fi 상태 아이콘 업데이트
        });
    }
}
