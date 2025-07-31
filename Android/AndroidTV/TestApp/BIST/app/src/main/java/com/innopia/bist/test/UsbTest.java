package com.innopia.bist.test;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class UsbTest implements Test {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final int TEST_FILE_SIZE_MB = 20;

    private File getUsbDrive(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        for (StorageVolume volume : storageManager.getStorageVolumes()) {
            if (volume.isRemovable() && volume.getDirectory() != null) {
                return volume.getDirectory();
            }
        }
        return null;
    }

    private String getUsbInfo(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            return "Can't Get UsbManager.";
        }

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        StringBuilder builder = new StringBuilder();

        if (deviceList.isEmpty()) {
            builder.append("No Connected USB.\n");
        } else {
            builder.append("--- Connected USB List ---\n\n");
            for (UsbDevice device : deviceList.values()) {
                builder.append("============================\n");

                builder.append("Device Name: ").append(device.getDeviceName()).append("\n");
                builder.append("Device ID: ").append(device.getDeviceId()).append("\n");
                builder.append("Vendor ID: ").append(device.getVendorId()).append("\n");
                builder.append("Product ID: ").append(device.getProductId()).append("\n");
                builder.append("Manufacturer Name: ").append(device.getManufacturerName()).append("\n");
                builder.append("Product Name: ").append(device.getProductName()).append("\n");

                try {
                    builder.append("Serial Number: ").append(device.getSerialNumber()).append("\n");
                } catch (SecurityException e) {
                    builder.append("Serial Number: no permission.\n");
                }
                builder.append("============================\n\n");
            }
        }
        return builder.toString();
    }

    private String performSpeedTest(File path) {
        File testFile = new File(path, "bist_speed_test.tmp");
        byte[] data = new byte[1024 * 1024];

        try {
            long startTime = System.nanoTime();
            FileOutputStream fos = new FileOutputStream(testFile);
            for (int i = 0; i < TEST_FILE_SIZE_MB; i++) {
                fos.write(data);
            }
            fos.close();
            long endTime = System.nanoTime();
            double writeSpeed = (double) TEST_FILE_SIZE_MB / ((endTime - startTime) / 1_000_000_000.0);

            startTime = System.nanoTime();
            FileInputStream fis = new FileInputStream(testFile);
            while (fis.read(data) != -1) {
            }
            fis.close();
            endTime = System.nanoTime();
            double readSpeed = (double) TEST_FILE_SIZE_MB / ((endTime - startTime) / 1_000_000_000.0);

            testFile.delete();

            return String.format("== USB Speed Test ==\nWrite: %.2f MB/s\nRead: %.2f MB/s", writeSpeed, readSpeed);

        } catch (IOException e) {
            testFile.delete();
            return "USB Speed Test Failed: " + e.getMessage();
        }
    }

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
        usbTest(params, callback);
    }

    @Override
    public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
        usbTest(params, callback);
    }

    private void executeTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executor.execute(() -> {
            Context context = (Context) params.get("context");
            if (context == null) {
                callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null"));
                return;
            }
            callback.accept(new TestResult(TestStatus.PASSED, "USB Test pass"));
        });
    }

    private void usbTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executor.execute(() -> {
            Context context = (Context) params.get("context");
            if (context == null) {
                callback.accept(new TestResult(TestStatus.ERROR,"Error: Context is null"));
                return;
            }
            String result = getUsbInfo(context);
			if (result.contains("PASS"))  {
				callback.accept(new TestResult(TestStatus.PASSED, "USB Test Pass \n"+result));
			} else {
				callback.accept(new TestResult(TestStatus.FAILED, "USB Test Fail \n"+result));
			}
        });
    }
}
