package com.innopia.bist;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.tests.bluetooth.BluetoothTestFragment;
import com.innopia.bist.tests.wifi.WifiTestFragment;
import com.innopia.bist.viewmodel.MainViewModel;

public class MainActivity extends AppCompatActivity {

    private MainViewModel mainViewModel;
    private TextView logWindow;
    private ImageView ivWifiStatus;
    private ImageView ivBtStatus;
    private ImageView ivEthStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ViewModel 인스턴스 가져오기
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupUI();
        setupListeners();
        observeViewModel();

        // 초기 프래그먼트 로드
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, WifiTestFragment.newInstance())
                    .commit();
        }
    }

    private void setupUI() {
        logWindow = findViewById(R.id.text_log_window);
        ivWifiStatus = findViewById(R.id.iv_wifi_status);
        ivBtStatus = findViewById(R.id.iv_bt_status);
        ivEthStatus = findViewById(R.id.iv_ethernet_status);
        // ... (다른 UI 요소 초기화)
    }

    private void setupListeners() {
        findViewById(R.id.button_wifi_test).setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, WifiTestFragment.newInstance())
                    .addToBackStack(null)
                    .commit();
            mainViewModel.appendLog("Wi-Fi Test fragment loaded.");
        });
        // ... (다른 테스트 버튼 리스너 추가)
        findViewById(R.id.button_bt_test).setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, BluetoothTestFragment.newInstance())
                    .addToBackStack(null)
                    .commit();
            mainViewModel.appendLog("Bluetooth Test fragment loaded.");
        });
    }

    private void observeViewModel() {
        // 로그 LiveData 관찰하여 로그창 업데이트
        mainViewModel.logOutput.observe(this, logText -> logWindow.setText(logText));

        // Wi-Fi 상태 LiveData 관찰하여 아이콘 업데이트
        mainViewModel.wifiStatus.observe(this, isConnected -> {
            // R.drawable.ic_wifi_on, ic_wifi 리소스가 있다고 가정
            ivWifiStatus.setImageResource(isConnected ? R.drawable.ic_wifi_on : R.drawable.ic_wifi);
        });
    }
}
