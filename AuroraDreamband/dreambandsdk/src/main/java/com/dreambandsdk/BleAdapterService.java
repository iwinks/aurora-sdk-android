package com.dreambandsdk;

/**
 * Created by seanf on 8/30/2017.
 */


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import android.util.Log;

import java.util.Formatter;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class BleAdapterService extends Service {

    private static final String TAG = "BleAdapterService";

    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothManager mBluetoothManager;
    private Handler mActivityHandler = null;
    private BluetoothDevice device;
    private BluetoothGattDescriptor descriptor;

    public BluetoothDevice getDevice() {
        return device;
    }

    // Messages sent back to service
    public static final int GATT_CONNECTED = 1;
    public static final int GATT_DISCONNECT = 2;
    public static final int GATT_SERVICES_DISCOVERED = 3;
    public static final int GATT_CHARACTERISTIC_READ = 4;
    public static final int GATT_REMOTE_RSSI = 5;
    public static final int MESSAGE = 6;
    public static final int NOTIFICATION_RECEIVED = 7;
    public static final int SIMULATED_NOTIFICATION_RECEIVED = 8;
    public static final int GATT_CHARACTERISTIC_WRITTEN = 9;
    public static final int GATT_DESCRIPTOR_WRITTEN = 10;
    public static final int ERROR = 11;
    public static final int GATT_BONDED = 12;
    public static final int GATT_BOND_AUTH_FAILURE = 13;

    // Message parms
    public static final String PARCEL_DESCRIPTOR_UUID = "DESCRIPTOR_UUID";
    public static final String PARCEL_CHARACTERISTIC_UUID = "CHARACTERISTIC_UUID";
    public static final String PARCEL_SERVICE_UUID = "SERVICE_UUID";
    public static final String PARCEL_VALUE = "VALUE";
    public static final String PARCEL_RSSI = "RSSI";
    public static final String PARCEL_TEXT = "TEXT";
    public static final String PARCEL_ERROR = "ERROR";

    // UUIDs
    public static final String NXTID_ADMINISTRATOR_SERVICE_SERVICE_UUID = "0D25378A101C40C18B1641FFA2A24CF0";
    public static final String NXTID_DATA_RECORD_MANAGEMENT_SERVICE_SERVICE_UUID = "0D25E3BF101C40C18B1641FFA2A24CF0";

    public static final String FIRMWARE_VERISON_CHARACTERISTIC_UUID = "0D2548AF101C40C18B1641FFA2A24CF0";
    public static final String ADMIN_REQUEST_CHARACTERISTIC_UUID = "0D25F92F101C40C18B1641FFA2A24CF0";
    public static final String PUBLIC_KEY_VAL1_CHARACTERISTIC_UUID = "0D256829101C40C18B1641FFA2A24CF0";
    public static final String PUBLIC_KEY_VAL2_CHARACTERISTIC_UUID = "0D257E56101C40C18B1641FFA2A24CF0";
    public static final String REQUEST_RESP_CHARACTERISTIC_UUID = "0D252430101C40C18B1641FFA2A24CF0";


    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    // Ble Gatt Callback
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                Log.d(Constants.TAG, "Bond state: " + gatt.getDevice().getBondState());
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    // Starting the Broadcast Receiver that will listen for bonding process changes
                    final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    mContext.registerReceiver(mBondingBroadcastReceiver, filter);
                }

                sendConsoleMessage("Connected");
                Message msg = Message.obtain(mActivityHandler, GATT_CONNECTED);
                msg.sendToTarget();
				/*
				 * The onConnectionStateChange event is triggered just after the Android connects to a device.
				 * In case of bonded devices, the encryption is reestablished AFTER this callback is called.
				 * Moreover, when the device has Service Changed indication enabled, and the list of services has changed (e.g. using the DFU),
				 * the indication is received few milliseconds later, depending on the connection interval.
				 * When received, Android will start performing a service discovery operation itself, internally.
				 *
				 * If the mBluetoothGatt.discoverServices() method would be invoked here, if would returned cached services,
				 * as the SC indication wouldn't be received yet.
				 * Therefore we have to postpone the service discovery operation until we are (almost, as there is no such callback) sure, that it had to be handled.
				 * Our tests has shown that 600 ms is enough. It is important to call it AFTER receiving the SC indication, but not necessarily
				 * after Android finishes the internal service discovery.
				 *
				 * NOTE: This applies only for bonded devices with Service Changed characteristic, but to be sure we will postpone
				 * service discovery for all devices.
				 */
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Some proximity tags (e.g. nRF PROXIMITY) initialize bonding automatically when connected.
                        if (mBluetoothGatt.getDevice().getBondState() != BluetoothDevice.BOND_BONDING) {
                            Log.d(Constants.TAG, "Discovering Services...");
                            boolean result = mBluetoothGatt.discoverServices();
                            if (result == false)
                                Log.e(Constants.TAG, "Error discovering services");
                            // Delete bond information?
                        }
                    }
                }, 800);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                sendConsoleMessage("Disconnected");
                Message msg = Message.obtain(mActivityHandler, GATT_DISCONNECT);
                msg.sendToTarget();
                mBluetoothGatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Message msg = Message.obtain(mActivityHandler, GATT_SERVICES_DISCOVERED);
            msg.sendToTarget();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                sendConsoleMessage("characteristic read OK");
                Bundle bundle = new Bundle();
                bundle.putString(PARCEL_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
                bundle.putString(PARCEL_SERVICE_UUID, characteristic.getService().getUuid().toString());
                bundle.putByteArray(PARCEL_VALUE, characteristic.getValue());
                Message msg = Message.obtain(mActivityHandler,GATT_CHARACTERISTIC_READ);
                msg.setData(bundle);
                msg.sendToTarget();
            } else {
                sendConsoleMessage("characteristic read err:"+status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                sendConsoleMessage("Characteristic " + characteristic.getUuid().toString() + " written OK");
                Bundle bundle = new Bundle();
                bundle.putString(PARCEL_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
                bundle.putString(PARCEL_SERVICE_UUID, characteristic.getService().getUuid().toString());
                bundle.putByteArray(PARCEL_VALUE, characteristic.getValue());
                Message msg = Message.obtain(mActivityHandler, GATT_CHARACTERISTIC_WRITTEN);
                msg.setData(bundle);
                msg.sendToTarget();
            } else {
                reportError("characteristic write err:" + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Bundle bundle = new Bundle();
            bundle.putString(PARCEL_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
            bundle.putString(PARCEL_SERVICE_UUID, characteristic.getService().getUuid().toString());
            bundle.putByteArray(PARCEL_VALUE, characteristic.getValue());
            Message msg = Message.obtain(mActivityHandler,NOTIFICATION_RECEIVED);
            msg.setData(bundle);
            msg.sendToTarget();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                sendConsoleMessage("Descriptor " + descriptor.getUuid().toString() + " written OK");
                Bundle bundle = new Bundle();
                bundle.putString(PARCEL_DESCRIPTOR_UUID, descriptor.getUuid().toString());
                bundle.putString(PARCEL_CHARACTERISTIC_UUID, descriptor.getCharacteristic().getService().getUuid().toString());
                bundle.putString(PARCEL_SERVICE_UUID, descriptor.getCharacteristic().getService().getUuid().toString());
                bundle.putByteArray(PARCEL_VALUE, descriptor.getValue());
                Message msg = Message.obtain(mActivityHandler, GATT_DESCRIPTOR_WRITTEN);
                msg.setData(bundle);
                msg.sendToTarget();
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                    || status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
                // Start listening to bond changes
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    // Starting the Broadcast Receiver that will listen for bonding process changes
                    final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    mContext.registerReceiver(mBondingBroadcastReceiver, filter);
                }

            } else {
                reportError("Descriptor write err:" + status);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Bundle bundle = new Bundle();
                bundle.putInt(PARCEL_RSSI, rssi);
                Message msg = Message.obtain(mActivityHandler, GATT_REMOTE_RSSI);
                msg.setData(bundle);
                msg.sendToTarget();
            } else {
                reportError("RSSI read err:"+status);
            }
        }
    };

    private BroadcastReceiver mBondingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
            final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

            Log.d(TAG, "Bond state changed for: " + device.getAddress() + " new state: " + bondState + " previous: " + previousBondState);

            // skip other devices
            if (!device.getAddress().equals(mBluetoothGatt.getDevice().getAddress()))
                return;

            if (bondState == BluetoothDevice.BOND_BONDED) {
                publishMessage(device.getAddress(), GATT_BONDED);
                mContext.unregisterReceiver(this);
            } else if (bondState == BluetoothDevice.BOND_NONE && previousBondState == BluetoothDevice.BOND_BONDING) {
                Log.e(Constants.TAG, "Pairing authentication failure");
                publishMessage(device.getAddress(), GATT_BOND_AUTH_FAILURE);
            }
            else {
                Log.e(Constants.TAG, "Not Bonded. BOND State: " + bondState);
            }
        }
    };

    // Service binder
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BleAdapterService getService() {
            return BleAdapterService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {

        mContext = this;
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return;
        }

    }

    // Connect to the device
    public boolean connect(final String address) {
        Log.d(Constants.TAG, "CONNECTING VIA BLE: " + address);

        if (mBluetoothAdapter == null || address == null) {
            sendConsoleMessage("connect: mBluetoothAdapter=null");
            return false;
        }

        device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            sendConsoleMessage("connect: device=null");
            return false;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (device != null) {
                    // set auto connect to true
                    if (mBluetoothGatt != null)
                        mBluetoothGatt.close();
                    mBluetoothGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
                    sendConsoleMessage("Connecting...");
                }
            }
        });

        return true;
    }

    // disconnect from device
    public void disconnect() {
        sendConsoleMessage("disconnect");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            sendConsoleMessage("disconnect: mBluetoothAdapter|mBluetoothGatt null");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    // Set activity that will receive the messages
    public void setActivityHandler(Handler handler) {
        mActivityHandler = handler;
    }

    // return list of supported services
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;
        return mBluetoothGatt.getServices();
    }

    // writes a value to a characteristic with response required
    public boolean writeCharacteristic(String serviceUuid,String characteristicUuid, byte[] value) {
        return writeCharacteristic(serviceUuid,characteristicUuid, value,true);
    }

    // writes a value to a characteristic with/without response
    public boolean writeCharacteristic(String serviceUuid,String characteristicUuid, byte[] value, boolean require_response) {
        Log.d(TAG, "writeCharacteristic serviceUuid="+serviceUuid+" characteristicUuid="+characteristicUuid+" require_response="+require_response);
        // DEBUGGING, Print value of written bytes
		/*Formatter formatter = new Formatter();
		for (byte b : value) {
			formatter.format("%02x", b);
		}
		String hex = formatter.toString();
		Log.d(TAG, hex);*/

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            sendConsoleMessage("writeCharacteristic: mBluetoothAdapter|mBluetoothGatt null");
            return false;
        }

        BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(serviceUuid));
        if (gattService == null) {
            sendConsoleMessage("writeCharacteristic: gattService null");
            return false;
        }
        BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(UUID.fromString(characteristicUuid));
        if (gattChar == null) {
            sendConsoleMessage("writeCharacteristic: gattChar null");
            return false;
        }
        gattChar.setValue(value);

        if (require_response) {
            gattChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        } else {
            gattChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        }

        return mBluetoothGatt.writeCharacteristic(gattChar);
    }

    // Read value from service
    public boolean readCharacteristic(String serviceUuid,	String characteristicUuid) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            sendConsoleMessage("readCharacteristic: mBluetoothAdapter|mBluetoothGatt null");
            return false;
        }

        BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(serviceUuid));
        if (gattService == null) {
            sendConsoleMessage("readCharacteristic: gattService null");
            return false;
        }
        BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(UUID.fromString(characteristicUuid));
        if (gattChar == null) {
            sendConsoleMessage("readCharacteristic: gattChar null");
            return false;
        }
        return mBluetoothGatt.readCharacteristic(gattChar);
    }

    public boolean setNotificationsState(String serviceUuid, String characteristicUuid, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            sendConsoleMessage("setNotificationsState: mBluetoothAdapter|mBluetoothGatt null");
            return false;
        }

        BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(serviceUuid));
        if (gattService == null) {
            sendConsoleMessage("setNotificationsState: gattService null");
            return false;
        }
        BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(UUID.fromString(characteristicUuid));
        if (gattChar == null) {
            sendConsoleMessage("setNotificationsState: gattChar null");
            return false;
        }
        mBluetoothGatt.setCharacteristicNotification (gattChar, enabled);
        // Enable remote notifications
        descriptor = gattChar.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        Log.d(Constants.TAG, "XXXX Descriptor:" + descriptor.getUuid());
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        boolean ok = mBluetoothGatt.writeDescriptor(descriptor);
        return ok;
    }

    public void readRemoteRssi() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readRemoteRssi();
    }

    private void reportError(String text) {
        Log.d(Constants.TAG, "ERROR: "+text);
        Message msg = Message.obtain(mActivityHandler, ERROR);
        Bundle data = new Bundle();
        data.putString(PARCEL_ERROR, text);
        msg.setData(data);
        msg.sendToTarget();
    }

    private void sendConsoleMessage(String text) {
        Log.d(Constants.TAG, "XXXX "+text);
        Message msg = Message.obtain(mActivityHandler, MESSAGE);
        Bundle data = new Bundle();
        data.putString(PARCEL_TEXT, text);
        msg.setData(data);
        msg.sendToTarget();
    }

    private void publishMessage(String parcelMsg, int gattID) {
        if (parcelMsg != null && !parcelMsg.isEmpty() && mActivityHandler != null) {
            Bundle bundle = new Bundle();
            bundle.putString(PARCEL_VALUE, parcelMsg);
            Message msg = Message.obtain(mActivityHandler, gattID);

            msg.setData(bundle);
            msg.sendToTarget();
        }
    }
}