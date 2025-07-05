
package com.innopia.factorytools;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import ca.uol.aig.fftpack.Complex1D;
import ca.uol.aig.fftpack.RealDoubleFFT;

import android.util.Log;

public class VoiceMicTest extends Test {
	
	private static final String TAG = "InnoFactory.VoiceMicTest";

	private  String RECORD_FILE_PREFIX                 = "/data/vendor/audiohal/mic_4ch_16.input.";

	public static final int TEST_TYPE_OPEN 		= 0;
	public static final int TEST_TYPE_MUTE 		= 1;
	public static final int TEST_TYPE_CLOSE 	= 2;

	public static final String EXTRA_KEY_VOICE_MIC_TEST_TYPE 		= "VOICE_MIC_TEST_TYPE";
	public static final String EXTRA_KEY_VOICE_MIC_400HZ_DECIBEL	= "VOICE_MIC_400HZ_DECIBEL";
	public static final String EXTRA_KEY_VOICE_MIC_1KHZ_DECIBEL 	= "VOICE_MIC_1KHZ_DECIBEL";
	public static final String EXTRA_KEY_VOICE_MIC_FAILED_PORT		= "VOICE_MIC_FAILED_PORT";
	
	private final String CONFIG_TAG_VOICE_MIC_TEST_TIME			= "voice_mic_test_time";
	private final String CONFIG_TAG_PART_OPEN_VOICE_MIC 		= "open_voice_mic_";
	private final String CONFIG_TAG_PART_SHIELDED_VOICE_MIC		= "shielded_voice_mic_";
	private final String CONFIG_TAG_PART_400HZ_MIN		= "_400hz_min";
	private final String CONFIG_TAG_PART_400HZ_MAX 		= "_400hz_max";
	private final String CONFIG_TAG_PART_1KHZ_MIN		= "_1khz_min";
	private final String CONFIG_TAG_PART_1KHZ_MAX		= "_1khz_max";

	private int mTestType;

	private ArrayList<Integer> mAll400hzMin;
	private ArrayList<Integer> mAll400hzMax;
	private ArrayList<Integer> mAll1khzMin;
	private ArrayList<Integer> mAll1khzMax;
	private ArrayList<Integer> mFailedPorts;

	private ArrayList mAll400hzResult;
	private ArrayList mAll1khzResult;

	private MediaPlayer m400hzMediaPlayer;
	private MediaPlayer m1khzMediaPlayer;

	private RecordThread m400hzRecordThread;
	private RecordThread m1khzRecordThread;

	private Handler mResultHandler;

	private boolean mIsEnabled;
	private Context mContext;

	public VoiceMicTest(Context context, int testType, Bundle config) {  
		super(context);
		mContext = context;
		
		mTestType = testType;

		mAll400hzMin = new ArrayList<Integer>();
		mAll400hzMax = new ArrayList<Integer>();
		mAll1khzMin = new ArrayList<Integer>();
		mAll1khzMax = new ArrayList<Integer>();
		mFailedPorts = new ArrayList<Integer>();

		mIsEnabled = false;

		if(config != null) {
			try {
				mIsEnabled = true;
				 /*
				   <open_voice_mic_1_400hz_min>-50</open_voice_mic_1_400hz_min>
				   <open_voice_mic_1_400hz_max>50</open_voice_mic_1_400hz_max>
				   <open_voice_mic_1_1khz_min>-36</open_voice_mic_1_1khz_min>
				   <open_voice_mic_1_1khz_max>50</open_voice_mic_1_1khz_max>
				  */

				for(int i=0; i<4; i++) {
					int num = i+1;
					if(mTestType == TEST_TYPE_OPEN) {
						mAll400hzMin.add(i, Integer.parseInt(config.getString(CONFIG_TAG_PART_OPEN_VOICE_MIC + num + CONFIG_TAG_PART_400HZ_MIN)));
						mAll400hzMax.add(i, Integer.parseInt(config.getString(CONFIG_TAG_PART_OPEN_VOICE_MIC + num + CONFIG_TAG_PART_400HZ_MAX)));
						mAll1khzMin.add(i, Integer.parseInt(config.getString(CONFIG_TAG_PART_OPEN_VOICE_MIC + num + CONFIG_TAG_PART_1KHZ_MIN)));
						mAll1khzMax.add(i, Integer.parseInt(config.getString(CONFIG_TAG_PART_OPEN_VOICE_MIC + num + CONFIG_TAG_PART_1KHZ_MAX)));
					} else {
						mAll400hzMin.add(i, Integer.parseInt(config.getString(CONFIG_TAG_PART_SHIELDED_VOICE_MIC + num + CONFIG_TAG_PART_400HZ_MIN)));
						mAll400hzMax.add(i, Integer.parseInt(config.getString(CONFIG_TAG_PART_SHIELDED_VOICE_MIC + num + CONFIG_TAG_PART_400HZ_MAX)));
						mAll1khzMin.add(i, Integer.parseInt(config.getString(CONFIG_TAG_PART_SHIELDED_VOICE_MIC + num + CONFIG_TAG_PART_1KHZ_MIN)));
						mAll1khzMax.add(i, Integer.parseInt(config.getString(CONFIG_TAG_PART_SHIELDED_VOICE_MIC + num + CONFIG_TAG_PART_1KHZ_MAX)));
					}
				}

			} catch (NumberFormatException e) {
				mIsEnabled = false;
				e.printStackTrace();
			}
		}
		Log.d(TAG,"VoiceMicTest : testType=" + mTestType);
	}

