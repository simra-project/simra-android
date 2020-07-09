package de.tuberlin.mcc.simra.app.annotation;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.tuberlin.mcc.simra.app.entities.AccEvent;
import de.tuberlin.mcc.simra.app.util.IOUtils;

import static de.tuberlin.mcc.simra.app.util.Constants.ACCEVENTS_HEADER;
import static de.tuberlin.mcc.simra.app.util.Utils.appendToFile;
import static de.tuberlin.mcc.simra.app.util.Utils.checkForAnnotation;
import static de.tuberlin.mcc.simra.app.util.Utils.fileExists;
import static de.tuberlin.mcc.simra.app.util.Utils.overWriteFile;

public class LegacyRide {

    static String TAG = "Ride_LOG";
    public List<AccEvent> events;
    public long distance;
    public int bike;
    public int child;
    public int trailer;
    public int pLoc;
    public int waitedTime;
    Context context;
    Polyline route;
    boolean temp = false;
    int state;
    private String key = "";

    public LegacyRide(
            File accGpsFile,
            int state,
            int bike,
            int child,
            int trailer,
            int pLoc,
            Context context,
            boolean calculateEvents,
            boolean temp
    ) throws IOException {
        Object[] waitedTimeRouteAndDistance = calculateWaitedTimePolylineDistance(accGpsFile);
        this.waitedTime = (int) waitedTimeRouteAndDistance[0];
        this.route = (Polyline) waitedTimeRouteAndDistance[1];
        this.distance = (long) waitedTimeRouteAndDistance[2];
        Log.d(TAG, "distance: " + distance + " waitedTime: " + waitedTime);
        this.state = state;
        this.bike = bike;
        this.child = child;
        this.trailer = trailer;
        this.pLoc = pLoc;
        this.context = context;
        this.temp = temp;
        this.key = accGpsFile.getName().split("_")[0];
        String pathToAccEventsOfRide = IOUtils.Files.getEventsFileName(Integer.parseInt(key), false);
        if (temp) {
            this.key = accGpsFile.getName().split("_")[0].replace("Temp", "");
            pathToAccEventsOfRide = "TempaccEvents" + key + ".csv";
        }
        String content = ACCEVENTS_HEADER;

        if (calculateEvents || temp) {
            this.events = findAccEvents(accGpsFile);
        } else {
            this.events = new ArrayList<>();
            File accEventsFile = IOUtils.Files.getEventsFile(Integer.parseInt(key), false, context);
            if (accEventsFile.exists()) {
                Log.d(TAG, "reading " + pathToAccEventsOfRide + " to get accEvents");
                try (BufferedReader br = new BufferedReader(new FileReader(accEventsFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] accEventLine = line.split(",", -1);
                        Log.d(TAG, "accEventLine: " + line);
                        if (!(accEventLine[0].equals("key") || (accEventLine.length < 20))) {
                            int key = Integer.parseInt(accEventLine[0]);
                            double lat = Double.parseDouble(accEventLine[1]);
                            double lon = Double.parseDouble(accEventLine[2]);
                            long timestamp = Long.parseLong(accEventLine[3]);
                            boolean annotated = checkForAnnotation(accEventLine);
                            String incidentType = accEventLine[8];
                            String scary = accEventLine[18];
                            events.add(new AccEvent(key, lat, lon, timestamp, annotated, incidentType, scary));
                        }
                    }
                }
            }
        }

        for (int i = 0; i < events.size(); i++) {
            AccEvent actualAccEvent = events.get(i);
            // TODO: Use IncidentLogEntry
            content += i + "," + actualAccEvent.position.getLatitude() + "," + actualAccEvent.position.getLongitude() + "," + actualAccEvent.timeStamp + "," + bike + "," + child + "," + trailer + "," + pLoc + ",,,,,,,,,,,," + System.lineSeparator();
        }
        String fileInfoLine = IOUtils.Files.getFileInfoLine();
        if (!fileExists(pathToAccEventsOfRide, context) && !temp) {
            appendToFile((fileInfoLine + content), pathToAccEventsOfRide, context);
        } else if (temp) {
            overWriteFile((fileInfoLine + content), pathToAccEventsOfRide, context);
        }

    }

