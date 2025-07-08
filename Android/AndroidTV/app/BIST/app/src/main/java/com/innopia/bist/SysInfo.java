package com.innopia.bist;

import android.Manifest;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class SysInfo {
    private static final String TAG = "BIST_SYS_INFO";

    // getprop 명령으로 특정 속성 값을 읽어오는 헬퍼 메서드
    private static String getSystemProperty(String propName) {
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

    // 필요한 시스템 정보들을 모아서 문자열로 반환
    public static String getSystemInfo() {
        String model = getSystemProperty("ro.product.model");
        String fwVer = getSystemProperty("ro.build.version.incremental");
        String androidVer = getSystemProperty("ro.build.version.release");
        String buildDate = getSystemProperty("ro.build.date");
        String serialNo = "This info will be shown in system app"; // Manifest.permission.READ_PRIVILEGED_PHONE_STATE 필요
        Log.d(TAG, getSystemProperty("ro.serialno")); //null값 으로 보임 priv-app 이여야 보임

        // 각 항목의 출력 길이를 '%' 뒤의 숫자로 조절할 수 있습니다. (예: %-15s는 15칸을 차지)
        String line1 = String.format("Model: %-15s  FW Ver: %-15s Android Ver: %s", model, fwVer, androidVer);
        String line2 = "Build Date: " + buildDate;
        String line3 = "Serial No: "+serialNo;

        return line1 + "\n" + line2 + "\n" + line3;
    }
}
