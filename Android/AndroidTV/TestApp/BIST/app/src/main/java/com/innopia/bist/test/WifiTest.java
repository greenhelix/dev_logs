package com.innopia.bist.test;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.util.TestConfig;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
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
	private ConnectivityManager.NetworkCallback networkCallback;

	@Override
	public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
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

	@Override
	public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
		Context context = (Context) params.get("context");
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		TestConfig.Wifi wifiConfig = (TestConfig.Wifi) params.get("config");
		if (wifiConfig != null && wifiConfig.id != null) {
			Log.d(TAG, "Wi-Fi config found. SSID: " + wifiConfig.id + ". Attempting auto-connection.");

			connectToWifiWithConfig(context, wifiConfig, callback);

		} else {
			Log.d(TAG, "No Wi-Fi config found. Using legacy user-interactive flow.");
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
	}

	public boolean isSystemApp(Context context) {
		PackageManager pm = context.getPackageManager();
		try {
			ApplicationInfo appInfo = pm.getApplicationInfo(context.getPackageName(), 0);
			// FLAG_SYSTEM 비트가 설정되어 있는지 확인
			return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}

	private void connectToWifiWithConfig(Context context, TestConfig.Wifi config, Consumer<TestResult> callback) {
		Log.d(TAG, "connectToWifiWithConfig() start");
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			Log.d(TAG, "Auto-connection is not supported under Android 14. Falling back to manual flow.");
			String message = "Wi-Fi auto-connection is not supported. Do you want to open Wi-Fi Settings now?";
			callback.accept(new TestResult(TestStatus.WAITING_FOR_USER, message));
			return;
		}

		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (!wifiManager.isWifiEnabled()) {
			if (isSystemApp(context)) {
				Log.d(TAG, "System App detected. Enabling Wi-Fi programmatically.");
				wifiManager.setWifiEnabled(true);
			} else {
				Log.d(TAG, "Not a System App. Cannot enable Wi-Fi programmatically. Requesting user action.");
				// TODO: 일반 앱일 경우 설정 화면으로 유도하는 로직 추가
				callback.accept(new TestResult(TestStatus.FAILED, "Cannot enable Wi-Fi. Please enable it manually."));
				return;
			}
		}
		WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder().setSsid(config.id).setWpa2Passphrase(config.pw).build();
		NetworkRequest request = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).setNetworkSpecifier(specifier).build();
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (networkCallback != null) {
			try {
				connectivityManager.unregisterNetworkCallback(networkCallback);
			} catch (Exception e) {
				Log.d(TAG, "Before setting callback unregister Fail!!!\nReason: "+e);
			}
		}
		Handler timeoutHandler = new Handler(Looper.getMainLooper());
		Runnable timeoutRunnable = () -> {
			Log.d(TAG, "Wi-Fi connection timed out.");
			connectivityManager.unregisterNetworkCallback(networkCallback);
			callback.accept(new TestResult(TestStatus.FAILED, "Wi-Fi connection timed out for SSID: "+config.id));
		};

		networkCallback = new ConnectivityManager.NetworkCallback() {
			@Override
			public void onAvailable(@NonNull Network network) {
				super.onAvailable(network);
				timeoutHandler.removeCallbacks(timeoutRunnable);
				Log.d(TAG, "Wi-Fi network available: " + config.id);
				connectivityManager.bindProcessToNetwork(network);
				wifiTest(Map.of("context", context), callback);
				connectivityManager.unregisterNetworkCallback(this);
			}

			@Override
			public void onUnavailable() {
				super.onUnavailable();
				timeoutHandler.removeCallbacks(timeoutRunnable);
				Log.d(TAG, "Could not connect to Wi-Fi network: " + config.id);
				callback.accept(new TestResult(TestStatus.FAILED, "Failed to connect. Check SSID/PW for: "+ config.id));
				connectivityManager.unregisterNetworkCallback(this);
			}
		};
		connectivityManager.requestNetwork(request, networkCallback);
		timeoutHandler.postDelayed(timeoutRunnable, 30000);
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
