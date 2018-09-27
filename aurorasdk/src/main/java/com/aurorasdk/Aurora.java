package com.aurorasdk;

import android.content.Context;
import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


public class Aurora implements AuroraBleCallbacks {

    public enum ConnectionState {
        IDLE, SCANNING, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, RECONNECTING
    }

    public enum ErrorType {
        NO_BLE_ADAPTER, BLE_DISABLED, NO_PERMISSIONS, SCAN_ERROR, DEVICE_NOT_SUPPORTED,
        CONNECTION_ERROR, NOTIFICATION_ERROR, ATTRIBUTE_WRITE_ERROR, ATTRIBUTE_READ_ERROR
    }

    public interface ConnectionListener{
        void onConnectionStateChange(ConnectionState connectionState);
    }

    public interface ScanListener{
        void onScanResultsChange(List<BluetoothDevice> scanResults);
    }

    public interface SessionsListListener {

        void onSessionsListReady(List<String> sessions);
    }

    private ScanListener scanListener;

    private AuroraBleScanner deviceScanner;
    private AuroraBleConnectionManager connectionManager;

    public interface EventListener{
        void onEvent(Event event);
    }
    public interface ErrorListener{
        void onError(ErrorType errorType, String errorMessage);
    }

    private ConnectionListener connectionListener;
    private Command.CommandCompletionListener commandCompletionListener;
    private ErrorListener errorListener;

    private ConnectionState connectionState;
    private EventListener eventListener;

    private boolean autoConnect;
    private boolean explicitDisconnect;

    private CommandProcessor commandProcessor;

    private static Aurora instance;

    private final EnumSet<Event.EventType> enabledEventTypes = EnumSet.noneOf(Event.EventType.class);

    private Aurora(){

        connectionState = ConnectionState.IDLE;
    }

    public static Aurora getInstance(){

        if (instance == null) {

            instance = new Aurora();
        }

        return instance;
    }

