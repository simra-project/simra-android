package app.com.example.android.octeight;

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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static app.com.example.android.octeight.Utils.appendToFile;

public class RecorderService extends Service implements SensorEventListener, LocationListener {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Properties
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public static final String TAG = "RecorderService_LOG:";
    final int ACC_POLL_FREQUENCY = Constants.ACC_FREQUENCY;
    final int GPS_POLL_FREQUENCY = Constants.GPS_FREQUENCY;
    long curTime;
    long startTime;
    long endTime;
    ExecutorService executor;
    Sensor accelerometer;
    float[] accelerometerMatrix = new float[3];
    String pathToAccGpsFile = "";
    LocationManager locationManager;
    Location lastLocation;


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
    private String accGpsString = "";

    public String getPathToAccGpsFile() { return pathToAccGpsFile; }

    public double getDuration() { return (curTime - startTime); }

    public boolean getRecordingAllowed() { return recordingAllowed; }

    public long getStartTime() { return startTime; }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // The ride object which will be created and retrieved by the MainActivity at the end of the
    // service
    public Ride getRide() {
        File accGpsFile = getFileStreamPath(pathToAccGpsFile);
        return new Ride(accGpsFile, String.valueOf(startTime), String.valueOf((curTime - startTime)), 0, this);
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // SensorEventListener Methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public void onSensorChanged(SensorEvent event) {
        curTime = System.currentTimeMillis();

        // Privacy filter: Set recordingAllowed to true, when enough time (privacyDuration) passed
        // since the user pressed Start Recording AND there is enough distance (privacyDistance)
        // between the starting location and the current location.
        if(!recordingAllowed && startLocation != null && lastLocation!= null){
            if((startLocation.distanceTo(lastLocation)>=(float) privacyDistance)
                    && ((curTime-startTime)>privacyDuration)){
                    recordingAllowed = true;
            }
        }

        accelerometerMatrix = event.values;


        if(((curTime - lastAccUpdate) >= ACC_POLL_FREQUENCY) && recordingAllowed) {

            lastAccUpdate = curTime;
            // Write data to file in background thread
            try {
                Runnable insertHandler = new InsertHandler(accelerometerMatrix);
                executor.execute(insertHandler);
            } catch (Exception e) {
                Log.e(TAG, "insertData: " + e.getMessage(), e);
            }

        }


        /*
        if ((curTime - lastGPSUpdate) >= GPS_POLL_FREQUENCY){
            lastGPSUpdate = curTime;
            //write data to file in background thread
            try{
                Log.d(TAG, "RecorderService executing gpsExecutor");
                Runnable insertGPSHandler = new InsertGPSHandler();
                gpsExecutor.execute(insertGPSHandler);
            } catch (Exception e) {
                Log.e(TAG, "insertData: " + e.getMessage(), e);
            }
        }*/

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
        if (location.getAccuracy() < Constants.GPS_ACCURACY_THRESHOLD){
            // Set start location. Important for privacy distance.
            if(startLocation == null){
                startLocation = location;
            }
            lastLocation = location;
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
        startTime = System.currentTimeMillis();

        // Initialize sharedPrefs & editor
        sharedPrefs = getApplicationContext()
                .getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);

        editor = sharedPrefs.edit();


        // Prepare the accelerometer accGpsFile
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Request location updates
        locationManager = (LocationManager) getSystemService(Context
                .LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager
                .GPS_PROVIDER,3000,1.0f,this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,3000,1.0f,this);

        // Queues for storing acc data
        accXQueue = new LinkedList<>();
        accYQueue = new LinkedList<>();
        accZQueue = new LinkedList<>();
        // accQQueue = new LinkedList<>();

        // date = DateFormat.getDateTimeInstance().format(new Date());

        // When the user records a route for the first time, the ride key is 0.
        // For all subsequent rides, the key value increases by one at a time.
        if (!sharedPrefs.contains("RIDE-KEY")) {

            editor.putInt("RIDE-KEY", 0);

            editor.apply();
        }

        // Load the privacy settings
        privacyDistance = (float) sharedPrefs.getInt("Privacy-Distance", 30);
        privacyDuration = (sharedPrefs.getLong("Privacy-Duration", 30)*1000);
        Log.d(TAG, "privacyDistance: "  + privacyDistance + " privacyDuration: " + privacyDuration);

        pathToAccGpsFile = sharedPrefs.getInt("RIDE-KEY", 0)
                + "_accGps_"
                + startTime +/*date +*/ ".csv";

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

        // Fire the notification while recording
        Notification notification = createNotification().build();
        notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId, notification);
        startForeground(notificationId, notification);
        wakeLock.acquire();

        // Register Accelerometer accGpsFile
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        endTime = curTime;

