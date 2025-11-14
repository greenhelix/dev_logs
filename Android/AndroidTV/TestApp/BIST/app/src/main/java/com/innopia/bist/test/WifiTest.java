package com.innopia.bist.test;

import android.content.Context;
import android.net.ConnectivityManager;
//import android.net.InterfaceConfiguration;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
//import android.os.INetworkManagementService;
//import android.os.ServiceManager;
import android.os.Looper;
import android.util.Log;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WifiTest implements Test {

	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private static final String TAG = "BIST_WIFI_TEST";

	private Handler handler = new Handler(Looper.getMainLooper());
	private static final int STATE_CHECK_ETHERNET = 0;
	private static final int STATE_WAIT_FOR_ETHERNET_DISCONNECT = 1;
	private static final int STATE_CHECK_WIFI = 2;
	private static final int STATE_WAIT_FOR_WIFI_CONNECT = 3;

	private Context context;

	@Override
	public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
		executeTest(params, callback);
	}

	@Override
	public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
		configureEthernet(false);
		// delay
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// executeTest(params, callback);
		context = (Context) params.get("context");
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		Bundle config = (Bundle) params.get("config");
		if (config != null) {
			String configSsid = (String) config.get("wifi_ssid");
			String configPassword = (String) config.get("wifi_password");
			WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			if (wm == null) {
				callback.accept(new TestResult(TestStatus.FAILED, "WifiManager is null"));
				return;
			}
			// turn on wifi, if currently off
			if (!wm.isWifiEnabled()) {
				wm.setWifiEnabled(true);
				try { // a delay for wifi to turn on
					TimeUnit.SECONDS.sleep(2);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}

			WifiConfiguration wifiConfig = new WifiConfiguration();
			wifiConfig.SSID = String.format("\"%s\"", configSsid);
			wifiConfig.preSharedKey = String.format("\"%s\"", configPassword);
			// remove existing network configuration
			int networkId = -1;
			for (WifiConfiguration existingConfig : wm.getConfiguredNetworks()) {
				if (existingConfig.SSID != null && existingConfig.SSID.equals(wifiConfig.SSID)) {
					wm.removeNetwork(existingConfig.networkId);
					wm.saveConfiguration();
					break;
				}
			}

			networkId = wm.addNetwork(wifiConfig);
			if (networkId == -1) {
				callback.accept(new TestResult(TestStatus.FAILED, "Failed to add network configuration for " + configSsid));
				return;
			}

			boolean disconnectResult = wm.disconnect();
			boolean enableResult = wm.enableNetwork(networkId, true);
			boolean reconnectResult = wm.reconnect();

			if (enableResult && reconnectResult) {
				// delay
				try {
					TimeUnit.SECONDS.sleep(5);
				} catch (Exception e) {
					e.printStackTrace();
				}
				checkCurrentConnection(context, callback); // callback handled inside
			} else {
				callback.accept(new TestResult(TestStatus.FAILED, "Failed to connect to network: " + configSsid));
			}
		}

		//if (!(boolean) params.getOrDefault("isResume", false)) {
		//	checkEthernetAndProceed(context, cm, params, callback);
		//} else {
		//	int state = (int) params.getOrDefault("state", STATE_CHECK_ETHERNET);
		//	boolean userChoice = (boolean) params.getOrDefault("userChoice", false);
		//	

		//	switch (state) {
		//		case STATE_WAIT_FOR_ETHERNET_DISCONNECT:
		//			pollForEthernetDisconnect(context, cm, params, callback);
		//			break;

		//		case STATE_CHECK_WIFI:
		//			if (userChoice) {
		//				params.put("state", STATE_WAIT_FOR_WIFI_CONNECT);
		//				callback.accept(new TestResult(TestStatus.WAITING_FOR_USER, "Please connect to a Wi-Fi network from Settings and press OK."));
		//			} else {
		//				callback.accept(new TestResult(TestStatus.RETEST, "User skipped Wi-Fi connection. Marked for re-test"));
		//			}
		//			break;
		//		case STATE_WAIT_FOR_WIFI_CONNECT:
		//			checkWifiAndTest(context, cm, params, callback);
		//			break;
		//	}
		//}
		configureEthernet(true);
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
		Context context = (Context) params.get("context");
		if (context == null) {
			callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null"));
			return;
		}
		executor.execute(() -> {
			configureEthernet(false);
			// delay
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (Exception e) {
				e.printStackTrace();
			}
			checkCurrentConnection(context, callback);
			configureEthernet(true);
		});
	}

	private void wifiTest(Map<String, Object> params, Consumer<TestResult> callback) {
		Context context = (Context) params.get("context");
		if (context == null) {
			callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null. Cannot perform test."));
			return;
		}
		executor.execute(() -> {
			configureEthernet(false);
			// delay
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (Exception e) {
				e.printStackTrace();
			}
			checkCurrentConnection(context, callback);
			configureEthernet(true);
		});
	}

	private void checkCurrentConnection(Context context, Consumer<TestResult> callback) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		Network activeNetwork = connectivityManager.getActiveNetwork();
		if (activeNetwork == null) {
			// turn on wifi
			if (!wm.isWifiEnabled()) {
				wm.setWifiEnabled(true);
			}
		}

		NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
		if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
			callback.accept(new TestResult(TestStatus.FAILED, "Active network is not Wi-Fi."));
			return;
		}

		WifiInfo wifiInfo = wm.getConnectionInfo();
		String ssid = wifiInfo.getSSID().replace("\"", "");
		String bssid = wifiInfo.getBSSID();
		int rssi = wifiInfo.getRssi();
		int linkSpeed = wifiInfo.getLinkSpeed();

		boolean isInternetValidated = isInternetAvailable(activeNetwork);

		String details = "SSID: " + ssid + "\n" +
				"BSSID: " + bssid + "\n" +
				"Signal Strength (RSSI): " + rssi + " dBm\n" +
				"Link Speed: " + linkSpeed + " Mbps";

		String message;
		TestStatus status;
		if (isInternetValidated) {
			message = "Status: Connected (Internet OK)\n" + details;
			status = TestStatus.PASSED;
			Log.d("BIST_WIFI", "Test Result: PASSED. " + message);
		} else {
			message = "Status: Connected (No Internet)\n" + details;
			status = TestStatus.FAILED;
			Log.e("BIST_WIFI", "Test Result: FAILED. " + message);
		}
		callback.accept(new TestResult(status, message));
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

	private void configureEthernet(boolean isUp) {
		Log.d(TAG, "configureEthernet : " + isUp);
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

//		IBinder binder = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
//		INetworkManagementService service = INetworkManagementService.Stub.asInterface(binder);

		String iface = "eth0";

		if(isEthernetConnected(cm)) {

		}

		try {
			// ServiceManager는 여전히 숨겨져 있으므로 리플렉션으로 호출
			Class<?> smClass = Class.forName("android.os.ServiceManager");
			Method getServiceMethod = smClass.getMethod("getService", String.class);
//			IBinder binder = (IBinder) getServiceMethod.invoke(null, Context.NETWORKMANAGEMENT_SERVICE);

			// AIDL을 추가했기 때문에 Stub.asInterface는 직접 호출 가능
//			INetworkManagementService networkManagementService = INetworkManagementService.Stub.asInterface(binder);

		try {
			//InterfaceConfiguration interfaceConfiguration = service.getInterfaceConfig(iface);
			//if (interfaceConfiguration != null) {
				//if (isUp) {
					//interfaceConfiguration.setInterfaceUp();
				//} else {
					//interfaceConfiguration.setInterfaceDown();
				//}
				//interfaceConfiguration.clearFlag("running");
				//service.setInterfaceConfig(iface, interfaceConfiguration);
			//} else {
			//	Log.d(TAG, "interfaceConfiguration is null");
			//}
		} catch (Exception e) {
			Log.d(TAG,"Exception : " + e);
		}
	} catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
	}
