package com.innopia.bist.ver1.test;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.innopia.bist.ver1.info.SystemInfo;
import com.innopia.bist.ver1.util.FileUtils;
import com.innopia.bist.ver1.util.TestResult;
import com.innopia.bist.ver1.util.TestStatus;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EthernetTest implements Test {

	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private static final String TAG = "BIST_ETH_AutoTest";
	private Handler handler = new Handler(Looper.getMainLooper());

	private static final int STATE_CHECK_WIFI = 0;
	private static final int STATE_CHECK_ETHERNET = 1;
	private static final int STATE_WAIT_FOR_ETHERNET_CONNECT = 2;

	private static final String SYSFS_ETH0_SPEED = "/sys/class/net/eth0/speed";

	@Override
	public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
		executeTest(params, callback);
	}

	@Override
	public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
		executeTest(params, callback);
		//Context context = (Context) params.get("context");
		//if (!(boolean) params.getOrDefault("isResume", false)) {
		//	checkWifiAndProceed(context, params, callback);
		//} else {
		//	int state = (int) params.getOrDefault("state", STATE_CHECK_WIFI);
		//	boolean userChoice = (boolean) params.getOrDefault("userChoice", false);

		//	switch (state) {
		//		case STATE_CHECK_WIFI:
		//			if (userChoice) {
		//				WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		//				wifiManager.setWifiEnabled(false); // Requires CHANGE_WIFI_STATE permission
		//				Log.d(TAG, "User agreed. Turning Wi-Fi off.");
		//				// Give it a moment to turn off before re-checking.
		//				handler.postDelayed(() -> checkEthernetAndTest(context, params, callback), 1000);
		//			} else {
		//				Log.w(TAG, "User refused to turn off Wi-Fi.");
		//				callback.accept(new TestResult(TestStatus.FAILED, "Test cannot run while Wi-Fi is active. User declined to turn it off."));
		//			}
		//			break;
		//		case STATE_WAIT_FOR_ETHERNET_CONNECT:
		//			// User pressed OK on "Connect Ethernet" dialog. Start polling.
		//			pollForEthernetConnect(context, params, callback);
		//			break;
		//	}
		//}
	}

	private void pollForEthernetConnect(Context context, Map<String, Object> params, Consumer<TestResult> callback) {
		final long timeout = 15000; // 15 seconds
		final long startTime = System.currentTimeMillis();
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		handler.post(new Runnable() {
			@Override
			public void run() {
				Network activeNetwork = cm.getActiveNetwork();
				if (activeNetwork != null) {
					NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
					if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
						Log.d(TAG, "Ethernet connected. Running test.");
						ethernetTest(params, callback);
						return;
					}
				}

				if (System.currentTimeMillis() - startTime > timeout) {
					Log.e(TAG, "Timeout waiting for Ethernet to be connected.");
					callback.accept(new TestResult(TestStatus.FAILED, "Timeout (15s): User did not connect Ethernet cable."));
				} else {
					handler.postDelayed(this, 1000);
				}
			}
		});
	}

	private void checkWifiAndProceed(Context context, Map<String, Object> params, Consumer<TestResult> callback) {
		WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (wifiManager.isWifiEnabled()) {
			Log.d(TAG, "Wi-Fi is enabled. Asking user to disable it.");
			params.put("state", STATE_CHECK_WIFI);
			callback.accept(new TestResult(TestStatus.WAITING_FOR_USER, "Wi-Fi is currently active. Can the test turn it off to ensure a valid Ethernet test?"));
		} else {
			Log.d(TAG, "Wi-Fi is already off. Proceeding to Ethernet check.");
			checkEthernetAndTest(context, params, callback);
		}
	}

	private void checkEthernetAndTest(Context context, Map<String, Object> params, Consumer<TestResult> callback) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		Network activeNetwork = cm.getActiveNetwork();
		if (activeNetwork != null) {
			NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
			if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
				Log.d(TAG, "Ethernet is connected. Running test.");
				ethernetTest(params, callback); // Use existing test logic
				return;
			}
		}

		Log.d(TAG, "Ethernet is not connected. Asking user to connect.");
		params.put("state", STATE_WAIT_FOR_ETHERNET_CONNECT);
		callback.accept(new TestResult(TestStatus.WAITING_FOR_USER, "Please connect the Ethernet cable. The test will start automatically."));
	}

	private void executeTest(Map<String, Object> params, Consumer<TestResult> callback) {
		Context context = (Context) params.get("context");
		if (context == null) {
			callback.accept(new TestResult(TestStatus.FAILED, "Error: Context is null"));
			return;
		}
		executor.execute(() -> {
			checkCurrentConnection(context, callback);
		});
	}

	private void ethernetTest(Map<String, Object> params, Consumer<TestResult> callback) {
		Context context = (Context) params.get("context");
		if (context == null) {
			callback.accept(new TestResult(TestStatus.FAILED, "Error: Context is null."));
			return;
		}
		executor.execute(() -> {
			checkCurrentConnection(context, callback);
		});
	}

	private void checkCurrentConnection(Context context, Consumer<TestResult> callback) {
		// delay until ethernet is back
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null) {
			callback.accept(new TestResult(TestStatus.FAILED, "ConnectivityManager is null."));
			return;
		}

		Network network = cm.getActiveNetwork();
		if (network == null) {
			callback.accept(new TestResult(TestStatus.FAILED, "No active networks."));
			return;
		}

		NetworkCapabilities nc = cm.getNetworkCapabilities(network);
		if (nc == null) {
			callback.accept(new TestResult(TestStatus.FAILED, "NetworkCapabilities is null."));
			return;
		}
		if (!nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
			callback.accept(new TestResult(TestStatus.FAILED, "Current active network is not Ethernet."));
			return;
		}


		boolean isValidated = isInternetAvailable(network);
		SystemInfo si = new SystemInfo(context);
		String ip = si.getIpAddress();
		String speed = getEthernetSpeed();
		String info = "Status: " + (isValidated ? "Connected (Internet OK)" : "Connected (No Internet)") + "\n" +
					  "Link Speed: " + speed + " Mbps";
		if (isValidated) {
			callback.accept(new TestResult(TestStatus.PASSED, info));
		} else {
			callback.accept(new TestResult(TestStatus.FAILED, info));
		}
	}

	// allow in system sign app
	private String getEthernetSpeed() {
		return FileUtils.readFromFile(SYSFS_ETH0_SPEED);
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
