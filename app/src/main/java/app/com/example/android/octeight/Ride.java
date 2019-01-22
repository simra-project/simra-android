package app.com.example.android.octeight;

import android.app.Activity;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
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
    String pathToAccGpsFile;
    String timeStamp;
    static String TAG = "Ride_LOG";
    Polyline route;

    public String getTimeStamp(){
        return this.timeStamp;
    }
    public Polyline getRoute(){ return this.route; }
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
    public Ride (File accGpsFile, String timeStamp, int state){
        // this.pathToAccGpsFile = pathToAccGpsFile;
        this.accGpsFile = accGpsFile;
        this.timeStamp = timeStamp;
        this.route = getRouteLine(accGpsFile);
        this.state = state;
    }

    private void updateEvents (){
        events = new LinkedList<AccEvent>() ;
        // search for Events in sensorData
        // add every Event to the List
    }

    // Takes a File which contains all the data and creates a
    // PolyLine to be displayed on the map as a route.
    // Maybe this should happen in RecorderService since we
    // already loop through all the lines there.
    // It is not good to do loop here through the data again.
    public static Polyline getRouteLine(File gpsFile){
        List<GeoPoint> geoPoints = new ArrayList<>();
        Polyline polyLine = new Polyline();

        try{
            BufferedReader br = new BufferedReader(new FileReader(gpsFile));
            // br.readLine() to skip the first line which contains the headers
            String line= br.readLine();

            while ((line = br.readLine()) != null) {

                try {
                    if((line.startsWith(",,"))){
                        continue;
                    }
                    String[] separatedLine = line.split(",");
                    GeoPoint actualGeoPoint = new GeoPoint(Double.valueOf(separatedLine[0]),Double.valueOf(separatedLine[1]));
                    polyLine.addPoint(actualGeoPoint);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            br.close();
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
        }
        return polyLine;
    }

    // ignore for now
    public static Polyline getRouteLine(String accGpsString, Boolean b){
        List<GeoPoint> geoPoints = new ArrayList<>();
        String[] gpsArray = accGpsString.split("\\n");
        GeoPoint actualGeoPoint = new GeoPoint(0.0, 0.0);

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
