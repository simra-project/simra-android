package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;


import static android.content.Context.MODE_APPEND;
import static de.tuberlin.mcc.simra.app.util.Constants.METADATA_HEADER;

public class Utils {

    private static final String TAG = "Utils_LOG";

    public static String readContentFromFile(String fileName, Context context) {
        File file;
        if (fileName.contains(File.separator)) {
            file = new File(fileName);
        } else {
            file = context.getFileStreamPath(fileName);
        }
        if (file.isDirectory()) {
            return "FILE IS DIRECTORY";
        }
        StringBuilder content = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;

            while ((line = br.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        } catch (IOException ioe) {
            Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
        }
        return content.toString();
    }

    public static String readContentFromFileAndIncreaseFileVersion(String fileName, Context context) {
        StringBuilder content = new StringBuilder();
        int appVersion = getAppVersionNumber(context);
        String fileInfoLine = appVersion + "#-1";

        try (BufferedReader br = new BufferedReader(new FileReader(context.getFileStreamPath(fileName)))) {
            String line;
            line = br.readLine();
            String fileVersion = "" + ((Integer.valueOf(line.split("#")[1])) + 1);
            fileInfoLine = appVersion + "#" + fileVersion + System.lineSeparator();
            while ((line = br.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            overWriteFile((fileInfoLine + content.toString()), fileName, context);
        } catch (IOException ioe) {
            Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
        }
        return (fileInfoLine + content.toString());
    }


    public static void appendToFile(String content, String fileName, Context context) {

        try {
            FileOutputStream writer = context.openFileOutput(fileName, MODE_APPEND);
            writer.write(content.getBytes());
            writer.flush();
            writer.close();
        } catch (IOException ioe) {
            Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
        }
    }

    // appends content from file <fileNameBottom> to the content of file <fileNameTop>
    // and increases both their file version number
    public static String appendFromFileToFile(String fileNameTop, String fileNameBottom, Context context){

        StringBuilder content = new StringBuilder();
        StringBuilder contentTop = new StringBuilder();
        int appVersion = getAppVersionNumber(context);
        String topFileInfoLine = appVersion + "#-1";

        try (BufferedReader br = new BufferedReader(new FileReader(context.getFileStreamPath(fileNameTop)))) {
            String line;
            line = br.readLine();
            String topFileVersion = "" + ((Integer.valueOf(line.split("#")[1])) + 1);
            topFileInfoLine = appVersion + "#" + topFileVersion + System.lineSeparator();
            while ((line = br.readLine()) != null) {
                contentTop.append(line).append(System.lineSeparator());
            }
            overWriteFile((topFileInfoLine + contentTop.toString()), fileNameTop, context);

        } catch (IOException ioe) {
            Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
        }

        StringBuilder contentBottom = new StringBuilder();
        String bottomFileInfoLine = appVersion + "#-1";
        try (BufferedReader br = new BufferedReader(new FileReader(context.getFileStreamPath(fileNameBottom)))) {
            String line;
            line = br.readLine();
            String bottomFileVersion = "" + ((Integer.valueOf(line.split("#")[1])) + 1);
            bottomFileInfoLine = appVersion + "#" + bottomFileVersion + System.lineSeparator();
            while ((line = br.readLine()) != null) {
                contentBottom.append(line).append(System.lineSeparator());
            }
            overWriteFile((bottomFileInfoLine + contentBottom.toString()), fileNameBottom, context);

        } catch (IOException ioe) {
            Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
        }
        content.append(topFileInfoLine).append(contentTop);
        content.append(System.lineSeparator()).append("=========================").append(System.lineSeparator());
        content.append(bottomFileInfoLine).append(contentBottom);
        return content.toString();
    }

    public static void overWriteFile(String content, String fileName, Context context) {
        try {
            FileOutputStream writer = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            writer.write(content.getBytes());
            writer.flush();
            writer.close();
        } catch (IOException ioe) {
            Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
        }
    }

    public static boolean fileExists(String fileName, Context context) {
        return context.getFileStreamPath(fileName).exists();
    }

    public static String lookUpSharedPrefs(String key, String defValue, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        return sharedPrefs.getString(key, defValue);
    }

    public static int lookUpIntSharedPrefs(String key, int defValue, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        return sharedPrefs.getInt(key, defValue);
    }

    public static long lookUpLongSharedPrefs(String key, long defValue, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        return sharedPrefs.getLong(key, defValue);
    }
    public static boolean lookUpBooleanSharedPrefs(String key, boolean defValue, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        return sharedPrefs.getBoolean(key, defValue);
    }

    public static void writeToSharedPrefs(String key, String value, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void writeIntToSharedPrefs(String key, int value, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void writeLongToSharedPrefs(String key, long value, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static void writeBooleanToSharedPrefs(String key, boolean value, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }


    // Check if an accEvent has already been annotated based on one line of the accEvents csv file.
    // Returns true, if accEvent was already annotated.

    public static boolean checkForAnnotation(String[] incidentProps) {

        // Only checking for empty strings, which means we are retaining
        // events that were labeled as 'nothing happened'
        return !incidentProps[10].equals("") || !incidentProps[11].equals("") ||
                !incidentProps[12].equals("") || !incidentProps[13].equals("") ||
                !incidentProps[14].equals("") || !incidentProps[15].equals("") ||
                !incidentProps[16].equals("") || !incidentProps[17].equals("") ||
                !incidentProps[18].equals("") || !incidentProps[19].equals("");

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Create an AlertDialog with an OK Button displaying a message
    public static void showMessageOK(String message, DialogInterface.OnClickListener okListener, Context context) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", okListener)
                .create()
                .show();
    }

    public static int getAppVersionNumber(Context context) {
        PackageInfo pinfo = null;
        try {
            pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, Arrays.toString(e.getStackTrace()));
        }
        int installedVersionNumber = -1;
        if (pinfo != null) {
            installedVersionNumber = pinfo.versionCode;
        }
        return installedVersionNumber;
    }

    public void resetAppIfVersionIsBelow(Context context, int version) {
        int appVersion = lookUpIntSharedPrefs("App-Version", -1, "simraPrefs", context);

        if (appVersion < version) {
            File[] dirFiles = context.getFilesDir().listFiles();
            String path;
            for (int i = 0; i < dirFiles.length; i++) {

                path = dirFiles[i].getName();
                Log.d(TAG, "path: " + path);
                if (!path.equals("profile.csv")) {
                    dirFiles[i].delete();
                }
            }

            String fileInfoLine = getAppVersionNumber(context) + "#1" + System.lineSeparator();

            overWriteFile((fileInfoLine + METADATA_HEADER), "metaData.csv", context);
            writeIntToSharedPrefs("RIDE-KEY", 0, "simraPrefs", context);
        }
        writeIntToSharedPrefs("App-Version", getAppVersionNumber(context), "simraPrefs", context);
    }

    public static void deleteErrorLogsForVersion(Context context, int version) {
        int appVersion = lookUpIntSharedPrefs("App-Version", -1, "simraPrefs", context);
        if (appVersion < version) {
            writeBooleanToSharedPrefs("NEW-UNSENT-ERROR", false, "simraPrefs", context);
            File[] dirFiles = context.getFilesDir().listFiles();
            String path;
            for (int i = 0; i < dirFiles.length; i++) {
                path = dirFiles[i].getName();
                if (path.startsWith("CRASH")) {
                    dirFiles[i].delete();
                }
            }
        }

            //writeIntToSharedPrefs("App-Version", getAppVersionNumber(context), "simraPrefs", context);


    }


    public static void showKeyPrefs(Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences("keyPrefs", Context.MODE_PRIVATE);
        Log.d(TAG, "keyPrefs:" + Arrays.toString(sharedPrefs.getAll().entrySet().toArray()));
    }

    public static void updateProfile(Context context, int birth, int gender, int region, int experience, int behaviour) {

        updateProfile(context, birth, gender, region, experience, -1,-1,-1,-1,-1,-1,null,behaviour,-1);

    }

    public static void updateProfile(Context context, int birth, int gender, int region, int experience, int numberOfRides, long duration, int numberOfIncidents, long waitedTime, long distance, long co2, float[] timeBuckets, int behaviour, int numberOfScary) {

    SharedPreferences sharedPrefs = context.getApplicationContext()
            .getSharedPreferences("Profile", Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPrefs.edit();
    if (birth > -1) {
        editor.putInt("Birth", birth);
    }
    if (gender > -1) {
        editor.putInt("Gender", gender);
    }
    if (region > -1) {
        editor.putInt("Region", region);
    }
    if (experience > -1) {
        editor.putInt("Experience", experience);
    }
    if (numberOfRides > -1) {
        editor.putInt("NumberOfRides", numberOfRides);
    }
    if (duration > -1) {
        editor.putLong("Duration", duration);
    }
    if (numberOfIncidents > -1) {
        editor.putInt("NumberOfIncidents", numberOfIncidents);
    }
    if (waitedTime > -1) {
        editor.putLong("WaitedTime", waitedTime);
    }
    if (distance > -1) {
        editor.putLong("Distance", distance);
    }
    if (co2 > -1) {
        editor.putLong("Co2", co2);
    }
    if (timeBuckets != null) {
        for (int i = 0; i < timeBuckets.length; i++) {
            editor.putFloat(i+"",timeBuckets[i]);
        }
    }
    if (behaviour > -2) {
        editor.putInt("Behaviour", behaviour);
    }
    if (numberOfScary > -1) {
        editor.putInt("NumberOfScary", numberOfScary);
    }
    editor.apply();

}

    public static int[] getProfileDemographics(Context context) {
        // {ageGroup, gender, region, experience, behaviour}
        int[] result = new int[5];
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences("Profile", Context.MODE_PRIVATE);
        result[0] = sharedPrefs.getInt("Birth", 0);
        result[1] = sharedPrefs.getInt("Gender", 0);
        result[2] = sharedPrefs.getInt("Region", 0);
        result[3] = sharedPrefs.getInt("Experience", 0);
        result[4] = sharedPrefs.getInt("Behaviour", -1);
        return result;
    }

    public static Object[] getProfileWithoutDemographics(Context context) {
        // {numberOfRides,duration,numberOfIncidents,waitedTime,distance,co2,0,1,2,...,23,numberOfScary}
        Object[] result = new Object[31];
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences("Profile", Context.MODE_PRIVATE);
        result[0] = sharedPrefs.getInt("NumberOfRides", 0);
        result[1] = sharedPrefs.getLong("Duration", 0);
        result[2] = sharedPrefs.getInt("NumberOfIncidents", 0);
        result[3] = sharedPrefs.getLong("WaitedTime", 0);
        result[4] = sharedPrefs.getLong("Distance", 0);
        result[5] = sharedPrefs.getLong("Co2", 0);
        for (int i = 0; i <= 23 ; i++) {
            result[i+6] = sharedPrefs.getFloat(i+"",0f);
        }
        result[30] = sharedPrefs.getInt("NumberOfScary", 0);
        return result;
    }

    // returns values of
    // {(int)ageGroup,(int)gender,(int)region,(int)experience,(int)numberOfRides,(long)duration,(int)numberOfIncidents,(long)waitedTime,(long)distance,(long)co2,(int)0,1,2,...,23,(int)numberOfScary,(int)behaviour}
    public static Object[] getProfile(Context context) {
        Object[] result = new Object[36];
        int[] demographics = getProfileDemographics(context);
        Object[] rest = getProfileWithoutDemographics(context);

        for (int j = 0; j < demographics.length-1; j++) {
            result[j] = demographics[j];
        }
        for (int k = 0; k < rest.length; k++) {
            result[k+4] = rest[k];
        }

        result[35] = demographics[4];
        Log.d(TAG, "profile: " + Arrays.toString(result));
        return result;
    }



}
