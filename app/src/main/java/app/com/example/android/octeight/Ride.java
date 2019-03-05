package app.com.example.android.octeight;

import android.content.Context;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static app.com.example.android.octeight.Utils.appendToFile;
import static app.com.example.android.octeight.Utils.fileExists;
import static app.com.example.android.octeight.Utils.lookUpIntSharedPrefs;

public class Ride {

    private String id = "";

    public String getId() {
        return id;
    }

    File accGpsFile;
    ArrayList<AccEvent> events;

    public ArrayList<AccEvent> getEvents() {
        return events;
    }

    String duration;
    String startTime;
    Context context;
    static String TAG = "Ride_LOG";
    Polyline route;

    int bike;
    int child;
    int trailer;
    int pLoc;

    public String getDuration() {
        return duration;
    }

    public Polyline getRoute() {
        return this.route;
    }

    int state;
    final int ANNOTATION_NOT_STARTED = 0;
    final int ANNOTATION_NOT_FINISHED = 1;
    final int ANNOTATION_FINISHED = 2;


    // This is the constructor that is used for now.
    public Ride(File accGpsFile, String duration, String startTime, /*String date,*/ int state, int bike, int child, int trailer, int pLoc, Context context) {
        this.accGpsFile = accGpsFile;
        this.duration = duration;
        this.startTime = startTime;
        this.route = getRouteLine(accGpsFile);
        this.state = state;
        this.bike = bike;
        this.child = child;
        this.trailer = trailer;
        this.pLoc = pLoc;
        this.events = findAccEvents();
        this.context = context;
        String prefix = "/data/user/0/app.com.example.android.octeight/files/";
        String path = accGpsFile.getPath().replace(prefix, "");
        this.id = path.split("_")[0];

        String pathToAccEventsOfRide = "accEvents" + id + ".csv";
        String content = "key,lat,lon,ts,bike,child,trailer,pLoc,incident,i1,i2,i3,i4,i5,i6,i7,i8,i9,scary,desc";
        content += System.lineSeparator();

        if (!fileExists(pathToAccEventsOfRide, context)) {

            for (int i = 0; i < events.size(); i++) {

                AccEvent actualAccEvent = events.get(i);

                // Forwarding the key to the accEvents; from the accEvents, we forward them to
                // the Info-Windows and from there to IncidentPopUpActivity.
                //actualAccEvent.setKey(i);
                // @TODO do AccEvents have to have a key here???

                content += i + "," + actualAccEvent.position.getLatitude() + "," + actualAccEvent.position.getLongitude() + "," + actualAccEvent.timeStamp + "," + bike + "," + child + "," + trailer + "," + pLoc + ",,,,,,,,,,,," + System.lineSeparator();
            }

            appendToFile(content, pathToAccEventsOfRide, context);
        }

    }


