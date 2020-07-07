package de.tuberlin.mcc.simra.app.services.radmesser;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BLEServiceManager {
    // TODO: Why is this a set? Why should one UUID map to a set of services?
    // TODO: Each service is only detectable through its unique Id (-> UUID ;-) ) so there should never be more than one service for a given UUID
    // TODO: Refactor!
    private Map<UUID, Set<BLEService>> byService        = new HashMap<>();
    private Map<UUID, BLEService>      byCharacteristic = new HashMap<>();

    public BLEServiceManager(BLEService... services) {
        for (BLEService service : services) {
            for (UUID characteristicUUID : service.characteristicUUIDs) {
                byCharacteristic.put(characteristicUUID, service);
            }

            // TODO: This will *always* occur because for a given UUID, there will never be a service with the same UUID already in the list.
            if (byService.get(service.serviceUUID) == null) {
                byService.put(service.serviceUUID, new HashSet<>());
            }

            byService.get(service.serviceUUID).add(service);
        }
    }

    public Set<UUID> getAllUUIDs() {
        Set<UUID> allUUIDs = new HashSet<>();
        for (UUID id : byService.keySet()) {
            allUUIDs.add(id);
        }
        return allUUIDs;

    }

    public Set<BLEService> byService(UUID uuid) {
        return byService.get(uuid);
    }

    public BLEService byCharacteristic(UUID uuid) {
        return byCharacteristic.get(uuid);
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
