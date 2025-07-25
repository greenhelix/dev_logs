package com.innopia.bist.test;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

//import com.innopia.bist.util.FileReadHelper; // allow sign app
import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class EthernetTest implements Test {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String SYSFS_ETH0_SPEED = "/sys/class/net/eth0/speed";

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executeTest(params, callback);
    }

    @Override
    public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executeTest(params, callback);
    }

    private void executeTest(Map<String, Object> params, Consumer<TestResult> callback) {
        Context context = (Context) params.get("context");
        if (context == null) {
            callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null."));
            return;
        }
        executor.execute(() -> {
            checkCurrentConnection(context, callback);
        });
    }
    private void checkCurrentConnection(Context context, Consumer<TestResult> callback) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            callback.accept(new TestResult(TestStatus.ERROR, "ConnectivityManager is null."));
            return;
        }

        Network network = cm.getActiveNetwork();
        if (network == null) {
            callback.accept(new TestResult(TestStatus.ERROR,"No active networks."));
            return;
        }

        NetworkCapabilities nc = cm.getNetworkCapabilities(network);
        if (nc == null) {
            callback.accept(new TestResult(TestStatus.ERROR,"NetworkCapabilities is null."));
            return;
        }
        if (!nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            callback.accept(new TestResult(TestStatus.ERROR,"Current active network is not Ethernet."));
            return;
        }

//        String speed = getEthernetSpeed();
        String speed = "getEthernetSpeed() fake run 100Mbps";
        boolean isValidated = isInternetAvailable(network);
        String info = "Status: " + (isValidated ? "Connected (Internet OK)" : "Connected (No Internet)") + "\n" +
                "Link Speed: " + speed + " Mbps";
        if(isValidated) {
            callback.accept(new TestResult(TestStatus.PASSED, info));
        }else {
            callback.accept(new TestResult(TestStatus.FAILED, info));
        }
    }

// allow in system sign app
//    private String getEthernetSpeed() {
//        return FileReadHelper.readFromFile(SYSFS_ETH0_SPEED);
//    }

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