    public static Aurora create(Context context, ConnectionListener connectionListener, ErrorListener errorListener){

        Aurora instance = getInstance();

        instance.deviceScanner = new AuroraBleScanner(context, instance::onScanStatusChange);
        instance.connectionManager = new AuroraBleConnectionManager(context);
        instance.connectionManager.setGattCallbacks(instance);

        instance.commandProcessor = new CommandProcessor(
                instance.connectionManager::sendCommand,
                instance.connectionManager::writeCommandInput
        );

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

    public void setDebug(boolean debug){

        Logger.setDebug(debug);
    }

    public void startScan(ScanListener scanListener){

        if (connectionState == ConnectionState.IDLE || connectionState == ConnectionState.DISCONNECTED) {

            this.scanListener = scanListener;
            deviceScanner.startScan();

            boolean reconnecting = (connectionState == ConnectionState.DISCONNECTED) && !explicitDisconnect;

            setConnectionState(reconnecting ? ConnectionState.RECONNECTING : ConnectionState.SCANNING);
        }
    }

    public void startScan(){

        startScan(null);
    }

    public void stopScan(){

        scanListener = null;

        deviceScanner.stopScan();

        //move back to idle state if we haven't established a connection
        if (connectionState == ConnectionState.SCANNING || connectionState == ConnectionState.RECONNECTING){

            setConnectionState(ConnectionState.IDLE);
        }
    }


    public void connect(BluetoothDevice device, Command.CommandCompletionListener commandCompletionListener){

        this.commandCompletionListener = commandCompletionListener;

        if (device == null){

            autoConnect = true;
            this.commandCompletionListener = commandCompletionListener;

            startScan();
        }
        else {

            autoConnect = false;
            connectionManager.connect(device);
        }
    }


    public void connect(Command.CommandCompletionListener commandCompletionListener){

        connect(null, commandCompletionListener);
    }

    public void connect(){

        connect(null, null);
    }


    public void disconnect(){

        autoConnect = false;
        explicitDisconnect = true;

        stopScan();
        connectionManager.disconnect();
    }


    public boolean isConnected(){

        return connectionState == ConnectionState.CONNECTED;
    }


    public Command queueCommand(Command command, Command.CommandCompletionListener listener) throws UnsupportedOperationException {

        Logger.d("queueCommand: " + command.getCommandString());

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

    public void getSessionsList(String filter, SessionsListListener listener){

        final List<String> sessions = new ArrayList<String>();

        queueCommand("sd-dir-read sessions 0 " + filter, (Command dirReadCmd) -> {

            if (dirReadCmd.hasError() || !dirReadCmd.isTable()){

                listener.onSessionsListReady(sessions);
                return;
            }

            CountDownLatch countDownLatch = new CountDownLatch(dirReadCmd.getResponseTable().size());

            for (Map<String, String> s : dirReadCmd.getResponseTable()){

                if (s.containsKey("name")){

                    String sessionName = s.get("name") + "/session.txt";

                    queueCommand("sd-file-info " + sessionName, (Command cmd) -> {

                        if (!cmd.hasError() && !cmd.isTable()){

                            Long sessionSize = cmd.getResponseValueAsLong("size");

                            if (sessionSize > 0 && sessionSize <= 256*1024){

                                sessions.add(sessionName);
                            }
                        }

                        countDownLatch.countDown();

                        if (countDownLatch.getCount() == 0){
                            listener.onSessionsListReady(sessions);
                        }

                    });
                }
                else {

                    countDownLatch.countDown();

                    if (countDownLatch.getCount() == 0){
                        listener.onSessionsListReady(sessions);
                    }
                }
            }
        });
    }


    //TODO: remove this when firmware 2.X is deprecated
    public void useIndicationsForCommandOutput(){

        connectionManager.useIndicationsForCommandOutput();
    }


    /* Helper methods
    ------------------------------------------------------------------------------------------------
    */

    private void setConnectionState(ConnectionState _connectionState){

        Logger.d("setConnectionState: " + _connectionState.name());

        if (connectionListener != null && connectionState != _connectionState){

            connectionState = _connectionState;
            connectionListener.onConnectionStateChange(connectionState);
        }

    }

    private void sendError(ErrorType errorType, String errorMessage){

        if (errorListener != null){

            errorListener.onError(errorType, errorMessage);
        }

        Logger.e(errorType.name() + ": " + errorMessage);
    }


    /* Event Handlers
    ------------------------------------------------------------------------------------------------
    */

    private void onScanStatusChange(AuroraBleScanner.ScanStatusType scanStatus, List<BluetoothDevice> scanResults) {

        if (scanStatus == AuroraBleScanner.ScanStatusType.SCAN_RESULTS_CHANGE){

            if (scanListener != null){
                scanListener.onScanResultsChange(scanResults);
            }

            if (autoConnect){

                connectionManager.connect(scanResults.get(0));
            }

        }
        else {

            switch (scanStatus){


                case ERROR_NO_ADAPTER:
                    sendError(ErrorType.NO_BLE_ADAPTER, "No BLE adapter is available.");
                    break;

                case ERROR_ADAPTER_DISABLED:
                    sendError(ErrorType.BLE_DISABLED, "BLE not enabled");
                    break;

                case ERROR_NO_PERMISSIONS:
                    sendError(ErrorType.NO_PERMISSIONS, "BLE scanning failed. Requires ACCESS_FINE_LOCATION permission.");
                    break;

                case ERROR_UNKNOWN:
                default:
                    sendError(ErrorType.SCAN_ERROR, "Unknown scanning error.");
                    break;
            }
        }
    }

    @Override
    public void onDeviceReady(final BluetoothDevice device) {

        setConnectionState(ConnectionState.CONNECTED);
        stopScan();
    }

    @Override
    public void onDeviceNotSupported(final BluetoothDevice device) {

        setConnectionState(ConnectionState.IDLE);
        sendError(ErrorType.DEVICE_NOT_SUPPORTED, "Device not supported.");
    }

    @Override
    public void onDeviceConnecting(final BluetoothDevice device) {

        if (connectionState != ConnectionState.RECONNECTING){
            setConnectionState(ConnectionState.CONNECTING);
        }

    }

    @Override
    public void onDeviceDisconnecting(final BluetoothDevice device) {

        setConnectionState(ConnectionState.DISCONNECTING);
    }

    @Override
    public void onDeviceDisconnected(final BluetoothDevice device) {

        setConnectionState(ConnectionState.DISCONNECTED);
    }

    @Override
    public void onLinklossOccur(final BluetoothDevice device) {

        if (connectionState == ConnectionState.CONNECTED) {
            setConnectionState(ConnectionState.RECONNECTING);
        }
    }

    @Override
    public void onCommandStatusChange(byte statusByte, byte infoByte){

        //this gets called a lot...
        //we hard code the enum values here
        //to avoid any unnecessary lookups or
        //array copies and order by the most frequently
        //emitted status values

        if (statusByte == 4){

            commandProcessor.setCommandState(CommandProcessor.CommandState.INPUT_REQUESTED, infoByte & 0xFF);
        }
        else if (statusByte == 2 || statusByte == 3){

            commandProcessor.setCommandState(statusByte == 2 ?
                    CommandProcessor.CommandState.RESPONSE_OBJECT_READY : CommandProcessor.CommandState.RESPONSE_TABLE_READY, infoByte
            );

            connectionManager.readCommandResponse(infoByte & 0xFF);
        }
        else if (statusByte == 0){

            commandProcessor.setCommandState(CommandProcessor.CommandState.IDlE, infoByte);
        }

    }

    @Override
    public void onCommandOutput(byte[] data){

        commandProcessor.processCommandOutput(data);
    }

    @Override
    public void onCommandResponse(String responseLine){

        commandProcessor.processCommandResponseLine(responseLine);
    }

    @Override
    public void onAuroraEvent(int eventId, long flags){

        if (eventListener != null) {

            try {

                eventListener.onEvent(new Event(eventId, flags));

            } catch (Exception e) {

                Logger.w("Unknown event received: " + eventId);
            }
        }
    }

    @Override
    public void onError(final BluetoothDevice device, final String message, final int errorCode) {
        Logger.d("error: " + message);
    }

}
