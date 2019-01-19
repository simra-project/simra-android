package app.com.example.android.octeight;

import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Ride {

    static int nextID = 0;
    final int id = nextID++;
    Date date;
    GeoPoint start;
    GeoPoint finish;
    int duration;
    int user;
    File accGpsFile;
    LinkedList<AccEvent> events;
    String accString;
    String gpsString;
    String accGpsString;
    String timeStamp;
    static String TAG = "Ride_LOG";
    Polyline route;
    Marker[] incidents;
    int state;
    final int OFFLINE = 0;
    final int PENDING = 1;
    final int READY = 2;



    public Ride (int user, File sensorData){
        user = this.user;
        accGpsFile = sensorData;
        date =  new Date();
        // start = get first GeoPoint in sensorData
        // finish = get last GeoPoint in sensorData
        // duration = last timestamp - first timestamp
        updateEvents();
    }

    // This is the constructor that is used for now.
    public Ride (String accGpsString, String timeStamp, int state){
        this.accGpsString = accGpsString;
        this.timeStamp = timeStamp;
        this.route = getRouteLine(accGpsString);
        this.state = state;
    }

    private void updateEvents (){
        events = new LinkedList<AccEvent>() ;
        // search for Events in sensorData
        // add every Event to the List
    }

    // Takes a String which contains all the data and creates a
    // PolyLine to be displayed on the map as a route.
    // Maybe this should happen in RecorderService since we
    // already loop through all the lines there.
    // It is not good to do loop here through the data again.
    public static Polyline getRouteLine(String accGpsString){
        List<GeoPoint> geoPoints = new ArrayList<>();
        String[] gpsArray = accGpsString.split("\\n");
        GeoPoint actualGeoPoint = new GeoPoint(0.0, 0.0);
        Queue<Float> accXQueue;
        Queue<Float> accYQueue;
        Queue<Float> accZQueue;

        for (int i = 0; i < gpsArray.length; i++){
            String actualLine = gpsArray[i];
            try {
                if((actualLine.startsWith(",,"))||(actualLine.split(",").length<8)){
                    continue;
                }
                String[] line = gpsArray[i].split(",");
                actualGeoPoint.setLatitude(Double.valueOf(line[0]));
                actualGeoPoint.setLongitude(Double.valueOf(line[1]));
                geoPoints.add(actualGeoPoint);

            } catch ( Exception e) {
                e.printStackTrace();
            }
        }
        Polyline line = new Polyline();
        line.setPoints(geoPoints);
        return line;
    }

    // ignore for now
    public static Polyline getRouteLine(String accGpsString, Boolean b){
        List<GeoPoint> geoPoints = new ArrayList<>();
        String[] gpsArray = accGpsString.split("\\n");
        GeoPoint actualGeoPoint = new GeoPoint(0.0, 0.0);
        Queue<Float> accQQueue;

        for (int i = 0; i < gpsArray.length; i++){
            String actualLine = gpsArray[i];
            try {
                if((actualLine.startsWith(",,"))||(actualLine.split(",").length<8)){
                    continue;
                }
                String[] line = gpsArray[i].split(",");
                actualGeoPoint.setLatitude(Double.valueOf(line[0]));
                actualGeoPoint.setLongitude(Double.valueOf(line[1]));
                geoPoints.add(actualGeoPoint);

            } catch ( Exception e) {
                e.printStackTrace();
            }
        }
        Polyline line = new Polyline();
        line.setPoints(geoPoints);
        return line;
    }

    public String getTimeStamp(){
        return this.timeStamp;
    }
    public Polyline getRoute(){ return this.route; }

    // The idea was to split the route to multiple parts of 3 seconds and than analyse
    // whether there was a probable incident. Debatable.
    public class RoutePart {
        Queue<Float> accXQueue;
        Queue<Float> accYQueue;
        Queue<Float> accZQueue;
        GeoPoint gps;
        String timeStamp;
        double averageX;
        double averageY;
        double averageZ;

        public RoutePart(Queue<Float> accXQueue, Queue<Float> accYQueue, Queue<Float> accZQueue, String timeStamp){
            this.accXQueue = accXQueue;
            this.accYQueue = accYQueue;
            this.accZQueue = accZQueue;
            this.timeStamp = timeStamp;

            computeAverage(accXQueue);

        }

        private double computeAverage(Collection<Float> myVals) {

            double sum = 0;

            for(double f : myVals) {

                sum += f;

            }

            return sum/myVals.size();

        }



    }

    // The Idea was that each Ride (or RoutePart) has a couple of Incident objects. Debatable.
    public class Incident {
        Polyline incidentRoute;
        String timestamp;
        Ride owner;
        int type;

        public Incident(Polyline incidentRoute, String timestamp, Ride owner){
            this.incidentRoute = incidentRoute;
            this.timestamp = timestamp;
            this.owner = owner;
        }

    }

}
