package de.tuberlin.mcc.simra.app.util;

public class Constants {

    public static final int ZOOM_LEVEL = 19;

    // Accelerometer data gets recorded 50 times per second (every 20 millisecs).
    public static final int ACCELEROMETER_FREQUENCY = 20;

    /**
     * Step size for accelerometer moving average
     */
    public static final int MVG_AVG_STEP = 5;

    /**
     * GPS-Trace: one recording every 3 secs = every 3000 millisecs.
     */
    public static final int GPS_FREQUENCY = 3000;

    /**
     * Every GPS fix has to be at least this accurate to be taken into account.
     */
    public static final double GPS_ACCURACY_THRESHOLD = 30.0;

    public static final String PROFILE_HEADER = "birth,gender,region,experience,numberOfRides,duration,numberOfIncidents,waitedTime,distance,co2,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,behaviour,numberOfScary" + System.lineSeparator();
}
