package de.tuberlin.mcc.simra.app.annotation;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.util.Pair;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import static de.tuberlin.mcc.simra.app.util.Constants.ACCEVENTS_HEADER;
import static de.tuberlin.mcc.simra.app.util.Utils.appendToFile;
import static de.tuberlin.mcc.simra.app.util.Utils.fileExists;
import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.util.Utils.overWriteFile;

public class Ride {

    private String key = "";

    public String getKey() {
        return key;
    }

    public File accGpsFile;
    public ArrayList<AccEvent> events;

    public ArrayList<AccEvent> getEvents() {
        return events;
    }

    String duration;
    String startTime;
    String endTime;
    public double distance;
    int waitedTime;
    Context context;
    static String TAG = "Ride_LOG";
    Polyline route;

    public int bike;
    public int child;
    public int trailer;
    public int pLoc;
    boolean temp = false;

    public String getDuration() {
        return duration;
    }

    public Polyline getRoute() {
        return this.route;
    }

    int state;


    // This is the constructor that is used for now.
    public Ride(File accGpsFile, String duration, String startTime, /*String date,*/ int state, int bike, int child, int trailer, int pLoc, Context context) throws IOException {
        this.accGpsFile = accGpsFile;
        this.duration = duration;
        this.startTime = startTime;
        Pair<Integer, Polyline> routAndWaitedTime = getRouteAndWaitTime(accGpsFile);
        this.route = routAndWaitedTime.second;
        this.waitedTime = routAndWaitedTime.first;
        this.distance = route.getDistance();
        Log.d(TAG,"distance: " + distance + " waitedTime: " + waitedTime);
        this.state = state;
        this.bike = bike;
        this.child = child;
        this.trailer = trailer;
        this.pLoc = pLoc;
        this.context = context;
        this.key = accGpsFile.getName().split("_")[0];
        this.events = findAccEvents();

        String pathToAccEventsOfRide = "accEvents" + key + ".csv";
        String content = ACCEVENTS_HEADER;
        String fileInfoLine = getAppVersionNumber(context) + "#1" + System.lineSeparator();

        if (!fileExists(pathToAccEventsOfRide, context)) {

            for (int i = 0; i < events.size(); i++) {

                AccEvent actualAccEvent = events.get(i);


                content += i + "," + actualAccEvent.position.getLatitude() + "," + actualAccEvent.position.getLongitude() + "," + actualAccEvent.timeStamp + "," + bike + "," + child + "," + trailer + "," + pLoc + ",,,,,,,,,,,," + System.lineSeparator();
            }

            appendToFile((fileInfoLine + content), pathToAccEventsOfRide, context);
        }

    }

    // temp ride
    public Ride(File tempAccGpsFile, String duration, String startTime, String endTime, /*String date,*/ int state, int bike, int child, int trailer, int pLoc, boolean temp, Context context) throws IOException {
        this.accGpsFile = tempAccGpsFile;
        this.duration = duration;
        this.startTime = startTime;
        // this.endTime = endTime;
        Pair<Integer, Polyline> routAndWaitedTime = getRouteAndWaitTime(tempAccGpsFile);
        this.route = routAndWaitedTime.second;
        this.waitedTime = routAndWaitedTime.first;
        this.distance = route.getDistance();
        Log.d(TAG,"distance: " + distance + " waitedTime: " + waitedTime);
        this.state = state;
        this.bike = bike;
        this.child = child;
        this.trailer = trailer;
        this.pLoc = pLoc;
        this.context = context;
        this.key = tempAccGpsFile.getName().split("_")[0].replace("Temp", "");
        this.temp = temp;
        this.events = findAccEvents();

        String pathToAccEventsOfRide = "TempaccEvents" + key + ".csv";
        String content = ACCEVENTS_HEADER;
        String fileInfoLine = getAppVersionNumber(context) + "#1" + System.lineSeparator();

        for (int i = 0; i < events.size(); i++) {

            AccEvent actualAccEvent = events.get(i);

            content += i + "," + actualAccEvent.position.getLatitude() + "," + actualAccEvent.position.getLongitude() + "," + actualAccEvent.timeStamp + "," + bike + "," + child + "," + trailer + "," + pLoc + ",,,,,,,,,,,," + System.lineSeparator();
        }

        overWriteFile((fileInfoLine + content), pathToAccEventsOfRide, context);

    }


