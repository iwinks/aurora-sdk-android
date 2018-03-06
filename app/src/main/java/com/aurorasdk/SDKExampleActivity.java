package com.aurorasdk;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;


public class SDKExampleActivity extends AppCompatActivity {

    private final static String TAG = SDKExampleActivity.class.getSimpleName();

    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private TextView txt_status;
    private EditText txt_command;
    private EditText txt_response;
    private EditText txt_event;
    private ProgressBar prgs_bleActive;

    private Aurora aurora;


    /* Lifecycle
    ------------------------------------------------------------------------------------------------
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdkexample);

        txt_status = (TextView) findViewById(R.id.statusTextView);
        txt_command = (EditText) findViewById(R.id.txtCommand);
        txt_response = (EditText) findViewById(R.id.responseText);
        txt_event = (EditText) findViewById(R.id.eventText);
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

        aurora = Aurora.create(this, this::onConnectionStateChange, this::onError);

        aurora.setDebug(true);
    }


    /* Button Click Handlers
    ------------------------------------------------------------------------------------------------
    */

    public void onStartScanClick(View v) {

        aurora.startScan(scanResults -> {

            if (!aurora.isConnected()){

                showMsg("Auroras found: " + scanResults.size());
            }

        });
    }

    public void onStopScanClick(View v)
    {
        aurora.stopScan();
    }

    public void onConnectClick(View v) {

        if (!aurora.isConnected()) {

            aurora.connect(this::onCommandComplete);
        }
    }

    public void onDisconnectClick(View v) {

        aurora.disconnect();
    }

    public void onCustomCommandClick(View v) {
        String command = txt_command.getText().toString();
        if (!command.isEmpty()) {
            this.queueCommand(command);
        }
    }

    public void onLedDemoClick(View v){

        queueCommand("led-demo");
    }

    public void onSyncTimeClick(View v){

        queueCommand(new CommandTimeSync());
    }

    public void onOsInfoClick(View v){

        queueCommand("os-info");
    }

    public void onWriteFileClick(View v) {

        String content = "Here we go. Let's write more than 20 bytes just to make sure this is working. We need to add a little extra to make sure the timeout isn't effected. Here we go. Let's write more than 20 bytes just to make sure this is working. We need to add a little extra to make sure the timeout isn't effected.";

        queueCommand(new CommandSdFileWrite("test.txt", content));
    }

    public void onReadFileClick(View v)
    {
        queueCommand(new CommandSdFileRead("test.txt"));
    }

    public void onLoadProfileClick(View v) {

        queueCommand(new CommandProfileLoad("default.prof"));
    }

    public void onUnloadProfileClick(View v)
    {
        queueCommand(new CommandProfileUnload());
    }

    public void onUpdateProfileClick(View v){

        queueCommand(new CommandSdFileRead("profiles/default.prof"), (readCmd) -> {

            if (!readCmd.hasError()){

                Profile profile = new Profile(readCmd.getResponseOutputString());

                profile.setOptionValue("stim-enabled", profile.getOptions().get("stim-enabled") == "0" ? "1" : "0");

                queueCommand(new CommandSdFileWrite("profiles/default.prof", profile.toString()));
            }

        });

    }

    public void onEnableEventsClick(View v) {

        if (aurora.isConnected()){

            showMsg("Enabling events...");
            aurora.enableEvents(EnumSet.of(Event.EventType.buttonMonitor, Event.EventType.batteryMonitor), this::onEvent);
        }
        else {

            showMsg("Aurora must be connected to enable events.");
        }
    }

    public void onDisableEventsClick(View v) {

        if (aurora.isConnected()){

            showMsg("Disabling events...");
            aurora.disableEvents(EnumSet.of(Event.EventType.buttonMonitor, Event.EventType.batteryMonitor));
        }
        else {

            showMsg("Aurora must be connected to disable events.");
        }
    }


    /* Helper methods
    ------------------------------------------------------------------------------------------------
    */
    private void showMsg(String msg) {

        Log.d(TAG, msg);

        runOnUiThread(() -> {

            txt_status.setText(msg);

        });

    }

    private void queueCommand(Command command, Command.CommandCompletionListener completionListener){

        if (aurora.isConnected()){

            showMsg("Queuing command: " + command.getCommandString());
            aurora.queueCommand(command, completionListener);
        }
        else {

            showMsg("Dreamband not connected, command canceled.");
        }
    }

    private void queueCommand(Command command){

        queueCommand(command, null);
    }

    private void queueCommand(String commandString){

        Command command = new Command();
        command.setCommandString(commandString);
        queueCommand(command);
    }


    /* Event Handlers
    ------------------------------------------------------------------------------------------------
    */

    private void onCommandComplete(Command command){

        runOnUiThread(() -> {

            prgs_bleActive.setVisibility(View.INVISIBLE);

            if (command.hasError()){

                showMsg("Command error: " + command.getErrorMessage());
            }
            else {

                showMsg("Command completed successfully.");
            }

            txt_response.setText(command.isTable() ? command.getResponseTable().toString() : command.getResponseObject().toString());

        });

    }

    private void onConnectionStateChange(Aurora.ConnectionState connectionState){

        final String message;
        boolean busy = false;

        switch (connectionState){

            case IDLE:
                message = "Touch SCAN or CONNECT to begin.";
                break;

            case SCANNING:
                message = "Scanning for Aurora devices...";
                busy = true;
                break;

            case CONNECTING:
                message = "Connecting to Aurora device...";
                busy = true;
                break;

            case CONNECTED:
                message = "Connected to Aurora.";
                break;

            case DISCONNECTING:
                message = "Disconnecting from Aurora...";
                busy = true;
                break;

            case DISCONNECTED:
                message = "Disconnected from Aurora.";
                break;

            case RECONNECTING:
                message = "Attempting to reconnect to Aurora...";
                busy = true;
                break;

            default:
                message = "";
                break;
        }

        showMsg(message);

        prgs_bleActive.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
    }

    private void onEvent(Event event){

        runOnUiThread(() -> {
            txt_event.setText(event.toString());
        });
    }

    private void onError(Aurora.ErrorType errorType, String errorMessage){

        showMsg(errorType.name() + ": " + errorMessage);
    }

}
