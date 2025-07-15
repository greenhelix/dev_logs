package com.innopia.bist.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.R;
import com.innopia.bist.util.Status;
import com.innopia.bist.util.SysInfo;
import com.innopia.bist.util.TestType;
import com.innopia.bist.viewmodel.MainViewModel;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends AppCompatActivity  {

    private static final String TAG = "BIST_MAIN";
    private static final int ALL_PERMISSIONS_REQUEST_CODE = 100;

    private MainViewModel mainViewModel;

    private ImageView ivWifiStatus;
    private ImageView ivBtStatus;
    private ImageView ivEthStatus;
    private TextView tvLogWindow;
    private ScrollView svLog;

    private final String[] REQUIRED_PERMISSIONS = new String[]{/* ... 권한 목록 ... */};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ViewModel 초기화
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // UI 셋팅
        setupViews();
        setupObservers();

        mainViewModel.appendLog(TAG, "Activity starting.");
        mainViewModel.setDeviceInfo(SysInfo.getSystemInfo());

    }

    @Override
    protected void onResume() {
        super.onResume();
        // 브로드캐스트 리시버 등록
        IntentFilter filter = new IntentFilter("com.innopia.bistservice.ACTION_USB_DETACHED");
//        registerReceiver(usbDetachReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 리시버 등록 해제
        unregisterReceiver(usbDetachReceiver);
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

//    android.Fragment 사용시 아래방법으로 전환
//    private void showTestFragment(Fragment testFragment) {
//        if (testFragment == null) return;
//        mainViewModel.appendLog(TAG, testFragment.getClass().getSimpleName() + " button clicked. Opening fragment...");
//        getFragmentManager().beginTransaction()
//                .replace(R.id.fragment_container, testFragment)
//                .addToBackStack(null)
//                .commit();
//    }

    public void updateStatusIcon(ImageView imageView, boolean isConnected, int onIconResId, int offIconResId) {
        imageView.setImageDrawable(ContextCompat.getDrawable(this, isConnected ? onIconResId : offIconResId));
    }

    private static final String BIST_PACKAGE_NAME = "com.innopia.bist";

    private BroadcastReceiver usbDetachReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.innopia.bistservice.ACTION_USB_DETACHED".equals(intent.getAction())) {
                // USB 분리 시 사용자에게 제거 여부를 묻는 다이얼로그 표시
                //showUninstallDialog();
            }
        }
    };

//    private void showUninstallDialog() {
//        new AlertDialog.Builder(this)
//                .setTitle("Uninstall BIST App")
//                .setMessage("USB device has been detached. Do you want to uninstall the BIST application?")
//                .setPositiveButton("Uninstall", (dialog, which) -> {
//                    BISTService.uninstallPackage(MainActivity.this, BIST_PACKAGE_NAME, this::finish);
//                })
//                .setNegativeButton("Cancel", null)
//                .show();
//    }

}
