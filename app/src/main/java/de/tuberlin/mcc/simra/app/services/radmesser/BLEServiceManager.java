package de.tuberlin.mcc.simra.app.services.radmesser;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BLEServiceManager {
    private Map<UUID, BLEService> serviceMap                     = new HashMap<>();
    private Map<UUID, BLEService> characteristicUuidToServiceMap = new HashMap<>();

    public BLEServiceManager(BLEService... services) {
        for (BLEService service : services) {
            serviceMap.put(service.uuid, service);

            for (BLEServiceCharacteristic characteristic : service.characteristics) {
                characteristicUuidToServiceMap.put(characteristic.uuid, service);
            }
        }
    }

    public Set<UUID> getAllUUIDs() {
        return serviceMap.keySet();
    }

    public BLEService getServiceByUUID(UUID uuid) {
        return serviceMap.get(uuid);
    }

    public BLEService getServiceByCharacteristicUUID(UUID uuid) {
        return characteristicUuidToServiceMap.get(uuid);
    }

    public interface ValueCallback {
        void onValue(BluetoothGattCharacteristic characteristic);
    }

    public static class BLEService {
        public final UUID uuid;
        public final List<BLEServiceCharacteristic> characteristics;
        public boolean registered;

        public BLEService(String uuidString) {
            this.uuid = UUID.fromString(uuidString);
            this.characteristics = new ArrayList<>();
        }

        public BLEService addCharacteristic(String uuidString, ValueCallback callback) {
            this.characteristics.add(new BLEServiceCharacteristic(uuidString) {
                @Override
                public void onValue(BluetoothGattCharacteristic characteristic) {
                    callback.onValue(characteristic);
                }
            });

            return this;
        }

        public void onValue(BluetoothGattCharacteristic gattCharacteristic) {
            for (BLEServiceCharacteristic characteristic : characteristics) {
                if (characteristic.uuid.equals(gattCharacteristic.getUuid())) {
                    characteristic.onValue(gattCharacteristic);
                }
            }
        }
    }

    public static abstract class BLEServiceCharacteristic {
        public final UUID uuid;

        public BLEServiceCharacteristic(String uuidString) {
            this.uuid = UUID.fromString(uuidString);
        }

        public abstract void onValue(BluetoothGattCharacteristic characteristic);
    }
}
