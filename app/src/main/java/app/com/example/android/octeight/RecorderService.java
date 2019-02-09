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
import android.location.Criteria;
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
import android.support.v4.content.LocalBroadcastManager;
import android.util.FloatMath;
import android.util.Log;

import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RecorderService extends Service implements SensorEventListener, LocationListener {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Properties
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public static final String TAG = "RecorderService_LOG:";
    final int ACC_POLL_FREQUENCY = Constants.ACC_FREQUENCY;
    private long lastAccUpdate = 0;
    //private long lastGPSUpdate = 0;
    private long lastGPSUpdate = 0;

    long curTime;
    long startTime;
    long endTime;
    final int GPS_POLL_FREQUENCY = Constants.GPS_FREQUENCY;
    private SensorManager sensorManager = null;
    private PowerManager.WakeLock wakeLock = null;
    ExecutorService executor;
    Sensor accelerometer;
    float[] accelerometerMatrix = new float[3];
    private IBinder mBinder = new MyBinder();
    String pathToAccGpsFile = "";

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Strings for storing data to enable continued use by other activities

    private String accString = "";
    private String gpsString = "";
    private String accGpsString = "";

    public String getGpsString() {
        return gpsString;
    }
    public String getAccString() {
        return accString;
    }
    public String getAccGpsString() { return accGpsString; }
    public String getPathToAccGpsFile() { return pathToAccGpsFile; }
    public double getDuration() { return (curTime - startTime); }
    public long getTimeStamp() { return curTime; }
    public long getEndTime() { return endTime; }
    public long getStartTime() { return startTime; }
    public String mAcceleration = "";

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // The ride object which will be created and retrieved by the MainActivity at the end of the
    // service
    public Ride getRide() {
        File accGpsFile = getFileStreamPath(pathToAccGpsFile);
        return new Ride(accGpsFile, String.valueOf(startTime), String.valueOf((curTime - startTime)), 0, this);
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // The files which contain accString, gpsString and accGpsString
    // private File accFile;
    // private File gpsFile;
    private File accGpsFile;
    private File metaDataFile;

    LocationManager locationManager;
    Location lastLocation;
    String recordedAccData = "";
    String recordedGPSData = "";

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // For Managing the notification shown while the service is running
    int notificationId = 1337;
    NotificationManagerCompat notificationManager;

    Queue<Float> accXQueue;
    Queue<Float> accYQueue;
    Queue<Float> accZQueue;
    // Queue<Float> accQQueue;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // SharedPrefs (same as in MainActivity) to enable continuously increasing unique
    // code for each ride => connection between meta file and individual ride files
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    SharedPreferences sharedPrefs;

    SharedPreferences.Editor editor;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // SensorEventListener Methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public void onSensorChanged(SensorEvent event) {

        accelerometerMatrix = event.values;

        curTime = System.currentTimeMillis();

        if((curTime - lastAccUpdate) >= ACC_POLL_FREQUENCY) {

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
        if (location.getAccuracy() < 20.0){
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
        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,this);

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

        pathToAccGpsFile = sharedPrefs.getInt("RIDE-KEY", 0)
                + "_accGps_"
                + startTime +/*date +*/ ".csv";


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

        Notification notification = createNotification().build();

        notificationManager = NotificationManagerCompat.from(this);
        // Send the notification.
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

        if((curTime - startTime) > Constants.MINIMAL_RIDE_DURATION) {



            // Create files to write gps and accelerometer data
            try {

                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                // COMPLETE DATA FILE (one per ride)

                //accFile = getFileStreamPath("acc"+date + ".csv");
                //gpsFile = getFileStreamPath("gps"+date + ".csv");

                accGpsFile = getFileStreamPath(pathToAccGpsFile);
                //accFile.createNewFile();
                //appendToFile("X,Y,Z,curTime,diffTime,date", accFile);
                //gpsFile.createNewFile();
                //appendToFile("lat,lon,time,diff,date", gpsFile);
                accGpsFile.createNewFile();
                appendToFile("lat,lon,X,Y,Z,timeStamp"+System.lineSeparator(), accGpsFile);

                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                // META-FILE (one per user): contains ...
                // * the information required to display rides in the ride history (DATE,
                //   DURATION, ANNOTATED YES/NO)
                // * the RIDE KEY which allows to identify the file containing the complete data for
                //   a ride. => Use case: user wants to view a ride from history - retrieve data
                // * one meta file per user, so we only want to create it if it doesn't exist yet.
                //   (fileExists is a custom method, can be found at the very bottom of this class)

                if(!fileExists("metaData.csv")) {

                    metaDataFile = getFileStreamPath("metaData.csv");

                    metaDataFile.createNewFile();

                    appendToFile("key, startTime, endTime, annotated"
                            +System.lineSeparator(), metaDataFile);

                } else {

                    metaDataFile = getFileStreamPath("metaData.csv");

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            // Write String data to files
            try {
                appendToFile(accGpsString, accGpsFile);
                appendToFile(String.valueOf(sharedPrefs.getInt("RIDE-KEY",0)) + ","
                        + String.valueOf(startTime) + "," + String.valueOf(endTime) + ","
                        + "0" + System.lineSeparator(), metaDataFile);
            } catch (IOException e) {
                Log.d(TAG, "Error while writing the file: " + e.getMessage());
                e.printStackTrace();
            }
            // Log.d(TAG, "onDestroy() accGpsString successfully written");

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

    private void appendToFile(String str, File file) throws IOException {
        FileOutputStream writer = openFileOutput(file.getName(), MODE_APPEND);
        writer.write(str.getBytes());
        writer.flush();
        writer.close();
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

    private float computeAverage(Collection<Float> myVals) {

        float sum = 0;

        for(float f : myVals) {

            sum += f;

        }

        return sum/myVals.size();

    }

    private NotificationCompat.Builder createNotification() {
        String CHANNEL_ID = "RecorderServiceNotification";
        /*
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        */
        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, contentIntent, 0);
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_nameDE);
            String description = getString(R.string.channel_descriptionDE);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.helmet)
                .setContentTitle("Aufzeichnung der Fahrt")
                .setContentText("Ihre Fahrt wird aufgezeichnet.")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent);

        return mBuilder;

    }

    public class MyBinder extends Binder {
        RecorderService getService() {
            return RecorderService.this;
        }
    }

    public boolean fileExists(String fname){
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

}
