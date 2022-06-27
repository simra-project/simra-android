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
        boradcastConnectionStateChanged(newState);
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
        Log.d(TAG,"OBS service started");
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
                    BLEDevice.UUID_SERVICE_OBS_CHAR_DISTANCE ,
                    val -> broadcastDistanceValue(val.getValue())
            ).addCharacteristic(
                    BLEDevice.UUID_SERVICE_OBS_CHAR_CLOSEPASS ,
                    val -> broadcastClosePassEvent(val.getValue())
            ),
            // Additional OBS services, not currently supported
            /*.addCharacteristic(
                    BLEDevice.UUID_SERVICE_OBS_CHAR_TIME,
                    val -> broadcastTime(val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,0))
            ).addCharacteristic(
                    BLEDevice.UUID_SERVICE_OBS_CHAR_OFFSET ,
                    val -> broadcastOffset(val.getValue())
            ).addCharacteristic(
                    BLEDevice.UUID_SERVICE_OBS_CHAR_TRACK,
                    val -> broadcastTrack(val.getStringValue(0))
            )
            */


            new BLEService(BLEDevice.UUID_SERVICE_HEARTRATE).addCharacteristic(
                    BLEDevice.UUID_SERVICE_HEARTRATE_CHAR,
                    val -> broadcastHeartRate(String.valueOf(val.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1)))
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
    // Convert Byte Array into String
    private String ByteToString(byte[] data){
        // If Byte Array received is from Distance Characteristic
       if(data.length>=8) {
           long timeStamp;
           int leftSensor;
           int rightSensor;

           timeStamp = (data[3] & 0xFF << 24) | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
           leftSensor = (data[5] & 0xFF) << 8 | (data[4] & 0xFF);
           rightSensor = (data[7] & 0xFF) << 8 | (data[6] & 0xFF);

           return timeStamp + ";" + leftSensor + ";" + rightSensor;
       }
       // If Byte Array received is from Offset Characteristic
       else{
           int leftOffset;
           int rightOffset;
           leftOffset = (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
           rightOffset = (data[3] & 0xFF) << 8 | (data[2] & 0xFF);

           return leftOffset+";" + rightOffset;
       }
    }


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
                device -> boradcastDeviceFound(device.getName(), device.getAddress())
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
        unPairedOBS();
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
    final static String ACTION_VALUE_RECEIVED_HEARTRATE = "de.tuberlin.mcc.simra.app.obsservice.actiondvaluereceivedheartrate";
    final static String ACTION_VALUE_RECEIVED_CLOSEPASS_EVENT = "de.tuberlin.mcc.simra.app.obsservice.actiondvaluereceivedclosepass.event";
    final static String ACTION_VALUE_RECEIVED_DISTANCE = "de.tuberlin.mcc.simra.app.obsservice.actiondvaluereceiveddistance";
    final static String ACTION_VALUE_RECEIVED_OFFSET = "de.tuberlin.mcc.simra.app.obsservice.actiondvaluereceivedoffset";
    final static String ACTION_VALUE_RECEIVED_TIME = "de.tuberlin.mcc.simra.app.obsservice.actiondvaluereceivedtime";
    final static String ACTION_VALUE_RECEIVED_TRACKID = "de.tuberlin.mcc.simra.app.obsservice.actiondvaluereceivedtrack";

    final static String EXTRA_DEVICE_ID = "de.tuberlin.mcc.simra.app.obsservice.extraid";
    final static String EXTRA_DEVICE_NAME = "de.tuberlin.mcc.simra.app.obsservice.extraname";
    final static String EXTRA_CONNECTION_STATE = "de.tuberlin.mcc.simra.app.obsservice.extraconnectionstate";
    final static String EXTRA_VALUE = "de.tuberlin.mcc.simra.app.obsservice.extravalue";
    final static String EXTRA_VALUE_SERIALIZED = "de.tuberlin.mcc.simra.app.obsservice.extravalueserialized";


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

    private void broadcastHeartRate(String value) {
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_HEARTRATE);
        intent.putExtra(EXTRA_VALUE, value);
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastClosePassEvent(byte[] value) {
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_CLOSEPASS_EVENT);
        String line = ByteToString(value);
        intent.putExtra(EXTRA_VALUE, value);
        intent.putExtra(EXTRA_VALUE_SERIALIZED, new ClosePassEvent(line));
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastDistanceValue(byte[] value) {
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_DISTANCE);
        String line = ByteToString(value);
        intent.putExtra(EXTRA_VALUE, value);
        intent.putExtra(EXTRA_VALUE_SERIALIZED, Measurement.fromString(line));
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastOffset(byte[] value){
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_OFFSET);
        String line = ByteToString(value);
        intent.putExtra(EXTRA_VALUE, line);
        broadcastManager.sendBroadcast(intent);
    }

    private void broadcastTime(int value){
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_TIME);
        intent.putExtra(EXTRA_VALUE, value);
        broadcastManager.sendBroadcast(intent);
    }
    private void broadcastTrack(String value){
        Intent intent = new Intent(ACTION_VALUE_RECEIVED_TRACKID);
        intent.putExtra(EXTRA_VALUE, value);
        broadcastManager.sendBroadcast(intent);
    }






    public abstract static class OBSServiceCallbacks {
        public void onDeviceFound(String deviceName, String deviceId) {
        }

        public void onConnectionStateChanged(ConnectionState newState) {
        }

        public void onClosePassIncidentEvent(@Nullable ClosePassEvent measurement) {
        }

        public void onDistanceValue(@Nullable Measurement measurement) {
        }

        public void onHeartRate(Short value) {
        }

        public void onOffset(String value){
        }

        public void onTime(int value){
        }

        public void onTrack(String value){
        }
    }



    /*
     * the caller ist responible for unregistering thr receiver, when he does not need him anymore
     */

    public static BroadcastReceiver registerCallbacks(Context ctx, OBSServiceCallbacks callbacks) {

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DEVICE_FOUND);
        filter.addAction(ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(ACTION_VALUE_RECEIVED_DISTANCE);
        filter.addAction(ACTION_VALUE_RECEIVED_CLOSEPASS_EVENT);
        filter.addAction(ACTION_VALUE_RECEIVED_OFFSET);
        filter.addAction(ACTION_VALUE_RECEIVED_TIME);
        filter.addAction(ACTION_VALUE_RECEIVED_TRACKID);

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
                    case ACTION_VALUE_RECEIVED_OFFSET:
                        callbacks.onOffset(
                                intent.getStringExtra(EXTRA_VALUE));
                        break;

                    case ACTION_VALUE_RECEIVED_TIME:
                        callbacks.onTime(
                                intent.getIntExtra(EXTRA_VALUE,0));
                        break;
                    case ACTION_VALUE_RECEIVED_TRACKID:
                        callbacks.onTrack(
                                intent.getStringExtra(EXTRA_VALUE));
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
        public List<String> leftSensor;
        public List<String> rightSensor;

        public ClosePassEvent(String rawData) {
            originalValue = rawData;
            Log.i("ClosePassEvent", rawData);

            String[] sections = rawData.split(";", -1);
            timestamp = Long.parseLong(sections[0]);
            eventType = EVENT_TYPE_BUTTON;
            leftSensor = Arrays.asList(sections[1].split(",", -1));
            rightSensor = Arrays.asList(sections[2].split(",", -1));
        }

        @Override
        public String toString() {
            return timestamp + " " + eventType + " " + TextUtils.join(", ", leftSensor);
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
         * This description always consists of a general description line, the leftSensor data in a readable format and the
         * raw event string that was passed over bluetooth (formatted in square brackets). These sections are always
         * separated by a newline.
         */
        public String getIncidentDescription(Context context) {
            String headerLine = context.getString(R.string.obsIncidentDescriptionHeaderLine, eventType);
            String dataLine = context.getString(R.string.obsIncidentDescriptionDataLine, TextUtils.join(", ", leftSensor));

            switch (eventType) {
                case EVENT_TYPE_BUTTON:
                    headerLine = context.getString(R.string.obsIncidentDescriptionButtonHeaderLine);
                    if (leftSensor.size() >= 1) {
                        dataLine = context.getString(R.string.obsIncidentDescriptionButtonDataLine, leftSensor.get(0));
                    }
                    break;
                case EVENT_TYPE_AVG2S:
                    headerLine = context.getString(R.string.obsIncidentDescriptionAvg2sHeaderLine);
                    if (leftSensor.size() >= 2) {
                        dataLine = context.getString(R.string.obsIncidentDescriptionAvg2sDataLine, leftSensor.get(0), leftSensor.get(1));
                    }
                    break;
                case EVENT_TYPE_MIN_KALMAN:
                    headerLine = context.getString(R.string.obsIncidentDescriptionMinKalmanHeaderLine);
                    if (leftSensor.size() >= 1) {
                        dataLine = context.getString(R.string.obsIncidentDescriptionMinKalmanDataLine, leftSensor.get(0));
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

    public void unPairedOBS() {
        getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).edit().remove(sharedPrefsKeyOBSID).apply();
    }

    @Nullable
    private static String getPairedOBSID(Context ctx) {
        return ctx.getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).getString(sharedPrefsKeyOBSID, null);
    }

    private static void setPairedOBSID(String id, Context ctx) {
        ctx.getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE).edit().putString(sharedPrefsKeyOBSID, id).apply();
    }
}