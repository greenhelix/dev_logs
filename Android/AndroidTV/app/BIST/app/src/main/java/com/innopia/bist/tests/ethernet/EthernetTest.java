package com.innopia.bist.tests.ethernet;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;

import com.innopia.bist.util.ILogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EthernetTest {

    private static final String TAG = "BIST_ETHER_TEST";
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private final ILogger mLogger;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public interface ConnectionInfoListener {
        void onInfoUpdated(String info, Network network, boolean isConnected);
    }

    public EthernetTest(Context context, ILogger logger) {
        mContext = context;
        mLogger = logger;
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void checkCurrentConnection(EthernetTest.ConnectionInfoListener listener){

    }

    private boolean isInternetAvailable(Network network) {
        return true;
    }

    public void runPingTest(Network network, ILogger listener) {

    }
}
