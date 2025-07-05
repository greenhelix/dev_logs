package com.innopia.factorytools.tcp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.zip.CRC32;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;


public class LogTCPServer {

	private String TAG = "LogTCPServer";
	private final String CONFIG_TAG_ETHERNET_PING = "ethernet_ping";

	private String LogServerAddress;
	private Context mContext;
	private ConnectedThread mConnectedThread;

//	private ServerSocket mServerSocket;

	private boolean mIsCryptoEnabled = false;

	private Handler mHandler;
	private int PORT = 7109;
	private boolean requestStop = false;


	public LogTCPServer(Context context,Bundle config) {
		mContext = context;
		requestStop = false;

		if(config != null) {
			LogServerAddress = config.getString(CONFIG_TAG_ETHERNET_PING);
		}
	}


	public void connect() {
		Log.d(TAG, "innofactory::server connect");
		if(mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		mConnectedThread = new ConnectedThread();
		mConnectedThread.start();
	}

	public void disconnect() {
		if(requestStop) return;

		Log.d(TAG, "innofactory::server disconnect");
		if(mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		mHandler = new Handler(Looper.getMainLooper());
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				connect();
			}
		}, 3000);
	}

	public void disconnect(int reconnect) {
		if(requestStop) return;

		Log.d(TAG, "innofactory::server disconnect");
		if(mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		if(reconnect == 1) {
			mHandler = new Handler(Looper.getMainLooper());
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					connect();
				}
			}, 3000);
		}
	}



	public void stop() {
		Log.d(TAG, "innofactory::server stop");
		requestStop = true;

		try {
			if(mConnectedThread != null) {
				mConnectedThread.cancel();
				mConnectedThread = null;
			}
//			if(mServerSocket != null) {
//				mServerSocket.close();
//				mServerSocket = null;
//			}
		} catch (Exception ex) {
			Log.d(TAG, "innofactory::exception server socket close");
			ex.printStackTrace();
		}
	}


	class ConnectedThread extends Thread {

		Socket mSocket;
		InputStream mInputStream;
		OutputStream mOutputStream;

		@Override
		public void run() {
			try {
				if(LogServerAddress == null) return;
				Log.d(TAG, "innofactory::wait connection LogServerAddress : " + LogServerAddress);	
				mSocket = new Socket(LogServerAddress,PORT);
			} catch (ConnectException e) {
				Log.d(TAG, "innofactory::ConnectException make socket");
				disconnect(0);
			} catch (Exception ex) {
				Log.d(TAG, "innofactory::exception make socket");
				ex.printStackTrace();
				try {
					Thread.sleep(3000);
					disconnect(1);
				} catch (Exception e) {
				}
			}

			try {
				Log.d(TAG, "innofactory::getInputStream & OutputStream");
				mInputStream = mSocket.getInputStream();
				mOutputStream = mSocket.getOutputStream();
			} catch (Exception ex) {
				ex.printStackTrace();
				Log.d(TAG, "innofactory::exception make stream");
				disconnect(0);
			}
			
			if(mOutputStream == null) return;
			
			try {
				Runtime.getRuntime().exec("logcat -c;");
				Process process = Runtime.getRuntime().exec("logcat AudioFlinger:S INNO_SI:S innotest:S");
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));				
				
				Writer bw = new OutputStreamWriter(mOutputStream);

				String line;

				while((line = bufferedReader.readLine()) != null) {
					if(line.length() != 0) {
						bw.write(line+"\r\n");
//						Log.d("innotest","sendLog : " + line);
						bw.flush();
					}
					line = bufferedReader.readLine();
				}

				bw.close();

			} catch (IOException e) {
				Log.d("innofactory","sendLog IOException : " + e.toString());
				disconnect(0);
			}

		}

		public void cancel() {
			try {
				if(mSocket != null) {
					mSocket.close();
					mSocket = null;
				}
				if(mInputStream != null) {
					mInputStream.close();
					mInputStream = null;
				}
				if(mOutputStream != null) {
					mOutputStream.close();
					mOutputStream = null;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				Log.d(TAG, "exception close stream");
				disconnect(0);
			}
		}
	}
/*
	public void sendLogFile(int cnt){
		try {
			File[] files = new File("/data/").listFiles();
			BufferedOutputStream bos = new BufferedOutputStream(JavaSocketServer.aSocket.getOutputStream());
			DataOutputStream dos = new DataOutputStream(bos);

			dos.writeInt(cnt);

			for(File file: files) {
				if(file.getName().contains(".log")) {
					Log.d("innofactory",file.getName());
				}
			}

			for(File file: files) {
				if(file.getName().contains(".log")) {
					long length = file.length();
					Log.d("innofactory","writeUTF : " + file.getName());
					dos.writeUTF(file.getName());
					
					FileInputStream fis = new FileInputStream(file);
					BufferedInputStream bis = new BufferedInputStream(fis);

					int mByte = 0;
					while((mByte = bis.read() != -1)) {
						bos.write(mByte);
					}
					bos.flush();
					bis.close();
				} else {
					continue;
				}
				dos.flush();

			}
			

		} catch (IOException e ){
			Log.d("innofactory","sendLogFile IOException : " + e.toString());
		}

	}
*/


}
