package de.tuberlin.mcc.simra.app.services;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import de.tuberlin.mcc.simra.app.services.radmesser.BLEScanner;
import de.tuberlin.mcc.simra.app.services.radmesser.BLEServiceManager;
import de.tuberlin.mcc.simra.app.services.radmesser.RadmesserDevice;
import de.tuberlin.mcc.simra.app.util.ForegroundServiceNotificationManager;

public class RadmesserService extends Service {

    private static final String TAG = "RadmesserService";
    private static final String sharedPrefsKey = "RadmesserServiceBLE";
    private static final String sharedPrefsKeyRadmesserID = "connectedDevice";

    private volatile HandlerThread mHandlerThread;
    private ServiceHandler mServiceHandler;
    private LocalBroadcastManager mLocalBroadcastManager;


    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
    }

    private ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;

    public ConnectionStatus getCurrentConnectionStatus() {
        return connectionStatus;
    }

    private void setConnectionStatus(ConnectionStatus newStatus) {
        this.connectionStatus = newStatus;
        serviceCallbacks.connectionStatusChanged(newStatus);
        ForegroundServiceNotificationManager.createOrUpdateNotification(
                this,
                "SimRa RadmesseR connection",
                newStatus.toString()
        );
    }

    @Override
    public void onCreate(){
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mHandlerThread = new HandlerThread(TAG + ".HandlerThread");
        mHandlerThread.start();
        mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());

        Notification notification =
                ForegroundServiceNotificationManager.createOrUpdateNotification(
                        this,
                        "SimRa RadmesseR connection",
                        "Disconnected"
                );

        mServiceHandler.post(() -> {
            BLEServiceManager serviceManager = registerBLEServices();
            scanner.connectAnyNewDevice(
                    serviceManager,
                    device -> {
                        connectedDevice = new RadmesserDevice(device, radmesserCallbacks, serviceManager);
                        connectedDevice.connect(this);
                        setConnectionStatus(ConnectionStatus.PAIRING);
                    }
            );
            // stopSelf();
        });

        startForeground(ForegroundServiceNotificationManager.getNotificationId(), notification);
    }

    @Override
    public void onDestroy(){
        // Cleanup service before destruction
        mHandlerThread.quit();
    }

    /**
     * Bound Service interface
     */
    private final IBinder binder = new LocalBinder();
    private RadmesserServiceCallbacks serviceCallbacks;
    public class LocalBinder extends Binder {
        public RadmesserService getService() {

            // Return this instance of LocalService so clients can call public methods
            return RadmesserService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void registerCallbacks(RadmesserServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

    public void unregisterCallback(){
        serviceCallbacks = null;
    }

    private RadmesserDevice connectedDevice;

    private BLEScanner scanner = new BLEScanner(new BLEScanner.BLEScannerCallbacks() {
        @Override
        public void onNewDeviceFound(BluetoothDevice device) {
        }

        @Override
        public void onScanStarted() {
            setConnectionStatus(ConnectionStatus.SEARCHING);
        }

        @Override
        public void onScanStopped() {

        }
    });

    private RadmesserDevice.RadmesserDeviceCallbacks radmesserCallbacks = new RadmesserDevice.RadmesserDeviceCallbacks() {
        @Override
        public void onConnectionStateChange() {
        }
    };



    private BLEServiceManager registerBLEServices() {

        BLEServiceManager bleServices = new BLEServiceManager();
        bleServices.addService(
                RadmesserDevice.UUID_SERVICE_HEARTRATE,
                RadmesserDevice.UUID_SERVICE_CHARACTERISTIC_HEARTRATE,
                val -> Log.i(TAG, "new HEARTRATE Value:" + val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1))
        );


        bleServices.addService(
                RadmesserDevice.UUID_SERVICE_CLOSEPASS,
                RadmesserDevice.UUID_SERVICE_CHARACTERISTIC_CLOSEPASS,
                val -> Log.i(TAG, "new CLOSEPASS Value:" + val.getStringValue(0))
        );

        bleServices.addService(
                RadmesserDevice.UUID_SERVICE_DISTANCE,
                RadmesserDevice.UUID_SERVICE_CHARACTERISTIC_DISTANCE,
                val -> Log.i(TAG, "new DISTANCE Value:" + val.getStringValue(0))
        );

        bleServices.addService(
                RadmesserDevice.UUID_SERVICE_CONNECTION,
                RadmesserDevice.UUID_SERVICE_CHARACTERISTIC_CONNECTION,
                val -> {
                    Log.i(TAG, "new CONNECTION Value:" + val.getStringValue(0));
                    if (val.getStringValue(0).equals("1")) {
                        setConnectionStatus(ConnectionStatus.CONNECTED);
                        setPairedRadmesserID(connectedDevice.getID(), this);
                    }
                }
        );
        return bleServices;
    }


//    public void scanAndConnectToRandomRadmesser() {
//        BLEServiceManager serviceManager = registerBLEServices();
//        scanner = new BLEScanner(callbacks);
//        scanner.connectAnyNewDevice(
//                serviceManager,
//                device -> {
//                    connectedDevice = new RadmesserDevice(device, radmesserCallbacks, serviceManager);
//                    connectedDevice.connect(this);
//                }
//        );
//
//    }
//
//    private void scanAndConnectToPairedRadmesser() {
//        BLEServiceManager serviceManager = registerBLEServices();
//        scanner = new BLEScanner(callbacks);
//        scanner.tryConnectPairedDevice(
//                getPairedRadmesserID(),
//                device -> {
//                    connectedDevice = new RadmesserDevice(device, radmesserCallbacks, serviceManager);
//                    connectedDevice.connect(this);
//                }
//        );
//    }
//
//    private boolean connectToRadmesserByID(String id) {
//        BLEServiceManager serviceManager = registerBLEServices();
//        HashMap<String, BluetoothDevice> foundDevices = scanner.getFoundDevices();
//        if (!foundDevices.containsKey(id))
//            return false;
//
//        connectedDevice = new RadmesserDevice(foundDevices.get(id), radmesserCallbacks, serviceManager);
//        connectedDevice.connect(this);
//
//        return true;
//    }


    // TODO: Use Utils (or refactor) shared Prefs usage
    @Nullable
    private String getPairedRadmesserID() {
        return getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).getString(sharedPrefsKeyRadmesserID, null);
    }

    private static void setPairedRadmesserID(String id, Context ctx) {
        ctx.getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).edit().putString(sharedPrefsKeyRadmesserID, id).apply();
    }

    public void unPairedRadmesser() {
        getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).edit().remove(sharedPrefsKeyRadmesserID).apply();
    }

    public interface RadmesserServiceCallbacks {
        void connectionStatusChanged(RadmesserService.ConnectionStatus status);
    }

    public enum ConnectionStatus {
        DISCONNECTED,
        SEARCHING ,
        PAIRING,
        CONNECTED
    }
}

