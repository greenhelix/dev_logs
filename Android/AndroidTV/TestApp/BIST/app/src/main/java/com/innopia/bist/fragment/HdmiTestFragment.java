package com.innopia.bist.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.innopia.bist.R;
import com.innopia.bist.viewmodel.HdmiTestViewModel;
import com.innopia.bist.viewmodel.MainViewModel;

/**
 * A Fragment that displays the UI for the HDMI test.
 * It allows the user to start the test and shows the results.
 */
public class HdmiTestFragment extends Fragment {

    private HdmiTestViewModel hdmiTestViewModel;
    private MainViewModel mainViewModel;
    private TextView tvHdmiInfo;

    public static HdmiTestFragment newInstance() {
        return new HdmiTestFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Custom factory to inject MainViewModel into HdmiTestViewModel.
        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass.isAssignableFrom(HdmiTestViewModel.class)) {
                    return (T) new HdmiTestViewModel(requireActivity().getApplication(), mainViewModel);
                }
                throw new IllegalArgumentException("Unknown ViewModel class");
            }
        };
        hdmiTestViewModel = new ViewModelProvider(this, factory).get(HdmiTestViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_hdmi_test, container, false);

        tvHdmiInfo = rootView.findViewById(R.id.text_hdmi_info);
        Button btnHdmiTest = rootView.findViewById(R.id.btn_hdmi_manual_test);

        // Set a click listener for the test button.
        btnHdmiTest.setOnClickListener(v -> {
            mainViewModel.appendLog(getTag(), "HDMI Test Start");
            hdmiTestViewModel.startManualTest();
        });

        // Observe the LiveData for test results and update the UI.
        hdmiTestViewModel.getTestResult().observe(getViewLifecycleOwner(), result -> {
            tvHdmiInfo.setText(result);
            mainViewModel.appendLog(getTag(), "HDMI Result \n" + result);
        });

        return rootView;
    }
}