        // Create a file for the ride and write ride into it (AccGpsFile). Also, update metaData.csv
        // with current ride and and sharedPrefs with current ride key. Do these things only,
        // if recording is allowed (see privacyDuration and privacyDistance) and we have written some
        // data.
        if(recordingAllowed && lineAdded) {

            // Create head of the csv-file
            appendToFile("lat,lon,X,Y,Z,timeStamp"+System.lineSeparator(), pathToAccGpsFile, this);

            // Write String data to files
            appendToFile(accGpsString, pathToAccGpsFile, this);
            appendToFile(String.valueOf(sharedPrefs.getInt("RIDE-KEY",0)) + ","
                    + String.valueOf(startTime) + "," + String.valueOf(endTime) + ","
                    + "0" + System.lineSeparator(), "metaData.csv", this);

            // When the user records a route for the first time, the ride key is 0.
            // For all subsequent rides, the key value increases by one at a time.

            int key = sharedPrefs.getInt("RIDE-KEY", 0);

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

        // Log.d(TAG, "onDestroy() writing accGpsString");
        // Log.d(TAG, accString);
        // Log.d(TAG, gpsString);

        /*
        executor.execute( () -> {
            try {
                appendToFile(gpsString, gpsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        */


        // Create new thread to wait for gpsExecutor to clear queue and wait for termination
        new Thread(() -> {
            try {
                // Wait for all tasks to finish before we proceed
                while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    Log.i(TAG, "Waiting for current tasks to finish");
                }
                Log.i(TAG, "No queue to clear");
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
        for(float f : myVals) {
            sum += f;
        }
        return sum/myVals.size();
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
                .setContentTitle("Aufzeichnung der Fahrt")
                .setContentText("Ihre Fahrt wird aufgezeichnet.")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent);

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Runnables
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    class InsertHandler implements Runnable {

        final float[] accelerometerMatrix;

        // Store the current accGpsFile array values into THIS objects arrays, and db insert from this object
        public InsertHandler(float[] accelerometerMatrix) {
            this.accelerometerMatrix = accelerometerMatrix;
        }

        @SuppressLint("MissingPermission")
        public void run() {

            // Record location data
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            /*
            if((curTime - lastGPSUpdate) >= GPS_POLL_FREQUENCY) {
                lastGPSUpdate = curTime;

                if (lastLocation == null){
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if (lastLocation == null){
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (lastLocation == null){
                    lastLocation = new Location(LocationManager.GPS_PROVIDER);
                }

                String str = String.valueOf(lastLocation.getLongitude()) + ", " +
                        String.valueOf(lastLocation.getLatitude()) + ", " +
                        (curTime - startTime) + ", " +
                        (curTime - lastGPSUpdate) + ", " +
                        DateFormat.getDateTimeInstance().format(new Date());

                // Log.d(TAG, "GPSService InsertAccHandler run(): " + str);

                gpsString += str += '\n';
            }
            */

            // Record accelerometer and location data
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

            // Log.d(TAG, String.valueOf(curTime));
                double x = accelerometerMatrix[0];
                double y = accelerometerMatrix[1];
                double z = accelerometerMatrix[2];
                /*
                float mAccelCurrent = (float) Math.sqrt(x*x+y*y+z*z);

                mAcceleration += String.valueOf(x) + "," + String.valueOf(y) + ","
                        + String.valueOf(z) + ","+ String.valueOf(mAccelCurrent);
                mAcceleration += '\n';
                */



            /** Every average is computed over 30 data points, so we want the queues for the
                three accelerometer values to be of size 30 in order to compute the averages.

                Accordingly, when the queues are shorter we're adding data points.
             */

            if(accXQueue.size() < 30) {

                accXQueue.add(accelerometerMatrix[0]);
                accYQueue.add(accelerometerMatrix[1]);
                accZQueue.add(accelerometerMatrix[2]);
                // accQQueue.add(mAccelCurrent);

            } else {

                // The gps (lat, lon) information are recorded (around) every 3 seconds.
                // The accGpsFile data (x,y,z) information are recorded 50 times a second.
                // So the gps changes (around) every 150 lines. We store the gps data
                // only after the 3 seconds are over.
                String gps = ",,";

                if((lastAccUpdate - lastGPSUpdate) >= GPS_POLL_FREQUENCY) {
                    lastGPSUpdate = lastAccUpdate;

                    if (lastLocation == null) {
                        lastLocation = locationManager.getLastKnownLocation(LocationManager
                                .GPS_PROVIDER);
                    }
                    if (lastLocation == null) {
                        lastLocation = locationManager.getLastKnownLocation(LocationManager
                                .NETWORK_PROVIDER);
                    }
                    if (lastLocation == null) {
                        lastLocation = new Location(LocationManager
                                .GPS_PROVIDER);
                    }

                    gps = String.valueOf(lastLocation.getLatitude()) + "," +
                            String.valueOf(lastLocation.getLongitude()) + ",";
                }

                // The queues are of sufficient size, let's compute the averages.

                float xAvg = computeAverage(accXQueue);
                float yAvg = computeAverage(accYQueue);
                float zAvg = computeAverage(accZQueue);
                //float qAvg = computeAverage(accQQueue);

                // Put the averages + time data into a string and append to file.
                String str = gps + String.valueOf(xAvg) + "," +
                        String.valueOf(yAvg) + "," +
                        String.valueOf(zAvg) + "," +
                        curTime;

                accGpsString += str /*+= String.valueOf(qAvg)*/;
                accGpsString += System.getProperty("line.separator");
                lineAdded = true;

                /** Now remove as many elements from the queues as our moving average step/shift
                    specifies and therefore enable new data points to come in.
                 */

                for(int i = 0; i < Constants.MVG_AVG_STEP; i++) {

                    accXQueue.remove();
                    accYQueue.remove();
                    accZQueue.remove();
                    //accQQueue.remove();

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
