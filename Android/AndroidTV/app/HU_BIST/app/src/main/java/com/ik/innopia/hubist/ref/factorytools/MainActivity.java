

package com.innopia.factorytools;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.EthernetManager.InterfaceStateListener;
import android.net.EthernetNetworkUpdateRequest;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.InetAddresses;
import android.net.StaticIpConfiguration;
import android.net.StaticIpConfiguration.Builder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.Configuration;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import com.innopia.factorytools.tcp.DefProtocol;
import com.innopia.factorytools.tcp.GenData;
import com.innopia.factorytools.tcp.TCPServer;
import com.innopia.factorytools.tcp.TCPServer.SocketListener;
import com.innopia.factorytools.tcp.LogTCPServer;


public class MainActivity extends Activity {


	private final String TAG = "InnoFactory";
	private final boolean DEBUG = true;

	public void LOG(String msg) {
		if(DEBUG) {
			Log.d(TAG, msg);
		}
	}

	private final String STORAGE_PATH 					= "/mnt/media_rw";
	private final String STORAGE_PATH2                   = "/storage";
	private final String CONFIGURATION_FILE_NAME 				= "factorytools_config.xml";


	private final String INIT_IMAGE_FILE_PATH 				= "/data/InitImage.img";
	
	private final String HDCP_KEY_FILE_PATH					= "/vendor/factory/HDCP_Key.store";
	private final String HDCP20_LC_FILE_PATH				= "/vendor/factory/HDCP20_LC.store";
	private final String KB_FILE_PATH					= "/vendor/factory/kb.bin";
	private final String MAC_ADDR_FILE_PATH					= "/vendor/factory/MAC_ADDR";
	private final String UNIQUE_ID_FILE_PATH				= "/vendor/factory/Unique_ID.bin";
	private final String KM_CERT_FILE_PATH					= "/vendor/factory/KM.cert";
	private final String KM_KEY_FILE_PATH					= "/vendor/factory/KM_key.store";
	private final String PLAYREADY30_KEY_FILE_PATH				= "/vendor/factory/PLAYREADY30_Key.store";
	private final String BGROUPCERT30_FILE_PATH				= "/vendor/factory/bgroupcert30.dat";

	private final String OTP_LOCK_SUCCESS_FILE_PATH				= "/vendor/factory/keyinjection.done";

	private final String WIFI_MAC_ADDR_FILE_PATH            = "/data/WIFI_MAC_ADDR";
	private final String BT_MAC_ADDR_FILE_PATH            = "/data/BT_MAC_ADDR";
	private final String ETH_MAC_ADDR_FILE_PATH				= "/data/MAC_ADDR";
	private final String PCB_SERIALNO_FILE_PATH                 = "/data/pcbserialno";
	private final String DT_SERIALNO_FILE_PATH              = "/data/serialno";
	private final String RKEK_FILE_PATH                     = "/data/RKEK_ID";


	private final String OTP_LOCK_LOG_FILE_PATH				= "/data/ret_otp";

	private final String CONFIG_TAG_FACTORY_STEP            = "factory_step";
	private final String CONFIG_TAG_ETHERNET_IP				= "ethernet_ip_address";
	private final String CONFIG_TAG_ETHERNET_PREFIX_LENGTH 			= "ethernet_network_prefix_length";
	private final String CONFIG_TAG_AUDIO_VOLUME    			= "audio_volume";
	private final String CONFIG_TAG_KEY_PAIRING_TIMEOUT			= "key_pairing_timeout";
	private final String CONFIG_TAG_OTP_LOCK_TIMEOUT			= "otp_lock_timeout";
	private final String CONFIG_TAG_STREAMING_URL = "streaming_url";

	private final String CONFIG_TAG_LOG_ENABLE     =  "log_enable";
	private final String CONFIG_TAG_TEST_MODE      =  "test_mode";

	private final String CMD_HDCP_DISABLE					= "test_disp hdcp disable";

	private final String CMD_RESOLUTION_1080P				= "test_disp setres 26 2 2 2";
	private final String CMD_RESOLUTION_720P 				= "test_disp setres 15 2 2 2"; 

	private final String CMD_RECEIVED_ETHERNET_MAC_ADDRESS			= "cat /vendor/factory/MAC_ADDR";
	private final String CMD_RECEIVED_WIFI_MAC_ADDRESS          = "cat /vendor/factory/WIFI_MAC_ADDR";
	private final String CMD_RECEIVED_BT_MAC_ADDRESS            = "cat /vendor/factory/BT_MAC_ADDR";
	private final String CMD_RECEIVED_PCB_SERIAL                = "cat /vendor/factory/pcbserialno";
	private final String CMD_RECEIVED_DT_SERIAL                 = "cat /vendor/factory/serialno";
//	private final String CMD_WRITTEN_ETHERNET_MAC_ADDRESS			= "cat /sys/class/net/eth0/address";
	private final String CMD_RECEIVED_RKEK_ID					= "cat /vendor/factory/RKEK_ID";
	
	private final String CMD_WRITTEN_WIFI_MAC_ADDRESS			= "cat /sys/class/net/wlan0/address";

	private final String CMD_SYNC						= "sync";

	private final String PROP_KEY_LED_GREEN					= "vendor.sys.led.green";
	private final String PROP_KEY_LED_RED					= "vendor.sys.led.red";


	private final int TEST_STREAMING 					= 0;
	private final int TEST_BUTTON_1                 = 1;
	private final int TEST_BUTTON_2                 = 2;
	private final int TEST_USB1						= 3;
//	private final int TEST_USB2						= 3;
	private final int TEST_WIFI_2G						= 4;
	private final int TEST_WIFI_5G						= 5;
	private final int TEST_BLUETOOTH					= 6;
	private final int TEST_HDMI_EDID					= 7;
	private final int TEST_HDMI_CEC						= 8;
	private final int TEST_ETHERNET						= 9;
	private final int TEST_CPU						= 10;
	private final int TEST_IR_LED                   = 11;
	private final int TEST_AUDIO					= 12;
	private final int TEST_MIC_OPENED				= 13;
	private final int TEST_MIC_MUTE					= 14;
	private final int TEST_MIC_CLOSED				= 15;
	private final int TEST_MAX_INDEX				= 16;

	private final String KEY_PAIRING_RESULT_SUCCESS				= "Success";
	private final String KEY_PAIRING_RESULT_FAILURE				= "Failure";
	private final String KEY_PAIRING_RESULT_TIMEOUT				= "Timeout";
	private final String KEY_PAIRING_RESULT_MISMATCH			= "Mismatch";

	private final String STEP_1ST = "1st";
	private final String STEP_2ND = "2nd";

	private int[] mTestResult;
	private Bundle[] mTestResultExtra;
	private int mAudioTestResult = -1;

	private boolean isFailedOTP = false;

	private SharedPreferences mSharedPreferences;

	private final int BUTTON_TEST_PAIRING					= 0;
	private final int BUTTON_TEST_MUTE 					= 1;

	private int[] mButtonTestResult;	
	private int mIRCount = 0;

	private boolean mIsAllSucceeded 					= false;

	private boolean mIsTesting 						= false;

	private boolean mIsStartAutoTest				= false;

	private boolean mIsStarted = false;

	private boolean mIsTestmode = false;

//	private boolean mIsOtpLock						= false;

	private SurfaceView mSurfaceView;

	private TestView mTestViewStreaming;
	private TestView mTestViewButton1;
	private TestView mTestViewButton2;
	private TestView mTestViewIR;
	private TestView mTestViewAudio;
	private TestView mTestViewKeyPairing;
	private TestView mTestViewOtpLock;
	private TestView mTestViewDbClient;

	private TestView mTestViewUsb1;
//	private TestView mTestViewUsb2;
	private TestView mTestViewWifi;
	private TestView mTestViewBluetooth;
	private TestView mTestViewHdmiEdid;
	private TestView mTestViewHdmiCec;
	private TestView mTestViewEthernet;
	private TestView mTestViewCpu;
	private TestView mTestViewOpenMic; // MIC Open 2
	private TestView mTestViewMuteMic; // MIC Open 1
	private TestView mTestViewCloseMic;

	private VoiceMicTest mVoiceOpenMicTest;
	private VoiceMicTest mVoiceMuteMicTest;
	private VoiceMicTest mVoiceCloseMicTest;

	private TestView mTestViewButtonBluetoothPair;

	private TestView mTestViewWifi2G;
	private TestView mTestViewWifi5G;

	private TextView mTextViewModelName;
	private TextView mTextViewSwVersionService;
	private TextView mTextViewHwVersion;
	private TextView mTextViewFactoryVersion;
	private TextView mTextViewBluetoothMac;
	private TextView mTextViewSetIp;
	private TextView mTextViewSerialNumber;
	private TextView mTextViewRKEK;
	private TextView mTextViewWifiMac;
	private TextView mTextViewPcbSerial;
	private TextView mTextViewEthernetMac;

	private TextView mTextViewTestResult;
	private TextView mTextViewTestLog;

	private Bundle mConfigValues;

	private StreamingTest mStreamingTest;

	private BluetoothTest mBluetoothTest;
	private EthernetTest mEthernetTest;
	private WifiTest mWifiTest;
	private CpuTest mCpuTest;
	private UsbTest mUsbTest;
	private HdmiTest mHdmiTest;
	private Provision mProvision;

	private EthernetManager mEthernetManager;
	private IpConfiguration mIpConfiguration;
	
	private AudioManager mAudioManager;

	private TCPServer mTcpServer;
	private LogTCPServer mLogTcpServer;
	
	private String mReceivedPcbSerialNumber;

	private String mReceivedPcbSerial_1st;
	private String mReceivedPcbSerial_2nd;
	private String mReceivedDTSerialNumber;
	private String mReceivedMacAddress;
	private String mReceivedWifiMacAddress;
	private String mReceivedBTMacAddress;
	private String mReceivedRKEKID;

	private int mAudioTestResultFreq_L;
	private int mAudioTestResultFreq_R;
	private float mAudioTestResultVPP_L;
	private float mAudioTestResultVPP_R;

	private final int AudioTestFreq = 1000;


	private int mKeyPairingTryCount;

	private Timer mKeyPairingTimer;

	private Timer getConfigTimer;

	private boolean mIsTimeout;

	private boolean is_old_hw_version = false;


	private int mProvisionResultCsr;
	private int mProvisionResultApp;
	private int mProvisionResultEmmcOtp;
	private int mProvisionResultEmmc;
	private int mProvisionResultOtp;
	
	private boolean flagOTPFail = false; 

	private Handler mHandler;
	private AsyncLogViewTask mLogTask = null;

	private final int HDMI_CEC_PASS_BY_HIDDEN_KEY = 5;
	private int mHdmiCecHiddenKeyPressed          = 0;

	class MicSwitchObserver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if ( "com.innopia.leds.mic_state_changed".equals(intent.getAction()) ){
				if ( getFactoryStep(mConfigValues).equals(STEP_2ND) && mTestViewCloseMic.getStatus() != TestView.STATUS_SUCCEEDED ) 
					return ;

				boolean isMuteMic = isMuteMic();
				//if ( !isMuteMic && mTestViewMuteMic.getStatus() == TestView.STATUS_UNKNOWN ) 
				//	return ;

				if ( mTestViewButton2.getStatus() == TestView.STATUS_READY ) {
					mButtonTestResult[BUTTON_TEST_MUTE] = Test.RESULT_SUCCEEDED;
					mTestResult[TEST_BUTTON_2] = Test.RESULT_SUCCEEDED;
					checkButtonTestResult();
					Log.d(TAG,"KeyEvent.KEYCODE_MUTE::mTestViewButton2.setStatus(TestView.STATUS_SUCCEEDED, null);");
					Log.d(TAG,"mTestResult[TEST_STREAMING] : " + mTestResult[TEST_STREAMING] + "(SUCCEEDED = " + Test.RESULT_SUCCEEDED + ")");
				} else if ( mTestViewButton2.getStatus() == TestView.STATUS_UNKNOWN ) {
					mTestViewButton2.setStatus(TestView.STATUS_READY, null);
					Log.d(TAG,"mTestResult[TEST_STREAMING] : " + mTestResult[TEST_STREAMING] + "(SUCCEEDED = " + Test.RESULT_SUCCEEDED + ")");
				}

				if ( getFactoryStep(mConfigValues).equals(STEP_1ST) ) {
					if ( isMuteMic ) {
						mStreamingTest.pause();
						mTestViewMuteMic.setStatus(TestView.STATUS_TESTING, null);
						mVoiceMuteMicTest.start();
					}
					return ;
				}

				if ( !isMicReady() || mTestResult[TEST_STREAMING] != Test.RESULT_SUCCEEDED || !getFactoryStep(mConfigValues).equals(STEP_2ND) ) {
					Log.d(TAG, "mic test is not ready");
					return;
				}

