package de.tuberlin.mcc.simra.app.services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.HashMap;

import de.tuberlin.mcc.simra.app.services.radmesser.BLEScanner;
import de.tuberlin.mcc.simra.app.services.radmesser.BLEServiceManager;
import de.tuberlin.mcc.simra.app.services.radmesser.RadmesserDevice;

public class RadmesserService extends Service {
    private IBinder mBinder = new MyBinder();
    private int connectionStatus = 0;


    // das soll mit einem Listener passieren
    public int getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(int newStatus) {
        this.connectionStatus = newStatus;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class MyBinder extends Binder {
        public RadmesserService getService() {
            return RadmesserService.this;
        }
    }


    // Linus
    private final String TAG = "BLE_SERVICE";
    private final String sharedPrefsKey = "RadmesserServiceBLE";
    private final String sharedPrefsKeyRadmesserID = "connectedDevice";


    public interface BLECallbacks {
        void onNewDeviceFound(BluetoothDevice device);

        void onScanstarted();

        void onScanStopped();

        void onConnectionStateChange();
    }


    BLEScanner scanner;

    private RadmesserDevice connectedDevice;


    BLECallbacks callbacks = new BLECallbacks() {
        @Override
        public void onNewDeviceFound(BluetoothDevice device) {

            //todo: call UI with device.getName() device.getAddress()
        }

        @Override
        public void onScanstarted() {

        }

        @Override
        public void onScanStopped() {

        }

        @Override
        public void onConnectionStateChange() {
            Log.i(TAG, "onConnectionStateChange: " + connectedDevice.getConnectionState().toString());
            //todo: call UI
        }
    };

    private BLEServiceManager registerBLEServices() {
        BLEServiceManager bleServices = new BLEServiceManager();
        //HEARTRATE
        bleServices.addService(
                "0000180D-0000-1000-8000-00805F9B34FB",
                "00002a37-0000-1000-8000-00805f9b34fb",

                val -> Log.i(TAG, "new HEARTRATE Value:" + val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1))
        );


        //CLOSEPASS
        bleServices.addService(
                "1FE7FAF9-CE63-4236-0003-000000000000",
                "1FE7FAF9-CE63-4236-0003-000000000001",

                val -> Log.i(TAG, "new CLOSEPASS Value:" + val.getStringValue(0))
        );

        //DISTANCE
        bleServices.addService(
                "1FE7FAF9-CE63-4236-0001-000000000000",
                "1FE7FAF9-CE63-4236-0001-000000000001",

                val -> Log.i(TAG, "new DISTANCE Value:" + val.getStringValue(0))
        );

        //CONNECTION
        bleServices.addService(
                "1FE7FAF9-CE63-4236-0002-000000000000",
                "1FE7FAF9-CE63-4236-0002-000000000001",

                val -> {
                    Log.i(TAG, "new CONNECTION Value:" + val.getStringValue(0));
                    if (val.getStringValue(0).equals("1")) {
                        connectedDevice.setConnectionState(RadmesserDevice.ConnectionStage.PairingCompleted);
                        setPairedRadmesserID(connectedDevice.getID());
                    }
                }
        );
        return bleServices;
    }


    private void scanAndConnectToRandomRadmesser() {
        BLEServiceManager serviceManager = registerBLEServices();
        scanner = new BLEScanner(callbacks, this);
        scanner.connectAnyNewDevice(
                serviceManager,
                device -> {
                    connectedDevice = new RadmesserDevice(device, callbacks, serviceManager);
                    connectedDevice.connect(this);
                }
        );

    }

    private void scanAndConnectToPairedRadmesser() {
        BLEServiceManager serviceManager = registerBLEServices();
        scanner = new BLEScanner(callbacks, this);
        scanner.tryConnectPairedDevice(
                getPairedRadmesserID(),
                device -> {
                    connectedDevice = new RadmesserDevice(device, callbacks, serviceManager);
                    connectedDevice.connect(this);
                }
        );
    }

    private boolean connectToRadmesserByID(String id) {
        BLEServiceManager serviceManager = registerBLEServices();
        HashMap<String, BluetoothDevice> foundDevices = scanner.getFoundDevices();
        if (!foundDevices.containsKey(id))
            return false;

        connectedDevice = new RadmesserDevice(foundDevices.get(id), callbacks, serviceManager);
        connectedDevice.connect(this);

        return true;
    }


    @Nullable
    private String getPairedRadmesserID() {
        return getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).getString(sharedPrefsKeyRadmesserID, null);
    }

    private void setPairedRadmesserID(String id) {
        getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).edit().putString(sharedPrefsKeyRadmesserID, id).apply();
    }

    public void unPairedRadmesser() {
        getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).edit().remove(sharedPrefsKeyRadmesserID).apply();
    }
}