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


public class RadmesserDevice {
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

    private final String TAG = "RadmesserDevice";
    public boolean devicePaired;        //needen From the outside
    private final BluetoothDevice bleDevice;
    private final ConnectionStateCallbacks callbacks;
    private BLEServiceManager servicesDefinitions;
    private BluetoothGatt gattConnection;
    private Context ctx;
    private ConnectionStatus connectionState = ConnectionStatus.GATT_DISCONNECTED;

    public RadmesserDevice(BluetoothDevice bleDevice, ConnectionStateCallbacks stateCallbacks, BLEServiceManager servicesDefinitions, Context parentContext) {
        this.bleDevice = bleDevice;
        this.callbacks = stateCallbacks;
        this.servicesDefinitions = servicesDefinitions;
        this.ctx = parentContext;
        connect();
    }

    private void connect() {
        setConnectionState(ConnectionStatus.INIT_CONNECTION);
        bleDevice.createBond();   //start connect to device
        bleDevice.fetchUuidsWithSdp();   //start discover services on that device
        gattConnection = bleDevice.connectGatt(ctx, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.i(TAG, "onConnectionStateChange: " + gatt.getServices().size());
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    setConnectionState(ConnectionStatus.GATT_CONNECTED);
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // try to reconnect, ony update status (stop connection in Service, if not Paired)
                    setConnectionState(ConnectionStatus.GATT_DISCONNECTED);
                    if(!devicePaired)
                        disconnectDevice();
                }
                // ignore others
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.i(TAG, "onServicesDiscovered: " + gatt.getServices().size());
                super.onServicesDiscovered(gatt, status);
                for (BluetoothGattService foundService : gatt.getServices()) {
                    HashSet<BLEServiceManager.BLEService> requestedServices = servicesDefinitions.byService(foundService.getUuid());
                    if (requestedServices == null) continue;
                    Log.i(TAG, "Found " + requestedServices.size() + " Services for UUID:" + foundService.getUuid().toString());
                    for (BLEServiceManager.BLEService requestedService : requestedServices) {
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
                servicesDefinitions.byCharakteristic(characteristic.getUuid()).onValue(characteristic);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                servicesDefinitions.byCharakteristic(characteristic.getUuid()).onValue(characteristic);
            }
        });
    }

    public ConnectionStatus getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(ConnectionStatus newState) {
        if (connectionState == newState) return;
        connectionState = newState;
        callbacks.onConnectionStateChange(connectionState, this);
    }

    public void disconnectDevice() {
        if (gattConnection != null)
            gattConnection.disconnect();

        setConnectionState(ConnectionStatus.GATT_DISCONNECTED);
    }

    public String getName() {
        return bleDevice.getName();
    }

    public String getID() {
        return bleDevice.getAddress();
    }

    public enum ConnectionStatus {
        INIT_CONNECTION,
        GATT_CONNECTED,
        GATT_DISCONNECTED
    }

    public interface ConnectionStateCallbacks {
        void onConnectionStateChange(ConnectionStatus newState, RadmesserDevice instnace);
    }
}
