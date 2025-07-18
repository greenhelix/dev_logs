package com.innopia.bist.test;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class WifiTest implements Test {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<String> callback) {
        Context context = (Context) params.get("context");
        if (context == null) {
            callback.accept("Error: Context is null.");
            return;
        }
        executor.execute(() -> {
            checkCurrentConnection(context, callback);
        });
    }

//    @Override
//    public void runAutoTest(Map<String, Object> params, Consumer<String> callback) {
//
//    }

    private void checkCurrentConnection(Context context, Consumer<String> callback) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            callback.accept("Wi-Fi is not connected.");
            return;
        }

        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            callback.accept("Current active network is not Wi-Fi.");
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID().replace("\"", "");
        String bssid = wifiInfo.getBSSID();
        int rssi = wifiInfo.getRssi();
        int linkSpeed = wifiInfo.getLinkSpeed();
        boolean isValidated = isInternetAvailable(activeNetwork);

        String info = "Status: " + (isValidated ? "Connected (Internet OK)" : "Connected (No Internet)") + "\n" +
                "SSID: " + ssid + "\n" +
                "BSSID: " + bssid + "\n" +
                "Signal Strength (RSSI): " + rssi + " dBm\n" +
                "Link Speed: " + linkSpeed + " Mbps";
        Log.d("BIST", "Connection check finished. SSID: " + ssid + ", Internet available: " + isValidated);
        callback.accept(info);
    }

    private boolean isInternetAvailable(Network network) {
        try {
            URL url = new URL("https://clients3.google.com/generate_204");
            HttpURLConnection urlConnection = (HttpURLConnection) network.openConnection(url);
            urlConnection.setConnectTimeout(3000);
            urlConnection.connect();
            int responseCode = urlConnection.getResponseCode();
            urlConnection.disconnect();
            return responseCode == 204;
        } catch (IOException e) {
            return false;
        }
    }
}
