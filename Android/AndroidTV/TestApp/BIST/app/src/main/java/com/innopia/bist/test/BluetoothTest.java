package com.innopia.bist.test;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import com.innopia.bist.util.TestConfig;
import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Implements the Test interface for Bluetooth functionality.
 * This class handles device discovery, connection testing, and information retrieval.
 */
public class BluetoothTest implements Test {

	private static final String TAG = "BIST_BT_TEST";
	private static final UUID STANDARD_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private BroadcastReceiver autoConnectReceiver;
	private Handler timeoutHandler = new Handler(Looper.getMainLooper());

	/**
	 * Asynchronously finds all currently connected Bluetooth devices (A2DP and HEADSET profiles).
	 * @param context The application context.
	 * @param callback A consumer to receive the list of connected devices.
	 */
	public void findConnectedDevices(Context context, Consumer<List<BluetoothDevice>> callback) {
		BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled() ||
				ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
			Log.e(TAG, "Bluetooth is unavailable or permission is missing.");
			callback.accept(new ArrayList<>());
			return;
		}

		List<BluetoothDevice> connectedDevices = new ArrayList<>();
		final CountDownLatch latch = new CountDownLatch(2);

		BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener() {
			@SuppressLint("MissingPermission")
			@Override
			public void onServiceConnected(int profile, BluetoothProfile proxy) {
				for (BluetoothDevice device : proxy.getConnectedDevices()) {
					if (!connectedDevices.contains(device)) {
						connectedDevices.add(device);
					}
				}
				bluetoothAdapter.closeProfileProxy(profile, proxy);
				latch.countDown();
			}

			@Override
			public void onServiceDisconnected(int profile) {
				latch.countDown();
			}
		};

		bluetoothAdapter.getProfileProxy(context, listener, BluetoothProfile.A2DP);
		bluetoothAdapter.getProfileProxy(context, listener, BluetoothProfile.HEADSET);

