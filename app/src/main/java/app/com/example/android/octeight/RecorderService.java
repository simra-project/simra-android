package app.com.example.android.octeight;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
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
    final short GPS_POLL_FREQUENCY = Constants.GPS_FREQUENCY;
    private SensorManager sensorManager = null;
    private PowerManager.WakeLock wakeLock = null;
    ExecutorService executor;
    Sensor accelerometer;
    float[] accelerometerMatrix = new float[3];

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Strings for storing data to enable continued use by other activities

    private String accString;
    private String gpsString;

    public String getGpsString() {
        return gpsString;
    }

    public String getAccString() {
        return accString;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private File accFile;
    private File gpsFile;

    LocationManager locationManager;
    Location lastLocation;
    int notificationId = 1337;
    NotificationManagerCompat notificationManager;

    Queue<Float> accXQueue;
    Queue<Float> accYQueue;
    Queue<Float> accZQueue;

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

        // Queues for storing acc data
        accXQueue = new LinkedList<>();
        accYQueue = new LinkedList<>();
        accZQueue = new LinkedList<>();

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


        Notification notification = createNotification().build();

        notificationManager = NotificationManagerCompat.from(this);
        // Send the notification.
        notificationManager.notify(notificationId, notification);
        startForeground(notificationId, notification);
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

        // Write String data to files
        executor.execute( () -> {
            try {
                        appendToFile(accString, accFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        executor.execute( () -> {
            try {
                appendToFile(gpsString, gpsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

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
                // Stop everything else once the task queue is clear
                stopForeground(true);
                wakeLock.release();

            }
        }).start();

        // Remove the Notification
        notificationManager.cancel(notificationId);

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

                gpsString += str += '\n';
            }

            // Record accelerometer data
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

            if((curTime - lastAccUpdate) >= ACC_POLL_FREQUENCY) {

            lastAccUpdate = curTime;

            /** Every average is computed over 30 data points, so we want the queues for the
                three accelerometer values to be of size 30 in order to compute the averages.

                Accordingly, when the queues are shorter we're adding data points.
             */

            if(accXQueue.size() < 30) {

                accXQueue.add(accelerometerMatrix[0]);
                accYQueue.add(accelerometerMatrix[1]);
                accZQueue.add(accelerometerMatrix[2]);

            } else {

                // The queues are of sufficient size, let's compute the averages.

                float xAvg = computeAverage(accXQueue);
                float yAvg = computeAverage(accYQueue);
                float zAvg = computeAverage(accZQueue);

                // Put the averages + time data into a string and append to file.

                String str = String.valueOf(xAvg) + ", " +
                        String.valueOf(yAvg) + ", " +
                        String.valueOf(zAvg) + ", " +
                        (curTime - startTime) + ", " +
                        (curTime - lastAccUpdate) + ", " +
                        DateFormat.getDateTimeInstance().format(new Date());

                accString += str += '\n';

                /** Now remove as many elements from the queues as our moving average step/shift
                    specifies and therefore enable new data points to come in.
                 */

                for(int i = 0; i < Constants.MVG_AVG_STEP; i++) {

                    accXQueue.remove();
                    accYQueue.remove();
                    accZQueue.remove();

                }

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
}
