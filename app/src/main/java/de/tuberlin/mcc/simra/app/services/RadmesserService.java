package de.tuberlin.mcc.simra.app.services;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.ArrayList;
import de.tuberlin.mcc.simra.app.services.radmesser.BLEScanner;
import de.tuberlin.mcc.simra.app.services.radmesser.BLEServiceManager;
import de.tuberlin.mcc.simra.app.services.radmesser.RadmesserDevice;
import de.tuberlin.mcc.simra.app.util.ForegroundServiceNotificationManager;

public class RadmesserService extends Service {
    private static final String TAG = "RadmesserService";
    private static final String sharedPrefsKey = "RadmesserServiceBLE";
    private static final String sharedPrefsKeyRadmesserID = "connectedDevice";
    private RadmesserDevice connectedDevice;
    private volatile HandlerThread mHandlerThread;
    private BLEScanner bluetoothScanner;
    private LocalBroadcastManager broadcastManager;
    public static ConnectionState getConnectionState() {
        return connectionState;
    }
    private static ConnectionState connectionState = ConnectionState.DISCONNECTED;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
    }

    //todo: refactor
    public enum ConnectionState {
        DISCONNECTED,
        SEARCHING,
        PAIRING,
        CONNECTED,
        CONNECTION_REFUSED
    }

    private synchronized void setConnectionState(ConnectionState newState) {
        if (this.connectionState == newState)
            return;

        this.connectionState = newState;
        boradcastConnectionStateChanged(newState);
        ForegroundServiceNotificationManager.createOrUpdateNotification(
                this,
                "SimRa RadmesseR connection",
                newState.toString()
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        bluetoothScanner = new BLEScanner(scannerStatusCallbacks);
        broadcastManager = LocalBroadcastManager.getInstance(this);
        mHandlerThread = new HandlerThread(TAG + ".HandlerThread");
        mHandlerThread.start();
        ServiceHandler mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());

        Notification notification =
                ForegroundServiceNotificationManager.createOrUpdateNotification(
                        this,
                        "SimRa RadmesseR connection",
                        "Disconnected"
                );
        startForeground(ForegroundServiceNotificationManager.getNotificationId(), notification);
    }

    @Override
    public void onDestroy() {
        // Cleanup service before destruction
        ForegroundServiceNotificationManager.cancelNotification(this);
        mHandlerThread.quit();
    }

    //todo: refactor
    public BLEScanner.BLEScanCallbacks scannerStatusCallbacks = new BLEScanner.BLEScanCallbacks() {

        @Override
        public void onScanStarted() {
            setConnectionState(ConnectionState.SEARCHING);
        }

        @Override
        public void onScanSelfFinished() {
            if (connectionState == ConnectionState.SEARCHING && connectedDevice == null)
                setConnectionState(ConnectionState.DISCONNECTED);
            else if (connectedDevice != null)   //restore connection state of the current device, after scan
                radmesserConnectionCallbacks.onConnectionStateChange(connectedDevice.getConnectionState(), connectedDevice);
        }

        @Override
        public void onScanAborted() {
            if (connectionState == ConnectionState.SEARCHING && connectedDevice == null)
                setConnectionState(ConnectionState.DISCONNECTED);
            else if (connectedDevice != null) //restore connection state of the current device, after scan
                radmesserConnectionCallbacks.onConnectionStateChange(connectedDevice.getConnectionState(), connectedDevice);
        }
    };

    private RadmesserDevice.ConnectionStateCallbacks radmesserConnectionCallbacks = (RadmesserDevice.ConnectionStatus newState, RadmesserDevice instance) -> {
        if (instance != connectedDevice) return;    // only interested in current device

        switch (newState) {
            case GATT_CONNECTED:
                if (!instance.devicePaired)
                    setConnectionState(ConnectionState.PAIRING);
                else
                    setConnectionState(ConnectionState.CONNECTED);
                break;

            case GATT_DISCONNECTED:
                if (!instance.devicePaired)
                    setConnectionState(ConnectionState.CONNECTION_REFUSED);
                else
                    setConnectionState(ConnectionState.DISCONNECTED);
        }
    };


    private BLEServiceManager radmesserServicesDefinition = new BLEServiceManager(
            BLEServiceManager.createService(
                    RadmesserDevice.UUID_SERVICE_HEARTRATE,
                    RadmesserDevice.UUID_SERVICE_CHARACTERISTIC_HEARTRATE,
                    val -> Log.i("onHeartRate", String.valueOf(val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1)))
            ),

            BLEServiceManager.createService(
                    RadmesserDevice.UUID_SERVICE_CLOSEPASS,
                    RadmesserDevice.UUID_SERVICE_CHARACTERISTIC_CLOSEPASS,
                    val -> boradcasClosePassIncedent(val.getStringValue(0))
            ),

            BLEServiceManager.createService(
                    RadmesserDevice.UUID_SERVICE_DISTANCE,
                    RadmesserDevice.UUID_SERVICE_CHARACTERISTIC_DISTANCE,
                    val -> boradcastDistanceValue(val.getStringValue(0))
            ),

            BLEServiceManager.createService(
                    RadmesserDevice.UUID_SERVICE_CONNECTION,
                    RadmesserDevice.UUID_SERVICE_CHARACTERISTIC_CONNECTION,
                    val -> {
                        connectedDevice.devicePaired = true;
                        Log.i(TAG, "new CONNECTION Value:" + val.getStringValue(0));
                        String strVal = val.getStringValue(0);
                        if (strVal != null && strVal.equals("1")) {
                            setConnectionState(ConnectionState.CONNECTED);
                            setPairedRadmesserID(connectedDevice.getID(), this);
                        }
                    }
            )
    );


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
        Log.i(TAG, "Intent received " + action);
        switch (intent.getAction()) {
            case ACTION_START_SCANN:
                startScanning();
                break;
            case ACTION_CONNECT_DEVICE:
                connectDevice(intent.getStringExtra(EXTRA_CONNECT_DEVICE));
                break;
            case ACTION_DISCONNECT_AND_UNPAIR:
                disconnectAndUnpairDevice();
                break;
            case ACTION_STOP_SERVICE:
                terminateService();
                break;
        }
    }

    // internal processing of Action-Requests
    private void startScanning() {
        bluetoothScanner.findDevicesByServices(radmesserServicesDefinition,
                device -> boradcastDeviceFound(device.getName(), device.getAddress())
        );
    }

    private void connectDevice(String deviceId) {
        disconnectAndUnpairDevice();
        bluetoothScanner.findDeviceById(deviceId,
                device -> connectedDevice = new RadmesserDevice(device, radmesserConnectionCallbacks, radmesserServicesDefinition, this)
        );
    }

    private void disconnectAndUnpairDevice() {
        disconnectAnyRadmesser();
        unPairedRadmesser();
    }

    private void disconnectAnyRadmesser() {
        setConnectionState(ConnectionState.DISCONNECTED);
        if (connectedDevice != null)
            connectedDevice.disconnectDevice();
        connectedDevice = null;
    }

    private void terminateService() {
        disconnectAnyRadmesser();
        stopSelf();
    }
    // ## outgoing communication

    // Broadcasts
    final static String ACTION_DEVICE_FOUND = "de.tuberlin.mcc.simra.app.radmesserservice.actiondevicefound";
    final static String ACTION_CONNECTION_STATE_CHANGED = "de.tuberlin.mcc.simra.app.radmesserservice.actiondconnectionstatechanged";
    final static String ACTION_VALUE_RECEIVED_CLOSEPASS = "de.tuberlin.mcc.simra.app.radmesserservice.actiondvaluereceivedclosepass";
    final static String ACTION_VALUE_RECEIVED_DISTANCE = "de.tuberlin.mcc.simra.app.radmesserservice.actiondvaluereceiveddistance";

    final static String EXTRA_DEVICE_ID = "de.tuberlin.mcc.simra.app.radmesserservice.extraid";
    final static String EXTRA_DEVICE_NAME = "de.tuberlin.mcc.simra.app.radmesserservice.extraname";
    final static String EXTRA_CONNECTION_STATE = "de.tuberlin.mcc.simra.app.radmesserservice.extraconnectionstate";
    final static String EXTRA_VALUE = "de.tuberlin.mcc.simra.app.radmesserservice.extravalue";


    private void boradcastDeviceFound(String deviceName, String deviceId) {
        Intent intent = new Intent(ACTION_DEVICE_FOUND);
        intent.putExtra(EXTRA_DEVICE_ID, deviceId);
        intent.putExtra(EXTRA_DEVICE_NAME, deviceName);
        broadcastManager.sendBroadcast(intent);
    }

    private void boradcastConnectionStateChanged(ConnectionState newState) {
        Intent intent = new Intent(ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(EXTRA_CONNECTION_STATE, newState.toString());
        broadcastManager.sendBroadcast(intent);
    }

    private void boradcasClosePassIncedent(String value) {
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_CLOSEPASS);
        intent.putExtra(EXTRA_VALUE, value);
        broadcastManager.sendBroadcast(intent);
    }

    private void boradcastDistanceValue(String value) {
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_DISTANCE);
        intent.putExtra(EXTRA_VALUE, value);
        broadcastManager.sendBroadcast(intent);
    }

    public abstract static class RadmesserServiceCallbacks {
        public void onDeviceFound(String deviceName, String deviceId) {
        }

        public void onConnectionStateChanged(ConnectionState newState) {
        }

        public void onClosePassIncedent(String raw) {     //todo: change type to Measurement
        }

        public void onDistanceValue(String raw) {         //todo: change type to Measurement
        }
    }

    /*
     * the caller ist responible for unregistering thr receiver, when he does not need him anymore
     * */
    public static BroadcastReceiver registerCallbacks(Context ctx, RadmesserServiceCallbacks callbacks) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DEVICE_FOUND);
        filter.addAction(ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(ACTION_VALUE_RECEIVED_CLOSEPASS);
        filter.addAction(ACTION_VALUE_RECEIVED_DISTANCE);

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
                    case ACTION_VALUE_RECEIVED_CLOSEPASS:    //todo:  call parseMesuarementLine() and return result
                        callbacks.onClosePassIncedent(
                                intent.getStringExtra(EXTRA_VALUE)
                        );
                        break;
                    case ACTION_VALUE_RECEIVED_DISTANCE:     //todo:  call parseMesuarementLine() and return result
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

    private Measurement parseMesuarementLine(String line) {
        if (line.equals(""))
            return null;

        try {
            String[] sections = line.split(";");

            long timestamp = Long.parseLong(sections[0]);
            return new Measurement(timestamp, parseValues(sections[1].split(",")), parseValues(sections[2].split(",")));

        } catch (ArrayIndexOutOfBoundsException iex) {
            return null;
        } catch (NumberFormatException fex) {
            return null;
        }
    }

    private ArrayList<Integer> parseValues(String[] values) {
        ArrayList<Integer> vaalueList = new ArrayList<>();

        for (String value : values) {
            vaalueList.add((int) Float.parseFloat(value));
        }
        return vaalueList;
    }

    public class Measurement {
        long timestamp;
        ArrayList<Integer> leftSensorValues;
        ArrayList<Integer> rightSensorValues;

        public Measurement(long timestamp, ArrayList<Integer> leftSensorValues, ArrayList<Integer> rightSensorValues) {
            this.timestamp = timestamp;
            this.leftSensorValues = leftSensorValues;
            this.rightSensorValues = rightSensorValues;
        }
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
