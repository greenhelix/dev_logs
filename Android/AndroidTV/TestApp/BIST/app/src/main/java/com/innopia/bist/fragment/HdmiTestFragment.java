package com.innopia.bist.fragment;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.HdmiTestViewModel;

public class HdmiTestFragment extends Fragment {

    private HdmiTestViewModel hdmiTestViewModel;
    private TextView tvHdmiInfo;
    private TextView tvTestResult;

    public static HdmiTestFragment newInstance() {
        return new HdmiTestFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We don't need a custom factory if the ViewModel constructor matches standard patterns.
        // If it requires MainViewModel, we might need a factory like in BluetoothTestFragment.
        // For simplicity, let's assume a simpler ViewModel or use the Bluetooth factory pattern.
        MainViewModel mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        // Custom factory to inject MainViewModel.
        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new HdmiTestViewModel(requireActivity().getApplication(), mainViewModel);
            }
        };
        hdmiTestViewModel = new ViewModelProvider(this, factory).get(HdmiTestViewModel.class);
    }
}