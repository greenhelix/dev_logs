package com.innopia.bist.tests.bluetooth;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothTest {

    private static final String TAG = "BIST_BT_TEST";
    private final Context mContext;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public BluetoothTest(Context context) {
        mContext = context;
    }

    /**
     * BIST_RENEWAL: 현재 Blueooth 연결 상태를 확인하고 정보를 반환하는 핵심 메서드
     * 이 메서드는 Fragment의 onResume 등에서 호출됩니다.
     */

    /**
     * BIST_RENEWAL: Blueooth 유효성을 검사하는 private 헬퍼 메서드
     */
    private boolean isInternetAvailable() {
        return true;
    }

    /**
     * BIST_RENEWAL: 연결된 Blueooth 에  테스트를 실행하는 메서드
     */
    public void runBtTest() {

        executor.execute(() -> {

        });
    }

    public boolean checkWifiPermission() {
        return ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
