package com.innopia.factorytools;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.View;
import android.util.Log;


import java.io.File;
import java.io.IOException;


public class StreamingTest extends Test {
	private final String TAG = "InnoFactory.StreamingTest";

	private final String STORAGE_PATH = "/storage";
	private final String PREFIX_FILE = "file://";

	private final String CONFIG_TAG_STREAMING_URL = "streaming_url";
													 
	private MediaPlayer mMediaPlayer;
	private Bundle mConfig;

	public StreamingTest(Context context, SurfaceHolder surfaceHolder, Bundle config) {
		super(context);
		Log.d(TAG,"onCreate");
		mMediaPlayer = new MediaPlayer();

		mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {												
				mOnResultListener.onResult(StreamingTest.this, RESULT_SUCCEEDED, null);
				mp.start();
			}
		});

		mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				mp.reset();
			}
		});

		mConfig = config;

		surfaceHolder.addCallback(new SurfaceHolder.Callback() {
			
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				Log.d(TAG,"surfaceCreated");
				mMediaPlayer.setDisplay(holder);
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int widht, int height) {
				Log.d(TAG,"surfaceChanged format : " + format +" , widht : " + widht + " , height : " + height);
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				Log.d(TAG,"surfaceDestroyed");
				mMediaPlayer.setDisplay(null);
			}
		});
	}



	@Override
	public void start() {
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				if(mMediaPlayer != null) {
					if(!mMediaPlayer.isPlaying()) {
						try {
							if(setDataSource(mConfig)) {
								mMediaPlayer.prepare();
							} else {
								mOnResultListener.onResult(StreamingTest.this, RESULT_FAILED, null);
							}
						} catch (IOException e) {
							mOnResultListener.onResult(StreamingTest.this, RESULT_FAILED, null);
						} catch (IllegalStateException e) {
							mOnResultListener.onResult(StreamingTest.this, RESULT_FAILED, null);
						}
					} else {
						mOnResultListener.onResult(StreamingTest.this, RESULT_SUCCEEDED, null);
					}
				} else {
					mMediaPlayer = new MediaPlayer();
					mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
						@Override
						public void onPrepared(MediaPlayer mp) {
							mOnResultListener.onResult(StreamingTest.this, RESULT_SUCCEEDED, null);
							mp.start();
						}
					});
					
					mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
						@Override
						public void onCompletion(MediaPlayer mp) {
							mp.reset();
						}
					});
					
					try {
						if(setDataSource(mConfig)) {
							mMediaPlayer.prepare();
						} else {
							mOnResultListener.onResult(StreamingTest.this, RESULT_FAILED, null);
						}
					} catch (IOException e) {
						mOnResultListener.onResult(StreamingTest.this, RESULT_FAILED, null);
					} catch (IllegalStateException e) {
						mOnResultListener.onResult(StreamingTest.this, RESULT_FAILED, null);
					}
				}
			}
		}).start();	
	}

	public void start(Bundle config) {
	  new Thread(new Runnable() {
		  @Override
		  public void run() {
			  if(mMediaPlayer != null) {
				  if(!mMediaPlayer.isPlaying()) {
					  try {
		 				 if(setDataSource(config)) {
						 	mMediaPlayer.prepare();
						 } else {
							 mOnResultListener.onResult(StreamingTest.this, RESULT_FAILED, null);
						 }
					 } catch (IOException e) {
						 mOnResultListener.onResult(StreamingTest.this, RESULT_FAILED, null);
					 } catch (IllegalStateException e) {
						 mOnResultListener.onResult(StreamingTest.this, RESULT_FAILED, null);
					 }
				 } else {
					 mOnResultListener.onResult(StreamingTest.this, RESULT_SUCCEEDED, null);
				 }
			 }	
		 }
	  }).start();
	}



	@Override
	public void stop() {
		if(mMediaPlayer != null) {
			if(mMediaPlayer.isPlaying()) {
				mMediaPlayer.stop();
			}

			mMediaPlayer.reset();
		}
	}

	public void resume() {
		if ( mMediaPlayer != null ) {
			if ( !mMediaPlayer.isPlaying() ) {
				mMediaPlayer.start();
			}
			mOnResultListener.onResult(StreamingTest.this, RESULT_SUCCEEDED, null);
		}
	}

	public void pause() {
		if ( mMediaPlayer != null ) {
			if ( mMediaPlayer.isPlaying() ) {
				mMediaPlayer.pause();
			}
		}
	}



	private boolean setDataSource(Bundle config) {

		boolean result = false;

		if(config != null) {
			final String videoUrl = config.getString(CONFIG_TAG_STREAMING_URL);

			try{
				if(videoUrl != null) {

					if(videoUrl.startsWith(PREFIX_FILE)) {
						String videoFileName = videoUrl.substring(PREFIX_FILE.length()); 
						File storageDirectory = new File(STORAGE_PATH);
						
						for(File file : storageDirectory.listFiles()) {
							if(file.isDirectory()) {
								String directoryName = file.getName();
								if (!directoryName.equals("emulated") && !directoryName.equals("self")) {
									File videoFile = new File(file.getPath()+"/"+videoFileName);

									if(videoFile.exists()) {
										mMediaPlayer.setDataSource(videoFile.getPath());
										mMediaPlayer.setLooping(true);
										result = true;
									} else {
										result = false;
									}
								}
							}
						}

					} else {
						ConnectivityManager connectivityManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
						NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

						if(networkInfo != null && networkInfo.isConnected()) {
							mMediaPlayer.setDataSource(mContext.getApplicationContext(), Uri.parse(videoUrl));
							mMediaPlayer.setLooping(true);
							result = true;
						} else {
							result = false;
						}
					}
				}
			}catch (IOException e) {
				result = false;
				
			}catch (IllegalStateException e) {
				result = false;
			}

		} else {
			result = false;
		}

		return result;
	}

}
