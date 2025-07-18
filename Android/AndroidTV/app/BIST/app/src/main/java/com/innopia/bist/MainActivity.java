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
import com.innopia.bist.tests.ethernet.EthernetTest;
import com.innopia.bist.tests.ethernet.EthernetTestFragment;
import com.innopia.bist.util.FocusNavigationHandler;
import com.innopia.bist.util.ILogger;
import com.innopia.bist.util.SysInfo;
import com.innopia.bist.tests.wifi.WifiTest;
import com.innopia.bist.tests.wifi.WifiTestFragment;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends Activity {

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
    public EthernetTest etherTest;

    public boolean isFocusFeatureEnabled;
    public ILogger logUtil;

    private final String[] REQUIRED_PERMISSIONS = new String[] {
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

        ivWifiStatus = findViewById(R.id.iv_wifi_status);
        ivBtStatus = findViewById(R.id.iv_bt_status);
        ivEthStatus = findViewById(R.id.iv_ethernet_status);
        tvLogWindow = findViewById(R.id.text_log_window);
        svLog = findViewById(R.id.log_scroll_view);

        svLog.setOnKeyListener((v, keyCode, event) -> {
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
            return false;
        });

        logUtil = new ILogger() {
            @Override
            public void log(String tag, String message) {
                appendToLog(tag+": "+message);
            }

            @Override
            public void log(String message) {
                appendToLog(TAG+": "+message);
            }
        };

        TextView mText1 = findViewById(R.id.text1);
        mText1.setText(SysInfo.getSystemInfo());

        wifiTest = new WifiTest(this, logUtil);
        bluetoothTest = new BluetoothTest(this, logUtil);
        etherTest = new EthernetTest(this, logUtil);

        checkAndRequestPermissions();

        Button btnWifiTest = findViewById(R.id.button_wifi_test);
        btnWifiTest.setOnClickListener(v -> showTestFragment(WifiTestFragment.newInstance()));
        Button btnBluetoothTest = findViewById(R.id.button_bt_test);
        btnBluetoothTest.setOnClickListener(v -> showTestFragment(BluetoothTestFragment.newInstance()));
        Button btnEternetTest = findViewById(R.id.button_ethernet_test);
        btnEternetTest.setOnClickListener(v -> showTestFragment(EthernetTestFragment.newInstance()));

        loadFocusFeatureSetting();
        refreshFocusFeatures();
    }

    private void showTestFragment(Fragment testFragment) {
        if (testFragment == null) return;
        logUtil.log(TAG, testFragment.getClass().getSimpleName() + " button clicked. Opening fragment...");
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, testFragment)
                .addToBackStack(null)
                .commit();
    }

    public void updateStatusIcon(ImageView imageView, boolean isConnected, int onIconResId, int offIconResId) {
        runOnUiThread(() -> {
            imageView.setImageDrawable(ContextCompat.getDrawable(this, isConnected ? onIconResId : offIconResId));
        });
    }

    public void appendToLog(String message) {
        runOnUiThread(() -> {
            tvLogWindow.append("\n" + message);
            svLog.post(() -> svLog.scrollTo(0, tvLogWindow.getHeight()));
        });
    }

    public WifiTest getWifiTest() { return wifiTest; }
    public BluetoothTest getBluetoothTest() { return bluetoothTest; }
    public EthernetTest getEtherTest() { return etherTest; }

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
