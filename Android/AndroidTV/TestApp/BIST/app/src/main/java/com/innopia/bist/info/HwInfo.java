package com.innopia.bist.info;

//import android.os.SystemProperties;

import com.innopia.bist.util.LogManager;
import com.innopia.bist.util.SysInfo;

public class HwInfo {

    private static final String TAG = "BIST|HwInfo";

    private static final String PROP_CHIP_ID = "ro.runtime.innopia.soc";
    private static final String PROP_DDR = "ro.runtime.innopia.ddr";
    private static final String PROP_EMMC = "ro.runtime.innopia.flash";
    private static final String PROP_WIFI = "ro.runtime.innopia.wifi";

    public static String getChipId() {
        LogManager.d(TAG, "getChipId ++");
        String id = SysInfo.getSystemProperty(PROP_CHIP_ID);
        LogManager.d(TAG, "getChipId(id=" + id + ") --");
        return id;
    }

    public static String getDdr() {
        LogManager.d(TAG, "getDdr ++");
        String ddr = SysInfo.getSystemProperty(PROP_DDR);
        LogManager.d(TAG, "getDdr(ddr=" + ddr + ") --");
        return ddr;
    }

    public static String getEmmc() {
        LogManager.d(TAG, "getEmmc ++");
        String emmc = SysInfo.getSystemProperty(PROP_EMMC);
        LogManager.d(TAG, "getEmmc(emmc=" + emmc + ") --");
        return emmc;
    }

    public static String getWifi() {
        LogManager.d(TAG, "getWifi ++");
        String wifi = SysInfo.getSystemProperty(PROP_WIFI);
        LogManager.d(TAG, "getWifi(wifi=" + wifi + ") --");
        return wifi;
    }
}
