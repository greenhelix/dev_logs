package com.innopia.factorytools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.InterfaceConfiguration;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.net.wifi.WifiNetworkSuggestion.Builder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

public class WifiTest extends Test {

	private static final String TAG = "WifiTest";

	public static final String EXTRA_KEY_WIFI_FREQUENCY 	= "WIFI_FREQUENCY";
	public static final String EXTRA_KEY_WIFI_ANTENNA_A	= "WIFI_ANTENNA_A";
	public static final String EXTRA_KEY_WIFI_ANTENNA_B	= "WIFI_ANTENNA_B";

	public static final int FREQUENCY_2G	= 0;
	public static final int FREQUENCY_5G 	= 1;
	public static final int FREQUENCY_ALL	= 2;

	private final int ANTENNA_A 	= 0;
	private final int ANTENNA_B	= 1;

	private final String CONFIG_TAG_WIFI_SSID_2G			= "wifi_ssid_2g";
	private final String CONFIG_TAG_WIFI_PASSWORD_2G 		= "wifi_password_2g";
	private final String CONFIG_TAG_WIFI_RSSI_MIN_2G		= "wifi_rssi_min_2g";
	private final String CONFIG_TAG_WIFI_RSSI_MAX_2G		= "wifi_rssi_max_2g";
	private final String CONFIG_TAG_WIFI_SSID_5G			= "wifi_ssid_5g";
	private final String CONFIG_TAG_WIFI_PASSWORD_5G 		= "wifi_password_5g";
	private final String CONFIG_TAG_WIFI_RSSI_MIN_5G		= "wifi_rssi_min_5g";
	private final String CONFIG_TAG_WIFI_RSSI_MAX_5G		= "wifi_rssi_max_5g";
	private final String CONFIG_TAG_WIFI_CONNECTION_TRY_COUNT	= "wifi_connection_try_count";
	private final String CONFIG_TAG_WIFI_CONNECTION_TRY_TIME	= "wifi_connection_try_time";
	private final String CONFIG_TAG_WIFI_RSSI_WAIT_TIME		= "wifi_rssi_wait_time";

	private ConnectivityManager mConnectivityManager;
	private ConnectivityManager.NetworkCallback mNetworkCallback;
	private WifiManager mWifiManager;
	
	private BroadcastReceiver mBroadcastReceiver;
	private IntentFilter mIntentFilter;

	private String mSsid2G;
	private String mPassword2G;
	private int mRssiMin2G;
	private int mRssiMax2G;

	private String mSsid5G;
	private String mPassword5G;
	private int mRssiMin5G;
	private int mRssiMax5G;

	private int mMaxConnectionTryCount;
	private int mTryTime;
	private int mWaitTime;

	private int mTestFrequency;
	private int mTestConnection;

	private Handler mConnectionHandler;
	
	private Timer mConnectionTryTimer;

	private int mRssi2G_A;
	private int mRssi2G_B;
	private int mRssi5G_A;
	private int mRssi5G_B;

	private int mConnectionTryCount;

	private ScanResult mScanResult2G;
	private ScanResult mScanResult5G;


	public WifiTest(Context context, Bundle config) {
		super(context);
		mConnectivityManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		mWifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);

