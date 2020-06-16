package de.tuberlin.mcc.simra.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.location.Location;
import android.util.Log;
import android.util.Pair;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static de.tuberlin.mcc.simra.app.util.Constants.ACCEVENTS_HEADER;
import static de.tuberlin.mcc.simra.app.util.Constants.METADATA_HEADER;

public class Utils {

    private static final String TAG = "Utils_LOG";

    public static String readContentFromFile(String fileName, Context context) {
        File file = new File(getBaseFolderPath(context) + fileName);
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
            Log.d(TAG, "readContentFromFile() Exception: " + Arrays.toString(ioe.getStackTrace()));
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
            File tempFile = new File(getBaseFolderPath(context) + fileName);
            FileOutputStream writer = new FileOutputStream(tempFile, true);
            writer.write(content.getBytes());
            writer.flush();
            writer.close();
        } catch (IOException ioe) {
            Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
        }
    }

    // appends content from file <accGps> to the content of file <accEvents>
    // and increases both their file version number
    public static Pair<String, String> appendAccGpsToAccEvents(String accEvents, String accGps, Context context) {

        StringBuilder content = new StringBuilder();
        StringBuilder accEventsContentToOverwrite = new StringBuilder();
        StringBuilder accEventsContentToUpload = new StringBuilder();
        int appVersion = getAppVersionNumber(context);
        String accEventsFileInfoLine = appVersion + "#-1" + System.lineSeparator();

        try (BufferedReader br = new BufferedReader(new FileReader(context.getFileStreamPath(accEvents)))) {
            String line = br.readLine();
            String topFileVersion = "" + ((Integer.valueOf(line.split("#")[1])) + 1);
            accEventsFileInfoLine = appVersion + "#" + topFileVersion + System.lineSeparator();
            accEventsContentToUpload.append(accEventsFileInfoLine);
            while ((line = br.readLine()) != null) {
                accEventsContentToOverwrite.append(line).append(System.lineSeparator());
                if (Utils.checkForAnnotation(line.split(",", -1))) {
                    accEventsContentToUpload.append(line).append(System.lineSeparator());
                }
            }
            overWriteFile((accEventsFileInfoLine + accEventsContentToOverwrite.toString()), accEvents, context);

        } catch (IOException ioe) {
            Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
        }

        StringBuilder contentBottom = new StringBuilder();
        String bottomFileInfoLine = appVersion + "#-1";
        try (BufferedReader br = new BufferedReader(new FileReader(context.getFileStreamPath(accGps)))) {
            String line;
            line = br.readLine();
            String bottomFileVersion = "" + ((Integer.valueOf(line.split("#")[1])) + 1);
            bottomFileInfoLine = appVersion + "#" + bottomFileVersion + System.lineSeparator();
            while ((line = br.readLine()) != null) {
                contentBottom.append(line).append(System.lineSeparator());
            }
            overWriteFile((bottomFileInfoLine + contentBottom.toString()), accGps, context);

        } catch (IOException ioe) {
            Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
        }
        content.append(accEventsContentToUpload);
        content.append(System.lineSeparator()).append("=========================").append(System.lineSeparator());
        content.append(bottomFileInfoLine).append(contentBottom);

        return new Pair<>(content.toString(), accEventsContentToUpload.toString());
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
        return new File(getBaseFolderPath(context) + fileName).exists();
    }

    /**
     * Returns the Base Folder (Private App File Directory)
     *
     * @param ctx The App Context
     * @return Path with trailing slash
     */
    public static String getBaseFolderPath(Context ctx) {
        return ctx.getFilesDir() + "/";
    }


    // Check if an accEvent has already been annotated based on one line of the accEvents csv file.
    // Returns true, if accEvent was already annotated.

    public static boolean checkForAnnotation(String[] incidentProps) {
        // Only checking for empty strings, which means we are retaining
        // events that were labeled as 'nothing happened'
        return (!incidentProps[8].equals("") && !incidentProps[8].equals("0")) || !incidentProps[19].equals("");
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

    public static void deleteErrorLogsForVersion(Context context, int version) {
        int appVersion = SharedPref.lookUpIntSharedPrefs("App-Version", -1, "simraPrefs", context);
        if (appVersion < version) {
            SharedPref.writeBooleanToSharedPrefs("NEW-UNSENT-ERROR", false, "simraPrefs", context);
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

    public static void updateProfile(boolean global, Context context, int ageGroup, int gender, int region, int experience, int behaviour) {

        updateProfile(global, context, ageGroup, gender, region, experience, -1, -1, -1, -1, -1, -1, null, behaviour, -1);

    }

    public static void updateProfile(boolean global, Context context, int ageGroup, int gender, int region, int experience, int numberOfRides, long duration, int numberOfIncidents, long waitedTime, long distance, long co2, Object[] timeBuckets, int behaviour, int numberOfScary) {

        SharedPreferences sharedPrefs;
        if (global) {
            sharedPrefs = context.getApplicationContext()
                    .getSharedPreferences("Profile", Context.MODE_PRIVATE);
        } else {
            sharedPrefs = context.getApplicationContext()
                    .getSharedPreferences("Profile_" + region, Context.MODE_PRIVATE);
        }

        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (ageGroup > -1) {
            editor.putInt("Birth", ageGroup);
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
                editor.putFloat(i + "", (Float) timeBuckets[i]);
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

    /**
     * @param region must be "Profile" for global or "Profile_0" for Profile of region 0.
     */
    public static Object[] getProfileWithoutDemographics(String region, Context context) {
        // {numberOfRides,duration,numberOfIncidents,waitedTime,distance,co2,0,1,2,...,23,numberOfScary}
        Object[] result = new Object[31];
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(region, Context.MODE_PRIVATE);
        result[0] = sharedPrefs.getInt("NumberOfRides", 0);
        result[1] = sharedPrefs.getLong("Duration", 0);
        result[2] = sharedPrefs.getInt("NumberOfIncidents", 0);
        result[3] = sharedPrefs.getLong("WaitedTime", 0);
        result[4] = sharedPrefs.getLong("Distance", 0);
        result[5] = sharedPrefs.getLong("Co2", 0);
        for (int i = 0; i <= 23; i++) {
            result[i + 6] = sharedPrefs.getFloat(i + "", 0f);
        }
        result[30] = sharedPrefs.getInt("NumberOfScary", 0);
        return result;
    }

    // returns values of
    // {(int)ageGroup,(int)gender,(int)region,(int)experience,(int)numberOfRides,(long)duration,(int)numberOfIncidents,(long)waitedTime,(long)distance,(long)co2,(int)0,1,2,...,23,(int)numberOfScary,(int)behaviour}
    // for a specific region or global
    public static Object[] getGlobalProfile(Context context) {
        Object[] result = new Object[36];
        int[] demographics = getProfileDemographics(context);
        Object[] rest = getProfileWithoutDemographics("Profile", context);

        for (int j = 0; j < demographics.length - 1; j++) {
            result[j] = demographics[j];
        }
        for (int k = 0; k < rest.length; k++) {
            result[k + 4] = rest[k];
        }

        result[35] = demographics[4];
        Log.d(TAG, "profile: " + Arrays.toString(result));
        return result;
    }

    /**
     * returns {NumberOfRides,Duration,NumberOfIncidents,WaitedTime,Distance,Co2,0,...,23,NumberOfScary}
     * for each region
     */
    public static Object[][] getRegionProfilesArrays(int numberOfRegions, Context context) {
        Object[][] result = new Object[numberOfRegions][31];
        for (int i = 0; i < result.length; i++) {
            result[i] = getProfileWithoutDemographics("Profile_" + i, context);
        }
        return result;
    }

    public static void permissionRequest(Activity activity, final String[] requestedPermission, String rationaleMessage, final int accessCode) {
        // Check whether FINE_LOCATION or ACCESS_BACKGROUND_LOCATION permissions are not granted
        if ((ContextCompat.checkSelfPermission(activity, requestedPermission[0])
                != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(activity, requestedPermission[1])
                != PackageManager.PERMISSION_GRANTED)) {

            // Permission for FINE_LOCATION is not granted. Show rationale why location permission is needed
            // in an AlertDialog and request access to FINE_LOCATION

            // The OK-Button fires a requestPermissions
            DialogInterface.OnClickListener rationaleOnClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(activity,
                            requestedPermission, accessCode);
                }
            };
            showMessageOK(rationaleMessage, rationaleOnClickListener, activity);
        }
    }

    // recalculates all statistics, updates metaData.csv, Profile.xml and deletes temp files
    public static void recalculateStatistics(Context context) {
        Log.d(TAG, "===========================V=recalculateStatistics=V===========================");
        File metaDataFile = new File(context.getFilesDir() + "/metaData.csv");
        StringBuilder contentOfMetaData = new StringBuilder();
        // total number of (scary) incidents read from each accEvents csv.
        int totalNumberOfIncidents = 0;
        int totalNumberOfScary = 0;

        // number of rides, distance, duration, CO2-Savings, waited time and time buckets.
        int totalNumberOfRides = 0;
        long totalDistance = 0; // in m
        long totalDuration = 0; // in ms
        long totalCO2Savings = 0; // in g
        long totalWaitedTime = 0; // in s
        // 0h - 23h
        Float[] timeBuckets = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};

        // loop through each line of metaData.csv
        try (BufferedReader metaDataReader = new BufferedReader(new InputStreamReader(new FileInputStream(metaDataFile)))) {
            // fileInfoLine (24#2)
            String metaDataFileVersion = metaDataReader.readLine().split("#", -1)[1];
            contentOfMetaData.append(getAppVersionNumber(context)).append("#").append(metaDataFileVersion)
                    .append(System.lineSeparator())
                    .append(METADATA_HEADER);
            // header (key,startTime,endTime,state,numberOfIncidents,waitedTime,distance,numberOfScary)
            String metaDataLine = metaDataReader.readLine();
            // loop through the metaData.csv lines
            // rides (0,1553426842081,1553426843354,2,0,0,0)
            while ((metaDataLine = metaDataReader.readLine()) != null) {
                String[] metaDataLineArray = metaDataLine.split(",", -1);
                if (metaDataLineArray.length < 3) {
                    continue;
                }
                String key = metaDataLineArray[0];
                // update totalNumberOf... only, if the ride has been uploaded
                boolean uploaded = true;
                if (!metaDataLineArray[3].equals("2")) {
                    uploaded = false;
                }
                // First part: read accEvents and calculate number of (scary) incidents.
                File accEventsFile = context.getFileStreamPath("accEvents" + key + ".csv");
                StringBuilder contentOfAccEvents = new StringBuilder();
                if (!accEventsFile.exists()) {
                    contentOfMetaData.append(metaDataLine).append(System.lineSeparator());
                    // updateProfile(context,-1,-1,-1,-1,totalNumberOfRides,totalDuration,totalNumberOfIncidents,totalWaitedTime,totalDistance,totalCO2Savings,timeBuckets,-2,totalNumberOfScary);
                    if (uploaded) {
                        totalNumberOfRides++;
                        totalDuration += (Long.valueOf(metaDataLineArray[2]) - Long.valueOf(metaDataLineArray[1]));
                        totalNumberOfIncidents += Integer.valueOf(metaDataLineArray[4]);
                        totalWaitedTime += Long.valueOf(metaDataLineArray[4]);
                        totalDistance += Long.valueOf(metaDataLineArray[5]);
                        Locale locale = Resources.getSystem().getConfiguration().locale;
                        SimpleDateFormat sdf = new SimpleDateFormat("HH", locale);
                        int startHour = Integer.valueOf(sdf.format(new Date(Long.valueOf(metaDataLineArray[1]))));
                        int endHour = Integer.valueOf(sdf.format(new Date(Long.valueOf(metaDataLineArray[2]))));
                        float duration = endHour - startHour + 1;
                        for (int i = startHour; i <= endHour; i++) {
                            timeBuckets[i] += (1 / duration);
                        }
                        totalNumberOfScary += Integer.valueOf(metaDataLineArray[7]);
                    }
                    continue;
                }
                // number of (scary) incidents read from the actual accEvents csv.
                int actualNumberOfIncidents = 0;
                int actualNumberOfScary = 0;
                // loop through each line of the actual accEvents csv
                try (BufferedReader accEventsReader = new BufferedReader(new InputStreamReader(new FileInputStream(accEventsFile)))) {
                    // fileInfoLine (24#2)
                    String accEventsLine = accEventsReader.readLine();
                    contentOfAccEvents.append(getAppVersionNumber(context)).append("#").append(accEventsLine.split("#", -1)[1])
                            .append(System.lineSeparator())
                            .append(ACCEVENTS_HEADER);
                    // key,lat,lon,ts,bike,childCheckBox,trailerCheckBox,pLoc,incident,i1,i2,i3,i4,i5,i6,i7,i8,i9,scary,desc,i10
                    // 2,52.4251924,13.4405942,1553427047561,0,0,0,0,,,,,,,,,,,,,
                    // 21 entries per metaDataLine (index 0-20)
                    accEventsLine = accEventsReader.readLine();
                    // skip fileInfo line and accEvents header
                    while (((accEventsLine = accEventsReader.readLine()) != null)) {
                        Log.d(TAG, "recalculateStatistics() " + accEventsFile.getName() + ": " + accEventsLine);
                        String[] accEventsLineArray = accEventsLine.split(",", -1);
                        // if the accEvent of the actual line is annotated, update the number of (scary) incidents)
                        if (checkForAnnotation(accEventsLineArray)) {
                            contentOfAccEvents.append(accEventsLine).append(System.lineSeparator());
                            actualNumberOfIncidents++;
                            if (accEventsLineArray[18].equals("1")) {
                                actualNumberOfScary++;
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Exception in recalculateStatistics(): " + Arrays.toString(e.getStackTrace()));
                    e.printStackTrace();
                }
                // Update the total number of (scary) incidents only if the ride has already been uploaded.
                // Otherwise the statistics will be faulty, since they get updated after an upload as well.
                if (uploaded) {
                    totalNumberOfIncidents += actualNumberOfIncidents;
                    totalNumberOfScary += actualNumberOfScary;
                    overWriteFile(contentOfAccEvents.toString(), "accEvents" + key + ".csv", context);
                }
                // Second part: read accGps and calculate number of rides, distance, duration, CO2-savings and waited time.
                File accGpsFile = context.getFileStreamPath(key + "_accGps.csv");
                if (!accGpsFile.exists()) {
                    Log.d(TAG, accGpsFile.getName() + " does not exist!");
                    if (uploaded) {
                        totalNumberOfRides++;
                        totalDuration += (Long.valueOf(metaDataLineArray[2]) - Long.valueOf(metaDataLineArray[1]));
                        totalWaitedTime += Long.valueOf(metaDataLineArray[4]);
                        totalDistance += Long.valueOf(metaDataLineArray[5]);
                        Locale locale = Resources.getSystem().getConfiguration().locale;
                        SimpleDateFormat sdf = new SimpleDateFormat("HH", locale);
                        int startHour = Integer.valueOf(sdf.format(new Date(Long.valueOf(metaDataLineArray[1]))));
                        int endHour = Integer.valueOf(sdf.format(new Date(Long.valueOf(metaDataLineArray[2]))));
                        float duration = endHour - startHour + 1;
                        for (int i = startHour; i <= endHour; i++) {
                            timeBuckets[i] += (1 / duration);
                        }
                    }
                    continue;
                }
                // long[distance, duration, waitedTime, firstTimeStamp, lastTimeStamp]
                long[] rideStatistics = calculateRideStatistics(context, key);
                long actualDistance = rideStatistics[0]; // distance of actual ride in m
                long actualDuration = rideStatistics[1]; // duration of actual ride in ms
                long actualWaitedTime = rideStatistics[2]; // waited time of actual ride in s
                long startTimeStamp = rideStatistics[3];
                long endTimeStamp = rideStatistics[4];

                if (uploaded) {
                    totalNumberOfRides++;
                    totalDistance += actualDistance;
                    totalDuration += actualDuration;
                    totalWaitedTime += actualWaitedTime;
                    Locale locale = Resources.getSystem().getConfiguration().locale;
                    SimpleDateFormat sdf = new SimpleDateFormat("HH", locale);
                    int startHour = Integer.valueOf(sdf.format(new Date(startTimeStamp)));
                    int endHour = Integer.valueOf(sdf.format(new Date(endTimeStamp)));
                    float duration = endHour - startHour + 1;
                    for (int i = startHour; i <= endHour; i++) {
                        timeBuckets[i] += (1 / duration);
                    }
                }
                // key and state are not changed
                contentOfMetaData
                        .append(key).append(",")
                        .append(startTimeStamp).append(",")
                        .append(endTimeStamp).append(",")
                        .append(metaDataLineArray[3]).append(",")
                        .append(actualNumberOfIncidents).append(",")
                        .append(actualWaitedTime).append(",")
                        .append(actualDistance).append(",")
                        .append(actualNumberOfScary).append(System.lineSeparator());

            }
            totalCO2Savings = (long) (totalDistance / 1000.0 * 138.0);
            Log.d(TAG, "recalculateStatistics() overwriting profile with following statistics");
            Log.d(TAG, "totalNumberOfRides: " + totalNumberOfRides);
            Log.d(TAG, "totalDuration: " + totalDuration);
            Log.d(TAG, "totalNumberOfIncidents: " + totalNumberOfIncidents);
            Log.d(TAG, "totalWaitedTime: " + totalWaitedTime);
            Log.d(TAG, "totalDistance: " + totalDistance);
            Log.d(TAG, "totalCO2Savings: " + totalCO2Savings);
            Log.d(TAG, "totalNumberOfScary: " + totalNumberOfScary);
            Log.d(TAG, "recalculateStatistics() overwriting metaData.csv with: ");
            Log.d(TAG, contentOfMetaData.toString());
            overWriteFile(contentOfMetaData.toString(), "metaData.csv", context);
            updateProfile(true, context, -1, -1, -1, -1, totalNumberOfRides, totalDuration, totalNumberOfIncidents, totalWaitedTime, totalDistance, totalCO2Savings, timeBuckets, -2, totalNumberOfScary);
        } catch (IOException e) {
            Log.d(TAG, "Exception in recalculateStatistics(): " + Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        Log.d(TAG, "===========================Λ=recalculateStatistics=Λ===========================");
    }

    // returns distance (m), duration (ms) and waited time (s) start and end time stamps (ms) of a ride
    // long[distance, duration, waitedTime, firstTimeStamp, lastTimeStamp]
    public static long[] calculateRideStatistics(Context context, String key) {
        long[] result = new long[5];
        File accGpsFile = context.getFileStreamPath(key + "_accGps.csv");
        Location previousLocation = null; // lat,lon
        Location thisLocation = null; // lat,lon
        long previousTimeStamp = 0; // milliseconds
        long thisTimeStamp = 0; // milliseconds
        long distance = 0; // meters
        long startTimeStamp = 0; // milliseconds
        long endTimeStamp = 0; // milliseconds
        long waitedTime = 0; // seconds
        // loop through each line of the accGps csv
        try (BufferedReader accGpsReader = new BufferedReader(new InputStreamReader(new FileInputStream(accGpsFile)))) {
            // skip fileInfo line (47#2)
            String accGpsLine = accGpsReader.readLine();
            // skip ACCGPS_HEADER
            // key,lat,lon,ts,bike,childCheckBox,trailerCheckBox,pLoc,incident,i1,i2,i3,i4,i5,i6,i7,i8,i9,scary,desc,i10
            // 2,52.4251924,13.4405942,1553427047561,0,0,0,0,,,,,,,,,,,,,
            // 21 entries per metaDataLine (index 0-20)
            accGpsReader.readLine();
            while ((accGpsLine = accGpsReader.readLine()) != null) {
                String[] accGpsLineArray = accGpsLine.split(",", -1);
                if (startTimeStamp == 0) {
                    startTimeStamp = Long.valueOf(accGpsLineArray[5]);
                } else {
                    endTimeStamp = Long.valueOf(accGpsLineArray[5]);
                }
                if (!accGpsLine.startsWith(",,")) {
                    try {
                        // initialize this and previous locations
                        if (thisLocation == null || previousLocation == null) {
                            thisLocation = new Location("thisLocation");
                            thisLocation.setLatitude(Double.valueOf(accGpsLineArray[0]));
                            thisLocation.setLongitude(Double.valueOf(accGpsLineArray[1]));
                            previousLocation = new Location("lastLocation");
                            previousLocation.setLatitude(Double.valueOf(accGpsLineArray[0]));
                            previousLocation.setLongitude(Double.valueOf(accGpsLineArray[1]));
                            previousTimeStamp = Long.valueOf(accGpsLineArray[5]);
                        } else {
                            thisLocation.setLatitude(Double.valueOf(accGpsLineArray[0]));
                            thisLocation.setLongitude(Double.valueOf(accGpsLineArray[1]));
                            thisTimeStamp = Long.valueOf(accGpsLineArray[5]);
                            double distanceToLastPoint = thisLocation.distanceTo(previousLocation);
                            long timePassed = (thisTimeStamp - previousTimeStamp) / 1000;
                            // if speed < 2.99km/h: waiting
                            if (distanceToLastPoint < 2.5) {
                                waitedTime += timePassed;
                            }
                            // if speed > 80km/h: too fast, do not consider for distance
                            if (distanceToLastPoint < 66) {
                                distance += distanceToLastPoint;
                            } else {
                                Log.d(TAG, "speed between " + previousLocation.getLatitude() + "," + previousLocation.getLongitude() + " and " + thisLocation.getLatitude() + "," + thisLocation.getLongitude() + " was " + (int) distanceToLastPoint / timePassed + " m/s or " + (int) (distanceToLastPoint / timePassed) * 3.6 + " km/h.");
                            }
                            previousLocation.setLatitude(Double.valueOf(accGpsLineArray[0]));
                            previousLocation.setLongitude(Double.valueOf(accGpsLineArray[1]));
                            previousTimeStamp = Long.valueOf(accGpsLineArray[5]);
                        }
                    } catch (NumberFormatException nfe) {
                        Log.d(TAG, "fixIncidentStatistics() Exception: " + Arrays.toString(nfe.getStackTrace()));
                    }
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "fixIncidentStatistics() Exception: " + Arrays.toString(e.getStackTrace()));
        }
        result[0] = distance;
        result[1] = endTimeStamp - startTimeStamp;
        // result[2] = (long)(distance / 1000.0 * 138.0); // CO2-Savings = 138g/km
        result[2] = waitedTime;
        result[3] = startTimeStamp; // for metaData.csv
        result[4] = endTimeStamp; // for metaData.csv
        return result;
    }

    public static String[] getRegions(Context context) {
        String[] simRa_regions_config = (Utils.readContentFromFile("simRa_regions.config", context)).split(System.lineSeparator());
        if (simRa_regions_config.length < 8) {
            try {
                AssetManager am = context.getApplicationContext().getAssets();
                InputStream is = am.open("simRa_regions.config");
                InputStreamReader inputStreamReader = new InputStreamReader(is, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {

                    stringBuilder.append(receiveString.trim() + System.lineSeparator());

                }
                is.close();
                simRa_regions_config = stringBuilder.toString().split(System.lineSeparator());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return simRa_regions_config;
    }
}