    // Takes a File which contains all the data and creates a
    // PolyLine to be displayed on the map as a route.
    public static Pair<Integer, Polyline> getRouteAndWaitTime(File gpsFile) throws IOException {
        Polyline polyLine = new Polyline();

        BufferedReader br = new BufferedReader(new FileReader(gpsFile));
        // br.readLine() to skip the first two lines which contain the file version info and headers
        String line = br.readLine();
        line = br.readLine();
        int waitedTime = 0;
        Location lastLocation = null;
        Location thisLocation = null;
        while ((line = br.readLine()) != null) {
            String[] accGpsArray = line.split(",");

            if (!line.startsWith(",,")) {
                try {
                    double distanceToLastPoint = 0;

                    if (thisLocation == null) {
                        thisLocation = new Location("thisLocation");
                        thisLocation.setLatitude(Double.valueOf(accGpsArray[0]));
                        thisLocation.setLongitude(Double.valueOf(accGpsArray[1]));
                        lastLocation = new Location("lastLocation");
                        lastLocation.setLatitude(Double.valueOf(accGpsArray[0]));
                        lastLocation.setLongitude(Double.valueOf(accGpsArray[1]));
                    } else {
                        thisLocation.setLatitude(Double.valueOf(accGpsArray[0]));
                        thisLocation.setLongitude(Double.valueOf(accGpsArray[1]));
                        if (thisLocation.distanceTo(lastLocation) < 2.5) {
                            waitedTime += 3;
                        }
                        lastLocation.setLatitude(Double.valueOf(accGpsArray[0]));
                        lastLocation.setLongitude(Double.valueOf(accGpsArray[1]));
                    }
                    polyLine.addPoint(new GeoPoint(thisLocation));
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }
            }

        }

        br.close();
        Pair<Integer, Polyline> result = new Pair<>(waitedTime, polyLine);
        return result;
    }

    public ArrayList<AccEvent> findAccEvents() {
        ArrayList<AccEvent> accEvents = new ArrayList<>(6);

        File accEventsFile = new File(context.getFilesDir() + "/accEvents"+ key + ".csv");
        if (accEventsFile.exists() && !temp) {
            // loop through accEvents lines
            try (BufferedReader accEventsReader = new BufferedReader(new InputStreamReader(new FileInputStream(accEventsFile)))) {
                // fileInfoLine (24#2)
                String accEventsLine = accEventsReader.readLine();
                // key,lat,lon,ts,bike,childCheckBox,trailerCheckBox,pLoc,incident,i1,i2,i3,i4,i5,i6,i7,i8,i9,scary,desc,i10
                // 2,52.4251924,13.4405942,1553427047561,0,0,0,0,,,,,,,,,,,,
                // 20 entries per line (index 0-19)
                accEventsReader.readLine();
                while ((accEventsLine = accEventsReader.readLine()) != null) {
                    String[] accEventsArray = accEventsLine.split(",", -1);
                    accEvents.add(new AccEvent(Integer.valueOf(accEventsArray[0]),Double.valueOf(accEventsArray[1]),Double.valueOf(accEventsArray[2]),Long.valueOf(accEventsArray[3]),Boolean.valueOf(accEventsArray[4]),accEventsArray[5],accEventsArray[18]));
                }
            } catch (FileNotFoundException e) {
            e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            BufferedReader br = null;
            String thisLine = null;
            String nextLine = null;
            try {
                br = new BufferedReader(new FileReader(accGpsFile));
                br.readLine();
                br.readLine();
                thisLine = br.readLine();
                nextLine = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String[] partOfRide;
            // Each String[] in ride is a part of the ride which is ca. 3 seconds long.
            ArrayList<String[]> ride = new ArrayList<>();
            ArrayList<String[]> events = new ArrayList<>(6);
            Log.d(TAG, thisLine + " nextLine: " + nextLine);
            String[] thisLineArray = thisLine.split(",");
            accEvents.add(new AccEvent(thisLineArray));
            accEvents.add(new AccEvent(thisLineArray));
            accEvents.add(new AccEvent(thisLineArray));
            accEvents.add(new AccEvent(thisLineArray));
            accEvents.add(new AccEvent(thisLineArray));
            accEvents.add(new AccEvent(thisLineArray));

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


        }
        return accEvents;

    }

}
