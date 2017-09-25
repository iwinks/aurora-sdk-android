package com.auroradreamband;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dreambandsdk.DreambandBLEService;
import com.dreambandsdk.DreambandResp;

import java.util.HashMap;

public class SDKExampleActivity extends AppCompatActivity {
    private final static String TAG = SDKExampleActivity.class.getSimpleName();

    private DreambandBLEService _dreambandServices;
    private boolean _serviceBound = false;

    private TextView txt_status;
    private ProgressBar prgs_bleActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdkexample);

        txt_status = (TextView) findViewById(R.id.statusTextView);
        prgs_bleActive = (ProgressBar) findViewById(R.id.progressBarBleActive);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Be sure to register service in the Manifest
        Intent intent = new Intent(getApplicationContext(), DreambandBLEService.class);

        startService(intent);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        // The service will autoconnect to the device.
        // Start the BLE progress indicator while we wait for a result
        prgs_bleActive.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is paused.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                dreambandServicesMessageReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        // Register to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(
                dreambandServicesMessageReceiver, DreambandBLEService.makeNxtMobileIntentFilter());
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (_serviceBound) {
            unbindService(mServiceConnection);
            _serviceBound = false;
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            _serviceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DreambandBLEService.LocalBinder myBinder = ( DreambandBLEService.LocalBinder) service;
            _dreambandServices = myBinder.getService();
            _serviceBound = true;
        }
    };

    private void showMsg(String msg)
    {
        Log.d(TAG, msg);
        txt_status.setText(msg);
    }


    // ********* Button Handlers ********** //
    public void onGetUnsyncedSessionCount(View v)
    {
        if (_dreambandServices != null) {
            prgs_bleActive.setVisibility(View.VISIBLE);
            showMsg("Getting unsynced session count.");
            _dreambandServices.unsyncedSessionCount();
        }
    }


    // ********* Dreamband Services Handler ********** //
    // Handler that will be called whenever an Intent is broadcasted from the Dreamband service.
    private BroadcastReceiver dreambandServicesMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action.equals(DreambandResp.RESP_DEVICE_CONNECTED)) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showMsg("Device Connected.");
                        prgs_bleActive.setVisibility(View.INVISIBLE);
                    }
                }, 100);
            }
            else if (action.equals(DreambandResp.RESP_DEVICE_DISCONNECTED)) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showMsg("Device Disconnected. Searching for device...");
                        prgs_bleActive.setVisibility(View.VISIBLE);
                    }
                }, 100);
            }
            else if (action.equals(DreambandResp.RESP_UNSYNCED_SESSION_COUNT)) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        // The table contents are stored in:
                        HashMap<String, String> tableContents = (HashMap<String, String>)intent.getSerializableExtra(DreambandResp.RESP_UNSYNCED_SESSION_COUNT);

                        showMsg("Received unsync session count: " + intent.getStringExtra(DreambandResp.RESP_TABLE_SIZE));
                        prgs_bleActive.setVisibility(View.INVISIBLE);
                    }
                }, 100);
            }
        }
    };
}
