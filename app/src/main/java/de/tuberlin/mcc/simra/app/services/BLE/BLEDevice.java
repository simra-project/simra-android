package de.tuberlin.mcc.simra.app.services.BLE;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

public class BLEDevice {
    /**
     * Bluetooth Service UUIDs
     */
    public static final String UUID_SERVICE_OBS = "1FE7FAF9-CE63-4236-0004-000000000000";
    public static final String UUID_SERVICE_OBS_CHAR_SENSOR_DISTANCE = "1FE7FAF9-CE63-4236-0004-000000000002";
    public static final String UUID_SERVICE_OBS_CHAR_ClOSE_PASS = "1FE7FAF9-CE63-4236-0004-000000000003";
    public static final String UUID_SERVICE_OBS_CHAR_TIME = "1FE7FAF9-CE63-4236-0004-000000000001";

    public static final String UUID_CCC_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    private final String TAG = "OpenBikeSensorDevice_LOG";
    public boolean devicePaired = true; // needed from the outside
    private final BluetoothDevice bleDevice;
    private final ConnectionStateCallbacks callbacks;
    private BLEServiceManager servicesDefinitions;
    private BluetoothGatt gattConnection;
    private Context ctx;
    private ConnectionStatus connectionState = ConnectionStatus.GATT_DISCONNECTED;

    public BLEDevice(BluetoothDevice bleDevice, ConnectionStateCallbacks stateCallbacks, BLEServiceManager servicesDefinitions, Context parentContext) {
        this.bleDevice = bleDevice;
        this.callbacks = stateCallbacks;
        this.servicesDefinitions = servicesDefinitions;
        this.ctx = parentContext;
        connect();
    }

    private void connect() {
        setConnectionState(ConnectionStatus.INIT_CONNECTION);
        bleDevice.createBond(); // start connecting to device
        bleDevice.fetchUuidsWithSdp(); // start discovering services on that device
        gattConnection = bleDevice.connectGatt(ctx, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.i(TAG, "onConnectionStateChange: " + gatt.getServices().size());
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    setConnectionState(ConnectionStatus.GATT_CONNECTED);
                    gatt.discoverServices(); // callback -> BLEDevice.onServicesDiscovered
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // try to reconnect, ony update status (stop connection in Service, if not Paired)
                    setConnectionState(ConnectionStatus.GATT_DISCONNECTED);

                    if (!devicePaired) {
                        disconnectDevice();
                    }
                }
                // ignore others
            }

            /**
             * Reads OBS time to determine obsStartTime. callback -> onCharacteristicRead
             */
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.i(TAG, "onServicesDiscovered: " + gatt.getServices().size());
                super.onServicesDiscovered(gatt, status);
                BluetoothGattCharacteristic obsTimeCharacteristic = gatt.getService(UUID.fromString(UUID_SERVICE_OBS)).getCharacteristic(UUID.fromString(UUID_SERVICE_OBS_CHAR_TIME));
                gatt.readCharacteristic(obsTimeCharacteristic);
            }

            /**
             * Gets called when sensor distance notifications are successfully subscribed to.
             * This is done in onCharacteristicRead(), after the OBS start time is read.
             * Sends a request to the OBS to subscribe to close pass notifications.
             */
            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                UUID thisCharacteristicsUUID = descriptor.getCharacteristic().getUuid();
                if (status == 0) {
                    Log.d(TAG, "successfully subscribed to " + thisCharacteristicsUUID);
                } else {
                    Log.e(TAG, "onDescriptorWrite not successful! Status: " + status);
                }
                Log.d(TAG, thisCharacteristicsUUID.toString().toUpperCase() + ".equals(" + UUID_SERVICE_OBS_CHAR_SENSOR_DISTANCE.toUpperCase() +") = " + (thisCharacteristicsUUID.toString().equals(UUID_SERVICE_OBS_CHAR_SENSOR_DISTANCE)));
                if (thisCharacteristicsUUID.equals(UUID.fromString(UUID_SERVICE_OBS_CHAR_SENSOR_DISTANCE))) {
                    BluetoothGattCharacteristic closePassCharacteristic = gatt.getService(UUID.fromString(UUID_SERVICE_OBS)).getCharacteristic(UUID.fromString(UUID_SERVICE_OBS_CHAR_ClOSE_PASS));
                    enableNotification(gatt,closePassCharacteristic);
                }


            }

            /**
             * Gets called when obs time is successfully read.
             * This is done in onServicesDiscovered().
             * Sends a request to the OBS to subscribe to sensor distance notifications.
             */
            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                BluetoothGattCharacteristic sensorDistanceCharacteristic = gatt.getService(UUID.fromString(UUID_SERVICE_OBS)).getCharacteristic(UUID.fromString(UUID_SERVICE_OBS_CHAR_SENSOR_DISTANCE));
                enableNotification(gatt,sensorDistanceCharacteristic);
                servicesDefinitions.getServiceByCharacteristicUUID(characteristic.getUuid()).onValue(characteristic);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                servicesDefinitions.getServiceByCharacteristicUUID(characteristic.getUuid()).onValue(characteristic);
            }
        });
    }

    private void enableNotification(BluetoothGatt gatt, BluetoothGattCharacteristic gattCharacteristic) {
        byte[] payload = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        BluetoothGattDescriptor cccDescriptor = gattCharacteristic.getDescriptor(UUID.fromString(UUID_CCC_DESCRIPTOR));
        if (payload != null && cccDescriptor != null) {
            cccDescriptor.setValue(payload);
            gatt.writeDescriptor(cccDescriptor);
            gatt.setCharacteristicNotification(gattCharacteristic, true);
        }
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
        if (gattConnection != null) {
            gattConnection.disconnect();
        }

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
        void onConnectionStateChange(ConnectionStatus newState, BLEDevice instnace);
    }
}
