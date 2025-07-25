package com.innopia.bist.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.innopia.bist.test.Test;
import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class BaseTestViewModel extends AndroidViewModel {

    protected final Test testModel;
    protected final MainViewModel mainViewModel;

    protected final MutableLiveData<TestResult> _testResultLiveData = new MutableLiveData<>();
    public final LiveData<TestResult> testResultLiveData = _testResultLiveData;

    public BaseTestViewModel(@NonNull Application application, Test testModel, MainViewModel mainViewModel) {
        super(application);
        this.testModel = testModel;
        this.mainViewModel = mainViewModel;
    }

    /**
     * [NEW] For simple tests that do not require any specific parameters.
     * This method automatically creates the params map with the application context.
     * ViewModels for CPU, Memory, etc., will call this method.
     */
    public void startTest() {
        mainViewModel.appendLog(getTag(), "Simple test started (no custom params).");
        // Create a basic map with just the context and call the main test execution logic.
        Map<String, Object> baseParams = new HashMap<>();
        baseParams.put("context", getApplication().getApplicationContext());
        executeManualTest(baseParams);
    }

    /**
     * [MODIFIED] For complex tests that require specific parameters (e.g., a BluetoothDevice).
     * This method takes custom parameters from the specific ViewModel, adds the context,
     * and then calls the main test execution logic.
     * The BluetoothTestViewModel will call this method.
     * @param customParams A map of custom parameters needed for the test.
     */
    public void startTest(Map<String, Object> customParams) {
        mainViewModel.appendLog(getTag(), "Complex test started with custom params.");
        // Add the application context to the custom parameters provided by the subclass.
        if (customParams == null) {
            customParams = new HashMap<>();
        }
        customParams.putIfAbsent("context", getApplication().getApplicationContext());
        executeManualTest(customParams);
    }

    /**
     * [RENAMED & MADE PRIVATE] The core, private test execution logic.
     * This is called by the public startTest() methods.
     * @param params The final map of parameters to be passed to the test model.
     */
    private void executeManualTest(Map<String, Object> params) {
        // The callback logic remains the same and is now fully centralized.
        Consumer<TestResult> callback = result -> {
            _testResultLiveData.postValue(result);
            String logMessage = "Test finished. Result: " + result.getStatus().name();
            mainViewModel.appendLog(getTag(), logMessage);
            mainViewModel.updateTestResult(getTestType(), result.getStatus());
        };

        // Run the test model.
        testModel.runManualTest(params, callback);
    }

    // Abstract methods to be implemented by subclasses.
    protected abstract String getTag();
    protected abstract TestType getTestType();
}
