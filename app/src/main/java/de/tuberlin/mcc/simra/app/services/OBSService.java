package de.tuberlin.mcc.simra.app.services;

import android.app.Application;
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
import java.util.List;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.activities.OpenBikeSensorActivity;
import de.tuberlin.mcc.simra.app.services.BLE.BLEDevice;
import de.tuberlin.mcc.simra.app.services.BLE.BLEScanner;
import de.tuberlin.mcc.simra.app.services.BLE.BLEServiceManager;
import de.tuberlin.mcc.simra.app.util.ForegroundServiceNotificationManager;
import de.tuberlin.mcc.simra.app.util.SharedPref;

import static de.tuberlin.mcc.simra.app.services.BLE.BLEServiceManager.BLEService;

public class OBSService extends Service {
    private static final String TAG = "OBSService_LOG";
    private static final String sharedPrefsKey = "OBSServiceBLE";
    private static final String sharedPrefsKeyOBSID = "connectedDevice";
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
        broadcastConnectionStateChanged(newState);
        ForegroundServiceNotificationManager.createOrUpdateNotification(
                this,
                "SimRa OpenBikeSensor connection",
                newState.toString()
        );
        if (newState == ConnectionState.CONNECTED)
            setPairedOBSID(connectedDevice.getID(), this);
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
                        "SimRa OpenBikeSensor connection",
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
                obsConnectionCallbacks.onConnectionStateChange(connectedDevice.getConnectionState(), connectedDevice);
        }

        @Override
        public void onScanAborted() {
            if (connectionState == ConnectionState.SEARCHING && connectedDevice == null)
                setConnectionState(ConnectionState.DISCONNECTED);
            else if (connectedDevice != null) //restore connection state of the current device, after scan
                obsConnectionCallbacks.onConnectionStateChange(connectedDevice.getConnectionState(), connectedDevice);
        }
    };

    private BLEDevice.ConnectionStateCallbacks obsConnectionCallbacks = (BLEDevice.ConnectionStatus newState, BLEDevice instance) -> {
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


    private BLEServiceManager obsServicesDefinition = new BLEServiceManager(
            new BLEService(BLEDevice.UUID_SERVICE_OBS).addCharacteristic(
                    BLEDevice.UUID_SERVICE_OBS_CHAR_SENSOR_DISTANCE,
                    val -> broadcastSensorDistance(val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0) + ";" + val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 4) + ";" + val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 6))
            ).addCharacteristic(
                    BLEDevice.UUID_SERVICE_OBS_CHAR_ClOSE_PASS,
                    val -> broadcastClosePass(val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0) + ";" + val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 4) + ";" + val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 6))
            ).addCharacteristic(
                    BLEDevice.UUID_SERVICE_OBS_CHAR_TIME,
                    val -> broadcastTime(val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,0))
            )/*,
            new BLEService(BLEDevice.UUID_SERVICE_OBS).addCharacteristic(
                    BLEDevice.UUID_SERVICE_OBS_CHAR_ClOSE_PASS_EVENT,
                    val -> broadcastClosePassEvent(val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0) + ";" + val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 4) + ";" + val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 6))
            )*/
    );


    // ## incoming communication
    // Action-Requests
    final static String ACTION_PREFIX = "de.tuberlin.mcc.simra.app.obsservice.";
    final static String ACTION_START_SCANN = ACTION_PREFIX + ".ACTION_START_SCANN";
    final static String ACTION_CONNECT_DEVICE = ACTION_PREFIX + ".ACTION_CONNECT_DEVICE";
    final static String ACTION_DISCONNECT_AND_UNPAIR = ACTION_PREFIX + ".ACTION_DISCONNECT_AND_UNPAIR";
    final static String ACTION_STOP_SERVICE = ACTION_PREFIX + ".ACTION_STOP_SERVICE";
    final static String EXTRA_CONNECT_DEVICE = ACTION_PREFIX + ".EXTRA_CONNECT_DEVICE";

    // incoming Action-Requests

    /*
     * Request Service to scan for OpenBikeSensor devices
     * Scan results can be subscribed with registerCallbacks()
     * */
    public static void startScanning(Context ctx) {
        Intent intent = new Intent(ctx, OBSService.class);
        intent.setAction(ACTION_START_SCANN);
        ctx.startService(intent);
    }

    private static String lastConnectionRequest;

    /*
     * Request Service to connect to a specific device by providing its Hardware Address
     *
     * returns false if already connecting to this device
     * */
    public static boolean connectDevice(Context ctx, String deviceId) {
        if (deviceId.equals(lastConnectionRequest) &&
                connectionState != ConnectionState.CONNECTION_REFUSED &&
                connectionState != ConnectionState.DISCONNECTED)
            return false;

        lastConnectionRequest = deviceId;

        Intent intent = new Intent(ctx, OBSService.class);
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
        String deviceId = getPairedOBSID(ctx);
        if (deviceId == null)
            return false;

        Intent intent = new Intent(ctx, OBSService.class);
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
        Intent intent = new Intent(ctx, OBSService.class);
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
        Intent intent = new Intent(ctx, OBSService.class);
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
        bluetoothScanner.findDevicesByServices(obsServicesDefinition,
                device -> broadcastDeviceFound(device.getName(), device.getAddress())
        );
    }

    private void connectDevice(String deviceId) {
        if (connectedDevice != null && connectedDevice.getID().equals(deviceId) && connectionState == ConnectionState.CONNECTED)
            return;

        disconnectAndUnpairDevice();
        bluetoothScanner.findDeviceById(deviceId,
                device -> connectedDevice = new BLEDevice(device, obsConnectionCallbacks, obsServicesDefinition, this)
        );
    }

    private void disconnectAndUnpairDevice() {
        disconnectAnyOBS();
        unPairedOBS(this);
    }

    private void disconnectAnyOBS() {
        setConnectionState(ConnectionState.DISCONNECTED);
        if (connectedDevice != null)
            connectedDevice.disconnectDevice();
        connectedDevice = null;
    }

    private void terminateService() {
        disconnectAnyOBS();
        stopSelf();
    }
    // ## outgoing communication

    // Broadcasts
    final static String ACTION_DEVICE_FOUND = "de.tuberlin.mcc.simra.app.obsservice.actiondevicefound";
    final static String ACTION_CONNECTION_STATE_CHANGED = "de.tuberlin.mcc.simra.app.obsservice.actiondconnectionstatechanged";
    final static String ACTION_VALUE_RECEIVED_SENSOR_DISTANCE = "de.tuberlin.mcc.simra.app.obsservice.actiondvaluereceivedsensordistance";
    final static String ACTION_VALUE_RECEIVED_CLOSEPASS = "de.tuberlin.mcc.simra.app.obsservice.actiondvaluereceivedclosepass";
    final static String ACTION_VALUE_READ_TIME = "de.tuberlin.mcc.simra.app.obsservice.actiondvaluereadtime";
    final static String EXTRA_DEVICE_ID = "de.tuberlin.mcc.simra.app.obsservice.extraid";
    final static String EXTRA_DEVICE_NAME = "de.tuberlin.mcc.simra.app.obsservice.extraname";
    final static String EXTRA_CONNECTION_STATE = "de.tuberlin.mcc.simra.app.obsservice.extraconnectionstate";
    final static String EXTRA_VALUE = "de.tuberlin.mcc.simra.app.obsservice.extravalue";
    final static String EXTRA_VALUE_SERIALIZED = "de.tuberlin.mcc.simra.app.obsservice.extravalueserialized";


    private void broadcastDeviceFound(String deviceName, String deviceId) {
        Log.d(TAG, "broadcastDeviceFound: " + deviceName + " " + deviceId);
        Intent intent = new Intent(ACTION_DEVICE_FOUND);
        intent.putExtra(EXTRA_DEVICE_ID, deviceId);
        intent.putExtra(EXTRA_DEVICE_NAME, deviceName);
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastConnectionStateChanged(ConnectionState newState) {
        Intent intent = new Intent(ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(EXTRA_CONNECTION_STATE, newState.toString());
        broadcastManager.sendBroadcast(intent);
    }


    private void broadcastSensorDistance(String value) {
        // Log.d(TAG, "broadcastSensorDistance: " + Measurement.fromString(value));
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_SENSOR_DISTANCE);
        intent.putExtra(EXTRA_VALUE_SERIALIZED, Measurement.fromString(value));
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastClosePass(String value) {
        Log.d(TAG, "broadcastClosePass: " + Measurement.fromString(value));
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_CLOSEPASS);
        // intent.putExtra(EXTRA_VALUE, value);
        intent.putExtra(EXTRA_VALUE_SERIALIZED, Measurement.fromString(value));
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastTime(int value) {
        Log.d(TAG, "broadcastTime: " + value);
        Intent intent = new Intent(ACTION_VALUE_READ_TIME);
        intent.putExtra(EXTRA_VALUE, value);
        broadcastManager.sendBroadcast(intent);
    }

    public abstract static class OBSServiceCallbacks {
        public void onDeviceFound(String deviceName, String deviceId) {
        }

        public void onConnectionStateChanged(ConnectionState newState) {
        }

        public void onClosePass(@Nullable Measurement measurement) {
        }

        /*public void onClosePassIncidentEvent(@Nullable ClosePassEvent measurement) {
        }*/

        /*public void onDistanceValue(@Nullable Measurement measurement) {
        }*/

        public void onSensorDistance(@Nullable Measurement measurement) {
        }

        public void onTime(int time, Context context) {
            Log.d(TAG, "onTime: " + time);
            long oldStartTime = SharedPref.App.OpenBikeSensor.getObsStartTime(context);
            if (oldStartTime == 0L) {
                long newStartTime = System.currentTimeMillis() - time;
                SharedPref.App.OpenBikeSensor.setObsStartTime(newStartTime, context);
            }
        }
    }

    /*
     * the caller ist responible for unregistering thr receiver, when he does not need him anymore
     */

    public static BroadcastReceiver registerCallbacks(Context ctx, OBSServiceCallbacks callbacks) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DEVICE_FOUND);
        filter.addAction(ACTION_CONNECTION_STATE_CHANGED);
        /*filter.addAction(ACTION_VALUE_RECEIVED_DISTANCE);
        filter.addAction(ACTION_VALUE_RECEIVED_CLOSEPASS_DISTANCE);*/
        filter.addAction(ACTION_VALUE_RECEIVED_SENSOR_DISTANCE);
        filter.addAction(ACTION_VALUE_RECEIVED_CLOSEPASS);
        filter.addAction(ACTION_VALUE_READ_TIME);

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
                   /* case ACTION_VALUE_RECEIVED_CLOSEPASS_DISTANCE:
                        serializable = intent.getSerializableExtra(EXTRA_VALUE_SERIALIZED);

                        if (serializable instanceof Measurement) {
                            callbacks.onClosePassIncident((Measurement) serializable);
                        }
                        break;*/
                    // case ACTION_VALUE_RECEIVED_DISTANCE:
                    case ACTION_VALUE_RECEIVED_CLOSEPASS:
                        serializable = intent.getSerializableExtra(EXTRA_VALUE_SERIALIZED);

                        if (serializable instanceof Measurement) {
                            callbacks.onClosePass((Measurement) serializable);
                        }
                        break;

                    case ACTION_VALUE_READ_TIME:
                        callbacks.onTime(intent.getIntExtra(EXTRA_VALUE, -1), ctx);


                    case ACTION_VALUE_RECEIVED_SENSOR_DISTANCE:
                        serializable = intent.getSerializableExtra(EXTRA_VALUE_SERIALIZED);
                        if (serializable instanceof Measurement) {
                            callbacks.onSensorDistance((Measurement) serializable);
                        }
                        /*String[] characteristicValues = intent.getStringExtra(EXTRA_VALUE).split(";");
                        callbacks.onSensorDistance(
                                Integer.parseInt(characteristicValues[0]),parseShort(characteristicValues[1]),parseShort(characteristicValues[2])
                        );
*/                        break;
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
        public int leftSensorValue;
        public int rightSensorValue;

        private Measurement(String line) throws MeasurementFormatException {
            try {
                String[] sections = line.split(";", -1);

                timestamp = Long.parseLong(sections[0]);
                leftSensorValue = Integer.parseInt(sections[1]);
                rightSensorValue = Integer.parseInt(sections[2]);
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException | NullPointerException nex) {
                throw new MeasurementFormatException();
            }
        }

        @Override
        public String toString() {
            return timestamp + " " + leftSensorValue + " " + rightSensorValue;
        }
        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        public static Measurement fromString(String line) {
            try {
                return new Measurement(line);
            } catch (MeasurementFormatException e) {
                return null;
            }
        }

        public String getIncidentDescription(Context context) {
            String headerLine = context.getString(R.string.obsIncidentDescriptionButtonHeaderLine);
            String dataLine = context.getString(R.string.obsIncidentDescriptionButtonDataLine, TextUtils.concat("left distance: ", (String.valueOf(leftSensorValue)), "right distance: ", (String.valueOf(rightSensorValue))));
            return String.format("%s\n%s", headerLine, dataLine);
        }

        private class MeasurementFormatException extends Exception {
        }
    }

    /*public static class ClosePassEvent implements Serializable {
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

        *//**
         * Only button events should be treated as a real close pass event. All
         * other events therefore will be assigned an "unknown" incident type
         * (prefixed with 'OBS_') to be hidden from the regular incident view
         * after a ride ends.
         *//*
        public int getIncidentType() {
            if (eventType.equals(EVENT_TYPE_BUTTON))
                return IncidentLogEntry.INCIDENT_TYPE.CLOSE_PASS;
            if (eventType.equals(EVENT_TYPE_AVG2S)) return IncidentLogEntry.INCIDENT_TYPE.OBS_AVG2S;
            if (eventType.equals(EVENT_TYPE_MIN_KALMAN))
                return IncidentLogEntry.INCIDENT_TYPE.OBS_MIN_KALMAN;
            return IncidentLogEntry.INCIDENT_TYPE.OBS_UNKNOWN;
        }

        *//**
         * Creates an incident description that will be viewable by users and should therefore be human-readable.
         * This description always consists of a general description line, the payload data in a readable format and the
         * raw event string that was passed over bluetooth (formatted in square brackets). These sections are always
         * separated by a newline.
         *//*
        public String getIncidentDescription(Context context) {
            String headerLine = context.getString(R.string.obsIncidentDescriptionHeaderLine, eventType);
            String dataLine = context.getString(R.string.obsIncidentDescriptionDataLine, TextUtils.join(", ", payload));

            switch (eventType) {
                case EVENT_TYPE_BUTTON:
                    headerLine = context.getString(R.string.obsIncidentDescriptionButtonHeaderLine);
                    if (payload.size() >= 1) {
                        dataLine = context.getString(R.string.obsIncidentDescriptionButtonDataLine, payload.get(0));
                    }
                    break;
                case EVENT_TYPE_AVG2S:
                    headerLine = context.getString(R.string.obsIncidentDescriptionAvg2sHeaderLine);
                    if (payload.size() >= 2) {
                        dataLine = context.getString(R.string.obsIncidentDescriptionAvg2sDataLine, payload.get(0), payload.get(1));
                    }
                    break;
                case EVENT_TYPE_MIN_KALMAN:
                    headerLine = context.getString(R.string.obsIncidentDescriptionMinKalmanHeaderLine);
                    if (payload.size() >= 1) {
                        dataLine = context.getString(R.string.obsIncidentDescriptionMinKalmanDataLine, payload.get(0));
                    }
                    break;
                default:
                    break;
            }

            return String.format("%s\n%s\n[%s]", headerLine, dataLine, originalValue);
        }
    }*/

    public static void unRegisterCallbacks(BroadcastReceiver receiver, Context ctx) {
        if (receiver != null)
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(receiver);
    }

    public void unPairedOBS(Context ctx) {
        SharedPref.App.OpenBikeSensor.deleteObsDeviceName(ctx);
        // getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).edit().remove(sharedPrefsKeyOBSID).apply();
    }

    @Nullable
    private static String getPairedOBSID(Context ctx) {
        return SharedPref.App.OpenBikeSensor.getObsDeviceName(ctx);
        // return ctx.getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).getString(sharedPrefsKeyOBSID, null);
    }

    private static void setPairedOBSID(String id, Context ctx) {
        SharedPref.App.OpenBikeSensor.setObsDeviceName(id,ctx);
        // ctx.getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).edit().putString(sharedPrefsKeyOBSID, id).apply();
    }
}