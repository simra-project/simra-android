package de.tuberlin.mcc.simra.app.services.radmesser;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class BLEServiceManager {
    private HashMap<String, HashSet<BLEService>> byService = new HashMap<>();
    private HashMap<String, BLEService> byCharakteristic = new HashMap<>();

    public BLEServiceManager(BLEService... services) {
        for (BLEService service : services) {
            byCharakteristic.put(service.charackteristicUUIDs, service);
            if (byService.get(service.serviceUUID) == null)
                byService.put(service.serviceUUID, new HashSet<>());
            byService.get(service.serviceUUID).add(service);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public Set<UUID> getAllUUIDs() {
        return byService.keySet().stream().map(UUID::fromString).collect(Collectors.toSet());   //todo: make backward compartible
    }

    public HashSet<BLEService> byService(UUID uuid) {
        return byService.get(uuid.toString());
    }

    public BLEService byCharakteristic(UUID uuid) {
        return byCharakteristic.get(uuid.toString());
    }

    public static BLEService createService(String serviceUUID, String charackteristicUUIDs, ValueCallback callback) {

        // sanity-check and format correctly
        serviceUUID = UUID.fromString(serviceUUID).toString();
        charackteristicUUIDs = UUID.fromString(charackteristicUUIDs).toString();

        return new BLEService(serviceUUID, charackteristicUUIDs) {
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
        public final String serviceUUID;
        public final String charackteristicUUIDs;
        public boolean registered;

        public BLEService(String serviceUUID, String charackteristicUUIDs) {
            this.serviceUUID = serviceUUID;
            this.charackteristicUUIDs = charackteristicUUIDs;
        }

        public abstract void onValue(BluetoothGattCharacteristic characteristic);
    }
}
