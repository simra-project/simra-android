package app.com.example.android.octeight;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.SQLException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GPSService extends Service implements LocationListener {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Properties
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public static final String TAG = "TAG:";
    final short GPS_POLL_FREQUENCY = 3000;
    private long lastUpdate = 0;
    long curTime;
    long startTime;
    private File gpsFile;
    ExecutorService executor;
    LocationManager locationManager;
    Location lastLocation;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // SensorEventListener Methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "GPSService onLocationChanged() fired ");
        lastLocation = location;

        curTime = System.currentTimeMillis();
        Log.d(TAG, "GPSService curTime: " + curTime);
        if ((curTime - lastUpdate) >= GPS_POLL_FREQUENCY){
            lastUpdate = curTime;

            try {
                Runnable insertHandler = new GPSService.InsertHandler();
                executor.execute(insertHandler);
            } catch (Exception e) {
                Log.e(TAG, "insertData: " + e.getMessage(), e);
            }
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
        Log.d(TAG, "GPSService onCreate()");
        startTime = System.currentTimeMillis();
        super.onCreate();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,this);

        String date = DateFormat.getDateTimeInstance().format(new Date())+".csv";
        gpsFile = getFileStreamPath("gps"+date);

        try {
            gpsFile.createNewFile();
            appendToFile("lon, lat, time, diff, date", gpsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Executor service for DB inserts
        executor = Executors.newSingleThreadExecutor();
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "GPSService onStartCommand()");


        startForeground(Process.myPid(), new Notification());
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,this);


        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        //stop requesting location updates
        locationManager.removeUpdates(this);

        //Prevent new tasks from being added to thread
        executor.shutdown();
        Log.d(TAG, "GPSExecutor shutdown is called");

        //Create new thread to wait for accExecutor to clear queue and wait for termination
        new Thread(new Runnable() {

            public void run() {
                try {
                    //Wait for all tasks to finish before we proceed
                    while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                        Log.i(TAG, "Waiting for current tasks to finish");
                    }
                    Log.i(TAG, "No queue to clear");
                } catch (InterruptedException e) {
                    Log.e(TAG, "Exception caught while waiting for finishing accExecutor tasks");
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }

                if (executor.isTerminated()) {
                    //Stop everything else once the task queue is clear
                    stopForeground(true);
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
    class InsertHandler implements Runnable {


        //Store the current sensor array values into THIS objects arrays, and db insert from this object
        public InsertHandler(){
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
                    (curTime - lastUpdate) + ", " +
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
