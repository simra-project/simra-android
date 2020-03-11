package de.tuberlin.mcc.simra.app.main;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.util.Constants;

import static de.tuberlin.mcc.simra.app.util.Utils.appendToFile;
import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpIntSharedPrefs;

public class RecorderService extends Service implements SensorEventListener, LocationListener {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Properties
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public static final String TAG = "RecorderService_LOG:";
    final int ACC_POLL_FREQUENCY = Constants.ACC_FREQUENCY;
    final int GPS_POLL_FREQUENCY = Constants.GPS_FREQUENCY;
    long curTime;
    long startTime = 0;
    long endTime;
    ExecutorService executor;
    Sensor accelerometer;
    Sensor gyroscope;
    float[] accelerometerMatrix = new float[3];
    float[] gyroscopeMatrix = new float[3];
    String pathToAccGpsFile = "";
    int key;
    LocationManager locationManager;
    Location lastLocation;
    float lastAccuracy;
    Polyline route = new Polyline();
    long waitedTime = 0;


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Strings for storing data to enable continued use by other activities
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // For Managing the notification shown while the service is running
    int notificationId = 1337;
    NotificationManagerCompat notificationManager;
    Queue<Float> accXQueue;
    Queue<Float> accYQueue;
    Queue<Float> accZQueue;
    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;
    Location startLocation;

    // This is set to true, when recording is allowed according to Privacy-Duration and
    // Privacy-Distance (see sharedPrefs, set in StartActivity and edited in settings)
    private boolean recordingAllowed;
    private float privacyDistance;
    private long privacyDuration;
    private boolean lineAdded;

    private long lastAccUpdate = 0;
    private long lastGPSUpdate = 0;
    private SensorManager sensorManager = null;
    private PowerManager.WakeLock wakeLock = null;
    private IBinder mBinder = new MyBinder();
    private StringBuilder accGpsString = new StringBuilder();

    public String getPathToAccGpsFile() {
        return pathToAccGpsFile;
    }

    public double getDuration() {
        return (curTime - startTime);
    }

    public boolean getRecordingAllowed() {
        return recordingAllowed;
    }

    public long getStartTime() {
        return startTime;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // SensorEventListener Methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            curTime = System.currentTimeMillis();

            // Privacy filter: Set recordingAllowed to true, when enough time (privacyDuration) passed
            // since the user pressed Start Recording AND there is enough distance (privacyDistance)
            // between the starting location and the current location.
            if (!recordingAllowed && startLocation != null && lastLocation != null) {
                if ((startLocation.distanceTo(lastLocation) >= (float) privacyDistance)
                        && ((curTime - startTime) > privacyDuration)) {
                    recordingAllowed = true;
                }
            }
                accelerometerMatrix = event.values;


            if (((curTime - lastAccUpdate) >= ACC_POLL_FREQUENCY) && recordingAllowed) {

                lastAccUpdate = curTime;
                // Write data to file in background thread
                try {
                    Runnable insertHandler = new InsertHandler(accelerometerMatrix, gyroscopeMatrix);
                    executor.execute(insertHandler);
                } catch (Exception e) {
                    Log.e(TAG, "insertData: " + e.getMessage(), e);
                }

            }
        }
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroscopeMatrix = event.values;

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
        if (location.getAccuracy() < Constants.GPS_ACCURACY_THRESHOLD) {
            // Set start location. Important for privacy distance.
            if (startLocation == null) {
                startLocation = location;
            }
            lastLocation = location;
            lastAccuracy = location.getAccuracy();
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
        sharedPrefs = getApplicationContext()
                .getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);

        editor = sharedPrefs.edit();


        // Prepare the accelerometer accGpsFile
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // Request location updates
        locationManager = (LocationManager) getSystemService(Context
                .LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager
                .GPS_PROVIDER, 3000, 1.0f, this);

        // Queues for storing acc data
        accXQueue = new LinkedList<>();
        accYQueue = new LinkedList<>();
        accZQueue = new LinkedList<>();

        // When the user records a route for the first time, the ride key is 0.
        // For all subsequent rides, the key value increases by one at a time.
        if (!sharedPrefs.contains("RIDE-KEY")) {

            editor.putInt("RIDE-KEY", 0);

            editor.apply();
        }

        // Load the privacy settings
        privacyDistance = (float) sharedPrefs.getInt("Privacy-Distance", 30);
        privacyDuration = (sharedPrefs.getLong("Privacy-Duration", 30) * 1000);
        Log.d(TAG, "privacyDistance: " + privacyDistance + " privacyDuration: " + privacyDuration);



