package com.innopia.factorytools.tcp;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;


public class CryptoUtils {
	private final String TAG = "Security";

	private final int AES_KEY_SIZE = 128; 
	private final int IV_SIZE = 16;

	private Context mContext;

	private RSAPublicKey mRSAPublicKey;

	private Key mAESKey;
	private byte[] mIV;


	public CryptoUtils(Context context) {
		mContext = context;
		initAES();	
	}



	public void setRSAPublicKey(byte[] keyData) {

		try {
			String stringPublicKey = new String(keyData);

			stringPublicKey = stringPublicKey.replace("-----BEGIN PUBLIC KEY-----\n", "");
			stringPublicKey = stringPublicKey.replace("-----END PUBLIC KEY-----\n", "");

			byte[] encodedKey = Base64.decode(stringPublicKey, Base64.DEFAULT);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");

			mRSAPublicKey = (RSAPublicKey)keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < setRSAPublicKey() > - 1");
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < setRSAPublicKey() > - 2");
		}
	}


	public byte[] encryptDataByRSA(byte[] data) {
		
		byte[] encryptedData = null;
	
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, mRSAPublicKey);
			
			encryptedData = cipher.doFinal(data);

		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < encryptDataByRSA() > - 1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < encryptDataByRSA() > - 2");
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < encryptDataByRSA() > - 3");
		} catch (BadPaddingException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < encryptDataByRSA() > - 4");
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < encryptDataByRSA() > - 5");
		}
		
		return encryptedData;
	}


	public byte[] encryptDataByAES(byte[] data) {
		
		byte[] encryptedData = null;

		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			IvParameterSpec ivParameterSpec = new IvParameterSpec(mIV);

			cipher.init(Cipher.ENCRYPT_MODE, mAESKey, ivParameterSpec);
			
			encryptedData = cipher.doFinal(data);
/*
			StringBuilder stringEncryptedData = new StringBuilder();
			for(final byte b : encryptedData) {
				stringEncryptedData.append(String.format("%02x ", b&0xff));
			}
			Log.d(TAG, "encryptedData : "+stringEncryptedData.toString());
*/
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < encryptDataByAES() > - 1");
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < encryptDataByAES() > - 2");
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < encryptDataByAES() > - 3");
		} catch (BadPaddingException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < encryptDataByAES() > - 4");
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < encryptDataByAES() > - 5");
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < encryptDataByAES() > - 6");
		}

		return encryptedData;
	}



	public byte[] decryptDataByAES(byte[] data) {
		
		byte[] decryptedData = null;

		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			IvParameterSpec ivParameterSpec = new IvParameterSpec(mIV);

			cipher.init(Cipher.DECRYPT_MODE, mAESKey, ivParameterSpec);

			decryptedData = cipher.doFinal(data);
	
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < decryptDataByAES() > - 1");
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < decryptDataByAES() > - 2");
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < decryptDataByAES() > - 3");
		} catch (BadPaddingException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < decryptDataByAES() > - 4");
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < decryptDataByAES() > - 5");
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < decryptDataByAES() > - 6");
		}

		return decryptedData;
	}




	public byte[] getAESKey() {
		return mAESKey.getEncoded();
	}


	public byte[] getIV() {
		return mIV;
	}





	private void initAES() {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			SecureRandom secureRandom = SecureRandom.getInstanceStrong();
		
			keyGenerator.init(AES_KEY_SIZE, secureRandom);

			mAESKey = keyGenerator.generateKey();
/*	
			StringBuilder stringKey = new StringBuilder();
			for(final byte b : mAESKey.getEncoded()) {
				stringKey.append(String.format("%02x ", b&0xff));
			}
			Log.d(TAG, "key : "+stringKey.toString());
*/		
			mIV = new byte[IV_SIZE];
			secureRandom.nextBytes(mIV);
/*
			StringBuilder stringIv = new StringBuilder();
			for(final byte b : mIV) {
				stringIv.append(String.format("%02x ", b&0xff));
			}
			Log.d(TAG, "iv : "+stringIv.toString());
*/			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed < initAES() >");
		}
	}



}
