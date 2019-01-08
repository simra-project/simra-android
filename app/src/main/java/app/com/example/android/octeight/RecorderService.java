package app.com.example.android.octeight;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RecorderService extends Service implements SensorEventListener, LocationListener {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Properties
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public static final String TAG = "RecorderService_LOG:";
    final short ACC_POLL_FREQUENCY = 20;
    private long lastAccUpdate = 0;
    private long lastGPSUpdate = 0;
    long curTime;
    long startTime;
    final short GPS_POLL_FREQUENCY = 3000;
    private SensorManager sensorManager = null;
    private PowerManager.WakeLock wakeLock = null;
    ExecutorService executor;
    Sensor accelerometer;
    float[] accelerometerMatrix = new float[3];
    private File accFile;
    private File gpsFile;
    LocationManager locationManager;
    Location lastLocation;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // SensorEventListener Methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public void onSensorChanged(SensorEvent event) {

        accelerometerMatrix = event.values;

        curTime = System.currentTimeMillis();

            // Write data to file in background thread
            try{
                Runnable insertHandler = new InsertHandler(accelerometerMatrix);
                executor.execute(insertHandler);
            } catch (Exception e) {
                Log.e(TAG, "insertData: " + e.getMessage(), e);
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
        Log.d(TAG, "GPSService onLocationChanged() fired ");
        lastLocation = location;
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

        // Prepare the accelerometer sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Request location updates
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,this);

        // Create files to write gps and accelerometer data
        try {
            String date = DateFormat.getDateTimeInstance().format(new Date())+".csv";
            accFile = getFileStreamPath("acc"+date);
            gpsFile = getFileStreamPath("gps"+date);
            accFile.createNewFile();
            appendToFile("X, Y, Z, curTime, diffTime, date", accFile);
            gpsFile.createNewFile();
            appendToFile("lon, lat, time, diff, date", gpsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":RecorderService");
        // Executor service for writing data
        executor = Executors.newSingleThreadExecutor();

    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        startForeground(Process.myPid(), new Notification());
        wakeLock.acquire();

        // Register Accelerometer sensor
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        // Unregister receiver and listener prior to gpsExecutor shutdown
        sensorManager.unregisterListener(this);

        // Stop requesting location updates
        locationManager.removeUpdates(this);

        // Prevent new tasks from being added to thread
        executor.shutdown();

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
                wakeLock.release();
                // Stop everything else once the task queue is clear
                stopForeground(true);

            }
        }).start();

    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void appendToFile(String str, File file) throws IOException {
        FileOutputStream writer = openFileOutput(file.getName(), MODE_APPEND);
        writer.write(str.getBytes());
        writer.write(System.getProperty("line.separator").getBytes());
        writer.flush();
        writer.close();
    }



    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Runnables
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    class InsertHandler implements Runnable {

        final float[] accelerometerMatrix;

        // Store the current sensor array values into THIS objects arrays, and db insert from this object
        public InsertHandler(float[] accelerometerMatrix) {

            this.accelerometerMatrix = accelerometerMatrix;
        }

        // Record location data
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        @SuppressLint("MissingPermission")
        public void run() {

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

                try {
                    appendToFile(str, gpsFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Record accelerometer data
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

            if((curTime - lastAccUpdate) >= ACC_POLL_FREQUENCY) {
            lastAccUpdate = curTime;
            String str = String.valueOf(accelerometerMatrix[0]) + ", " +
                    String.valueOf(accelerometerMatrix[1]) + ", " +
                    String.valueOf(accelerometerMatrix[2]) + ", " +
                    (curTime - startTime) + ", " +
                    (curTime - lastAccUpdate) + ", " +
                    DateFormat.getDateTimeInstance().format(new Date());

            try {
                appendToFile(str, accFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        }


    }

}
