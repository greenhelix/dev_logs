## Sample Test 

이 코드는 테스트 코드의 예시 양식에 대한 설명과 샘플 입니다.

- **runManualTest()함수** : 이 함수는 반드시 포함되야하는 함수이며, ViewModel에서 불려집니다. 
- 이 외에 테스트에 필요한 모든 함수를 구현하여 추가합니다.
- 그리고 그 함수들을 runManualTest에서 실행시키기만 하면 됩니다.

- **param** : runManualTest는 params를 받아서 오는데 여기에는 context를 대표적으로 받아올 수 있습니다. 
이 외에 다른 정보들이 필요하다면 view-viewModel 에서 param을 통해 받아 올 수 있습니다. 

- callback :  테스트의 결과는 callback을 통해 다시 보내줄 수 있습니다. 

```java
package com.innopia.bist.test;

import android.content.Context;
import android.util.Log;
import java.util.Map;
import java.util.function.Consumer;

/**
* A sample test implementation that demonstrates the basic structure.
* It implements the Test interface.
  */
  public class SampleTest implements Test {
  private static final String TAG = "SampleTest";
  // Define a key for a custom parameter.
  public static final String PARAM_INPUT_STRING = "inputString";

  /**
    * Runs a manual test with a given input string.
    * @param params A map containing parameters. Expected keys: 'context' and 'inputString'.
    * @param callback A consumer to return the test result string.
      */
      @Override
      public void runManualTest(Map<String, Object> params, Consumer<String> callback) {
      Context context = (Context) params.get("context");
      // Retrieve the custom parameter from the map.
      String input = (String) params.get(PARAM_INPUT_STRING);

      if (context == null || input == null) {
      Log.e(TAG, "Context or input string is null.");
      callback.accept("Test Result: FAIL\nReason: Missing required parameters.");
      return;
      }

      // Simulate a test process. For example, check if the input is not empty.
      // In a real scenario, this would contain complex test logic.
      new Thread(() -> {
      try {
      Log.d(TAG, "Starting sample test with input: " + input);
      // Simulate a delay for the test.
      Thread.sleep(1000);

               if (input.equalsIgnoreCase("test")) {
                   callback.accept("Test Result: PASS\nDetails: Input validation successful.");
               } else {
                   callback.accept("Test Result: FAIL\nReason: Input did not match expected value 'test'.");
               }
           } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               callback.accept("Test Result: FAIL\nReason: Test was interrupted.");
           }
      }).start();
      }
      }
```