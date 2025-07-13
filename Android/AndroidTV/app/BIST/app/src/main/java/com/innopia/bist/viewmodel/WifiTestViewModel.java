package com.innopia.bist.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.innopia.bist.tests.TestResult;
import com.innopia.bist.tests.wifi.WifiDetails;
import com.innopia.bist.tests.wifi.WifiTest;
import java.util.function.Consumer;

public class WifiTestViewModel extends ViewModel {

    private final WifiTest wifiTest;
    private final MutableLiveData<String> _displayInfo = new MutableLiveData<>("Press 'Scan' to connect to a Wi-Fi network.");
    public LiveData<String> displayInfo = _displayInfo;

    public WifiTestViewModel() {
        this.wifiTest = new WifiTest();
    }

    /**
     * 요구사항 반영: Wi-Fi 정보를 조회하고 UI에 표시할 문자열로 가공합니다.
     * 로그를 남기지 않고, 순수하게 화면 정보만 업데이트합니다.
     */
    public void updateWifiInfo(Context context) {
        WifiDetails details = wifiTest.getCurrentWifiInfo(context);
        String infoText;
        if (details.isConnected) {
            infoText = "SSID: " + details.ssid + "\n" +
                    "BSSID: " + details.bssid + "\n" +
                    "MAC: " + details.macAddress + "\n" +
                    "RSSI: " + details.rssi + " dBm\n" +
                    "Link Speed: " + details.linkSpeed + " Mbps";
        } else {
            infoText = "Wi-Fi is not connected.";
        }
        _displayInfo.postValue(infoText);
    }

    /**
     * Ping 테스트를 시작하고, 결과 텍스트를 UI에 표시합니다.
     * 실행 과정과 결과는 '로그'로 전달합니다.
     */
    public void startPingTest(Consumer<String> logConsumer, Consumer<Boolean> statusConsumer) {
        logConsumer.accept("Starting Wi-Fi Ping test..."); // 로그창에만 표시
        wifiTest.runPingTest(result -> {
            logConsumer.accept("Ping Test Result: " + result.message); // 로그창에 결과 기록
            statusConsumer.accept(result.isSuccess);
        });
    }

    /**
     * 자동 테스트를 시작하고, 결과 텍스트를 UI에 표시합니다.
     */
    public void startAutoTest(Consumer<String> logConsumer, Consumer<Boolean> statusConsumer) {
        logConsumer.accept("Starting Wi-Fi Auto Test..."); // 로그창에만 표시
        wifiTest.runPingTest(result -> {
            _displayInfo.postValue("Auto Test Result: " + result.message); // 정보창에 결과 표시
            logConsumer.accept("Auto Test Result: " + result.message); // 로그창에 결과 기록
            statusConsumer.accept(result.isSuccess);
        });
    }
}
