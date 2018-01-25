package com.auroradreamband;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dreambandsdk.DreambandClient;


public class SDKExampleActivity extends AppCompatActivity {

    private final static String TAG = SDKExampleActivity.class.getSimpleName();

    String exampleFileName = "hello_world.bin";
    String exampleDirName = "example";

    private TextView txt_status;
    private EditText txt_command;
    private EditText txt_response;
    private EditText txt_profileName;
    private ProgressBar prgs_bleActive;

    private DreambandClient dreambandClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdkexample);

        txt_status = (TextView) findViewById(R.id.statusTextView);
        txt_command = (EditText) findViewById(R.id.txtCommand);
        txt_response = (EditText) findViewById(R.id.responseText);
        txt_profileName = (EditText) findViewById(R.id.txtProfileName);
        prgs_bleActive = (ProgressBar) findViewById(R.id.progressBarBleActive);

        dreambandClient = DreambandClient.create(this, this::onConnectionStateChange);
    }

    @Override
    protected void onStart() {
        super.onStart();

        prgs_bleActive.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void showMsg(String msg)
    {
        Log.d(TAG, msg);
        txt_status.setText(msg);
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(SDKExampleActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public void onStartScan(View v)
    {
        dreambandClient.startScan();
    }

    public void onStopScan(View v)
    {
        dreambandClient.stopScan();
    }

    // ********* Button Handlers ********** //
    public void onConnect(View v)
    {
        if (!dreambandClient.isConnected()) {
            prgs_bleActive.setVisibility(View.VISIBLE);

            dreambandClient.connect();
        }
    }

    public void onDisconnect(View v)
    {
        prgs_bleActive.setVisibility(View.VISIBLE);

        dreambandClient.disconnect();
    }

    public void onCustomCommand(View v)
    {
        String command = txt_command.getText().toString();
        if (!command.isEmpty()) {
            prgs_bleActive.setVisibility(View.VISIBLE);
            showMsg("Sending command to dreamband...");
            dreambandClient.queueCommand(command);
        }
    }

    public void onGetUnsyncedSessionCount(View v)
    {
        prgs_bleActive.setVisibility(View.VISIBLE);
        showMsg("Getting unsynced session count.");
    }

    public void onGetOSVersion(View v)
    {
        prgs_bleActive.setVisibility(View.VISIBLE);
        showMsg("Getting OS version info.");
    }

    public void onObserveEvents(View v)
    {
        prgs_bleActive.setVisibility(View.VISIBLE);
        showMsg("Subscribing to events.");
    }

    public void onDisableEvents(View v)
    {
        prgs_bleActive.setVisibility(View.VISIBLE);
        showMsg("Disabling events.");
    }

    public void onListProfiles(View v)
    {
        prgs_bleActive.setVisibility(View.VISIBLE);
        showMsg("Getting profiles list.");
    }

    public void onUnloadProfile(View v)
    {
        prgs_bleActive.setVisibility(View.VISIBLE);
        showMsg("Unloading profile.");
    }

    public void onLoadProfile(View v)
    {
        String profName = txt_profileName.getText().toString();
        if (profName == null || profName.isEmpty() || profName.length() == 0)
        {
            showMsg("Error: Check profile name");
            return;
        }

        prgs_bleActive.setVisibility(View.VISIBLE);
        showMsg("Loading profile: " + profName);
    }

    public void onCreateDir(View v)
    {
        prgs_bleActive.setVisibility(View.VISIBLE);
        showMsg("Creating directory: " + exampleDirName);
        String cmd = "sd-dir-create " + exampleDirName;
    }


    public void onWriteFile(View v)
    {
        prgs_bleActive.setVisibility(View.VISIBLE);
        showMsg("Writing file: " + exampleFileName);
        String cmd = "sd-file-write " + exampleFileName + " " + exampleDirName +  " 0 1 250 0";
    }

    public void onReadFile(View v)
    {
        prgs_bleActive.setVisibility(View.VISIBLE);
        showMsg("Reading file: " + exampleFileName);
        String cmd = "sd-file-read " + exampleFileName + " " + exampleDirName +  " 0";
    }

    public void onConnectionStateChange(DreambandClient.ConnectionState connectionState){

        final String message;

        switch (connectionState){

            case IDLE:
                message = "Touch SCAN or CONNECT to begin.";
                break;

            case SCANNING:
                message = "Scanning for Aurora devices...";
                break;

            case CONNECTING:
                message = "Connecting to Aurora device...";
                break;

            case CONNECTED:
                message = "Connected to Aurora.";
                break;

            case DISCONNECTING:
                message = "Disconnecting from Aurora...";
                break;

            case DISCONNECTED:
                message = "Disconnected from Aurora...";
                break;

            case RECONNECTING:
                message = "Attempting to reconnect to Aurora...";
                break;

            default:
                message = "";
                break;
        }

        showMsg(message);
        prgs_bleActive.setVisibility(View.INVISIBLE);
    }

}
