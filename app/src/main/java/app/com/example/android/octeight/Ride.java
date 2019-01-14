package app.com.example.android.octeight;

import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;

public class Ride {

    static int nextID = 0;
    final int id = nextID++;
    Date date;
    GeoPoint start;
    GeoPoint finish;
    int duration;
    int user;
    File sensor;
    LinkedList<AccEvent> events;
    String accString;
    String gpsString;
    String timeStamp;

    public Ride (int user, File sensorData){
        user = this.user;
        sensor = sensorData;
        date =  new Date();
        // start = get first GeoPoint in sensorData
        // finish = get last GeoPoint in sensorData
        // duration = last timestamp - first timestamp
        updateEvents();
    }

    public Ride (String accString, String gpsString, String timeStamp){
        this.accString = accString;
        this.gpsString = gpsString;
        this.timeStamp = timeStamp;

    }

    private void updateEvents (){
        events = new LinkedList<AccEvent>() ;
        // search for Events in sensorData
        // add every Event to the List
    }
}
