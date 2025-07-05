package com.innopia.factorytools.tcp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;

public class TCPServer {

	private String TAG = "InnoFactory.TCPServer";

	private Context mContext;
	private ConnectedThread mConnectedThread = null;

	private ServerSocket mServerSocket;

	private CryptoUtils mCryptoUtils;
	private boolean mIsCryptoEnabled = false;

	private Handler mHandler;
	private int PORT = 7654;
	private AtomicBoolean mRequestStop = new AtomicBoolean(false);;


	private int[] mCrc16Tab;

	private boolean isSecure = false;



	public interface SocketListener {
		void onStatusChanged(boolean isConnected);
		void onReceivedFlag(int flag);
		void onReceivedData(int tag, byte[] data);
	}

	private SocketListener mSocketListener = null;

//	static {
//		System.loadLibrary("jni_nagracrc");
//	}

	private native int getNagraCRC16(byte[] data);	


	public TCPServer(Context context) {
		mContext = context;
		mRequestStop.set(false);

		try {
			mServerSocket = new ServerSocket();
			mServerSocket.setReuseAddress(true);
			mServerSocket.bind(new InetSocketAddress(PORT));
		} catch (Exception ex) {
			Log.d(TAG, "exception make server socket");
			ex.printStackTrace();
		}
	}

	public void setSocketListener(SocketListener listener) {
		mSocketListener = listener;
	}

	public void setCryptoEnabled(boolean enabled) {
		mIsCryptoEnabled = enabled;
	}


