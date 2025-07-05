package com.ik.innopia.hubist.main;
//package com.innopia.bist;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ik.innopia.hubist.R;
//import com.innopia.bist.R;
import com.ik.innopia.hubist.main.wifi.WifiTestFragment3;
//import com.innopia.bist.wifi.WifiTestFragment;

public class MainActivity extends Activity {

    private static final String TAG = "BIST";

    private TextView mText1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mText1 = findViewById(R.id.text1);
        this.mText1.setText("SystemInfo:\n" + "  HW / SW Ver.\n" + "  App Ver.\n" + "  Model Name\n" + "  Serial Number\n" + "  Date\n" + "  CPU Temp.\n" + "  Data Parition: encrypted\n" + "  MAC (Ethernet / Wi-Fi / BT)\n" + "  IP Addr.\n" + "HWInfo:\n" + "  DDR : Size, Type, Frequency\n" + "  Chip ID\n" + "  EMMC: Size, Type\n" + "  Wi-Fi: Module");

        Log.d(TAG, "onCreate()");

        Button btnWifiTest = findViewById(R.id.button_wifi_test);
        btnWifiTest.setOnClickListener(v ->{
            showWifiTestFragment();
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
    }

    private void showWifiTestFragment() {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.text_container, new WifiTestFragment());
        ft.commit();
    }

    private void showBluetoothTestFragment() {
         FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.text_container, new BluetoothTestFragment());
        ft.commit();
    }
}
