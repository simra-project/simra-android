package de.tuberlin.mcc.simra.app.services;

import android.annotation.SuppressLint;
import android.app.Notification;
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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import de.tuberlin.mcc.simra.app.util.Constants;
import de.tuberlin.mcc.simra.app.util.ForegroundServiceNotificationManager;
import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.IncidentBroadcaster;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.UnitHelper;

import static de.tuberlin.mcc.simra.app.services.OBSService.ACTION_VALUE_RECEIVED_CLOSEPASS_EVENT;
import static de.tuberlin.mcc.simra.app.services.OBSService.ACTION_VALUE_RECEIVED_DISTANCE;
import static de.tuberlin.mcc.simra.app.services.OBSService.EXTRA_VALUE_SERIALIZED;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.overwriteFile;

public class RecorderService extends Service implements SensorEventListener, LocationListener {
    public static final String TAG = "RecorderService_LOG:";
    //long curTime;
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
    private final LinkedList<OBSService.Measurement> lastOBSDistanceValues = new LinkedList<>();
    private final LinkedList<OBSService.ClosePassEvent> lastOBSClosePassEvents = new LinkedList<>();
    private LocationManager locationManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor linearAccelerometer;
    private Sensor rotation;
    private int key;
    private long lastPictureTaken = 0;
    private Integer incidentDuringRide = null;
    private final BroadcastReceiver openBikeSensorMessageReceiverDistanceValue = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Serializable serializable = intent.getSerializableExtra(EXTRA_VALUE_SERIALIZED);

