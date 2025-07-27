package com.innopia.bist.test;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
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
    private static final String TAG = "BIST_WIFI_TEST";

    private Handler handler = new Handler(Looper.getMainLooper());
    private static final int STATE_CHECK_ETHERNET = 0;
    private static final int STATE_WAIT_FOR_ETHERNET_DISCONNECT = 1;
    private static final int STATE_CHECK_WIFI = 2;
    private static final int STATE_WAIT_FOR_WIFI_CONNECT = 3;

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executeTest(params, callback);
    }

    @Override
    public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
       // executeTest(params, callback);
        Context context = (Context) params.get("context");
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (!(boolean) params.getOrDefault("isResume", false)) {
            checkEthernetAndProceed(context, cm, params, callback);
        } else {
            int state = (int) params.getOrDefault("state", STATE_CHECK_ETHERNET);
            boolean userChoice = (boolean) params.getOrDefault("userChoice", false);

            switch (state) {
                case STATE_WAIT_FOR_ETHERNET_DISCONNECT:
                    pollForEthernetDisconnect(context, cm, params, callback);
                    break;

                case STATE_CHECK_WIFI:
                    if (userChoice) {
                        params.put("state", STATE_WAIT_FOR_WIFI_CONNECT);
                        callback.accept(new TestResult(TestStatus.WAITING_FOR_USER, "Please connect to a Wi-Fi network from Settings and press OK."));
                    } else {
                        callback.accept(new TestResult(TestStatus.RETEST, "User skipped Wi-Fi connection. Marked for re-test"));
                    }
                    break;
                case STATE_WAIT_FOR_WIFI_CONNECT:
                    checkWifiAndTest(context, cm, params, callback);
                    break;
            }
        }
    }

    private void checkWifiAndTest(Context context, ConnectivityManager cm, Map<String, Object> params, Consumer<TestResult> callback) {
        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                // Wi-Fi is connected, run the test.
                Log.d(TAG, "Wi-Fi is connected. Running test.");
                // Using the existing manual test logic for the actual test part.
                wifiTest(params, callback);
                return;
            }
        }

        // Wi-Fi is not connected. Ask user.
        Log.d(TAG, "Wi-Fi is not connected. Asking user.");
        params.put("state", STATE_CHECK_WIFI);
        callback.accept(new TestResult(TestStatus.WAITING_FOR_USER, "Wi-Fi is not connected. Do you want to connect now?"));
    }

    private void pollForEthernetDisconnect(Context context, ConnectivityManager cm, Map<String, Object> params, Consumer<TestResult> callback) {
        final long timeout = 15000;
        final long startTime = System.currentTimeMillis();

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isEthernetConnected(cm)) {
                    Log.d(TAG, "Ethernet successfully disconnected.");
                    checkWifiAndTest(context, cm, params, callback);
                } else if (System.currentTimeMillis() - startTime > timeout) {
                    Log.e(TAG, "Timeout waiting for Ethernet to be disconnected.");
                    callback.accept(new TestResult(TestStatus.FAILED, "Timeout (15s): User did not disconnect Ethernet cable."));
                } else {
                    // Check again after 1 second
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }

    private boolean isEthernetConnected(ConnectivityManager cm) {
        Network[] networks = cm.getAllNetworks();
        for (Network network : networks) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return true;
            }
        }
        return false;
    }

    private void checkEthernetAndProceed(Context context, ConnectivityManager cm, Map<String, Object> params, Consumer<TestResult> callback) {
        if (isEthernetConnected(cm)) {
            Log.d(TAG, "Ethernet is connected. Asking user to disconnect.");
            params.put("state", STATE_WAIT_FOR_ETHERNET_DISCONNECT);
            callback.accept(new TestResult(TestStatus.WAITING_FOR_USER, "Ethernet is connected. Please disconnect the Ethernet cable to proceed with the Wi-Fi test. Press OK when ready."));
        } else {
            Log.d(TAG, "Ethernet is not connected. Proceeding to Wi-Fi check.");
            checkWifiAndTest(context, cm, params, callback);
        }
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
