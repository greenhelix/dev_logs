package com.innopia.bist.util;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class SysInfo {
    private static final String TAG = "BIST_SYS_INFO";
    private static final String PROP_HW_VERSION = "ro.oem.hw.version";
    private static final String PROP_SW_VERSION = "ro.oem.sw.version";
    private static final String PROP_MODEL = "ro.product.model";

    private static final String SYSFS_CPU_TEMP = "/sys/class/thermal/thermal_zone0/temp";
    private static final String SYSFS_ETH0_MAC = "/sys/class/net/eth0/address";
    private static final String SYSFS_WIFI_MAC = "/sys/class/net/wlan0/address";

    private static final String BLUETOOTH_ADDRESS = "bluetooth_address";

    private static final String IPV4_REGEX = "^(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
            "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
            "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
            "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    public static String getSystemProperty(String propName) {
        try {
            Process process = Runtime.getRuntime().exec("getprop " + propName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            return line != null ? line.trim() : "N/A";
        } catch (IOException e) {
            Log.e(TAG, "Failed to read system property " + propName, e);
            return "Error";
        }
    }

    public static String getSystemInfo() {
        String model = getSystemProperty("ro.product.model");
        String fwVer = getSystemProperty("ro.build.version.incremental");
        String androidVer = getSystemProperty("ro.build.version.release");
        String buildDate = getSystemProperty("ro.build.date");
        String serialNo = "This info will be shown in system app";
        Log.d(TAG, getSystemProperty("ro.serialno"));

        String line1 = String.format("Model: %-15s  FW Ver: %-15s", model, fwVer);
        String line2 = String.format("Build Date: %-15s Android Ver: %s",buildDate, androidVer);
        String line3 = "Serial No: "+serialNo;

        return line1 + "\n" + line2 + "\n" + line3;
    }
}
