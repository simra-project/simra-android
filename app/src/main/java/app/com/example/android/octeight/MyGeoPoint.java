package app.com.example.android.octeight;

import org.osmdroid.util.GeoPoint;

import java.sql.Timestamp;

public class MyGeoPoint  {

    double lat;

    double lon;

    long timeDiff = 0L;

    Timestamp timeStamp = null;

    // Providing two constructors because we agreed to record timestamp only every minute but
    // latitude, longitude and difference between recording time and time we first started recording
    // every 3 seconds.

    public MyGeoPoint(double lat, double lon, long timeDiff) {
        this.lat = lat;
        this.lon =  lon;
        this.timeDiff = timeDiff;
    }

    public MyGeoPoint(double lat, double lon, long timeDiff, Timestamp timeStamp) {
        this.lat = lat;
        this.lon =  lon;
        this.timeDiff = timeDiff;
        this.timeStamp = timeStamp;
    }

}
