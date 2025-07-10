package com.innopia.bist.tests.wifi;

public class WifiDetails {
    public final boolean isConnected;
    public final String ssid;
    public final String bssid;
    public final String macAddress;
    public final int rssi; // 수신 상태 (신호 강도)
    public final int linkSpeed; // 연결 속도

    // 연결되지 않았을 때 사용할 생성자
    public WifiDetails() {
        this.isConnected = false;
        this.ssid = "N/A";
        this.bssid = "N/A";
        this.macAddress = "N/A";
        this.rssi = 0;
        this.linkSpeed = 0;
    }

    // 연결되었을 때 사용할 생성자
    public WifiDetails(String ssid, String bssid, String macAddress, int rssi, int linkSpeed) {
        this.isConnected = true;
        this.ssid = ssid;
        this.bssid = bssid;
        this.macAddress = macAddress;
        this.rssi = rssi;
        this.linkSpeed = linkSpeed;
    }
}