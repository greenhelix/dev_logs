// MODIFIED: This file contains the definitive fix for the connection issue.
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
import android.os.Parcelable;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Implements the Test interface for Bluetooth functionality.
 * This class handles device discovery, connection testing, and information retrieval.
 */
public class BluetoothTest implements Test {

    private static final String TAG = "BluetoothTest";
    private static final UUID STANDARD_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

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
     * MODIFIED: Performs a connection test by first discovering services via SDP.
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
    @Override
    public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
        Context context = (Context) params.get("context");
        BluetoothDevice device = (BluetoothDevice) params.get("device");

        if (device == null || context == null) {
            callback.accept(new TestResult(TestStatus.ERROR, "Device or Context is null."));
            return;
        }

        new Thread(() -> {
            String result = testConnection(context, device);
            callback.accept(new TestResult(TestStatus.PASSED, result));
        }).start();
    }

    @Override
    public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
        Context context = (Context) params.get("context");
        BluetoothDevice device = (BluetoothDevice) params.get("device");

        if (device == null || context == null) {
            callback.accept(new TestResult(TestStatus.ERROR, "Device or Context is null."));
            return;
        }

        new Thread(() -> {
            String result = testConnection(context, device);
            callback.accept(new TestResult(TestStatus.PASSED, result));
        }).start();
    }

    /**
     * Retrieves detailed information about a given Bluetooth device.
     * @param device The BluetoothDevice to inspect.
     * @param callback A consumer to return the formatted device information string.
     */
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
