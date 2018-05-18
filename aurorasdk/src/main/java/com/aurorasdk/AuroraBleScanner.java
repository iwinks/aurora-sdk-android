package com.aurorasdk;

import android.Manifest;
import android.content.Context;
import android.support.v4.content.ContextCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.pm.PackageManager;

public class AuroraBleScanner {

    public enum ScanStatusType {
        SCAN_RESULTS_CHANGE, ERROR_NO_ADAPTER, ERROR_ADAPTER_DISABLED, ERROR_NO_PERMISSIONS, ERROR_UNKNOWN
    };

    private Context context;
    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;

    private ScanCallback scanCallback;
    private Map<String, BluetoothDevice> scanResults;

    private boolean scanning;

    public interface ScanListener {
        void onScanStatusChange(ScanStatusType status, List<BluetoothDevice> scanResults);
    }

    private ScanListener scanListener;

    public AuroraBleScanner(Context context, ScanListener scanListener){

        this.context = context;
        this.scanListener = scanListener;

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        scanResults = new HashMap<>();

        adapter = bluetoothManager.getAdapter();
    }

    public void startScan(){

        if (!hasPermissions() || scanning) return;

        scanning = true;

        scanner = adapter.getBluetoothLeScanner();

        scanResults.clear();

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(Constants.AURORA_SERVICE_UUID)
                .build();


        List<ScanFilter> filters = new ArrayList<>();
        filters.add(scanFilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanCallback = new BleScanCallback();
        scanner.startScan(filters, settings, scanCallback);
    }

    public void stopScan() {

        if (scanning) {

            scanner.stopScan(scanCallback);
            scanning = false;
        }
    }

    public Map<String, BluetoothDevice> getResults() {

        return scanResults;
    }

    private void addScanResult(ScanResult result) {

        BluetoothDevice device = result.getDevice();
        String deviceAddress = device.getAddress();

        scanResults.put(deviceAddress, device);

        scanListener.onScanStatusChange(ScanStatusType.SCAN_RESULTS_CHANGE, new ArrayList<>(scanResults.values()));
    }

    private class BleScanCallback extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            addScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {

            for (ScanResult result : results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {

            stopScan();

            switch(errorCode){

                case SCAN_FAILED_ALREADY_STARTED:                   //1
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:   //2
                case SCAN_FAILED_INTERNAL_ERROR:                    //3
                case SCAN_FAILED_FEATURE_UNSUPPORTED:               //4
                default:
                    scanListener.onScanStatusChange(ScanStatusType.ERROR_UNKNOWN, null);
                    break;
            }
        }
    }

    private boolean hasPermissions() {

        if (adapter == null) {

            scanListener.onScanStatusChange(ScanStatusType.ERROR_NO_ADAPTER, null);
            return false;
        }
        else if (!adapter.isEnabled()){

            scanListener.onScanStatusChange(ScanStatusType.ERROR_ADAPTER_DISABLED, null);
            return false;

        } if (!hasLocationPermissions()) {

            scanListener.onScanStatusChange(ScanStatusType.ERROR_NO_PERMISSIONS, null);
            return false;
        }

        return true;
    }

    private boolean hasLocationPermissions() {

        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }


}


