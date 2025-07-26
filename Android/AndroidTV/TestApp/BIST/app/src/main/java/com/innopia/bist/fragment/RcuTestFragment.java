package com.innopia.bist.fragment;

import android.app.Application;
import android.os.Bundle;
import android.view.KeyEvent;
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
import com.innopia.bist.test.RcuTest;
import com.innopia.bist.viewmodel.RcuTestViewModel;
import com.innopia.bist.viewmodel.MainViewModel;

import java.util.HashMap;
import java.util.Map;

public class RcuTestFragment extends Fragment {

    private static final String TAG = "RcuTestFragment";

    private RcuTestViewModel rcuTestViewModel;
    private MainViewModel mainViewModel;
    private View rootView;
    private Button btnRcuTest;
    private TextView tvRcuTest;
    private RcuTest rcuTest = new RcuTest();
    private boolean testInProgress = false;

    public static RcuTestFragment newInstance() {
        return new RcuTestFragment();
    }

    public static class RcuTestViewModelFactory implements ViewModelProvider.Factory {

        private final Application application;
        private final RcuTest rcuTest;
        private final MainViewModel mainViewModel;

        public RcuTestViewModelFactory(@NonNull Application application, @NonNull RcuTest rcuTest, @NonNull MainViewModel mainViewModel) {
            this.application = application;
            this.rcuTest = rcuTest;
            this.mainViewModel = mainViewModel;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(RcuTestViewModel.class)) {
                return (T) new RcuTestViewModel(application, rcuTest, mainViewModel);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        RcuTestViewModelFactory factory = new RcuTestViewModelFactory(
                requireActivity().getApplication(),
                rcuTest,
                mainViewModel
        );
        rcuTestViewModel = new ViewModelProvider(this, factory).get(RcuTestViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_rcu_test, container, false);
        tvRcuTest = rootView.findViewById(R.id.text_rcu_test);

        // Request focus
        rootView.setFocusable(true);
        rootView.post(() -> rootView.requestFocus());

        btnRcuTest = rootView.findViewById(R.id.btn_rcu_test);
        btnRcuTest.setOnClickListener(v -> {
            tvRcuTest.setText("Running RCU Test...");
            startManualTest();
        });

        //rcuTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> {
        //	tvRcuTest.setText(result);
        //});
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        //rcuTestViewModel.startManualTest();
        startManualTest();
    }

    private void startManualTest() {
        // Start test
        testInProgress = true;
        mainViewModel.appendLog(TAG, "Manual test started.");
        Map<String, Object> params = new HashMap<>();
        params.put("context", getContext());

        rcuTest.runManualTest(params, result -> {
            requireActivity().runOnUiThread(() -> tvRcuTest.setText(result.getMessage()));
            if (result.getMessage().contains("Test Passed")) {
                rootView.setFocusable(false);
                btnRcuTest.setFocusable(true);
                tvRcuTest.setFocusable(true);
                testInProgress = false;
                mainViewModel.appendLog(TAG, "Manual test finished.");
                tvRcuTest.requestFocus();
            }
        });

        // Request key focus
        rootView.setFocusable(true);
        rootView.requestFocus();
        rootView.setOnKeyListener((v, keyCode, event) -> {
            if (testInProgress) {
                rcuTest.onKeyEvent(event); // Pass to RcuTest
                return true;
            }
            return false;
        });
    }
}
