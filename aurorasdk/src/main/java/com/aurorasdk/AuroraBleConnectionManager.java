package com.aurorasdk;

import android.content.Context;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.Request;


public class AuroraBleConnectionManager extends BleManager<AuroraBleCallbacks> {

    private BluetoothGattCharacteristic commandStatusChar, commandDataChar, commandOutputChar, eventChar;

    private Queue<ByteBuffer> readBuffers;

    public AuroraBleConnectionManager(final Context context) {

        super(context);

        readBuffers = new ArrayBlockingQueue<ByteBuffer>(5);
    }

    @Override
    protected BleManagerGattCallback getGattCallback() {

        return gattCallback;
    }

    @Override
    protected boolean shouldAutoConnect() {

        return true;
    }

    private final BleManagerGattCallback gattCallback = new BleManagerGattCallback() {

        @Override
        protected Deque<Request> initGatt(final BluetoothGatt gatt) {

            final LinkedList<Request> requests = new LinkedList<>();
            requests.push(Request.newEnableIndicationsRequest(commandStatusChar));
            requests.push(Request.newEnableIndicationsRequest(commandOutputChar));
            requests.push(Request.newEnableNotificationsRequest(eventChar));
            return requests;
        }

        @Override
        public boolean isRequiredServiceSupported(final BluetoothGatt gatt) {

            final BluetoothGattService service = gatt.getService(Constants.AURORA_SERVICE_UUID.getUuid());

            if (service == null) return false;

            commandStatusChar = service.getCharacteristic(Constants.COMMAND_STATUS_UUID.getUuid());
            commandDataChar = service.getCharacteristic(Constants.COMMAND_DATA_UUID.getUuid());
            commandOutputChar = service.getCharacteristic(Constants.COMMAND_OUTPUT_INDICATED_UUID.getUuid());
            eventChar = service.getCharacteristic(Constants.EVENT_NOTIFIED_UUID.getUuid());

            return commandStatusChar != null && commandDataChar != null;
        }

        @Override
        protected void onDeviceDisconnected() {

            commandStatusChar = null;
            commandDataChar = null;
        }

        @Override
        protected void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {

            Logger.d("onCharacteristicRead: " + characteristic.getUuid().toString());
            Logger.d("onCharacteristicRead: " + characteristic.getStringValue(0));

            if (characteristic == commandDataChar){

                ByteBuffer readBuffer = readBuffers.peek();

                if (readBuffer != null){

                    if (readBuffer.remaining() < characteristic.getValue().length){

                        Logger.e("Command data read buffer only allocated " + readBuffer.capacity() + " bytes.");
                        Logger.e("Characteristic length: " + characteristic.getValue().length + " Remaining space in buffer:" + readBuffer.remaining());

                        readBuffers.clear();
                        return;
                    }

                    readBuffer.put(characteristic.getValue());

                    if (readBuffer.hasRemaining()){

                        readCharacteristic(commandDataChar);
                    }
                    else {

                        readBuffers.remove();
                        mCallbacks.onCommandResponse(new String(readBuffer.array()));

                        if (!readBuffers.isEmpty()){

                            readCharacteristic(commandDataChar);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {

        }

        @Override
        public void onCharacteristicNotified(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {

            onCharacteristic(gatt, characteristic);
        }

        @Override
        public void onCharacteristicIndicated(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {

            onCharacteristic(gatt, characteristic);
        }

        private void onCharacteristic(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic){

            Logger.d("onCharacteristic: " + characteristic.getUuid());

            byte[] charValue = characteristic.getValue();

            if (characteristic == commandOutputChar){

                mCallbacks.onCommandOutput(charValue);
            }
            else if (characteristic == eventChar){

                mCallbacks.onAuroraEvent(charValue[0] & 0xFF, Utility.getUnsignedInt32(charValue, 1));
            }
            else if (characteristic == commandStatusChar){

                mCallbacks.onCommandStatusChange(charValue[0], charValue[1]);
            }
        }

    };

    public void sendCommand(Command command){

        Logger.d("Sending command: " + command.getCommandString());

        ByteBuffer b;
        while ((b = readBuffers.poll()) != null){

            b.clear();
        }

        enqueue(Request.newWriteRequest(commandStatusChar, new byte[] {(byte)CommandProcessor.CommandState.IDlE.ordinal()}));
        enqueue(Request.newWriteRequest(commandDataChar, command.getCommandStringBytes()));
        enqueue(Request.newWriteRequest(commandStatusChar, new byte[] { (byte)CommandProcessor.CommandState.EXECUTE.ordinal()}));
    }

    public void readCommandResponse(int numBytes){

        readBuffers.add(ByteBuffer.allocate(numBytes));

        Logger.d("Read requests pending: " + readBuffers.size());

        readCharacteristic(commandDataChar);
    }

    public void writeCommandInput(byte[] input){

        commandDataChar.setValue(input);
        writeCharacteristic(commandDataChar);
    }

}