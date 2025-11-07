package com.innopia.bist.info;

import android.os.SystemProperties;

import com.innopia.bist.util.LogManager;

public class HwInfo {

	private static final String TAG = "BIST|HwInfo";

	private static final String PROP_CPU = "ro.runtime.innopia.soc";
	private static final String PROP_DDR = "ro.runtime.innopia.ddr";
	private static final String PROP_EMMC = "ro.runtime.innopia.flash";
	private static final String PROP_WIFI = "ro.runtime.innopia.wifi";

	public static String getCpu() {
		LogManager.d(TAG, "getCpu ++");
		String cpu = SystemProperties.get(PROP_CPU, "");
		LogManager.d(TAG, "getCpu(cpu=" + cpu + ") --");
		return cpu;
	}

	public static String getDdr() {
		LogManager.d(TAG, "getDdr ++");
		String ddr = SystemProperties.get(PROP_DDR, "");
		LogManager.d(TAG, "getDdr(ddr=" + ddr + ") --");
		return ddr;
	}

	public static String getEmmc() {
		LogManager.d(TAG, "getEmmc ++");
		String emmc = SystemProperties.get(PROP_EMMC, "");
		LogManager.d(TAG, "getEmmc(emmc=" + emmc + ") --");
		return emmc;
	}

	public static String getWifi() {
		LogManager.d(TAG, "getWifi ++");
		String wifi = SystemProperties.get(PROP_WIFI, "");
		LogManager.d(TAG, "getWifi(wifi=" + wifi + ") --");
		return wifi;
	}
}
