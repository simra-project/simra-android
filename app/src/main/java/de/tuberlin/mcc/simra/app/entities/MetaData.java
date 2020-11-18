package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.Utils;

/**
 * //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * // META-FILE (one per user): contains ...
 * // * the information required to display rides in the ride history (See RecorderService)
 * //   (DATE,START TIME, END TIME, ANNOTATED TRUE/FALSE)
 * // * the RIDE KEY which allows to identify the file containing the complete data for
 * //   a ride. => Use case: user wants to view a ride from history - retrieve data
 * // * one meta file per user, so we only want to create it if it doesn't exist yet.
 */
public class MetaData {
    public final static String METADATA_HEADER = "key,startTime,endTime,state,numberOfIncidents,waitedTime,distance,numberOfScary,region";
    private Map<Integer, MetaDataEntry> metaDataEntries;

    public MetaData(Map<Integer, MetaDataEntry> metaDataEntries) {
        this.metaDataEntries = metaDataEntries;
    }

    public static void saveMetaData(MetaData metaData, Context context) {
        StringBuilder metaDataString = new StringBuilder();
        Iterator<Map.Entry<Integer, MetaDataEntry>> iterator = metaData.metaDataEntries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, MetaDataEntry> entry = iterator.next();
            metaDataString.append(entry.getValue().stringifyMetaDataEntry()).append(System.lineSeparator());
        }
        File newFile = IOUtils.Files.getMetaDataFile(context);
        Utils.overwriteFile(IOUtils.Files.getFileInfoLine() + METADATA_HEADER + System.lineSeparator() + metaDataString, newFile);
    }

    public static MetaData loadMetaData(Context context) {
        File metaDataFile = IOUtils.Files.getMetaDataFile(context);
        Map<Integer, MetaDataEntry> metaDataEntries = new HashMap() {};
        if (metaDataFile.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(metaDataFile))) {
                // Skip first two line as they do only contain the Header
                bufferedReader.readLine();
                bufferedReader.readLine();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        MetaDataEntry metaDataEntry = MetaDataEntry.parseEntryFromLine(line);
                        metaDataEntries.put(metaDataEntry.rideId, metaDataEntry);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new MetaData(metaDataEntries);
    }

    public static MetaDataEntry getMetaDataEntryForRide(Integer rideId, Context context) {
        File metaDataFile = IOUtils.Files.getMetaDataFile(context);
        if (metaDataFile.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(metaDataFile))) {
                // Skip first two line as they do only contain the Header
                bufferedReader.readLine();
                bufferedReader.readLine();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        MetaDataEntry metaDataEntry = MetaDataEntry.parseEntryFromLine(line);
                        if (metaDataEntry.rideId.equals(rideId)) {
                            return metaDataEntry;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void updateOrAddMetaDataEntryForRide(MetaDataEntry metaDataEntry, Context context) {
        MetaData metaData = loadMetaData(context);
        metaData.metaDataEntries.put(metaDataEntry.rideId, metaDataEntry);
        saveMetaData(metaData, context);
    }

    public static void deleteMetaDataEntryForRide(int rideId, Context context) {
        MetaData metaData = loadMetaData(context);
        metaData.metaDataEntries.remove(rideId);
        saveMetaData(metaData, context);
    }

    public static List<MetaDataEntry> getMetaDataEntries(Context context) {
        List<MetaDataEntry> metaDataEntries = new ArrayList<>();
        Iterator<Map.Entry<Integer, MetaDataEntry>> iterator = loadMetaData(context).metaDataEntries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, MetaDataEntry> entry = iterator.next();
            metaDataEntries.add(entry.getValue());
        }
        return metaDataEntries;
    }

    public static class STATE {
        /**
         * The ride is saved locally and was not yet annotated
         */
        public static final int JUST_RECORDED = 0;
        /**
         * The ride is saved locally and was annotated by the user
         */
        public static final int ANNOTATED = 1;
        /**
         * The ride is synced with the server and can not be edited anymore
         */
        public static final int SYNCED = 2;
    }
}
