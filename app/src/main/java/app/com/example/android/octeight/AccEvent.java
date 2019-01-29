package app.com.example.android.octeight;

import android.util.Log;

import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

public class AccEvent {
    long timeStamp;
    GeoPoint position;
    Date date;
    File sensorData;
    String TAG = "AccEvent_LOG";

    public AccEvent(GeoPoint position, Date date, File sensorData) {
        this.position = position;
        this.date = date;
        this.sensorData = sensorData;
    }

    public AccEvent(String[] eventLine){
        this.position = new GeoPoint(Double.valueOf(eventLine[0]), Double.valueOf(eventLine[1]));
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        // Log.d(TAG, "eventLine: " + Arrays.toString(eventLine));
        //String dateString = format.format(new Date());
        this.timeStamp = Long.valueOf(eventLine[5]);
        try {
            this.date = format.parse ( eventLine[7] );
            // Log.d(TAG, "this.date: " + this.date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    /*LinkedList<Float> accX;
    LinkedList<Float> accY;
    LinkedList<Float> accZ;

    LinkedList<GeoPoint> routeSnap;*/

}