    // Takes a File which contains all the data and creates a
    // PolyLine to be displayed on the map as a route.
    public static Object[] calculateWaitedTimePolylineDistance(File gpsFile) throws IOException {
        Polyline polyLine = new Polyline();

        // TODO: Use IOUtils for File Location
        BufferedReader br = new BufferedReader(new FileReader(gpsFile));
        // br.readLine() to skip the first two lines which contain the file version info and headers
        String line = br.readLine();
        line = br.readLine();
        int waitedTime = 0; // seconds
        Location previousLocation = null;
        Location thisLocation = null;
        long previousTimeStamp = 0; // milliseconds
        long thisTimeStamp = 0; // milliseconds
        long distance = 0; // meters
        while ((line = br.readLine()) != null) {
            String[] accGpsLineArray = line.split(",");
            if (!line.startsWith(",,")) {
                try {
                    if (thisLocation == null) {
                        thisLocation = new Location("thisLocation");
                        thisLocation.setLatitude(Double.parseDouble(accGpsLineArray[0]));
                        thisLocation.setLongitude(Double.parseDouble(accGpsLineArray[1]));
                        previousLocation = new Location("previousLocation");
                        previousLocation.setLatitude(Double.parseDouble(accGpsLineArray[0]));
                        previousLocation.setLongitude(Double.parseDouble(accGpsLineArray[1]));
                        thisTimeStamp = Long.parseLong(accGpsLineArray[5]);
                        previousTimeStamp = Long.parseLong(accGpsLineArray[5]);
                    } else {
                        thisLocation.setLatitude(Double.parseDouble(accGpsLineArray[0]));
                        thisLocation.setLongitude(Double.parseDouble(accGpsLineArray[1]));
                        thisTimeStamp = Long.parseLong(accGpsLineArray[5]);
                        // distance to last location in meters
                        double distanceToLastPoint = thisLocation.distanceTo(previousLocation);
                        // time passed from last point in seconds
                        long timePassed = (thisTimeStamp - previousTimeStamp) / 1000;
                        // if speed < 2.99km/h: waiting
                        if (distanceToLastPoint < 2.5) {
                            waitedTime += timePassed;
                        }
                        // if speed > 80km/h: too fast, do not consider for distance
                        if ((distanceToLastPoint / timePassed) < 22) {
                            distance += (long) distanceToLastPoint;
                            polyLine.addPoint(new GeoPoint(thisLocation));
                        }
                        previousLocation.setLatitude(Double.parseDouble(accGpsLineArray[0]));
                        previousLocation.setLongitude(Double.parseDouble(accGpsLineArray[1]));
                        previousTimeStamp = Long.parseLong(accGpsLineArray[5]);
                    }
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }
            }

        }

        br.close();
        return new Object[]{waitedTime, polyLine, distance};
    }

