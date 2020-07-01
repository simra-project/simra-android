package de.tuberlin.mcc.simra.app.services;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
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

    public RadmesserDevice connectedDevice;
    BLEServiceManager serviceManager;
    private volatile HandlerThread mHandlerThread;

    public static ConnectionState getConnectionState() {
        return connectionState;
    }

    private static ConnectionState connectionState = ConnectionState.DISCONNECTED;

    private final IBinder binder = new LocalBinder();

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

    private static final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
    }

    public enum ConnectionState {
        DISCONNECTED,
        SEARCHING,
        PAIRING,
        CONNECTED,
        CONNECTION_REFUSED
    }

    public BLEScanner scanner = new BLEScanner(new BLEScanner.BLEScannerCallbacks() {
        @Override
        public void onNewDeviceFound(BluetoothDevice device) {
            boradcastDeviceFound(device.getName(), device.getAddress());
        }

        @Override
        public void onScanStarted() {
            setConnectionState(ConnectionState.SEARCHING);
        }

        @Override
        public void onScanFinished() {
            if (connectionState == ConnectionState.SEARCHING)
                setConnectionState(ConnectionState.DISCONNECTED);
        }
    });

    private RadmesserDevice.RadmesserDeviceConnectionStateCallback radmesserConnectionCallbacks = (RadmesserDevice.ConnectionStatus newState, RadmesserDevice instance) -> {
        if (instance != connectedDevice) return;
        // if Device disconnected
        if (newState == RadmesserDevice.ConnectionStatus.gattDisconnected) {
            // and Pairing not completed
            if (connectionState != ConnectionState.CONNECTED && connectionState != ConnectionState.DISCONNECTED) {
                // Uncomplete Pairing -> diconnect from Radmesser
                disconnectAndUnpairDevice();
                setConnectionState(ConnectionState.CONNECTION_REFUSED);
                Log.i(TAG, "CONNECTION_REFUSED");
            } else {
                // connection lost
                setConnectionState(ConnectionState.DISCONNECTED);
            }
        }

        if(newState== RadmesserDevice.ConnectionStatus.gattConnected){
            setConnectionState(instance.connectionSucceded?ConnectionState.CONNECTED:ConnectionState.PAIRING);
        }
    };


    public ConnectionState getCurrentConnectionStatus() {
        return connectionState;
    }

    private void setConnectionState(ConnectionState newStatus) {
        if (this.connectionState == newStatus)
            return;

        this.connectionState = newStatus;
        boradcastConnectionStateChanged(newStatus);
        ForegroundServiceNotificationManager.createOrUpdateNotification(
                this,
                "SimRa RadmesseR connection",
                newStatus.toString()
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "created");
        serviceManager = registerBLEServices();

        LocalBroadcastManager mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mHandlerThread = new HandlerThread(TAG + ".HandlerThread");
        mHandlerThread.start();
        ServiceHandler mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());

        Notification notification =
                ForegroundServiceNotificationManager.createOrUpdateNotification(
                        this,
                        "SimRa RadmesseR connection",
                        "Disconnected"
                );

       /* mServiceHandler.post(() -> {
            serviceManager = registerBLEServices();
            scanner.scanDevices(serviceManager);
            // stopSelf();
        });*/

        startForeground(ForegroundServiceNotificationManager.getNotificationId(), notification);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "stopped");
        // Cleanup service before destruction
        ForegroundServiceNotificationManager.cancelNotification(this);
        mHandlerThread.quit();
    }

    private BLEServiceManager registerBLEServices() {

        BLEServiceManager bleServices = new BLEServiceManager();
        bleServices.addService(
                RadmesserDevice.UUID_SERVICE_HEARTRATE,
                RadmesserDevice.UUID_SERVICE_CHARACTERISTIC_HEARTRATE,
                val -> Log.i("onHeartRate", String.valueOf(val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1)))
        );

        bleServices.addService(
                RadmesserDevice.UUID_SERVICE_CLOSEPASS,
                RadmesserDevice.UUID_SERVICE_CHARACTERISTIC_CLOSEPASS,
                val -> boradcasClosePassIncedent(val.getStringValue(0))
        );

        bleServices.addService(
                RadmesserDevice.UUID_SERVICE_DISTANCE,
                RadmesserDevice.UUID_SERVICE_CHARACTERISTIC_DISTANCE,
                val -> boradcastDistanceValue(val.getStringValue(0))
        );

        bleServices.addService(
                RadmesserDevice.UUID_SERVICE_CONNECTION,
                RadmesserDevice.UUID_SERVICE_CHARACTERISTIC_CONNECTION,
                val -> {
                    connectedDevice.connectionSucceded = true;
                    Log.i(TAG, "new CONNECTION Value:" + val.getStringValue(0));
                    String strVal = val.getStringValue(0);
                    if (strVal != null && strVal.equals("1")) {
                        setConnectionState(ConnectionState.CONNECTED);
                        setPairedRadmesserID(connectedDevice.getID(), this);
                    }
                }
        );
        return bleServices;
    }

    // ## incoming communication
    // Action-Requests
    final static String ACTION_START_SCANN = "de.tuberlin.mcc.simra.app.radmesserservice.ACTION_START_SCANN";
    final static String ACTION_CONNECT_DEVICE = "de.tuberlin.mcc.simra.app.radmesserservice.ACTION_CONNECT_DEVICE";
    final static String ACTION_DISCONNECT_AND_UNPAIR = "de.tuberlin.mcc.simra.app.radmesserservice.ACTION_DISCONNECT_AND_UNPAIR";
    final static String ACTION_STOP_SERVICE = "de.tuberlin.mcc.simra.app.radmesserservice.ACTION_STOP_SERVICE";
    final static String EXTRA_CONNECT_DEVICE = "de.tuberlin.mcc.simra.app.radmesserservice.EXTRA_CONNECT_DEVICE";

    // incoming Action-Requests
    public static void startScanning(Context ctx) {
        Intent intent = new Intent(ctx, RadmesserService.class);
        intent.setAction(ACTION_START_SCANN);
        ctx.startService(intent);
    }

    public static void connectDevice(Context ctx, String deviceId) {
        Intent intent = new Intent(ctx, RadmesserService.class);
        intent.setAction(ACTION_CONNECT_DEVICE);
        intent.putExtra(EXTRA_CONNECT_DEVICE, deviceId);
        ctx.startService(intent);
    }

    /*
     * returns false if no Radmesser is Paired
     */
    public static boolean tryConnectPairedDevice(Context ctx) {
        String connectedDevice = getPairedRadmesserID(ctx);
        if (connectedDevice == null)
            return false;

        Intent intent = new Intent(ctx, RadmesserService.class);
        intent.setAction(ACTION_CONNECT_DEVICE);
        intent.putExtra(EXTRA_CONNECT_DEVICE, connectedDevice);
        ctx.startService(intent);
        return true;
    }

    public static void disconnectAndUnpairDevice(Context ctx) {
        Intent intent = new Intent(ctx, RadmesserService.class);
        intent.setAction(ACTION_DISCONNECT_AND_UNPAIR);
        ctx.startService(intent);
    }

    public static void terminateService(Context ctx) {
        Intent intent = new Intent(ctx, RadmesserService.class);
        intent.setAction(ACTION_STOP_SERVICE);
        ctx.startService(intent);
    }

    // internal routing of Action-Requests
    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;
        String action = intent.getAction();
        Log.i("Intent", action);
        if (action.equals(ACTION_START_SCANN))
            startScanning();
        else if (action.equals(ACTION_CONNECT_DEVICE))
            connectDevice(intent.getStringExtra(EXTRA_CONNECT_DEVICE));
        else if (action.equals(ACTION_DISCONNECT_AND_UNPAIR))
            disconnectAndUnpairDevice();
        else if (action.equals(ACTION_STOP_SERVICE))
            terminateService();
    }

    // internal processing of Action-Requests
    private void startScanning() {
        scanner.scanDevices(serviceManager);
    }

    private void connectDevice(String deviceId) {
        disconnectAnyRadmesser();

        // search device
        scanner.findDeviceById(deviceId,
                device -> {
                    setConnectionState(ConnectionState.PAIRING);
                    connectedDevice = new RadmesserDevice(device, radmesserConnectionCallbacks, serviceManager);
                    connectedDevice.connect(this);
                });
    }

    private void disconnectAndUnpairDevice() {
        disconnectAnyRadmesser();
        unPairedRadmesser();
    }

    private void terminateService() {
        disconnectAnyRadmesser();
        stopSelf();
    }

    private void disconnectAnyRadmesser() {
        setConnectionState(ConnectionState.DISCONNECTED);
        if (connectedDevice != null)
            connectedDevice.disconnectDevice();
        connectedDevice = null;
    }


    // ## outgoing communication

    // Broadcasts
    final static String ACTION_DEVICE_FOUND = "de.tuberlin.mcc.simra.app.radmesserservice.actiondevicefound";
    final static String ACTION_CONNECTION_STATE_CHANGED = "de.tuberlin.mcc.simra.app.radmesserservice.actiondconnectionstatechanged";
    final static String ACTION_VALE_RECEIVED_CLOSEPASS = "de.tuberlin.mcc.simra.app.radmesserservice.actiondvaluereceivedclosepass";
    final static String ACTION_VALE_RECEIVED_DISTANCE = "de.tuberlin.mcc.simra.app.radmesserservice.actiondvaluereceiveddistance";

    final static String EXTRA_DEVICE_ID = "de.tuberlin.mcc.simra.app.radmesserservice.extraid";
    final static String EXTRA_DEVICE_NAME = "de.tuberlin.mcc.simra.app.radmesserservice.extraname";
    final static String EXTRA_CONNECTION_STATE = "de.tuberlin.mcc.simra.app.radmesserservice.extraconnectionstate";
    final static String EXTRA_VALUE = "de.tuberlin.mcc.simra.app.radmesserservice.extravalue";


    private void boradcastDeviceFound(String deviceName, String deviceId) {
        Intent intent = new Intent(ACTION_DEVICE_FOUND);
        intent.putExtra(EXTRA_DEVICE_ID, deviceId);
        intent.putExtra(EXTRA_DEVICE_NAME, deviceName);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void boradcastConnectionStateChanged(ConnectionState newState) {
        Intent intent = new Intent(ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(EXTRA_CONNECTION_STATE, newState.toString());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void boradcasClosePassIncedent(String value) {
        Intent intent = new Intent(ACTION_VALE_RECEIVED_CLOSEPASS);
        intent.putExtra(EXTRA_VALUE, value);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void boradcastDistanceValue(String value) {
        Intent intent = new Intent(ACTION_VALE_RECEIVED_DISTANCE);
        intent.putExtra(EXTRA_VALUE, value);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public abstract static class RadmesserServiceCallbacks {
        public void onDeviceFound(String deviceName, String deviceId) {
        }

        public void onConnectionStateChanged(ConnectionState newState) {
        }

        public void onClosePassIncedent(String value) {
        }

        public void onDistanceValue(String value) {
        }
    }

    /*
     * the caller ist responible for unregistering thr receiver, when he does not need him anymore
     * */
    public static BroadcastReceiver registerCallbacks(Context ctx, RadmesserServiceCallbacks callbacks) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DEVICE_FOUND);
        filter.addAction(ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(ACTION_VALE_RECEIVED_CLOSEPASS);
        filter.addAction(ACTION_VALE_RECEIVED_DISTANCE);

        BroadcastReceiver rec = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case ACTION_DEVICE_FOUND:
                        callbacks.onDeviceFound(
                                intent.getStringExtra(EXTRA_DEVICE_NAME),
                                intent.getStringExtra(EXTRA_DEVICE_ID)
                        );
                        break;
                    case ACTION_CONNECTION_STATE_CHANGED:
                        callbacks.onConnectionStateChanged(
                                ConnectionState.valueOf(intent.getStringExtra(EXTRA_CONNECTION_STATE))
                        );
                        break;
                    case ACTION_VALE_RECEIVED_CLOSEPASS:
                        callbacks.onClosePassIncedent(
                                intent.getStringExtra(EXTRA_VALUE)
                        );
                        break;
                    case ACTION_VALE_RECEIVED_DISTANCE:
                        callbacks.onDistanceValue(
                                intent.getStringExtra(EXTRA_VALUE)
                        );
                        break;
                }
            }
        };
        LocalBroadcastManager.getInstance(ctx).registerReceiver(rec, filter);
        return rec;
    }

    public static void unRegisterCallbacks(BroadcastReceiver receiver, Context ctx) {
        if (receiver != null)
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(receiver);
    }


    // TODO: Use Utils (or refactor) shared Prefs usage

    public void unPairedRadmesser() {
        getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).edit().remove(sharedPrefsKeyRadmesserID).apply();
    }

    @Nullable
    private static String getPairedRadmesserID(Context ctx) {
        return ctx.getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).getString(sharedPrefsKeyRadmesserID, null);
    }

    private static void setPairedRadmesserID(String id, Context ctx) {
        ctx.getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).edit().putString(sharedPrefsKeyRadmesserID, id).apply();
    }


}
