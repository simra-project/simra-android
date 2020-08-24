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
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.services.BLE.BLEDevice;
import de.tuberlin.mcc.simra.app.services.BLE.BLEScanner;
import de.tuberlin.mcc.simra.app.services.BLE.BLEServiceManager;
import de.tuberlin.mcc.simra.app.util.ForegroundServiceNotificationManager;

import static de.tuberlin.mcc.simra.app.services.BLE.BLEServiceManager.BLEService;

public class RadmesserService extends Service {
    private static final String TAG = "RadmesserService";
    private static final String sharedPrefsKey = "RadmesserServiceBLE";
    private static final String sharedPrefsKeyRadmesserID = "connectedDevice";
    private BLEDevice connectedDevice;
    private volatile HandlerThread mHandlerThread;
    private BLEScanner bluetoothScanner;
    private LocalBroadcastManager broadcastManager;
    private static boolean isServiceActive;

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
        if (newState == ConnectionState.CONNECTED)
            setPairedRadmesserID(connectedDevice.getID(), this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        isServiceActive = true;
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

    private BLEDevice.ConnectionStateCallbacks radmesserConnectionCallbacks = (BLEDevice.ConnectionStatus newState, BLEDevice instance) -> {
        if (instance != connectedDevice) return;    // only interested in currently connected device

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
            new BLEService(BLEDevice.UUID_SERVICE_HEARTRATE).addCharacteristic(
                    BLEDevice.UUID_SERVICE_HEARTRATE_CHAR,
                    val -> broadcastHeartRate(String.valueOf(val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1)))
            ),

            new BLEService(BLEDevice.UUID_SERVICE_CLOSEPASS).addCharacteristic(
                    BLEDevice.UUID_SERVICE_CLOSEPASS_CHAR_DISTANCE,
                    val -> broadcastClosePassDistance(val.getStringValue(0))
            ).addCharacteristic(
                    BLEDevice.UUID_SERVICE_CLOSEPASS_CHAR_EVENT,
                    val -> broadcastClosePassEvent(val.getStringValue(0))
            ),

            new BLEService(BLEDevice.UUID_SERVICE_DISTANCE).addCharacteristic(
                    BLEDevice.UUID_SERVICE_DISTANCE_CHAR_50MS,
                    val -> broadcastDistanceValue(val.getStringValue(0))
            ),

            //legacy Service from Radmesser
            new BLEService(BLEDevice.UUID_SERVICE_CONNECTION).addCharacteristic(
                    BLEDevice.UUID_SERVICE_CONNECTION_CHAR_CONNECTED,
                    val -> {
                        //Log.i(TAG, "new CONNECTION Value:" + val.getStringValue(0));
                        String strVal = val.getStringValue(0);
                        if (strVal != null && strVal.equals("1")) {
                            connectedDevice.devicePaired = true;
                        }
                    }
            )
    );


    // ## incoming communication
    // Action-Requests
    final static String ACTION_PREFIX = "de.tuberlin.mcc.simra.app.radmesserservice.";
    final static String ACTION_START_SCANN = ACTION_PREFIX + ".ACTION_START_SCANN";
    final static String ACTION_CONNECT_DEVICE = ACTION_PREFIX + ".ACTION_CONNECT_DEVICE";
    final static String ACTION_DISCONNECT_AND_UNPAIR = ACTION_PREFIX + ".ACTION_DISCONNECT_AND_UNPAIR";
    final static String ACTION_STOP_SERVICE = ACTION_PREFIX + ".ACTION_STOP_SERVICE";
    final static String EXTRA_CONNECT_DEVICE = ACTION_PREFIX + ".EXTRA_CONNECT_DEVICE";

    // incoming Action-Requests

    /*
     * Request Service to scan for Radmesser devices
     * Scan results can be subscribed with registerCallbacks()
     * */
    public static void startScanning(Context ctx) {
        Intent intent = new Intent(ctx, RadmesserService.class);
        intent.setAction(ACTION_START_SCANN);
        ctx.startService(intent);
    }

