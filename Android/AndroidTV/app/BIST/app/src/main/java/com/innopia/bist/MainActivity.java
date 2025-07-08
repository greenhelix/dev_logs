package com.innopia.bist;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.innopia.bist.wifi.WifiTest;
import com.innopia.bist.wifi.WifiTestFragment;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends Activity implements ILogger {

    private static final String TAG = "BIST_MAIN";
    private static final int ALL_PERMISSIONS_REQUEST_CODE = 100;
    ImageView ivWifiStatus;
    ImageView ivBtStatus;
    ImageView ivEthStatus;
    private TextView tvLogWindow;
    private ScrollView svLog;
    private Button btnEthernetTest;
    private WifiTest wifiTest;

    private final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.NEARBY_WIFI_DEVICES,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: Activity starting.");

        // UI 초기화
        ivWifiStatus = findViewById(R.id.iv_wifi_status);
        ivBtStatus = findViewById(R.id.iv_bt_status);
        ivEthStatus = findViewById(R.id.iv_ethernet_status);
        tvLogWindow = findViewById(R.id.text_log_window);
        svLog = findViewById(R.id.log_scroll_view);
        TextView mText1 = findViewById(R.id.text1);
        mText1.setText(SysInfo.getSystemInfo());

        // BIST_RENEWAL: WifiTest 인스턴스는 한 번만 생성하여 프래그먼트에 제공합니다.
        wifiTest = new WifiTest(this, this);

        // 앱 시작 시 권한 확인
        checkAndRequestPermissions();

        // 초기 포커스 설정
        btnEthernetTest = findViewById(R.id.button_ethernet_test);
        if (btnEthernetTest != null) {
            btnEthernetTest.requestFocus();
        }

        Button btnWifiTest = findViewById(R.id.button_wifi_test);
        btnWifiTest.setOnClickListener(v -> {
            showWifiTestFragment();
        });
    }

    private void showWifiTestFragment() {
        log(TAG, "Wi-Fi Test button clicked. Opening fragment...");
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        // newInstance 팩토리 메서드를 사용하여 ILogger(this)를 전달합니다.
        ft.replace(R.id.fragment_container, WifiTestFragment.newInstance(this));
        ft.addToBackStack(null); // 뒤로가기 버튼으로 프래그먼트를 닫을 수 있게 함
        ft.commit();
    }

    // BIST_RENEWAL: Fragment에서 호출할 공용 메서드들 (변경 없음)
    public void updateWifiIcon(boolean isConnected) {
        runOnUiThread(() -> {
            ivWifiStatus.setImageDrawable(ContextCompat.getDrawable(this, isConnected ? R.drawable.ic_wifi_on : R.drawable.ic_wifi_off));
        });
    }

    // ILogger 인터페이스 구현
    @Override
    public void log(String tag, String message) {
        appendToLog(tag + ": " + message);
    }

    public void appendToLog(String message) {
        runOnUiThread(() -> {
            tvLogWindow.append("\n" + message);
            svLog.post(() -> svLog.fullScroll(View.FOCUS_DOWN));
        });
    }

    // BIST_RENEWAL: 프래그먼트가 WifiTest 인스턴스에 접근할 수 있도록 getter 제공
    public WifiTest getWifiTest() {
        return wifiTest;
    }

    // --- 권한 및 설정 관련 메서드 (기존과 거의 동일) ---

    @Override
    protected void onResume() {
        super.onResume();
        if (btnEthernetTest != null) {
            btnEthernetTest.requestFocus();
        }
    }

    private void checkAndRequestPermissions() {
        boolean allPermissionsGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, ALL_PERMISSIONS_REQUEST_CODE);
            appendToLog("Requesting necessary permissions...");
        } else {
            appendToLog("All necessary permissions already granted.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ALL_PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                appendToLog("All permissions have been granted.");
            } else {
                appendToLog("Some permissions were denied. App functionality may be limited.");
                Toast.makeText(this, "Some permissions were denied. Certain features may not work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void showLocationSettingsAlert() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Location Services Required")
                .setMessage("This feature requires Location Services to be enabled. Please enable it in the settings to get Wi-Fi information.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
