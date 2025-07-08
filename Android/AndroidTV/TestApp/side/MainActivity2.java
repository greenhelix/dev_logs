package com.ik.innopia.hubist.side;

import android.os.Bundle;
// Activity 대신 AppCompatActivity를 상속합니다.
import androidx.appcompat.app.AppCompatActivity;
// androidx 버전의 Fragment와 FragmentTransaction을 import 합니다.
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.ik.innopia.hubist.R;
import com.ik.innopia.hubist.side.test.bluetooth.BluetoothTestFragment;
import com.ik.innopia.hubist.side.ui.LeftFragment;
import com.ik.innopia.hubist.side.ui.RightFragment;
import com.ik.innopia.hubist.side.test.wifi.WifiTestFragment;

// extends Activity -> extends AppCompatActivity
// LeftFragment의 인터페이스를 구현합니다. (인터페이스 이름은 OnTestMenuClickListener로 가정)
public class MainActivity2 extends AppCompatActivity implements LeftFragment.OnTestMenuClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // 앱이 처음 실행될 때 (savedInstanceState가 null일 때)
        // 양쪽 패널에 초기 프래그먼트를 모두 코드로 추가합니다.
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    // 1. 왼쪽 컨테이너에 LeftFragment 추가
                    .add(R.id.left_pane_container, new LeftFragment())
                    // 2. 오른쪽 컨테이너에 RightFragment 추가
                    .add(R.id.right_pane_container, new RightFragment())
                    .commit();
        }
    }

    // LeftFragment에서 정의한 인터페이스 메서드를 구현합니다.
    @Override
    public void onTestMenuSelected(String testName) {
        // 이 변수는 이제 androidx.fragment.app.Fragment 타입입니다.
        Fragment newFragment;

        // ... 다른 케이스들
        switch (testName) {
            case "WIFI":
                newFragment = new WifiTestFragment();
                break;
            case "BLUETOOTH":
                newFragment = new BluetoothTestFragment();
                break;
            default:
                newFragment = new RightFragment();
                break;
        }

        // 수정: getFragmentManager() -> getSupportFragmentManager()
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.right_pane_container, newFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }
}