	public void connect() {
		Log.d(TAG, "server connect");
		if(mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		Log.d(TAG, "--------------------");
		Log.d(TAG, "--- Start Server ---");
		Log.d(TAG, "--------------------");
		mConnectedThread = new ConnectedThread();
		mConnectedThread.start();
	}


	public void reconnect() {
		if ( mRequestStop.get() ) {
			return;
		}

		Log.d(TAG, "server reconnect");
		if(mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		mSocketListener.onStatusChanged(false);
		mHandler = new Handler(Looper.getMainLooper());
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				connect();
			}
		}, 2000);
	}

	public void stop() {
		Log.d(TAG, "-------------------");
		Log.d(TAG, "--- Stop Server ---");
		Log.d(TAG, "-------------------");
		mRequestStop.set(true);

		try {
			if(mConnectedThread != null) {
				mConnectedThread.cancel();
				mConnectedThread = null;
			}

			if(mServerSocket != null) {
				mServerSocket.close();
				mServerSocket = null;
			}
		} catch (Exception ex) {
			Log.d(TAG, "exception server socket close");
			ex.printStackTrace();
		}
	}


	class ConnectedThread extends Thread {
		Socket mSocket = null;
		InputStream mInputStream = null;
		OutputStream mOutputStream = null;

		@Override
		public void run() {
			try {
				Log.d(TAG, "wait connection");
				mSocket = mServerSocket.accept();
				mSocketListener.onStatusChanged(true);
				mCryptoUtils = new CryptoUtils(mContext);
			} catch (Exception ex) {
				Log.d(TAG, "exception make socket");
				ex.printStackTrace();
				reconnect();
				return;
			}

			try {
				mInputStream = mSocket.getInputStream();
				mOutputStream = mSocket.getOutputStream();
			} catch (Exception ex) {
				ex.printStackTrace();
				Log.d(TAG, "exception make stream");
				reconnect();
				return;
			}

			byte[] dataBytes = new byte[4096];
			int totalLen = 0;
			int dataLen = 0;

			while ( ! mRequestStop.get() ) {
				byte[] m = new byte[4096];
				try {
					int n = mInputStream.read(m);
					Log.d(TAG, "receive n =" + n);
					Log.d(TAG, "receive m =" + m);

					if (n == -1) {
						reconnect();
						break;
					} else if(n==2) {
						parseRecvFlag(m);
					} else {
						long header = ((m[0] & 0xff) << 24) | ((m[1] & 0xff) << 16) | ((m[2] & 0xff) << 8) | (m[3] & 0xff);
						if(header == DefProtocol.TAG_DATA_HEADER || header == DefProtocol.TAG_AES_DATA_HEADER) {

							totalLen = ((m[4] & 0xff) << 24) | ((m[5] & 0xff) << 16) | ((m[6] & 0xff) << 8) | (m[7] & 0xff);
							dataBytes = new byte[totalLen+12];
							dataLen = 0;

							if(header == DefProtocol.TAG_DATA_HEADER) {
								totalLen += 12;
							} else {
								totalLen += 8;
							}
						}

						System.arraycopy(m, 0, dataBytes, dataLen, n);
						dataLen += n;
						
						Log.d(TAG, "received dataLen :"+dataLen+" totalLen :"+totalLen);
						if(dataLen == totalLen) {
							if(parseRecvData(dataBytes, dataLen) == -1){
								sendErrorMessage("Failed to receive key data.");
								reconnect();
								return;
							}
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					if ( mRequestStop.get() ) {
						Log.d(TAG, "stopped read/write stream");
					}
					else {
						Log.e(TAG, "exception read/write stream ex :" + ex);
					}
					reconnect();
					return;
				}
			}

			Log.d(TAG, "Connected Thread is stopped!");
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
					Log.d(TAG, "mOutputStream is close");
					mOutputStream.close();
					mOutputStream = null;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				Log.d(TAG, "exception close stream");
				reconnect();
			}
		}

		public int write(byte[] b) {
			if ( mRequestStop.get() ) {
				Log.e(TAG, "Already stopped!");
				return -1;
			}

			if ( mSocket == null ) {
				Log.e(TAG, "Not connected yet!");
				return -1;
			}

			try {
				if ( mOutputStream == null ) {
					mOutputStream = mSocket.getOutputStream();
					if ( mOutputStream == null ) {
						Log.e(TAG, "Can't write. OutputStream is null");
						return -1;
					}
				}
				mOutputStream.write(b);
				return 0;
			} catch (IOException e) {
				Log.d(TAG, "exception during write", e);
				reconnect();
				return -1;
			}
		}

		public int write(byte[] b, int off, int len) {
			if ( mRequestStop.get() ) {
				Log.e(TAG, "Already stopped!");
				return -1;
			}

			if ( mSocket == null ) {
				Log.e(TAG, "Not connected yet!");
				return -1;
			}

			try {
				if ( mOutputStream == null ) {
					mOutputStream = mSocket.getOutputStream();
					if ( mOutputStream == null ) {
						Log.e(TAG, "Can't write. OutputStream is null");
						return -1;
					}
				}
				mOutputStream.write(b, off, len);
				return 0;
			} catch (IOException e) {
				Log.e(TAG, "exception during write", e);
				reconnect();
				return -1;
			}
		}
	}

	public int write(byte[] b) {
		ConnectedThread r = mConnectedThread;

		if ( mRequestStop.get() ) {
			Log.e(TAG, "Already stopped!");
			return -1;
		}

		synchronized (this) {
			if ( r == null ) {
				return -1;
			} else {
				if ( mIsCryptoEnabled ) {
					GenData genData = new GenData();
					genData.genDataInit(DefProtocol.TAG_AES_DATA_HEADER, mCryptoUtils.encryptDataByAES(b));
					genData.genDataAddCRC();
					return r.write(genData.genDataGetData());

				}else {
					return r.write(b);
				}
			}
		}
	}
	
	private int writeByRSA(byte[] b) {
		ConnectedThread r = mConnectedThread;

		if ( mRequestStop.get() ) {
			Log.e(TAG, "Already stopped!");
			return -1;
		}

		synchronized (this) {
			if ( r == null ) {
				return -1;
			} else {
				GenData genData = new GenData();
				genData.genDataInit(DefProtocol.TAG_PKI_DATA_HEADER, mCryptoUtils.encryptDataByRSA(b));
				genData.genDataAddCRC();
				return r.write(genData.genDataGetData());
			}
		}
	}

	public int write(byte[] b, int off, int len) {
		ConnectedThread r = mConnectedThread;

		if ( mRequestStop.get() ) {
			Log.e(TAG, "Already stopped!");
			return -1;
		}

		synchronized (this) {
			if ( r == null ) {
				return -1;
			} else {
				if( b != null ) {
					return r.write(b, off, len);
				}
				return -1;
			}
		}
	}

	public void parseRecvFlag(byte[] m) {
		int flag = ((m[0] & 0xff) << 8) | (m[1] & 0xff) ;

		if ( mRequestStop.get() ) {
			Log.e(TAG, "Already stopped!");
			return;
		}

		if ( mSocketListener != null ) {
			mSocketListener.onReceivedFlag(flag);
		}
	}

	public int parseRecvData(byte[] m, int nread) {	
		long header = ((m[0] & 0xff) << 24) | ((m[1] & 0xff) << 16) | ((m[2] & 0xff) << 8) | (m[3] & 0xff);

		if ( mRequestStop.get() ) {
			Log.e(TAG, "Already stopped!");
			return -1;
		}

		if( header != DefProtocol.TAG_DATA_HEADER && header != DefProtocol.TAG_AES_DATA_HEADER ) {
			Log.d(TAG, "error - header : 0x" + Long.toHexString(header));
		}
		
		int totalLen = ((m[4] & 0xff) << 24) | ((m[5] & 0xff) << 16) | ((m[6] & 0xff) << 8) | (m[7] & 0xff);
		Log.d(TAG, "nread :"+nread+" totalLen :"+totalLen);
		Log.d(TAG,"parseRecvData : " + byteArrayToHex(m));
		if ( header == DefProtocol.TAG_AES_DATA_HEADER) {
			Log.d(TAG, "TAG_AES_DATA_HEADER");
			if(nread != (totalLen + 8)) {
				Log.d(TAG, "mismatch totalLength and recv. read");
				return -1;
			}
			byte[] encryptedData = getBytes(m, 8, totalLen);
			byte[] decryptedData = mCryptoUtils.decryptDataByAES(encryptedData);
			return parseRecvData(decryptedData, decryptedData.length);
		} else {
			Log.d(TAG, "TAG_DATA_HEADER");
			if(nread != (totalLen + 12)) {
				Log.d(TAG, "mismatch totalLength and recv. read");
				return -1;
			}

			int calcCrc32 = getCRC32(m, totalLen);
			int posCrc32 = 8 + totalLen;
			int tailCrc32= ((m[posCrc32] & 0xff) << 24) | ((m[posCrc32+1] & 0xff) << 16) | ((m[posCrc32+2] & 0xff) << 8) | (m[posCrc32+3] & 0xff) ;
			if(calcCrc32 != tailCrc32) {
				Log.d(TAG, "mismatch crc calc[" + calcCrc32 + "], tail[" + tailCrc32+ "]");
				return -1;
			}

			int posTag = 8;
			int tag;
			int dataLen;
			byte[] data;
			while (true) {
				tag = ((m[posTag] & 0xff) << 24) | ((m[posTag+1] & 0xff) << 16) | ((m[posTag+2] & 0xff) << 8) | (m[posTag+3] & 0xff);
				dataLen = ((m[posTag+4] & 0xff) << 24) | ((m[posTag+5] & 0xff) << 16) | ((m[posTag+6] & 0xff) << 8) | (m[posTag+7] & 0xff);
				data = getBytes(m, posTag+8, dataLen);
				Log.d(TAG, "received tag : "+ Integer.toHexString(tag));

				if(tag == DefProtocol.TAG_PUBLIC_KEY) {

					mCryptoUtils.setRSAPublicKey(data);

					GenData genDataAesKey = new GenData();
					genDataAesKey.genDataInit();
					
					genDataAesKey.genDataAddData(DefProtocol.TAG_AES_128_KEY, mCryptoUtils.getAESKey().length, mCryptoUtils.getAESKey());
					genDataAesKey.genDataAddData(DefProtocol.TAG_AES_16_IV, mCryptoUtils.getIV().length,  mCryptoUtils.getIV());
					genDataAesKey.genDataAddData(DefProtocol.TAG_TAIL_AES_KEY, 0, null);
					genDataAesKey.genDataAddCRC();
				
					int ret = writeByRSA(genDataAesKey.genDataGetData());
					Log.d(TAG,"writeByRSA :" + ret);

					if(ret != -1){
						isSecure = true;
					}
				} else {
					if(tag == DefProtocol.TAG_NAGRA_PK || tag == DefProtocol.TAG_NAGRA_CSC) {
						int receivedCrc = (((data[dataLen-2] & 0xff) << 8) | (data[dataLen-1] & 0xff));
						int calculatedCrc = getNagraCRC16(getBytes(data, 0, dataLen-2));
						Log.d(TAG, "receivedCrc : "+receivedCrc+" calculatedCrc : " + calculatedCrc);
						
						if(receivedCrc == calculatedCrc) {
							mSocketListener.onReceivedData(tag, data);
						} else {
							return -1;
						}
											
					} else {
						mSocketListener.onReceivedData(tag, data);
					}
				}
				totalLen -= (dataLen + 4 + 4);
				if(totalLen <= 0) break;
				posTag += dataLen + 4 + 4;
			}

			return checkTag(tag);
		}
	}

	private int checkTag(int tag) {
		Log.d(TAG,"checkTag" + tag);
		if(tag == DefProtocol.TAG_TAIL_PUBLIC_KEY 
			|| tag == DefProtocol.TAG_TAIL_KEY_1
			|| tag == DefProtocol.TAG_TAIL_REQ_KEY_DATA
			|| tag == DefProtocol.TAG_TAIL_DB_CLIENT_12
			|| tag == DefProtocol.TAG_TAIL_DB_CLIENT_11
			|| tag == DefProtocol.TAG_TAIL_DB_CLIENT_21 ) {
			return 0;
		} else {
			return -1;
		}
	}

	public boolean isTCPServerReady(){
		return isSecure;
	}

	private int sendCal() {
		GenData genData = new GenData();
		genData.genDataInit();
		byte[] cal = genData.genDataGetData();
		return write(cal);
	}

	public int sendErrorMessage(String errorMessage) {
		GenData genData = new GenData();
		genData.genDataInit();
		genData.genDataAddData(DefProtocol.TAG_ERROR_1, errorMessage.getBytes().length, errorMessage.getBytes());
		genData.genDataAddData(DefProtocol.TAG_TAIL_ERROR, 0, null);
		genData.genDataAddCRC();
		byte[] error = genData.genDataGetData();
		return write(error);
	}

	public int sendFlag(int flag) {
		Log.d(TAG, "sendFlag : " + flag);	
		byte[] szBuf = new byte[4];
		szBuf[0] = (byte)((flag >> 8) & 0xff);
		szBuf[1] = (byte)(flag & 0xff);
		Log.d(TAG,"sendFlag : " + byteArrayToHex(szBuf));
		return write(szBuf, 0, 2);
	}

	public int getCRC32(byte[] buf, int len) {
		CRC32 crc32 = new CRC32();
		crc32.update(buf, 8, len);
		return (int) crc32.getValue();
	}

	public byte[] getBytes(byte[] src, int offset, int length) {
		byte[] arr = new byte[length];
		for ( int ii = offset; ii < length + offset; ii++ ) {
			arr[ii - offset] = src[ii];
		}
		return arr;
	}

	public String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder();
		for( final byte b : a ) {
			sb.append(String.format("%02x ", b&0xff));
		}
		return sb.toString();
	}
}
