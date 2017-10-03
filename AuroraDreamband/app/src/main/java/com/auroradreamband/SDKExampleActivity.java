package com.auroradreamband;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dreambandsdk.DreambandBLEService;
import com.dreambandsdk.DreambandResp;
import com.dreambandsdk.TableRow;
import com.dreambandsdk.request.DreambandRequest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class SDKExampleActivity extends AppCompatActivity {
    private final static String TAG = SDKExampleActivity.class.getSimpleName();
    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private DreambandBLEService _dreambandServices;
    private boolean _serviceBound = false;
    private TextView txt_status;
    private EditText txt_command;
    private EditText txt_response;
    private ProgressBar prgs_bleActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdkexample);

        txt_status = (TextView) findViewById(R.id.statusTextView);
        txt_command = (EditText) findViewById(R.id.txtCommand);
        txt_response = (EditText) findViewById(R.id.responseText);
        prgs_bleActive = (ProgressBar) findViewById(R.id.progressBarBleActive);
        if (Build.VERSION.SDK_INT >= 23) {
            // Marshmallow+ Permission APIs
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final List<String> permissionsList = new ArrayList<String>();
                permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);

                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Be sure to register service in the Manifest
        Intent intent = new Intent(getApplicationContext(), DreambandBLEService.class);

        startService(intent);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        if (DreambandBLEService.SCAN_ON_START) {
            // The service will autoconnect to the device.
            // Start the BLE progress indicator while we wait for a result
            showMsg("Searching for dreamband...");
            prgs_bleActive.setVisibility(View.VISIBLE);
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);


                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

                        ) {
                    // All Permissions Granted
                    Toast.makeText(SDKExampleActivity.this, "All permissions granted, launching App.", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    // Permission Denied
                    Toast.makeText(SDKExampleActivity.this, "One or more permissions denied, exiting App.", Toast.LENGTH_SHORT)
                            .show();

                    finish();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(SDKExampleActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    // ********* Button Handlers ********** //
    public void onConnect(View v)
    {
        if (_dreambandServices != null) {
            prgs_bleActive.setVisibility(View.VISIBLE);
            showMsg("Searching for dreamband...");
            _dreambandServices.connect();
        }
    }

    public void onDisconnect(View v)
    {
        if (_dreambandServices != null) {
            prgs_bleActive.setVisibility(View.VISIBLE);
            showMsg("Disconnecting from dreamband...");
            _dreambandServices.disconnect();
        }
    }

    public void onCustomCommand(View v)
    {
        String command = txt_command.getText().toString();
        if (_dreambandServices != null && !command.isEmpty()) {
            prgs_bleActive.setVisibility(View.VISIBLE);
            showMsg("Sending command to dreamband...");
            _dreambandServices.sendCommand(command, null);
        }
    }

    public void onGetUnsyncedSessionCount(View v)
    {
        if (_dreambandServices != null) {
            prgs_bleActive.setVisibility(View.VISIBLE);
            showMsg("Getting unsynced session count.");
            _dreambandServices.unsyncedSessionCount();
        }
    }

    public void onGetOSVersion(View v)
    {
        if (_dreambandServices != null) {
            prgs_bleActive.setVisibility(View.VISIBLE);
            showMsg("Getting OS version info.");
            _dreambandServices.osVersion();
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
                        showMsg("Dreamband Connected.");
                        prgs_bleActive.setVisibility(View.INVISIBLE);
                    }
                }, 100);
            }
            else if (action.equals(DreambandResp.RESP_DEVICE_DISCONNECTED)) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showMsg("Dreamband Disconnected.");
                        prgs_bleActive.setVisibility(View.INVISIBLE);
                    }
                }, 100);
            }
            else if (action.equals(DreambandResp.RESP_COMMAND)) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // The response contents are stored in:
                        String command = intent.getStringExtra(DreambandResp.RESP_COMMAND);
                        DreambandRequest.ResponseType respType = (DreambandRequest.ResponseType)intent.getSerializableExtra(DreambandResp.RESP_TYPE);
                        showMsg("Dreamband response received for command: " + command + ", Response Type: " + respType);
                        if (respType == DreambandRequest.ResponseType.TABLE_RESP)
                        {
                            ArrayList<TableRow> respTable = intent.getParcelableArrayListExtra(DreambandResp.RESPONSE);
                            Log.d(TAG, respTable.toString());
                            txt_response.setText(respTable.toString());
                        } else {
                            HashMap<String, String> respObj = (HashMap<String, String>)intent.getSerializableExtra(DreambandResp.RESPONSE);
                            Log.d(TAG, respObj.toString());
                            txt_response.setText(respObj.toString());
                        }
                        prgs_bleActive.setVisibility(View.INVISIBLE);
                    }
                }, 100);
            }
            else if (action.equals(DreambandResp.RESP_UNSYNCED_SESSION_COUNT)) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        int unsyncedSessionCount =intent.getIntExtra(DreambandResp.RESP_UNSYNCED_SESSION_COUNT, -1);
                        showMsg("Received unsync session count: " + unsyncedSessionCount);
                        prgs_bleActive.setVisibility(View.INVISIBLE);
                    }
                }, 100);
            }
            else if (action.equals(DreambandResp.RESP_OS_VERSION)) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        showMsg("Received version info: " + intent.getIntExtra(DreambandResp.RESP_OS_VERSION, -1));
                        prgs_bleActive.setVisibility(View.INVISIBLE);
                    }
                }, 100);
            }
        }
    };
}
