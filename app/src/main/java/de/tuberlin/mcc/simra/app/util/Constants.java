package de.tuberlin.mcc.simra.app.util;

public class Constants {

    public static final int ZOOM_LEVEL = 19;

    // Accelerometer data gets recorded 50 times per second (every 20 millisecs).

    public static final int ACC_FREQUENCY = 20;

    // Step size for accelerometer moving average

    public static final int MVG_AVG_STEP = 5;

    // GPS-Trace: one recording every 3 secs = every 3000 millisecs.

    public static final int GPS_FREQUENCY = 3000;


    public static final String MCC_VM1 = "https://vm1.mcc.tu-berlin.de:8082/";
    public static final String MCC_VM2 = "https://vm2.mcc.tu-berlin.de:8082/";
    public static final String MCC_VM3 = "https://vm3.mcc.tu-berlin.de:8082/";

    // Every GPS fix has to be at least this accurate to be taken into account.
    public static final double GPS_ACCURACY_THRESHOLD = 30.0;

    // Backend Interface Version
    public static final int BACKEND_VERSION = 10;

    // Locales
    public static final String[] LOCALE_ABVS = {"UNKNOWN", "Berlin", "London", "other", "TEST"};


}
