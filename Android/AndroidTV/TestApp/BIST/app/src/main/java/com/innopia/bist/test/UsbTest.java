package com.innopia.bist.test;

import android.content.Context;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class UsbTest implements Test {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final int TEST_FILE_SIZE_MB = 20;

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<String> callback) {
        executor.execute(() -> {
            Context context = (Context) params.get("context");
            if (context == null) {
                callback.accept("Error: Context is null");
                return;
            }
            File usbDrive = getUsbDrive(context);
            if (usbDrive == null) {
                callback.accept("USB drive not found or not accessible.");
                return;
            }
            callback.accept(performSpeedTest(usbDrive));
        });
    }

//    @Override
//    public void runAutoTest(Map<String, Object> params, Consumer<String> callback) {
//        runManualTest(params, callback);
//    }

    private File getUsbDrive(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        for (StorageVolume volume : storageManager.getStorageVolumes()) {
            if (volume.isRemovable() && volume.getDirectory() != null) {
                return volume.getDirectory();
            }
        }
        return null;
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
}
