package app.com.example.android.octeight;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.text.SimpleDateFormat;

public class Constants extends Service {

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

    public static final String SERVICE_URL = "http://vm1.mcc.tu-berlin.de:8080/resource/";

    public static final String UPLOAD_HASH_SUFFIX = "mcc_simra";

    public static final String APP_PATH = "/data/user/0/app.com.example.android.octeight/";


    public Constants() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
