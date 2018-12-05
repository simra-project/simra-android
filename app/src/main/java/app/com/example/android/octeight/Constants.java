package app.com.example.android.octeight;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class Constants extends Service {

    // Accelerometer data gets recorded 50 times per second (every 50000 millisecs).

    public static final long ACC_FREQUENCY = 50000;

    // GPS-Trace: one recording every 3 secs = every 3000 millisecs.

    public static final long GPS_FREQUENCY = 3000;

    // TimeStamp for GPS: to be recorded every minute = every 60000 millisecs.

    public static final long GPS_TIME_FREQUENCY = 60000;

    public Constants() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