    public static List<AccEvent> findAccEvents(File accGpsFile) {
        List<AccEvent> accEvents = new ArrayList<>(6);

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
        List<String[]> ride = new ArrayList<>();
        List<String[]> events = new ArrayList<>(6);
        accEvents.add(new AccEvent(0, 999.0, 999.0, 0, false, "0", "0"));
        accEvents.add(new AccEvent(1, 999.0, 999.0, 0, false, "0", "0"));
        accEvents.add(new AccEvent(2, 999.0, 999.0, 0, false, "0", "0"));
        accEvents.add(new AccEvent(3, 999.0, 999.0, 0, false, "0", "0"));
        accEvents.add(new AccEvent(4, 999.0, 999.0, 0, false, "0", "0"));
        accEvents.add(new AccEvent(5, 999.0, 999.0, 0, false, "0", "0"));

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

            double maxX = Double.parseDouble(currentLine[2]);
            double minX = Double.parseDouble(currentLine[2]);
            double maxY = Double.parseDouble(currentLine[3]);
            double minY = Double.parseDouble(currentLine[3]);
            double maxZ = Double.parseDouble(currentLine[4]);
            double minZ = Double.parseDouble(currentLine[4]);
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
                if (Double.parseDouble(currentLine[2]) >= maxX) {
                    maxX = Double.parseDouble(currentLine[2]);
                } else if (Double.parseDouble(currentLine[2]) < minX) {
                    minX = Double.parseDouble(currentLine[2]);
                }
                if (Double.parseDouble(currentLine[3]) >= maxY) {
                    maxY = Double.parseDouble(currentLine[3]);
                } else if (Double.parseDouble(currentLine[3]) < minY) {
                    minY = Double.parseDouble(currentLine[3]);
                }
                if (Double.parseDouble(currentLine[4]) >= maxZ) {
                    maxZ = Double.parseDouble(currentLine[4]);
                } else if (Double.parseDouble(currentLine[4]) < minZ) {
                    minZ = Double.parseDouble(currentLine[4]);
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
                long actualTimeDelta = Long.parseLong(partOfRide[5]) - Long.parseLong(events.get(i)[5]);
                if (actualTimeDelta < minTimeDelta) {
                    minTimeDelta = actualTimeDelta;
                }
            }
            boolean enoughTimePassed = minTimeDelta > threshold;

            // Check whether actualX is one of the top 2 events
            boolean eventAdded = false;
            if (maxXDelta > Double.parseDouble(events.get(0)[2]) && !eventAdded && enoughTimePassed) {

                String[] temp = events.get(0);
                events.set(0, partOfRide);
                accEvents.set(0, new AccEvent(0, Double.parseDouble(partOfRide[0]), Double.parseDouble(partOfRide[1]), Long.parseLong(partOfRide[5]), false, "0", "0"));

                events.set(1, temp);
                accEvents.set(1, new AccEvent(1, Double.parseDouble(temp[0]), Double.parseDouble(temp[1]), Long.parseLong(temp[5]), false, "0", "0"));
                eventAdded = true;
            } else if (maxXDelta > Double.parseDouble(events.get(1)[2]) && !eventAdded && enoughTimePassed) {

                events.set(1, partOfRide);
                accEvents.set(1, new AccEvent(1, Double.parseDouble(partOfRide[0]), Double.parseDouble(partOfRide[1]), Long.parseLong(partOfRide[5]), false, "0", "0"));
                eventAdded = true;
            }
            // Check whether actualY is one of the top 2 events
            else if (maxYDelta > Double.parseDouble(events.get(2)[3]) && !eventAdded && enoughTimePassed) {

                String[] temp = events.get(2);
                events.set(2, partOfRide);
                accEvents.set(2, new AccEvent(2, Double.parseDouble(partOfRide[0]), Double.parseDouble(partOfRide[1]), Long.parseLong(partOfRide[5]), false, "0", "0"));
                events.set(3, temp);
                accEvents.set(3, new AccEvent(3, Double.parseDouble(temp[0]), Double.parseDouble(temp[1]), Long.parseLong(temp[5]), false, "0", "0"));
                eventAdded = true;

            } else if (maxYDelta > Double.parseDouble(events.get(3)[3]) && !eventAdded && enoughTimePassed) {
                events.set(3, partOfRide);
                accEvents.set(3, new AccEvent(3, Double.parseDouble(partOfRide[0]), Double.parseDouble(partOfRide[1]), Long.parseLong(partOfRide[5]), false, "0", "0"));
                eventAdded = true;
            }
            // Check whether actualZ is one of the top 2 events
            else if (maxZDelta > Double.parseDouble(events.get(4)[4]) && !eventAdded && enoughTimePassed) {
                String[] temp = events.get(4);
                events.set(4, partOfRide);
                accEvents.set(4, new AccEvent(4, Double.parseDouble(partOfRide[0]), Double.parseDouble(partOfRide[1]), Long.parseLong(partOfRide[5]), false, "0", "0"));
                events.set(5, temp);
                accEvents.set(5, new AccEvent(5, Double.parseDouble(temp[0]), Double.parseDouble(temp[1]), Long.parseLong(temp[5]), false, "0", "0"));

            } else if (maxZDelta > Double.parseDouble(events.get(5)[4]) && !eventAdded && enoughTimePassed) {
                events.set(5, partOfRide);
                accEvents.set(5, new AccEvent(5, Double.parseDouble(partOfRide[0]), Double.parseDouble(partOfRide[1]), Long.parseLong(partOfRide[5]), false, "0", "0"));
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

    public Integer getKey() {
        return Integer.parseInt(key);
    }

    public List<AccEvent> getEvents() {
        return events;
    }

    public Polyline getRoute() {
        return this.route;
    }

}
