package com.aurorasdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.support.v4.content.ContextCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({android.os.Process.class, ContextCompat.class, android.os.ParcelUuid.class, AuroraBleScanner.class})
public class AuroraBleScannerTest {

    private Context context = null;

    private BluetoothManager blManager = null;

    private AuroraBleScanner.ScanListener listener = null;

    @Before
    public void setTest() {
        context = PowerMockito.mock(Context.class);
        blManager = PowerMockito.mock(BluetoothManager.class);
        PowerMockito.when(context.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(blManager);
        listener = PowerMockito.mock(AuroraBleScanner.ScanListener.class);
        PowerMockito.mockStatic(android.os.Process.class);
    }

    @Test
    public void startScan_NoHasPermissionsDueToAdapterNull() {

        AuroraBleScanner scanner = new AuroraBleScanner(context, listener);
        scanner.startScan();
        scanner.stopScan();
        scanner.getResults();
    }

    @Test
    public void startScan_NoHasPermissionsDueToAdapterIsNotEnabled() {

        BluetoothAdapter adapter = PowerMockito.mock(BluetoothAdapter.class);
        PowerMockito.when(adapter.isEnabled()).thenReturn(false);
        PowerMockito.when(blManager.getAdapter()).thenReturn(adapter);

        AuroraBleScanner scanner = new AuroraBleScanner(context, listener);
        scanner.startScan();
    }

    @Test
    public void startScan_NoHasPermissionsDueToHasNotLocationPermissions() throws Exception {

        BluetoothAdapter adapter = PowerMockito.mock(BluetoothAdapter.class);

        PowerMockito.when(adapter.isEnabled()).thenReturn(true);
        PowerMockito.when(blManager.getAdapter()).thenReturn(adapter);

        AuroraBleScanner scanner = new AuroraBleScanner(context, listener);

        PowerMockito.mockStatic(ContextCompat.class);
        PowerMockito.when(ContextCompat.checkSelfPermission(Mockito.any(), Mockito.any())).thenReturn(12);
        scanner.startScan();
    }

    @Test
    public void startScan_isCorrectWork() throws Exception {
        BluetoothAdapter adapter = PowerMockito.mock(BluetoothAdapter.class);
        BluetoothLeScanner mockScanner = PowerMockito.mock(BluetoothLeScanner.class);
        PowerMockito.when(adapter.getBluetoothLeScanner()).thenReturn(mockScanner);
        PowerMockito.mockStatic(android.os.ParcelUuid.class);
        PowerMockito.when(android.os.ParcelUuid.fromString(Mockito.any())).thenReturn(null);
        PowerMockito.when(adapter.isEnabled()).thenReturn(true);
        PowerMockito.when(blManager.getAdapter()).thenReturn(adapter);
        CreateScanFilterMock();
        CreateScanSettingMock();
        AuroraBleScanner scanner = new AuroraBleScanner(context, listener);

        scanner.startScan();
        scanner.stopScan();
    }

    private void CreateScanSettingMock() throws Exception {
        ScanSettings.Builder mockBuilder = PowerMockito.mock(ScanSettings.Builder.class);
        ScanSettings mockScanSettings = PowerMockito.mock(ScanSettings.class);
        PowerMockito.when(mockBuilder.build()).thenReturn(mockScanSettings);
        PowerMockito.when(mockBuilder.setScanMode(Mockito.anyInt())).thenReturn(mockBuilder);
        PowerMockito.whenNew(ScanSettings.Builder.class).withNoArguments().thenReturn(mockBuilder);
    }

    private void CreateScanFilterMock() throws Exception {
        ScanFilter.Builder mockBuilder = PowerMockito.mock(ScanFilter.Builder.class);
        ScanFilter mockScanFilter = PowerMockito.mock(ScanFilter.class);
        PowerMockito.when(mockBuilder.build()).thenReturn(mockScanFilter);
        PowerMockito.when(mockBuilder.setServiceUuid(Mockito.any())).thenReturn(mockBuilder);
        PowerMockito.whenNew(ScanFilter.Builder.class).withNoArguments().thenReturn(mockBuilder);
    }

}
