package com.innopia.bist.view;

import android.app.Application;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.viewmodel.BluetoothTestViewModel;
import com.innopia.bist.viewmodel.MainViewModel;

public class BluetoothTestFragment extends Fragment {

    private BluetoothTestViewModel bluetoothTestViewModel;
    private MainViewModel mainViewModel;
    private TextView tvBluetoothInfo;

    public static BluetoothTestFragment newInstance() {
        return new BluetoothTestFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        BluetoothTestViewModelFactory factory = new BluetoothTestViewModelFactory(requireActivity().getApplication(), mainViewModel);
        bluetoothTestViewModel = new ViewModelProvider(this, factory).get(BluetoothTestViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public static class BluetoothTestViewModelFactory implements ViewModelProvider.Factory {
        private final Application application;
        private final MainViewModel mainViewModel;

        public BluetoothTestViewModelFactory(@NonNull Application application, @NonNull MainViewModel mainViewModel){
            this.application = application;
            this.mainViewModel = mainViewModel;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if(modelClass.isAssignableFrom(BluetoothTestViewModel.class)) {
                return (T) new BluetoothTestViewModel(application, mainViewModel);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
