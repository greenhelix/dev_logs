package com.innopia.bist.view;

import android.app.Application;
import android.os.Bundle;
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
import com.innopia.bist.viewmodel.CpuTestViewModel;
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.MemoryTestViewModel;

public class MemoryTestFragment extends Fragment {

    private MemoryTestViewModel memoryTestViewModel;
    private MainViewModel mainViewModel;
    private TextView tvMemoryInfo;

    public static MemoryTestFragment newInstance() {
        return new MemoryTestFragment();
    }

    /**
     * 1. 사용자 정의 ViewModel 팩토리 클래스
     * WifiTestViewModel 생성 시 필요한 Application과 MainViewModel을 모두 전달하는 역할을 합니다.
     */
    public static class MemoryTestViewModelFactory implements ViewModelProvider.Factory {
        private final Application application;
        private final MainViewModel mainViewModel;

        public MemoryTestViewModelFactory(@NonNull Application application, @NonNull MainViewModel mainViewModel) {
            this.application = application;
            this.mainViewModel = mainViewModel;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(MemoryTestViewModel.class)) {
                // MemoryTestViewModel을 생성할 때, 저장해둔 application과 mainViewModel 인스턴스를 전달합니다.
                return (T) new MemoryTestViewModel(application, mainViewModel);
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
        MemoryTestViewModelFactory factory = new MemoryTestViewModelFactory(requireActivity().getApplication(), mainViewModel);
        memoryTestViewModel = new ViewModelProvider(this, factory).get(MemoryTestViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_memory_test, container, false);

        tvMemoryInfo = rootView.findViewById(R.id.text_memory_info);
        Button btnMemoryManTest = rootView.findViewById(R.id.btn_memory_manual_test);
        Button btnMemoryAutoTest = rootView.findViewById(R.id.btn_memory_auto_test);

        btnMemoryManTest.setOnClickListener(v -> {
            tvMemoryInfo.setText("Running Memory Manual Test...");
            memoryTestViewModel.startManualTest();
        });

        btnMemoryAutoTest.setOnClickListener(v -> {
            tvMemoryInfo.setText("Running Memory Auto Test...");
            memoryTestViewModel.startAutoTest();
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 화면에 다시 나타날 때마다 테스트 실행
        memoryTestViewModel.startManualTest();
    }
}