    // Takes a File which contains all the data and creates a
    // PolyLine to be displayed on the map as a route.
    // Maybe this should happen in RecorderService since we
    // already loop through all the lines there.
    // It is not good to do loop here through the data again.
    public static Polyline getRouteLine(File gpsFile) {
        List<GeoPoint> geoPoints = new ArrayList<>();
        Polyline polyLine = new Polyline();

        try {
            BufferedReader br = new BufferedReader(new FileReader(gpsFile));
            // br.readLine() to skip the first line which contains the headers
            String line = br.readLine();

            while ((line = br.readLine()) != null) {

                try {
                    if ((line.startsWith(",,"))) {
                        continue;
                    }
                    String[] separatedLine = line.split(",");
                    GeoPoint actualGeoPoint = new GeoPoint(Double.valueOf(separatedLine[0]), Double.valueOf(separatedLine[1]));
                    polyLine.addPoint(actualGeoPoint);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return polyLine;
    }

    public ArrayList<AccEvent> findAccEvents() {

/*
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


        Log.d(TAG, "findAccEvents(): " + Arrays.deepToString(ride.toArray()));
        return accEvents;
        */

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
        // Each String[] in ride is a part of the ride which is ca. 3 seconds long.
        ArrayList<String[]> ride = new ArrayList<>();
        ArrayList<AccEvent> accEvents = new ArrayList<>(6);
        ArrayList<String[]> events = new ArrayList<>(6);

        accEvents.add(new AccEvent(thisLine.split(",")));
        accEvents.add(new AccEvent(thisLine.split(",")));
        accEvents.add(new AccEvent(thisLine.split(",")));
        accEvents.add(new AccEvent(thisLine.split(",")));
        accEvents.add(new AccEvent(thisLine.split(",")));
        accEvents.add(new AccEvent(thisLine.split(",")));

        String[] template = {"0.0", "0.0", "0.0", "0.0", "0.0", "0"};
        events.add(template);
        events.add(template);
        events.add(template);
        events.add(template);
        events.add(template);
        events.add(template);

        boolean newSubPart = false;
        // Loops through all lines. If the line starts with lat and lon, it is consolidated into
        // a part of a ride together with the subsequent lines that don't have lat and lon.
        // Then, it takes the top two X-, Y- and Z-deltas and creates AccEvents from them.
        while ((thisLine != null) && (!newSubPart)) {
            String[] currentLine = thisLine.split(",");
            // currentLine: {lat, lon, maxXDelta, maxYDelta, maxZDelta, timeStamp}
            partOfRide = new String[6];
            String lat = currentLine[0];
            String lon = currentLine[1];
            String timeStamp = currentLine[5];

            partOfRide[0] = lat; // lat
            partOfRide[1] = lon; // lon
            partOfRide[2] = "0"; // maxXDelta
            partOfRide[3] = "0"; // maxYDelta
            partOfRide[4] = "0"; // maxZDelta
            partOfRide[5] = timeStamp; // timeStamp

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
            if (thisLine != null && thisLine.startsWith(",,")) {
                newSubPart = true;
            }

            while ((thisLine != null) && newSubPart) {

                currentLine = thisLine.split(",");
                if (Double.valueOf(currentLine[2]) >= maxX) {
                    maxX = Double.valueOf(currentLine[2]);
                } else if (Double.valueOf(currentLine[2]) < minX) {
                    minX = Double.valueOf(currentLine[2]);
                }
                if (Double.valueOf(currentLine[3]) >= maxY) {
                    maxY = Double.valueOf(currentLine[3]);
                } else if (Double.valueOf(currentLine[3]) < minY) {
                    minY = Double.valueOf(currentLine[3]);
                }
                if (Double.valueOf(currentLine[4]) >= maxZ) {
                    maxZ = Double.valueOf(currentLine[4]);
                } else if (Double.valueOf(currentLine[4]) < minZ) {
                    minZ = Double.valueOf(currentLine[4]);
                }
                thisLine = nextLine;
                try {
                    nextLine = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (thisLine != null && !thisLine.startsWith(",,")) {
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

            // Checks whether there is a minimum of <threshold> milliseconds
            // between the actual event and the top 6 events so far.
            int threshold = 10000; // 10 seconds
            long minTimeDelta = 999999999;
            for (int i = 0; i < events.size(); i++) {
                long actualTimeDelta = Long.valueOf(partOfRide[5]) - Long.valueOf(events.get(i)[5]);
                if (actualTimeDelta < minTimeDelta) {
                    minTimeDelta = actualTimeDelta;
                }
            }
            boolean enoughTimePassed = minTimeDelta > threshold;

            // Check whether actualX is one of the top 2 events
            boolean eventAdded = false;
            if (maxXDelta > Double.valueOf(events.get(0)[2]) && !eventAdded && enoughTimePassed) {

                String[] temp = events.get(0);
                events.set(0, partOfRide);
                accEvents.set(0, new AccEvent(partOfRide));

                events.set(1, temp);
                accEvents.set(1, new AccEvent(temp));
                eventAdded = true;
            } else if (maxXDelta > Double.valueOf(events.get(1)[2]) && !eventAdded && enoughTimePassed) {

                events.set(1, partOfRide);
                accEvents.set(1, new AccEvent(partOfRide));
                eventAdded = true;
            }
            // Check whether actualY is one of the top 2 events
            else if (maxYDelta > Double.valueOf(events.get(2)[3]) && !eventAdded && enoughTimePassed) {

                String[] temp = events.get(2);
                events.set(2, partOfRide);
                accEvents.set(2, new AccEvent(partOfRide));
                events.set(3, temp);
                accEvents.set(3, new AccEvent(temp));
                eventAdded = true;

            } else if (maxYDelta > Double.valueOf(events.get(3)[3]) && !eventAdded && enoughTimePassed) {
                events.set(3, partOfRide);
                accEvents.set(3, new AccEvent(partOfRide));
                eventAdded = true;
            }
            // Check whether actualZ is one of the top 2 events
            else if (maxZDelta > Double.valueOf(events.get(4)[4]) && !eventAdded && enoughTimePassed) {
                String[] temp = events.get(4);
                events.set(4, partOfRide);
                accEvents.set(4, new AccEvent(partOfRide));
                events.set(5, temp);
                accEvents.set(5, new AccEvent(temp));

            } else if (maxZDelta > Double.valueOf(events.get(5)[4]) && !eventAdded && enoughTimePassed) {
                events.set(5, partOfRide);
                accEvents.set(5, new AccEvent(partOfRide));
                eventAdded = true;
            }

            if (nextLine == null) {
                break;
            }
        }
        for (int i = 0; i < accEvents.size(); i++) {
            Log.d(TAG, "accEvents.get(" + i + ") Position: " + (accEvents.get(i).position.toString())
                    + " timeStamp: " + (accEvents.get(i).timeStamp));
        }

        return accEvents;


    }

}
