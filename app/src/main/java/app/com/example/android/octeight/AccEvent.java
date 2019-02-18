package app.com.example.android.octeight;

import android.util.Log;

import org.osmdroid.util.GeoPoint;

import java.util.Arrays;
import java.util.Date;

public class AccEvent {

    long timeStamp;
    GeoPoint position;
    Date date;
    String TAG = "AccEvent_LOG";
    int key = 999;              // when an event doesn't have a key yet, the key is 999
                                // (can't use 0 because that's an actual valid key)

    boolean annotated = false;  // events are labeled as not annotated when first created.

    /**public void setKey(int key) {

        this.key = key;

    }

    public void setAnnotationStatus(boolean annotated) {
        this.annotated = annotated;
    }

    public void setAnnotationStatus(boolean annotated) {
        this.annotated = annotated;
    }*/



    public long getTimeStamp(){ return this.timeStamp; }

    public AccEvent(String[] eventLine) {
        this.position = new GeoPoint(Double.valueOf(eventLine[0]), Double.valueOf(eventLine[1]));
        this.timeStamp = Long.valueOf(eventLine[5]);
    }

    public AccEvent(int key, double lat, double lon, long timeStamp, boolean annotated) {
        this.key = key;
        this.position = new GeoPoint(lat, lon);
        this.timeStamp = timeStamp;
        this.annotated = annotated;
    }

}
