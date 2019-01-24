package app.com.example.android.octeight;

import android.app.Activity;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
    ArrayList<AccEvent> events;
    public ArrayList<AccEvent> getEvents() { return events; }
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
    ArrayList<Marker> incidents;
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
        this.events = findAccEvents();
        incidents = new ArrayList<Marker>();
    }

    private void updateEvents (){
        events = new ArrayList<AccEvent>() ;
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

    public ArrayList<AccEvent> findAccEvents() {


        BufferedReader br = null;
        String thisLine = null;
        String nextLine = null;
        try {
            br = new BufferedReader(new FileReader(accGpsFile));
            br.readLine();
            thisLine = br.readLine();
            nextLine = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }


        String[] partOfRide;
        ArrayList<String[]> ride = new ArrayList<>();
        boolean newSubPart = false;
        while ( (thisLine != null )&&(!newSubPart)) {
            System.out.println("outer thisLine: " + thisLine);
            System.out.println("outer nextLine: " + nextLine);

            String[] currentLine = thisLine.split(",");
            partOfRide = new String[6];
            String lat = currentLine[0];
            String lon = currentLine[1];
            String date = currentLine[7];
            partOfRide[0] = lat; // lat
            partOfRide[1] = lon; // lon
            partOfRide[2] = "0"; // maxXDelta
            partOfRide[3] = "0"; // maxYDelta
            partOfRide[4] = "0"; // maxZDelta
            partOfRide[5] = date; // date

            double maxX = Double.valueOf(currentLine[2]);
            double minX = Double.valueOf(currentLine[2]);
            double maxY = Double.valueOf(currentLine[3]);
            double minY = Double.valueOf(currentLine[3]);
            double maxZ = Double.valueOf(currentLine[4]);
            double minZ = Double.valueOf(currentLine[4]);
            thisLine = nextLine;
            try {
                nextLine = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(thisLine.startsWith(",,")){
                newSubPart = true;
            }

            while ((thisLine!= null) && newSubPart) {
                System.out.println("inner thisLine: " + thisLine);
                System.out.println("inner nextLine: " + nextLine);

                currentLine = thisLine.split(",");
                if (Double.valueOf(currentLine[2]) >= maxX){
                    maxX = Double.valueOf(currentLine[2]);
                } else if (Double.valueOf(currentLine[2]) < minX){
                    minX =  Double.valueOf(currentLine[2]);
                }
                if (Double.valueOf(currentLine[3]) >= maxY){
                    maxY = Double.valueOf(currentLine[3]);
                } else if (Double.valueOf(currentLine[3]) < minY){
                    minY =  Double.valueOf(currentLine[3]);
                }
                if (Double.valueOf(currentLine[4]) >= maxZ){
                    maxZ = Double.valueOf(currentLine[4]);
                } else if (Double.valueOf(currentLine[4]) < minZ){
                    minZ =  Double.valueOf(currentLine[4]);
                }
                thisLine = nextLine;
                try {
                    nextLine = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(thisLine == nextLine);
                if(thisLine != null && !thisLine.startsWith(",,")){
                    newSubPart = false;
                }
            }


            double maxXDelta = Math.abs(maxX - minX);
            double maxYDelta = Math.abs(maxY - minY);
            double maxZDelta = Math.abs(maxZ - minZ);

            partOfRide[2] = String.valueOf(maxXDelta);
            partOfRide[3] = String.valueOf(maxYDelta);
            partOfRide[4] = String.valueOf(maxZDelta);


            ride.add(partOfRide);
            if(nextLine == null){
                break;
            }
        }

        ArrayList<String[]> output = new ArrayList<>();

        double oldXAverage = Double.valueOf((ride.get(0))[2]) / Double.valueOf((ride.get(1))[2]);
        double oldYAverage = Double.valueOf((ride.get(0))[3]) / Double.valueOf((ride.get(1))[3]);
        double oldZAverage = Double.valueOf((ride.get(0))[4]) / Double.valueOf((ride.get(1))[4]);
        int newSize = 2;


        for (int i = 2; i < ride.size() ; i++) {
            newSize++;
            double actualDeltaX = Double.valueOf(ride.get(i)[2]);
            double actualDeltaY = Double.valueOf(ride.get(i)[3]);
            double actualDeltaZ = Double.valueOf(ride.get(i)[4]);
            double newAverageDeltaX = (oldXAverage * (newSize - 1) + actualDeltaX)/newSize;
            double newAverageDeltaY = (oldYAverage * (newSize - 1) + actualDeltaY)/newSize;
            double newAverageDeltaZ = (oldZAverage * (newSize - 1) + actualDeltaZ)/newSize;
            if(actualDeltaX > 3 * newAverageDeltaX || actualDeltaY > 3 * newAverageDeltaY || actualDeltaZ > 3 * newAverageDeltaZ){
                output.add(ride.get(i));
            }
            oldXAverage = newAverageDeltaX;
            oldYAverage = newAverageDeltaY;
            oldZAverage = newAverageDeltaZ;
        }

        ArrayList<AccEvent> accEvents = new ArrayList<>();
        Collections.sort(ride, new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                return Double.valueOf(o1[2]).compareTo(Double.valueOf(o2[2]));
            }
        });
        try{
        for (int i = ride.size()-1; i > ride.size()-6; i--){
            accEvents.add(new AccEvent(ride.get(i)));
            Log.d(TAG, "sorted: " + ride.get(i)[3]);
        }} catch(Exception e) {
            e.printStackTrace();
        }
        /*
        for (int i = 0; i < ride.size() ; i++) {
            accEvents.add(new AccEvent(ride.get(i)));
        }*/

        Log.d(TAG, "findAccEvents(): " + Arrays.deepToString(ride.toArray()));
        return accEvents;

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
