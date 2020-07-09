package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.Utils;

public class MetaData {
    public final static String METADATA_HEADER = "key,startTime,endTime,state,numberOfIncidents,waitedTime,distance,numberOfScary,region";
    private Map<Integer, MetaDataEntry> metaDataEntries;

    public MetaData(Map<Integer, MetaDataEntry> metaDataEntries) {
        this.metaDataEntries = metaDataEntries;
    }

    public static void saveMetaData(MetaData metaData, Context context) {
        String metaDataString = "";
        Iterator<Map.Entry<Integer, MetaDataEntry>> iterator = metaData.metaDataEntries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, MetaDataEntry> entry = iterator.next();
            metaDataString += entry.getValue().stringifyMetaDataEntry() + System.lineSeparator();
        }
        File newFile = IOUtils.Files.getMetaDataFile(context);
        Utils.overwriteFile(IOUtils.Files.getFileInfoLine() + METADATA_HEADER + System.lineSeparator() + metaDataString, newFile);
    }

    public static MetaData loadMetaData(Context context) {
        File metaDataFile = IOUtils.Files.getMetaDataFile(context);
        Map<Integer, MetaDataEntry> metaDataEntries = new HashMap() {
        };
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
                        if (metaDataEntry.rideId == rideId) {
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

    public static void updateMetaDataEntryForRide(MetaDataEntry metaDataEntry, Context context) {
        MetaData metaData = loadMetaData(context);
        metaData.metaDataEntries.put(metaDataEntry.rideId, metaDataEntry);
        saveMetaData(metaData, context);
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
         * The ride is was synced with the server and can not be edited anymore
         */
        public static final int SYNCED = 2;
    }
}
