package com.innopia.bist.util;

import org.json.JSONObject;

public class TestConfig {
    public static class Wifi {
        public String id;
        public String pw;

        public Wifi(JSONObject json) {
            this.id = json.optString("id", null);
            this.pw = json.optString("pw", null);
        }
    }

    public static class Ethernet {
        public String ip;
        public String mask;
        public String gateway;
        public String dns;
        public Ethernet(JSONObject json) {
            this.ip = json.optString("ip", null);
            this.mask = json.optString("mask", null);
            this.gateway = json.optString("gateway", null);
            this.dns = json.optString("dns", null);
        }
    }

    public static class Bluetooth {
        public String name;

        public Bluetooth(JSONObject json) {
            this.name = json.optString("name", null);
        }
    }

    public Wifi wifiConfig;
    public Ethernet ethernetConfig;
    public Bluetooth bluetoothConfig;

    public TestConfig(String jsonString) throws org.json.JSONException {
        JSONObject config = new JSONObject(jsonString);
        if (config.has("wifi")) {
            this.wifiConfig = new Wifi(config.getJSONObject("wifi"));
        }
        if (config.has("ethernet")) {
            this.ethernetConfig = new Ethernet(config.getJSONObject("ethernet"));
        }
        if (config.has("bluetooth")) {
            this.bluetoothConfig = new Bluetooth(config.getJSONObject("bluetooth"));
        }
    }
}
