package com.innopia.factorytools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class EthernetTest extends Test {
	private final String TAG = "InnoFactory.EthernetTest";

	public static final String EXTRA_KEY_ETHERNET_SPEED = "ETHERNET_SPEED";

	private final String CONFIG_TAG_ETHERNET_PING = "ethernet_ping";

	private ConnectivityManager mConnectivityManager;
	private ConnectivityManager.NetworkCallback mNetworkCallback;

	private String pingAddress;



	public EthernetTest(Context context, Bundle config) {
		super(context);
		mConnectivityManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

		if(config != null) {
			pingAddress = config.getString(CONFIG_TAG_ETHERNET_PING);
		}

		mNetworkCallback = new ConnectivityManager.NetworkCallback() {
			@Override
			public void onAvailable(Network network) {
				super.onAvailable(network);
				NetworkCapabilities networkCapabilities = mConnectivityManager.getNetworkCapabilities(network);

				if(networkCapabilities != null) {
					if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {

						int ethernetSpeed = getEthernetSpeed();
						
						Bundle extra = new Bundle();
						extra.putInt(EXTRA_KEY_ETHERNET_SPEED, ethernetSpeed);
						if(ethernetSpeed >= 100) {
							mOnResultListener.onResult(EthernetTest.this, RESULT_SUCCEEDED, extra);
						} else {
							mOnResultListener.onResult(EthernetTest.this, RESULT_FAILED, extra);
						}
					}
				}
			}

			@Override
			public void onLost(Network network) {
				super.onLost(network);
				mOnResultListener.onResult(EthernetTest.this, RESULT_FAILED, null);
			}
		};

	}



	@Override
	public void start() {
		NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
		if(networkInfo == null || !networkInfo.isConnected()) {
			mOnResultListener.onResult(EthernetTest.this, RESULT_FAILED, null);
		}

		mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback);
	}



	@Override
	public void stop() {
		mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
	}


	private int getEthernetSpeed() {
		int ethernetSpeed = 0;

		try {
			Process process = Runtime.getRuntime().exec("cat sys/class/net/eth0/speed");

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = bufferedReader.readLine();
			bufferedReader.close();

			ethernetSpeed = Integer.parseInt(line);
		} catch (IOException e) {
			ethernetSpeed = 0;
		} catch (NumberFormatException e) { 
			ethernetSpeed = 0;
		}finally {
			return ethernetSpeed;
		}
				
	}
	
}

