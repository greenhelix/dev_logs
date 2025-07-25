package com.innopia.bist.fragment;

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

public class CpuTestFragment extends Fragment {

    private CpuTestViewModel cpuTestViewModel;
    private MainViewModel mainViewModel;
    private TextView tvCpuInfo;

    public static CpuTestFragment newInstance() {
        return new CpuTestFragment();
    }

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
                return (T) new CpuTestViewModel(application, mainViewModel);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        CpuTestViewModelFactory factory = new CpuTestViewModelFactory(requireActivity().getApplication(), mainViewModel);
        cpuTestViewModel = new ViewModelProvider(this, factory).get(CpuTestViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_cpu_test, container, false);
        tvCpuInfo = rootView.findViewById(R.id.text_cpu_info);
        Button btnCpuTest = rootView.findViewById(R.id.btn_cpu_manual_test);
        btnCpuTest.setOnClickListener(v -> {
            mainViewModel.appendLog("CpuTestFragment", "Cpu Test Start");
            cpuTestViewModel.startTest();
        });

        cpuTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> {
            tvCpuInfo.setText(result.getMessage());
        });

        return rootView;
    }
}