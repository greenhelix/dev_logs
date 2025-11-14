package com.innopia.bist;

import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;

public class TestActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_main);

        // Fragment를 코드로 추가
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_frame, new TestMainFragment())
                    .commitNow();
        }
    }
}