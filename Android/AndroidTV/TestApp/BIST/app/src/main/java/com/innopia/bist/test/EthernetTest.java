package com.innopia.bist.test;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

//import com.innopia.bist.util.FileReadHelper; // allow sign app
import com.innopia.bist.util.TestConfig;
import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
		Context context = (Context) params.get("context");
		TestConfig.Ethernet ethConfig = (TestConfig.Ethernet) params.get("config");

		if (ethConfig != null && ethConfig.ip != null) {
			Log.d(TAG, "Ethernet config found. Expected IP: " + ethConfig.ip);
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
				Log.d(TAG, "Auto-verification is not supported under Android 14. Falling back to manual flow.");
				runLegacyAutoTest(params, callback);
				return;
			}
			ethernetTestWithVerification(context, ethConfig, callback);
		} else {
			Log.d(TAG, "No Ethernet config found. Using legacy user-interactive flow.");
			runLegacyAutoTest(params, callback);
		}
	}

	private void ethernetTestWithVerification(Context context, TestConfig.Ethernet ethConfig, Consumer<TestResult> callback) {
		executor.execute(() -> {
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			Network ethNetwork = null;

			for (Network network : cm.getAllNetworks()) {
				NetworkCapabilities caps = cm.getNetworkCapabilities(network);
				if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
					ethNetwork = network;
					break;
				}
			}

			if (ethNetwork == null) {
				callback.accept(new TestResult(TestStatus.FAILED, "Ethernet is not connected."));
				return;
			}

			LinkProperties linkProperties = cm.getLinkProperties(ethNetwork);
			boolean ipMatch = false;
			if (linkProperties != null) {
				for (LinkAddress addr : linkProperties.getLinkAddresses()) {
					if (addr.getAddress().getHostAddress().equals(ethConfig.ip)) {
						ipMatch = true;
						break;
					}
				}
			}

			if (!ipMatch) {
				callback.accept(new TestResult(TestStatus.FAILED, "IP mismatch. Expected: " + ethConfig.ip + ", but was not found."));
				return;
			}

			boolean isInternetOk = isInternetAvailable(ethNetwork);
			String info = "IP Address Verified: " + ethConfig.ip + "\n" +
					"Internet Status: " + (isInternetOk ? "OK" : "No Internet");

			if (isInternetOk) {
				callback.accept(new TestResult(TestStatus.PASSED, info));
			} else {
				callback.accept(new TestResult(TestStatus.FAILED, info));
			}
		});
	}

	private void runLegacyAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
		Context context = (Context) params.get("context");

		// 테스트 재개가 아닌 최초 실행 시
		if (!(boolean) params.getOrDefault("isResume", false)) {
			checkWifiAndProceed(context, params, callback);
		} else { // 사용자 응답 후 테스트 재개 시
			int state = (int) params.getOrDefault("state", STATE_CHECK_WIFI);
			boolean userChoice = (boolean) params.getOrDefault("userChoice", false);

			switch (state) {
				case STATE_CHECK_WIFI: // "Wi-Fi를 끄시겠습니까?" 에 대한 응답 처리
					if (userChoice) {
						WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
						if (isSystemApp(context)) { // 시스템 앱 권한 확인
							wifiManager.setWifiEnabled(false);
						}
						Log.d(TAG, "User agreed. Turning Wi-Fi off.");
						// Wi-Fi가 꺼질 시간을 잠시 기다린 후 이더넷 확인
						handler.postDelayed(() -> checkEthernetAndTest(context, params, callback), 1000);
					} else {
						Log.w(TAG, "User refused to turn off Wi-Fi.");
						callback.accept(new TestResult(TestStatus.FAILED, "Test cannot run while Wi-Fi is active. User declined."));
					}
					break;
				case STATE_WAIT_FOR_ETHERNET_CONNECT: // "이더넷 케이블을 연결하세요" 에 대한 응답 처리
					// 사용자가 OK를 누르면, 이더넷 연결 폴링 시작
					pollForEthernetConnect(context, params, callback);
					break;
			}
		}
	}

	public boolean isSystemApp(Context context) {
		PackageManager pm = context.getPackageManager();
		try {
			ApplicationInfo appInfo = pm.getApplicationInfo(context.getPackageName(), 0);
			return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			return false;
		}
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
		executor.execute(() -> {
			Context context = (Context) params.get("context");
			if (context == null) {
				callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null"));
				return;
			}
			callback.accept(new TestResult(TestStatus.PASSED, "Ethernet Test pass"));
		});
	}
	private void ethernetTest(Map<String, Object> params, Consumer<TestResult> callback) {
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