		if(config != null) {
			mSsid2G = config.getString(CONFIG_TAG_WIFI_SSID_2G);
			mPassword2G = config.getString(CONFIG_TAG_WIFI_PASSWORD_2G);
			mRssiMin2G = Integer.parseInt(config.getString(CONFIG_TAG_WIFI_RSSI_MIN_2G));
			mRssiMax2G = Integer.parseInt(config.getString(CONFIG_TAG_WIFI_RSSI_MAX_2G));
			mSsid5G = config.getString(CONFIG_TAG_WIFI_SSID_5G);
			mPassword5G = config.getString(CONFIG_TAG_WIFI_PASSWORD_5G);
			mRssiMin5G = Integer.parseInt(config.getString(CONFIG_TAG_WIFI_RSSI_MIN_5G));
			mRssiMax5G = Integer.parseInt(config.getString(CONFIG_TAG_WIFI_RSSI_MAX_5G));
			mMaxConnectionTryCount = Integer.parseInt(config.getString(CONFIG_TAG_WIFI_CONNECTION_TRY_COUNT));
			mTryTime = Integer.parseInt(config.getString(CONFIG_TAG_WIFI_CONNECTION_TRY_TIME));
			mWaitTime = Integer.parseInt(config.getString(CONFIG_TAG_WIFI_RSSI_WAIT_TIME));

			Log.d(TAG, "SSID 2.4G:"+mSsid2G+" mRssiMin24G:"+mRssiMin2G+" mRssiMax24G:"+ mRssiMax2G);
			Log.d(TAG, "SSID 5G:"+mSsid5G+" mRssiMin5G:"+mRssiMin5G+" mRssiMax5G:"+ mRssiMax5G);
		}


		mConnectionHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				mTestConnection = msg.what;
				Log.d(TAG, "mTestConnection : " + mTestConnection);	
					switch(mTestConnection) {
						case FREQUENCY_2G:
							connectWifi(mSsid2G);
							break;
						case FREQUENCY_5G:
							connectWifi(mSsid5G);
							break;

					}
			}
		};

		
		
		mBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
		
				if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
					Log.d(TAG, "NETWORK_STATE_CHANGED_ACTION");

					WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
					if(wifiInfo != null) {
						Log.d(TAG, "Wifi SSID : "+ wifiInfo.getSSID()+" WifiInfo RSSI : " + wifiInfo.getRssi());
						String ssid = wifiInfo.getSSID();
						ssid = ssid.replaceAll("\"", "");
						Log.d(TAG, "ssid : " + ssid);
						//getAllRssi();

//						int[] rssi = {-1, -1};
						int[] rssi = {wifiInfo.getRssi(), wifiInfo.getRssi()};

						switch(mTestConnection) {
							case FREQUENCY_2G:
								if(mSsid2G != null && mSsid2G.equals(ssid)) {
									if(mRssi2G_A == 0 && mRssi2G_B == 0) {
//										rssi = getAllRssi();

										if((rssi[ANTENNA_A] != -1 && rssi[ANTENNA_B] != -1) && (rssi[ANTENNA_A] != 0 && rssi[ANTENNA_B] != 0) && (rssi[ANTENNA_A] != -127 && rssi[ANTENNA_B] != -127)) {

											rssi = getWiFiRssi();
											mRssi2G_A = rssi[ANTENNA_A];
											mRssi2G_B = rssi[ANTENNA_B];
											Log.d(TAG, "2.4G A:"+mRssi2G_A+" B:"+mRssi2G_B);
										
											mWifiManager.disconnect();				
											mWifiManager.removeNetwork(wifiInfo.getNetworkId());

											Bundle extra2G = new Bundle();
											extra2G.putInt(EXTRA_KEY_WIFI_FREQUENCY, FREQUENCY_2G);
											extra2G.putInt(EXTRA_KEY_WIFI_ANTENNA_A, mRssi2G_A);
											extra2G.putInt(EXTRA_KEY_WIFI_ANTENNA_B, mRssi2G_B);			

											if(mRssiMin2G < mRssi2G_A && mRssi2G_A < mRssiMax2G && mRssiMin2G < mRssi2G_B && mRssi2G_B < mRssiMax2G) {
												mOnResultListener.onResult(WifiTest.this, RESULT_SUCCEEDED, extra2G);
											} else {
												mOnResultListener.onResult(WifiTest.this, RESULT_FAILED, extra2G);
											}
									
											if(mConnectionTryTimer != null) {	
												mConnectionTryTimer.cancel();
												mConnectionTryTimer=null;
											}

											mWifiManager.setWifiEnabled(false);
											configureEthernet(true);
											mContext.unregisterReceiver(mBroadcastReceiver);
										}
									}
								}
								break;

							case FREQUENCY_5G:
								if(mSsid5G != null && mSsid5G.equals(ssid)) {
									if(mRssi5G_A == 0 && mRssi5G_B == 0) {
//										rssi = getAllRssi();
										if((rssi[ANTENNA_A] != -1 && rssi[ANTENNA_B] != -1) && (rssi[ANTENNA_A] != 0 && rssi[ANTENNA_B] != 0) && (rssi[ANTENNA_A] != -127 && rssi[ANTENNA_B] != -127)) {
											rssi = getWiFiRssi();
											mRssi5G_A = rssi[ANTENNA_A];
											mRssi5G_B = rssi[ANTENNA_B];
											Log.d(TAG, "5G A:"+mRssi5G_A+" B:"+mRssi5G_B);

											mWifiManager.disconnect();				
											mWifiManager.removeNetwork(wifiInfo.getNetworkId());	
 
											Bundle extra5G = new Bundle();
											extra5G.putInt(EXTRA_KEY_WIFI_FREQUENCY, FREQUENCY_5G);
											extra5G.putInt(EXTRA_KEY_WIFI_ANTENNA_A, mRssi5G_A);
											extra5G.putInt(EXTRA_KEY_WIFI_ANTENNA_B, mRssi5G_B);

											if(mRssiMin5G < mRssi5G_A && mRssi5G_A < mRssiMax5G && mRssiMin5G < mRssi5G_B && mRssi5G_B < mRssiMax5G) {
												Log.d(TAG, "onResult::RESULT_SUCCEEDED 5G A:"+mRssi5G_A+" B:"+mRssi5G_B);
												mOnResultListener.onResult(WifiTest.this, RESULT_SUCCEEDED, extra5G);
											} else {
												Log.d(TAG, "onResult::RESULT_FAILED 5G A:"+mRssi5G_A+" B:"+mRssi5G_B);
												mOnResultListener.onResult(WifiTest.this, RESULT_FAILED, extra5G);
											} 

											if(mConnectionTryTimer != null) {	
												mConnectionTryTimer.cancel();
												mConnectionTryTimer=null;
											}

											if(mTestFrequency == FREQUENCY_ALL) {	
												mConnectionHandler.sendEmptyMessage(FREQUENCY_2G);
											} else {
												mWifiManager.setWifiEnabled(false);
												configureEthernet(true);
												mContext.unregisterReceiver(mBroadcastReceiver);
											}
										}
									}
								}
								break;
						}
					}
				} 
			}
		};

		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		

	}


	@Override
	public void start() {
		Log.d(TAG,"start");
		configureEthernet(false);
		mWifiManager.setWifiEnabled(true);

		List<WifiConfiguration> wifiList = mWifiManager.getConfiguredNetworks();
		for(WifiConfiguration wifiConfiguration : wifiList) {
			Log.d(TAG, "removeNetwork");
			mWifiManager.removeNetwork(wifiConfiguration.networkId);
			mWifiManager.saveConfiguration();
		}
			
				
		mScanResult2G = null;
		mScanResult5G = null;
		mRssi2G_A = 0;
		mRssi2G_B = 0;
		mRssi5G_A = 0;
		mRssi5G_B = 0;

		mConnectionTryCount = 0;
		
		mContext.registerReceiver(mBroadcastReceiver, mIntentFilter);

		if(mTestFrequency == FREQUENCY_2G) {
			mConnectionHandler.sendEmptyMessage(FREQUENCY_2G);
		} else {
			mConnectionHandler.sendEmptyMessage(FREQUENCY_5G);
		}
	}


	@Override
	public void stop() {
		mWifiManager.setWifiEnabled(false);
		configureEthernet(true);

		if(mConnectionTryTimer != null) {
			mConnectionTryTimer.cancel();
			mConnectionTryTimer = null;
		}

		if(mTestFrequency == FREQUENCY_2G) {
			if(mRssi2G_A == 0 || mRssi2G_B == 0) {
				mContext.unregisterReceiver(mBroadcastReceiver);
			}
		} else if ( mTestFrequency == FREQUENCY_5G) {
			if(mRssi5G_A == 0 || mRssi5G_B == 0) {
				mContext.unregisterReceiver(mBroadcastReceiver);
			}
		} else {
			if(mRssi2G_A == 0 || mRssi2G_B == 0 || mRssi5G_A == 0 || mRssi5G_B == 0) {
				mContext.unregisterReceiver(mBroadcastReceiver);
			}
		}
	}


	public void setTestFrequency(int frequency) {
		mTestFrequency = frequency;
	}


	private void connectWifi(String ssid) {
		if(ssid != null) {
			Log.d(TAG, "connectWifi : " + ssid);		
/*
			WifiConfiguration wifiConfiguration = new WifiConfiguration();

			wifiConfiguration.SSID ="\""+ssid+"\"";

			if(mTestConnection == FREQUENCY_2G) {
				if(TextUtils.isEmpty(mPassword2G)) {
					wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
				} else {
					wifiConfiguration.preSharedKey ="\""+mPassword2G+"\"";
				}
			} else {
				if(TextUtils.isEmpty(mPassword5G)) {
					wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
				} else {
					wifiConfiguration.preSharedKey ="\""+mPassword5G+"\"";
				}
			}
*/
			
			List<WifiNetworkSuggestion> mWifiList = new ArrayList<WifiNetworkSuggestion>();
			List<WifiNetworkSuggestion> mWifi2gList = new ArrayList<WifiNetworkSuggestion>();
			List<WifiNetworkSuggestion> mWifi5gList = new ArrayList<WifiNetworkSuggestion>();
			WifiNetworkSuggestion wifiNetworkSuggestion = null;

			//          int netId = -1;

			if(mTestConnection == FREQUENCY_2G) {
			  if(mWifiManager.getNetworkSuggestions() != null) {
				mWifiManager.removeNetworkSuggestions(mWifiManager.getNetworkSuggestions());
			  }
			  if(TextUtils.isEmpty(mPassword2G)) {
				wifiNetworkSuggestion = new Builder().setSsid(ssid).build();
			  } else {
				wifiNetworkSuggestion = new Builder().setSsid(ssid).setWpa2Passphrase(mPassword2G).build();
			  }
			  mWifi2gList.add(wifiNetworkSuggestion);
			  mWifiList = mWifi2gList;
			}

			if(mTestConnection == FREQUENCY_5G) {
			  if(mWifiManager.getNetworkSuggestions() != null) {
				mWifiManager.removeNetworkSuggestions(mWifiManager.getNetworkSuggestions());
			  }
			  if(TextUtils.isEmpty(mPassword5G)) {
				wifiNetworkSuggestion = new Builder().setSsid(ssid).build();
			  } else {
				wifiNetworkSuggestion = new Builder().setSsid(ssid).setWpa2Passphrase(mPassword5G).build();
			  }
			  mWifi5gList.add(wifiNetworkSuggestion);
			  mWifiList = mWifi5gList;
			}

			int netId = mWifiManager.addNetworkSuggestions(mWifiList);

			Log.d(TAG, "netId : "+netId);
			if(netId != -1) {
				mConnectionTryCount = 0;
				if(mConnectionTryTimer != null) {
					mConnectionTryTimer.cancel();
					mConnectionTryTimer = null;
				}

				mConnectionTryTimer = new Timer();

				TimerTask timerTask = new TimerTask() {
					@Override
					public void run() {
						if(mConnectionTryCount < mMaxConnectionTryCount) {
							Log.d(TAG, "try connection");
							mWifiManager.disconnect();
							mWifiManager.enableNetwork(netId, true);
							mWifiManager.reconnect();
							mConnectionTryCount++;
						} else {
							Bundle extra = new Bundle();
							extra.putInt(EXTRA_KEY_WIFI_FREQUENCY, mTestConnection);
							mOnResultListener.onResult(WifiTest.this, RESULT_FAILED, extra);

							if(mTestFrequency == FREQUENCY_ALL) {
								if(mTestConnection == FREQUENCY_5G) {
									mConnectionTryTimer.cancel();
									mConnectionTryTimer = null;
									mConnectionHandler.sendEmptyMessage(FREQUENCY_2G);
								} else {
									stop();
								}
							} else {
								stop();
							}
						}
					}
				}; 				

				mConnectionTryTimer.schedule(timerTask, 0, mTryTime*1000);	
			
			}
		} else {
			Bundle extra = new Bundle();
			extra.putInt(EXTRA_KEY_WIFI_FREQUENCY, mTestConnection);
			mOnResultListener.onResult(WifiTest.this, RESULT_FAILED, extra);

			if(mTestFrequency == FREQUENCY_ALL) {
				if(mTestConnection == FREQUENCY_5G) {
					mConnectionHandler.sendEmptyMessage(FREQUENCY_2G);
				} else {
					stop();
				}
			} else {
				stop();
			}
		}
				
	}


	private void configureEthernet(boolean isUp) {
		Log.d(TAG, "configureEthernet : " + isUp);
		IBinder binder = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
		INetworkManagementService service = INetworkManagementService.Stub.asInterface(binder);
		String iface = "eth0";

		try {
			InterfaceConfiguration interfaceConfiguration = service.getInterfaceConfig(iface);
			if(interfaceConfiguration != null) {
				if(isUp) {
					interfaceConfiguration.setInterfaceUp();
				} else {
					interfaceConfiguration.setInterfaceDown();
				}
				interfaceConfiguration.clearFlag("running");
				service.setInterfaceConfig(iface, interfaceConfiguration);
			} else {
				Log.d(TAG, "interfaceConfiguration is null");
			}
		} catch (Exception e) {
			Log.d(TAG,"Exception : " + e);
		}
	}


	private int getRssi() {

		int rssi = 0;
		String prefix = "rssi:";
		try {
			Process process = Runtime.getRuntime().exec("cat /proc/net/rtl88x2cs/wlan0/rx_signal");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = bufferedReader.readLine();
			
			while(line != null) {
				Log.d(TAG, "line : "+line);
				if(line.startsWith(prefix)) {
					rssi = Integer.parseInt(line.substring(prefix.length()));
					break;
				}
				line = bufferedReader.readLine();
			}

			bufferedReader.close();

		} catch (IOException e) {
			rssi = 0;
		}

		return rssi;
	}

	private int[] getWiFiRssi() {
		final String WIFI_RSSI_PROP     = "inno.factory.get_wifi_rssi";
		final String WIFI_RSSI_DATA		= "/data/wifi_rssi";
		final int    WIFI_RSSI_RETRY    = 10;
		final int    WIFI_RSSI_SLEEP_MS = 500;

		int[] rssi = {0, 0};
		String status, cmd, line = null;
		int count = 0;

		Log.d(TAG, "getWiFiRssi() IN");

		// set WiFi RSSI property
		SystemProperties.set(WIFI_RSSI_PROP, "start");

		while ( true ) {
			status = SystemProperties.get(WIFI_RSSI_PROP,"x");
			if ( !status.isEmpty() && status.equals("end") ) {
				break;
			}
			if ( count++ > WIFI_RSSI_RETRY ) {
				Log.e(TAG, "getWiFiRssi() Timeout! Failed to get WiFi RSSI.");
				return null;
			}
			Log.d(TAG, "getWiFiRssi() Waiting for getting RSSI of WiFi." +
					" (status=" + status + ", "+ count + "/" + WIFI_RSSI_RETRY + ")");
			try { Thread.sleep(WIFI_RSSI_SLEEP_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		}

		// read rssi data
		cmd = "cat " + WIFI_RSSI_DATA;
		try {
			Process ps = Runtime.getRuntime().exec(cmd);
			BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			while ( (line = br.readLine()) != null ) {
				Log.d(TAG, "getWiFiRssi() line : " + line);
				if ( line.isEmpty() ) continue;
				if ( line.contains("rssi[0]") && line.contains("rssi[1]") ) {
					break;
				}
			}
			br.close();
			ps.destroy();
		} catch ( Exception e ) {
			Log.e(TAG, "getWiFiRssi() Failed to run command: " + cmd + ", err: " + e.toString());
			e.printStackTrace();
			return null;
		}

		// parsing rssi data:
		//
		// rssi[0] -44  rssi[1] -43
		//
		try {
			if ( line == null || line.isEmpty() ) {
				Log.e(TAG, "getWiFiRssi() Failed to read WiFi RSSI...");
				return null;
			}
			else if ( line.contains("rssi[0]") && line.contains("rssi[1]") ) {
				String rssi0 = "rssi[0]", rssi1 = "rssi[1]";

				rssi0 = line.substring(line.indexOf(rssi0) + rssi0.length(), line.indexOf(rssi1)).trim();
				Log.d(TAG, "getWiFiRssi() rssi[0]=[" + rssi0 +"]");

				rssi1 = line.substring(line.indexOf(rssi1) + rssi1.length()).trim();
				Log.d(TAG, "getWiFiRssi() rssi[1]=[" + rssi1 +"]");

				rssi[ANTENNA_A] = Integer.parseInt(rssi0);
				rssi[ANTENNA_B] = Integer.parseInt(rssi1);
			}
			else {
				Log.e(TAG, "getWiFiRssi() Wrong rssi data: " + line);
				return null;
			}
		} catch ( Exception e ) {
			Log.e(TAG, "getWiFiRssi() Failed to parse data: " + line + ", err: " + e.toString());
			e.printStackTrace();
			return null;
		}

		return rssi;
	}


	private int[] getAllRssi() {

		int[] rssi = {-1, -1};
				
	
		try {

			for(int readTryCount=0; readTryCount<5; readTryCount++) {
				Log.d(TAG, "readTryCount : " +readTryCount);
				Process process = Runtime.getRuntime().exec("wl phy_rssi_ant");
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;

				while((line = bufferedReader.readLine()) != null) {
					Log.d(TAG, "wl phy_rssi_ant line : " + line);

					if(line.contains("rssi[0]") && line.contains("rssi[1]")) {
						String[] splitedString = line.split("rssi");

						for(int i=0; i<splitedString.length; i++) {
							Log.d(TAG,"splitstring["+i+"] : " +  splitedString[i]);
						}
						
/*
						for(int i=0; i<splitedString.length; i++) {
							if(splitedString[i].equals("rssi_a")) {
								String a = splitedString[i+2].substring(0,splitedString[i+2].indexOf("("));
								Log.d(TAG, "rssi_a :" + a);
								rssi[ANTENNA_A] = Integer.parseInt(a);
							}

							if(splitedString[i].equals("rssi_b")) {
								String b = splitedString[i+2].substring(0,splitedString[i+2].indexOf("("));
								Log.d(TAG, "rssi_b :" + b);
								rssi[ANTENNA_B] = Integer.parseInt(b);
							}
						}
						*/

					}	
				}
				Log.d(TAG, "readLine -> [" + line + "]");

				bufferedReader.close();
				process.waitFor();

				if(rssi[ANTENNA_A] != -1 && rssi[ANTENNA_B] != -1) {
					break;
				} else {
					Thread.sleep(200);
				}		
			}
		} catch (IOException e) {
			Log.d(TAG, "IOException");
			rssi[ANTENNA_A] = -1;
			rssi[ANTENNA_B] = -1;

		} catch (InterruptedException e) {
			Log.d(TAG, "InterruptedException");
			rssi[ANTENNA_A] = -1;
			rssi[ANTENNA_B] = -1;
			
		}

		return rssi;
	}


}
