package com.arsoft.edcpassportreader.common;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.arsoft.ol106151.BuildConfig;
import com.ingenico.pclservice.PclService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class CommonActivity extends AppCompatActivity implements CommonActivityIntf {
    public static final String TAG = "CommonActivity";
    protected PclService mPclService = null;
    private StateReceiver m_StateReceiver = null;
    protected PclServiceConnection mServiceConnection;
    protected int mReleaseService;
    protected boolean mBound = false;
    protected static boolean enableLog = false;

    protected MutableLiveData<Integer> batteryLevel = new MutableLiveData<>();
    protected MutableLiveData<String> serialNumber = new MutableLiveData<>();

    class PclServiceConnection implements ServiceConnection
    {
        public void onServiceConnected(ComponentName className, IBinder boundService )
        {
            PclService.LocalBinder binder = (PclService.LocalBinder) boundService;
            mPclService = (PclService) binder.getService();
            Log.d(TAG, "onServiceConnected" );
            onPclServiceConnected();
        }

        public void onServiceDisconnected(ComponentName className)
        {
            mPclService = null;
            Log.d(TAG, "onServiceDisconnected" );
        }
    };

    int SN, PN;

    public CommonActivity() {
    }

    @Override
    protected void onResume()
    {
        Log.d(TAG, "CommonActivity: onResume" );
        super.onResume();
        initStateReceiver();
    }
    @Override
    protected void onPause()
    {
        Log.d(TAG, "CommonActivity: onPause" );
        super.onPause();
        releaseStateReceiver();
    }


    protected abstract void onPclServiceConnected();

    protected void initService()
    {
        if (!mBound)
        {
            Log.d(TAG, "initService" );
            SharedPreferences settings = getSharedPreferences("PCLSERVICE", MODE_PRIVATE);
            boolean enableLog = settings.getBoolean("ENABLE_LOG", true);
            mServiceConnection = new PclServiceConnection();
            Intent intent = new Intent(this, PclService.class);
            intent.putExtra("PACKAGE_NAME", BuildConfig.APPLICATION_ID);
            intent.putExtra("FILE_NAME", "pairing_addr.txt");
            intent.putExtra("ENABLE_LOG", enableLog);
            mBound = bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    protected void releaseService()
    {

        if (mBound) {
            Log.d(TAG, "releaseService" );
            unbindService(mServiceConnection );
            mBound = false;
        }
    }

    boolean getFullSerialNumber(byte[] serialNbr) {
        boolean ret = false;

        if( mPclService != null ) {
            {
                ret = mPclService.getFullSerialNumber(serialNbr);
                ByteBuffer bbSN = ByteBuffer.wrap(serialNbr);
                bbSN.order(ByteOrder.LITTLE_ENDIAN);
                SN = bbSN.getInt();
            }
        }
        return ret;

    }

    public boolean isCompanionConnected()
    {
        boolean bRet = false;
        if (mPclService != null)
        {
            byte[] result = new byte[1];
            {
                if (mPclService.serverStatus(result))
                {
                    if (result[0] == 0x10)
                        bRet = true;
                }
            }
        }
        return bRet;
    }

    private void initStateReceiver()
    {
        if(m_StateReceiver == null)
        {
            m_StateReceiver = new StateReceiver(this);
            IntentFilter intentfilter = new IntentFilter("com.ingenico.pclservice.intent.action.STATE_CHANGED");
            registerReceiver(m_StateReceiver, intentfilter);
        }
    }
    private void releaseStateReceiver()
    {
        if(m_StateReceiver != null)
        {
            unregisterReceiver(m_StateReceiver);
            m_StateReceiver = null;
        }
    }

    private static class StateReceiver extends BroadcastReceiver
    {
        private CommonActivity ViewOwner = null;
        @SuppressLint("UseValueOf")
        public void onReceive(Context context, Intent intent)
        {
            String state = intent.getStringExtra("state");
            Log.d(TAG, String.format("receiver: State %s", state));
            ViewOwner.onStateChanged(state);
        }

        StateReceiver(CommonActivity receiver)
        {
            super();
            ViewOwner = receiver;
        }
    }

    protected void runGetBatteryLevel() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        int[] level = new int[1];

        executor.execute(() -> {
            mPclService.getBatteryLevel(level);

            handler.post(() -> {
                batteryLevel.postValue(level[0]);
            });
        });
    }

    protected void runGetFullSerialNumber() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        byte[] fullSN = new byte[30];

        executor.execute(() -> {
            getFullSerialNumber(fullSN);

            handler.post(() -> {
                serialNumber.postValue(new String(fullSN));
            });
        });
    }
}
