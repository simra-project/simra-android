package app.com.example.android.octeight;

import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;

public class AccEvent {
    GeoPoint position;
    Date date;
    File sensorData;

    public AccEvent(GeoPoint position, Date date, File sensorData) {
        this.position = position;
        this.date = date;
        this.sensorData = sensorData;
    }
    /*LinkedList<Float> accX;
    LinkedList<Float> accY;
    LinkedList<Float> accZ;

    LinkedList<GeoPoint> routeSnap;*/

}
