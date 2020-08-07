package de.tuberlin.mcc.simra.app.services.BLE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class BLEScanner {
    private final String TAG = "BLEScanner";
    private BluetoothLeScanner bluetoothLeScanner;
    private Map<String, BluetoothDevice> foundDevices;
    private ScanCallback currentScan;
    private final BLEScanCallbacks scanCallbacks;

    public BLEScanner(BLEScanCallbacks scanCallbacks) {
        this.scanCallbacks = scanCallbacks;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        } else {
            throw new RuntimeException("Blutooth not enabled or granted");
        }
    }

    public synchronized void abortAnyCurrentScan() {
        if (finishScan()) {
            scanCallbacks.onScanAborted();
        }
    }

    private synchronized boolean finishScan() {
        if (currentScan != null && bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(currentScan);
            currentScan = null;
            return true;
        }
        return false;
    }

    public interface BLEScanCallbacks {
        void onScanStarted();

        void onScanSelfFinished();

        void onScanAborted();
    }

    public interface DeviceFoundCallback {
        void onDeviceFound(BluetoothDevice device);
    }

    public boolean isScanning() {
        return currentScan != null;
    }

    public void findDeviceById(String radmesserID, DeviceFoundCallback deviceFoundCallback) {
        List<ScanFilter> filterList = new ArrayList<>();
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setDeviceAddress(radmesserID);
        filterList.add(builder.build());

        startScan(
                5,
                filterList,
                deviceFoundCallback // todo: maybe tops scan after, but may not be needed?
        );
    }

    public void findDevicesByServices(BLEServiceManager bleServices, DeviceFoundCallback deviceFoundCallback) {
        List<ScanFilter> filterList = new ArrayList<>();
            for (UUID serviceUUID : bleServices.getAllUUIDs()) {
                ScanFilter.Builder builder = new ScanFilter.Builder();
                builder.setServiceUuid(new ParcelUuid(serviceUUID));
                filterList.add(builder.build());
        }

        startScan(8, filterList, deviceFoundCallback);

    }

    private synchronized boolean startScan(int duration, List<ScanFilter> filters, DeviceFoundCallback deviceFoundCallback) {
        abortAnyCurrentScan();
        ScanSettings scanSettings = new ScanSettings.Builder().build();

        foundDevices = new HashMap<>();
        ScanCallback thisScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                String deviceID = result.getDevice().getAddress();
                if (!foundDevices.containsKey(deviceID)) {
                    foundDevices.put(deviceID, result.getDevice());
                    deviceFoundCallback.onDeviceFound(result.getDevice());
                }
                super.onScanResult(callbackType, result);
            }
        };
        currentScan = thisScanCallback;
        try {
            bluetoothLeScanner.startScan(filters, scanSettings, thisScanCallback);
            scanCallbacks.onScanStarted();
            Log.i(TAG, "scan started");
        } catch (NullPointerException nex) {
            //errorStatus = ErrorStatus.bluetooth_not_ready;
            Log.e(TAG, nex.getMessage());
            return false;
        }

        // stop scanning after duration, if not already stoped
        Handler stopScanHandler = new Handler(Looper.myLooper());
        stopScanHandler.postDelayed(() -> {
            if (finishScan()) {
                scanCallbacks.onScanSelfFinished();
            }
        }, duration * 1000);
        return true;
    }
}
