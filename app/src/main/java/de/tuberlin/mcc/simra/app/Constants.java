package de.tuberlin.mcc.simra.app;

import java.text.SimpleDateFormat;

public class Constants {

    public static final int ZOOM_LEVEL = 19;

    // Accelerometer data gets recorded 50 times per second (every 20 millisecs).

    public static final int ACC_FREQUENCY = 20;

    // Step size for accelerometer moving average

    public static final int MVG_AVG_STEP = 5;

    // GPS-Trace: one recording every 3 secs = every 3000 millisecs.

    public static final int GPS_FREQUENCY = 3000;

    // TimeStamp for GPS: to be recorded every minute = every 60000 millisecs.

    public static final long GPS_TIME_FREQUENCY = 60000;

    // The minimal duration of a ride in milliseconds
    public static final int MINIMAL_RIDE_DURATION = 30000;

    public static final SimpleDateFormat DATE_PATTERN_SHORT = new SimpleDateFormat("dd.MM.yyyy");

    public static final String MCC_VM1 = "https://vm1.mcc.tu-berlin.de:8082/";
    public static final String MCC_VM2 = "https://vm2.mcc.tu-berlin.de:8082/";

    public static final String MCC_VM3 = "https://vm3.mcc.tu-berlin.de:8082/";

    public static final String UPLOAD_HASH_SUFFIX = "mcc_simra";

    public static final String APP_PATH = "/data/user/0/de.tuberlin.mcc.simra.app/";

    // Every GPS fix has to be at least this accurate to be taken into account.
    public static final double GPS_ACCURACY_THRESHOLD = 30.0;

    // Backend Interface Version
    public static final int BACKEND_VERSION = 9;

    // Locales
    public static final String[] LOCALE_ABVS = {"UNKNOWN", "Berlin", "London", "other", "TEST"};


}
