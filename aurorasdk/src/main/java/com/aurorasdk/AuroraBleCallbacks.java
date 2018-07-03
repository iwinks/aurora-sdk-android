package com.aurorasdk;

import android.bluetooth.BluetoothDevice;

import no.nordicsemi.android.ble.BleManagerCallbacks;

public interface AuroraBleCallbacks extends BleManagerCallbacks {

    default void onDeviceConnected(final BluetoothDevice device) {}

    default boolean shouldEnableBatteryLevelNotifications(final BluetoothDevice device) {
        return false;
    }

    default void onBatteryValueReceived(final BluetoothDevice device, final int value) {}

    default void onBondingRequired(final BluetoothDevice device) {}

    default void onBonded(final BluetoothDevice device) {}

    default void onServicesDiscovered(final BluetoothDevice device, final boolean optionalServicesFound) {}

    void onCommandStatusChange(byte status, byte infoByte);

    void onCommandOutput(byte[] data);

    void onCommandResponse(String responseLine);

    void onAuroraEvent(int eventId, long flags);



}