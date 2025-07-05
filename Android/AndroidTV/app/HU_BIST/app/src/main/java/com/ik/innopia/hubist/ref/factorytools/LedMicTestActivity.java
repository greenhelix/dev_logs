package com.innopia.factorytools;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.content.res.Configuration;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import org.json.JSONObject;
import org.json.JSONException;

public class LedMicTestActivity extends Activity {

	private final String TAG = "InnoFactory";
	private final boolean DEBUG = true;

	private final String STORAGE_PATH = "/mnt/media_rw";
	private final String STORAGE_PATH2 = "/storage";
	private final String CONFIGURATION_FILE_NAME = "factorytools_config.xml";

	private int mIRCount = 0;

	private boolean mIsTesting = false;
	private TestView mTestViewIR;
	private TestView mTestViewCloseMic;
	
	private TextView mTextViewTestResult;
	private TextView mTextViewTestLog;
	private Bundle mConfigValues;

	private VoiceMicTest mVoiceCloseMicTest;

	private AsyncLogViewTask mLogTask = null;

	public void LOG(String msg) {
		if ( DEBUG ) Log.d(TAG, msg);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mic);
		mTestViewIR = (TestView) findViewById(R.id.testViewManualIRLed);
		mTestViewCloseMic = (TestView) findViewById(R.id.testViewManualCloseMic);
		mTextViewTestResult = (TextView)findViewById(R.id.textViewTestResult);
		mTextViewTestLog = (TextView)findViewById(R.id.textViewLog);

		readyToVoiceTest();

		mLogTask = new AsyncLogViewTask();
		mLogTask.execute();
	}

	private boolean readyToVoiceTest() {
		if ( mConfigValues != null && mVoiceCloseMicTest != null ) {
			return true;
		}

		mConfigValues = getConfigValues();
		if ( mConfigValues == null ) {
			LOG("voice test is not ready. you need to check USB");
			return false;
		}

		if ( mVoiceCloseMicTest == null ) {
			mVoiceCloseMicTest = new VoiceMicTest(this, VoiceMicTest.TEST_TYPE_CLOSE, mConfigValues); 
			mVoiceCloseMicTest.setOnResultListener(mOnResultListener);
		}

		if ( mVoiceCloseMicTest == null ) {
			LOG("A CRITICAL unknown error occurred. Please reboot.");
			return false;
		}
		return true;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if ( event.getKeyCode() == KeyEvent.KEYCODE_BACK ) 
			return false;

		if ( event.getAction() == KeyEvent.ACTION_UP ) { 
			int keyCode = event.getKeyCode();
			if ( keyCode == KeyEvent.KEYCODE_DPAD_CENTER ) {
				checkIRTestResult();
			} else if ( keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE && !mIsTesting ) {
				if ( !isMuteMic() && readyToVoiceTest() ) {
					mIsTesting = true;
					mTestViewCloseMic.setStatus(TestView.STATUS_TESTING, null);
					mVoiceCloseMicTest.start();
				} else if ( isMuteMic() ) { 
					LOG("mic mute!!!!!!!!!!!!");
				} 
			}
		}

		return super.dispatchKeyEvent(event);
	}

	private Test.OnResultListener mOnResultListener = new Test.OnResultListener() {
		@Override
		public void onResult(Test test, int resultCode, Bundle extra) { 
			if ( test.getClass() == VoiceMicTest.class ) {
				int status = (resultCode == Test.RESULT_SUCCEEDED) ? TestView.STATUS_SUCCEEDED : TestView.STATUS_FAILED;
				mIsTesting = false;
				mTestViewCloseMic.setStatus(status, extra.getString(VoiceMicTest.EXTRA_KEY_VOICE_MIC_FAILED_PORT));

				ArrayList mic1khzDecibel = extra.getParcelableArrayList(VoiceMicTest.EXTRA_KEY_VOICE_MIC_1KHZ_DECIBEL);

				if(mic1khzDecibel != null) {
					StringBuilder stringBuilder = new StringBuilder("Mic : ");
					for ( int ii = 0; ii < 4; ii++ ) {
						stringBuilder.append((ii+1) + "(");
						stringBuilder.append(mic1khzDecibel.get(ii)+") ");
					}

					mTextViewTestResult.setText(stringBuilder.toString());
				}
			}
		}
	};

	private void checkIRTestResult() { 
		switch ( mIRCount % 3 ) {
			case 2:
				setLedColor("blue");
				break;
			case 1:
				setLedColor("green");
				break;
			case 0:
				setLedColor("red");
				break;
		}

		mIRCount++;
		if ( mIRCount >= 3 ) {
			mTestViewIR.setStatus(TestView.STATUS_SUCCEEDED, ""+mIRCount);
		} else {
			mTestViewIR.setStatus(TestView.STATUS_TESTING, ""+mIRCount);
		}
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

	private boolean isMuteMic() { 
		final String MIC_STATUS = "/sys/class/leds/mic_led/mic_gpio";
		//final String MIC_STATUS = "/sys/class/gpio/gpio395/value";
		String value = readFile(MIC_STATUS);
		if ( TextUtils.isEmpty(value) ) 
			return false;

		return (value.trim().equals("0"));
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

	private void setLedColor(String color) {
		executeCommand("inno_set_led.sh " + color);
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

	private Bundle getConfigValues() {
		Log.d(TAG,"getConfigValues()");
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
			if ( mTextViewTestLog == null ) return ;
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
