package com.innopia.bist.view;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.R;
import com.innopia.bist.util.ServiceUtils;
import com.innopia.bist.util.Status;
import com.innopia.bist.util.SysInfo;
import com.innopia.bist.util.TestType;
import com.innopia.bist.util.UsbDetachReceiver;
import com.innopia.bist.viewmodel.MainViewModel;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends AppCompatActivity  {

    private static final String TAG = "BIST_MAIN";
    private MainViewModel mainViewModel;
    private BroadcastReceiver appUsbDetachReceiver;
    private ImageView ivWifiStatus;
    private ImageView ivBtStatus;
    private ImageView ivEthStatus;
    private TextView tvLogWindow;
    private ScrollView svLog;
    private static final int ALL_PERMISSIONS_REQUEST_CODE = 100;
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

        checkAndRequestPermissions();

        // ViewModel 초기화
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // UI 셋팅
        setupViews();
        setupObservers();

        mainViewModel.appendLog(TAG, "Activity starting.");
        mainViewModel.setDeviceInfo(SysInfo.getSystemInfo());

        // USB 분리 이벤트를 처리할 리시버 초기화
        appUsbDetachReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (UsbDetachReceiver.ACTION_USB_DETACHED_APP.equals(intent.getAction())) {
                    mainViewModel.appendLog(TAG, "USB device detached detected.");
                }
            }
        };

        checkAndLogBistServiceStatus();

    }

    @Override
    protected void onResume() {
        super.onResume();
        // 앱 내부용 USB 분리 감지 리시버 등록
        IntentFilter filter = new IntentFilter(UsbDetachReceiver.ACTION_USB_DETACHED_APP);
        ContextCompat.registerReceiver(this, appUsbDetachReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 리시버 등록 해제
        unregisterReceiver(appUsbDetachReceiver);
    }

    /**
        Add View (Button, TextView etc..)
     */
    private void setupViews() {
        ivWifiStatus = findViewById(R.id.iv_wifi_status);
        ivBtStatus = findViewById(R.id.iv_bt_status);
        ivEthStatus = findViewById(R.id.iv_ethernet_status);
        tvLogWindow = findViewById(R.id.text_log_window);
        svLog = findViewById(R.id.log_scroll_view);
        TextView mText1 = findViewById(R.id.text1);

        Button btnWifiTest = findViewById(R.id.button_wifi_test);
        btnWifiTest.setOnClickListener(v -> showTestFragment(WifiTestFragment.newInstance()));
        Button btn_BluetoothTest = findViewById(R.id.button_bt_test);
        btn_BluetoothTest.setOnClickListener(v -> showTestFragment(BluetoothTestFragment.newInstance()));
        Button btnEthernetTest = findViewById(R.id.button_ethernet_test);
        btnEthernetTest.setOnClickListener(v -> showTestFragment(EthernetTestFragment.newInstance()));
        Button btnHdmiTest = findViewById(R.id.button_hdmi_test);
        btnHdmiTest.setOnClickListener(v -> showTestFragment(HdmiTestFragment.newInstance()));
        Button btnVideoTest = findViewById(R.id.button_video_test);
        btnVideoTest.setOnClickListener(v -> showTestFragment(VideoTestFragment.newInstance()));
        Button btnRcuTest = findViewById(R.id.button_rcu_test);
        btnRcuTest.setOnClickListener(v -> showTestFragment(RcuTestFragment.newInstance()));
        Button btnCpuTest = findViewById(R.id.button_cpu_test);
        btnCpuTest.setOnClickListener(v -> showTestFragment(CpuTestFragment.newInstance()));
        Button btnMemoryTest = findViewById(R.id.button_memory_test);
        btnMemoryTest.setOnClickListener(v -> showTestFragment(MemoryTestFragment.newInstance()));
        Button btnUsbTest = findViewById(R.id.button_usb_test);
        btnUsbTest.setOnClickListener(v -> showTestFragment(UsbTestFragment.newInstance()));
        Button btnTest = findViewById(R.id.button_factory_reset_test);
        btnTest.setOnClickListener(v -> showTestFragment(FactoryResetTestFragment.newInstance()));
        Button btnUninstall = findViewById(R.id.button_uninstall); // layout에 해당 ID의 버튼이 있어야 함
        btnUninstall.setOnClickListener(v -> {
            mainViewModel.appendLog(TAG, "Uninstall button clicked. Requesting uninstall to BIST Service.");
            Intent intent = new Intent("com.innopia.bistservice.ACTION_REQUEST_UNINSTALL_FROM_APP");
            sendBroadcast(intent);
            // BIST App 종료
            finish();
        });

        /* Button Added? this code copy and paste >> modify test's name */
        //Button btnTest = findViewById(R.id.button__test);
        //btnTest.setOnClickListener(v -> showTestFragment(TestFragment.newInstance()));

        // 초기 디바이스 정보 설정
        mainViewModel.deviceInfoLiveData.observe(this, mText1::setText);
    }

    private void setupObservers() {
        // 로그창 업데이트 관찰
        mainViewModel.logLiveData.observe(this, logs -> {
            tvLogWindow.setText(String.join("\n", logs));
            svLog.post(() -> svLog.fullScroll(ScrollView.FOCUS_DOWN));
        });

        // 테스트 상태 아이콘 업데이트 관찰
        mainViewModel.testStatusesLiveData.observe(this, statuses -> {
            updateStatusIcon(ivWifiStatus, statuses.get(TestType.WIFI) == Status.ON, R.drawable.ic_wifi_on, R.drawable.ic_wifi_off);
            updateStatusIcon(ivBtStatus, statuses.get(TestType.BLUETOOTH) == Status.ON, R.drawable.ic_bt_on, R.drawable.ic_bt_off);
            updateStatusIcon(ivEthStatus, statuses.get(TestType.ETHERNET) == Status.ON, R.drawable.ic_ethernet_on, R.drawable.ic_ethernet_off);
        });
    }

//  androidx.Fragment 사용시 아래방법으로 전환
    private void showTestFragment(Fragment testFragment) {
        if (testFragment == null) return;
        mainViewModel.appendLog(TAG, testFragment.getClass().getSimpleName() + " button clicked. Opening fragment...");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, testFragment)
                .addToBackStack(null)
                .commit();
    }

    public void updateStatusIcon(ImageView imageView, boolean isConnected, int onIconResId, int offIconResId) {
        imageView.setImageDrawable(ContextCompat.getDrawable(this, isConnected ? onIconResId : offIconResId));
    }

    private void checkAndLogBistServiceStatus() {
        final String bistServiceClassName = "com.innopia.bistservice.BISTService";
        boolean isRunning = ServiceUtils.isServiceRunning(this, bistServiceClassName);
        String statusMessage = "BIST Service is " + (isRunning ? "running." : "not running.");
        mainViewModel.appendLog(TAG, statusMessage);
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
            Log.d(TAG, "Requesting necessary permissions...");
        } else {
            Log.d(TAG, "All necessary permissions already granted.");
        }
    }
}
