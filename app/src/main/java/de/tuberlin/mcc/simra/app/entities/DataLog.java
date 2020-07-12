package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;
import android.location.Location;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.Utils;

public class DataLog {
    public final static String DATA_LOG_HEADER = "lat,lon,X,Y,Z,timeStamp,acc,a,b,c,radmesserDistanceLeft1,radmesserDistanceLeft2,radmesserDistanceRight1,radmesserDistanceRight2,radmesserClosePassEvent,radmesserClosePassEventPayload1,radmesserClosePassEventPayload2";
    public final int rideId;
    public final List<DataLogEntry> dataLogEntries;
    public final List<DataLogEntry> onlyGPSDataLogEntries;
    public final RideAnalysisData rideAnalysisData;
    public final long startTime;
    public final long endTime;


    // TODO: Implement convienience Functions (duration, startTime, endTime, waitedTime, distance,
    // TODO: Build Map for accessing values by timestamp

    private DataLog(
            int rideId,
            List<DataLogEntry> dataLogEntries,
            List<DataLogEntry> onlyGPSDataLogEntries, RideAnalysisData rideAnalysisData,
            long startTime, long endTime) {
        this.rideId = rideId;
        this.dataLogEntries = dataLogEntries;
        this.onlyGPSDataLogEntries = onlyGPSDataLogEntries;
        this.rideAnalysisData = rideAnalysisData;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static DataLog loadDataLog(int rideId, Context context) {
        return loadDataLog(rideId, null, null, context);
    }

    public static DataLog loadDataLog(int rideId, Long startTimeBoundary, Long endTimeBoundary, Context context) {
        List<DataLogEntry> dataPoints = new ArrayList<>();
        List<DataLogEntry> onlyGPSDataLogEntries = new ArrayList<>();
        long startTime = 0;
        long endTime = 0;

        File gpsLogFile = IOUtils.Files.getGPSLogFile(rideId, false, context);
        if (gpsLogFile.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(gpsLogFile))) {
                // Skip first two line as they do only contain the Header
                bufferedReader.readLine();
                bufferedReader.readLine();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        DataLogEntry dataLogEntry = DataLogEntry.parseDataLogEntryFromLine(line);
                        if (Utils.isInTimeFrame(startTimeBoundary, endTimeBoundary, dataLogEntry.timestamp)
                        ) {
                            dataPoints.add(dataLogEntry);
                            if (dataLogEntry.longitude != null && dataLogEntry.latitude != null) {
                                onlyGPSDataLogEntries.add(dataLogEntry);
                            }
                        }
                    }
                }
                startTime = dataPoints.get(0).timestamp;
                endTime = dataPoints.get(dataPoints.size() - 1).timestamp;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        RideAnalysisData rideAnalysisData = null;
        rideAnalysisData = RideAnalysisData.calculateRideAnalysisData(onlyGPSDataLogEntries);
        return new DataLog(rideId, dataPoints, onlyGPSDataLogEntries, rideAnalysisData, startTime, endTime);
    }

    public static void saveDataLog(DataLog dataLog, Context context) {
        File gpsDataLogFile = IOUtils.Files.getGPSLogFile(dataLog.rideId, false, context);
        if (gpsDataLogFile.exists() && gpsDataLogFile.isFile()) {
            gpsDataLogFile.delete();
        }
        try (BufferedWriter writer = new BufferedWriter((new FileWriter(gpsDataLogFile)))) {
            writer.write(IOUtils.Files.getFileInfoLine() + DATA_LOG_HEADER + System.lineSeparator());
            for (DataLogEntry dataLogEntry : dataLog.dataLogEntries) {
                writer.write(dataLogEntry.stringifyDataLogEntry() + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static class RideAnalysisData {
        public final long waitedTime;
        public final Polyline route;
        public final long distance;

        public RideAnalysisData(long waitedTime, Polyline route, long distance) {
            this.waitedTime = waitedTime;
            this.route = route;
            this.distance = distance;
        }

        public static DataLog.RideAnalysisData calculateRideAnalysisData(List<DataLogEntry> dataLogEntries) {
            Polyline polyLine = new Polyline();

            int waitedTime = 0; // seconds
            Location previousLocation = null;
            Location thisLocation = null;
            long previousTimeStamp = 0; // milliseconds
            long thisTimeStamp = 0; // milliseconds
            long distance = 0; // meters
            for (DataLogEntry dataLogEntry : dataLogEntries) {
                if (thisLocation == null) {
                    thisLocation = new Location("thisLocation");
                    thisLocation.setLatitude(dataLogEntry.latitude);
                    thisLocation.setLongitude(dataLogEntry.longitude);
                    previousLocation = new Location("previousLocation");
                    previousLocation.setLatitude(dataLogEntry.latitude);
                    previousLocation.setLongitude(dataLogEntry.longitude);
                    thisTimeStamp = dataLogEntry.timestamp;
                    previousTimeStamp = dataLogEntry.timestamp;
                } else {
                    thisLocation.setLatitude(dataLogEntry.latitude);
                    thisLocation.setLongitude(dataLogEntry.longitude);
                    thisTimeStamp = dataLogEntry.timestamp;
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
                    previousLocation.setLatitude(dataLogEntry.latitude);
                    previousLocation.setLongitude(dataLogEntry.longitude);
                    previousTimeStamp = dataLogEntry.timestamp;
                }
            }
            return new DataLog.RideAnalysisData(waitedTime, polyLine, distance);
        }
    }
}
