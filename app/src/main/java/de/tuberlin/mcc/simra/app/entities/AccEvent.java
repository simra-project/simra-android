package de.tuberlin.mcc.simra.app.entities;


import org.osmdroid.util.GeoPoint;

/**
 * @deprecated Use IncidentLogEntry instead
 */
public class AccEvent {

    public long timeStamp;
    public GeoPoint position;
    public int key = 999;              // when an event doesn't have a key yet, the key is 999
    // (can't use 0 because that's an actual valid key)
    public String incidentType = "-1";
    public String scary = "0"; // default is non-scary
    public boolean annotated = false;  // events are labeled as not annotated when first created.
    String TAG = "AccEvent_LOG";

    public AccEvent(int key, double lat, double lon, long timeStamp, boolean annotated, String incidentType, String scary) {
        this.key = key;
        this.position = new GeoPoint(lat, lon);
        this.timeStamp = timeStamp;
        this.annotated = annotated;
        this.incidentType = incidentType;
        this.scary = scary;
    }


    public long getTimeStamp() {
        return this.timeStamp;
    }
}