        // Prevent the App to be killed while recording
        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":RecorderService");

        // Executor service for writing data
        executor = Executors.newSingleThreadExecutor();
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


        // When the user records a route for the first time, the ride key is 0.
        // For all subsequent rides, the key value increases by one at a time.

        key = sharedPrefs.getInt("RIDE-KEY", 0);
        pathToAccGpsFile = key + "_accGps.csv";

        // Fire the notification while recording
        Notification notification = createNotification().build();
        notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId, notification);
        startForeground(notificationId, notification);
        wakeLock.acquire(14400000);

        // Register Accelerometer and Gyroscope
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope,
                2900);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        // Create a file for the ride and write ride into it (AccGpsFile). Also, update metaData.csv
        // with current ride and and sharedPrefs with current ride key. Do these things only,
        // if recording is allowed (see privacyDuration and privacyDistance) and we have written some
        // data.
        if (recordingAllowed && lineAdded) {

            String fileInfoLine = getAppVersionNumber(this) + "#1" + System.lineSeparator();
            Log.d(TAG, "fileInfoLine: " + fileInfoLine);
            int region = lookUpIntSharedPrefs("Region",0,"Profile",this);
            // Create head of the csv-file
            appendToFile((fileInfoLine + "lat,lon,X,Y,Z,timeStamp,acc,a,b,c" + System.lineSeparator()), pathToAccGpsFile, this);
            // Write String data to files
            appendToFile(accGpsString.toString(), pathToAccGpsFile, this);
            appendToFile(key + ","
                    + String.valueOf(startTime) + "," + String.valueOf(endTime) + ","
                    + "0,0," + waitedTime + "," + Math.round(route.getDistance()) + ",0," + region + System.lineSeparator(), "metaData.csv", this);
            editor.putInt("RIDE-KEY", key + 1);
            editor.apply();
        }


        // Prevent new tasks from being added to thread
        executor.shutdown();

        // Unregister receiver and listener prior to gpsExecutor shutdown
        sensorManager.unregisterListener(this);

        // Stop requesting location updates
        locationManager.removeUpdates(this);

        // Remove the Notification
        notificationManager.cancel(notificationId);

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

    }

    private float computeAverage(Collection<Float> myVals) {
        float sum = 0;
        for (float f : myVals) {
            sum += f;
        }
        return sum / myVals.size();
    }

    private NotificationCompat.Builder createNotification() {
        String CHANNEL_ID = "RecorderServiceNotification";
        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, contentIntent, 0);
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.recorder_channel_name);
            String description = getString(R.string.recorder_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.helmet)
                .setContentTitle(getResources().getString(R.string.recordingNotificationTitle))
                .setContentText(getResources().getString(R.string.recordingNotificationBody))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent);

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Runnables
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    class InsertHandler implements Runnable {

        final float[] accelerometerMatrix;
        final float[] gyroscopeMatrix;

        // Store the current accGpsFile array values into THIS objects arrays, and db insert from this object
        public InsertHandler(float[] accelerometerMatrix, float[] gyroscopeMatrix) {
            this.accelerometerMatrix = accelerometerMatrix;
            this.gyroscopeMatrix = gyroscopeMatrix;
        }

        @SuppressLint("MissingPermission")
        public void run() {

            /** Every average is computed over 30 data points, so we want the queues for the
             three accelerometer values to be of size 30 in order to compute the averages.

             Accordingly, when the queues are shorter we're adding data points.
             */

            if (accXQueue.size() < 30) {

                accXQueue.add(accelerometerMatrix[0]);
                accYQueue.add(accelerometerMatrix[1]);
                accZQueue.add(accelerometerMatrix[2]);

            } else {

                // The gps (lat, lon, accuracy) information are recorded (around) every 3 seconds.
                // The accelerometer data (x,y,z) information are recorded 50 times a second.
                // The gyroscope data (a,b,c) information are recorded (around) every 3 seconds.
                // So the gps and gyroscope data changes (around) every 150 lines. We store the gps
                // data only after the 3 seconds are over.
                String gps = ",";
                String accuracy = "";
                String gyro = ",,";

                if ((lastAccUpdate - lastGPSUpdate) >= GPS_POLL_FREQUENCY) {
                    lastGPSUpdate = lastAccUpdate;

                    if (lastLocation == null) {
                        lastLocation = locationManager.getLastKnownLocation(LocationManager
                                .GPS_PROVIDER);
                    }
                    if (lastLocation == null) {
                        lastLocation = new Location(LocationManager
                                .GPS_PROVIDER);
                    }

                    route.addPoint(new GeoPoint(lastLocation.getLatitude(),lastLocation.getLongitude()));
                    if (lastLocation.getSpeed() <= 3.0) {
                        waitedTime += 3;
                    }
                    gps =   String.valueOf(lastLocation.getLatitude()) + "," +
                            String.valueOf(lastLocation.getLongitude());
                    accuracy =  String.valueOf(lastAccuracy);
                    gyro =  String.valueOf(gyroscopeMatrix[0]) + "," +
                            String.valueOf(gyroscopeMatrix[1]) + "," +
                            String.valueOf(gyroscopeMatrix[2]);
                }

                // The queues are of sufficient size, let's compute the averages.

                float xAvg = computeAverage(accXQueue);
                float yAvg = computeAverage(accYQueue);
                float zAvg = computeAverage(accZQueue);

                // Put the averages + time data into a string and append to file.
                String str =
                        gps + "," +
                        String.valueOf(xAvg) + "," +
                        String.valueOf(yAvg) + "," +
                        String.valueOf(zAvg) + "," +
                        curTime + "," +
                        accuracy + "," +
                        gyro;

                accGpsString.append(str).append(System.getProperty("line.separator"));
                lineAdded = true;
                if (startTime == 0) {
                    startTime = curTime;
                } else {
                    endTime = curTime;
                }

                /** Now remove as many elements from the queues as our moving average step/shift
                 specifies and therefore enable new data points to come in.
                 */

                for (int i = 0; i < Constants.MVG_AVG_STEP; i++) {

                    accXQueue.remove();
                    accYQueue.remove();
                    accZQueue.remove();
                }

            }

        }
    }

    public class MyBinder extends Binder {
        RecorderService getService() {
            return RecorderService.this;
        }
    }


}
