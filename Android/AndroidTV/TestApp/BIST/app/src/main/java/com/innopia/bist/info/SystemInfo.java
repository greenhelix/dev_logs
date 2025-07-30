package com.innopia.bist.info;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.os.Build;
import android.os.HardwarePropertiesManager;
//import android.os.SystemProperties;
import android.provider.Settings;

//import com.innopia.bist.util.FileReadHelper;
import com.innopia.bist.util.LogManager;
import com.innopia.bist.util.SysInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class SystemInfo {

    private static final String TAG = "BIST|SystemInfo";

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

    private Context mContext;

    public SystemInfo(Context context) {
        LogManager.d(TAG, "SystemInfo");
        mContext = context;
    }

    public static String getHwVersion() {
        LogManager.d(TAG, "getHwVersion ++");
//        String version = SystemProperties.get(PROP_HW_VERSION, "");
        String version = SysInfo.getSystemProperty(PROP_HW_VERSION);
        LogManager.d(TAG, "getHwVersion(version=" + version + ") --");
        return version;
    }

    public static String getSwVersion() {
        LogManager.d(TAG, "getSwVersion ++");
//        String version = SystemProperties.get(PROP_SW_VERSION, "");
        String version = SysInfo.getSystemProperty(PROP_SW_VERSION);
        LogManager.d(TAG, "getSwVersion(version=" + version + ") --");
        return version;
    }

    public String getAppVersion() {
        LogManager.d(TAG, "getAppVersion ++");
        String version = "";
        try {
            PackageInfo pi = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            int code = pi.versionCode;
            String name = pi.versionName;
            version = code + " / " + name;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        LogManager.d(TAG, "getAppVersion(version=" + version + ") --");
        return version;
    }

    public static String getModelName() {
        LogManager.d(TAG, "getModelName ++");
//        String name = SystemProperties.get(PROP_MODEL, "");
        String name = SysInfo.getSystemProperty(PROP_MODEL);
        LogManager.d(TAG, "getModleName(name=" + name + ") --");
        return name;
    }

    public static String getSerialNumber() {
        LogManager.d(TAG, "getSerialNumber ++");
//        String serial = Build.getSerial();
        String serial = SysInfo.getSystemProperty("ro.serialno");
        LogManager.d(TAG, "getSerialNumber(serial=" + serial + ") --");
        return serial;
    }

    public static String getDate() {
        LogManager.d(TAG, "getDate ++");
        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String date = sdf.format(currentTime);
        LogManager.d(TAG, "getDate(date=" + date + ") --");
        return date;
    }

    public String getCpuTemp() {
        LogManager.d(TAG, "getCpuTemp ++");
        HardwarePropertiesManager hpm = (HardwarePropertiesManager) mContext.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE);
        float[] temps = hpm.getDeviceTemperatures(HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU, HardwarePropertiesManager.TEMPERATURE_CURRENT);
        String temp = String.format("%f", temps[0]) + "°C";
        LogManager.d(TAG, "getCpuTemp(temp=" + temp + ") --");
        return temp;
    }

    public static String getDataPartition() {
        return "encrypted";
    }

    public String getEthernetMac() {
        LogManager.d(TAG, "getEthernetMac ++");
//        String mac = FileReadHelper.readFromFile(SYSFS_ETH0_MAC);
        String mac = SysInfo.getSystemProperty(SYSFS_ETH0_MAC);
        LogManager.d(TAG, "getEthernetMac(mac=" + mac + ") --");
        return mac;
    }

    public String getWifiMac() {
        LogManager.d(TAG, "getWifiMac ++");
//        String mac = FileReadHelper.readFromFile(SYSFS_WIFI_MAC);
        String mac = SysInfo.getSystemProperty(SYSFS_WIFI_MAC);
        LogManager.d(TAG, "getWifiMac(mac=" + mac + ") --");
        return mac;
    }

    public String getBtMac() {
        LogManager.d(TAG, "getBtMac ++");
//        String mac = Settings.Secure.getString(mContext.getContentResolver(), BLUETOOTH_ADDRESS).toLowerCase();
//        LogManager.d(TAG, "getBtMac(mac=" + mac + ") --");
        return "bt mac";
    }

    public String getIpAddress() {
        LogManager.d(TAG, "getIpAddress ++");
        ConnectivityManager cm = mContext.getSystemService(ConnectivityManager.class);
        LinkProperties lps = cm.getLinkProperties(cm.getActiveNetwork());
//        Iterator<LinkAddress> iter = lps.getAllLinkAddresses().iterator();
        Iterator<LinkAddress> iter = lps.getLinkAddresses().iterator();
        if (!iter.hasNext()) {
            LogManager.d(TAG, "getIpAddress !iter --");
        }

        List<String> ipv4 = new ArrayList<String>();
        List<String> ipv6 = new ArrayList<String>();
        while (iter.hasNext()) {
            String address = iter.next().getAddress().getHostAddress();
            if (Pattern.matches(IPV4_REGEX, address)) {
                ipv4.add(address);
            } else {
                ipv6.add(address);
            }
        }

        StringBuilder addresses = new StringBuilder();
        //int i = 0;
        // ipv4 - only show single ipv4 address
        Iterator<String> ipv4Iter = ipv4.iterator();
        //while (ipv4Iter.hasNext()) {
        if (ipv4Iter.hasNext()) {
            String address = ipv4Iter.next();
            //if (i > 0) addresses.append("		   ");
            addresses.append(address);
            //if (ipv4Iter.hasNext()) addresses.append("\n");
            //i++;
        }

        // ipv6
        //if (ipv6.size() > 0) addresses.append("\n");
        //Iterator<String> ipv6Iter = ipv6.iterator();
        //while (ipv6Iter.hasNext()) {
        //	String address = ipv6Iter.next();
        //	if (i > 0) addresses.append("		   ");
        //	addresses.append(address);
        //	if (ipv6Iter.hasNext()) addresses.append("\n");
        //	i++;
        //}
        LogManager.d(TAG, "getIpAddress(addresses=" + addresses.toString() + ") --");
        return addresses.toString();
    }
}
