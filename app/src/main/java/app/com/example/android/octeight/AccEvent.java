package app.com.example.android.octeight;

import android.util.Log;

import org.osmdroid.util.GeoPoint;

import java.util.Date;

public class AccEvent {
    long timeStamp;
    GeoPoint position;
    Date date;
    String TAG = "AccEvent_LOG";

    public long getTimeStamp(){ return this.timeStamp; }


    public AccEvent(String[] eventLine) {
        this.position = new GeoPoint(Double.valueOf(eventLine[0]), Double.valueOf(eventLine[1]));
        this.timeStamp = Long.valueOf(eventLine[5]);
    }


}
