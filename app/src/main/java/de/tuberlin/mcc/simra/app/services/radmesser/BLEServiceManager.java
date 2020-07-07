package de.tuberlin.mcc.simra.app.services.radmesser;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BLEServiceManager {
    private HashMap<String, HashSet<BLEService>> byService = new HashMap<>();
    private HashMap<String, BLEService> byCharacteristic = new HashMap<>();

    public BLEServiceManager(BLEService... services) {
        for (BLEService service : services) {
            byCharacteristic.put(service.characteristicUUIDs.toString(), service);
            if (byService.get(service.serviceUUID.toString()) == null) {
                byService.put(service.serviceUUID.toString(), new HashSet<>());
            }

            byService.get(service.serviceUUID.toString()).add(service);
        }
    }

    public Set<UUID> getAllUUIDs() {
        HashSet<UUID> allUUIDs = new HashSet<>();
        for (String id : byService.keySet()) {
            allUUIDs.add(UUID.fromString(id));
        }
        return allUUIDs;

    }

    public HashSet<BLEService> byService(UUID uuid) {
        return byService.get(uuid.toString());
    }

    public BLEService byCharacteristic(UUID uuid) {
        return byCharacteristic.get(uuid.toString());
    }

    public static BLEService createService(ValueCallback callback, String serviceUUIDString, String... characteristicUUIDStrings) {
        UUID serviceUUID = UUID.fromString(serviceUUIDString);

        List<UUID> characteristicUUIDs = new ArrayList<>();
        for (String s : characteristicUUIDStrings) {
            characteristicUUIDs.add(UUID.fromString(s));
        }

        return new BLEService(serviceUUID, characteristicUUIDs) {
            @Override
            public void onValue(BluetoothGattCharacteristic characteristic) {
                callback.onValue(characteristic);
            }
        };
    }

    public interface ValueCallback {
        void onValue(BluetoothGattCharacteristic characteristic);
    }

    public static abstract class BLEService {
        public final UUID serviceUUID;
        public final List<UUID> characteristicUUIDs;
        public boolean registered;

        public BLEService(UUID serviceUUID, List<UUID> characteristicUUIDs) {
            this.serviceUUID = serviceUUID;
            this.characteristicUUIDs = characteristicUUIDs;
        }

        public abstract void onValue(BluetoothGattCharacteristic characteristic);
    }
}
