package com.ik.innopia.hubist.main.wifi;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ik.innopia.hubist.R;

import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.List;

public class WifiScanDialog3 extends DialogFragment {

    private static final String ARG_WIFI_LIST = "wifi_list";

    private List<ScanResult> wifiList = new ArrayList<>();

    public static WifiScanDialog3 newInstance(List<ScanResult> wifiList) {
        WifiScanDialog3 dialog = new WifiScanDialog3();
        Bundle args = new Bundle();
        args.putSerializable(ARG_WIFI_LIST, new ArrayList<>(wifiList));
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_wifi_scan3, container, false);

        if (getArguments() != null) {
            wifiList = (List<ScanResult>) getArguments().getSerializable(ARG_WIFI_LIST);
        }

        RecyclerView recyclerView = rootView.findViewById(R.id.recycler_wifi_scan);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        WifiScanAdapter3 adapter = new WifiScanAdapter3(wifiList);
        recyclerView.setAdapter(adapter);

        return rootView;
    }
}
