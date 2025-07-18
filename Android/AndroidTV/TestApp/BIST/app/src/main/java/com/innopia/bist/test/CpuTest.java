package com.innopia.bist.test;

import android.content.Context;
import android.os.Build;
import android.os.HardwarePropertiesManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class CpuTest implements Test {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String TAG = "CpuTest";

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<String> callback) {
        executor.execute(() -> {
            Context context = (Context) params.get("context");
//            String temp = checkCpuTemperature();
            String temp = checkCpuTemperature(context);
            String speed = checkCpuSpeed();
            String result = "== CPU Test Result ==\n" + temp + "\n" + speed;
            callback.accept(result);
        });
    }

//    @Override
//    public void runAutoTest(Map<String, Object> params, Consumer<String> callback) {
//        executor.execute(() -> {
//            long startTime = System.currentTimeMillis();
//            while (System.currentTimeMillis() - startTime < 10000) {
//                // stress inject test
//            }
//            String result = "CPU Stress Test Completed. \n" + checkCpuSpeed();
//            callback.accept(result);
//        });
//    }

    private String checkCpuTemperature() {
        try {
            Process process = Runtime.getRuntime().exec("cat /sys/class/thermal/thermal_zone0/temp");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String temp = reader.readLine();
            if (temp != null) {
                float tempValue = Float.parseFloat(temp) / 1000.0f;
                return String.format("CPU Temperature: %.2f °C", tempValue);
            }
        } catch (Exception e) {
            return "CPU Temperature: Not Available";
        }
        return "CPU Temperature: Not Available";
    }

    private String checkCpuTemperature(Context context) {
        // This API requires Android N (Nougat) or higher.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return "CPU Temperature: Not Available (Requires Android 7.0+)";
        }

        try {
            // Get the HardwarePropertiesManager service.
            HardwarePropertiesManager hwpm = (HardwarePropertiesManager) context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE);
            if (hwpm == null) {
                return "CPU Temperature: Service not available on this device.";
            }

            // Fetch device temperatures. The type constant for CPU is 0.
            float[] temps = hwpm.getDeviceTemperatures(HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU, HardwarePropertiesManager.TEMPERATURE_CURRENT);

            // Check if the API returned a valid temperature.
            if (temps != null && temps.length > 0 && temps[0] != HardwarePropertiesManager.UNDEFINED_TEMPERATURE) {
                return String.format("CPU Temperature: %.2f °C (Official API)", temps[0]);
            } else {
                return "CPU Temperature: Not Available (API returned no data)";
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get CPU temperature.", e);
            return "CPU Temperature: Not Available (Error)";
        }
    }

    private String checkCpuSpeed() {
        try {
            Process process = Runtime.getRuntime().exec("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String speed = reader.readLine();
            if (speed != null) {
                float speedValue = Float.parseFloat(speed) / 1000.0f;
                return String.format("CPU Speed: %.0f MHz", speedValue);
            }
        } catch (Exception e) {
            return "CPU Speed: Not Available or ";
        }
        return "CPU Speed: Not Available";
    }
}
