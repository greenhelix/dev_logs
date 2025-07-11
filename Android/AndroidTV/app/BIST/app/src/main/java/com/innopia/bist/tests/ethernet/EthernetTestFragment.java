package com.innopia.bist.tests.ethernet;

import android.content.Intent;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.innopia.bist.MainActivity;
import com.innopia.bist.R;
import com.innopia.bist.tests.ethernet.EthernetTest;
import com.innopia.bist.util.FocusNavigationHandler;
import com.innopia.bist.util.ILogger;

public class EthernetTestFragment extends Fragment implements FocusNavigationHandler {

    private static final String TAG = "BIST_ETHER_FRAG";
    private EthernetTest etherTest;
    private TextView tvEtherInfo;
    private Button btnEtherScan;
    private Button btnEtherTest;
    private ILogger mLogger;
    private Network currentNetwork;

    public static EthernetTestFragment newInstance() {
        return new EthernetTestFragment();
    }

    @Override
    public int getTargetFocusId(int direction) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (getActivity() instanceof MainActivity && ((MainActivity) getActivity()).isFocusFeatureEnabled()) {
                if (direction == KeyEvent.KEYCODE_DPAD_UP) {
                    return R.id.text_ether_info;
                } else if (direction == KeyEvent.KEYCODE_DPAD_DOWN) {
                    return R.id.btn_ether_scan;
                }
            }
        }
        return 0;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                this.mLogger = activity.logUtil;
                this.etherTest = activity.getEtherTest();
            }else{
                mLogger.log("*** TIRAMISU not supported");
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_ethernet_test, container, false);
        mLogger.log("onCreateView called. Initializing Ethernet Test Fragment");

        tvEtherInfo = rootView.findViewById(R.id.text_ether_info);
        btnEtherScan = rootView.findViewById(R.id.btn_ether_scan);
        btnEtherTest = rootView.findViewById(R.id.btn_ether_test);

        btnEtherScan.setOnClickListener(v -> {
            mLogger.log("Scan button clicked. Opening system Ethernet settings...");
            Toast.makeText(getActivity(), "Opening Wi-Fi Settings...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        });
        return rootView;
    }
}