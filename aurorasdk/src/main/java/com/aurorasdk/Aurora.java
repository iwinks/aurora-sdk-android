package com.aurorasdk;

import android.content.Context;
import android.support.v4.util.Pair;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleScanResult;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;

import android.util.Log;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class Aurora {

    public enum ConnectionState {
        IDLE, SCANNING, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, RECONNECTING
    }

    public interface ConnectionListener{
        void onConnectionStateChange(ConnectionState connectionState);
    }
    public interface ScanListener{
        void onScanResultsChange(List<ScanResult> scanResults);
    }
    public interface EventListener{
        void onEvent(Event event);
    }
    public interface ErrorListener{
        void onError(int errorCode, String errorMessage);
    }

    private RxBleClient rxBleClient;
    private RxBleDevice rxBleDevice;

    private ConnectionListener connectionListener;
    private ScanListener scanListener;
    private Command.CommandCompletionListener commandCompletionListener;
    private ErrorListener errorListener;

    private ConnectionState connectionState;
    private EventListener eventListener;

    private final List<ScanResult> scanResults = new ArrayList<>();
    private boolean autoConnect;
    private boolean explicitDisconnect;

    private final CommandProcessor commandProcessor;

    private Observable<ScanResult> scanObservable;
    private Observable<RxBleConnection> connectObservable;

    private Subscription scanSubscription;
    private Subscription connectSubscription;
    private Subscription connectionStateSubscription;

    private static Aurora instance = null;

    private final EnumSet<Event.EventType> enabledEventTypes = EnumSet.noneOf(Event.EventType.class);

    private Aurora(){

        RxBleClient.setLogLevel(RxBleLog.VERBOSE);
        connectionState = ConnectionState.IDLE;

        commandProcessor = new CommandProcessor(
                this::sendCommand,
                this::writeCommandInput
        );
    }

    public static Aurora getInstance(){

        if (instance == null) {

            instance = new Aurora();
        }

        return instance;
    }

    public static Aurora create(Context context, ConnectionListener connectionListener, ErrorListener errorListener){

        Aurora instance = getInstance();

        if (instance.rxBleClient == null){

            instance.rxBleClient = RxBleClient.create(context);
        }

        instance.connectionListener = connectionListener;
        instance.errorListener = errorListener;

        return instance;
    }

    public static Aurora create(Context context, ConnectionListener connectionListener){

        return Aurora.create(context, connectionListener, null);
    }


    /* Public Methods
    ------------------------------------------------------------------------------------------------
    */

    public void startScan(ScanListener scanListener){

        this.scanListener = scanListener;

        if (connectionState == ConnectionState.IDLE || connectionState == ConnectionState.DISCONNECTED) {

            Log.w("Aurora", "startScan()");

            scanResults.clear();

            boolean reconnecting = (connectionState == ConnectionState.DISCONNECTED) && !explicitDisconnect;

            setConnectionState(reconnecting ? ConnectionState.RECONNECTING : ConnectionState.SCANNING);

            if (scanObservable == null) {

                Log.w("Aurora", "creating scanObservable");

                scanObservable = rxBleClient.scanBleDevices(
                        new ScanSettings.Builder()
                                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                                .build(),
                        new ScanFilter.Builder()
                                .setServiceUuid(Constants.DREAMBAND_SERVICE_UUID)
                                .build()
                ).observeOn(AndroidSchedulers.mainThread());
            }

            scanSubscription = scanObservable.subscribe(this::onScanResult, this::onScanError);
        }
    }

    public void startScan(){

        startScan(null);
    }


    public void stopScan(){

        Log.w("Aurora", "stopScan()");

        scanListener = null;

        //move back to idle state if we haven't established a connection
        if (connectionState == ConnectionState.SCANNING || connectionState == ConnectionState.RECONNECTING){

            setConnectionState(ConnectionState.IDLE);
        }

        if (scanSubscription != null && !scanSubscription.isUnsubscribed()) {

            Log.w("Aurora", "Unsubscribing from scan.");
            scanSubscription.unsubscribe();
        }
    }


    public void connect(RxBleScanResult scanResult, Command.CommandCompletionListener commandCompletionListener){

        Log.w("Aurora", "connect()");

        this.commandCompletionListener = commandCompletionListener;

        if (scanResult == null){

            autoConnect = true;
            this.commandCompletionListener = commandCompletionListener;

            startScan();
        }
        else {

            autoConnect = false;
            connectAndSubscribeToNotifications(scanResult.getBleDevice());
        }
    }

    public void connect(Command.CommandCompletionListener commandCompletionListener){

        connect(null, commandCompletionListener);
    }

    public void connect(){

        connect(null, null);
    }


    public void disconnect(){

        Log.w("Aurora", "disconnect()");

        autoConnect = false;
        explicitDisconnect = true;

        if (connectSubscription != null && !connectSubscription.isUnsubscribed()) {

            Log.w("Aurora", "Unsubscribing from connect.");
            connectSubscription.unsubscribe();
        }
        else {

            this.stopScan();
        }
    }


    public boolean isConnected(){

        return rxBleDevice != null && rxBleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }


    public Command queueCommand(Command command, Command.CommandCompletionListener listener) throws UnsupportedOperationException {

        if (!isConnected()){

            throw new UnsupportedOperationException("Cannot queue Aurora command while not connected.");
        }

        if (listener != null){

            command.addCompletionListener(listener);
        }

        if (commandCompletionListener != null){

            command.addCompletionListener(commandCompletionListener);
        }

        commandProcessor.queueCommand(command);

        return command;
    }

    public Command queueCommand(String commandString, Command.CommandCompletionListener listener){

        Command command = new Command();
        command.setCommandString(commandString);

        return queueCommand(command, listener);
    }

    public Command queueCommand(Command command){

        return queueCommand(command, null);
    }

    public Command queueCommand(String commandString){

        return queueCommand(commandString, null);
    }


    public Command enableEvents(EnumSet<Event.EventType> eventTypes, EventListener eventListener){

        if (eventListener != null && this.eventListener == null){

            this.eventListener = eventListener;
        }

        enabledEventTypes.addAll(eventTypes);

        int mask = Event.eventTypesToMask(eventTypes);

        return queueCommand("event-output-enable " + mask + " 16");
    }


    public Command disableEvents(EnumSet<Event.EventType> eventTypes){

        eventListener = null;

        enabledEventTypes.removeAll(eventTypes);

        int mask = Event.eventTypesToMask(eventTypes);

        return queueCommand("event-output-disable " + mask + " 16");
    }


    /* Helper methods
    ------------------------------------------------------------------------------------------------
    */

    private void sendCommand(Command command){

        Log.w("Aurora", "Sending command: " + command.getCommandString());

        connectObservable.flatMap(rxBleConnection -> Observable.merge(
                rxBleConnection.writeCharacteristic(Constants.COMMAND_STATUS_UUID.getUuid(), new byte[] { (byte)CommandProcessor.CommandState.IDlE.ordinal()}),
                rxBleConnection.writeCharacteristic(Constants.COMMAND_DATA_UUID.getUuid(), command.getCommandStringBytes()),
                rxBleConnection.writeCharacteristic(Constants.COMMAND_STATUS_UUID.getUuid(), new byte[] { (byte)CommandProcessor.CommandState.EXECUTE.ordinal()})
        ))
        .take(3)
        .subscribe(
                bytes -> Log.w("Aurora", "Write cmd success: " + bytes.toString()),
                throwable -> Log.w("Aurora", "Write cmd failure: " + throwable)
        );
    }

    private void writeCommandInput(byte[] data){

        Log.w("Aurora", "writeCommandInput: " + data.toString());

        connectObservable.flatMap(
            rxBleConnection -> rxBleConnection.writeCharacteristic(Constants.COMMAND_DATA_UUID.getUuid(), data)
        )
        .take(1)
        .subscribe(
            bytes -> Log.w("Aurora", "Write input success: " + bytes.toString()),
            throwable -> Log.w("Aurora", "Write input failure: " + throwable)
        );
    }

    private void readCommandResponse(int numBytes){

        final ByteBuffer readBuffer = ByteBuffer.allocate(numBytes);

        connectObservable.flatMap(
            rxBleConnection -> rxBleConnection
                .readCharacteristic(Constants.COMMAND_DATA_UUID.getUuid())
                .doOnNext(bytes -> readBuffer.put(bytes))
                .repeat()
                .takeUntil(bytes -> readBuffer.remaining() == 0)
                .filter(bytes -> readBuffer.remaining() == 0)
                .map(bytes -> readBuffer)
        )
        .take(1)
        .subscribe(
                buffer -> commandProcessor.processCommandResponse(buffer.array()),
                throwable -> Log.w("Aurora", "Read command response failure: " + throwable)
        );
    }

    private void connectAndSubscribeToNotifications(RxBleDevice rxBleDevice){

        this.rxBleDevice = rxBleDevice;

        setConnectionState(ConnectionState.CONNECTING);

        connectionStateSubscription = rxBleDevice.observeConnectionStateChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnectionStateChange);

        connectObservable = rxBleDevice.establishConnection(false)
                .compose(new ConnectionSharingAdapter())
                .observeOn(AndroidSchedulers.mainThread());

        connectSubscription = connectObservable.doOnNext(rxBleConnection -> {
            explicitDisconnect = false;
            stopScan();
        })
        .flatMap(connection -> Observable.merge(
                Observable.from(
                        Arrays.asList(
                                Constants.EVENT_NOTIFIED_UUID.getUuid()
                        )
                ).flatMap(characteristic -> connection.setupNotification(characteristic).flatMap(observable -> observable), Pair::new),
                Observable.from(
                        Arrays.asList(
                                Constants.COMMAND_STATUS_UUID.getUuid(),
                                Constants.COMMAND_OUTPUT_INDICATED_UUID.getUuid()
                        )
                ).flatMap(characteristic -> connection.setupIndication(characteristic).flatMap(observable -> observable), Pair::new)
            )
        )
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.newThread())
        .subscribe(this::onAuroraNotification, this::onAuroraNotificationError);
    }

    private void setConnectionState(ConnectionState _connectionState){

        Log.w("Aurora", "setConnectionState: " + _connectionState);

        if (connectionListener != null && connectionState != _connectionState){

            connectionState = _connectionState;
            connectionListener.onConnectionStateChange(connectionState);
        }

    }



    /* Event Handlers
    ------------------------------------------------------------------------------------------------
    */

    private void onScanResult(ScanResult bleScanResult) {

        Log.w("Aurora", "Device found: " + bleScanResult.getBleDevice().getMacAddress());

        boolean scanResultAlreadyExists = false;

        for (int i = 0; i < scanResults.size(); i++) {

            if (scanResults.get(i).getBleDevice().equals(bleScanResult.getBleDevice())) {

                scanResults.set(i, bleScanResult);
                scanResultAlreadyExists = true;
            }
        }

        if (!scanResultAlreadyExists){

            scanResults.add(bleScanResult);
        }

        if (autoConnect || connectionState == ConnectionState.RECONNECTING){

            if (connectionState == ConnectionState.RECONNECTING && !bleScanResult.getBleDevice().equals(rxBleDevice)){

                return;
            }

            connectAndSubscribeToNotifications(bleScanResult.getBleDevice());
        }
        else if (scanListener != null){

            scanListener.onScanResultsChange(scanResults);
        }
    }

    private void onScanError(Throwable throwable) {

        if (throwable instanceof BleScanException) {

            final String text;
            final BleScanException bleScanException = (BleScanException) throwable;

            switch (bleScanException.getReason()) {

                case BleScanException.BLUETOOTH_NOT_AVAILABLE:
                    text = "Bluetooth is not available";
                    break;
                case BleScanException.BLUETOOTH_DISABLED:
                    text = "Enable bluetooth and try again";
                    break;
                case BleScanException.LOCATION_PERMISSION_MISSING:
                    text = "On Android 6.0 location permission is required. Implement Runtime Permissions";
                    break;
                case BleScanException.LOCATION_SERVICES_DISABLED:
                    text = "Location services needs to be enabled on Android 6.0";
                    break;
                case BleScanException.SCAN_FAILED_ALREADY_STARTED:
                    text = "Scan with the same filters is already started";
                    break;
                case BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    text = "Failed to register application for bluetooth scan";
                    break;
                case BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED:
                    text = "Scan with specified parameters is not supported";
                    break;
                case BleScanException.SCAN_FAILED_INTERNAL_ERROR:
                    text = "Scan failed due to internal error";
                    break;
                case BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES:
                    text = "Scan cannot start due to limited hardware resources";
                    break;
                case BleScanException.UNDOCUMENTED_SCAN_THROTTLE:
                    text = "Android 7+ requires extra delay between successive scan attempts.";
                    break;
                case BleScanException.UNKNOWN_ERROR_CODE:
                case BleScanException.BLUETOOTH_CANNOT_START:
                default:
                    text = "Unable to start scanning";
                    break;
            }

            Log.w("EXCEPTION", text, bleScanException);

            setConnectionState(ConnectionState.IDLE);
        }
    }

    private void onConnectionStateChange(RxBleConnection.RxBleConnectionState connectionState) {

        Log.w("Aurora", "onConnectionStateChange: " + connectionState.toString());

        switch (connectionState){

            case CONNECTING:
                setConnectionState(ConnectionState.CONNECTING);
                break;

            case CONNECTED:
                commandProcessor.reset();
                setConnectionState(ConnectionState.CONNECTED);
                break;

            case DISCONNECTING:
                setConnectionState(ConnectionState.DISCONNECTING);
                break;

            case DISCONNECTED:

                if (connectionStateSubscription != null && !connectionStateSubscription.isUnsubscribed()) {

                    connectionStateSubscription.unsubscribe();
                }

                setConnectionState(ConnectionState.DISCONNECTED);

                //check if this was an explicit disconnect,
                //if not, we should try to reconnect automatically
                if (!explicitDisconnect){

                    startScan();
                }

                break;
        }
    }

    private void onAuroraNotification(Pair<UUID, byte[]> charAndValue){

        String charString = charAndValue.first.toString();
        byte[] value = charAndValue.second;

        Log.w("Aurora", "Char: " + charString + " | value: " + value);

        //this gets called at a high frequency
        //so the switch cases have been ordered by
        //emission frequency
        switch (charString){

            case Constants.COMMAND_OUTPUT_INDICATED_UUID_STRING:
            case Constants.COMMAND_OUTPUT_NOTIFIED_UUID_STRING:

                commandProcessor.processCommandOutput(value);
                break;

            case Constants.COMMAND_STATUS_UUID_STRING:

                //this gets called a lot...
                //we hard code the enum values here
                //to avoid any unnecessary lookups or
                //array copies and order by the most frequently
                //emitted status values
                if (value[0] == 4){

                    commandProcessor.setCommandState(CommandProcessor.CommandState.INPUT_REQUESTED);
                    commandProcessor.requestInput(value[1]);

                }
                else if (value[0] == 2 || value[0] == 3){

                    commandProcessor.setCommandState(value[0] == 2 ?
                            CommandProcessor.CommandState.RESPONSE_OBJECT_READY : CommandProcessor.CommandState.RESPONSE_TABLE_READY, value[1]
                    );

                    readCommandResponse(value[1]);
                }
                else if (value[0] == 0){

                    commandProcessor.setCommandState(CommandProcessor.CommandState.IDlE, value[1]);
                }

                break;

            case Constants.EVENT_NOTIFIED_UUID_STRING:
            case Constants.EVENT_INDICATED_UUID_STRING:

                int eventId = value[0];
                long flags = Utility.getUnsignedInt32(value, 1);

                if (eventListener != null) {

                    try {

                        eventListener.onEvent(new Event(eventId, flags));

                    } catch (Exception e) {

                        Log.w("Aurora", "Unknown event received.");
                    }
                }

                break;

            default:
                Log.w("Aurora", "Unknown Char: " + charString);
                break;

        }
    }

    private void onAuroraNotificationError(Throwable throwable){
        Log.w("Aurora", "Notification failure: " + throwable);
    }

}
