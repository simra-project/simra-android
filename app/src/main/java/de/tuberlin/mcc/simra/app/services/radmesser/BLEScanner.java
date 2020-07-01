package de.tuberlin.mcc.simra.app.services.radmesser;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
public class BLEScanner {
    private final String TAG = "BLEScanner";
    private final int DEFAULT_SCANNING_DURATION_SECONDS = 8;
    private BluetoothLeScanner bluetoothLeScanner;
    private BLEScannerCallbacks callbacks;
    private ScanCallback scanCallabck;
    private HashMap<String, BluetoothDevice> foundDevices;

    public BLEScanner(BLEScannerCallbacks callbacks) {
        this.callbacks = callbacks;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }else
            throw new RuntimeException("Blutooth not enabled or granted");
    }

    public void findDeviceById(String pairedRadmesserID, SingleDeviceScanCB then) {
        abortAnyCurrentScan();

        //create filter matching this device
        ArrayList<ScanFilter> filterList = new ArrayList<>();
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setDeviceAddress(pairedRadmesserID);
        filterList.add(builder.build());

        startScan(
                3,
                filterList,
                (device, callback) -> {
                    then.onSpecificDeviceFound(device);
                    finishScan(callback);
                }
        );
    }


    public void scanDevices(BLEServiceManager bleServices) {

        abortAnyCurrentScan();
        startScan(
                8,
                createFilterListFromBLEServiceManager(bleServices),
                (device, callback) -> callbacks.onNewDeviceFound(device));
    }

    private ArrayList<ScanFilter> createFilterListFromBLEServiceManager(BLEServiceManager bleServices) {
        ArrayList<ScanFilter> filterList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            for (UUID serviceUUID : bleServices.getAllUUIDs()) {
                ScanFilter.Builder builder = new ScanFilter.Builder();
                builder.setServiceUuid(new ParcelUuid(serviceUUID));
                filterList.add(builder.build());
            }
        }
        return filterList;
    }

    private boolean startScan(int duration, ArrayList<ScanFilter> fitlerList, ScanResultCallback then) {
        if (duration <= 0 || duration > 10)
            duration = DEFAULT_SCANNING_DURATION_SECONDS;

        ScanSettings scanSettings = new ScanSettings.Builder().build();

        foundDevices = new HashMap<>();
        ScanCallback myScanCallabck = new ScanCallback() {
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
        scanCallabck = myScanCallabck;
        try {
            bluetoothLeScanner.startScan(fitlerList, scanSettings, scanCallabck);
            callbacks.onScanStarted();
            Log.i(TAG, "scan started");
        } catch (NullPointerException nex) {
            //errorStatus = ErrorStatus.bluetooth_not_ready;
            Log.e(TAG, nex.getMessage());
            return false;
        }

        //stop Scan after duration, if not already stoped
        Handler stopScanHandler = new Handler(Looper.myLooper());
        stopScanHandler.postDelayed(() -> {
            finishScan(myScanCallabck);

        }, duration * 1000);
        return true;
    }

    private void finishScan(ScanCallback scanCallback) {
        Log.i(TAG, "scan stopped");
        bluetoothLeScanner.stopScan(scanCallback);
        callbacks.onScanFinished();
        scanCallabck = null;
    }

    public void abortAnyCurrentScan() {
        // if (RadmesserService.getConnectionState() == RadmesserService.ConnectionState.SEARCHING)
        if (scanCallabck != null && bluetoothLeScanner != null)
            bluetoothLeScanner.stopScan(scanCallabck);
        scanCallabck = null;
    }

    public HashMap<String, BluetoothDevice> getFoundDevices() {
        return foundDevices;
    }

    private interface ScanResultCallback {
        void onNewDeviceFound(BluetoothDevice device, ScanCallback callback);
    }

    public interface SingleDeviceScanCB {
        void onSpecificDeviceFound(BluetoothDevice device);
    }

    public interface BLEScannerCallbacks {
        void onNewDeviceFound(BluetoothDevice device);

        void onScanStarted();

        void onScanFinished();
    }
}
