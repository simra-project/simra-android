package de.tuberlin.mcc.simra.app.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.TreeMap;

import de.tuberlin.mcc.simra.app.Event;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import de.tuberlin.mcc.simra.app.obslite.OBSLiteSession;
import de.tuberlin.mcc.simra.app.util.ConnectionManager;
import de.tuberlin.mcc.simra.app.util.Constants;
import de.tuberlin.mcc.simra.app.util.ForegroundServiceNotificationManager;
import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.IncidentBroadcaster;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.UnitHelper;
import de.tuberlin.mcc.simra.app.util.ble.ConnectionEventListener;

/*import static de.tuberlin.mcc.simra.app.services.OBSService.ACTION_VALUE_RECEIVED_CLOSEPASS_EVENT;
import static de.tuberlin.mcc.simra.app.services.OBSService.ACTION_VALUE_RECEIVED_DISTANCE;
import static de.tuberlin.mcc.simra.app.services.OBSService.EXTRA_VALUE_SERIALIZED;*/
import static android.app.PendingIntent.getActivity;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.mergeGPSandSensorLines;
import static de.tuberlin.mcc.simra.app.util.Utils.overwriteFile;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class RecorderService extends Service implements SensorEventListener, LocationListener, SerialInputOutputManager.Listener {
    public static final String TAG = "RecorderService_LOG:";
    long startTime = 0;
    long endTime;
    float[] accelerometerMatrix = new float[3];
    float[] gyroscopeMatrix = new float[3];
    float[] linearAccelerometerMatrix = new float[3];
    float[] rotationMatrix = new float[5];
    Location lastLocation;
    Polyline route = new Polyline();
    Handler recordingStarterHandler = new Handler();
    Handler recordingHandler = new Handler();
    long waitedTime = 0;
    long lastHandlerStart = 0;
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Strings for storing data to enable continued use by other activities
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    Queue<Float> accelerometerQueueX = new LinkedList<>();
    Queue<Float> accelerometerQueueY = new LinkedList<>();
    Queue<Float> accelerometerQueueZ = new LinkedList<>();
    Queue<Float> linearAccelerometerQueueX = new LinkedList<>();
    Queue<Float> linearAccelerometerQueueY = new LinkedList<>();
    Queue<Float> linearAccelerometerQueueZ = new LinkedList<>();
    Queue<Float> rotationQueueX = new LinkedList<>();
    Queue<Float> rotationQueueY = new LinkedList<>();
    Queue<Float> rotationQueueZ = new LinkedList<>();
    Queue<Float> rotationQueueC = new LinkedList<>();
    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;
    Location startLocation;
    // OpenBikeSensor
    private ConnectionEventListener connectionEventListener = null;
    private final LinkedList<ConnectionManager.Measurement> obsMeasurements = new LinkedList<>();
    /*private final LinkedList<OBSService.Measurement> lastOBSDistanceValues = new LinkedList<>();
    private final LinkedList<OBSService.ClosePassEvent> lastOBSClosePassEvents = new LinkedList<>();*/
    private LocationManager locationManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor linearAccelerometer;
    private Sensor rotation;
    private int key;
    private Integer incidentDuringRide = null;

    private BroadcastReceiver incidentBroadcastReceiver;
    // This is set to true, when recording is allowed according to Privacy-Duration and
    // Privacy-Distance (see sharedPrefs, set in StartActivity and edited in settings)
    private boolean recordingAllowed;
    private int safetyDistanceWithTolerances;
    private float privacyDistance;
    private long privacyDuration;
    private boolean lineAdded;
    private long lastGPSUpdate = 0;
    private SensorManager sensorManager = null;
    private PowerManager.WakeLock wakeLock = null;
    private final IBinder mBinder = new MyBinder();
    private String accGpsString = "";
    private Queue<DataLogEntry> gpsLines = new LinkedList<>();
    private Queue<DataLogEntry> sensorLines = new LinkedList<>();
    private IncidentLog incidentLog = null;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // OBS-Lite
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private LinkedList<Byte> obsLiteData = new LinkedList<>();
    private byte[] obsLiteDataArray = new byte[0];
    private StringBuffer obsLiteDataSB = new StringBuffer();
    private LooperThread obsLiteLooperThread;
    private Handler obsLiteHandler;
    private HandlerThread obsLiteHandlerThread;
    private OBSLiteSession obsLiteSession;
    private Event obsLiteEvent;

    private UsbManager usbManager;
    private boolean obsLiteEnabled = false;
    private long obsLiteStartTime = 0L;
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "intent: " + intent);
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
                    if (!availableDrivers.isEmpty()) {
                        UsbSerialDriver driver = availableDrivers.get(0);
                        UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
                            try {
                                port.open(connection);
                                port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                                Log.d(TAG, "usb serial port opened");
                                usbIoManager = new SerialInputOutputManager(port);
                                usbIoManager.run();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            Log.d(TAG, "permission denied for device " + device);
                        }
                    }
                }
            }
        }
    };

    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;

    public int getCurrentRideKey() {
        return key;
    }

    public double getDuration() {
        return (System.currentTimeMillis() - startTime);
    }

    public boolean hasRecordedEnough() {
        return recordingAllowed && lineAdded;
    }

    public String getOBSLiteData() {
        Log.d(TAG, "obsLiteDataSB: " + obsLiteDataSB.toString());
        return obsLiteDataSB.toString();
    }
    /*public byte[] getOBSLiteData() {
        return obsLiteDataArray;
    }*/

    public long getObsLiteStartTime() { return obsLiteStartTime; }

    public int getObsLiteSessionCompleteEventsLength() {
        if (obsLiteSession == null) {
            return 0;
        } else {
            return obsLiteSession.getCompleteEvents().length;
        }
    }


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // SensorEventListener Methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            /**/accelerometerMatrix = event.values;/**/
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            /**/gyroscopeMatrix = event.values;/**/
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            /**/linearAccelerometerMatrix = event.values;/**/

        } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            rotationMatrix = event.values;
        }
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // LocationListener Methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onLocationChanged(Location location) {
        // Set start location. Important for privacy distance.
        if (startLocation == null) {
            startLocation = location;
        }
        lastLocation = location;

        // Only add GPS event if OBSLite is enabled and session exists
        if (obsLiteEnabled && obsLiteSession != null) {
            obsLiteSession.addGPSEvent(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Service Methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize sharedPrefs & editor
        sharedPrefs = getApplicationContext().getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);

        editor = sharedPrefs.edit();

        if(SharedPref.Settings.OpenBikeSensor.isEnabled(getBaseContext()) && connectionEventListener == null) {
            connectionEventListener = new ConnectionEventListener();
            connectionEventListener.setOnClosePassNotification(measurement -> {
                long closePassRealTime = (ConnectionManager.INSTANCE.getStartTime() + measurement.getObsTime());
                Log.d(TAG, "Close Pass - Time: " + closePassRealTime + " left: " + measurement.getLeftDistance() + " right: " + measurement.getRightDistance());
                obsMeasurements.add(measurement);
                return null;
            });
            ConnectionManager.INSTANCE.registerListener(connectionEventListener);
        }

        safetyDistanceWithTolerances = SharedPref.Settings.Ride.OvertakeWidth.getWidth(this);

        // Prepare the sensors for accGpsFile
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        // Request location updates
        locationManager = (LocationManager) getSystemService(Context
                .LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager
                .GPS_PROVIDER, 0, 0.0f, this);

        // When the user records a route for the first time, the ride key is 0.
        // For all subsequent rides, the key value increases by one at a time.
        if (!sharedPrefs.contains("RIDE-KEY")) {

            editor.putInt("RIDE-KEY", 0);

            editor.apply();
        }

        // Load the privacy settings
        privacyDistance = SharedPref.Settings.Ride.PrivacyDistance.getDistance(UnitHelper.DISTANCE.METRIC, this);
        privacyDuration = SharedPref.Settings.Ride.PrivacyDuration.getDuration(this) * 1000;
        Log.d(TAG, "privacyDistance: " + privacyDistance + " privacyDuration: " + privacyDuration);

        // Prevent the App to be killed while recording
        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":RecorderService");

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        /*localBroadcastManager.registerReceiver(openBikeSensorMessageReceiverDistanceValue, new IntentFilter(ACTION_VALUE_RECEIVED_DISTANCE));
        localBroadcastManager.registerReceiver(openBikeSensorMessageReceiverClosePassEvent, new IntentFilter(ACTION_VALUE_RECEIVED_CLOSEPASS_EVENT));
        */incidentBroadcastReceiver = IncidentBroadcaster.receiveIncidents(this, new IncidentBroadcaster.IncidentCallbacks() {
            @Override
            public void onManualIncident(int incidentType) {
                incidentDuringRide = incidentType;
            }
        });

        obsLiteEnabled = SharedPref.Settings.OBSLite.isEnabled(this);
        if (obsLiteEnabled) {
            obsLiteLooperThread = new LooperThread();
            obsLiteLooperThread.start();
            obsLiteHandlerThread = new HandlerThread("HandlerThread");
            obsLiteHandlerThread.start();
            obsLiteHandler = new Handler(obsLiteHandlerThread.getLooper());
            obsLiteSession = new OBSLiteSession(this);
        }


    }

    @Override
    public IBinder onBind(Intent intent) {
        if (obsLiteLooperThread != null) {
            obsLiteLooperThread.mHandler.post(this::tryConnectOBSLite);
        }
        Log.d(TAG, "onBind()");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind()");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind()");
        return true;
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        startTime = System.currentTimeMillis();
        Runnable recordingStarter = new Runnable() {
            @Override
            public void run() {

                // Privacy filter: Set recordingAllowed to true, when enough time (privacyDuration) passed
                // since the user pressed Start Recording AND there is enough distance (privacyDistance)
                // between the starting location and the current location.
                if (!recordingAllowed && startLocation != null && lastLocation != null
                        && (startLocation.distanceTo(lastLocation) >= privacyDistance)
                        && (System.currentTimeMillis() - startTime) > privacyDuration) {
                    recordingAllowed = true;
                    /**/
                    recordingStarterHandler.removeCallbacksAndMessages(null);/**/
                    Runnable recorder = new InsertHandler(/*actualLocation,actualAccelerometerMatrix, actualGyroscopeMatrix, actualLinearAccelerometerMatrix, actualRotationMatrix*/);
                    recordingHandler.post(recorder);

                } else {
                    // Repeat this the same runnable code block again another 50 ms
                    // 'this' is referencing the Runnable object
                    recordingStarterHandler.postDelayed(this, 50);
                }
            }
        };
        recordingStarterHandler.post(recordingStarter);
        // When the user records a route for the first time, the ride key is 0.
        // For all subsequent rides, the key value increases by one at a time.

        key = sharedPrefs.getInt("RIDE-KEY", 0);
        incidentLog = new IncidentLog(key, new TreeMap<>(), 0);

        // Fire the notification while recording
        Notification notification =
                ForegroundServiceNotificationManager.createOrUpdateNotification(
                        this,
                        getResources().getString(R.string.foregroundNotificationTitle_record),
                        getResources().getString(R.string.foregroundNotificationBody_record)
                );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ForegroundServiceNotificationManager.getNotificationId(), notification, FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(ForegroundServiceNotificationManager.getNotificationId(), notification);
        }
        wakeLock.acquire(28800000);

        // Register Accelerometer and Gyroscope
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope,
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, linearAccelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, rotation,
                SensorManager.SENSOR_DELAY_FASTEST);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        Log.d(TAG, "obsLiteDataArray.length: " + obsLiteDataArray.length);

        // Unregister from OBS callbacks
        if (connectionEventListener != null) {
            ConnectionManager.INSTANCE.unregisterListener(connectionEventListener);
        }

        // disconnect from OBS Lite
        if (obsLiteEnabled) {
            // obsLiteLooperThread.mHandler.post(this::disconnectOBSLite);
            obsLiteHandler.post(this::disconnectOBSLite);
            obsLiteHandlerThread.quitSafely();
        }

        // Create a file for the ride and write ride into it (AccGpsFile). Also, update metaData.csv
        // with current ride and and sharedPrefs with current ride key. Do these things only,
        // if recording is allowed (see privacyDuration and privacyDistance) and we have written some
        // data.
        Log.d(TAG, "recordingAllowed: " + recordingAllowed + " lineAdded: " + lineAdded);
        if (recordingAllowed && lineAdded) {
            recordingHandler.removeCallbacksAndMessages(null);
            int region = lookUpIntSharedPrefs("Region", 0, "Profile", this);
            addOBSIncidents(obsMeasurements, incidentLog, gpsLines, this);
            accGpsString = mergeGPSandSensorLines(gpsLines,sensorLines);
            overwriteFile((IOUtils.Files.getFileInfoLine() + DataLog.DATA_LOG_HEADER + System.lineSeparator() + accGpsString), IOUtils.Files.getGPSLogFile(key, false, this));
            MetaData.updateOrAddMetaDataEntryForRide(new MetaDataEntry(key, startTime, endTime, MetaData.STATE.JUST_RECORDED, 0, waitedTime, Math.round(route.getDistance()), 0, region), this);
            IncidentLog.saveIncidentLog(incidentLog, this);
            editor.putInt("RIDE-KEY", key + 1);
            editor.apply();

            if (obsLiteEnabled && obsLiteSession.getCompleteEvents().length > 0) {
                // overwriteFile(obsLiteDataSB.toString(), IOUtils.Files.getOBSLiteSessionFile(key,this));
                IOUtils.createBinaryFileOBSLite(obsLiteSession.getCompleteEvents(),IOUtils.Files.getOBSLiteSessionFile(key,this));
            }
        }

        // Unregister receiver and listener prior to gpsExecutor shutdown
        sensorManager.unregisterListener(this);

        // Stop requesting location updates
        locationManager.removeUpdates(this);

        // Stop OpenBikeSensor LocalBroadcast Listener
        /*LocalBroadcastManager.getInstance(this).unregisterReceiver(openBikeSensorMessageReceiverDistanceValue);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(openBikeSensorMessageReceiverClosePassEvent);
*/
        // Stop Manual Incident Broadcast Listener
        LocalBroadcastManager.getInstance(this).unregisterReceiver(incidentBroadcastReceiver);

        // Remove the Notification
        ForegroundServiceNotificationManager.cancelNotification(this);

        /**/stopForeground(true);/**/
        /**/wakeLock.release();/**/
    }

    /**
     * Adds all incidents from obsMeasurements to incidentLog.
     * Matches the obsMeasurements to gpsLines first by taking the last gpsLine, whose timestamp
     * is smaller than the measurements' timestamp.
     * @param obsMeasurements
     * @param incidentLog
     * @param gpsLines
     * @param context
     */
    private void addOBSIncidents(LinkedList<ConnectionManager.Measurement> obsMeasurements, IncidentLog incidentLog, Queue<DataLogEntry> gpsLines, Context context) {
        int gpsLinesIndex = 0;
        DataLogEntry[] dataLogEntries = gpsLines.toArray(new DataLogEntry[0]);
        for (int i = 0; i < this.obsMeasurements.size(); i++) {
            ConnectionManager.Measurement measurement = this.obsMeasurements.get(i);
            for (int j = gpsLinesIndex; j < gpsLines.size(); j++) {
                DataLogEntry thisDataLogEntry = dataLogEntries[j];
                long thisDataLogEntryTS = thisDataLogEntry.timestamp;
                long thisMeasurementTS = measurement.getRealTime();
                long thisDelta = thisDataLogEntryTS - thisMeasurementTS;
                // When thisDelta changes from negative to positive, the gpsLine this measurement belongs to was found.
                if (thisDelta >= 0 && j > 0) {
                    DataLogEntry lastDataLogEntry = dataLogEntries[j-1];
                    long lastDataLogEntryTS = lastDataLogEntry.timestamp;
                    double lastDataLogEntryLat = lastDataLogEntry.latitude;
                    double lastDataLogEntryLon = lastDataLogEntry.longitude;
                    // overtake distance is from side mirror of the car (~13cm) to the handlebar of the bicycle
                    double realLeftDistance = measurement.getLeftDistance() - SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidth(this) - 13;
                    double realRightDistance = measurement.getRightDistance() - SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidth(this) - 13;
                    // if the obs measurement is between -100 an 150cm, it is a close pass and needs to be shown as a near miss incident, else a "regular" pass, just to be shown in the csv and hidden on the map.
                    if ((realLeftDistance >= -100 && realLeftDistance <= 150) || ((realRightDistance >= -100 && realRightDistance <= 150))) {
                        Log.d(TAG, "Adding hidden Close Pass with TS: " + lastDataLogEntryTS + " realLeftDistance: " + realLeftDistance + " and realRightDistance: " + realRightDistance);
                        incidentLog.updateOrAddIncident(IncidentLogEntry.newBuilder().withIncidentType(IncidentLogEntry.INCIDENT_TYPE.OBS_UNKNOWN).withBaseInformation(lastDataLogEntryTS, lastDataLogEntryLat, lastDataLogEntryLon).withDescription(measurement.getIncidentDescription(context)).withKey(3000).build());
                    } else {
                        Log.d(TAG, "Adding visible Close Pass with TS: " + lastDataLogEntryTS + " realLeftDistance: " + realLeftDistance + " and realRightDistance: " + realRightDistance);
                        incidentLog.updateOrAddIncident(IncidentLogEntry.newBuilder().withIncidentType(IncidentLogEntry.INCIDENT_TYPE.CLOSE_PASS).withBaseInformation(lastDataLogEntryTS, lastDataLogEntryLat, lastDataLogEntryLon).withDescription(measurement.getIncidentDescription(context)).withKey(2000).build());
                    }
                    // finish, when at the end of the ride.
                    if (j+1 >= gpsLines.size()) {
                        return;
                    // else, update gpsLinesIndex, so that the next obs measurement is searched in the rest of the ride.
                    } else {
                        gpsLinesIndex = j+1;
                        break;
                    }
                }
            }
        }
    }

    private float computeAverage(Collection<Float> myVals) {
        float sum = 0;
        for (float f : myVals) {
            sum += f;
        }
        return sum / myVals.size();
    }

    // Create the Handler object (on the main thread by default)
    // Define the code block to be executed
    private

    class InsertHandler implements Runnable {

        public InsertHandler() {
        }

        @SuppressLint("MissingPermission")
        public void run() {
            /*
              How is this Working?
              We are collecting GPS, Accelerometer, Gyroscope (and OpenBikeSensor) Data, those are updated as following;
              - GPS (lat, lon, accuracy) roughly every 3 seconds
              - accelerometer data (x,y,z) roughly 50 times a second
              - gyroscope data (a,b,c) roughly every 3 seconds
              <p>
              Every Data Type is given asynchronously via its Callback function.
              In order to synchronize the accelerometer interval ist used as baseline.
              1. We wait till there are 30 values generated
              2. We write a Log Entry every {@link Constants.MVG_AVG_STEP}
                 as this number of values is removed at the end of this function
                 and we wait again till there are 30
             */
            boolean isGPSLine = false;
            long start = System.currentTimeMillis() - startTime;
            /**/
            if (accelerometerQueueX.size() < 30) {
                accelerometerQueueX.add(accelerometerMatrix[0]);
                accelerometerQueueY.add(accelerometerMatrix[1]);
                accelerometerQueueZ.add(accelerometerMatrix[2]);

            }
            if (linearAccelerometerQueueX.size() < 30) {
                linearAccelerometerQueueX.add(linearAccelerometerMatrix[0]);
                linearAccelerometerQueueY.add(linearAccelerometerMatrix[1]);
                linearAccelerometerQueueZ.add(linearAccelerometerMatrix[2]);
            }
            if (rotationQueueX.size() < 30) {
                rotationQueueX.add(rotationMatrix[0]);
                rotationQueueY.add(rotationMatrix[1]);
                rotationQueueZ.add(rotationMatrix[2]);
                rotationQueueC.add(rotationMatrix[3]);
            }
             /**/
            /**/if (accelerometerQueueX.size() >= 30 && linearAccelerometerQueueX.size() >= 30 && rotationQueueX.size() >= 30) {
                DataLogEntry.DataLogEntryBuilder dataLogEntryBuilder = DataLogEntry.newBuilder();
                long lastAccUpdate = System.currentTimeMillis();
                dataLogEntryBuilder.withTimestamp(lastAccUpdate);
                dataLogEntryBuilder.withAccelerometer(
                        // Every average is computed over 30 data points
                        /**/computeAverage(accelerometerQueueX),
                        computeAverage(accelerometerQueueY),
                        computeAverage(accelerometerQueueZ)/**/
                );

                dataLogEntryBuilder.withLinearAccelerometer(
                        // Every average is computed over 30 data points
                        /**/computeAverage(linearAccelerometerQueueX),
                        computeAverage(linearAccelerometerQueueY),
                        computeAverage(linearAccelerometerQueueZ)/**/
                );

                dataLogEntryBuilder.withRotation(
                        // Every average is computed over 30 data points
                        /**/computeAverage(rotationQueueX),
                        computeAverage(rotationQueueY),
                        computeAverage(rotationQueueZ),
                        computeAverage(rotationQueueC)/**/
                );

                if ((lastAccUpdate - lastGPSUpdate) >= Constants.GPS_FREQUENCY) {
                    isGPSLine = true;
                    lastGPSUpdate = lastAccUpdate;
                    Location thisLocation = lastLocation;
                    dataLogEntryBuilder.withTimestamp(thisLocation.getTime());

                    if (thisLocation == null) {
                        thisLocation = locationManager.getLastKnownLocation(LocationManager
                                .GPS_PROVIDER);
                    }
                    if (thisLocation == null) {
                        thisLocation = new Location(LocationManager
                                .GPS_PROVIDER);
                    }

                    route.addPoint(new GeoPoint(thisLocation.getLatitude(), thisLocation.getLongitude()));
                    if (thisLocation.getSpeed() <= 3.0) {
                        waitedTime += 3;
                    }
                    dataLogEntryBuilder.withGPS(
                            thisLocation.getLatitude(),
                            thisLocation.getLongitude(),
                            thisLocation.getAccuracy()
                    );

                    if (incidentDuringRide != null && lastLocation != null) {
                        incidentLog.updateOrAddIncident(IncidentLogEntry.newBuilder().withIncidentType(incidentDuringRide).withBaseInformation(lastAccUpdate, lastLocation.getLatitude(), lastLocation.getLongitude()).build());
                        incidentDuringRide = null;
                    }

                    if (obsLiteEvent != null && lastLocation != null) {
                        double handleBarLength = SharedPref.Settings.Ride.OvertakeWidth.getHandlebarWidth(RecorderService.this);
                        double eventDistance = obsLiteEvent.getDistanceMeasurement().getDistance() * 100.0;
                        double realDistance = handleBarLength + eventDistance;
                        if (realDistance >= 150) {
                            Log.d(TAG, "Adding hidden Close Pass with TS: " + lastAccUpdate + " realLeftDistance: " + realDistance);
                            incidentLog.updateOrAddIncident(IncidentLogEntry.newBuilder().withBaseInformation(lastAccUpdate,lastLocation.getLatitude(),lastLocation.getLongitude()).withIncidentType(IncidentLogEntry.INCIDENT_TYPE.OBS_LITE).withDescription(getString(R.string.overtake_distance_left,((int)obsLiteEvent.getDistanceMeasurement().getDistance()))).withKey(5000).build());
                        } else {
                            Log.d(TAG, "Adding visible Close Pass with TS: " + lastAccUpdate + " realLeftDistance: " + realDistance);
                            incidentLog.updateOrAddIncident(IncidentLogEntry.newBuilder().withBaseInformation(lastAccUpdate,lastLocation.getLatitude(),lastLocation.getLongitude()).withIncidentType(IncidentLogEntry.INCIDENT_TYPE.CLOSE_PASS).withDescription(getString(R.string.overtake_distance_left,obsLiteEvent.getDistanceMeasurement().getDistance())).withKey(4000).build());
                        }
                        obsLiteEvent = null;
                    }

                    /*while (lastOBSClosePassEvents.size() > 0) {
                        OBSService.ClosePassEvent closePassEvent = lastOBSClosePassEvents.removeFirst();
                        incidentLog.updateOrAddIncident(IncidentLogEntry.newBuilder()
                                .withIncidentType(closePassEvent.getIncidentType())
                                .withDescription(closePassEvent.getIncidentDescription(getApplicationContext()))
                                .withBaseInformation(lastAccUpdate, thisLocation.getLatitude(), thisLocation.getLongitude())
                                .build());
                    }*/
                }
                dataLogEntryBuilder.withGyroscope(
                        /**/gyroscopeMatrix[0],
                        gyroscopeMatrix[1],
                        gyroscopeMatrix[2]/**/
                );

                /*if (lastOBSDistanceValues.size() > 0) {
                    OBSService.Measurement lastOBSDistanceValue = lastOBSDistanceValues.removeFirst();
                    dataLogEntryBuilder.withOBS(lastOBSDistanceValue.leftSensorValues.get(0), null, null, null, null);
                }*/

                if(isGPSLine) {
                    gpsLines.add(dataLogEntryBuilder.build());
                    Log.d(TAG,"obsLiteDataSB.length(): " + obsLiteDataSB.length());
                    Log.d(TAG, "gpsLines.size(): " + gpsLines.size());
                    Log.d(TAG, "sensorLines.size(): " + sensorLines.size());
                } else if(!gpsLines.isEmpty()) {
                    sensorLines.add(dataLogEntryBuilder.build());
                }
                lineAdded = true;

                endTime = System.currentTimeMillis();

                /**/
                for (int i = 0; i < Constants.MVG_AVG_STEP; i++) {
                    accelerometerQueueX.remove();
                    accelerometerQueueY.remove();
                    accelerometerQueueZ.remove();
                    linearAccelerometerQueueX.remove();
                    linearAccelerometerQueueY.remove();
                    linearAccelerometerQueueZ.remove();
                    rotationQueueX.remove();
                    rotationQueueY.remove();
                    rotationQueueZ.remove();
                    rotationQueueC.remove();
                }
                 /**/
            /**/}
            lastHandlerStart = start;
            recordingHandler.postDelayed(this,50);
        }
    }

    static class LooperThread extends Thread {
        public Handler mHandler;

        @Override
        public void run() {
            Looper.prepare();


            mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
                public void handleMessage(@NonNull Message msg) {
                    Log.d(TAG, "msg:" + msg);
                }
            };
            Looper.loop();
        }

    }

    private void tryConnectOBSLite() {
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (!availableDrivers.isEmpty()) {
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());

            if (connection == null) {
                IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    this.registerReceiver(usbReceiver,filter,RECEIVER_EXPORTED);
                } else {
                    ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
                }
                PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                usbManager.requestPermission(driver.getDevice(),permissionIntent);
            } else {
                UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
                try {
                    port.open(connection);
                    port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    Log.d(TAG, "usb serial port opened");
                    usbIoManager = new SerialInputOutputManager(port, this);
                    usbIoManager.run();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void disconnectOBSLite() {
        if(usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            if (usbSerialPort != null) {
                usbSerialPort.close();
            }
        } catch (IOException ignored) {}
        usbSerialPort = null;
        Log.d(TAG,"disconnected from obsLite");
        Log.d(TAG, "obsLiteSession.getByteListQueue().size(): " + obsLiteSession.getByteListQueue().size());
    }
    @Override
    public void onNewData(byte[] data) {

        obsLiteHandler.post(new Runnable() {
            @Override
            public void run() {
                handleObsLiteData(data);
            }
        });
    }
    @SuppressLint("MissingPermission")
    public void handleObsLiteData(byte[] data) {

        if (obsLiteStartTime == 0L) {
            obsLiteStartTime = System.currentTimeMillis();
            obsLiteSession.setObsLiteStartTime(obsLiteStartTime);
        }

        obsLiteSession.fillByteList(data);

        boolean foundZero = obsLiteSession.completeCobsAvailable();

        if (lastLocation == null) {
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            // Only use GPS provider for privacy
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        }
        if (foundZero && lastLocation != null) {
            Event userInputEvent = obsLiteSession.handleEvent(lastLocation.getLatitude(),
                    lastLocation.getLongitude(), lastLocation.getAltitude(), lastLocation.getAccuracy());
            if (userInputEvent != null) {
                Log.d(TAG, "OBS event:" + userInputEvent.toString());
                obsLiteEvent = userInputEvent;
            }
        }

    }

    @Override
    public void onRunError(Exception e) {

    }

    public class MyBinder extends Binder {
        public RecorderService getService() {
            return RecorderService.this;
        }
    }
}
