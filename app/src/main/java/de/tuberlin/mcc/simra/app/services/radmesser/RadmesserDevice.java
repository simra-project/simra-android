package de.tuberlin.mcc.simra.app.services.radmesser;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;


import java.util.HashSet;
import java.util.UUID;

import de.tuberlin.mcc.simra.app.services.RadmesserService;


public class RadmesserDevice {
    private final String TAG = "RadmesserDevice";
    private final BluetoothDevice bluetoothDevice;
    private final RadmesserDeviceCallbacks callbacks;
    private de.tuberlin.mcc.simra.app.services.radmesser.BLEServiceManager bleServices;

    private ConnectionStatus connectionState = ConnectionStatus.gattDisconnected;

    public ConnectionStatus getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(ConnectionStatus newState) {
        connectionState = newState;
        callbacks.onConnectionStateChange();
    }

    private enum ConnectionStatus {
        startConnecting,
        gattConnected,
        gattDisconnected,
    }


    public RadmesserDevice(BluetoothDevice bluetoothDevice, RadmesserDeviceCallbacks callbacks, de.tuberlin.mcc.simra.app.services.radmesser.BLEServiceManager bleServices) {
        this.bluetoothDevice = bluetoothDevice;
        this.callbacks = callbacks;
        this.bleServices = bleServices;

    }

    public void connect(Context ctx) {
        setConnectionState(ConnectionStatus.startConnecting);
        bluetoothDevice.createBond();   //start connect to device
        bluetoothDevice.fetchUuidsWithSdp();   //start discover services on that device
        bluetoothDevice.connectGatt(ctx, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.i(TAG, "onConnectionStateChange: " + gatt.getServices().size());
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    setConnectionState(ConnectionStatus.gattConnected);
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // if not paired,
                    setConnectionState(ConnectionStatus.gattDisconnected);
                } else {
                    return;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.i(TAG, "onServicesDiscovered: " + gatt.getServices().size());
                super.onServicesDiscovered(gatt, status);
                for (BluetoothGattService foundService : gatt.getServices()) {
                    HashSet<BLEServiceManager.BLEService> requestedServices = bleServices.byService(foundService.getUuid());
                    if (requestedServices == null) continue;
                    Log.i(TAG, "Found " + requestedServices.size() + " Services for UUID:" + foundService.getUuid().toString());
                    for (de.tuberlin.mcc.simra.app.services.radmesser.BLEServiceManager.BLEService requestedService : requestedServices) {
                        if (requestedService.registered) continue;
                        //found new Service on device, which is to be registered
                        BluetoothGattCharacteristic characteristic = gatt
                                .getService(UUID.fromString(requestedService.serviceUUID))
                                .getCharacteristic(UUID.fromString(requestedService.charackteristicUUIDs));
                        if (characteristic == null) {
                            Log.i(TAG, "Error connecting to Characteristic: " + requestedService.charackteristicUUIDs);
                            continue;
                        }

                        BluetoothGattDescriptor desc = new BluetoothGattDescriptor(UUID.randomUUID(), BluetoothGattDescriptor.PERMISSION_READ);   //create generic descriptor
                        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(desc);

                        gatt.setCharacteristicNotification(characteristic, true);
                        gatt.readCharacteristic(characteristic);
                        requestedService.registered = true;
                    }
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                bleServices.byCharakteristic(characteristic.getUuid()).onValue(characteristic);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                bleServices.byCharakteristic(characteristic.getUuid()).onValue(characteristic);
            }
        });
    }


    public String getName() {
        return bluetoothDevice.getName();
    }

    public String getID() {
        return bluetoothDevice.getAddress();
    }

    public interface RadmesserDeviceCallbacks {
        public void onConnectionStateChange();
    }

    /**
     * Bluetooth Service UUIDs
     */
    public static final String UUID_SERVICE_HEARTRATE = "0000180D-0000-1000-8000-00805F9B34FB";
    public static final String UUID_SERVICE_CHARACTERISTIC_HEARTRATE = "00002a37-0000-1000-8000-00805f9b34fb";

    public static final String UUID_SERVICE_CLOSEPASS = "1FE7FAF9-CE63-4236-0003-000000000000";
    public static final String UUID_SERVICE_CHARACTERISTIC_CLOSEPASS = "1FE7FAF9-CE63-4236-0003-000000000001";

    public static final String UUID_SERVICE_DISTANCE = "1FE7FAF9-CE63-4236-0001-000000000000";
    public static final String UUID_SERVICE_CHARACTERISTIC_DISTANCE = "1FE7FAF9-CE63-4236-0001-000000000001";

    public static final String UUID_SERVICE_CONNECTION = "1FE7FAF9-CE63-4236-0002-000000000000";
    public static final String UUID_SERVICE_CHARACTERISTIC_CONNECTION = "1FE7FAF9-CE63-4236-0002-000000000001";


}
