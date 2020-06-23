package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.tuberlin.mcc.simra.app.util.IOUtils;

import static de.tuberlin.mcc.simra.app.util.Utils.fileExists;

public class RideImpl implements Serializable {
    public int rideID;
    public List<DataLogEntry> dataPoints;

    public RideImpl(int rideID, List<DataLogEntry> dataPoints) {
        this.rideID = rideID;
        this.dataPoints = dataPoints;
    }

    // TODO: Implement convienience Functions (duration, startTime, endTime, waitedTime, distance,
    // TODO: Build Map for accessing values by timestamp


    public static String getFileNameForRide(int rideID, boolean temp) {
        String pathToRide = rideID + "_accGps.csv";
        if (temp) {
            pathToRide = "Temp" + pathToRide;
        }
        return pathToRide;
    }

    public static RideImpl loadRideById(int rideID, Context context) {
        List<DataLogEntry> dataPoints = new ArrayList<>();
        String filePath = IOUtils.Directories.getBaseFolderPath(context) + getFileNameForRide(rideID, false);
        if (fileExists(filePath)) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(filePath)))) {
                // Skip first two line as they do only contain the Header, e.g.:
                // 59#1
                // lat,lon,X,Y,Z,timeStamp,acc,a,b,c
                bufferedReader.readLine();
                bufferedReader.readLine();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        dataPoints.add(DataLogEntry.parseDataLogEntryFromLine(line));
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new RideImpl(rideID, dataPoints);
    }

    public static RideImpl loadRideByFilePath(String path, Context context) {
        List<DataLogEntry> dataPoints = new ArrayList<>();
        if (fileExists(path)) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(path)))) {
                // Skip first two line as they do only contain the Header, e.g.:
                // 59#1
                // lat,lon,X,Y,Z,timeStamp,acc,a,b,c
                bufferedReader.readLine();
                bufferedReader.readLine();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        dataPoints.add(DataLogEntry.parseDataLogEntryFromLine(line));
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new RideImpl(22, dataPoints);
    }

    public static void writeRideToFile() {

    }
}
