package de.tuberlin.mcc.simra.app.annotation;


import android.util.Log;

import org.osmdroid.util.GeoPoint;

import java.util.Arrays;
import java.util.Date;

public class AccEvent {

    public long timeStamp;
    public GeoPoint position;
    String TAG = "AccEvent_LOG";
    public int key = 999;              // when an event doesn't have a key yet, the key is 999
    // (can't use 0 because that's an actual valid key)
    public String incidentType = "-1";
    public String scary = "0"; // default is non-scary
    public boolean annotated = false;  // events are labeled as not annotated when first created.

    /**
     * public void setKey(int key) {
     * <p>
     * this.key = key;
     * <p>
     * }
     * <p>
     * public void setAnnotationStatus(boolean annotated) {
     * this.annotated = annotated;
     * }
     * <p>
     * public void setAnnotationStatus(boolean annotated) {
     * this.annotated = annotated;
     * }
     */


    public long getTimeStamp() {
        return this.timeStamp;
    }

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

    public AccEvent(int key, double lat, double lon, long timeStamp, boolean annotated, String incidentType, String scary) {
        this.key = key;
        this.position = new GeoPoint(lat, lon);
        this.timeStamp = timeStamp;
        this.annotated = annotated;
        this.incidentType = incidentType;
        this.scary = scary;
        Log.d(TAG, "AccEvent " + this.key + " constructor incidentType: " + this.incidentType + " scary: " + this.scary);

    }

}
