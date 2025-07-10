package com.innopia.bist;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.innopia.bist.tests.bluetooth.BluetoothTest;
import com.innopia.bist.tests.bluetooth.BluetoothTestFragment;
import com.innopia.bist.util.FocusNavigationHandler;
import com.innopia.bist.util.ILogger;
import com.innopia.bist.util.SysInfo;
import com.innopia.bist.tests.wifi.WifiTest;
import com.innopia.bist.tests.wifi.WifiTestFragment;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends Activity implements ILogger {

    private static final String TAG = "BIST_MAIN";
    private static final int ALL_PERMISSIONS_REQUEST_CODE = 100;
    public ImageView ivWifiStatus;
    public ImageView ivBtStatus;
    public ImageView ivEthStatus;
    private TextView tvLogWindow;
    private ScrollView svLog;
    private Button btnEthernetTest;
    public WifiTest wifiTest;
    public BluetoothTest bluetoothTest;
    public boolean isFocusFeatureEnabled;

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
        // ScrollView에 키 리스너를 설정하여 포커스 이동을 제어합니다.
        svLog.setOnKeyListener((v, keyCode, event) -> {
            // '위' 방향키를 눌렀을 때
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                // 현재 프래그먼트 컨테이너에 있는 프래그먼트를 찾습니다.
                Fragment currentFragment = getFragmentManager().findFragmentById(R.id.fragment_container);

                // 프래그먼트가 있고, FocusNavigationHandler 인터페이스를 구현했다면
                if (currentFragment instanceof FocusNavigationHandler) {
                    // 인터페이스의 메서드를 호출하여 포커스를 받을 타겟 뷰 ID를 가져옵니다.
                    int targetViewId = ((FocusNavigationHandler) currentFragment).getTargetFocusId(keyCode);

                    if (targetViewId != 0) {
                        // 프래그먼트의 뷰 내에서 타겟 뷰를 찾아 포커스를 요청합니다.
                        View targetView = currentFragment.getView().findViewById(targetViewId);
                        if (targetView != null) {
                            targetView.requestFocus();
                            return true; // 이벤트 처리를 완료했음을 알립니다.
                        }
                    }
                }
            } else if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                View targetView = this.findViewById(R.id.text1);
                if (targetView != null) {
                    targetView.requestFocus();
                }
                return true;
            }
            return false; // 그 외의 키는 기본 동작을 따릅니다.
        });

        TextView mText1 = findViewById(R.id.text1);
        mText1.setText(SysInfo.getSystemInfo());

        // BIST_RENEWAL: WifiTest 인스턴스는 한 번만 생성하여 프래그먼트에 제공합니다.
        wifiTest = new WifiTest(this, this);
        bluetoothTest = new BluetoothTest(this, this);

        // 앱 시작 시 권한 확인
        checkAndRequestPermissions();

        btnEthernetTest = findViewById(R.id.button_ethernet_test);

        Button btnWifiTest = findViewById(R.id.button_wifi_test);
        btnWifiTest.setOnClickListener(v -> {
            showWifiTestFragment();
        });

        Button btnBluetoothTest = findViewById(R.id.button_bt_test);
        btnBluetoothTest.setOnClickListener(v -> {
            showBluetoothTestFragment();
        });

        loadFocusFeatureSetting();
        refreshFocusFeatures();
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

    private void showBluetoothTestFragment() {
        log(TAG, "Bluetooth Test button clicked. Opening fragment...");
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        // newInstance 팩토리 메서드를 사용하여 ILogger(this)를 전달합니다.
        ft.replace(R.id.fragment_container, BluetoothTestFragment.newInstance(this));
        ft.addToBackStack(null); // 뒤로가기 버튼으로 프래그먼트를 닫을 수 있게 함
        ft.commit();
    }

    // BIST_RENEWAL: Fragment에서 호출할 공용 메서드들 (변경 없음)
    public void updateWifiIcon(boolean isConnected) {
        runOnUiThread(() -> {
            ivWifiStatus.setImageDrawable(ContextCompat.getDrawable(this, isConnected ? R.drawable.ic_wifi_on : R.drawable.ic_wifi_off));
        });
    }

    public void updateBluetoothIcon(boolean isConnected) {
        runOnUiThread(() -> {
            ivBtStatus.setImageDrawable(ContextCompat.getDrawable(this, isConnected ? R.drawable.ic_bt_on : R.drawable.ic_bt_off));
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
            svLog.post(() -> svLog.scrollTo(0, tvLogWindow.getHeight()));
        });
    }

    // BIST_RENEWAL: 프래그먼트가 WifiTest 인스턴스에 접근할 수 있도록 getter 제공
    public WifiTest getWifiTest() { return wifiTest; }
    public BluetoothTest getBluetoothTest() { return bluetoothTest; }

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

    private void loadFocusFeatureSetting() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        isFocusFeatureEnabled = prefs.getBoolean("focus_feature_enabled", true); // 기본값은 ON
    }

    private void saveFocusFeatureSetting() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("focus_feature_enabled", isFocusFeatureEnabled).apply();
    }

    public void refreshFocusFeatures() {
        // 1. 시각적 하이라이트를 갱신합니다.
        updateVisualHighlights(isFocusFeatureEnabled);
    }

    public boolean isFocusFeatureEnabled() {
        return isFocusFeatureEnabled;
    }

    private void updateVisualHighlights(boolean enabled) {
        // 버튼 외 다른 뷰들
        findViewById(R.id.text1).setBackgroundResource(enabled ? R.drawable.focusable_item_border : 0);
        findViewById(R.id.iv_wifi_status).setBackgroundResource(enabled ? R.drawable.focusable_item_border : 0);
        findViewById(R.id.iv_bt_status).setBackgroundResource(enabled ? R.drawable.focusable_item_border : 0);
        findViewById(R.id.iv_ethernet_status).setBackgroundResource(enabled ? R.drawable.focusable_item_border : 0);

        // ScrollView (전용 Selector 사용)
        findViewById(R.id.log_scroll_view).setBackgroundResource(enabled ? R.drawable.focusable_scrollview_border : R.color.black); // OFF일 때 검은색 단색 배경
    }

    private final int[] HIDDEN_COMMAND = {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_LEFT
    };
    private int commandIndex = 0;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // 키가 '눌렸을 때(ACTION_DOWN)'만 감지하여 중복 실행을 방지합니다.
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            // 입력된 키가 현재 커맨드 순서와 일치하는지 확인합니다.
            if (event.getKeyCode() == HIDDEN_COMMAND[commandIndex]) {
                // 일치하면 다음 순서로 인덱스를 증가시킵니다.
                commandIndex++;
            } else {
                // 일치하지 않으면 인덱스를 0으로 리셋합니다.
                commandIndex = 0;
            }

            // 인덱스가 커맨드의 전체 길이와 같아지면, 커맨드 입력이 성공한 것입니다.
            if (commandIndex == HIDDEN_COMMAND.length) {
                // 성공했으므로, 다시 처음부터 입력받을 수 있도록 인덱스를 리셋합니다.
                commandIndex = 0;

                // 여기서 원하는 기능(포커스 헬퍼 토글)을 실행합니다.
                toggleFocusFeature();
            }
        }
        // 원래의 키 이벤트를 시스템이 계속 처리하도록 전달합니다.
        return super.dispatchKeyEvent(event);
    }

    private void toggleFocusFeature() {
        // 상태를 반전시킵니다.
        isFocusFeatureEnabled = !isFocusFeatureEnabled;
        // 변경된 상태를 저장합니다.
        saveFocusFeatureSetting();
        // UI를 즉시 갱신합니다.
        refreshFocusFeatures();

        // 사용자에게 기능이 변경되었음을 알려줍니다.
        String status = isFocusFeatureEnabled ? "ON" : "OFF";
        Toast.makeText(this, "Focus Helper " + status, Toast.LENGTH_SHORT).show();
    }
}
