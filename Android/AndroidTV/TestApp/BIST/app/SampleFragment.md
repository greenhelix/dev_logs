# Sample Fragment

```java
package com.innopia.bist.ver1.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.R;
import com.innopia.bist.ver1.viewmodel.MainViewModel;
import com.innopia.bist.ver1.viewmodel.SampleTestViewModel;

/**
 * Fragment for the Sample Test. It provides UI for user input and displays results.
 * It interacts with SampleTestViewModel.
 */
public class SampleTestFragment extends Fragment {

    private SampleTestViewModel viewModel;
    private EditText etTestInput;
    private TextView tvTestResult;

    public static SampleTestFragment newInstance() {
        return new SampleTestFragment();
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
                return (T) new SampleTestViewModel(requireActivity().getApplication(), mainViewModel);
            }
        };
        viewModel = new ViewModelProvider(this, factory).get(SampleTestViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Assume a layout file named 'fragment_sample_test.xml' exists.
        // It should contain:
        // - An EditText with id 'edit_text_sample_input'
        // - A Button with id 'btn_sample_test_start'
        // - A TextView with id 'text_sample_test_result'
        View rootView = inflater.inflate(R.layout.fragment_sample_test, container, false);

        etTestInput = rootView.findViewById(R.id.edit_text_sample_input);
        tvTestResult = rootView.findViewById(R.id.text_sample_test_result);
        Button btnStartTest = rootView.findViewById(R.id.btn_sample_test_start);

        btnStartTest.setOnClickListener(v -> {
            String input = etTestInput.getText().toString();
            // Call the ViewModel method to start the test with the user's input.
            viewModel.startManualTest(input);
        });

        observeViewModel();

        return rootView;
    }

    private void observeViewModel() {
        // Observe the testResult LiveData from the ViewModel.
        // The getTestResult() method is inherited from BaseTestViewModel.
        viewModel.getTestResult().observe(getViewLifecycleOwner(), result -> {
            // When the result changes, update the TextView.
            tvTestResult.setText(result);
        });
    }
}

```