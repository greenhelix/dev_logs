package com.ik.innopia.hubist.main.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class BluetoothTest {

    private static final String TAG = "BIST";
    private final BluetoothLeScanner bluetoothLeScanner;
    private final Context mContext;
    public List<ScanResult> bluetoothList = new ArrayList<>();

    public BluetoothTest(Context context) {
        mContext = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    public boolean checkBluetoothPermission() {
        boolean hasPermission = ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        if (!hasPermission) {
            Log.d(TAG, "Bluetooth no permission. BLUETOOTH_SCAN needed");
        }
        return hasPermission;
    }

    public void startBluetoothScan() {
        if (checkBluetoothPermission()) {
            bluetoothList.clear();
            bluetoothLeScanner.startScan(scanCallback);
            Log.d(TAG, "Bluetooth scan started");
        } else {
            Log.d(TAG, "Bluetooth scan not started: no permission.");
        }
    }

    public void stopBluetoothScan() {
        bluetoothLeScanner.stopScan(scanCallback);
        Log.d(TAG, "Bluetooth scan stopped");
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (!bluetoothList.contains(result)) {
                bluetoothList.add(result);
                Log.d(TAG, "Found device: " + result.getDevice().getName() + " [" + result.getDevice().getAddress() + "]");
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                if (!bluetoothList.contains(result)) {
                    bluetoothList.add(result);
                    Log.d(TAG, "Batch device: " + result.getDevice().getName() + " [" + result.getDevice().getAddress() + "]");
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Bluetooth scan failed with error: " + errorCode);
        }
    };

    public void printBluetoothList() {
        Log.d(TAG, "Bluetooth device list =======================");
        if (bluetoothList.isEmpty()) {
            Log.d(TAG, "No Bluetooth devices found.");
        } else {
            for (int i = 0; i < bluetoothList.size(); i++) {
                ScanResult result = bluetoothList.get(i);
                Log.d(TAG, (i + 1) + " : " + result.getDevice().getName() + " [" + result.getDevice().getAddress() + "]");
            }
        }
        Log.d(TAG, "Bluetooth device list end =======================");
    }
}
