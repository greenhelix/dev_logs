package com.innopia.factorytools.tcp;

import android.util.Log;

import java.util.zip.CRC32;

public class GenData {

    private String TAG = "GenData";

    private byte[] mSzBuf = new byte[16384];
    private int mDataPos;
    private int mTotalLen;
    private boolean mFlagAddCRC = false;

    public int genDataInit() {
        int header = DefProtocol.TAG_DATA_HEADER;
        mDataPos = 7 ;
        mTotalLen = 0 ;
        mFlagAddCRC = false;

        mSzBuf[0] = (byte) ((header >> 24) & 0xff);
        mSzBuf[1] = (byte) ((header >> 16) & 0xff);
        mSzBuf[2] = (byte) ((header >> 8) & 0xff);
        mSzBuf[3] = (byte) (header & 0xff);

        mSzBuf[4] = (byte) (0x00);
        mSzBuf[5] = (byte) (0x00);
        mSzBuf[6] = (byte) (0x00);
        mSzBuf[7] = (byte) (0x00);

        return 1;
    }

    public int genDataInit(int header, byte[] buf) {
        mDataPos = 7 ;
        mTotalLen = buf.length ;
        mFlagAddCRC = false;

        mSzBuf[0] = (byte) ((header >> 24) & 0xff);
        mSzBuf[1] = (byte) ((header >> 16) & 0xff);
        mSzBuf[2] = (byte) ((header >> 8) & 0xff);
        mSzBuf[3] = (byte) (header & 0xff);

        mSzBuf[4] = (byte) ((mTotalLen >> 24) & 0xff);
        mSzBuf[5] = (byte) ((mTotalLen >> 16) & 0xff);
        mSzBuf[6] = (byte) ((mTotalLen >> 8) & 0xff);
        mSzBuf[7] = (byte) (mTotalLen & 0xff);

        for(int i = 0; i < mTotalLen; i++) {
            mSzBuf[mDataPos + 1 + i] = buf[i];
        }
	mDataPos += buf.length; 
        return 1;	
    }

    public int genDataAddData(int tag, int length, byte[] buf) {
        if(mFlagAddCRC) {
            Log.d(TAG, "error : cannot add no more data.");
            return -1;
        }

        mSzBuf[mDataPos + 1] = (byte) ((tag >> 24) & 0xff);
        mSzBuf[mDataPos + 2] = (byte) ((tag >> 16) & 0xff);
        mSzBuf[mDataPos + 3] = (byte) ((tag >> 8) & 0xff);
        mSzBuf[mDataPos + 4] = (byte) (tag & 0xff);

        mSzBuf[mDataPos + 5] = (byte) ((length >> 24) & 0xff);
        mSzBuf[mDataPos + 6] = (byte) ((length >> 16) & 0xff);
        mSzBuf[mDataPos + 7] = (byte) ((length >> 8) & 0xff);
        mSzBuf[mDataPos + 8] = (byte) (length & 0xff);

        for(int i = 0; i < length; i++) {
            mSzBuf[mDataPos + 9 + i] = buf[i];
        }

        mDataPos += (4 + 4 + length);
        mTotalLen += (4 + 4 + length);

        // update total length
        mSzBuf[4] = (byte) ((mTotalLen >> 24) & 0xff);
        mSzBuf[5] = (byte) ((mTotalLen >> 16) & 0xff);
        mSzBuf[6] = (byte) ((mTotalLen >> 8) & 0xff);
        mSzBuf[7] = (byte) (mTotalLen & 0xff);
        return 0;
    }

    public int genDataAddCRC() {
        CRC32 crc32 = new CRC32();
        crc32.update(mSzBuf, 8, mTotalLen);

        int crc = (int) crc32.getValue();
        mSzBuf[mDataPos + 1] = (byte) ((crc >> 24) & 0xff);
        mSzBuf[mDataPos + 2] = (byte) ((crc >> 16) & 0xff);
        mSzBuf[mDataPos + 3] = (byte) ((crc >> 8) & 0xff);
        mSzBuf[mDataPos + 4] = (byte) (crc & 0xff);

        mFlagAddCRC = true;
        return 0;
    }

    public byte[] genDataGetData() {
        int len = mTotalLen + 4 + 4 + 4;
        byte[] buf = new byte[len];
        for(int i=0; i<len; i++) {
            buf[i] = mSzBuf[i];
        }
        return buf;
    }

    public String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for(final byte b : a)
            sb.append(String.format("%02x ", b&0xff));

        return sb.toString();
    }

    @Override
    public String toString() {
        return byteArrayToHex(mSzBuf);
    }
}