    private static String lastConnectionRequest;

    /*
     * Request Service to connect to a specific device by providing its Hardware Adress
     *
     * returns false if already connecting to this device
     * */
    public static boolean connectDevice(Context ctx, String deviceId) {
        if (deviceId.equals(lastConnectionRequest) &&
                connectionState != ConnectionState.CONNECTION_REFUSED &&
                connectionState != ConnectionState.DISCONNECTED)
            return false;

        lastConnectionRequest = deviceId;

        Intent intent = new Intent(ctx, RadmesserService.class);
        intent.setAction(ACTION_CONNECT_DEVICE);
        intent.putExtra(EXTRA_CONNECT_DEVICE, deviceId);
        ctx.startService(intent);
        return true;
    }

    /*
     * Request Service to connect to a the last paired device
     *
     * returns false if there is no device paired yet
     */
    public static boolean tryConnectPairedDevice(Context ctx) {
        String deviceId = getPairedRadmesserID(ctx);
        if (deviceId == null)
            return false;

        Intent intent = new Intent(ctx, RadmesserService.class);
        intent.setAction(ACTION_CONNECT_DEVICE);
        intent.putExtra(EXTRA_CONNECT_DEVICE, deviceId);
        ctx.startService(intent);
        return true;
    }
    /*
     * Request Service to connect to a the last paired device
     *
     * returns false if there is no device paired yet
     */

    public static void disconnectAndUnpairDevice(Context ctx) {
        Intent intent = new Intent(ctx, RadmesserService.class);
        intent.setAction(ACTION_DISCONNECT_AND_UNPAIR);
        ctx.startService(intent);
    }