	@Override
	public void start() {
		Log.d(TAG,"VoiceMicTest start : testType=" + mTestType);
		if(mIsEnabled) {
			mAll400hzResult = new ArrayList();
			mAll1khzResult = new ArrayList();

			m400hzMediaPlayer = new MediaPlayer();
			m1khzMediaPlayer = new MediaPlayer();  
		
			mResultHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					switch(msg.what) {
						case 400:
							m400hzMediaPlayer.stop();
							m400hzMediaPlayer.reset();
							m400hzMediaPlayer.release();
							m400hzMediaPlayer = null;

							mAll400hzResult = (ArrayList)msg.obj;

							try {
								Thread.sleep(1000);
								m1khzMediaPlayer.prepare();
							} catch (Exception e) {
								e.printStackTrace();
							}
							break;

						case 1000:
							m1khzMediaPlayer.stop();
							m1khzMediaPlayer.reset();
							m1khzMediaPlayer.release();
							m1khzMediaPlayer = null;

							mAll1khzResult = (ArrayList)msg.obj;

							checkResult();
							break;
					}
				}
			};

			try{
				if ( mTestType == TEST_TYPE_OPEN ) {
					m400hzMediaPlayer.setDataSource(mContext, Uri.parse("android.resource://" + mContext.getPackageName() + "/" + R.raw.mic_400hz));
					m400hzMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
						@Override
						public void onPrepared(MediaPlayer mp) {
							Log.d(TAG,"m400hzMediaPlayer::onPrepared");
							mp.start();

							m400hzRecordThread = new RecordThread(400, 10);
							m400hzRecordThread.start();
						}
					});
				}


				m1khzMediaPlayer.setDataSource(mContext, Uri.parse("android.resource://" + mContext.getPackageName() + "/" + R.raw.mic_1khz));
				m1khzMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					@Override
					public void onPrepared(MediaPlayer mp) {
						Log.d(TAG,"m1khzMediaPlayer::onPrepared");
						mp.start();

						m1khzRecordThread = new RecordThread(1000, 10);
						m1khzRecordThread.start();
					}
				});

				if ( mTestType == TEST_TYPE_OPEN ) 
					m400hzMediaPlayer.prepare();
				else 
					m1khzMediaPlayer.prepare();
				
			}catch(IOException e) {
				e.printStackTrace();
			}

		} else {
			Log.d(TAG, "RESULT_FAILED " + mTestType + "  -> start failed");
			Bundle extra = new Bundle();
			extra.putInt(EXTRA_KEY_VOICE_MIC_TEST_TYPE, mTestType);
			mOnResultListener.onResult(VoiceMicTest.this, RESULT_FAILED, extra);
		}
	}


	@Override
	public void stop() {
		SystemProperties.set("inno.factory.mic_test", "0");

		if(m400hzRecordThread != null) {
			m400hzRecordThread.stopThread();
		}

		if(m1khzRecordThread != null) {
			m1khzRecordThread.stopThread();
		}

		if(m400hzMediaPlayer != null && m400hzMediaPlayer.isPlaying()) {
			m400hzMediaPlayer.stop();
			m400hzMediaPlayer.reset();
			m400hzMediaPlayer.release();
		}

		if(m1khzMediaPlayer != null && m1khzMediaPlayer.isPlaying()) {
			m1khzMediaPlayer.stop();
			m1khzMediaPlayer.reset();
			m1khzMediaPlayer.release();
		}
	}

	private boolean checkDetails() {
		if ( mTestType == TEST_TYPE_OPEN ) 
			return checkOpenTypeResult();
		else 
			return checkOtherTypeResult();
	}

	private boolean checkOpenTypeResult() {
		if ( mAll400hzResult.size() != 4 || mAll1khzResult.size() != 4 ) 
			return false;

		mFailedPorts.clear();
		for ( int ii = 0; ii < 4; ii++ ) {
			double result400hz = new Double(mAll400hzResult.get(ii).toString());
			double result1khz = new Double(mAll1khzResult.get(ii).toString());
			if (Double.isNaN(result400hz) || Double.isNaN(result1khz)) {
				Log.d(TAG, "RESULT_FAILED " + mTestType + "  -> isNaN return true");
				mFailedPorts.add(ii + 1);
				//return false;
			} else if(!(mAll400hzMin.get(ii).intValue() < result400hz &&
						result400hz < mAll400hzMax.get(ii).intValue() &&
						mAll1khzMin.get(ii).intValue() < result1khz &&
						result1khz < mAll1khzMax.get(ii).intValue())) {
				Log.d(TAG, "RESULT_FAIELD " + mTestType + " -> value check failed");
				mFailedPorts.add(ii + 1);
				//return false;
			}
		}
		return ( mFailedPorts.size() == 0 );
		//return true;
	}

	private boolean checkOtherTypeResult() {
		if ( mAll1khzResult.size() != 4 ) 
			return false;

		mFailedPorts.clear();
		for ( int ii = 0; ii < 4; ii++ ) {
			double result1khz = new Double(mAll1khzResult.get(ii).toString());
			if (Double.isNaN(result1khz)) {
				Log.d(TAG, "RESULT_FAILED " + mTestType + "  -> isNaN return true");
				mFailedPorts.add(ii + 1);
				//return false;
			} else if( Double.isInfinite(result1khz) && mTestType == TEST_TYPE_MUTE ) { 
				Log.d(TAG, "RESULT_SUCCEEDED " + mTestType + " -> isInfinite return true");
			} else if(!(mAll1khzMin.get(ii).intValue() < result1khz &&
						result1khz < mAll1khzMax.get(ii).intValue())) {
				Log.d(TAG, "RESULT_FAIELD " + mTestType + " -> value check failed");
				mFailedPorts.add(ii + 1);
				//return false;
			}
		}
		return ( mFailedPorts.size() == 0 );
		//return true;
	}


	private void checkResult() {
		int testResult = RESULT_SUCCEEDED;
		Log.d(TAG,"mAll400hzMin.size : " + mAll400hzMin.size() + " , mAll400hzMax.size : " + mAll400hzMax.size() + 
				  	", mAll1khzMin.size : " + mAll1khzMin.size() + " ,mAll1khzMax.size : "+ mAll1khzMax.size() + 
 					", mAll400hzResult.size : " + mAll400hzResult.size() + " , mAll1khzResult.size : " + mAll1khzResult.size());
	
		if( !checkDetails() ) {
			fillDoubleNaN();
			Log.d(TAG, "RESULT_FAILED " + mTestType + " -> WTF");
			testResult = RESULT_FAILED;
		}

		Bundle extra = new Bundle();
		extra.putInt(EXTRA_KEY_VOICE_MIC_TEST_TYPE, mTestType);
		extra.putParcelableArrayList(EXTRA_KEY_VOICE_MIC_400HZ_DECIBEL, mAll400hzResult);
		extra.putParcelableArrayList(EXTRA_KEY_VOICE_MIC_1KHZ_DECIBEL, mAll1khzResult);
		String failedPorts = null;
		if ( mFailedPorts.size() != 0 ) {
			failedPorts = new String();
			for ( int ii = 0; ii < mFailedPorts.size(); ii++ ) {
				failedPorts += mFailedPorts.get(ii) + " ";
			}
		}
		extra.putString(EXTRA_KEY_VOICE_MIC_FAILED_PORT, failedPorts);

		mOnResultListener.onResult(VoiceMicTest.this, testResult, extra);
	}

	private void fillDoubleNaN() {
		if ( mAll1khzResult.size() == 0 ) {
			for ( int ii = 0; ii < 4; ii++ ) {
				mAll1khzResult.add(Double.NaN);
			}
		}
		
		if ( mAll400hzResult.size() == 0 ) {
			for ( int ii = 0; ii < 4; ii++ ) {
				mAll400hzResult.add(Double.NaN);
			}
		}
	}

	private class RecordThread extends Thread {
		//private int AUDIO_ENCODING 	= AudioFormat.ENCODING_PCM_FLOAT;
		//private int CHANNEL_MASK 	= AudioFormat.CHANNEL_IN_STEREO;
		private int SAMPLE_RATE 	= 48000;
		private int CHANNEL_NUM		= 4;
		private int FFT_SIZE		= 1024;
		private int PCM_DEPTH		= 2; // 2bytes, 16 bit signed PCM

		private int mFrequency;
		private int mDataNumber;

		private RealDoubleFFT mRealDoubleFFT;
		private FileInputStream micInputStream;

		RecordThread(int frequency, int dataNumber) {
			mFrequency = frequency;
			mDataNumber = dataNumber;
			mRealDoubleFFT = new RealDoubleFFT(FFT_SIZE);

			//mMinBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, AUDIO_ENCODING);
			Log.d(TAG,"DataNumber : " + mDataNumber + ", FFT_SIZE : " + FFT_SIZE);
		}

		public void stopThread() {
			try {
				if(micInputStream != null) {
					micInputStream.close();
					micInputStream = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private double getDecibel(Complex1D complex1D, int freq) {
			// get target frequency index
			int index = Math.round((float)freq/SAMPLE_RATE * FFT_SIZE);
			double real = complex1D.x[index];
			double imaginary = complex1D.y[index];
			double magnitude = Math.sqrt( (real*real) + (imaginary*imaginary) );
			return Double.parseDouble( String.format("%.1f", 20 * Math.log10(magnitude)) );
			//double result = Double.parseDouble( String.format("%.1f", 20 * Math.log10(magnitude)) );
			//return (Double.isInfinite(result) ? (double)0 : result);
		}


		@Override
			public void run() {
				super.run();
				boolean read_error = false;
				try {

					// mic_4ch_16.input.400, mic_4ch_16.input.401, mic_4ch_16.input.402, mic_4ch_16.input.1000, mic_4ch_16.input.1001, mic_4ch_16.input.1002
					// ex) 1000hz + TEST_TYPE_OPEN(=> 0) : 1000, 400hz + TEST_TYPE_MUTE(=> 1) : 401, 400hz + TEST_TYPE_CLOSE(=> 2) : 402
					// refer to 'android/vendor/synaptics/vsxxx/ffv/libaec/AudioAec.cpp'
					Thread.sleep(500);
					SystemProperties.set("inno.factory.mic_test", Integer.toString(mFrequency + mTestType)); 
					Thread.sleep(1500);
					SystemProperties.set("inno.factory.mic_test", "0");
					Thread.sleep(500);
				} catch(Exception e) {
					e.printStackTrace();
				}

				String micInputFilePath = RECORD_FILE_PREFIX + Integer.toString(mFrequency + mTestType);
				try {
					if(micInputStream != null) {
						micInputStream.close();
					}
					micInputStream = new FileInputStream(micInputFilePath);
				} catch (IOException e) {
					e.printStackTrace();
					micInputStream = null;
				}

				ArrayList resultList = new ArrayList();
				double[][][] channelData = new double[mDataNumber][CHANNEL_NUM][FFT_SIZE];

				if (micInputStream != null) {
					Log.d(TAG, "get mic input stream : " + micInputFilePath);

					try {
						int readBytes;
						int bufferSize = mDataNumber * CHANNEL_NUM * FFT_SIZE * PCM_DEPTH;
						ByteBuffer audioData = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
						micInputStream.skip(192000 - 4); // skip 0.5 second : 48000 (SAMPLE_RATE) * 4 (channel num) * 2 (signed 16bit PCM) / 2 (0.5 second)
														 // -4 : read() call, first 4 bytes are skipped by unkown reason. (why? BOM?)
						readBytes = micInputStream.read(audioData.array());
						if (readBytes >= bufferSize) {
							int channelDataSize = CHANNEL_NUM * FFT_SIZE;
							for (int dataNum=0; dataNum<mDataNumber; dataNum++) {
								for (int i=0; i < channelDataSize; i++) {
									int micIndex = i%4;
									int pcmDataIndex = i/4;
									short shortData = audioData.getShort();
									double doubleData = (double)shortData/Short.MAX_VALUE;
									channelData[dataNum][micIndex][pcmDataIndex] = doubleData;
									//Log.d(TAG, "channelData[" + dataNum + "][" + micIndex + "][" + pcmDataIndex + "] = " + shortData + "(" + doubleData + ")");
								}
							}
						} else {
							Log.d(TAG,"mic input steam: readBytes=" + readBytes + "(expected: greater than " + bufferSize + ")");
							read_error = true;
						}
						micInputStream.close();

					} catch (IOException e) {
						e.printStackTrace();
					}
					micInputStream = null;
				} else {
					Log.d(TAG,"get mic input stream failed");
					read_error = true;
				}

				for(int micNum=0; micNum < CHANNEL_NUM; micNum++) {
					if (!read_error) {
						ArrayList decibelList = new ArrayList();

						for(int dataNum=0; dataNum < mDataNumber; dataNum++) {
							Complex1D complex1D = new Complex1D();
							mRealDoubleFFT.ft(channelData[dataNum][micNum], complex1D);
							double decibel = getDecibel(complex1D, mFrequency);
							decibelList.add(decibel);
						}
						Log.d(TAG, "Mic("+(micNum+1)+").decibelList:"+ decibelList);

						if(mDataNumber < 2) {
							resultList.add(decibelList.get(0));
							continue;
						}

						int minIndex = 0;
						int maxIndex = 0;
						double minValue = (double)decibelList.get(0);
						double maxValue = (double)decibelList.get(0);
						double curValue;

						for(int dataNum=1; dataNum<mDataNumber; dataNum++) {
							curValue = (double)decibelList.get(dataNum);

							if(curValue < minValue) {
								minValue = curValue;
								minIndex = dataNum;
							}

							if(curValue > maxValue) {
								maxValue = curValue;
								maxIndex = dataNum;
							}
						}

						if(minIndex < maxIndex) {
							decibelList.remove(maxIndex);
							decibelList.remove(minIndex);
						} else {
							decibelList.remove(minIndex);
							decibelList.remove(maxIndex);
						}

						Iterator iter = decibelList.iterator();
						while (iter.hasNext()) {
							Object t = iter.next();
							if((double)t < -100 && iter.hasNext() ) {
								iter.remove();
							}
						}

						Log.d(TAG, "Mic("+(micNum+1)+").decibelList:"+ decibelList + " Min("+minIndex+"):"+minValue+" Max("+maxIndex+"):"+maxValue);
						if(decibelList.size() > 0 ) {
							double sum = 0;
							for(int dataNum=0; dataNum<decibelList.size(); dataNum++) {
								sum += (double)decibelList.get(dataNum);
							}

							double average = Double.parseDouble(String.format("%.1f", sum/decibelList.size()));
							Log.d(TAG, "average:" + average);

							resultList.add(average);
						} else {
							resultList.add(Double.NaN);
						}
					} else {
						resultList.add(Double.NaN);
					}
				}

				Message handlerMsg = new Message();
				handlerMsg.what = mFrequency;
				handlerMsg.obj = resultList;

				mResultHandler.sendMessage(handlerMsg);
			}
	}
}
