package com.ik.innopia.hubist.main.bluetooth;
//package com.innopia.bist;

import android.util.Log;

public class BluetoothTestFragment extends Fragment {

    private static final String TAG = "BIST";

    private BluetoothTest bluetoothTest;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bluetooth_test, container, false);

        Button btnScan = rootView.findViewById(R.id.btn_bluetooth_scan);
        Button btnTest = rootView.findViewById(R.id.btn_bluetooth_test);

        bluetoothTest = new BluetoothTest(getActivity());

        btnScan.setOnClickListener(v-> {
            bluetoothTest.startBluetoothScan();
        });
    }
        

}