				if ( !isMuteMic ) {
					mStreamingTest.pause();
					mTestViewOpenMic.setStatus(TestView.STATUS_TESTING, null);
					mTestViewMuteMic.setStatus(TestView.STATUS_UNKNOWN, null);
					mVoiceOpenMicTest.start();
				} else if ( mTestViewOpenMic.getStatus() == TestView.STATUS_SUCCEEDED ) { 
					mStreamingTest.pause();
					mTestViewMuteMic.setStatus(TestView.STATUS_TESTING, null);
					//mTestViewCloseMic.setStatus(TestView.STATUS_UNKNOWN, null);
					mVoiceMuteMicTest.start();
				}

			}
			
		}
	}

	private int readBootCount() {
		SharedPreferences prefBootCount = getSharedPreferences("boot_count", MODE_PRIVATE);
		return prefBootCount.getInt("count", 0);
	}

	private void writeBootCount(int count) {
		SharedPreferences prefBootCount = getSharedPreferences("boot_count", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefBootCount.edit();
		editor.putInt("count", count);
		editor.apply();
	}

	private void checkBootCount() {
		int count = readBootCount();
		count = count + 1;
		if ( mTextViewTestResult != null )
			mTextViewTestResult.setText("boot_count : " + count);

		writeBootCount(count);
	}

	private void resetBootCount() { 
		writeBootCount(0);
		if ( mTextViewTestResult != null )
			mTextViewTestResult.setText("");
	}

	private boolean isMicReady() {
		if ( getFactoryStep(mConfigValues).equals(STEP_1ST) ) 
			return false;

		if ( mTestViewMuteMic.getStatus() == TestView.STATUS_TESTING || 
				mTestViewOpenMic.getStatus() == TestView.STATUS_TESTING || 
				mTestViewCloseMic.getStatus() == TestView.STATUS_TESTING )
			return false;

		return true;
	}


	private Test.OnResultListener mOnResultListener = new Test.OnResultListener() {
		@Override
		public void onResult(Test test, int resultCode, Bundle extra) {
			if(test.getClass() == CpuTest.class) {
				if(resultCode != mTestViewCpu.getStatus())
					LOG("onResult (test: " + test.getClass().getSimpleName() + ", resultCode: " + resultCode +")");
			} else {
				LOG("onResult (test: " + test.getClass().getSimpleName() + ", resultCode: " + resultCode +")");
			}

			int status;

			if(resultCode == Test.RESULT_SUCCEEDED) {
				status = TestView.STATUS_SUCCEEDED;
			} else {
				status = TestView.STATUS_FAILED;
			}

			if(test.getClass() == StreamingTest.class) {
				mIsTesting = false;
				mTestResult[TEST_STREAMING] = resultCode;
				mTestViewStreaming.setStatus(status, null);
				if((resultCode == Test.RESULT_SUCCEEDED) && !(mAudioTestResult == 1)) {
					/*
					if ( mTcpServer == null ) {
						Log.d(TAG, "Start TCP Server...");
						mTcpServer = new TCPServer(MainActivity.this);
						mTcpServer.setSocketListener(mSocketListener);
						mTcpServer.connect();
					}
					*/
				}
			} else if(test.getClass() == BluetoothTest.class) {
				mTestResult[TEST_BLUETOOTH] = resultCode;
				mTestResultExtra[TEST_BLUETOOTH] = extra;
				
				if (extra != null) {
					String bluetoothName = extra.getString(BluetoothTest.EXTRA_KEY_BLUETOOTH_NAME);
					short rssi = extra.getShort(BluetoothTest.EXTRA_KEY_BLUETOOTH_RSSI);
				
					mTextViewTestResult.setText("BT : " + bluetoothName + " (" + rssi + ")" );
					mTestViewBluetooth.setStatus(status, Short.toString(rssi));
				} else {
					mTextViewTestResult.setText("");
					mTestViewBluetooth.setStatus(status, null);
					if(status == Test.RESULT_FAILED) { 
						if((mTestViewWifi5G.getStatus() != TestView.STATUS_TESTING) && 
							(mTestViewWifi2G.getStatus() != TestView.STATUS_TESTING))
								setLedColor("red_blink"); 
					}
				}

			} else if(test.getClass() == EthernetTest.class) {
				mTestResult[TEST_ETHERNET] = resultCode;
				
				int ethernetSpeed = 0;
				if(extra != null) {
					ethernetSpeed = extra.getInt(EthernetTest.EXTRA_KEY_ETHERNET_SPEED);
				}

				mTestViewEthernet.setStatus(status, Integer.toString(ethernetSpeed));
				mTextViewSetIp.setText("Set IP : "+getIpAddress());

				if(getIpAddress() == null) {
					setIpAddress(mConfigValues);
				}

				if(resultCode == Test.RESULT_SUCCEEDED) {
					if(mStreamingTest == null) {
						mStreamingTest = new StreamingTest(MainActivity.this, mSurfaceView.getHolder(), mConfigValues);				
						mStreamingTest.setOnResultListener(mOnResultListener);
					}
					mStreamingTest.start(mConfigValues);
				} else {
					mTestResult[TEST_STREAMING] = resultCode;
					mTestViewStreaming.setStatus(status, null);
				}

			} else if(test.getClass() == WifiTest.class) {
				LOG("WifiTest::onResult");
				if(extra != null) {
					mIsTesting = false;
					int frequency = extra.getInt(WifiTest.EXTRA_KEY_WIFI_FREQUENCY);

					int rssiA = extra.getInt(WifiTest.EXTRA_KEY_WIFI_ANTENNA_A);
					int rssiB = extra.getInt(WifiTest.EXTRA_KEY_WIFI_ANTENNA_B);

					switch(frequency){
						case WifiTest.FREQUENCY_2G:
							mTestResult[TEST_WIFI_2G] = resultCode;
							mTestResultExtra[TEST_WIFI_2G] = extra;
							LOG("WifiTest::onResult::mTestViewWifi2G.setStatus("+status+","+rssiA+","+rssiB+")");
							new Thread(new Runnable() {
								@Override
								public void run() {
									mTestViewWifi2G.setStatus(status, rssiA + " / " + rssiB,false);
								}
							}).start();
							break;

						case WifiTest.FREQUENCY_5G:
							mTestResult[TEST_WIFI_5G] = resultCode;
							mTestResultExtra[TEST_WIFI_5G] = extra;
							LOG("WifiTest::onResult::mTestViewWifi5G.setStatus("+status+","+rssiA+","+rssiB+")");
							new Thread(new Runnable() {
								@Override
								public void run() {
									mTestViewWifi5G.setStatus(status, rssiA + " / " + rssiB,false);
									if(!(mTestResult[TEST_WIFI_2G] == Test.RESULT_SUCCEEDED))
										mTestViewWifi2G.setStatus(TestView.STATUS_TESTING);
										
								}
							}).start();
							break;
					}
					checkWifiTestResult();
				} else {
					LOG("WifiTest::onResult: extra == null");
				}

			} else if(test.getClass() == CpuTest.class) {
				mTestResult[TEST_CPU] = resultCode;
				mTestResultExtra[TEST_CPU] = extra;
				
				float temperature = 0;
				if(extra != null) {
					temperature = extra.getFloat(CpuTest.EXTRA_KEY_CPU_TEMPERATURE);
				}
				mTestViewCpu.setStatus(status, temperature+"℃");

			} else if(test.getClass() == UsbTest.class) {
				String usbName = extra.getString(UsbTest.EXTRA_KEY_USB_NAME);

				if(usbName.equals(UsbTest.USB_1) || usbName.equals(UsbTest.USB_2) || usbName.equals(UsbTest.USB_3) ) {
					mTestResult[TEST_USB1] = resultCode;
					mTestViewUsb1.setStatus(status, null);
					Log.d(TAG,"UsbTest.USB_1 resultCode = " + resultCode);
//					if(status == TestView.STATUS_SUCCEEDED) {
					if(resultCode == Test.RESULT_SUCCEEDED) {					                
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Handler handler = new Handler();
								handler.postDelayed(new Runnable() {
									@Override
									public void run() {
										if(mConfigValues == null) {
											mTestResult[TEST_USB1] = Test.RESULT_FAILED;
											mConfigValues = getConfigValues(1);
											if(mConfigValues != null) {
												mTestResult[TEST_USB1] = Test.RESULT_SUCCEEDED;
												mIsStarted = true;
												setIpAddress(mConfigValues);
												setFactoryStep(mConfigValues);
												setInformation();
												setTest();
												startAutoTest();
												checkTestResult();
												resetBootCount();
											} else {
												while (mConfigValues == null) {
													mConfigValues = getConfigValues(2);
													try{
														Thread.sleep(1000);
													}catch(InterruptedException e) {
														e.printStackTrace();
													}
												}
												if(mConfigValues != null) {
													mTestResult[TEST_USB1] = Test.RESULT_SUCCEEDED;
													mIsStarted = true;
													setIpAddress(mConfigValues);
													setFactoryStep(mConfigValues);
													setInformation();
													setTest();
													startAutoTest();
													checkTestResult();
												}
											}
										}
									}
								}, 3000);
							}
						});
						if(mStreamingTest == null) {
							mStreamingTest = new StreamingTest(MainActivity.this, mSurfaceView.getHolder(), mConfigValues);
							mStreamingTest.setOnResultListener(mOnResultListener);
						}
					} else {  setLedColor("red_blink"); }
				} else {
				/* 840 value USB 2.0 only
					mTestResult[TEST_USB2] = resultCode;
					mTestViewUsb2.setStatus(status, null);
					Log.d(TAG,"UsbTest.USB_2 resultCode = " + resultCode);
					if(resultCode == Test.RESULT_SUCCEEDED) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Handler handler = new Handler();
								handler.postDelayed(new Runnable() {
									@Override
									public void run() {
										if(mConfigValues == null) {
											mConfigValues = getConfigValues();
											if(mConfigValues != null) {
												mIsStarted = true;
												setIpAddress(mConfigValues);
												setInformation();
												setTest();
												startAutoTest();
												checkTestResult();
											}
										}
									}
								}, 3000);
							}
						});
					}*/
				}
			} else if(test.getClass() == HdmiTest.class) {
				
				if(extra != null) {
					if(extra.containsKey(HdmiTest.EXTRA_KEY_HDMI_EDID)) {
						mTestResult[TEST_HDMI_EDID] = resultCode;
						mTestResultExtra[TEST_HDMI_EDID] = extra;
						String manufacturerId = extra.getString(HdmiTest.EXTRA_KEY_HDMI_EDID);
						mTestViewHdmiEdid.setStatus(status, manufacturerId);
					} else {
						mTestResult[TEST_HDMI_CEC] = resultCode;
						mTestResultExtra[TEST_HDMI_CEC] = extra;
						String cecVersion = extra.getString(HdmiTest.EXTRA_KEY_HDMI_CEC);
						if(cecVersion != null) {
							Log.d(TAG,"resultCode : " + resultCode + " , cecVersion : " + cecVersion);
							mTestViewHdmiCec.setStatus(status, cecVersion);
						} else {
							Log.d(TAG,"resultCode : " + resultCode + " , cecVersion : null ");
						}
					}
				}
			} else if(test.getClass() == VoiceMicTest.class) {
				mIsTesting = false;
				if(extra != null) {
					int type = extra.getInt(VoiceMicTest.EXTRA_KEY_VOICE_MIC_TEST_TYPE);

					Log.d("InnoFactory.VoiceMicTest", "type : " + type + " , resultCode : " + resultCode + " , status : " + status);
					if(type == VoiceMicTest.TEST_TYPE_OPEN) {  // 0
						mTestResult[TEST_MIC_OPENED] = resultCode;
						mTestResultExtra[TEST_MIC_OPENED] = extra;
						mTestViewOpenMic.setStatus(status, extra.getString(VoiceMicTest.EXTRA_KEY_VOICE_MIC_FAILED_PORT));
					} else if ( type == VoiceMicTest.TEST_TYPE_CLOSE ) {  // 2
						mTestResult[TEST_MIC_CLOSED] = resultCode;
						mTestResultExtra[TEST_MIC_CLOSED] = extra;
						mTestViewCloseMic.setStatus(status, extra.getString(VoiceMicTest.EXTRA_KEY_VOICE_MIC_FAILED_PORT));
					} else if ( type == VoiceMicTest.TEST_TYPE_MUTE ) {  // 1
						mTestResult[TEST_MIC_MUTE] = resultCode;
						mTestResultExtra[TEST_MIC_MUTE] = extra;
						mTestViewMuteMic.setStatus(status, extra.getString(VoiceMicTest.EXTRA_KEY_VOICE_MIC_FAILED_PORT));
					}

					/*
					if(status == TestView.STATUS_SUCCEEDED || status == TestView.STATUS_FAILED) {
						if(mStreamingTest != null) {
							mStreamingTest.resume();
						}
					}
					*/
					
					ArrayList mic400hzDecibel = extra.getParcelableArrayList(VoiceMicTest.EXTRA_KEY_VOICE_MIC_400HZ_DECIBEL);
					ArrayList mic1khzDecibel = extra.getParcelableArrayList(VoiceMicTest.EXTRA_KEY_VOICE_MIC_1KHZ_DECIBEL);
					boolean type_open = (type == VoiceMicTest.TEST_TYPE_OPEN);

					if(mic400hzDecibel != null && mic1khzDecibel != null) {
						StringBuilder stringBuilder = new StringBuilder("Mic : ");
						for ( int ii = 0; ii < 4; ii++ ) {
							stringBuilder.append((ii+1) + "(");
							if ( type == VoiceMicTest.TEST_TYPE_OPEN ) 
								stringBuilder.append(mic400hzDecibel.get(ii) + ", ");
							stringBuilder.append(mic1khzDecibel.get(ii)+") ");
						}

						mTextViewTestResult.setText(stringBuilder.toString());
					}
					
				}

			} else if(test.getClass() == Provision.class) {
				int provisionStep = extra.getInt(Provision.PROVISION_STEP);
				if( provisionStep != 0 ) {
					if ( provisionStep == Provision.PROVISION_STEP_CSR ) {
						if(mProvisionResultCsr == Test.RESULT_UNKNOWN) {
							mProvisionResultCsr = resultCode;
							new Thread(new Runnable() {
								@Override
								public void run() {
									if(resultCode == Test.RESULT_SUCCEEDED) {
										Log.d(TAG, "PROVISION: make Csr success!");
										String Wifimac = getProvisionedData(CMD_RECEIVED_WIFI_MAC_ADDRESS);
										if ( Wifimac == null || Wifimac.isEmpty() ) {
											Log.e(TAG, "PROVISION: No Provisioned WiFi MAC Address");
											setTestResult("No Provisioned WiFi MAC Address", true);
											return ;
										}

										if ( mReceivedWifiMacAddress.equals(Wifimac.toUpperCase()) ) {
											mTestViewKeyPairing.setStatus(TestView.STATUS_SUCCEEDED, KEY_PAIRING_RESULT_SUCCESS);
											setTestViewDbClient(TestView.STATUS_TESTING, "PROVISION", false);

											GenData genDataKeyData = new GenData();
											genDataKeyData.genDataInit();
											
											if ( mTestResultExtra[TEST_CPU] != null ) {
												String temperature = Float.toString(mTestResultExtra[TEST_CPU].getFloat(CpuTest.EXTRA_KEY_CPU_TEMPERATURE));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_TEMP_CPU_CLOSE, temperature.getBytes().length, temperature.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No CPU Test Result");
												setTestResult("No CPU Test Result", true);
												return ;
											}

											if ( mTestResultExtra[TEST_WIFI_2G] != null ) {
												String antenna_A = Integer.toString(mTestResultExtra[TEST_WIFI_2G].getInt(WifiTest.EXTRA_KEY_WIFI_ANTENNA_A));
												String antenna_B = Integer.toString(mTestResultExtra[TEST_WIFI_2G].getInt(WifiTest.EXTRA_KEY_WIFI_ANTENNA_B));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_WIFI_24_A, antenna_A.getBytes().length, antenna_A.getBytes());
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_WIFI_24_B, antenna_B.getBytes().length, antenna_B.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No WiFi 2G Test Result");
												setTestResult("No WiFi 2G Test Result", true);
												return ;
											}

											if ( mTestResultExtra[TEST_WIFI_5G] != null ) {
												String antenna_A = Integer.toString(mTestResultExtra[TEST_WIFI_5G].getInt(WifiTest.EXTRA_KEY_WIFI_ANTENNA_A));
												String antenna_B = Integer.toString(mTestResultExtra[TEST_WIFI_5G].getInt(WifiTest.EXTRA_KEY_WIFI_ANTENNA_B));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_WIFI_50_A, antenna_A.getBytes().length, antenna_A.getBytes());
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_WIFI_50_B, antenna_B.getBytes().length, antenna_B.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No WiFi 5G Test Result");
												setTestResult("No WiFi 5G Test Result", true);
												return ;
											}

											if ( mTestResultExtra[TEST_BLUETOOTH] != null ) {
												String rssi = Short.toString((short)mTestResultExtra[TEST_BLUETOOTH].getShort(BluetoothTest.EXTRA_KEY_BLUETOOTH_RSSI));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_BT, rssi.getBytes().length, rssi.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No Bluetooth Test Result");
												setTestResult("No Bluetooth Test Result", true);
												return ;
											}

											String PcbSerialNo = getProvisionedData(CMD_RECEIVED_PCB_SERIAL);
											if ( PcbSerialNo != null ) {
												mTextViewPcbSerial.setText("PCB Serial No : " + PcbSerialNo);
												genDataKeyData.genDataAddData(DefProtocol.TAG_WRITTEN_PCB_SN, PcbSerialNo.getBytes().length, PcbSerialNo.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No Provisioned PCB Serial Key");
												setTestResult("No Provisioned PCB Serial Key", true);
												mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, KEY_PAIRING_RESULT_FAILURE);
												mTestViewDbClient.setStatus(TestView.STATUS_FAILED);
												stopTcpServer();
												return ;
											}

											String DTSerialNo = getProvisionedData(CMD_RECEIVED_DT_SERIAL);
											if ( DTSerialNo != null ) {
												mTextViewSerialNumber.setText("Serial No : " + DTSerialNo);
												genDataKeyData.genDataAddData(DefProtocol.TAG_WRITTEN_SN, DTSerialNo.getBytes().length, DTSerialNo.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No No Provisioned DT Serial Key");
												setTestResult("No Provisioned DT Serial Key", true);
												return ;
											}

											String RKEK_ID = getProvisionedData(CMD_RECEIVED_RKEK_ID);
											if ( RKEK_ID != null ) {
												mTextViewRKEK.setText("RKEK ID : " + RKEK_ID);
												genDataKeyData.genDataAddData(DefProtocol.TAG_WRITTEN_KEY_1, RKEK_ID.getBytes().length, RKEK_ID.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No No Provisioned RKEK ID");
												setTestResult("No Provisioned RKEK ID", true);
												return ;
											}

											String CSR = extra.getString(Provision.EXTRA_KEY_CSR_DATA);
											if ( CSR != null ) { 
												genDataKeyData.genDataAddData(DefProtocol.TAG_WRITTEN_KEY_2, CSR.getBytes().length, CSR.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No No Provisioned CSR");
												setTestResult("No Provisioned CSR", true);
												return;
											}
										
											String Ethmac = getProvisionedData(CMD_RECEIVED_ETHERNET_MAC_ADDRESS);
											if ( Ethmac != null ) {
												mTextViewEthernetMac.setText("Ethernet MAC : " + Ethmac);
												genDataKeyData.genDataAddData(DefProtocol.TAG_WRITTEN_MAC_ETH, Ethmac.getBytes().length, Ethmac.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No No Provisioned Ethernet MAC");
												setTestResult("No Provisioned Ethernet MAC", true);
												return ;
											}

											mTextViewWifiMac.setText("Wi-Fi MAC : " + Wifimac);
											genDataKeyData.genDataAddData(DefProtocol.TAG_WRITTEN_MAC_WIFI, Wifimac.getBytes().length, Wifimac.getBytes());

											String btmac = getProvisionedData(CMD_RECEIVED_BT_MAC_ADDRESS);
											if ( btmac != null ) {
												mTextViewBluetoothMac.setText("Bluetooth MAC : " + btmac);
												genDataKeyData.genDataAddData(DefProtocol.TAG_WRITTEN_MAC_BT, btmac.getBytes().length, btmac.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No No Provisioned Bluetooth MAC");
												setTestResult("No Provisioned Bluetooth MAC", true);
												return ;
											}

											if(mTestResultExtra[TEST_MIC_OPENED] != null) {
												ArrayList mic400hzDecibel = mTestResultExtra[TEST_MIC_OPENED].getParcelableArrayList(VoiceMicTest.EXTRA_KEY_VOICE_MIC_400HZ_DECIBEL);
												ArrayList mic1khzDecibel = mTestResultExtra[TEST_MIC_OPENED].getParcelableArrayList(VoiceMicTest.EXTRA_KEY_VOICE_MIC_1KHZ_DECIBEL);

												String decibel1khz = Double.toString((double)mic1khzDecibel.get(0));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_1_1KHZ_OPEN, decibel1khz.getBytes().length, decibel1khz.getBytes());
												decibel1khz = Double.toString((double)mic1khzDecibel.get(1));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_2_1KHZ_OPEN, decibel1khz.getBytes().length, decibel1khz.getBytes());
												decibel1khz = Double.toString((double)mic1khzDecibel.get(2));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_3_1KHZ_OPEN, decibel1khz.getBytes().length, decibel1khz.getBytes());
												decibel1khz = Double.toString((double)mic1khzDecibel.get(3));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_4_1KHZ_OPEN, decibel1khz.getBytes().length, decibel1khz.getBytes());

												String decibel400hz = Double.toString((double)mic400hzDecibel.get(0));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_1_400HZ_OPEN, decibel400hz.getBytes().length, decibel400hz.getBytes());
												decibel400hz = Double.toString((double)mic400hzDecibel.get(1));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_2_400HZ_OPEN, decibel400hz.getBytes().length, decibel400hz.getBytes());
												decibel400hz = Double.toString((double)mic400hzDecibel.get(2));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_3_400HZ_OPEN, decibel400hz.getBytes().length, decibel400hz.getBytes());
												decibel400hz = Double.toString((double)mic400hzDecibel.get(3));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_4_400HZ_OPEN, decibel400hz.getBytes().length, decibel400hz.getBytes());

											}

											if(mTestResultExtra[TEST_MIC_CLOSED] != null) {
												ArrayList mic400hzDecibel = mTestResultExtra[TEST_MIC_CLOSED].getParcelableArrayList(VoiceMicTest.EXTRA_KEY_VOICE_MIC_400HZ_DECIBEL);
												ArrayList mic1khzDecibel = mTestResultExtra[TEST_MIC_CLOSED].getParcelableArrayList(VoiceMicTest.EXTRA_KEY_VOICE_MIC_1KHZ_DECIBEL);
												String decibel1khz = Double.toString((double)mic1khzDecibel.get(0));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_1_1KHZ_CLOSE, decibel1khz.getBytes().length, decibel1khz.getBytes());
												decibel1khz = Double.toString((double)mic1khzDecibel.get(1));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_2_1KHZ_CLOSE, decibel1khz.getBytes().length, decibel1khz.getBytes());
												decibel1khz = Double.toString((double)mic1khzDecibel.get(2));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_3_1KHZ_CLOSE, decibel1khz.getBytes().length, decibel1khz.getBytes());
												decibel1khz = Double.toString((double)mic1khzDecibel.get(3));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_4_1KHZ_CLOSE, decibel1khz.getBytes().length, decibel1khz.getBytes());

												/*
												String decibel400hz = Double.toString((double)mic400hzDecibel.get(0));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_1_400HZ_CLOSE, decibel400hz.getBytes().length, decibel400hz.getBytes());
												decibel400hz = Double.toString((double)mic400hzDecibel.get(1));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_2_400HZ_CLOSE, decibel400hz.getBytes().length, decibel400hz.getBytes());
												decibel400hz = Double.toString((double)mic400hzDecibel.get(2));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_3_400HZ_CLOSE, decibel400hz.getBytes().length, decibel400hz.getBytes());
												decibel400hz = Double.toString((double)mic400hzDecibel.get(3));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_MIC_4_400HZ_CLOSE, decibel400hz.getBytes().length, decibel400hz.getBytes());
												*/
											}

											genDataKeyData.genDataAddData(DefProtocol.TAG_TAIL_CLIENT_2ND, 0, null);
											genDataKeyData.genDataAddCRC();
											byte[] keyData = genDataKeyData.genDataGetData();
											Log.i(TAG, "write(DefProtocol.TAG_TAIL_CLIENT_2ND)");
											setSendResult(mTcpServer.write(keyData));
										} else {
											Log.e(TAG, "PROVISION: WiFi Mac Address Mismatch");
											Log.i(TAG, "sendErrorMessage(WiFi Mac Address Mismatch)");
											setSendResult(mTcpServer.sendErrorMessage("WiFi Mac Address Mismatch"));
											setTestResult("Provisioned WiFi MAC Address Mismatch", true);
											mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, KEY_PAIRING_RESULT_FAILURE);
											mTestViewDbClient.setStatus(TestView.STATUS_FAILED);
											stopTcpServer();
										}
									} else {
										flagOTPFail = true;
										Log.e(TAG, "PROVISION: App Key Pairing Failed!");
										setSendResult(mTcpServer.sendErrorMessage("App Key Pairing Failed"));
										setTestResult("App Key Pairing Failed!", true);
										mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, KEY_PAIRING_RESULT_FAILURE);
										mTestViewDbClient.setStatus(TestView.STATUS_FAILED);
										stopTcpServer();
									}
								}
							}).start();

						}
					}
					else if( provisionStep == Provision.PROVISION_STEP_APP_KEY ) {
						if(mProvisionResultApp == Test.RESULT_UNKNOWN) {
							mProvisionResultApp = resultCode;

							new Thread(new Runnable() {
								@Override
								public void run() {
									if(resultCode == Test.RESULT_SUCCEEDED) {
										Log.d(TAG, "PROVISION: App Key Pairing success!");
										String Wifimac = getProvisionedData(CMD_RECEIVED_WIFI_MAC_ADDRESS);
										if ( Wifimac == null || Wifimac.isEmpty() ) {
											Log.e(TAG, "PROVISION: No Provisioned WiFi MAC Address");
											setTestResult("No Provisioned WiFi MAC Address", true);
											return ;
										}

										if ( mReceivedWifiMacAddress.equals(Wifimac.toUpperCase()) ) {
											mTestViewKeyPairing.setStatus(TestView.STATUS_SUCCEEDED, KEY_PAIRING_RESULT_SUCCESS);
											setTestViewDbClient(TestView.STATUS_TESTING, "PROVISION", false);

											GenData genDataKeyData = new GenData();
											genDataKeyData.genDataInit();
											
											if ( mTestResultExtra[TEST_CPU] != null ) {
												String temperature = Float.toString(mTestResultExtra[TEST_CPU].getFloat(CpuTest.EXTRA_KEY_CPU_TEMPERATURE));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_TEMP_CPU_CLOSE, temperature.getBytes().length, temperature.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No CPU Test Result");
												setTestResult("No CPU Test Result", true);
												return ;
											}

											if ( mTestResultExtra[TEST_WIFI_2G] != null ) {
												String antenna_A = Integer.toString(mTestResultExtra[TEST_WIFI_2G].getInt(WifiTest.EXTRA_KEY_WIFI_ANTENNA_A));
												String antenna_B = Integer.toString(mTestResultExtra[TEST_WIFI_2G].getInt(WifiTest.EXTRA_KEY_WIFI_ANTENNA_B));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_WIFI_24_A, antenna_A.getBytes().length, antenna_A.getBytes());
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_WIFI_24_B, antenna_B.getBytes().length, antenna_B.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No WiFi 2G Test Result");
												setTestResult("No WiFi 2G Test Result", true);
												return ;
											}

											if ( mTestResultExtra[TEST_WIFI_5G] != null ) {
												String antenna_A = Integer.toString(mTestResultExtra[TEST_WIFI_5G].getInt(WifiTest.EXTRA_KEY_WIFI_ANTENNA_A));
												String antenna_B = Integer.toString(mTestResultExtra[TEST_WIFI_5G].getInt(WifiTest.EXTRA_KEY_WIFI_ANTENNA_B));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_WIFI_50_A, antenna_A.getBytes().length, antenna_A.getBytes());
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_WIFI_50_B, antenna_B.getBytes().length, antenna_B.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No WiFi 5G Test Result");
												setTestResult("No WiFi 5G Test Result", true);
												return ;
											}

											if ( mTestResultExtra[TEST_BLUETOOTH] != null ) {
												String rssi = Short.toString((short)mTestResultExtra[TEST_BLUETOOTH].getShort(BluetoothTest.EXTRA_KEY_BLUETOOTH_RSSI));
												genDataKeyData.genDataAddData(DefProtocol.TAG_CAL_BT, rssi.getBytes().length, rssi.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No Bluetooth Test Result");
												setTestResult("No Bluetooth Test Result", true);
												return ;
											}

											String PcbSerialNo = getProvisionedData(CMD_RECEIVED_PCB_SERIAL);
											if ( PcbSerialNo != null ) {
												mTextViewPcbSerial.setText("PCB Serial No : " + PcbSerialNo);
												genDataKeyData.genDataAddData(DefProtocol.TAG_WRITTEN_PCB_SN, PcbSerialNo.getBytes().length, PcbSerialNo.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No Provisioned PCB Serial Key");
												setTestResult("No Provisioned PCB Serial Key", true);
												mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, KEY_PAIRING_RESULT_FAILURE);
												mTestViewDbClient.setStatus(TestView.STATUS_FAILED);
												stopTcpServer();
												return ;
											}

											String DTSerialNo = getProvisionedData(CMD_RECEIVED_DT_SERIAL);
											if ( DTSerialNo != null ) {
												mTextViewSerialNumber.setText("Serial No : " + DTSerialNo);
												genDataKeyData.genDataAddData(DefProtocol.TAG_WRITTEN_SN, DTSerialNo.getBytes().length, DTSerialNo.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No No Provisioned DT Serial Key");
												setTestResult("No Provisioned DT Serial Key", true);
												return ;
											}

											String RKEK_ID = getProvisionedData(CMD_RECEIVED_RKEK_ID);
											if ( RKEK_ID != null ) {
												mTextViewRKEK.setText("RKEK ID : " + RKEK_ID);
												genDataKeyData.genDataAddData(DefProtocol.TAG_WRITTEN_KEY_1, RKEK_ID.getBytes().length, RKEK_ID.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No No Provisioned RKEK ID");
												setTestResult("No Provisioned RKEK ID", true);
												return ;
											}
										
											String Ethmac = getProvisionedData(CMD_RECEIVED_ETHERNET_MAC_ADDRESS);
											if ( Ethmac != null ) {
												mTextViewEthernetMac.setText("Ethernet MAC : " + Ethmac);
												genDataKeyData.genDataAddData(DefProtocol.TAG_WRITTEN_MAC_ETH, Ethmac.getBytes().length, Ethmac.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No No Provisioned Ethernet MAC");
												setTestResult("No Provisioned Ethernet MAC", true);
												return ;
											}

											mTextViewWifiMac.setText("Wi-Fi MAC : " + Wifimac);
											genDataKeyData.genDataAddData(DefProtocol.TAG_WRITTEN_MAC_WIFI, Wifimac.getBytes().length, Wifimac.getBytes());

											String btmac = getProvisionedData(CMD_RECEIVED_BT_MAC_ADDRESS);
											if ( btmac != null ) {
												mTextViewBluetoothMac.setText("Bluetooth MAC : " + btmac);
												genDataKeyData.genDataAddData(DefProtocol.TAG_WRITTEN_MAC_BT, btmac.getBytes().length, btmac.getBytes());
											} else {
												Log.e(TAG, "PROVISION: No No Provisioned Bluetooth MAC");
												setTestResult("No Provisioned Bluetooth MAC", true);
												return ;
											}

											genDataKeyData.genDataAddData(DefProtocol.TAG_TAIL_CLIENT_1ST, 0, null);
											genDataKeyData.genDataAddCRC();
											byte[] keyData = genDataKeyData.genDataGetData();
											Log.i(TAG, "write(DefProtocol.TAG_TAIL_CLIENT_1ST)");
											setSendResult(mTcpServer.write(keyData));
										} else {
											SystemProperties.set("inno.provision.app", "error");
											Log.e(TAG, "PROVISION: WiFi Mac Address Mismatch");
											Log.i(TAG, "sendErrorMessage(WiFi Mac Address Mismatch)");
											setSendResult(mTcpServer.sendErrorMessage("WiFi Mac Address Mismatch"));
											setTestResult("Provisioned WiFi MAC Address Mismatch", true);
											mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, KEY_PAIRING_RESULT_FAILURE);
											mTestViewDbClient.setStatus(TestView.STATUS_FAILED);
											stopTcpServer();
										}
									} else {
										flagOTPFail = true;
										Log.e(TAG, "PROVISION: App Key Pairing Failed!");
										Log.i(TAG, "sendErrorMessage(App Key Pairing Failed)");
										setSendResult(mTcpServer.sendErrorMessage("App Key Pairing Failed"));
										setTestResult("App Key Pairing Failed!", true);
										mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, KEY_PAIRING_RESULT_FAILURE);
										mTestViewDbClient.setStatus(TestView.STATUS_FAILED);
										stopTcpServer();
									}
								}
							}).start();
						}

					} 
					else if(provisionStep == Provision.PROVISION_STEP_EMMC_OTP ) {
						if(mProvisionResultEmmcOtp == Test.RESULT_UNKNOWN) {
							mProvisionResultEmmcOtp = resultCode;
							new Thread(new Runnable() {
								@Override
								public void run() {
									if(resultCode == Test.RESULT_SUCCEEDED) {
										Log.i(TAG, "PROVISION: Key provision and OTP lock are success!");
										mTestViewKeyPairing.setStatus(TestView.STATUS_SUCCEEDED, KEY_PAIRING_RESULT_FAILURE);
										boolean result = setWipeData();
										if ( !result ) {
											setTestResult("Wipe Data Failed!", true);
											return;
										}
										setFactoryComplete("Factory Step 1 Process has been Completed!");
										Log.i(TAG, "sendFlag(DefProtocol.FA_ACK_COMPLETE");
										setTestViewDbClient(TestView.STATUS_TESTING, "REQ ACK_COMPLETE");
										setSendResult(mTcpServer.sendFlag(DefProtocol.FA_ACK_COMPLETE));
										Log.d(TAG, "onReceiveFlag() Finish Factory Step 1...");


									} else {
										Log.e(TAG, "PROVISION: OTP Lock Failed!");
										Log.i(TAG, "sendErrorMessage(OTP Lock Failed)");
										setSendResult(mTcpServer.sendErrorMessage("OTP Lock Failed"));
										mTestViewOtpLock.setStatus(TestView.STATUS_FAILED, "FAILED");
										flagOTPFail = true;
										mTestViewDbClient.setStatus(TestView.STATUS_FAILED);
										setTestResult("OTP Lock Failed!", true);
										stopTcpServer();
									}
								}
							}).start();
						}
					}
				}
			}

			checkTestResult();
		}

	};



	private TCPServer.SocketListener mSocketListener = new TCPServer.SocketListener() {
		
		@Override
		public void onStatusChanged(boolean isConnected) {
			LOG("onStatusChanged (isConnected: " + isConnected + ")");
			if(isConnected) {
				setTestViewDbClient(TestView.STATUS_TESTING, null);
			} else {
				setTestViewDbClient(TestView.STATUS_FAILED, null);
			}
		}

		private String tag2String(int tag) {
			switch ( tag ) {
				case DefProtocol.DB_SECURE_REQUEST:
					return "DefProtocol.DB_SECURE_REQUEST";
				case DefProtocol.DB_REQUEST:
					return "DefProtocol.DB_REQUEST";
				case DefProtocol.DB_REQ_MODEL:
					return "DefProtocol.DB_REQ_MODEL";
				case DefProtocol.DB_UPDATE_COMPLETE_1ST:
					return "DefProtocol.DB_UPDATE_COMPLETE_1ST";
				case DefProtocol.DB_UPDATE_COMPLETE_2ND:
					return "DefProtocol.DB_UPDATE_COMPLETE_2ND";
				case DefProtocol.DB_UPDATE_FAILED_1ST:
					return "DefProtocol.DB_UPDATE_FAILED_1ST";
				case DefProtocol.DB_REQ_KEY_DATA:
					return "DefProtocol.DB_REQ_KEY_DATA";
				case DefProtocol.DB_UPDATE_FAILED_2ND:
					return "DefProtocol.DB_UPDATE_FAILED_2ND";
				case DefProtocol.DB_REQUEST_DISCONNECT:
					return "DefProtocol.DB_REQUEST_DISCONNECT";
				case DefProtocol.TAG_AUDIO_21_FREQ_L:
					return "DefProtocol.TAG_AUDIO_21_FREQ_L";
				case DefProtocol.TAG_AUDIO_21_FREQ_R:
					return "DefProtocol.TAG_AUDIO_21_FREQ_R";
				case DefProtocol.TAG_AUDIO_21_VPP_L:
					return "DefProtocol.TAG_AUDIO_21_VPP_L";
				case DefProtocol.TAG_AUDIO_21_VPP_R:
					return "DefProtocol.TAG_AUDIO_21_VPP_R";
				case DefProtocol.TAG_TAIL_DB_CLIENT_11:
					return "DefProtocol.TAG_TAIL_DB_CLIENT_11";
				case DefProtocol.TAG_TAIL_DB_CLIENT_12:
					return "DefProtocol.TAG_TAIL_DB_CLIENT_12";
				case DefProtocol.TAG_TAIL_DB_CLIENT_21:
					return "DefProtocol.TAG_TAIL_DB_CLIENT_21";
				case DefProtocol.TAG_SERIAL_NUMBER:
					return "DefProtocol.TAG_SERIAL_NUMBER";
				case DefProtocol.TAG_PCB_SN:
					return "DefProtocol.TAG_PCB_SN";
				case DefProtocol.TAG_MAC_IP4_ETHERNET:
					return "DefProtocol.TAG_MAC_IP4_ETHERNET";
				case DefProtocol.TAG_MAC_IP4_WIFI:
					return "DefProtocol.TAG_MAC_IP4_WIFI";
				case DefProtocol.TAG_MAC_IP4_BT:
					return "DefProtocol.TAG_MAC_IP4_BT";
				case DefProtocol.TAG_SYNAPTICS_INIT_IMAGE:
					return "DefProtocol.TAG_SYNAPTICS_INIT_IMAGE";
				case DefProtocol.TAG_KEY_1:
					return "DefProtocol.TAG_KEY_1";
				case DefProtocol.TAG_KEY_2:
					return "DefProtocol.TAG_KEY_2";
				case DefProtocol.TAG_TAIL_KEY_1:
					return "DefProtocol.TAG_TAIL_KEY_1";
				case DefProtocol.TAG_TAIL_REQ_KEY_DATA:
					return "DefProtocol.TAG_TAIL_REQ_KEY_DATA";
				case DefProtocol.TAG_TAIL_PUBLIC_KEY:
					return "DefProtocol.TAG_TAIL_PUBLIC_KEY";
				case DefProtocol.TAG_TAIL_AES_KEY:
					return "DefProtocol.TAG_TAIL_AES_KEY";
			}
			return "Unknown Tag";
		}


		@Override
		public void onReceivedFlag(int flag) {
			LOG("TCPserver::onReceivedFlag (flag: " + tag2String(flag) + ", " + Integer.toHexString(flag)+ ")");
			switch(flag) {
				case DefProtocol.DB_SECURE_REQUEST:
					mTcpServer.setCryptoEnabled(true);
					setTestViewDbClient(TestView.STATUS_TESTING, "REQUEST");
					setSendResult(mTcpServer.sendFlag(DefProtocol.FA_SECURE_ACK));
					LOG("Send : DefProtocol.FA_SECURE_ACK");
					break;

				case DefProtocol.DB_REQUEST:
					mTcpServer.setCryptoEnabled(false);
					setTestViewDbClient(TestView.STATUS_TESTING, "REQUEST");
					setSendResult(mTcpServer.sendFlag(DefProtocol.FA_ACK));
					 LOG("Send : DefProtocol.FA_ACK");
					break;

				case DefProtocol.DB_REQ_MODEL:
					setTestViewDbClient(TestView.STATUS_TESTING, "REQ MODEL");
					String modelName = Settings.Global.getString(MainActivity.this.getContentResolver(), Settings.Global.DEVICE_NAME);
					String factoryVersion = getFactoryVersion();

					String softwareVersion = SystemProperties.get("ro.inno.factory.service.version","empty");
					String hardwareVersion = SystemProperties.get("ro.oem.hw.version", "empty");
					String emmcVendor = SystemProperties.get("ro.runtime.innopia.flash","empty");
//					String buildVersion = SystemProperties.get("ro.inno.factory.version","empty");


					GenData genDataModel = new GenData();
					genDataModel.genDataInit();
					if(modelName != null)
						genDataModel.genDataAddData(DefProtocol.TAG_TEXT_MODEL, modelName.getBytes().length, modelName.getBytes());
					
					if(factoryVersion != null)
						genDataModel.genDataAddData(DefProtocol.TAG_VER_FACTORYSW, factoryVersion.getBytes().length, factoryVersion.getBytes());

					if(softwareVersion != null)
						genDataModel.genDataAddData(DefProtocol.TAG_VER_MAINSW, softwareVersion.getBytes().length, softwareVersion.getBytes());

					if(hardwareVersion != null)
						genDataModel.genDataAddData(DefProtocol.TAG_VER_HW, hardwareVersion.getBytes().length, hardwareVersion.getBytes());

					if(emmcVendor != null)
						genDataModel.genDataAddData(DefProtocol.TAG_NAME_EMMC, emmcVendor.getBytes().length, emmcVendor.getBytes());
							
					genDataModel.genDataAddData(DefProtocol.TAG_TAIL_MODEL, 0, null);
					genDataModel.genDataAddCRC();
					byte[] model = genDataModel.genDataGetData();

					setSendResult(mTcpServer.write(model));
					LOG("Send : DefProtocol.TAG_TAIL_MODEL");
					break;

				case DefProtocol.DB_UPDATE_COMPLETE_1ST:
					setTestViewDbClient(TestView.STATUS_TESTING, "UPDATE_COMPLETE_1ST");
					Log.d(TAG, "onReceiveFlag() Start OTP Lock on Factory Step 1");
					if ( mProvisionResultEmmcOtp == Test.RESULT_UNKNOWN ) {
						setTestResult("Key Provision: EMMC_OTP", false);
						mTestViewDbClient.setStatus(TestView.STATUS_TESTING, "EMMC_OTP", false);
						mTestViewOtpLock.setStatus(TestView.STATUS_TESTING, "EMMC_OTP", false);
						mProvision.setProvisionStep(Provision.PROVISION_STEP_EMMC_OTP);
					}
					break;
		        case DefProtocol.DB_UPDATE_COMPLETE_2ND:
					setTestViewDbClient(TestView.STATUS_TESTING, "UPDATE_COMPLETE_2ND");
					boolean result = setFactoryEnd();
					if ( !result ) {
						setTestResult("Factory End Failed!", true);
						return;
					}

					mTestViewOtpLock.setStatus(TestView.STATUS_SUCCEEDED, "Success");
					setFactoryComplete("All Factory Processes have been completed!");
					Log.i(TAG, "sendFlag(DefProtocol.FA_ACK_COMPLETE)");
					setTestViewDbClient(TestView.STATUS_TESTING, "REQ ACK_COMPLETE");
					setSendResult(mTcpServer.sendFlag(DefProtocol.FA_ACK_COMPLETE));
					break;

				case DefProtocol.DB_UPDATE_FAILED_1ST:
					setTestViewDbClient(TestView.STATUS_TESTING, "UPDATE_FAILED_1ST");
					Log.d(TAG, "onReceivedFlag() Received Key Paring Error on Factory Step 1");
					setTestResult("PROVISION Error!", true);
					mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, "PCB SN", false);
					mTestViewDbClient.setStatus(TestView.STATUS_FAILED, "PCB SN", false); 
					stopTcpServer();
					break;

				case DefProtocol.DB_REQ_KEY_DATA:
					setTestViewDbClient(TestView.STATUS_TESTING, "REQ KEY DATA");
					break;
				case DefProtocol.DB_UPDATE_FAILED_2ND:
					setTestViewDbClient(TestView.STATUS_TESTING, "UPDATE_FAILED_2ND");
					Log.e(TAG, "Error: DB_UPDATE_FAILED_2ND");
					setTestResult("Error: DB_UPDATE_FAILED_2ND", true);
					stopTcpServer();
					break;

				case DefProtocol.DB_REQUEST_DISCONNECT:
					//mTcpServer.stop();
					setTestViewDbClient(TestView.STATUS_SUCCEEDED, "DISCONNECT");
					mTestViewOtpLock.setStatus(TestView.STATUS_SUCCEEDED);	

					mTestViewKeyPairing.setStatus(TestView.STATUS_SUCCEEDED, "OK");
					mTestViewOtpLock.setStatus(TestView.STATUS_SUCCEEDED, "OK");
					

					break;
			}
		}


		@Override
		public void onReceivedData(int tag, byte[] data) {
			LOG("TCPServer::onReceivedData (tag: " + Integer.toHexString(tag)+")");
			switch(tag) {
				case DefProtocol.TAG_AUDIO_21_FREQ_L :
					mAudioTestResultFreq_L = Integer.parseInt(new String(data));
					LOG("TCPServer::mAudioTestResultFreq_L = "+mAudioTestResultFreq_L);
					break;
				case DefProtocol.TAG_AUDIO_21_FREQ_R :
					mAudioTestResultFreq_R = Integer.parseInt(new String(data));
					LOG("TCPServer::mAudioTestResultFreq_R = "+mAudioTestResultFreq_R);
					break;
				case DefProtocol.TAG_AUDIO_21_VPP_L :
					mAudioTestResultVPP_L = Float.parseFloat(new String(data));
					LOG("TCPServer::mAudioTestResultVPP_L = "+mAudioTestResultVPP_L);
					break;
				case DefProtocol.TAG_AUDIO_21_VPP_R :
				 	mAudioTestResultVPP_R = Float.parseFloat(new String(data));
					LOG("TCPServer::mAudioTestResultVPP_R = "+mAudioTestResultVPP_R);
					break;

				case DefProtocol.TAG_SERIAL_NUMBER:
					setTestViewDbClient(TestView.STATUS_TESTING, "DT SERIAL NUMBER");
					
					mReceivedDTSerialNumber = new String(data);

					createFile(DT_SERIALNO_FILE_PATH,data);
					LOG("onReceivedSerialNumber = "+mReceivedDTSerialNumber);
					break;

				case DefProtocol.TAG_KEY_2:
					if ( mIsAllSucceeded ) {
						mReceivedPcbSerial_1st = new String(data);
						LOG("onReceivedPcbSerial 1st = " + mReceivedPcbSerial_1st);
						createFile(PCB_SERIALNO_FILE_PATH,data);
					} else {
						String msg = "Key Pairing Error: All tests have NOT completed yet!";
						Log.e(TAG, msg);
						Log.i(TAG, "sendErrorMessage("+msg+")");
						setSendResult(mTcpServer.sendErrorMessage(msg));
						setTestResult("All tests have NOT completed yet!", true);
						mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, KEY_PAIRING_RESULT_FAILURE, false);
						mTestViewDbClient.setStatus(TestView.STATUS_FAILED, false);
					}
					break;

				case DefProtocol.TAG_PCB_SN:
					mReceivedPcbSerial_2nd = new String(data);
					LOG("onReceivedPcbSerialNumber = "+mReceivedPcbSerial_2nd);
					mTextViewPcbSerial.setText("PCB Serial No : " + (mReceivedPcbSerial_2nd == null ? "" : mReceivedPcbSerial_2nd));
					break;

				case DefProtocol.TAG_MAC_IP4_ETHERNET:
					setTestViewDbClient(TestView.STATUS_TESTING, "MAC ADDRESS");

					mReceivedMacAddress = new String(data);
					createFile(ETH_MAC_ADDR_FILE_PATH,data);

					LOG("onReceivedMacAddress = "+mReceivedMacAddress);
					break;

				case DefProtocol.TAG_MAC_IP4_WIFI:
					setTestViewDbClient(TestView.STATUS_TESTING, "WIFI MAC ADDRESS");
					mReceivedWifiMacAddress = new String(data);
					createFile(WIFI_MAC_ADDR_FILE_PATH,data);
					LOG("onReceivedWifiMacAddress = "+mReceivedWifiMacAddress);
					break;

				case DefProtocol.TAG_MAC_IP4_BT:
					setTestViewDbClient(TestView.STATUS_TESTING, "BT MAC ADDRESS");
					mReceivedBTMacAddress = new String(data);
					createFile(BT_MAC_ADDR_FILE_PATH,data);
					LOG("onReceivedBTMacAddress = "+mReceivedBTMacAddress);
					break;

				case DefProtocol.TAG_SYNAPTICS_INIT_IMAGE:
					createFile(INIT_IMAGE_FILE_PATH, data);
					break;

				case DefProtocol.TAG_KEY_1:
					mReceivedRKEKID = new String(data);
					createFile(RKEK_FILE_PATH,data);
					break;

				case DefProtocol.TAG_TAIL_DB_CLIENT_11:
					// TODO : key provision (PCB & APP)
					Log.d(TAG, "onReceivedData() Start Provision for Factory Step 1");
					mTestViewKeyPairing.setStatus(TestView.STATUS_TESTING, null);
					if ( mIsAllSucceeded ) {
						Log.d(TAG, "PROVISION : Start PCB Serial Key...");

						/*
						LOG("DT SerialNumber ("+mReceivedDTSerialNumber+") / " + checkSerial(mReceivedDTSerialNumber));
						LOG("WiFi Mac ("+mReceivedWifiMacAddress+") / " + checkMac(mReceivedWifiMacAddress));
						LOG("BT Mac ("+mReceivedBTMacAddress+") / " + checkMac(mReceivedBTMacAddress));
						LOG("PCB SerialNumber ("+mReceivedPcbSerial_2nd+") / " + checkPcbSn(mReceivedPcbSerial_2nd));
						*/

						if ( mReceivedDTSerialNumber == null || !checkSerial(mReceivedDTSerialNumber) || 
								mReceivedWifiMacAddress == null || !checkMac(mReceivedWifiMacAddress) || 
								mReceivedBTMacAddress == null || !checkMac(mReceivedBTMacAddress) ||
								mReceivedPcbSerial_1st == null || !checkPcbSn(mReceivedPcbSerial_1st) ) {
							Log.e(TAG, "PROVISION : Invalid Key!");
							Log.i(TAG, "sendErrorMessage ( Invalid Key! )");
							setSendResult(mTcpServer.sendErrorMessage("Invalid Key!"));
							setTestResult("Invalid Key!", true);
							mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, KEY_PAIRING_RESULT_FAILURE);
							mTestViewDbClient.setStatus(TestView.STATUS_FAILED);
							stopTcpServer();
							break;
						}

						mTestViewKeyPairing.setStatus(TestView.STATUS_TESTING, "APP KEY", false);
						Log.d(TAG, "PROVISION : Start provisioning App Keys...");
						setTestResult("Key Provision: APP KEY", false);
						mProvision.setProvisionStep(Provision.PROVISION_STEP_APP_KEY);
					} else {
						String msg = "Key Pairing Error: All tests have NOT completed yet!";
						Log.e(TAG, msg);
						Log.i(TAG, "sendErrorMessage("+msg+")");
						setSendResult(mTcpServer.sendErrorMessage(msg));
						setTestResult("All tests have NOT completed yet!", true);
						mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, KEY_PAIRING_RESULT_FAILURE, false);
						mTestViewDbClient.setStatus(TestView.STATUS_FAILED, false);
					}
					break;

				case DefProtocol.TAG_TAIL_DB_CLIENT_21:
					mTestViewKeyPairing.setStatus(TestView.STATUS_TESTING, null);
					Log.d(TAG, "onReceivedData() Start Provision for Factory Step 2");

					if ( mIsAllSucceeded ) {
						String prevSerialNo = getDTSerialNumber();
						if ( prevSerialNo == null || prevSerialNo.isEmpty() ) {
							Log.e(TAG, "Provision: No Provisioned DT Serial Number");
							Log.i(TAG, "sendErrorMessage ( No Provisioned DT Serial Number)");
							setSendResult(mTcpServer.sendErrorMessage("No Provisioned DT Serial Number"));
							setTestResult("No Provisioned Serial Number" , true);
							mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, KEY_PAIRING_RESULT_FAILURE);
							mTestViewDbClient.setStatus(TestView.STATUS_FAILED);
							stopTcpServer();
							break;
						}

						LOG("onReceivedData() provisioned DT Serial=" + prevSerialNo);
						if ( mReceivedDTSerialNumber == null || !mReceivedDTSerialNumber.equals(prevSerialNo) ) {
							Log.e(TAG, "PROVISION : DT Serial Number Mismatch");
							Log.i(TAG, "sendErrorMessage ( DT Serial Number Mismatch )");
							setSendResult(mTcpServer.sendErrorMessage("DT Serial Number Mismatch"));
							setTestResult("Provisioned DT Serial Number Mismatch", true);
							mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, KEY_PAIRING_RESULT_FAILURE);
							mTestViewDbClient.setStatus(TestView.STATUS_FAILED);
							stopTcpServer();
							break;
						}

						/*
						LOG("DT SerialNumber ("+mReceivedDTSerialNumber+") / " + checkSerial(mReceivedDTSerialNumber));
						LOG("WiFi Mac ("+mReceivedWifiMacAddress+") / " + checkMac(mReceivedWifiMacAddress));
						LOG("BT Mac ("+mReceivedBTMacAddress+") / " + checkMac(mReceivedBTMacAddress));
						LOG("PCB SerialNumber ("+mReceivedPcbSerial_2nd+") / " + checkPcbSn(mReceivedPcbSerial_2nd));
						*/

						if ( mReceivedDTSerialNumber == null || !checkSerial(mReceivedDTSerialNumber) || 
								mReceivedWifiMacAddress == null || !checkMac(mReceivedWifiMacAddress) || 
								mReceivedBTMacAddress == null || !checkMac(mReceivedBTMacAddress) ||
								mReceivedPcbSerial_2nd == null || !checkPcbSn(mReceivedPcbSerial_2nd) ) {
							Log.e(TAG, "PROVISION : Invalid Key!");
							Log.i(TAG, "sendErrorMessage ( Invalid Key! )");
							setSendResult(mTcpServer.sendErrorMessage("Invalid Key!"));
							setTestResult("Invalid Key!", true);
							mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, KEY_PAIRING_RESULT_FAILURE);
							mTestViewDbClient.setStatus(TestView.STATUS_FAILED);
							stopTcpServer();
							break;
						}

						// TODO : make csr file
						mTestViewKeyPairing.setStatus(TestView.STATUS_TESTING, "MAKE CSR", false);
						Log.d(TAG, "PROVISION : Start making CSR file...");
						setTestResult("Key Provision: CSR", false);
						mProvision.setProvisionStep(Provision.PROVISION_STEP_CSR);
					} else {
						String msg = "Key Pairing Error: All tests have NOT completed yet!";
						Log.e(TAG, msg);
						Log.i(TAG, "sendErrorMessage("+msg+")");
						setSendResult(mTcpServer.sendErrorMessage(msg));
						setTestResult("All tests have NOT completed yet!", true);
						mTestViewKeyPairing.setStatus(TestView.STATUS_FAILED, KEY_PAIRING_RESULT_FAILURE, false);
						mTestViewDbClient.setStatus(TestView.STATUS_FAILED, false);
					}
					break;
				case DefProtocol.TAG_TAIL_AES_KEY:
					break;
			}
		}

	};



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		LOG("onCreate");

		SystemProperties.set("persist.sys.usb.configfs", "0");
		Settings.Global.putInt(MainActivity.this.getContentResolver(),
				Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 
				(BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB));


		// QG 1.0 -> 000, QG1.5 -> 001
		//is_old_hw_version = SystemProperties.get("ro.boot.hw.version", "001").equals("000");