            if (serializable instanceof OBSService.Measurement) {
                lastOBSDistanceValues.add((OBSService.Measurement) serializable);
            }
        }
    };
    private final BroadcastReceiver openBikeSensorMessageReceiverClosePassEvent = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Serializable serializable = intent.getSerializableExtra(EXTRA_VALUE_SERIALIZED);

            if (serializable instanceof OBSService.ClosePassEvent) {
                lastOBSClosePassEvents.add((OBSService.ClosePassEvent) serializable);
            }
        }
    };
    private BroadcastReceiver incidentBroadcastReceiver;
    // This is set to true, when recording is allowed according to Privacy-Duration and
    // Privacy-Distance (see sharedPrefs, set in StartActivity and edited in settings)
    private boolean recordingAllowed;
    private boolean takePictureDuringRideActivated;
    private int takePictureDuringRideInterval;
    private int safetyDistanceWithTolerances;
    private float privacyDistance;
    private long privacyDuration;
    private boolean lineAdded;
    private long lastGPSUpdate = 0;
    private SensorManager sensorManager = null;
    private PowerManager.WakeLock wakeLock = null;
    private final IBinder mBinder = new MyBinder();
    private final StringBuilder accGpsString = new StringBuilder();
    private IncidentLog incidentLog = null;

    public int getCurrentRideKey() {
        return key;
    }

    public double getDuration() {
        return (System.currentTimeMillis() - startTime);
    }

    public boolean getRecordingAllowed() {
        return recordingAllowed;
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
            /*curTime = System.currentTimeMillis();*/
            /*
            // Privacy filter: Set recordingAllowed to true, when enough time (privacyDuration) passed
            // since the user pressed Start Recording AND there is enough distance (privacyDistance)
            // between the starting location and the current location.
            if (!recordingAllowed && startLocation != null && lastLocation != null) {
                if ((startLocation.distanceTo(lastLocation) >= privacyDistance)
                        && ((curTime - startTime) > privacyDuration)) {
                    recordingAllowed = true;
                }
            }
            */
            /**/linearAccelerometerMatrix = event.values;/**/
            /*
            if (((curTime - lastAccUpdate) >= Constants.ACCELEROMETER_FREQUENCY) && recordingAllowed) {

                lastAccUpdate = curTime;
                // Write data to file in background thread
                try {

                    Runnable insertHandler = new InsertHandler(accelerometerMatrix, gyroscopeMatrix, linearAccelerometerMatrix, rotationMatrix);
                    executor.execute(insertHandler);

                } catch (Exception e) {
                    Log.e(TAG, "insertData: " + e.getMessage(), e);
                }
            }
            */
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

        // Take only GPS fixes that are somewhat accurate to prevent spikes in the route.
        // if (location.getAccuracy() < Constants.GPS_ACCURACY_THRESHOLD) {
            // Set start location. Important for privacy distance.
            if (startLocation == null) {
                startLocation = location;
            }
            lastLocation = location;
        //}
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

        takePictureDuringRideActivated = SharedPref.Settings.Ride.PicturesDuringRide.isActivated(this);
        takePictureDuringRideInterval = SharedPref.Settings.Ride.PicturesDuringRideInterval.getInterval(this);
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

        // Executor service for writing data
        /*executor = Executors.newSingleThreadExecutor();*/

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(openBikeSensorMessageReceiverDistanceValue, new IntentFilter(ACTION_VALUE_RECEIVED_DISTANCE));
        localBroadcastManager.registerReceiver(openBikeSensorMessageReceiverClosePassEvent, new IntentFilter(ACTION_VALUE_RECEIVED_CLOSEPASS_EVENT));
        incidentBroadcastReceiver = IncidentBroadcaster.receiveIncidents(this, new IncidentBroadcaster.IncidentCallbacks() {
            @Override
            public void onManualIncident(int incidentType) {
                incidentDuringRide = incidentType;
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
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
                    /*
                    float[] actualAccelerometerMatrix = accelerometerMatrix;
                    float[] actualGyroscopeMatrix = gyroscopeMatrix;
                    float[] actualLinearAccelerometerMatrix = linearAccelerometerMatrix;
                    float[] actualRotationMatrix = rotationMatrix;
                    Location actualLocation = lastLocation;
                    if (actualLocation == null) {
                        actualLocation = locationManager.getLastKnownLocation(LocationManager
                                .GPS_PROVIDER);
                    }
                    if (actualLocation == null) {
                        actualLocation = new Location(LocationManager
                                .GPS_PROVIDER);
                    }
                    */
                    Runnable recorder = new InsertHandler(/*actualLocation,actualAccelerometerMatrix, actualGyroscopeMatrix, actualLinearAccelerometerMatrix, actualRotationMatrix*/);
                    recordingHandler.post(recorder);

                } else {
                    // Repeat this the same runnable code block again another 50 ms
                    // 'this' is referencing the Runnable object
                    recordingStarterHandler.postDelayed(this, 50);
                }

                Log.d(TAG, "Handler called on main thread: " + System.currentTimeMillis());

            }
        };
        recordingStarterHandler.post(recordingStarter);
        // When the user records a route for the first time, the ride key is 0.
        // For all subsequent rides, the key value increases by one at a time.

        key = sharedPrefs.getInt("RIDE-KEY", 0);
        incidentLog = new IncidentLog(key, new HashMap<>());

        // Fire the notification while recording
        Notification notification =
                ForegroundServiceNotificationManager.createOrUpdateNotification(
                        this,
                        getResources().getString(R.string.foregroundNotificationTitle_record),
                        getResources().getString(R.string.foregroundNotificationBody_record)
                );
        startForeground(ForegroundServiceNotificationManager.getNotificationId(), notification);
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

        // Create a file for the ride and write ride into it (AccGpsFile). Also, update metaData.csv
        // with current ride and and sharedPrefs with current ride key. Do these things only,
        // if recording is allowed (see privacyDuration and privacyDistance) and we have written some
        // data.
        if (recordingAllowed && lineAdded) {
            recordingHandler.removeCallbacksAndMessages(null);
            int region = lookUpIntSharedPrefs("Region", 0, "Profile", this);
            overwriteFile((IOUtils.Files.getFileInfoLine() + DataLog.DATA_LOG_HEADER + System.lineSeparator() + accGpsString.toString()), IOUtils.Files.getGPSLogFile(key, false, this));
            MetaData.updateOrAddMetaDataEntryForRide(new MetaDataEntry(key, startTime, endTime, MetaData.STATE.JUST_RECORDED, 0, waitedTime, Math.round(route.getDistance()), 0, region), this);
            IncidentLog.saveIncidentLog(incidentLog, this);
            editor.putInt("RIDE-KEY", key + 1);
            editor.apply();
        }

        // Prevent new tasks from being added to thread
        /*executor.shutdown();*/

        // Unregister receiver and listener prior to gpsExecutor shutdown
        sensorManager.unregisterListener(this);

        // Stop requesting location updates
        locationManager.removeUpdates(this);

        // Stop OpenBikeSensor LocalBroadcast Listener
        LocalBroadcastManager.getInstance(this).unregisterReceiver(openBikeSensorMessageReceiverDistanceValue);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(openBikeSensorMessageReceiverClosePassEvent);

        // Stop Manual Incident Broadcast Listener
        LocalBroadcastManager.getInstance(this).unregisterReceiver(incidentBroadcastReceiver);

        // Remove the Notification
        ForegroundServiceNotificationManager.cancelNotification(this);
        /*
        // Create new thread to wait for gpsExecutor to clear queue and wait for termination
        new Thread(() -> {
            try {
                // Wait for all tasks to finish before we proceed
                while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    Log.d(TAG, "Waiting for current tasks to finish");
                }
                Log.d(TAG, "No queue to clear");
            } catch (InterruptedException e) {
                Log.e(TAG, "Exception caught while waiting for finishing gpsExecutor tasks");
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            if (executor.isTerminated()) {
                // Stop everything else once the task queue is clear
                stopForeground(true);
                wakeLock.release();

            }
        }).start();
        */
        /**/stopForeground(true);/**/
        /**/wakeLock.release();/**/
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

        /*
        final float[] accelerometerMatrix;
        final float[] gyroscopeMatrix;
        final float[] linearAccelerometerMatrix;
        final float[] rotationMatrix;
        final Location lastLocation;
        */
        // Store the current accGpsFile array values into THIS objects arrays, and db insert from this object
        public InsertHandler(/*Location lastLocation, float[] accelerometerMatrix, float[] gyroscopeMatrix, float[] linearAccelerometerMatrix, float[] rotationMatrix*/) {
            /*
            this.accelerometerMatrix = accelerometerMatrix;
            this.gyroscopeMatrix = gyroscopeMatrix;
            this.linearAccelerometerMatrix = linearAccelerometerMatrix;
            this.rotationMatrix = rotationMatrix;
            this.lastLocation = lastLocation;
            */
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
                        /*0f,0f,0f*/
                        /**/computeAverage(accelerometerQueueX),
                        computeAverage(accelerometerQueueY),
                        computeAverage(accelerometerQueueZ)/**/
                );

                dataLogEntryBuilder.withLinearAccelerometer(
                        // Every average is computed over 30 data points
                        /*0f,0f,0f*/
                        /**/computeAverage(linearAccelerometerQueueX),
                        computeAverage(linearAccelerometerQueueY),
                        computeAverage(linearAccelerometerQueueZ)/**/
                );

                dataLogEntryBuilder.withRotation(
                        // Every average is computed over 30 data points
                        /*0f,0f,0f,0f,0f*/
                        /**/computeAverage(rotationQueueX),
                        computeAverage(rotationQueueY),
                        computeAverage(rotationQueueZ),
                        computeAverage(rotationQueueC)/**/
                );

                if ((lastAccUpdate - lastGPSUpdate) >= Constants.GPS_FREQUENCY) {
                    lastGPSUpdate = lastAccUpdate;
                    Location thisLocation = lastLocation;
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
                            thisLocation.getAccuracy(),
                            thisLocation.getTime()
                    );

                    if (incidentDuringRide != null) {
                        incidentLog.updateOrAddIncident(IncidentLogEntry.newBuilder().withIncidentType(incidentDuringRide).withBaseInformation(lastAccUpdate, lastLocation.getLatitude(), lastLocation.getLongitude()).build());
                        incidentDuringRide = null;
                    }

                    while (lastOBSClosePassEvents.size() > 0) {
                        OBSService.ClosePassEvent closePassEvent = lastOBSClosePassEvents.removeFirst();
                        incidentLog.updateOrAddIncident(IncidentLogEntry.newBuilder()
                                .withIncidentType(closePassEvent.getIncidentType())
                                .withDescription(closePassEvent.getIncidentDescription(getApplicationContext()))
                                .withBaseInformation(lastAccUpdate, thisLocation.getLatitude(), thisLocation.getLongitude())
                                .build());
                    }
                }
                dataLogEntryBuilder.withGyroscope(
                        /*0f,0f,0f*/
                        /**/gyroscopeMatrix[0],
                        gyroscopeMatrix[1],
                        gyroscopeMatrix[2]/**/
                );

                if (lastOBSDistanceValues.size() > 0) {
                    OBSService.Measurement lastOBSDistanceValue = lastOBSDistanceValues.removeFirst();
                    dataLogEntryBuilder.withOBS(lastOBSDistanceValue.leftSensorValues.get(0), null, null, null, null);

                    if (takePictureDuringRideActivated) {
                        if (lastOBSDistanceValue.leftSensorValues.get(0) <= safetyDistanceWithTolerances && lastPictureTaken + takePictureDuringRideInterval * 1000 <= lastAccUpdate) {
                            lastPictureTaken = lastAccUpdate;
                            CameraService.takePicture(RecorderService.this, String.valueOf(lastAccUpdate), IOUtils.Directories.getPictureCacheDirectoryPath());
                        }
                    }
                }

                String str = dataLogEntryBuilder.build().stringifyDataLogEntry();

                accGpsString.append(str).append(System.getProperty("line.separator"));
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

    public class MyBinder extends Binder {
        public RecorderService getService() {
            return RecorderService.this;
        }
    }
}
