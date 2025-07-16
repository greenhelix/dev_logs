package com.innopia.bist.view;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.R;
import com.innopia.bist.util.Status;
import com.innopia.bist.util.TestType;
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.WifiTestViewModel;

public class WifiTestFragment extends Fragment {

    private WifiTestViewModel wifiTestViewModel;
    private MainViewModel mainViewModel;
    private TextView tvWifiInfo;

    public static WifiTestFragment newInstance() {
        return new WifiTestFragment();
    }

    /**
     * 1. 사용자 정의 ViewModel 팩토리 클래스
     * WifiTestViewModel 생성 시 필요한 Application과 MainViewModel을 모두 전달하는 역할을 합니다.
     */
    public static class WifiTestViewModelFactory implements ViewModelProvider.Factory {
        private final Application application;
        private final MainViewModel mainViewModel;

        public WifiTestViewModelFactory(@NonNull Application application, @NonNull MainViewModel mainViewModel) {
            this.application = application;
            this.mainViewModel = mainViewModel;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(WifiTestViewModel.class)) {
                // WifiTestViewModel을 생성할 때, 저장해둔 application과 mainViewModel 인스턴스를 전달합니다.
                return (T) new WifiTestViewModel(application, mainViewModel);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Activity와 생명주기를 공유하는 MainViewModel 가져오기
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // 2. 위에서 정의한 사용자 정의 팩토리를 사용하여 WifiTestViewModel 인스턴스 생성
        WifiTestViewModelFactory factory = new WifiTestViewModelFactory(
                requireActivity().getApplication(),
                mainViewModel
        );
        wifiTestViewModel = new ViewModelProvider(this, factory).get(WifiTestViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_wifi_test, container, false);

        tvWifiInfo = rootView.findViewById(R.id.text_wifi_info);
        Button btnWifiScan = rootView.findViewById(R.id.btn_wifi_scan);
        Button btnWifiTest = rootView.findViewById(R.id.btn_wifi_test);

        btnWifiScan.setOnClickListener(v -> {
            mainViewModel.appendLog("WifiTestFragment", "Scan button clicked. Opening system Wi-Fi settings...");
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        });

        btnWifiTest.setOnClickListener(v -> {
            tvWifiInfo.setText("Running Wi-Fi Test...");
            wifiTestViewModel.startManualTest();
        });

        // ViewModel의 LiveData를 관찰하여 UI 업데이트
        wifiTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> {
            mainViewModel.appendLog(getTag(), result);
            tvWifiInfo.setText(result);
            boolean isConnected = result != null && !result.contains("not connected");
            mainViewModel.updateTestStatus(TestType.WIFI, isConnected ? Status.ON : Status.OFF);
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        wifiTestViewModel.startManualTest();
    }
}
