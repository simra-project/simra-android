package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import static de.tuberlin.mcc.simra.app.util.Utils.readContentFromFile;

/**
 * Log Helper Methods for easier debugging
 */
public class LogHelper {

    private static final String TAG = "LogHelper";

    public static void showKeyPrefs(Context context) {
        SharedPreferences keyPrefs = context.getApplicationContext()
                .getSharedPreferences("keyPrefs", Context.MODE_PRIVATE);
        Log.d(TAG, "===========================V=keyPrefs=V===========================");
        Object[] keyPrefsArray = keyPrefs.getAll().entrySet().toArray();
        for (Object keyPrefsEntry : keyPrefsArray) {
            Log.d(TAG, keyPrefsEntry + "");
        }
        Log.d(TAG, "===========================Λ=keyPrefs=Λ===========================");
    }

    public static void showDataDirectory(Context context) {
        Log.d(TAG, "===========================V=Directory=V===========================");
        String[] fileList = context.fileList();
        for (String fileName : fileList) {
            Log.d(TAG, fileName);
        }
        Log.d(TAG, "===========================Λ=Directory=Λ===========================");
    }

    public static void showMetadata(Context context) {
        Log.d(TAG, "===========================V=metaData=V===========================");
        try (BufferedReader metaDataReader = new BufferedReader(new InputStreamReader(new FileInputStream(IOUtils.Files.getMetaDataFile(context))))) {
            String metaDataLine;
            // loop through the metaData.csv lines
            while ((metaDataLine = metaDataReader.readLine()) != null) {
                Log.d(TAG, metaDataLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Exception in showMetadata(): " + Arrays.toString(e.getStackTrace()));
        }
        Log.d(TAG, "===========================Λ=metaData=Λ===========================");
    }

    public static void showStatistics(Context context) {
        SharedPreferences profilePrefs = context.getApplicationContext()
                .getSharedPreferences("Profile", Context.MODE_PRIVATE);
        Log.d(TAG, "===========================V=Statistics=V===========================");
        Log.d(TAG, "numberOfRides: " + profilePrefs.getInt("NumberOfRides", -1));
        Log.d(TAG, "Distance: " + profilePrefs.getLong("Distance", -1) + "m");
        Log.d(TAG, "Co2: " + profilePrefs.getLong("Co2", -1) + "g");
        Log.d(TAG, "Duration: " + profilePrefs.getLong("Duration", -1) + "ms");
        Log.d(TAG, "WaitedTime: " + profilePrefs.getLong("WaitedTime", -1) + "s");
        Log.d(TAG, "NumberOfIncidents: " + profilePrefs.getInt("NumberOfIncidents", -1));
        Log.d(TAG, "NumberOfScary: " + profilePrefs.getInt("NumberOfScary", -1));
        String[] buckets = new String[24];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = i + ": " + profilePrefs.getFloat(String.valueOf(i), -1.0f);
        }
        Log.d(TAG, "timeBuckets: " + Arrays.toString(buckets));
        Log.d(TAG, "===========================Λ=Statistics=Λ===========================");
    }

    public static void showMetaDataFile(Context context) {
        Log.d(TAG, "===========================V=metaData=V===========================");
        Log.d(TAG, "metaData.csv: " + readContentFromFile(IOUtils.Files.getMetaDataFile(context)));
        Log.d(TAG, "===========================Λ=metaData=Λ===========================");
    }
}
