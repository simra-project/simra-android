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
    //todo: place somewhere else, to avoid static state
    private HashMap<String, HashSet<BLEService>> byService = new HashMap<>();
    private HashMap<String, BLEService> byCharakteristic = new HashMap<>();


    @RequiresApi(api = Build.VERSION_CODES.N)
    public Set<UUID> getAllUUIDs() {
        return byService.keySet().stream().map(UUID::fromString).collect(Collectors.toSet());
    }

    public HashSet<BLEService> byService(UUID uuid) {
        return byService.get(uuid.toString());
    }

    public BLEService byCharakteristic(UUID uuid) {
        return byCharakteristic.get(uuid.toString());
    }

    public BLEService addService(String serviceUUID, String charackteristicUUIDs, ValueCallback callback) {

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

    public abstract class BLEService {
        public final String serviceUUID;
        public final String charackteristicUUIDs;

        public abstract void onValue(BluetoothGattCharacteristic characteristic);


        public boolean registered;

        public BLEService(String serviceUUID, String charackteristicUUIDs) {
            this.serviceUUID = serviceUUID;
            this.charackteristicUUIDs = charackteristicUUIDs;

            byCharakteristic.put(charackteristicUUIDs, this);

            if (byService.get(serviceUUID) == null)
                byService.put(serviceUUID, new HashSet<>());
            byService.get(serviceUUID).add(this);
        }
    }
}
