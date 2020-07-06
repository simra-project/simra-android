package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.tuberlin.mcc.simra.app.util.IOUtils;

public class DataLog {
    public final static String DATA_LOG_HEADER = "lat,lon,X,Y,Z,timeStamp,acc,a,b,c,radmesserDistanceLeft1,radmesserDistanceLeft2,radmesserDistanceRight1,radmesserDistanceRight2";
    public final int id;
    public final List<DataLogEntry> dataLogEntries;


    // TODO: Implement convienience Functions (duration, startTime, endTime, waitedTime, distance,
    // TODO: Build Map for accessing values by timestamp

    DataLog(
            int id,
            Context context
    ) {
        this.id = id;
        this.dataLogEntries = getDataLogEntries(id, context);
    }

    public static List<DataLogEntry> getDataLogEntries(int id, Context context) {
        List<DataLogEntry> dataPoints = new ArrayList<>();
        File gpsLogFile = IOUtils.Files.getGPSLogFile(id, false, context);
        if (gpsLogFile.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(gpsLogFile))) {
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
        return dataPoints;
    }
}
