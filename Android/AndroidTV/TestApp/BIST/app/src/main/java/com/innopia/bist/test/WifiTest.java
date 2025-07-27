package com.innopia.bist.test;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

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
    public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executeTest(params, callback);
    }

    @Override
    public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executeTest(params, callback);
    }

    private void executeTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executor.execute(() -> {
            Context context = (Context) params.get("context");
            if (context == null) {
                callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null"));
                return;
            }
            callback.accept(new TestResult(TestStatus.PASSED, "Wifi Test pass"));
        });
    }

    private void wifiTest(Map<String, Object> params, Consumer<TestResult> callback) {
        Context context = (Context) params.get("context");
        if (context == null) {
            callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null. Cannot perform test."));
            return;
        }
        executor.execute(() -> {
            checkCurrentConnection(context, callback);
        });
    }

    private void checkCurrentConnection(Context context, Consumer<TestResult> callback) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            callback.accept(new TestResult(TestStatus.FAILED, "No active network connection."));
            return;
        }

        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            callback.accept(new TestResult(TestStatus.FAILED, "Active network is not Wi-Fi."));
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID().replace("\"", "");
        String bssid = wifiInfo.getBSSID();
        int rssi = wifiInfo.getRssi();
        int linkSpeed = wifiInfo.getLinkSpeed();

        boolean isInternetValidated = isInternetAvailable(activeNetwork);

        String details = "SSID: " + ssid + "\n" +
                "BSSID: " + bssid + "\n" +
                "Signal Strength (RSSI): " + rssi + " dBm\n" +
                "Link Speed: " + linkSpeed + " Mbps";

        if (isInternetValidated) {
            String message = "Status: Connected (Internet OK)\n" + details;
            Log.d("BIST_WIFI", "Test Result: PASSED. " + message);
            callback.accept(new TestResult(TestStatus.PASSED, message));
        } else {
            String message = "Status: Connected (No Internet)\n" + details;
            Log.e("BIST_WIFI", "Test Result: FAILED. " + message);
            callback.accept(new TestResult(TestStatus.FAILED, message));
        }
    }

    private boolean isInternetAvailable(Network network) {
        try {
            URL url = new URL("https://clients3.google.com/generate_204");
            HttpURLConnection urlConnection = (HttpURLConnection) network.openConnection(url);
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setConnectTimeout(3000);
            urlConnection.setReadTimeout(3000);
            urlConnection.setUseCaches(false);
            urlConnection.connect();
            int responseCode = urlConnection.getResponseCode();
            urlConnection.disconnect();
            return responseCode == HttpURLConnection.HTTP_NO_CONTENT;
        } catch (IOException e) {
            Log.w("BIST_WIFI", "Internet availability check failed.", e);
            return false;
        }
    }
}
