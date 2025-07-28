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
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.UsbTestViewModel;

public class UsbTestFragment extends Fragment {

    private UsbTestViewModel usbTestViewModel;
    private MainViewModel mainViewModel;
    private TextView tvUsbResult;

    public static UsbTestFragment newInstance() {
        return new UsbTestFragment();
    }

    public static class UsbTestViewModelFactory implements ViewModelProvider.Factory {
        private final Application application;
        private final MainViewModel mainViewModel;

        public UsbTestViewModelFactory(@NonNull Application application, @NonNull MainViewModel mainViewModel) {
            this.application = application;
            this.mainViewModel = mainViewModel;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(UsbTestViewModel.class)) {
                return (T) new UsbTestViewModel(application, mainViewModel);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        UsbTestViewModelFactory factory = new UsbTestViewModelFactory(requireActivity().getApplication(), mainViewModel);
        usbTestViewModel = new ViewModelProvider(this, factory).get(UsbTestViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_usb_test, container, false);
        tvUsbResult = rootView.findViewById(R.id.text_usb_info);
        Button btnUsbTest = rootView.findViewById(R.id.btn_usb_manual_test);
        btnUsbTest.setOnClickListener(v -> {
            mainViewModel.appendLog(getTag(), "Usb Test Start");
            usbTestViewModel.startTest();
        });

        usbTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> {
            tvUsbResult.setText(result.getMessage());
            mainViewModel.appendLog(getTag(), "Usb Result \n" + result);
        });

        return rootView;
    }
}