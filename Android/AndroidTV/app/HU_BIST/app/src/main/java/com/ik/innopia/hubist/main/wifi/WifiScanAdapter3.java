package com.ik.innopia.hubist.main.wifi;

import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.ik.innopia.hubist.R;

import java.util.List;

public class WifiScanAdapter3 extends RecyclerView.Adapter<WifiScanAdapter3.ViewHolder> {

    private List<ScanResult> wifiList;

    public WifiScanAdapter3(List<ScanResult> wifiList) {
        this.wifiList = wifiList;
    }

    @Override
    public WifiScanAdapter3.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wifi_scan3, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(WifiScanAdapter3.ViewHolder holder, int position) {
        ScanResult scanResult = wifiList.get(position);
        holder.ssidText.setText(scanResult.SSID);
        holder.bssidText.setText(scanResult.BSSID);
        holder.levelText.setText(String.valueOf(scanResult.level));
    }

    @Override
    public int getItemCount() {
        return wifiList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView ssidText, bssidText, levelText;

        public ViewHolder(View itemView) {
            super(itemView);
            ssidText = itemView.findViewById(R.id.text_ssid);
            bssidText = itemView.findViewById(R.id.text_bssid);
            levelText = itemView.findViewById(R.id.text_level);
        }
    }
}