    /*
     * Request Service to terminate
     *
     * */
    public static void terminateService(Context ctx) {
        if (!isServiceActive) return;
        isServiceActive = false;
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
        if (connectedDevice != null && connectedDevice.getID().equals(deviceId) && connectionState == ConnectionState.CONNECTED)
            return;

        disconnectAndUnpairDevice();
        bluetoothScanner.findDeviceById(deviceId,
                device -> connectedDevice = new BLEDevice(device, radmesserConnectionCallbacks, radmesserServicesDefinition, this)
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
    final static String ACTION_VALUE_RECEIVED_CLOSEPASS_DISTANCE = "de.tuberlin.mcc.simra.app.radmesserservice.actiondvaluereceivedclosepass.distance";
    final static String ACTION_VALUE_RECEIVED_HEARTRATE = "de.tuberlin.mcc.simra.app.radmesserservice.actiondvaluereceivedheartrate";
    final static String ACTION_VALUE_RECEIVED_CLOSEPASS_EVENT = "de.tuberlin.mcc.simra.app.radmesserservice.actiondvaluereceivedclosepass.event";
    final static String ACTION_VALUE_RECEIVED_DISTANCE = "de.tuberlin.mcc.simra.app.radmesserservice.actiondvaluereceiveddistance";

    final static String EXTRA_DEVICE_ID = "de.tuberlin.mcc.simra.app.radmesserservice.extraid";
    final static String EXTRA_DEVICE_NAME = "de.tuberlin.mcc.simra.app.radmesserservice.extraname";
    final static String EXTRA_CONNECTION_STATE = "de.tuberlin.mcc.simra.app.radmesserservice.extraconnectionstate";
    final static String EXTRA_VALUE = "de.tuberlin.mcc.simra.app.radmesserservice.extravalue";
    final static String EXTRA_VALUE_SERIALIZED = "de.tuberlin.mcc.simra.app.radmesserservice.extravalueserialized";


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

    private void broadcastClosePassDistance(String value) {
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_CLOSEPASS_DISTANCE);
        intent.putExtra(EXTRA_VALUE, value);
        intent.putExtra(EXTRA_VALUE_SERIALIZED, Measurement.fromString(value));
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastHeartRate(String value) {
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_HEARTRATE);
        intent.putExtra(EXTRA_VALUE, value);
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastClosePassEvent(String value) {
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_CLOSEPASS_EVENT);
        intent.putExtra(EXTRA_VALUE, value);
        intent.putExtra(EXTRA_VALUE_SERIALIZED, new ClosePassEvent(value));
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastDistanceValue(String value) {
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_DISTANCE);
        intent.putExtra(EXTRA_VALUE, value);
        intent.putExtra(EXTRA_VALUE_SERIALIZED, Measurement.fromString(value));
        broadcastManager.sendBroadcast(intent);
    }

    public abstract static class RadmesserServiceCallbacks {
        public void onDeviceFound(String deviceName, String deviceId) {
        }

        public void onConnectionStateChanged(ConnectionState newState) {
        }

        public void onClosePassIncidentDistance(@Nullable Measurement measurement) {
        }

        public void onClosePassIncidentEvent(@Nullable ClosePassEvent measurement) {
        }

        public void onDistanceValue(@Nullable Measurement measurement) {
        }

        public void onHeartRate(Short value) {
        }
    }

    /*
     * the caller ist responible for unregistering thr receiver, when he does not need him anymore
     */

    public static BroadcastReceiver registerCallbacks(Context ctx, RadmesserServiceCallbacks callbacks) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DEVICE_FOUND);
        filter.addAction(ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(ACTION_VALUE_RECEIVED_DISTANCE);
        filter.addAction(ACTION_VALUE_RECEIVED_CLOSEPASS_DISTANCE);
        filter.addAction(ACTION_VALUE_RECEIVED_CLOSEPASS_EVENT);

        BroadcastReceiver rec = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Serializable serializable;
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
                    case ACTION_VALUE_RECEIVED_CLOSEPASS_DISTANCE:
                        serializable = intent.getSerializableExtra(EXTRA_VALUE_SERIALIZED);

                        if (serializable instanceof Measurement) {
                            callbacks.onClosePassIncidentDistance((Measurement) serializable);
                        }
                        break;
                    case ACTION_VALUE_RECEIVED_DISTANCE:
                        serializable = intent.getSerializableExtra(EXTRA_VALUE_SERIALIZED);

                        if (serializable instanceof Measurement) {
                            callbacks.onDistanceValue((Measurement) serializable);
                        }
                        break;
                    case ACTION_VALUE_RECEIVED_CLOSEPASS_EVENT:
                        serializable = intent.getSerializableExtra(EXTRA_VALUE_SERIALIZED);

                        if (serializable instanceof ClosePassEvent) {
                            callbacks.onClosePassIncidentEvent((ClosePassEvent) serializable);
                        }
                        break;
                    case ACTION_VALUE_RECEIVED_HEARTRATE:
                        callbacks.onHeartRate(
                                parseShort(intent.getStringExtra(EXTRA_VALUE))
                        );
                        break;
                }
            }
        };
        LocalBroadcastManager.getInstance(ctx).registerReceiver(rec, filter);
        return rec;
    }

    private static Short parseShort(String value) {
        try {
            return new Short(value);
        } catch (NumberFormatException nex) {
            return null;
        }

    }

    public static class Measurement implements Serializable {
        public long timestamp;
        public List<Integer> leftSensorValues;
        public List<Integer> rightSensorValues;

        private Measurement(String line) throws MeasurementFormatException {
            try {
                String[] sections = line.split(";", -1);

                timestamp = Long.parseLong(sections[0]);
                leftSensorValues = parseValues(sections[1].split(","));
                rightSensorValues = parseValues(sections[2].split(","));
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException | NullPointerException nex) {
                throw new MeasurementFormatException();
            }
        }

        @Override
        public String toString() {
            return timestamp + " " + leftSensorValues + " " + rightSensorValues;
        }

        private static List<Integer> parseValues(String[] values) {
            List<Integer> valueList = new ArrayList<>();

            for (String value : values) {
                if (value.equals("")) continue;
                valueList.add((int) Float.parseFloat(value));
            }
            return valueList;
        }

        public static Measurement fromString(String line) {
            try {
                return new Measurement(line);
            } catch (MeasurementFormatException e) {
                return null;
            }
        }

        private class MeasurementFormatException extends Exception {
        }
    }

    public static class ClosePassEvent implements Serializable {
        private static final String EVENT_TYPE_BUTTON = "button";
        private static final String EVENT_TYPE_AVG2S = "avg2s";
        private static final String EVENT_TYPE_MIN_KALMAN = "min_kalman";

        private String originalValue;
        private String eventType;
        private long timestamp;
        public List<String> payload;

        public ClosePassEvent(String rawData) {
            originalValue = rawData;
            Log.i("ClosePassEvent", rawData);

            String[] sections = rawData.split(";", -1);
            timestamp = Long.parseLong(sections[0]);
            eventType = sections[1];
            payload = Arrays.asList(sections[2].split(",", -1));
        }

        @Override
        public String toString() {
            return timestamp + " " + eventType + " " + TextUtils.join(", ", payload);
        }

        /**
         * Only button events should be treated as a real close pass event. All
         * other events therefore will be assigned an "unknown" incident type
         * (prefixed with 'OBS_') to be hidden from the regular incident view
         * after a ride ends.
         */
        public int getIncidentType() {
            if (eventType.equals(EVENT_TYPE_BUTTON))
                return IncidentLogEntry.INCIDENT_TYPE.CLOSE_PASS;
            if (eventType.equals(EVENT_TYPE_AVG2S)) return IncidentLogEntry.INCIDENT_TYPE.OBS_AVG2S;
            if (eventType.equals(EVENT_TYPE_MIN_KALMAN))
                return IncidentLogEntry.INCIDENT_TYPE.OBS_MIN_KALMAN;
            return IncidentLogEntry.INCIDENT_TYPE.OBS_UNKNOWN;
        }

        /**
         * Creates an incident description that will be viewable by users and should therefore be human-readable.
         * This description always consists of a general description line, the payload data in a readable format and the
         * raw event string that was passed over bluetooth (formatted in square brackets). These sections are always
         * separated by a newline.
         */
        public String getIncidentDescription(Context context) {
            String headerLine = context.getString(R.string.radmesserIncidentDescriptionHeaderLine, eventType);
            String dataLine = context.getString(R.string.radmesserIncidentDescriptionDataLine, TextUtils.join(", ", payload));

            switch (eventType) {
                case EVENT_TYPE_BUTTON:
                    headerLine = context.getString(R.string.radmesserIncidentDescriptionButtonHeaderLine);
                    if (payload.size() >= 1) {
                        dataLine = context.getString(R.string.radmesserIncidentDescriptionButtonDataLine, payload.get(0));
                    }
                    break;
                case EVENT_TYPE_AVG2S:
                    headerLine = context.getString(R.string.radmesserIncidentDescriptionAvg2sHeaderLine);
                    if (payload.size() >= 2) {
                        dataLine = context.getString(R.string.radmesserIncidentDescriptionAvg2sDataLine, payload.get(0), payload.get(1));
                    }
                    break;
                case EVENT_TYPE_MIN_KALMAN:
                    headerLine = context.getString(R.string.radmesserIncidentDescriptionMinKalmanHeaderLine);
                    if (payload.size() >= 1) {
                        dataLine = context.getString(R.string.radmesserIncidentDescriptionMinKalmanDataLine, payload.get(0));
                    }
                    break;
                default:
                    break;
            }

            return String.format("%s\n%s\n[%s]", headerLine, dataLine, originalValue);
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