/*		mHandler = new Handler();
		mHandler.postDelayed(new Runnable() {
			public void run() { 
				executeCommand(CMD_HDCP_DISABLE);
				executeCommand(CMD_RESOLUTION_1080P);
				mConfigValues = getConfigValues();

				mEthernetManager = (EthernetManager)getSystemService(Context.ETHERNET_SERVICE);
				mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
				mTcpServer = null;
				mKeyPairingTimer = null;
				setWidget();
				setIpAddress(mConfigValues);
				setVolume(mConfigValues);
				setInformation();
				initButtonTest();
				initWifiTest();
				if(!mIsStarted) {
					LOG("setTest");
					mIsStarted = true;
					setTest();
				}
						
			} 
		}, 4000);
		*/

//		executeCommand("settings put global hdmi_control_enabled 1");
//		executeCommand("echo 0 > /proc/inno/usb_id");
		Settings.Global.putString(getContentResolver(), "hdmi_control_enabled", "1");
//		executeCommand(CMD_HDCP_DISABLE);
//		executeCommand(CMD_RESOLUTION_1080P);
//		mConfigValues = getConfigValues();
		//setLedColor("green_blink");
		mConfigValues = null;

		mIpConfiguration = null;
		mEthernetManager = (EthernetManager)getSystemService(Context.ETHERNET_SERVICE);
		InterfaceStateListener ethernetListener = (iface, state, role, configuration) -> {
			LOG("ethernet link " +(state == EthernetManager.STATE_LINK_UP ? "up" : "down")+"("+state+")");
			if ( mIpConfiguration == null && state == EthernetManager.STATE_LINK_UP ) {
				LOG("ethernet set configuration");
				mIpConfiguration = configuration;
				setIpAddress(mConfigValues);
			}
		};

		mEthernetManager.addInterfaceStateListener(getMainExecutor(), ethernetListener);

		mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

		mTcpServer = null;
		
		mKeyPairingTimer = null;

		setWidget();

		mTestResult = new int[TEST_MAX_INDEX];
		mTestResultExtra = new Bundle[TEST_MAX_INDEX];

		for(int i=0; i<TEST_MAX_INDEX; i++) {
		  mTestResult[i] = Test.RESULT_UNKNOWN;
		  mTestResultExtra[i] = null;
		}

		if(mConfigValues == null) {
			LOG("!!!! mConfigValues is null");
			if ( mUsbTest == null ) {
				SystemProperties.set("persist.sys.usb.configfs", "1");
				mUsbTest = new UsbTest(this);
				mUsbTest.setOnResultListener(mOnResultListener);
				mUsbTest.start();
			}
			if(mStreamingTest == null) {
				mStreamingTest = new StreamingTest(MainActivity.this, mSurfaceView.getHolder(), mConfigValues);
				mStreamingTest.setOnResultListener(mOnResultListener);
			}
		}


		setIpAddress(mConfigValues);
		setFactoryStep(mConfigValues);
		setVolume(mConfigValues);
		setInformation();
		initButtonTest();
		initWifiTest();

		IntentFilter filter = new IntentFilter("com.innopia.leds.mic_state_changed");
		registerReceiver(new MicSwitchObserver(), filter, Context.RECEIVER_EXPORTED);

		mLogTask = new AsyncLogViewTask();
		mLogTask.execute();

		if ( mConfigValues == null ) {
			checkBootCount();
		}
	}



	@Override
	protected void onStart() {
		super.onStart();
		LOG("onStart");
	}



	@Override
	protected void onStop() {
		super.onStop();
		LOG("onStop");
		stopTest();
		if(mTcpServer != null) {
			mTcpServer.stop();
			mTcpServer = null;
		}
	}



	@Override
	protected void onDestroy() {
		stopTest();
		super.onDestroy();

	}



	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {

		if(!mIsStarted) return super.dispatchKeyEvent(event);
		if(mTestViewOtpLock.getStatus() == TestView.STATUS_TESTING || 
				mTestViewHdmiCec.getStatus() != TestView.STATUS_SUCCEEDED ) return false;

		LOG("KeyEvent :" + event + " , mIsStarted (" + mIsStarted + "), otp lock (" 
				+ (mTestViewOtpLock.getStatus() == TestView.STATUS_TESTING) + "), mIsTesting (" + mIsTesting + ")" );
		// Hidden Key
        if ( event.getAction() == KeyEvent.ACTION_UP ) {
            if ( event.getKeyCode() == KeyEvent.KEYCODE_9 || event.getKeyCode() == KeyEvent.KEYCODE_NUMPAD_9 ) {
                if ( ++mHdmiCecHiddenKeyPressed >= HDMI_CEC_PASS_BY_HIDDEN_KEY &&
                        mTestResult[TEST_HDMI_CEC] != Test.RESULT_SUCCEEDED )
                {
                    String cecVersion = "1";

                    Bundle extra = new Bundle();
                    extra.putString(HdmiTest.EXTRA_KEY_HDMI_CEC, cecVersion);

                    mTestResult[TEST_HDMI_CEC] = Test.RESULT_SUCCEEDED;
                    mTestResultExtra[TEST_HDMI_CEC] = extra;

                    mTestViewHdmiCec.setStatus(TestView.STATUS_SUCCEEDED, cecVersion);

                    Log.w(TAG, "HDMI CEC Test is passed manually by hidden key...");
                }
            }
            else {
                mHdmiCecHiddenKeyPressed = 0;
            }
        }

		if(event.getAction() == KeyEvent.ACTION_UP) {

			if(!mIsTesting){

				switch (event.getKeyCode()) {
					case KeyEvent.KEYCODE_1:
						if(mTcpServer == null) {
							mIsTesting = true;
							mTestResult[TEST_WIFI_2G] = Test.RESULT_UNKNOWN;
							mTestResultExtra[TEST_WIFI_2G] = null;
							mTestViewWifi2G.setStatus(TestView.STATUS_READY, null);
							mWifiTest.setTestFrequency(WifiTest.FREQUENCY_2G);
							mWifiTest.start();
							mTestViewWifi.setStatus(TestView.STATUS_TESTING, null);
						}
						break;

					case KeyEvent.KEYCODE_2:
						if(mTcpServer == null) {
							mIsTesting = true;
							mTestResult[TEST_WIFI_5G] = Test.RESULT_UNKNOWN;
							mTestResultExtra[TEST_WIFI_5G] = null;
							mTestViewWifi5G.setStatus(TestView.STATUS_READY, null);
							mWifiTest.setTestFrequency(WifiTest.FREQUENCY_5G);
							mWifiTest.start();
							mTestViewWifi.setStatus(TestView.STATUS_TESTING, null);
						}
						break;

					case KeyEvent.KEYCODE_3:
						if(mTcpServer == null) {
							mBluetoothTest.start();
							mTestViewBluetooth.setStatus(TestView.STATUS_READY, null);
						}
						break;

					case KeyEvent.KEYCODE_4:
						if(mTcpServer == null) {
							if(mTestResult[TEST_ETHERNET] == Test.RESULT_SUCCEEDED) {
								//mIsTesting = true;
								//mTestViewStreaming.setStatus(TestView.STATUS_TESTING, null);
								//mStreamingTest.start(mConfigValues);
								mStreamingTest.pause();
								mTestViewStreaming.setStatus(TestView.STATUS_READY, null);
								mStreamingTest.resume();
							} else {
								mTestResult[TEST_STREAMING] = Test.RESULT_FAILED;
								mTestViewStreaming.setStatus(TestView.STATUS_FAILED, null);
							}
						}
						break;
					case KeyEvent.KEYCODE_5:
						break;

					case KeyEvent.KEYCODE_7:
						break;

					case KeyEvent.KEYCODE_8:
						break;

					case KeyEvent.KEYCODE_DPAD_CENTER:
						checkIRTestResult();
						break;

					case KeyEvent.KEYCODE_MEDIA_REWIND:
						break;

					case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
						break;

					case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
						if ( !isMuteMic() && isMicReady() ) {
							//if (mTestViewOpenMic.getStatus() == TestView.STATUS_SUCCEEDED && 
							//		mTestViewMuteMic.getStatus() == TestView.STATUS_SUCCEEDED ) {
								mIsTesting = true;
								mStreamingTest.pause();
								mTestViewCloseMic.setStatus(TestView.STATUS_TESTING, null);
								mVoiceCloseMicTest.start();
							//}
						}
						break;
				}
			}

			switch(event.getKeyCode()) {

				case KeyEvent.KEYCODE_BUTTON_10: // pinhole button
					mButtonTestResult[BUTTON_TEST_PAIRING] = Test.RESULT_SUCCEEDED;
					Log.d(TAG,"KeyEvent.KEYCODE_PAIRING::mTestViewButton1.setStatus(TestView.STATUS_SUCCEEDED, null);");
					mTestResult[TEST_BUTTON_1] = Test.RESULT_SUCCEEDED;
					checkButtonTestResult();

					return false;
				case KeyEvent.KEYCODE_F12: // mic mute button : released
					mButtonTestResult[BUTTON_TEST_MUTE] = Test.RESULT_SUCCEEDED;
					Log.d(TAG,"KeyEvent.KEYCODE_MUTE::mTestViewButton2.setStatus(TestView.STATUS_SUCCEEDED, null);");
					mTestResult[TEST_BUTTON_2] = Test.RESULT_SUCCEEDED;
					checkButtonTestResult();
					Log.d(TAG,"mTestResult[TEST_STREAMING] : " + mTestResult[TEST_STREAMING] + "(SUCCEEDED = " + Test.RESULT_SUCCEEDED + ")");
					if (mTestResult[TEST_STREAMING] == Test.RESULT_SUCCEEDED) {
						mStreamingTest.stop();
						mTestViewMuteMic.setStatus(TestView.STATUS_TESTING, null);
						mVoiceMuteMicTest.start();
					}
					return false;
				case KeyEvent.KEYCODE_BACK:
						return false;
			}
		} else if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch(event.getKeyCode()) {
				case KeyEvent.KEYCODE_F6: // pinhole button
					mTestViewButton1.setStatus(TestView.STATUS_READY, null);
					break;
				case KeyEvent.KEYCODE_F9: // mic mute button : pressed
					mTestViewButton2.setStatus(TestView.STATUS_READY, null);
					Log.d(TAG,"mTestResult[TEST_STREAMING] : " + mTestResult[TEST_STREAMING] + "(SUCCEEDED = " + Test.RESULT_SUCCEEDED + ")");
					if (mTestResult[TEST_STREAMING] == Test.RESULT_SUCCEEDED) {
						mStreamingTest.stop();
						mTestViewOpenMic.setStatus(TestView.STATUS_TESTING, null);
						mVoiceOpenMicTest.start();
					}
					break;
			}
		}

		return super.dispatchKeyEvent(event);
	}



	private void setWidget() {
		mSurfaceView = (SurfaceView) findViewById(R.id.surfaceViewStreaming);

		mTestViewStreaming = (TestView)findViewById(R.id.testViewManualStreaming);
		mTestViewButton1 = (TestView)findViewById(R.id.testViewManualButton1); //PAIRING
		mTestViewButton2 = (TestView)findViewById(R.id.testViewManualButton2); //TOP_MUTE

		mTestViewIR = (TestView)findViewById(R.id.testViewManualIRLed);
		mTestViewAudio = (TestView)findViewById(R.id.testViewManualAudio);
		mTestViewKeyPairing = (TestView)findViewById(R.id.testViewManualKeyPairing);
		mTestViewOtpLock = (TestView)findViewById(R.id.testViewManualOtpLock);
		mTestViewDbClient = (TestView)findViewById(R.id.testViewDbClient);

		mTestViewUsb1 = (TestView) findViewById(R.id.testViewAutoUsb1);
//		mTestViewUsb2 = (TestView) findViewById(R.id.testViewAutoUsb2);
		mTestViewWifi = (TestView) findViewById(R.id.testViewAutoWifi);
		mTestViewBluetooth = (TestView) findViewById(R.id.testViewAutoBluetooth);
		mTestViewHdmiEdid = (TestView) findViewById(R.id.testViewAutoHdmiEdid);
		mTestViewHdmiCec = (TestView) findViewById(R.id.testViewAutoHdmiCec);
		mTestViewEthernet = (TestView) findViewById(R.id.testViewAutoEthernet);
		mTestViewCpu = (TestView) findViewById(R.id.testViewAutoCpu);

		mTestViewWifi2G = (TestView) findViewById(R.id.testViewWifi2G);
		mTestViewWifi5G = (TestView) findViewById(R.id.testViewWifi5G);

		mTestViewOpenMic = (TestView) findViewById(R.id.testViewManualOpenMic);
		mTestViewMuteMic = (TestView) findViewById(R.id.testViewManualMuteMic);
		mTestViewCloseMic = (TestView) findViewById(R.id.testViewManualCloseMic);

		mTextViewModelName = (TextView) findViewById(R.id.textViewInfoModelName);
		mTextViewSwVersionService = (TextView) findViewById(R.id.textViewInfoSoftwareVersionService);
		mTextViewHwVersion = (TextView) findViewById(R.id.textViewInfoHardwareVersion);
		mTextViewFactoryVersion = (TextView) findViewById(R.id.textViewInfoFactoryVersion);
		mTextViewBluetoothMac = (TextView) findViewById(R.id.textViewInfoBluetoothMac);
		mTextViewSetIp = (TextView) findViewById(R.id.textViewInfoSetIp);
		mTextViewSerialNumber = (TextView) findViewById(R.id.textViewInfoSerialNumber);
		mTextViewRKEK = (TextView) findViewById(R.id.textViewInfoRKEK);
		mTextViewWifiMac = (TextView) findViewById(R.id.textViewInfoWifiMac);
		mTextViewPcbSerial = (TextView) findViewById(R.id.textViewInfoPcbSerial);
		mTextViewEthernetMac = (TextView) findViewById(R.id.textViewInfoEthernetMac);


		mTextViewTestResult = (TextView)findViewById(R.id.textViewTestResult);
		mTextViewTestLog = (TextView)findViewById(R.id.textViewLog);
	}



	private Bundle getConfigValues(int debug) {
		Log.d(TAG,"getConfigValues() - " + debug);
		Bundle configValues = null;

		File storageDirectory = new File(STORAGE_PATH);
		if(storageDirectory != null) {
			Log.d(TAG,"storageDirectory != null");
		} else {
			 Log.d(TAG,"storageDirectory = null");
		}

		if(storageDirectory.listFiles() == null) {
			Log.d(TAG,"storageDirectory.listFiles() == null");	
		}
			
		for(File file :storageDirectory.listFiles()) {
			Log.d(TAG,"file : " + file.getPath());
			if(file.isDirectory()) {
				String directoryName = file.getName();
				if(!directoryName.equals("emulated") && !directoryName.equals("self")) {
//					File configurationFile = new File(file.getPath()+"/"+CONFIGURATION_FILE_NAME);
					File configurationFile = new File((file.getPath().replace(STORAGE_PATH,STORAGE_PATH2))+"/"+CONFIGURATION_FILE_NAME);
					Log.d(TAG,"configurationFile : " + configurationFile.getPath());
					if(configurationFile.exists()) {
						if(configurationFile.canRead()) {
							try {
								configValues = new Bundle();

								FileInputStream fileInputStream = new FileInputStream(configurationFile);
								XmlPullParser xmlPullParser = XmlPullParserFactory.newInstance().newPullParser();
								xmlPullParser.setInput(fileInputStream, "UTF-8");

								int eventType = xmlPullParser.getEventType();
								String name = null;
								while (eventType != XmlPullParser.END_DOCUMENT) {
									switch (eventType) {
										case XmlPullParser.START_TAG:
											name = xmlPullParser.getName();
											break;
										case XmlPullParser.END_TAG:
											name = null;
											break;
										case XmlPullParser.TEXT:
											if(name != null) {
												String text = xmlPullParser.getText();
												configValues.putString(name, text);
											}
											break;
									}
									eventType = xmlPullParser.next();
								}

								fileInputStream.close();

							} catch (XmlPullParserException e) {
								Log.d(TAG,"getConfigValues::XmlPullParserException");
								e.printStackTrace();
							} catch (FileNotFoundException e) {
								Log.d(TAG,"getConfigValues::FileNotFoundException");
								e.printStackTrace();
							} catch (IOException e) {
								Log.d(TAG,"getConfigValues::IOException");
								e.printStackTrace();
							}
						} else {
							Log.d(TAG,"getConfigValues::configurationFile.canRead() = fail");
						}
					} else {
						Log.d(TAG,"getConfigValues::configurationFile.exists() = fail");
					}
				}
			}
		}
		if(configValues == null) {
			Log.d(TAG,"getConfigValues ret = null");
		} else {
			Log.d(TAG,"getConfigValues success");
		}
		return configValues;
	}

	private int getLogEnable(Bundle config) {
		String enable = "0";
		if(config != null) {
			try {
				enable = config.getString(CONFIG_TAG_LOG_ENABLE);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return Integer.parseInt(enable);
	}

	private int getTestmode(Bundle config) {
	  String enable = "0";
	  if(config != null) {
		try {
		  enable = config.getString(CONFIG_TAG_TEST_MODE);
		} catch (NumberFormatException e) {
		  e.printStackTrace();
		}
	  }
	  if(enable != null)
		  return Integer.parseInt(enable);
	  else
	  	return 0;
	}
 	
	private String getStreamingUrl(Bundle config) {
		String url = "";
		if(config != null) {
			url = config.getString(CONFIG_TAG_LOG_ENABLE);
		}
		return url;

	}

	private String getFactoryStep(Bundle config) {
		if ( config == null ) {
			LOG("setFactoryStep : config is null");
			return STEP_1ST;
		}

		String step = config.getString(CONFIG_TAG_FACTORY_STEP);
		if ( step == null ) {
			LOG("setFactoryStep : need TAG value (factory_step)");
			step = STEP_1ST;
		}

		return step;
	}
	
	private void setFactoryStep(Bundle config) {
		String step = getFactoryStep(config);
		LOG("setFactoryStep : " + step);

		if ( step.equals(STEP_2ND) ) {
			mTestViewCloseMic.setVisibility(View.VISIBLE);
			mTestViewOpenMic.setVisibility(View.VISIBLE);
			mTestViewMuteMic.setVisibility(View.VISIBLE);
			//mTestViewDbClient.setVisibility(View.INVISIBLE);
			mTestViewKeyPairing.setVisibility(View.INVISIBLE);
			mTestViewOtpLock.setVisibility(View.INVISIBLE);
		} 
		else 
		{
			mTestViewMuteMic.setVisibility(View.VISIBLE);
		}
	}

	private void setIpAddress(Bundle config) {
		if(config != null) {
			String ipAddress = config.getString(CONFIG_TAG_ETHERNET_IP);
			int prefixLength = -1;
			try{
				prefixLength = Integer.parseInt(config.getString(CONFIG_TAG_ETHERNET_PREFIX_LENGTH));
			} catch (NumberFormatException e) {
				e.printStackTrace();	
			}

			LOG("ipAddress : " + ipAddress+ " prefixLength :"+prefixLength);
						
			if(ipAddress != null && prefixLength != -1 && mIpConfiguration != null ) {
				IpConfiguration ipConfiguration = mIpConfiguration;
				//IpConfiguration ipConfiguration = mEthernetManager.getConfiguration("eth0");

				ipConfiguration.setIpAssignment(IpConfiguration.IpAssignment.STATIC);

				StaticIpConfiguration.Builder builder = new StaticIpConfiguration.Builder();
				StaticIpConfiguration staticIpConfiguration = builder.setIpAddress(new LinkAddress((Inet4Address) InetAddresses.parseNumericAddress(ipAddress), prefixLength)).build();

				ipConfiguration.setStaticIpConfiguration(staticIpConfiguration);

				//mEthernetManager.setConfiguration("eth0", ipConfiguration);
				EthernetNetworkUpdateRequest request = new EthernetNetworkUpdateRequest.Builder().setIpConfiguration(ipConfiguration).build();
				mEthernetManager.updateConfiguration("eth0", request, r->r.run(), null);
			}
		}
	}


	private String getDTSerialNumber() {
		String prevSerialNo = SystemProperties.get("ro.serialno", "0000000000000000");
		if ( prevSerialNo.equals("0000000000000000") ||
				!checkSerial(prevSerialNo) )
			return null;

		return prevSerialNo;
	}

	private void setInformation(){
		String prevSerialNo = getDTSerialNumber();
		if ( (prevSerialNo != null) && !(prevSerialNo.isEmpty()) ) {
			mTextViewModelName.setBackgroundColor(Color.YELLOW);
			mTextViewSwVersionService.setBackgroundColor(Color.YELLOW);
			mTextViewHwVersion.setBackgroundColor(Color.YELLOW);
			mTextViewFactoryVersion.setBackgroundColor(Color.YELLOW);
			mTextViewBluetoothMac.setBackgroundColor(Color.YELLOW);
			mTextViewSetIp.setBackgroundColor(Color.YELLOW);
			mTextViewSerialNumber.setBackgroundColor(Color.YELLOW);
			mTextViewRKEK.setBackgroundColor(Color.YELLOW);
			mTextViewWifiMac.setBackgroundColor(Color.YELLOW);
			mTextViewPcbSerial.setBackgroundColor(Color.YELLOW);
			mTextViewEthernetMac.setBackgroundColor(Color.YELLOW);
		}

		mTextViewModelName.setText("Model Name  : " + Settings.Global.getString(this.getContentResolver(), Settings.Global.DEVICE_NAME));
		mTextViewSwVersionService.setText("SW Version(Service) : " + SystemProperties.get("ro.inno.factory.service.version"));
		mTextViewHwVersion.setText("HW Version  : " + SystemProperties.get("ro.oem.hw.version"));
		mTextViewFactoryVersion.setText("Factory Version : " + SystemProperties.get("ro.inno.factory.version"));

		String btmac = getProvisionedData(CMD_RECEIVED_BT_MAC_ADDRESS);
		mTextViewBluetoothMac.setText("Bluetooth MAC : " + (btmac == null ? "" : btmac));

		String ipaddr = getIpAddress();
		mTextViewSetIp.setText("Set IP : " + (ipaddr == null ? "" : ipaddr));

		String DTSerialNo = getProvisionedData(CMD_RECEIVED_DT_SERIAL);
		mTextViewSerialNumber.setText("Serial NO. : " + (DTSerialNo == null ? "" : DTSerialNo));

		String RKEK = getProvisionedData(CMD_RECEIVED_RKEK_ID);
		mTextViewRKEK.setText("RKEK ID : " + (RKEK == null ? "" : RKEK));

		String Wifimac = getProvisionedData(CMD_RECEIVED_WIFI_MAC_ADDRESS);
		mTextViewWifiMac.setText("Wi-Fi MAC : " + (Wifimac == null ? "" : Wifimac));

		String Ethmac = getProvisionedData(CMD_RECEIVED_ETHERNET_MAC_ADDRESS);
		mTextViewEthernetMac.setText("Ethernet MAC : " + (Ethmac == null ? "" : Ethmac));

		String PcbSerialNo = getProvisionedData(CMD_RECEIVED_PCB_SERIAL);
		mTextViewPcbSerial.setText("PCB Serial No : " + (PcbSerialNo == null ? "" : PcbSerialNo));

		Log.d(TAG,"====================================================================================================");
		Log.d(TAG,"Board Information");
		Log.d(TAG,"Model Name : " + Settings.Global.getString(this.getContentResolver(), Settings.Global.DEVICE_NAME));
		Log.d(TAG,"FactoryTools Version : " + getFactoryVersion());
		Log.d(TAG,"Service Image Version : " + SystemProperties.get("ro.inno.factory.service.version"));
		Log.d(TAG,"Factory Image Version : " + SystemProperties.get("ro.inno.factory.version"));
		Log.d(TAG,"HardWare Version : " + SystemProperties.get("ro.oem.hw.version"));
		Log.d(TAG,"EMMC Vendor : " + SystemProperties.get("ro.runtime.innopia.flash"));
		Log.d(TAG,"RKEK : " + SystemProperties.get("ro.rkek_id"));
		Log.d(TAG,"====================================================================================================");



	}

	

	private void setTest() {
		Log.d(TAG,"setTest()");
/*		mTestResult = new int[12];
		for(int i = 0 ; i < 12 ; i++) {
			mTestResult[i] = 3;
		}
		mTestResultExtra = new Bundle[12];

		for(int i=0; i<12; i++) {
			mTestResult[i] = Test.RESULT_UNKNOWN;
			mTestResultExtra[i] = null;
		}
*/
		mProvisionResultCsr = Test.RESULT_UNKNOWN;
		mProvisionResultApp = Test.RESULT_UNKNOWN;
		mProvisionResultEmmcOtp = Test.RESULT_UNKNOWN;

		if(mStreamingTest == null) {
			mSurfaceView = (SurfaceView) findViewById(R.id.surfaceViewStreaming);
			mStreamingTest = new StreamingTest(this, mSurfaceView.getHolder(), mConfigValues);
			mStreamingTest.setOnResultListener(mOnResultListener);
		}


		if(mVoiceOpenMicTest == null) {
			mVoiceOpenMicTest = new VoiceMicTest(this, VoiceMicTest.TEST_TYPE_OPEN, mConfigValues);
			mVoiceOpenMicTest.setOnResultListener(mOnResultListener);
		}

		if(mVoiceMuteMicTest == null) {
			mVoiceMuteMicTest = new VoiceMicTest(this, VoiceMicTest.TEST_TYPE_MUTE, mConfigValues);
			mVoiceMuteMicTest.setOnResultListener(mOnResultListener);
		}

		if(mVoiceCloseMicTest == null) {
			mVoiceCloseMicTest = new VoiceMicTest(this, VoiceMicTest.TEST_TYPE_CLOSE, mConfigValues);
			mVoiceCloseMicTest.setOnResultListener(mOnResultListener);
		}

		if(mBluetoothTest == null) {
			mBluetoothTest = new BluetoothTest(this, mConfigValues);
			mBluetoothTest.setOnResultListener(mOnResultListener);
		}

		if(mEthernetTest == null) {
			mEthernetTest = new EthernetTest(this, mConfigValues);
			mEthernetTest.setOnResultListener(mOnResultListener);
		}

		if(mWifiTest == null) {
			mWifiTest = new WifiTest(this, mConfigValues);
			mWifiTest.setOnResultListener(mOnResultListener);
			mWifiTest.setTestFrequency(WifiTest.FREQUENCY_ALL);
		}
		
		if(mCpuTest == null) {
			mCpuTest = new CpuTest(this, mConfigValues);
			mCpuTest.setOnResultListener(mOnResultListener);
		}

		if(mUsbTest == null) {
			SystemProperties.set("persist.sys.usb.configfs", "1");
			mUsbTest = new UsbTest(this);
			mUsbTest.setOnResultListener(mOnResultListener);
		}

		if(mHdmiTest == null) {
			mHdmiTest = new HdmiTest(this);
			mHdmiTest.setOnResultListener(mOnResultListener);
		}


		if(mProvision == null) {
			mProvision = new Provision(this);
			mProvision.setOnResultListener(mOnResultListener);
		}
	//	initWifiTest();
	//	initButtonTest();
	//		startAutoTest();

	}



	private void checkTestResult() {
		boolean isCompleted = true;
		boolean isSucceeded = true;
		int MAX_INDEX = getFactoryStep(mConfigValues).equals(STEP_1ST) ? TEST_IR_LED + 1 : TEST_MAX_INDEX;

		for(int i=0; i<MAX_INDEX; i++) {
			if ( i == TEST_AUDIO ) 
				continue;

			//LOG("Result (" + i +") : " +mTestResult[i]); 
			if(mTestResult[i] == Test.RESULT_UNKNOWN) {
				isCompleted = false;
				break;
			} else {
				if(mTestResult[i] == Test.RESULT_FAILED) {
					isSucceeded = false;
					break;
				}
			}
		}

		if ( mTestResult[TEST_MIC_MUTE] == Test.RESULT_UNKNOWN ) 
			isCompleted = false;
		else if ( mTestResult[TEST_MIC_MUTE] == Test.RESULT_FAILED ) 
			isSucceeded = false;

		if(isCompleted) {
		
			if(isSucceeded) {
				mIsAllSucceeded = true;

				if(mTcpServer == null && !isFailedOTP ) {					
					Log.d(TAG, "Start TCP Server...");
					mTcpServer = new TCPServer(MainActivity.this);
					mTcpServer.setSocketListener(mSocketListener);
					mTcpServer.connect();


					if(mConfigValues == null) mConfigValues = getConfigValues(3);
					if(getLogEnable(mConfigValues) > 0 ){
						if(mLogTcpServer == null) {
							mLogTcpServer = new LogTCPServer(MainActivity.this, mConfigValues);
							mLogTcpServer.connect();
						}
					}
		
					mKeyPairingTryCount = 0;
					mTestViewKeyPairing.setStatus(TestView.STATUS_READY, null);
					setTestViewDbClient(TestView.STATUS_TESTING, null);
				}
			}
		} 
	}



	private void initButtonTest() {
		mButtonTestResult = new int[2];

		for(int i=0; i<2; i++) {
			mButtonTestResult[i] = Test.RESULT_UNKNOWN;
		}
	}


	private void checkButtonTestResult() {
		if(mButtonTestResult[BUTTON_TEST_PAIRING] == Test.RESULT_SUCCEEDED) {
			mTestViewButton1.setStatus(TestView.STATUS_SUCCEEDED, null);
		}

		if(mButtonTestResult[BUTTON_TEST_MUTE] == Test.RESULT_SUCCEEDED) {
			mTestViewButton2.setStatus(TestView.STATUS_SUCCEEDED, null);
		}

		checkTestResult();
	}


	private void checkIRTestResult() {
		switch(mIRCount % 3) {
			case 2:
				setLedColor("blue");
				break;
			case 0:
				setLedColor("green");
				break;
			case 1:
				setLedColor("red");
			break;
		}
		mIRCount++;
		if(mIRCount >= 3) {
			mTestResult[TEST_IR_LED] = Test.RESULT_SUCCEEDED;
			mTestViewIR.setStatus(TestView.STATUS_SUCCEEDED,""+mIRCount);
			mIsTesting = false;
			checkTestResult();
		} else {
			mTestViewIR.setStatus(TestView.STATUS_TESTING,""+mIRCount);
		}
	}



	private void initWifiTest() {

//		mTestResult[TEST_WIFI_2G] = Test.RESULT_UNKNOWN;
//		mTestResult[TEST_WIFI_5G] = Test.RESULT_UNKNOWN;

	//	mTestResultExtra[TEST_WIFI_2G] = null;
	//	mTestResultExtra[TEST_WIFI_5G] = null;

		mTestViewWifi2G.setStatus(TestView.STATUS_UNKNOWN, null);
		mTestViewWifi5G.setStatus(TestView.STATUS_UNKNOWN, null);
		mTestViewWifi.setStatus(TestView.STATUS_UNKNOWN, null);
	}


	private void checkWifiTestResult() {
		if( mTestResult[TEST_WIFI_5G] == Test.RESULT_SUCCEEDED) {
			if( mTestResult[TEST_WIFI_2G] == Test.RESULT_SUCCEEDED) {
				mTestViewWifi.setStatus(TestView.STATUS_SUCCEEDED);
				if(mTestViewBluetooth.getStatus() == TestView.STATUS_FAILED) { 
					setLedColor("red_blink");
				} else if( mTestViewBluetooth.getStatus() == TestView.STATUS_SUCCEEDED) { 
					setLedColor("blue_blink"); 
				}
			} else if (mTestResult[TEST_WIFI_2G] == Test.RESULT_UNKNOWN) {
				mTestViewWifi.setStatus(TestView.STATUS_TESTING);
			} else if (mTestResult[TEST_WIFI_2G] == Test.RESULT_FAILED){
				mTestViewWifi.setStatus(TestView.STATUS_FAILED);
				setLedColor("red_blink");
			}
		} else {
			LOG("checkWifiTestResult : fail");
			mTestViewWifi.setStatus(TestView.STATUS_FAILED);
			setLedColor("red_blink");
		}

	}

	private void startWifiTest() {
		mTestViewWifi2G.setStatus(TestView.STATUS_TESTING, null);
		mTestViewWifi5G.setStatus(TestView.STATUS_TESTING, null);
		mTestViewWifi.setStatus(TestView.STATUS_TESTING, null);
		mWifiTest.setTestFrequency(WifiTest.FREQUENCY_ALL);
		mWifiTest.start();

	}

	private void setKeyPairingResult(String result) {
		SharedPreferences prefKeyPairingResult = getSharedPreferences("key_pairing_result", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefKeyPairingResult.edit();
		editor.putString("result", result);
		editor.commit();
		executeCommand(CMD_SYNC);
		
	}

	private boolean checkOtpLock() {
		File otpLockSuccessFile = new File(OTP_LOCK_SUCCESS_FILE_PATH);

		return otpLockSuccessFile.exists();
	}



	private boolean checkInitImage() {
		File initImageFile = new File(INIT_IMAGE_FILE_PATH);

		return initImageFile.exists();
	}


	private void startAutoTest() {
		Log.d(TAG,"startAutoTest mIsStartAutoTest : " + mIsStartAutoTest + " , ");
		if(mIsStartAutoTest) return;

		setLedColor("green_blink");

		mBluetoothTest.start();
		mEthernetTest.start();
		mWifiTest.start();
		mCpuTest.start();
		mUsbTest.start();
		mHdmiTest.start();

		mTestViewBluetooth.setStatus(TestView.STATUS_TESTING, null);
		mTestViewWifi.setStatus(TestView.STATUS_TESTING, null);
		mTestViewWifi5G.setStatus(TestView.STATUS_TESTING, null);

		mIsStartAutoTest = true;

	}



	private void stopTest() {
		mStreamingTest.stop();
//		if(!(mTestViewBluetooth.getStatus() == TestView.STATUS_UNKNOWN) && !(mTestViewBluetooth.getStatus() == TestView.STATUS_FAILED)) {
//			mBluetoothTest.stop();
//		}
//		if(!(mTestViewEthernet.getStatus() == TestView.STATUS_UNKNOWN) && !(mTestViewEthernet.getStatus() == TestView.STATUS_FAILED)) {
//			mEthernetTest.stop();
//		}
//		if(!(mTestViewWifi.getStatus() == TestView.STATUS_UNKNOWN) && !(mTestViewWifi.getStatus() == TestView.STATUS_FAILED)) {
//			mWifiTest.stop();
//		}
//		mCpuTest.stop();
		mUsbTest.stop();
//		mHdmiTest.stop();

	}



	private String getFactoryVersion() {
		return SystemProperties.get("ro.inno.factory.version");
		/*
		String factoryVersion = "";
		try {
			factoryVersion= ((PackageInfo)getPackageManager().getPackageInfo(getPackageName(), 0)).versionName;
		}catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return factoryVersion;
		*/
	}



	private String getIpAddress() {
		ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		LinkProperties linkProperties = connectivityManager.getLinkProperties(connectivityManager.getActiveNetwork());
		String ipAddress = null;
		if(linkProperties != null) {
			List<LinkAddress> linkAddresses = linkProperties.getLinkAddresses();

			if (linkAddresses != null) {
				for (LinkAddress linkAddress : linkAddresses) {
					if ((linkAddress.getAddress()) instanceof Inet4Address) {
						ipAddress = linkAddress.getAddress().getHostAddress();
					}
				}
			}
		}

		return ipAddress;
	}



	private String getMacAddress(String cmd) {
		String macAddress = null;

		try {
			Process process = Runtime.getRuntime().exec(cmd);

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			macAddress = bufferedReader.readLine();
			bufferedReader.close();

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			return macAddress;
		}
	}


	private String getProvisionedData(String cmd) {
		String data = null;
		try {
			Process process = Runtime.getRuntime().exec(cmd);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			data = bufferedReader.readLine();
			bufferedReader.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		} finally {
			return data;
		}
	}

	private void executeCommand (String cmd) {
		try {
			Process process = Runtime.getRuntime().exec(cmd);

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			bufferedReader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	private void setTestViewDbClient(int status, String phase){
		mTestViewDbClient.setStatus(status, phase);
	}

	private void setTestViewDbClient(int status, String phase , boolean needUiThread){
		mTextViewTestResult.setText("");
		mTestViewDbClient.setStatus(status, phase, needUiThread);
	}


	
	private void setSendResult(int result) {
		if(result == 0) {
			//mTextViewTestResult.setText("Send Msg to Server");
		} else {
			mTextViewTestResult.setText("Failure Send Msg");
		}
	}

	private void setTestResult(String msg, boolean error) {
		if ( mTextViewTestResult != null ) {
			mTextViewTestResult.setTextColor(Color.WHITE);
			mTextViewTestResult.setText(msg);
			if ( error ) {
				mTextViewTestResult.setBackgroundColor(Color.RED);
			} else {
				mTextViewTestResult.setBackgroundColor(Color.GRAY);
			}
		}
	}


	
	private void setVolume(Bundle config) {
		if(config != null) {
			String audioVolume = config.getString(CONFIG_TAG_AUDIO_VOLUME);
			
			if(audioVolume != null) {
				int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

				if(Integer.parseInt(audioVolume) != currentVolume) {
					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Integer.parseInt(audioVolume), AudioManager.FLAG_SHOW_UI);
				}
			}
		}
	}

	private boolean isMuteMic() {
		if ( is_old_hw_version ) 
			return false;

		final String MIC_STATUS = "/sys/class/leds/mic_led/mic_gpio";
		//final String MIC_STATUS = "/sys/class/gpio/gpio395/value";
		String value = readFile(MIC_STATUS);
		if ( TextUtils.isEmpty(value) ) 
			return false;

		return (value.trim().equals("0"));
	}


	private void createFile(String filePath, byte[] data) {

		File file = new File(filePath);

		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(data);
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	private void createFile(String filePath, int data) {

	        File file = new File(filePath);
			try {
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(data);
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	

	private String readFile(String filePath) {
		File file = new File(filePath);

		StringBuilder stringBuilder = new StringBuilder();

		if(file.exists()) {
			if(file.canRead()) {
				try {
					BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
					String line;
			
					while((line = bufferedReader.readLine()) != null) {
						stringBuilder.append(line);
						stringBuilder.append("\n");			
					}

					bufferedReader.close();					
			
				} catch (IOException e ) {
					LOG("readFile IOException");
					e.printStackTrace();
				}
			} else {
				LOG("File cannot be read");
			}
		} else {
			LOG("File does not exist");
		}

		return stringBuilder.toString();
	}


	public File getMediaRwStorage() {
//		File file = new File("/mnt/media_rw/");
		String path;
		File file = new File("/storage/");
		file.setWritable(Boolean.TRUE);
		if (file != null && file.exists() && file.isDirectory()) {
			Map<String, Object> maptmp;
			boolean skipFlag = false;
			File[] fileList = file.listFiles();
			if (fileList != null && fileList.length > 0) {
				for (File filetmp : fileList) {
					if(filetmp.getPath().contains("emulated") || filetmp.getPath().contains("self")) continue;
	//				Toast.makeText(getApplicationContext(),"copy ResultFile to "+filetmp.getPath() , Toast.LENGTH_LONG).show();
					return filetmp;
				}
			} else {
//				if (fileList == null) Toast.makeText(getApplicationContext(),"errorcode : 1 , Usb Drive is 0." , Toast.LENGTH_LONG).show();
//				else if(!(fileList.length > 0)) Toast.makeText(getApplicationContext(),"errorcode 2 , Usb Drive is 0." , Toast.LENGTH_LONG).show();
			}																																																												  
		}			
//		else if(file == null) Toast.makeText(getApplicationContext(),"errorcode 3 , Usb Drive is 0." , Toast.LENGTH_LONG).show();
//		else if(!file.exists()) Toast.makeText(getApplicationContext(),"errorcode 4 , Usb Drive is 0." , Toast.LENGTH_LONG).show();
//		else if(!file.isDirectory()) Toast.makeText(getApplicationContext(),"errorcode 5 , Usb Drive is 0." , Toast.LENGTH_LONG).show();
		return null;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(TAG,"onConfigurationChanged : " + newConfig.toString());

		if (newConfig.keyboardHidden == Configuration.KEYBOARDHIDDEN_YES) {
			Toast.makeText(this, "Keyboard available", Toast.LENGTH_SHORT).show();
		} else if (newConfig.keyboardHidden == Configuration.KEYBOARDHIDDEN_NO){
			Toast.makeText(this, "No keyboard", Toast.LENGTH_SHORT).show();
		}
	}

	private void stopTcpServer() {
		msleep(1000);
		if ( mTcpServer != null ) {
			Log.d(TAG, "stopTcpServer() Stop TCP Server...");
			mTcpServer.stop();
			mTcpServer = null;
		}
	}

	private void msleep(long ms) {
		try { 
			Thread.sleep(ms);
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
	}

	private boolean checkSerial(String serial) {
		final String PCBSN_REGEX = "^9222[0-9A-Za-z]";
		if ( serial != null ) {
			if ( serial.length() == 20 && serial.matches(PCBSN_REGEX + "{16}$") ) return true;
			if ( serial.length() == 16 && serial.matches(PCBSN_REGEX + "{12}$") ) return true;
		}
		return false;
	}

	private boolean checkPcbSn(String key) {
        final String PCBSN_REGEX = "^[0-9A-Za-z]{16}$";
        return ((key != null) ? key.matches(PCBSN_REGEX) : false);
    }

	private boolean checkMac(String mac) {
        final String MAC_REGEX = "^([0-9A-Fa-f]{2}:){5}([0-9A-Fa-f]{2})$";
        return ((mac != null) ? mac.matches(MAC_REGEX) : false);
    }

	private boolean setWipeData() {
		final String WIPE_DATA_PROP		= "inno.factory.wipe_data";
		final int	 WIPE_DATA_RETRY	= 10;
		final int 	 WIPE_DATA_SLEEP_MS = 200;

		String status;
		boolean result = false;
		int count = 0;

		// set wipe-data
		SystemProperties.set(WIPE_DATA_PROP, "start");

		// get setting status
		while ( true ) {
			status = SystemProperties.get(WIPE_DATA_PROP, "x");
			if ( !status.isEmpty() && status.equals("FDR_NO_REBOOT_OK") ) {
				Log.d(TAG, "setWipeData() Setting to erase all data on the next boot has been completed!");
				result = true;
				break;
			}

			if ( count++ > WIPE_DATA_RETRY ) {
				Log.e(TAG, "setWipeData() Timeout! Failed to get status of wipe-data setting.");
				return false;
			}

			Log.d(TAG, "setWipeData() Waiting for setting wipe-data to complete." + 
					" (status=" + status + ", " + count + "/" + WIPE_DATA_RETRY + ")");
			msleep(WIPE_DATA_SLEEP_MS);
		}

		return true;
	}

	private boolean switchBootSlot() {
		String cmd, line = null;
		boolean result = false;

		cmd = "bootctl set-slot-as-unbootable 0";
        Log.i(TAG, "switchBootSlot() run command: " + cmd);
        try {
            Process ps = Runtime.getRuntime().exec(cmd);
            ps.waitFor(); //destroy();
            int ret = ps.exitValue();
            Log.d(TAG, "switchBootSlot() command exited with " + ret);
            if ( ret != 0 ) {
                Log.e(TAG, "switchBootSlot() Failed to run '" + cmd + "' with err: " + ret);
                return false;
            }
        } catch ( Exception e ) {
            Log.e(TAG, "switchBootSlot() Failed to run '" + cmd + "' with err: " + e.toString());
            e.printStackTrace();
            return false;
        }

		cmd = "bootctl set-active-boot-slot 1";
		try {
			Process ps = Runtime.getRuntime().exec(cmd);
			ps.waitFor();
		} catch ( Exception e ) {
			Log.e(TAG, "switchBootSlot() Failed to run command: '" + cmd + "', err: " + e.toString());
			e.printStackTrace();
			return false;
		}

		cmd = "bootctl get-active-boot-slot";
		try {
			Process ps = Runtime.getRuntime().exec(cmd);
			BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			while ( (line = br.readLine()) != null ) {
				if ( line.isEmpty() ) continue;
				if ( line.equals("1") ) {
					Log.d(TAG, "switchBoot() Boot slot has been set to B successfully!");
					result = true;
					break;
				}
			}
			br.close();
			ps.destroy();
		} catch ( Exception e ) {
			Log.e(TAG, "switchBootSlot() Failed to run command: " + cmd + ", err: " + e.toString());
			e.printStackTrace();
		}

		return result;
	}

	private void setFactoryComplete(String msg) {
		mTextViewModelName.setTextColor(Color.WHITE);
		mTextViewModelName.setBackgroundColor(Color.BLUE);
		mTextViewSwVersionService.setTextColor(Color.WHITE);
		mTextViewSwVersionService.setBackgroundColor(Color.BLUE);
		mTextViewHwVersion.setTextColor(Color.WHITE);
		mTextViewHwVersion.setBackgroundColor(Color.BLUE);
		mTextViewFactoryVersion.setTextColor(Color.WHITE);
		mTextViewFactoryVersion.setBackgroundColor(Color.BLUE);
		mTextViewBluetoothMac.setTextColor(Color.WHITE);
		mTextViewBluetoothMac.setBackgroundColor(Color.BLUE);
		mTextViewSetIp.setTextColor(Color.WHITE);
		mTextViewSetIp.setBackgroundColor(Color.BLUE);
		mTextViewSerialNumber.setTextColor(Color.WHITE);
		mTextViewSerialNumber.setBackgroundColor(Color.BLUE);
		mTextViewRKEK.setTextColor(Color.WHITE);
		mTextViewRKEK.setBackgroundColor(Color.BLUE);
		mTextViewWifiMac.setTextColor(Color.WHITE);
		mTextViewWifiMac.setBackgroundColor(Color.BLUE);
		mTextViewPcbSerial.setTextColor(Color.WHITE);
		mTextViewPcbSerial.setBackgroundColor(Color.BLUE);
		mTextViewEthernetMac.setTextColor(Color.WHITE);
		mTextViewEthernetMac.setBackgroundColor(Color.BLUE);

		mTextViewTestResult.setText(msg);
		if ( mTextViewTestResult != null ) {
			mTextViewTestResult.setBackgroundColor(Color.BLUE);
		}


	}

	private boolean setFactoryEnd() {
		boolean ret;
		String result;

		Log.d(TAG, "setFactoryEnd() Start finishing the factory process...");

		// factory reset
		Log.d(TAG, "setFactoryEnd() *");
		Log.d(TAG, "setFactoryEnd() * Set wipe-data...");
		Log.d(TAG, "setFactoryEnd() *");
		ret = setWipeData();
		if ( !ret ) {
			Log.d(TAG, "setFactoryEnd() Failed to set wipe-data.");
			return false;
		}

		// factory reset
		Log.d(TAG, "setFactoryEnd() *");
		Log.d(TAG, "setFactoryEnd() * Switch boot slot to B...");
		Log.d(TAG, "setFactoryEnd() *");
		ret = switchBootSlot();
		if ( !ret ) {
			Log.d(TAG, "setFactoryEnd() Failed to switch boot slot.");
			return false;
		}

		Log.d(TAG, "setFactoryEnd() *");
		Log.d(TAG, "setFactoryEnd() * Completed the Factory Process!!!");
		Log.d(TAG, "setFactoryEnd() *");

		return true;
	}


	class copyTask extends AsyncTask<Void, Long, Boolean> {
		String f_file, t_file;
		File file;
		long filesize;
		InputStream input;
		OutputStream output;
		long count;
		
		public copyTask(){
			File fromFile = new File("/data/ret_otp");
			f_file = "/data/ret_otp";
			filesize = getFileSize(f_file);
			file = new File(getMediaRwStorage(),"ret_otp");

			try {
				t_file = file.getPath();
				file.createNewFile();
				file.setWritable(Boolean.TRUE);
				Log.i("@@@@",file.getPath());
			} catch (IOException e) {
				Log.i("@@@@",e.toString());
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			// TODO Auto-generated method stub
			try {
//				disableKeyEvent();
				input = new FileInputStream(new File(f_file));
				output = new FileOutputStream(file);
				byte[] buffer = new byte[1024];
				boolean n = false;
				long percent = 0;
				int n1;
				for(count = 0L; -1 != (n1 = input.read(buffer)); count += (long)n1) {
					output.write(buffer, 0, n1);
					if ((count * 100)/filesize > percent) {
						percent = (count * 100) / filesize;
						publishProgress(percent);
						Thread.sleep(100);
					}
				}
				publishProgress(100L);
				output.flush();
			} catch(Exception ex){
				ex.printStackTrace();
				return false;
			}
			return true;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			count = 0;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if(result) {
				Toast.makeText(getApplicationContext(),"Success to Copy report file.." , Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getApplicationContext(),"Fail to Copy file.." , Toast.LENGTH_LONG).show();
				try{
					input.close();
					input = null;
					output.close();
					output = null;
				} catch ( Exception e ) {
					e.printStackTrace();
				}
				this.cancel(true);
				count = -1L;
			//	finish();
			}
		}

		@Override
		protected void onProgressUpdate(Long... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values[0]);
	//		pb.setProgress((int)(long)values[0]);
		}

		public long getFileSize(String filename) {
			File file = new File(filename);
			if(file.exists()) {
				return file.length();
			} else {
				return -1;
			}
		}
	}

	public void copyFile_otp(){
		if(getMediaRwStorage() == null) return;
		String path = getMediaRwStorage().getPath();
		try{
			Runtime.getRuntime().exec("cp /data/ret_otp " + path);
			Toast.makeText(getApplicationContext(),"copy Logfile into USB Drive." , Toast.LENGTH_LONG).show();
		} catch(Exception e) { 
			e.printStackTrace(); 
		}
	}

	public void copyFile_emmc(){
		if(getMediaRwStorage() == null) return;
		String path = getMediaRwStorage().getPath();
		try{
			Runtime.getRuntime().exec("cp /data/ret_emmc " + path);
			Toast.makeText(getApplicationContext(),"copy Logfile into USB Drive." , Toast.LENGTH_LONG).show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void copyFile_app(){
		if(getMediaRwStorage() == null) return;
		String path = getMediaRwStorage().getPath();
		try{
			Runtime.getRuntime().exec("cp /data/ret_app " + path);
			Toast.makeText(getApplicationContext(),"copy Logfile into USB Drive." , Toast.LENGTH_LONG).show();
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	private String getLogcatLog() { 
		StringBuilder log = new StringBuilder(); 
		try { 
			Process process = Runtime.getRuntime().exec("logcat "); 
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream())); 
			String line = ""; 
			while ((line = bufferedReader.readLine()) != null) { 
				log.append(line);
				log.append("\n");
			} 
		} catch (IOException e) { 
			Log.d(TAG,e.toString());
		} 
		return log.toString();
	}

	

	private void sendRetLog(int ret_log , int codeLine ) {
		StringBuilder log = new StringBuilder();
		String mTag = "";
		try {
			String cmd = "";
			switch(ret_log) {
				case 0 :
					cmd = "cat /data/ret_app";
					mTag = "ret_app";
					break;
				case 1 :
					cmd = "cat /data/ret_emmc";
					mTag = "ret_emmc";
					break;
				case 2 :
					cmd = "cat /data/ret_otp";
					mTag = "ret_otp";
					break;
				case 3 :
					break;
			}
			Process process = Runtime.getRuntime().exec(cmd);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//			Log.d("innofactory","sendLog_ret_otp::cat /data/ret_otp");
			String line = "";
			while ((line = bufferedReader.readLine()) != null) {
				Log.d(mTag +":"+ codeLine,line +"\n");
			}
		} catch (IOException e) {
			Log.d(TAG,e.toString());
		}
	}

	private void sendRetLog(int ret_log) {
		StringBuilder log = new StringBuilder();
		String mTag = "";
		try {
			String cmd = "";
			switch(ret_log) {
				case 0 :
					cmd = "cat /data/ret_app";
					mTag = "ret_app";
					break;
				case 1 :
					cmd = "cat /data/ret_emmc";
					mTag = "ret_emmc";
					break;
				case 2 :
					cmd = "cat /data/ret_otp";
					mTag = "ret_otp";
					break;
				case 3 :
					break;
			}
			Process process = Runtime.getRuntime().exec(cmd);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			//      Log.d("innofactory","sendLog_ret_otp::cat /data/ret_otp");
			String line = "";
			while ((line = bufferedReader.readLine()) != null) {
				Log.d(mTag,line +"\n");
			}
		} catch (IOException e) {
			Log.d(TAG,e.toString());
		}

	}

	public void sendRetLogAll(int codeLine) {
		sendRetLog(0,codeLine);
		sendRetLog(1,codeLine);
		sendRetLog(2,codeLine);
		sendRetLog(3,codeLine);
	}


	public void getLogOnUsb() {
		LOG("getLogOnUsb::Build.getSerial() : " +Build.getSerial());
		if(Build.getSerial() != null) {
			if(getMediaRwStorage() == null) return;
			String cmd = "logcat -f "+ getMediaRwStorage() +"/"+ Build.getSerial() + " AudioFlinger:S INNO_SI:S innotest:S";
			LOG("getLogOnUsb::cmd : " +cmd);
			try{
				Runtime.getRuntime().exec(cmd);
			} catch(Exception e) {
				e.printStackTrace();
			}
		} else {
			return;
		}

	}

	private void setLedColor(String color) {
		executeCommand("inno_set_led.sh " + color);
	}

	class AsyncLogViewTask extends AsyncTask<Void, String, String> {
		String method = "LogViewTask";
		
		@Override
		protected String doInBackground(Void... v) {
			Process p;
			BufferedReader br;
			try {
				Log.d(TAG, "Clear system logs...");
				p = Runtime.getRuntime().exec("logcat -c");
				p.waitFor();
				Log.d(TAG, "Start capturing logs with TAG: " + TAG);
		//		String cmd = String.format("logcat | grep %s", TAG), line;
				String cmd = "logcat | grep InnoFactory";
				String line = "";
				p  = Runtime.getRuntime().exec(cmd);
				br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				while ( ! isCancelled() ) {
					line = br.readLine();
					if ( line != null ) {
						if(line.contains("InnoFactory"))
							publishProgress(line.length() < 32 ? line : line.substring(31));
					}
				}
				p.destroy();
				br.close();
			} catch ( Exception e ) {
				Log.e(TAG, "" + e);
				e.printStackTrace();
			}
			return "Stopped capturing logs..";
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			appendLog(values[0]);
		}
		
		@Override
		protected void onPostExecute(String msg) {
			super.onPostExecute(msg);
			appendLog(method + "() " + msg);
			Log.i(TAG, msg);
		}
		
		
		@Override
		protected void onCancelled(){
			String msg = "Cancelled log capture!";
			super.onCancelled();
			appendLog(method + "() " + msg);
			Log.i(TAG, msg);
		}
		
		private void appendLog(String msg) {
			if ( mTextViewTestLog != null ) {
				mTextViewTestLog.append(msg+"\n");
				scrollBottom();
			}
		}

		private void scrollBottom() {
			int lineTop =  mTextViewTestLog.getLayout().getLineTop(mTextViewTestLog.getLineCount());
			int scrollY = lineTop - mTextViewTestLog.getHeight();
			if (scrollY > 0) {
				mTextViewTestLog.scrollTo(0, scrollY);
			} else {
				mTextViewTestLog.scrollTo(0, 0);
			}
		}
	} // AsyncLogViewTask



}

