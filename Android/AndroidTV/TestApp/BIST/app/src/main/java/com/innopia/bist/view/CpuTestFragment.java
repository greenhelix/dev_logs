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
import com.innopia.bist.viewmodel.CpuTestViewModel;
import com.innopia.bist.viewmodel.MainViewModel;

public class CpuTestFragment extends Fragment {

    private CpuTestViewModel cpuTestViewModel;
    private MainViewModel mainViewModel;
    private TextView tvCpuInfo;

    public static CpuTestFragment newInstance() {
        return new CpuTestFragment();
    }

    /**
     * 1. 사용자 정의 ViewModel 팩토리 클래스
     * WifiTestViewModel 생성 시 필요한 Application과 MainViewModel을 모두 전달하는 역할을 합니다.
     */
    public static class CpuTestViewModelFactory implements ViewModelProvider.Factory {
        private final Application application;
        private final MainViewModel mainViewModel;

        public CpuTestViewModelFactory(@NonNull Application application, @NonNull MainViewModel mainViewModel) {
            this.application = application;
            this.mainViewModel = mainViewModel;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(CpuTestViewModel.class)) {
                // CpuTestViewModel을 생성할 때, 저장해둔 application과 mainViewModel 인스턴스를 전달합니다.
                return (T) new CpuTestViewModel(application, mainViewModel);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Activity와 생명주기를 공유하는 MainViewModel 가져오기
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // 위에서 정의한 사용자 정의 팩토리를 사용하여 WifiTestViewModel 인스턴스 생성
        CpuTestViewModelFactory factory = new CpuTestViewModelFactory(requireActivity().getApplication(), mainViewModel);
        cpuTestViewModel = new ViewModelProvider(this, factory).get(CpuTestViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_cpu_test, container, false);

        tvCpuInfo = rootView.findViewById(R.id.text_cpu_info);
        Button btnCpu = rootView.findViewById(R.id.btn_cpu_manual_test);
        Button btnCpuTest = rootView.findViewById(R.id.btn_cpu_auto_test);

        btnCpu.setOnClickListener(v -> {
            mainViewModel.appendLog("CpuTestFragment", "Scan button clicked. This part is not Worked");
            //startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        });

        btnCpuTest.setOnClickListener(v -> {
            tvCpuInfo.setText("Running CPU Test...");
            cpuTestViewModel.startManualTest();
        });

        // ViewModel의 LiveData를 관찰하여 UI 업데이트
        cpuTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> {
            tvCpuInfo.setText(result);
            boolean isConnected = result != null && !result.contains("not connected");
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 화면에 다시 나타날 때마다 테스트 실행
        cpuTestViewModel.startManualTest();
    }
}