		new Thread(() -> {
			try {
				latch.await();
				callback.accept(connectedDevices);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				callback.accept(new ArrayList<>());
			}
		}).start();
	}

	/**
	 * Performs a connection test by first discovering services via SDP.
	 * It fetches the device's advertised UUIDs and attempts to connect to each one.
	 * This is far more reliable than assuming SPP is always available.
	 * @param context The application context.
	 * @param device The BluetoothDevice to test.
	 * @return A string indicating the test result.
	 */
	@SuppressLint("MissingPermission")
	private String testConnection(Context context, BluetoothDevice device) {
		final List<UUID> foundUuids = new ArrayList<>();
		final CountDownLatch sdpLatch = new CountDownLatch(1);

		// A BroadcastReceiver to capture the results of the SDP query.
		BroadcastReceiver uuidReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context ctx, Intent intent) {
				String action = intent.getAction();
				if (BluetoothDevice.ACTION_UUID.equals(action)) {
					Parcelable[] uuidExtras = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
					if (uuidExtras != null) {
						for (Parcelable uuidExtra : uuidExtras) {
							foundUuids.add(UUID.fromString(uuidExtra.toString()));
						}
					}
					// SDP query finished, release the latch.
					sdpLatch.countDown();
					// Unregister the receiver to prevent leaks.
					ctx.unregisterReceiver(this);
				}
			}
		};

		context.registerReceiver(uuidReceiver, new IntentFilter(BluetoothDevice.ACTION_UUID));
		// Start the service discovery process. This is asynchronous.
		device.fetchUuidsWithSdp();

		try {
			// Wait for the SDP query to complete, with a generous timeout.
			if (!sdpLatch.await(15, TimeUnit.SECONDS)) {
				// To prevent leaks, ensure receiver is unregistered on timeout.
				try {
					context.unregisterReceiver(uuidReceiver);
				} catch (IllegalArgumentException e) {
					// Receiver might have already been unregistered, which is fine.
				}
				return "Connection Test: FAIL\nReason: Service discovery (SDP) timed out.";
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return "Connection Test: FAIL\nReason: Service discovery was interrupted.";
		}

		if (foundUuids.isEmpty()) {
			return "Connection Test: FAIL\nReason: No compatible services (UUIDs) were found on the device.";
		}

		Log.d(TAG, "Found " + foundUuids.size() + " services on " + device.getName() + ". Attempting to connect...");

		// Always cancel discovery before connecting, as it is an intensive process.
		BluetoothAdapter adapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
		if (adapter.isDiscovering()) {
			adapter.cancelDiscovery();
		}

		// Iterate through the discovered UUIDs and try to connect.
		for (UUID uuid : foundUuids) {
			BluetoothSocket socket = null;
			try {
				socket = device.createRfcommSocketToServiceRecord(uuid);
				socket.connect();
				// If connect() succeeds, the test passes.
				return "Connection Test: PASS\n(Connected with UUID: " + uuid.toString() + ")";
			} catch (IOException e) {
				Log.w(TAG, "Failed to connect with UUID: " + uuid.toString() + ". Trying next...");
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						// Ignore close exception.
					}
				}
			}
		}

		return "Connection Test: FAIL\nReason: Failed to connect using any of the " + foundUuids.size() + " available services.";
	}

	/**
	 * Runs the manual test flow.
	 * @param params A map containing the 'context' and 'device' for the test.
	 * @param callback A consumer to return the test result string.
	 */
	private void bluetoothTest (Map<String, Object> params, Consumer<TestResult> callback) {
		executor.execute(() -> {
			Context context = (Context) params.get("context");
			BluetoothDevice device = (BluetoothDevice) params.get("device");
			if (device == null || context == null) {
				callback.accept(new TestResult(TestStatus.ERROR, "Device or Context is null."));
				return;
			}
			String result = testConnection(context, device);
			if (result.contains("PASS")) {
				callback.accept(new TestResult(TestStatus.PASSED, "BT Test pass \n"+result));
			} else {
				callback.accept(new TestResult(TestStatus.FAILED, "BT Test fail \n"+result));
			}
		});
	}

	@Override
	public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
		bluetoothTest(params, callback);
	}

	@SuppressLint("MissingPermission")
	@Override
	public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
		Context context = (Context) params.get("context");
		TestConfig.Bluetooth btConfig = (TestConfig.Bluetooth) params.get("config");
		if (btConfig != null && btConfig.name != null) {
			Log.d(TAG, "Bluetooth config found. Target device name: " + btConfig.name);
			connectToBluetoothWithName(context, btConfig.name, callback);
		} else {
			Log.d(TAG, "No Bluetooth config found. Using legacy user-interactive flow.");
			runLegacyAutoTest(params, callback);
		}
	}

	@RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    private void connectToBluetoothWithName(Context context, String name, Consumer<TestResult> callback) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			Log.d(TAG, "Auto-connection is not supported under Android 14. Falling back to normal flow.");
			runLegacyAutoTest(new HashMap<>() , callback);
			return;
		}
		BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
			callback.accept(new TestResult(TestStatus.FAILED, "Bluetooth is not enabled."));
			return;
		}

		String deviceName = "";
		for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
			if (deviceName.equals(device.getName())) {
				Log.d(TAG, "Deivce already bonded.. Starting connection test for: "+deviceName);
				String resultStr = testConnection(context, device);
				callback.accept(new TestResult(
						resultStr.contains("PASS") ? TestStatus.PASSED : TestStatus.FAILED,
						resultStr
				));
				return;
			}
		}

		Log.d(TAG, "Starting discovery to find device: " + deviceName);

		Runnable timeoutRunnable = () -> {
			Log.d(TAG, "Discovery timed out. Could not find device: " + deviceName);
			bluetoothAdapter.cancelDiscovery();
			try {
				context.unregisterReceiver(autoConnectReceiver);
			} catch (IllegalArgumentException e) {
			}

			callback.accept(new TestResult(TestStatus.FAILED, "Discovery Timeout Device not found: " + deviceName));
		};
		autoConnectReceiver = new BroadcastReceiver() {
			private boolean found = false;

			@RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
			@Override
			public void onReceive(Context ctx, Intent intent) {
				String action = intent.getAction();
				if (BluetoothDevice.ACTION_FOUND.equals(action) && !found) {
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					if (device != null && deviceName.equals(device.getName())) {
						found = true;
						timeoutHandler.removeCallbacks(timeoutRunnable);
						Log.d(TAG, "Device found: " + deviceName + ". Cancelling discovery and starting bond.");
						bluetoothAdapter.cancelDiscovery();
						device.createBond();
					}
				} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

					if (deviceName.equals(device.getName())) {
						if (state == BluetoothDevice.BOND_BONDED) {
							Log.d(TAG, "Bonding successful with " + deviceName);
							ctx.unregisterReceiver(this);
							String resultStr = testConnection(context, device);
							callback.accept(new TestResult(resultStr.contains("PASS") ? TestStatus.PASSED : TestStatus.FAILED, resultStr));
						} else if (state == BluetoothDevice.BOND_NONE) {
							Log.d(TAG, "Bonding failed with " + deviceName);
							ctx.unregisterReceiver(this);
							callback.accept(new TestResult(TestStatus.FAILED, "Bonding failed with " + deviceName));
						}
					}
				}
			}
		};
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		context.registerReceiver(autoConnectReceiver, filter);
		bluetoothAdapter.startDiscovery();
		timeoutHandler.postDelayed(timeoutRunnable, 30000);
	}

	private void runLegacyAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
		Context context = (Context) params.get("context");
		Boolean isResume = (Boolean) params.getOrDefault("isResume", false);

		BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
			callback.accept(new TestResult(TestStatus.FAILED, "Bluetooth is not enabled."));
			return;
		}

		if (!isResume) {
			findConnectedDevices(context, connectedDevices -> {
				if (!connectedDevices.isEmpty()) {
					Log.d(TAG, "Legacy AutoTest: Device already connected. Starting connection test.");
					String resultStr = testConnection(context, connectedDevices.get(0));
					callback.accept(new TestResult(
							resultStr.contains("PASS") ? TestStatus.PASSED : TestStatus.FAILED,
							resultStr
					));
				} else {
					Log.d(TAG, "Legacy AutoTest: No device connected. Waiting for user action.");
					callback.accept(new TestResult(TestStatus.WAITING_FOR_USER, "Bluetooth device is not connected. Do you want to scan and connect now?"));
				}
			});
		} else {
			Boolean userChoice = (Boolean) params.getOrDefault("userChoice", false);
			if (userChoice) {
				Log.d(TAG, "Legacy AutoTest: User chose to connect. Waiting for user to pair a device.");
				callback.accept(new TestResult(TestStatus.WAITING_FOR_USER, "Please pair a Bluetooth device from Settings, then press OK."));
			} else {
				Log.d(TAG, "Legacy AutoTest: User chose not to connect.");
				// 사용자가 '아니오'를 선택하면, 재테스트 상태로 변경
				callback.accept(new TestResult(TestStatus.RETEST, "User skipped Bluetooth Connection. Marked for re-test."));
			}
		}
	}

	@SuppressLint("MissingPermission")
	private List<BluetoothDevice> getProxyConnectedDevices(BluetoothAdapter adapter, Context context) {
		return new ArrayList<>(adapter.getBondedDevices());
	}

	private void executeTest(Map<String, Object> params, Consumer<TestResult> callback) {
		executor.execute(() -> {
			Context context = (Context) params.get("context");
			if (context == null) {
				callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null"));
				return;
			}
			callback.accept(new TestResult(TestStatus.PASSED, "Bluetooth Test pass"));
		});
	}

	@SuppressLint("MissingPermission")
	public void getDeviceInfo(BluetoothDevice device, Consumer<String> callback) {
		if (device == null) {
			callback.accept("Device is null.");
			return;
		}
		StringBuilder info = new StringBuilder();
		info.append("Device Name: ").append(device.getName()).append("\n");
		info.append("Address: ").append(device.getAddress()).append("\n");
		info.append("Bond State: ").append(getBondStateString(device.getBondState())).append("\n");
		info.append("Type: ").append(getDeviceTypeString(device.getType())).append("\n");

		Parcelable[] uuids = device.getUuids();
		if (uuids != null && uuids.length > 0) {
			info.append("Cached UUIDs:\n");
			for (Parcelable p : uuids) {
				info.append(" - ").append(p.toString()).append("\n");
			}
		} else {
			info.append("Cached UUIDs: Not available. (Use SDP to fetch live services)\n");
		}
		callback.accept(info.toString());
	}

	private String getBondStateString(int bondState) {
		switch (bondState) {
			case BluetoothDevice.BOND_BONDED: return "BONDED (Paired)";
			case BluetoothDevice.BOND_BONDING: return "BONDING (Pairing)";
			case BluetoothDevice.BOND_NONE: return "NONE (Not Paired)";
			default: return "UNKNOWN";
		}
	}

	private String getDeviceTypeString(int deviceType) {
		switch (deviceType) {
			case BluetoothDevice.DEVICE_TYPE_CLASSIC: return "CLASSIC (BR/EDR)";
			case BluetoothDevice.DEVICE_TYPE_LE: return "LOW ENERGY (LE)";
			case BluetoothDevice.DEVICE_TYPE_DUAL: return "DUAL MODE (Classic + LE)";
			default: return "UNKNOWN";
		}
	}
}
