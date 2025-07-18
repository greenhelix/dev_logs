# Sample ViewModel


```java
package com.innopia.bist.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import com.innopia.bist.test.SampleTest;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * ViewModel for the SampleTest. It extends BaseTestViewModel to reuse common logic.
 */
public class SampleTestViewModel extends BaseTestViewModel {
    private static final String TAG = "SampleTestViewModel";

    public SampleTestViewModel(@NonNull Application application, @NonNull MainViewModel mainViewModel) {
        // Pass a new instance of our test model to the base class.
        super(application, new SampleTest(), mainViewModel);
    }

    /**
     * Overrides the base method to provide specific parameters for SampleTest.
     * This method is called from the Fragment to start the test.
     * @param inputString The custom parameter needed for this specific test.
     */
    public void startManualTest(@NonNull String inputString) {
        mainViewModel.appendLog(getTag(), "Manual test started with input: " + inputString);
        
        // The callback logic is handled by the base class, but you can customize it.
        // Here, we just define how to pass parameters.
        Consumer<String> callback = result -> {
            // The base class already has a LiveData for the result. We just post the value.
            _testResultLiveData.postValue(result);
            mainViewModel.appendLog(getTag(), "Manual test finished. Result: " + result);
        };
        
        // Prepare parameters for the test model.
        Map<String, Object> params = new HashMap<>();
        params.put("context", getApplication().getApplicationContext());
        params.put(SampleTest.PARAM_INPUT_STRING, inputString); // Add the custom parameter.
        
        // Call the test model's execution method.
        testModel.runManualTest(params, callback);
    }
    
    @Override
    protected String getTag() {
        return TAG;
    }
}

```