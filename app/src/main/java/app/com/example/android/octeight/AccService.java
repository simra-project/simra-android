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

public class AccService extends Service implements SensorEventListener, LocationListener {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Properties
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public static final String TAG = "TAG:";
    public static final int SCREEN_OFF_RECEIVER_DELAY = 100;
    final short ACC_POLL_FREQUENCY = 20;
    private long lastAccUpdate = 0;
    private long lastGPSUpdate = 0;
    long curTime;
    long startTime;
    final short GPS_POLL_FREQUENCY = 3000;
    private SensorManager sensorManager = null;
    ExecutorService accExecutor;
    ExecutorService gpsExecutor;
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

        // only allow one update every ACC_POLL_FREQUENCY (convert from ms to nano for comparison).
        if((curTime - lastAccUpdate) >= ACC_POLL_FREQUENCY) {

            lastAccUpdate = curTime;

            //write data to file in background thread
            try{
                Runnable insertAccHandler = new InsertAccHandler(accelerometerMatrix);
                accExecutor.execute(insertAccHandler);
            } catch (Exception e) {
                Log.e(TAG, "insertData: " + e.getMessage(), e);
            }
        }
        if ((curTime - lastGPSUpdate) >= GPS_POLL_FREQUENCY){
            lastGPSUpdate = curTime;
            //write data to file in background thread
            try{
                Log.d(TAG, "AccService executing gpsExecutor");
                Runnable insertGPSHandler = new InsertGPSHandler();
                gpsExecutor.execute(insertGPSHandler);
            } catch (Exception e) {
                Log.e(TAG, "insertData: " + e.getMessage(), e);
            }
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
        Log.d(TAG, "GPSService onLocationChanged() fired ");
        lastLocation = location;

        // curTime = System.currentTimeMillis();
        Log.d(TAG, "GPSService curTime: " + curTime);
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

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,this);


        String date = DateFormat.getDateTimeInstance().format(new Date())+".csv";
        accFile = getFileStreamPath("acc"+date);
        gpsFile = getFileStreamPath("gps"+date);

        try {
            accFile.createNewFile();
            appendToFile("X, Y, Z, curTime, diffTime, date", accFile);
            gpsFile.createNewFile();
            appendToFile("lon, lat, time, diff, date", gpsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Executor service for DB inserts
        accExecutor = Executors.newSingleThreadExecutor();
        gpsExecutor = Executors.newSingleThreadExecutor();

    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        startForeground(Process.myPid(), new Notification());
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,this);

        /*
        //Message handler for progress dialog
        Bundle extras = intent.getExtras();
        messageHandler = (Messenger) extras.get("MESSENGER");
        */
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //Show dialog
        // sendMessage("SHOW");

        //Unregister receiver and listener prior to accExecutor shutdown
        sensorManager.unregisterListener(this);

        //stop requesting location updates
        locationManager.removeUpdates(this);

        //Prevent new tasks from being added to thread
        accExecutor.shutdown();
        gpsExecutor.shutdown();
        //Log.d(TAG, "AccExecutor shutdown is called");


        //Create new thread to wait for accExecutor to clear queue and wait for termination
        new Thread(new Runnable() {

            public void run() {
                try {
                    //Wait for all tasks to finish before we proceed
                    while ((!accExecutor.awaitTermination(1, TimeUnit.SECONDS))&&(!gpsExecutor.awaitTermination(1, TimeUnit.SECONDS))) {
                        Log.i(TAG, "Waiting for current tasks to finish");
                    }
                    Log.i(TAG, "No queue to clear");
                } catch (InterruptedException e) {
                    Log.e(TAG, "Exception caught while waiting for finishing accExecutor tasks");
                    accExecutor.shutdownNow();
                    gpsExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }

                if (accExecutor.isTerminated() && gpsExecutor.isTerminated()) {
                    //Stop everything else once the task queue is clear
                    stopForeground(true);

                    //Dismiss progress dialog
                    //sendMessage("HIDE");
                }
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
    class InsertAccHandler implements Runnable {

        final float[] accelerometerMatrix;

        //Store the current sensor array values into THIS objects arrays, and db insert from this object
        public InsertAccHandler(float[] accelerometerMatrix) {

            this.accelerometerMatrix = accelerometerMatrix;
        }

        public void run() {



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

    class InsertGPSHandler implements Runnable {


        //Store the current sensor array values into THIS objects arrays, and db insert from this object
        public InsertGPSHandler(){
        }

        @SuppressLint("MissingPermission")
        public void run() {


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
            Log.d(TAG, "GPSService InsertAccHandler run(): " + str);

            try {
                appendToFile(str, gpsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }



        }


    }
}
