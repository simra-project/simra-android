package de.tuberlin.mcc.simra.app.services.radmesser;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import de.tuberlin.mcc.simra.app.services.RadmesserService;


public class BLEScanner {
    private final String TAG = "BLEScanner";
    private final int DEFAULT_SCANNING_DURATION_SECONSA = 8;
    private BluetoothLeScanner bluetoothLeScanner;
    private RadmesserService.BLECallbacks callbacks;


    private HashMap<String,BluetoothDevice> foundDevices;

    public BLEScanner(RadmesserService.BLECallbacks callbacks, Context c) {
        this.callbacks = callbacks;
        BluetoothAdapter bluetoothAdapter = ((BluetoothManager) c.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

    }

    private interface ScanResultCallback {
        void onNewDeviceFound(BluetoothDevice device, ScanCallback callback);
    }

    public interface SingleDeviceScanCB {
        void onSpecificDeviceFound(BluetoothDevice device);
    }

    public boolean tryConnectPairedDevice(String pairedRadmesserID, SingleDeviceScanCB then) {
        if (pairedRadmesserID == null)
            return false;

        //create filter matching this device
        ArrayList<ScanFilter> fitlerList = new ArrayList();
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setDeviceAddress(pairedRadmesserID);
        fitlerList.add(builder.build());

        startScan(
                5,
                fitlerList,
                (device, callback) -> {
                    then.onSpecificDeviceFound(device);
                    stopScan(callback);
                }
        );
        return true;
    }

    public void connectAnyNewDevice(de.tuberlin.mcc.simra.app.services.radmesser.BLEServiceManager bleServices, SingleDeviceScanCB then) {
        startScan(
                8,
                createFilterListFromBLEServiceManager(bleServices),
                (device, callback) -> {
                    then.onSpecificDeviceFound(device);
                    stopScan(callback);
                });
    }

    public void scanDevices(de.tuberlin.mcc.simra.app.services.radmesser.BLEServiceManager bleServices) {
        startScan(
                8,
                createFilterListFromBLEServiceManager(bleServices),
                (device, callback) -> {
                    callbacks.onNewDeviceFound(device);
                });
    }

    private ArrayList<ScanFilter> createFilterListFromBLEServiceManager(de.tuberlin.mcc.simra.app.services.radmesser.BLEServiceManager bleServices) {
        ArrayList<ScanFilter> fitlerList = new ArrayList();
        for (UUID serviceUUID : bleServices.getAllUUIDs()) {
            ScanFilter.Builder builder = new ScanFilter.Builder();
            builder.setServiceUuid(new ParcelUuid(serviceUUID));
            fitlerList.add(builder.build());
        }
        return fitlerList;
    }

    private boolean startScan(int duration, ArrayList<ScanFilter> fitlerList, ScanResultCallback then) {
        if (duration <= 0 || duration > 10)
            duration = DEFAULT_SCANNING_DURATION_SECONSA;

        ScanSettings scanSettings = new ScanSettings.Builder().build();

        foundDevices = new HashMap<>();
        ScanCallback scanCallabck = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                String deviceID = result.getDevice().getAddress();
                if (!foundDevices.containsKey(deviceID)) {
                    foundDevices.put(deviceID, result.getDevice());
                    then.onNewDeviceFound(result.getDevice(), this);
                }
                super.onScanResult(callbackType, result);
            }
        };

        try {
            bluetoothLeScanner.startScan(fitlerList, scanSettings, scanCallabck);
            callbacks.onScanstarted();
            Log.i(TAG, "scan started");
        } catch (NullPointerException nex) {
            //errorStatus = ErrorStatus.bluetooth_not_ready;
            Log.e(TAG, nex.getMessage());
            return false;
        }

        //stop Scan after duration, if not already stoped
        Handler stopScanHandler = new Handler(Looper.getMainLooper());
        stopScanHandler.postDelayed(() -> {
            stopScan(scanCallabck);
            Log.i(TAG, "scan stopped");
        }, duration * 1000);
        return true;
    }

    private void stopScan(ScanCallback scanCallback) {
        bluetoothLeScanner.stopScan(scanCallback);
        callbacks.onScanStopped();
    }

    public HashMap<String,BluetoothDevice> getFoundDevices() {
        return foundDevices;
    }
}
