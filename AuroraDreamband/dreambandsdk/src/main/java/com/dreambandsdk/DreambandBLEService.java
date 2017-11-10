package com.dreambandsdk;

/**
 * Created by seanf on 9/9/2017.
 */
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.dreambandsdk.profile.Profile;
import com.dreambandsdk.profile.ProfileManager;
import com.dreambandsdk.profile.ProfileSetting;
import com.dreambandsdk.request.DreambandRequest;
import com.dreambandsdk.request.ReadProfileRequest;
import com.dreambandsdk.request.WriteProfileRequest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class DreambandBLEService extends Service {

    /*************** Constants **********************/
    private final static String TAG = DreambandBLEService.class.getSimpleName();

    public static final String  EXTRA_NAME = "name";
    public static final String  EXTRA_ADDRESS = "address";

    public static final boolean SCAN_ON_START = false;

    /*************** Data Fields **********************/
    enum BleState {IDLE, WAIT_REQUEST_RESP, DATA_RX, DATA_TX, CMD_DATA_TX, STATUS_CMD_START, STATUS_CMD_END}
    enum ConnectionState {DISCONNECTED, CONNECTING, CONNECTED, SEARCHING}

    private BleState _bleState;
    private ConnectionState _connectionState;
    private boolean _isInitialized = false;
    private Queue<DreambandRequest> _commandQueue;
    private Queue<String> _notificationUUIDs;
    private Queue<Integer> _readQueue;
    private ByteBuffer _bleBuffer, _bleRxBuffer;
    private ArrayList<CharacteristicProperties> _characteristicProperties;
    private String _deviceName;
    private String _deviceAddress;
    private BleAdapterService _bluetoothLeService;
    private Thread _serviceThread;
    private CountDownLatch _serviceThreadLatch;
    private Boolean _serviceThreadDone;
    private boolean _scanning;
    private int _bluetoothState = BluetoothAdapter.STATE_OFF;

    private long _startedSearchingTime;
    private long _requestSentTime;
    private boolean _requestTimeoutEnabled = true;
    private long SERVICE_DISC_TIMEOUT_MS = 5 * 1000;
    private long SEARCHING_TIMEOUT_MS = 20 * 1000;
    private long REQUEST_TIMEOUT_MS = 180 * 1000;


    /*************** Singleton Service Instance **********************/
    private static DreambandBLEService _ourInstance = new DreambandBLEService();
    public static DreambandBLEService getInstance() {
        return _ourInstance;
    }

    /********* Start Service Setup ***********/
    private final IBinder mBinder = new LocalBinder();
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            _bluetoothLeService = ((BleAdapterService.LocalBinder) service).getService();
            _bluetoothLeService.setActivityHandler(mMessageHandler);
            // Connect to Dreamband device
            if (_bluetoothLeService != null && !_deviceAddress.isEmpty()) {
                if (!_bluetoothLeService.connect(_deviceAddress)) {
                    Log.d(TAG, "onConnect: failed to connect");
                }
            } else {
                Log.d(TAG, "Connected to internal BleAdapterService, no device to connect to.");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            _bluetoothLeService = null;
            _connectionState = ConnectionState.DISCONNECTED;
        }
    };

    public DreambandBLEService() {
        // All setup is done on onCreate() and onStartCommand()
    }

    /**
     * This method provides a way to listen to all the different callbacks of the Service
     * @return IntentFilter with all the proper actions to work with the BroadcastReceiver
     */
    public static IntentFilter makeNxtMobileIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DreambandResp.RESP_DEVICE_CONNECTED);
        intentFilter.addAction(DreambandResp.RESP_DEVICE_NAME);
        intentFilter.addAction(DreambandResp.RESP_DEVICE_ADDRESS);
        intentFilter.addAction(DreambandResp.RESP_DEVICE_DISCONNECTED);

        intentFilter.addAction(DreambandResp.EVENT);
        intentFilter.addAction(DreambandResp.RESP_COMMAND);
        intentFilter.addAction(DreambandResp.RESP_UNSYNCED_SESSION_COUNT);
        intentFilter.addAction(DreambandResp.RESP_RENAME_SYNCED_SESSION);
        intentFilter.addAction(DreambandResp.RESP_REMOVE_EMPTY_SESSION);
        intentFilter.addAction(DreambandResp.RESP_OS_VERSION);
        intentFilter.addAction(DreambandResp.RESP_BATTERY_LEVEL);
        intentFilter.addAction(DreambandResp.RESP_IS_PROFILE_LOADED);
        intentFilter.addAction(DreambandResp.RESP_SHUTDOWN);
        intentFilter.addAction(DreambandResp.RESP_OBSERVE_EVENTS);
        intentFilter.addAction(DreambandResp.RESP_DISABLE_EVENTS);
        intentFilter.addAction(DreambandResp.RESP_HELP);
        intentFilter.addAction(DreambandResp.RESP_BUZZ);
        intentFilter.addAction(DreambandResp.RESP_LIST_PROFILES);
        intentFilter.addAction(DreambandResp.RESP_WRITE_PROFILE);
        intentFilter.addAction(DreambandResp.RESP_READ_PROFILE);
        intentFilter.addAction(DreambandResp.RESP_UPDATE_PROFILE);
        intentFilter.addAction(DreambandResp.RESP_LOAD_PROFILE);
        intentFilter.addAction(DreambandResp.RESP_UNLOAD_PROFILE);

        return intentFilter;
    }

    public class LocalBinder extends Binder {
        public DreambandBLEService getService() {
            return DreambandBLEService.this;
        }
    }

    // Internal thread for executing commands off the Command Queue
    private Runnable serviceRunnable = new Runnable() {
        @Override
        public void run() {

            // Handle commands from the queue
            while(!_serviceThreadDone)
            {
                if (_connectionState == ConnectionState.CONNECTED)
                {
                    // Make sure we're not waiting on a response or sending data
                    if (_commandQueue.size() > 0 && (_bleState == BleState.IDLE))
                    {
                        // Send the next command request
                        DreambandRequest req = _commandQueue.peek();
                        if (req != null) {
                            // Copy the command data into the _bleBuffer
                            byte[] cmdBytes = req.getRequestData();
                            if (cmdBytes != null && cmdBytes.length > 0) {
                                _bleBuffer = ByteBuffer.allocate(cmdBytes.length);
                                _bleBuffer.put(cmdBytes);
                                _bleBuffer.flip();
                            }

                            // Write the status byte, indicating start of command
                            _bleState = BleState.STATUS_CMD_START;
                            cmdBytes = new byte[] { Constants.CommandState.IDLE.value()};

                            // Save the time the request was sent
                            _requestSentTime = SystemClock.elapsedRealtime();
                            sendCommandStatusData(cmdBytes);
                        }
                    }
                }
                else if (_connectionState == ConnectionState.DISCONNECTED)
                {
                    // Search for dreamband for reconnection
                    // TODO: Handle scan on disconnect
                    /*Log.d(TAG, "Searching for device: " + _deviceAddress);
                    scanLeDevice(true);
                    _connectionState = ConnectionState.SEARCHING;*/
                }
                else if (_connectionState == ConnectionState.SEARCHING)
                {
                    // Determine if we have exceeded the scan threshold
                    long timeElapsed = SystemClock.elapsedRealtime() - _startedSearchingTime;
                    if (timeElapsed > SEARCHING_TIMEOUT_MS) {
                        // Searching for longer than threshold, restart scan
                        Log.d(TAG, "Searching for device: " + _deviceAddress);
                        scanLeDevice(true);
                    }
                }
                try {
                    // Wait for a command to process
                    _serviceThreadLatch.await(2000, TimeUnit.MILLISECONDS);
                    _serviceThreadLatch = new CountDownLatch(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            synchronized (this) {
                notify();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) { return mBinder;  }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Bluetooth
        initialiseCharacteristicProperties();
        _bleState = BleState.IDLE;
        _connectionState = ConnectionState.DISCONNECTED;
        _bleBuffer = ByteBuffer.allocate(1024);
        _bleBuffer.order(ByteOrder.LITTLE_ENDIAN);
        _bleRxBuffer = ByteBuffer.allocate(1024);
        _bleRxBuffer.order(ByteOrder.LITTLE_ENDIAN);
        _commandQueue = new ConcurrentLinkedQueue<DreambandRequest>();
        _notificationUUIDs = new ConcurrentLinkedQueue<String>();
        _readQueue = new ConcurrentLinkedQueue<Integer>();
        _serviceThreadLatch = new CountDownLatch(1);
        _serviceThreadDone = false;
        // Create new thread to handle command requests
        _serviceThread = new Thread(serviceRunnable);
        _serviceThread.start();

        // Connect to the Bluetooth smart service
        Intent gattServiceIntent = new Intent(this, BleAdapterService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        if (BluetoothAdapter.getDefaultAdapter() != null &&
                BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            _bluetoothState = BluetoothAdapter.STATE_ON;
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateBroadcastReceiver, filter);
    }

    @Override
    public void onDestroy() {
        scanLeDevice(false);
        unregisterReceiver(bluetoothStateBroadcastReceiver);
        if (_serviceThread != null) {
            synchronized(serviceRunnable) {
                _serviceThreadDone = true;
                try {
                    serviceRunnable.wait();
                } catch (InterruptedException e) {
                    Log.d(TAG, "onDestroy exception on serviceRunnable" + e.toString());
                }
            }
        }
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Save device name and address if passed in
        Log.d(TAG, "onStartCommand state: " + _connectionState);
        if (_connectionState == ConnectionState.CONNECTED)
            return START_NOT_STICKY;

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        // Allow connect by address
        _deviceName = intent.getStringExtra(EXTRA_NAME);
        _deviceAddress = intent.getStringExtra(EXTRA_ADDRESS);

        if (_deviceAddress == null || _deviceAddress.isEmpty()) {
            _deviceAddress = "";
            _deviceName = "";
            if (SCAN_ON_START) {
                // Start scanning for the dreamband
                Log.d(TAG, "Scanning for dreamband device");
                scanLeDevice(true);
            }
        } else {
            // Connect by addresss
            if (!_bluetoothLeService.connect(_deviceAddress)) {
                Log.d(TAG, "onConnect: failed to connect");
            }
        }

        return START_NOT_STICKY;
    }

    // Store characteristic properties
    private void initialiseCharacteristicProperties() {
        CharacteristicProperties char_props = null;
        _characteristicProperties = new ArrayList<CharacteristicProperties>();
        // Only read, write, write no response and notify are currently supported
        char_props = new CharacteristicProperties(Constants.DREAMBAND_SERVICE_UUID, Constants.COMMAND_DATA_UUID);
        char_props.setSupports_read(true);
        char_props.setSupports_write(true);
        _characteristicProperties.add(char_props);

        char_props = new CharacteristicProperties(Constants.DREAMBAND_SERVICE_UUID, Constants.COMMAND_STATUS_UUID);
        char_props.setSupports_read(true);
        char_props.setSupports_write(true);
        char_props.setSupports_indicate(true);
        _characteristicProperties.add(char_props);

        char_props = new CharacteristicProperties(Constants.DREAMBAND_SERVICE_UUID, Constants.EVENT_NOTIFIED_UUID);
        char_props.setSupports_read(true);
        char_props.setSupports_notify(true);
        _characteristicProperties.add(char_props);

        char_props = new CharacteristicProperties(Constants.DREAMBAND_SERVICE_UUID, Constants.COMMAND_OUTPUT_NOTIFIED_UUID);
        char_props.setSupports_read(true);
        char_props.setSupports_notify(true);
        _characteristicProperties.add(char_props);

        char_props = new CharacteristicProperties(Constants.DREAMBAND_SERVICE_UUID, Constants.COMMAND_OUTPUT_INDICATED_UUID);
        char_props.setSupports_read(true);
        char_props.setSupports_notify(true);
        _characteristicProperties.add(char_props);

        char_props = new CharacteristicProperties(Constants.DREAMBAND_SERVICE_UUID, Constants.STREAM_DATA_NOTIFIED_UUID);
        char_props.setSupports_read(true);
        char_props.setSupports_notify(true);
        _characteristicProperties.add(char_props);
    }

    /********* Internal Dreamband Service methods ***********/
    // Reads a characteristic UUID from _notificationUUIDs queue and enables the BLE notification
    private void enableNotifications()  {
        if (!_isInitialized && !_notificationUUIDs.isEmpty()) {
            // Enable Dreamband Service Command Characteristic Notifications
            // Note: Only one characteristic notification can be written at a time, therefore
            // we need to wait until the write notification is received to enable remaining notifications
            String notifUUID = _notificationUUIDs.peek();
            CharacteristicProperties char_props = new CharacteristicProperties(Constants.DREAMBAND_SERVICE_UUID, notifUUID);
            int char_props_inx = _characteristicProperties.indexOf(char_props);
            if (char_props_inx == -1) {
                Log.e(TAG, "Error:Could not find characteristic properties");
                return;
            }
            char_props = _characteristicProperties.get(char_props_inx);
            if (!char_props.isSupports_notify() && !char_props.isSupports_indicate()) {
                Log.e(TAG, "Error:Notifications not supported");
                return;
            }

            if (_bluetoothLeService != null) {
                if (!_bluetoothLeService.setNotificationsState(Utility.normaliseUUID(Constants.DREAMBAND_SERVICE_UUID), Utility.normaliseUUID(notifUUID), true, char_props.isSupports_indicate())) {
                    Log.e(TAG, "Failed to set " + notifUUID + " notifications state");
                }
            } else {
                Log.e(TAG, "Failed to set " + notifUUID + " notifications state");
            }
        }
    }

    // Add a request to the queue and notify the sending thread
    private DreambandResp.ErrorCode issueQueueRequest(DreambandRequest request) {
        Log.d(TAG, "Adding command to queue: " + request.getClass());
        DreambandResp.ErrorCode result = DreambandResp.ErrorCode.ERROR;

        // Add request to command queue
        if (request != null && _commandQueue.offer(request)) {
            // Notify the command queue a new command is ready
            if (_serviceThreadLatch.getCount() > 0)
                _serviceThreadLatch.countDown();

            result = DreambandResp.ErrorCode.SUCCESS;
        }

        return result;
    }

    // Send an Intent that will be received by any listening Activities
    private void broadcast(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Broadcast receiver for BleAdapterService notifications
    private final BroadcastReceiver bluetoothStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            _bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);
        }
        }
    };

    // Scan for BLE Device
    // enable = true: start scan, enable = false: stop scan
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Make sure we're disconnected
            if (_bluetoothLeService != null)
                _bluetoothLeService.disconnect();
            // Stop scanning before we start again
            BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(mNordicLeScanCallback);
            _connectionState = ConnectionState.SEARCHING;
            _scanning = true;
            // Save the time we started searching
            _startedSearchingTime = SystemClock.elapsedRealtime();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setUseHardwareBatchingIfSupported(false).build();
            // Filter only Dreamband service devices
            List<ScanFilter> filters = new ArrayList<>();
            filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(Utility.normaliseUUID(Constants.DREAMBAND_SERVICE_UUID))).build());
            // before starting scan check bt status
            if (_bluetoothState == BluetoothAdapter.STATE_ON) {
                scanner.startScan(filters, settings, mNordicLeScanCallback);
            }
        } else {
            _scanning = false;
            BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(mNordicLeScanCallback);
        }
    }

    // Callback for BLE scan results
    private ScanCallback mNordicLeScanCallback =
        new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                super.onScanResult(callbackType, result);

                _deviceName = result.getDevice().getName();
                _deviceAddress = result.getDevice().getAddress();
                if (_deviceName != null && !_deviceName.isEmpty() &&
                    _deviceAddress != null && !_deviceAddress.isEmpty() && _deviceAddress.length() > 4) {
                    scanLeDevice(false);
                    _connectionState = ConnectionState.CONNECTING;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Connect to the device and stop scanning
                            if (_bluetoothLeService != null) {
                                if (!_bluetoothLeService.connect(_deviceAddress)) {
                                    Log.d(TAG, "onConnect: failed to connect");
                                }
                            } else {
                                Log.d(TAG, "onConnect: _bluetoothLeService=null");
                            }
                        }
                    }, 100);
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };

    // Sends data over the Command Status Characteristic
    private void sendCommandStatusData(byte[] dataToSend)
    {
        CharacteristicProperties char_props = new CharacteristicProperties(Constants.DREAMBAND_SERVICE_UUID, Constants.COMMAND_STATUS_UUID);
        int char_props_inx = _characteristicProperties.indexOf(char_props);
        if (char_props_inx == -1) {
            Log.e(TAG, "Error:Could not find characteristic properties");
            return;
        }
        char_props = _characteristicProperties.get(char_props_inx);
        if (!char_props.isSupports_write() && !char_props.isSupports_write_without_response()) {
            Log.e(TAG, "Error:Writing to characteristic not allowed");
            return;
        }

        if (_bluetoothLeService != null) {
            _bluetoothLeService.writeCharacteristic(Utility.normaliseUUID(Constants.DREAMBAND_SERVICE_UUID), Utility.normaliseUUID(Constants.COMMAND_STATUS_UUID), dataToSend);
        } else {
            Log.e(TAG, "Error: bluetoothLeService is null when attempting to send command status data");
        }
    }

    // Reads data from internal _bleBuffer and sends in 20 byte packets to the Dreamband device over the Command Data Characteristic
    private void sendCommandData(BleState stateAfterWrite)
    {
        CharacteristicProperties char_props = new CharacteristicProperties(Constants.DREAMBAND_SERVICE_UUID, Constants.COMMAND_DATA_UUID);
        int char_props_inx = _characteristicProperties.indexOf(char_props);
        if (char_props_inx == -1) {
            Log.e(TAG, "Error:Could not find characteristic properties");
            return;
        }
        char_props = _characteristicProperties.get(char_props_inx);
        if (!char_props.isSupports_write() && !char_props.isSupports_write_without_response()) {
            Log.e(TAG, "Error:Writing to characteristic not allowed");
            return;
        }

        if (_bluetoothLeService != null) {
            byte[] value = new byte[Constants.BLE_MTU];
            int dataSize = Constants.BLE_MTU;
            if ((_bleBuffer.limit() - _bleBuffer.position()) < Constants.BLE_MTU) {
                dataSize = _bleBuffer.limit() - _bleBuffer.position();
                // After the write notification is received, write status char to indicate completion
                _bleState = stateAfterWrite;
            }
            _bleBuffer.get(value, 0, dataSize);

            if (_bleBuffer.limit() == _bleBuffer.position()) {
                // After the write notification is received, write status char to indicate completion
                _bleState = stateAfterWrite;
            }

            _bluetoothLeService.writeCharacteristic(Utility.normaliseUUID(Constants.DREAMBAND_SERVICE_UUID), Utility.normaliseUUID(Constants.COMMAND_DATA_UUID), value);
        } else {
            Log.e(TAG, "Error: bluetoothLeService is null when attempting to send command data");
        }
    }

    // Reads data from the Dreamband device over the Command Data Characteristic
    private void readCommandData()
    {
        CharacteristicProperties char_props = new CharacteristicProperties(Constants.DREAMBAND_SERVICE_UUID, Constants.COMMAND_DATA_UUID);
        int char_props_inx = _characteristicProperties.indexOf(char_props);
        if (char_props_inx == -1) {
            Log.e(TAG, "Error:Could not find characteristic properties");
            return;
        }
        char_props = _characteristicProperties.get(char_props_inx);
        if (!char_props.isSupports_read()) {
            Log.e(TAG, "Error:Reading characteristic not allowed");
            return;
        }

        if (_bluetoothLeService != null) {
            _bluetoothLeService.readCharacteristic(Utility.normaliseUUID(Constants.DREAMBAND_SERVICE_UUID), Utility.normaliseUUID(Constants.COMMAND_DATA_UUID));
        } else {
            Log.e(TAG, "Error: bluetoothLeService is null when attempting to read command data");
        }
    }

    // Handles messages read from the BleAdapterService
    private Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Bundle bundle;
            String service_uuid="";
            String characteristic_uuid="";
            String descriptor_uuid="";
            byte[] b=null;
            try {
                switch (msg.what) {
                    case BleAdapterService.GATT_BONDED: {
                        if (_serviceThreadLatch.getCount() > 0) {
                            _serviceThreadLatch.countDown();
                        }
                    }
                    break;
                    case BleAdapterService.GATT_CONNECTED: {
                        if (_serviceThreadLatch.getCount() > 0) {
                            _serviceThreadLatch.countDown();
                        }
                    }
                    break;
                    case BleAdapterService.GATT_DISCONNECT: {
                        // Clean up state variables
                        _bleState = BleState.IDLE;
                        _isInitialized = false;
                        _deviceAddress = "";
                        _deviceName = "";
                        _connectionState = ConnectionState.DISCONNECTED;
                        // Remove any commands in the queue
                        _commandQueue.clear();
                        _notificationUUIDs.clear();
                        _readQueue.clear();
                        // Broadcast Disconnected intent
                        Intent intent = new Intent(DreambandResp.RESP_DEVICE_DISCONNECTED);
                        intent.putExtra(DreambandResp.RESP_VALID, true);
                        broadcast(intent);
                    }
                    break;
                    case BleAdapterService.GATT_SERVICES_DISCOVERED: {

                        // Queue all of the UUIDs to subscribe to for notifications
                        _notificationUUIDs.clear();
                        _notificationUUIDs.add(Constants.COMMAND_STATUS_UUID);
                        _notificationUUIDs.add(Constants.EVENT_NOTIFIED_UUID);
                        _notificationUUIDs.add(Constants.COMMAND_OUTPUT_NOTIFIED_UUID);
                        _notificationUUIDs.add(Constants.STREAM_DATA_NOTIFIED_UUID);

                        enableNotifications();
                    }
                    break;
                    case BleAdapterService.GATT_CHARACTERISTIC_READ: {
                        bundle = msg.getData();
                        service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                        characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                        b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                        Log.d(TAG, "Handler processing characteristic read result: " + characteristic_uuid + ", Value=" + Utility.byteArrayAsHexString(b));

                        if (characteristic_uuid.equalsIgnoreCase(Utility.normaliseUUID(Constants.COMMAND_DATA_UUID))) {
                            // Append data to buffer and determine if there is more data to read
                            if (_bleRxBuffer.position() + b.length > _bleRxBuffer.limit()) {
                                Log.e(TAG, "Attempted to write more data to BLE RX buffer than allocated");
                                _bleRxBuffer.put(b, 0, _bleRxBuffer.limit() - _bleRxBuffer.position());
                            } else
                                _bleRxBuffer.put(b);
                            if (_bleRxBuffer.position() >= _bleRxBuffer.limit()) {
                                // Append data to command in queue
                                _bleRxBuffer.flip();
                                DreambandRequest req = _commandQueue.peek();
                                req.responseData(_bleRxBuffer.array());
                                Log.i(TAG, "<<<< READ RESPONSE");

                                if (_readQueue.isEmpty()) {
                                    _bleState = BleState.WAIT_REQUEST_RESP;
                                    if (req.is_complete())
                                        handleComplete();
                                }
                                else {
                                    // Issue a read command data for the number of bytes at the top of the queue
                                    Integer readCount = _readQueue.poll();
                                    if (readCount == null) {
                                        Log.d(TAG, "readCount is null");
                                        if (_readQueue.isEmpty()) {
                                            _bleState = BleState.WAIT_REQUEST_RESP;
                                            if (req.is_complete())
                                                handleComplete();
                                        }
                                    } else {
                                        _bleRxBuffer = ByteBuffer.allocate(readCount).order(ByteOrder.LITTLE_ENDIAN);
                                        _bleState = BleState.DATA_RX;
                                        readCommandData();
                                    }
                                }
                            } else {
                                // There is more data to read
                                readCommandData();
                            }
                        }
                    }
                    break;
                    case BleAdapterService.GATT_CHARACTERISTIC_WRITTEN: {
                        bundle = msg.getData();
                        service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                        characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                        if (characteristic_uuid.equalsIgnoreCase(Utility.normaliseUUID(Constants.COMMAND_DATA_UUID)))
                        {
                            if (_bleState == BleState.DATA_TX) {
                                // There is more data to send
                                sendCommandData(BleState.WAIT_REQUEST_RESP);
                            } else if (_bleState == BleState.CMD_DATA_TX) {
                                // There is more data to send
                                sendCommandData(BleState.STATUS_CMD_END);
                            } else if (_bleState == BleState.STATUS_CMD_END) {
                                // Command finished, send status byte indicating end of command
                                byte[] cmdBytes = new byte[]{ Constants.CommandState.EXECUTE.value() };
                                sendCommandStatusData(cmdBytes);
                            }
                        }
                        else if (characteristic_uuid.equalsIgnoreCase(Utility.normaliseUUID(Constants.COMMAND_STATUS_UUID))) {
                            if (_bleState == BleState.STATUS_CMD_START) {
                                // Finished sending the status byte indicating start of command
                                // Start sending the ascii command data
                                _bleState = BleState.CMD_DATA_TX;
                                sendCommandData(BleState.STATUS_CMD_END);
                            } else if (_bleState == BleState.STATUS_CMD_END) {
                                // Command finished writing, wait for response
                                _bleState = BleState.WAIT_REQUEST_RESP;
                            }
                        }
                    }
                    break;
                    case BleAdapterService.GATT_DESCRIPTOR_WRITTEN:
                        bundle = msg.getData();
                        service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                        characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                        descriptor_uuid = bundle.getString(BleAdapterService.PARCEL_DESCRIPTOR_UUID);

                        // Pop the last notification from the queue
                        _notificationUUIDs.poll();
                        if (!_notificationUUIDs.isEmpty()) {
                            enableNotifications();
                        } else {
                            // Finished enabling all notifications, broadcast connected intent
                            _isInitialized = true;
                            _connectionState = ConnectionState.CONNECTED;
                            sendConnectedBroadcast();
                        }
                        break;
                    case BleAdapterService.NOTIFICATION_RECEIVED:
                        Log.d(TAG, "Handler received notification");
                        bundle = msg.getData();
                        service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                        characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                        b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                        Log.d(TAG, "Value=" + Utility.byteArrayAsHexString(b));

                        // Determine which characteristic was written and handle appropriately
                        if (characteristic_uuid.equalsIgnoreCase(Utility.normaliseUUID(Constants.COMMAND_OUTPUT_INDICATED_UUID)) ||
                            characteristic_uuid.equalsIgnoreCase(Utility.normaliseUUID(Constants.COMMAND_OUTPUT_NOTIFIED_UUID)))
                        {
                            commandOutputHandler(b);
                        } else if (characteristic_uuid.equalsIgnoreCase(Utility.normaliseUUID(Constants.COMMAND_STATUS_UUID)))
                        {
                            commandStatusHandler(b);
                        } else if (characteristic_uuid.equalsIgnoreCase(Utility.normaliseUUID(Constants.EVENT_INDICATED_UUID)) ||
                                   characteristic_uuid.equalsIgnoreCase(Utility.normaliseUUID(Constants.EVENT_NOTIFIED_UUID)))
                        {
                            eventHandler(b);
                        } else if (characteristic_uuid.equalsIgnoreCase(Utility.normaliseUUID(Constants.STREAM_DATA_INDICATED_UUID)) ||
                                   characteristic_uuid.equalsIgnoreCase(Utility.normaliseUUID(Constants.STREAM_DATA_NOTIFIED_UUID)))
                        {
                            streamDataHandler(b);
                        }
                        break;
                    case BleAdapterService.MESSAGE:
                        bundle = msg.getData();
                        String text = bundle.getString(BleAdapterService.PARCEL_TEXT);
                        break;
                    case BleAdapterService.ERROR:
                        bundle = msg.getData();
                        String error = bundle.getString(BleAdapterService.PARCEL_ERROR);

                }
            }
            catch(Exception e){
                e.printStackTrace();
                Log.d(TAG, "onConnect: failed to connect");
            }
        }
    };

    // Status data received
    private void commandStatusHandler(byte[] charData)
    {
        // Make sure we are processing a command
        if (_commandQueue.size() == 0 || _commandQueue.peek() == null)
        {
            Log.w(TAG, "commandStatusHandler() executed with no queued commands");
            return;
        }

        Constants.CommandState state = Constants.CommandState.fromValue(charData[0]);
        Log.i(TAG, "commandStatusHandler() state: " + state);
        DreambandRequest req = _commandQueue.peek();
        switch (state) {
            // End of current command
            case IDLE:
                Log.i(TAG, ">>>> IDLE");
                // Make sure we are not still reading data
                req.set_complete(true);
                if (_bleState != BleState.DATA_RX) {
                    handleComplete();
                }
                break;
            // Received response line(s)
            case RESPONSE_OBJECT_RDY:
            case RESPONSE_TABLE_RDY:
                Log.i(TAG, ">>>> READ RESPONSE");
                if (state == Constants.CommandState.RESPONSE_OBJECT_RDY)
                    req.setResponseType(DreambandRequest.ResponseType.OBJECT_RESP);
                else
                    req.setResponseType(DreambandRequest.ResponseType.TABLE_RESP);
                // Second status byte is number of bytes available to read
                int count = charData[1];
                Log.d(TAG, "Response type: " + state + ". Length: " + count);
                // Read count bytes from the Command Data characteristic
                if (_bleState == BleState.WAIT_REQUEST_RESP && _readQueue.isEmpty()) {
                    _bleRxBuffer = ByteBuffer.allocate(count).order(ByteOrder.LITTLE_ENDIAN);
                    _bleState = BleState.DATA_RX;
                    readCommandData();
                } else {
                    _readQueue.add(count);
                }
                break;
            case INPUT_REQUESTED:

                Log.i(TAG, ">>>> INPUT REQUESTED");
                byte[] cmdData = req.getExtraRequestData();
                if (cmdData == null || cmdData.length == 0)
                {
                    Log.i(TAG, "No data to write");
                } else {
                    Log.i(TAG, "Writing command data with " + cmdData.length + " bytes");
                    req.clearExtraRequestData();
                    _bleBuffer = ByteBuffer.allocate(cmdData.length);
                    _bleBuffer.put(cmdData);
                    _bleBuffer.flip();
                    _bleState = BleState.DATA_TX;
                    sendCommandData(BleState.WAIT_REQUEST_RESP);
                }

                Log.i(TAG, "<<<< INPUT REQUESTED");
                break;
        }
    }

    private void handleComplete() {

        // Finish the current command in the queue
        DreambandRequest req = _commandQueue.poll();
        Intent cmdIntent = req.handleComplete();
        if (req.is_broadcastResult())
            broadcast(cmdIntent);
        _readQueue.clear();
        _bleState = BleState.IDLE;
        Log.i(TAG, "<<<< IDLE");
    }

    // Command Output data received
    private void commandOutputHandler(byte[] charData)
    {
        Log.i(TAG, "commandOutputHandler()");
        // Make sure there is a command in the queue
        if (_commandQueue.size() == 0 || _commandQueue.peek() == null)
        {
            Log.w(TAG, "commandOutputHandler() executed with no queued commands");
            return;
        }
        // Append data to command output
        DreambandRequest req = _commandQueue.peek();
        req.outputData(charData);
        Log.i(TAG, "<<<< READ OUTPUT");
    }

    // Event data received
    private void eventHandler(byte[] data)
    {
        Log.i(TAG, "eventHandler()");
        // Parse event data
        byte eventId = data[0];
        long flags = Utility.getUnsignedInt32(data, 1);
        DreambandEvent event = DreambandEvent.fromValue(eventId);
        Log.d(TAG, "Event data: " + event + " flags = " + flags);

        // Notify event
        Intent intent = new Intent(DreambandResp.EVENT);
        intent.putExtra(DreambandResp.RESP_VALID, true);
        intent.putExtra(DreambandResp.EVENT, event);
        intent.putExtra(DreambandResp.EVENT_FLAGS, flags);
        broadcast(intent);
    }

    // Stream data received
    private void streamDataHandler(byte[] charData)
    {
        Log.i(TAG, "streamDataHandler()");
    }

    // Broadcast intent with Device Connected notification
    private void sendConnectedBroadcast() {
        Intent intent = new Intent(DreambandResp.RESP_DEVICE_CONNECTED);
        intent.putExtra(DreambandResp.RESP_VALID, true);
        intent.putExtra(DreambandResp.RESP_DEVICE_NAME, _deviceName);
        intent.putExtra(DreambandResp.RESP_DEVICE_ADDRESS, _deviceAddress);
        broadcast(intent);
    }

    private void setTime() {
        // TODO: Set time upon connection
        String command = "clock-set ";


    }
    /**** Public Dreamband Service Methods ***********/

    // Starts searching for the dreamband device to connect to
    public boolean connect()
    {
        if (_connectionState != ConnectionState.CONNECTED)
        {
            scanLeDevice(true);
        }
        return true;
    }

    // Disconnects and stops scanning for the dreamband device
    public boolean disconnect()
    {
        if (_scanning)
            scanLeDevice(false);
        if (_connectionState != ConnectionState.DISCONNECTED)
        {
            if (_bleState != BleState.IDLE)
            {
                _bluetoothLeService.disconnect();
            } else {
                // Add the command to the queue and return true for success, false otherwise
                // Results will be broadcasted after they are received
                String command = "ble-disconnect";
                return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_DEVICE_DISCONNECTED))
                        == DreambandResp.ErrorCode.SUCCESS;
            }
        }

        return true;
    }

    /** Send a generic command to the dreamband
     *  - broadcasts DreambandResp.RESP_COMMAND notification upon response
     * @param command String command to send to the Dreamband
     * @param data Extra request data to send to the Dreamband or null
     * @return True if added to command queue, false otherwise
     */
    public boolean sendCommand(String command, byte[] data)
    {
        // Add the command to the queue and return true for success, false otherwise
        //
        return issueQueueRequest(new DreambandRequest(command, data, DreambandResp.RESP_COMMAND))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    // Returns the number of unsynced sessions in the Aurora.
    public boolean unsyncedSessionCount()
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "sd-dir-read sessions 0 *@*";
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_UNSYNCED_SESSION_COUNT))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    /**
     Renames the session specified from Aurora

     - parameter id:         session id received by the rest API after syncing the session
     - parameter name:       name of the session in the Aurora file system
     - completion: broadcasts DreambandResp.RESP_RENAME_SYNCED_SESSION notification upon completion
     */
    public boolean renameSyncedSession(String id, String name)
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "sd-rename sessions/" + name +  " sessions/" + id;
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_RENAME_SYNCED_SESSION))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    /**
     Removes the session specified from Aurora

     - parameter name:       name of the session in the Aurora file system
     - completion: broadcasts DreambandResp.RESP_REMOVE_EMPTY_SESSION notification upon completion
     */
    public boolean removeEmptySession(String name)
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "sd-dir-del sessions/" + name;
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_REMOVE_EMPTY_SESSION))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    /**
     Retrieves OS version from Aurora

     - completion: broadcasts DreambandResp.RESP_OS_VERSION notification upon completion
     */
    public boolean osVersion()
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "os-info";
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_OS_VERSION))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    /**
     Retrieves battery level from Aurora

     - completion: broadcasts DreambandResp.RESP_BATTERY_LEVEL notification upon completion
     */
    public boolean batteryLevel()
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "os-info";
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_BATTERY_LEVEL))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    /**
     Retrieves if the profile is loaded from Aurora

     - completion: broadcasts DreambandResp.RESP_IS_PROFILE_LOADED notification upon completion
     */
    public boolean isProfileLoaded()
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "os-info";
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_IS_PROFILE_LOADED))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    /**
     Shuts down the Aurora

     - completion: broadcasts DreambandResp.RESP_SHUTDOWN notification upon completion
     */
    public boolean shutdown()
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "os-shutdown";
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_SHUTDOWN))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    /**
     Starts observing events from the Aurora

     Events will be broadcasted as EVENT notifications as they are received from the Aurora
     - completion: broadcasts DreambandResp.RESP_OBSERVE_EVENTS notification upon completion
     */
    public boolean observeEvents(EnumSet<DreambandEvent> eventsToObserve, EnumSet<EventOutput> eventOutputs)
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        int eventIds = DreambandEvent.getEventIdsValue(eventsToObserve);
        int outputEventIds = EventOutput.getEventIdsValue(eventOutputs);
        String command = "event-output-enable " + eventIds + " "+ outputEventIds;
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_OBSERVE_EVENTS))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    public boolean disableEvents(EnumSet<DreambandEvent> eventsToObserve, EnumSet<EventOutput> eventOutputs)
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        int eventIds = DreambandEvent.getEventIdsValue(eventsToObserve);
        int outputEventIds = EventOutput.getEventIdsValue(eventOutputs);
        String command = "event-output-disable " + eventIds + " "+ outputEventIds;
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_DISABLE_EVENTS))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    /**
     Retrieves help info from the Aurora

     - completion: broadcasts DreambandResp.RESP_HELP notification upon completion
     */
    public boolean help()
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "help";
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_HELP))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    /**
     Buzzes the note for duration on the Aurora

     - completion: broadcasts DreambandResp.RESP_BUZZ notification upon completion
     */
    public boolean buzz(int note, int duration)
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "buzz-note " + note + " " + duration;
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_BUZZ))
                == DreambandResp.ErrorCode.SUCCESS;
    }


    public boolean listProfiles()
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "sd-dir-read profiles 1 *.prof";
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_LIST_PROFILES))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    public boolean loadProfile(String name)
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "prof-load " + name;
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_READ_PROFILE))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    public boolean unloadProfile()
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "prof-unload";
        return issueQueueRequest(new DreambandRequest(command, null, DreambandResp.RESP_READ_PROFILE))
                == DreambandResp.ErrorCode.SUCCESS;
    }


    public boolean readProfile(String name)
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "sd-file-read " + name + " profiles 0";
        return issueQueueRequest(new ReadProfileRequest(command, name, DreambandResp.RESP_READ_PROFILE))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    public boolean writeProfile(String name, Profile profile)
    {
        // Add the command to the queue and return true for success, false otherwise
        // Results will be broadcasted after they are received
        String command = "sd-file-write " + name + " profiles 0 1 250 0";
        byte[] profData = new byte[256];
        return issueQueueRequest(new DreambandRequest(command, profData, DreambandResp.RESP_WRITE_PROFILE))
                == DreambandResp.ErrorCode.SUCCESS;
    }

    public boolean updateProfile(String name, ProfileSetting[] profSettings)
    {
        // Determine if the profile is already read
        ProfileManager pm = ProfileManager.getInstance();
        if (pm.is_auroraProfileLoaded() &&
            pm.get_auroraProfile().get_fileName().equalsIgnoreCase(name)) {

            // Profile already loaded, update settings
            pm.updateProfileSettings(profSettings);
            // Update profile on the dreamband
            String command = "sd-file-write " + name + " profiles 0 1 250 0";
            return issueQueueRequest(new WriteProfileRequest(command, name, pm.get_auroraProfile().get_profileBytes(),
                    DreambandResp.RESP_UPDATE_PROFILE)) == DreambandResp.ErrorCode.SUCCESS;
        } else {
            // First read the profile
            readProfile(name);
            // Then update profile
            String command = "sd-file-write " + name + " profiles 0 1 250 0";
            return issueQueueRequest(new WriteProfileRequest(command, name, profSettings,
                    DreambandResp.RESP_UPDATE_PROFILE)) == DreambandResp.ErrorCode.SUCCESS;
        }
    }

}
