package com.dreambandsdk;

import android.content.Context;
import android.os.Handler;
import android.support.v4.util.Pair;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.polidea.rxandroidble.RxBleClient;
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

import android.bluetooth.BluetoothGatt;

public class DreambandClient  {

    public enum ConnectionState {
        IDLE, SCANNING, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, RECONNECTING
    }
    public interface ConnectionListener{
        void onConnectionStateChange(ConnectionState connectionState);
    }


    private RxBleClient rxBleClient;
    private RxBleDevice rxBleDevice;


    private ConnectionListener connectionListener;
    private ConnectionState connectionState;

    private final List<ScanResult> scanResults = new ArrayList<>();
    private boolean autoConnect;
    private boolean explicitDisconnect;

    private final CommandProcessor commandProcessor;

    private Observable<ScanResult> scanObservable;
    private Subscription scanSubscription;

    private Subscription connectSubscription;
    private Observable<RxBleConnection> connectObservable;
    private Subscription connectionStateSubscription;

    private static DreambandClient instance = null;

    private DreambandClient(){

        RxBleClient.setLogLevel(RxBleLog.VERBOSE);
        connectionState = ConnectionState.IDLE;

        commandProcessor = new CommandProcessor(
                this::sendCommand,
                this::writeCommandInput
        );
    }

    public static DreambandClient getInstance(){

        if (instance == null) {

            instance = new DreambandClient();
        }

        return instance;
    }

    public static DreambandClient create(Context context, ConnectionListener connectionListener){

        DreambandClient instance = getInstance();

        if (instance.rxBleClient == null){

            instance.rxBleClient = RxBleClient.create(context);
        }

        instance.connectionListener = connectionListener;

        return instance;
    }




    public void startScan(){

        if (connectionState == ConnectionState.IDLE || connectionState == ConnectionState.DISCONNECTED) {

            Log.w("DreambandClient", "startScan()");

            scanResults.clear();

            boolean reconnecting = (connectionState == ConnectionState.DISCONNECTED) && !explicitDisconnect;

            setConnectionState(reconnecting ? ConnectionState.RECONNECTING : ConnectionState.SCANNING);

            if (scanObservable == null) {

                Log.w("DreambandClient", "creating scanObservable");

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

    public void stopScan(){

        Log.w("DreambandClient", "stopScan()");

        //move back to idle state if we haven't established a connection
        if (connectionState == ConnectionState.SCANNING || connectionState == ConnectionState.RECONNECTING){

            setConnectionState(ConnectionState.IDLE);
        }

        if (scanSubscription != null && !scanSubscription.isUnsubscribed()) {

            Log.w("DreambandClient", "Unsubscribing from scan.");
            scanSubscription.unsubscribe();
        }
    }

    public void connect(){

        Log.w("DreambandClient", "connect()");

        autoConnect = true;

        startScan();
    }

    public void disconnect(){

        Log.w("DreambandClient", "disconnect()");

        //explicitly disconnecting means we need to
        //disable autoconnecting
        autoConnect = false;
        explicitDisconnect = true;

        if (connectSubscription != null && !connectSubscription.isUnsubscribed()) {

            Log.w("DreambandClient", "Unsubscribing from connect.");
            connectSubscription.unsubscribe();
        }
        else {

            this.stopScan();
        }
    }

    public boolean isConnected(){

        return rxBleDevice != null && rxBleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    public void queueCommand(AuroraCommand command){

        commandProcessor.queueCommand(command);
    }

    public void queueCommand(String commandString){

        queueCommand(new AuroraCommand(commandString));
    }




    private void sendCommand(AuroraCommand command){

        Log.w("DreambandClient", "sendCommand: " + command.toString());

        connectObservable.flatMap(rxBleConnection -> Observable.merge(
                rxBleConnection.writeCharacteristic(Constants.COMMAND_STATUS_UUID.getUuid(), new byte[] {Constants.CommandState.IDLE.value()}),
                rxBleConnection.writeCharacteristic(Constants.COMMAND_DATA_UUID.getUuid(), command.toBytes()),
                rxBleConnection.writeCharacteristic(Constants.COMMAND_STATUS_UUID.getUuid(), new byte[] {Constants.CommandState.EXECUTE.value()})
        ))
        .subscribe(
                bytes -> Log.w("DreambandClient", "Write cmd success: " + bytes.toString()),
                throwable -> Log.w("DreambandClient", "Write cmd failure: " + throwable)
        );
    }

    private void writeCommandInput(byte[] data){

        connectObservable.flatMap(
            rxBleConnection -> rxBleConnection.writeCharacteristic(Constants.COMMAND_DATA_UUID.getUuid(), data)
        )
        .subscribe(
            bytes -> Log.w("DreambandClient", "Write input success: " + bytes.toString()),
            throwable -> Log.w("DreambandClient", "Write input failure: " + throwable)
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
        .subscribe(
                buffer -> commandProcessor.processCommandResponse(buffer.array()),
                throwable -> Log.w("DreambandClient", "Read command response failure: " + throwable)
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

        Log.w("DreambandClient", "setConnectionState: " + _connectionState);

        if (connectionListener != null && connectionState != _connectionState){

            connectionState = _connectionState;
            connectionListener.onConnectionStateChange(connectionState);
        }

    }

    private void onScanResult(ScanResult bleScanResult) {

        Log.w("DreambandClient", "Device found: " + bleScanResult.getBleDevice().getMacAddress());

        int existingIndex = scanResults.indexOf(bleScanResult);

        if (existingIndex != -1){

            scanResults.set(existingIndex, bleScanResult);
        }
        else {

            scanResults.add(bleScanResult);
        }

        if (autoConnect || connectionState == ConnectionState.RECONNECTING){

            if (connectionState == ConnectionState.RECONNECTING && !bleScanResult.getBleDevice().equals(rxBleDevice)){

                return;
            }

            connectAndSubscribeToNotifications(bleScanResult.getBleDevice());
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

        Log.w("DreambandClient", "onConnectionStateChange: " + connectionState.toString());

        switch (connectionState){

            case CONNECTING:
                setConnectionState(ConnectionState.CONNECTING);
                break;

            case CONNECTED:
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

        Log.w("DreambandClient", "Char: " + charString + " | value: " + value);

        switch (charString){

            case Constants.EVENT_NOTIFIED_UUID_STRING:
            case Constants.EVENT_INDICATED_UUID_STRING:

                int eventId = value[0];
                long flags = Utility.getUnsignedInt32(value, 1);

                Log.w("DreambandClient", "Event: " + eventId + " flags: " + flags);

                break;

            case Constants.COMMAND_OUTPUT_INDICATED_UUID_STRING:
            case Constants.COMMAND_OUTPUT_NOTIFIED_UUID_STRING:

                commandProcessor.processCommandOutput(value);
                break;

            case Constants.COMMAND_STATUS_UUID_STRING:

                if (value[0] == 2 || value[0] == 3){

                    commandProcessor.setCommandState(value[0] == 2 ?
                            CommandProcessor.CommandState.RESPONSE_OBJECT_READY : CommandProcessor.CommandState.RESPONSE_TABLE_READY, value[1]
                    );

                    readCommandResponse(value[1]);
                }
                else if (value[0] == 0){

                    commandProcessor.setCommandState(CommandProcessor.CommandState.IDlE, value[1]);
                }
                else if (value[0] == 4){

                    commandProcessor.requestInput();
                }

                break;

            default:
                Log.w("DreambandClient", "Unknown Char: " + charString);
                break;

        }
    }

    private void onAuroraNotificationError(Throwable throwable){
        Log.w("DreambandClient", "Notification failure: " + throwable);
    }

}
