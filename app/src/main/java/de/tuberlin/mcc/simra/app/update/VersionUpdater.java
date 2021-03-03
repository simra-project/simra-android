package de.tuberlin.mcc.simra.app.update;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.location.Location;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

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
import java.util.Map;

import static de.tuberlin.mcc.simra.app.util.Utils.calculateCO2Savings;

@SuppressWarnings("ALL")
@SuppressLint("all")

/**
 * Independent Package for Migrating Data between Versions.
 * This File is in an extra package in order to not depend on other App Functionality, when writing a migration do not use any Application Logic!
 * Why? Because Application Code might change thus breaking the Migration
 * <p>
 * DO NOT MOVE THIS FILE OR RENAME
 * In order for checkstyle to enforce the independence of this package!
 */
public class VersionUpdater {

    private static final String TAG = "VersionUpdater_LOG";

    /**
     * Stuff that needs to be done in version24:
     * updating metaData.csv with column "distance" and "waitTime" and calculating distance for
     * already existing rides.
     * Also, renaming accGps files: removing timestamp from filename.
     */
    public static void updateToV27(Context context, int lastAppVersion) {
        if (lastAppVersion < 27) {

            // rename accGps files
            File directory = context.getFilesDir();
            File[] fileList = directory.listFiles();
            String name;
            for (int i = 0; i < fileList.length; i++) {
                name = fileList[i].getName();
                if (!(name.equals("metaData.csv") || name.equals("profile.csv") || fileList[i].isDirectory() || name.startsWith("accEvents") || name.startsWith("CRASH") || name.startsWith("simRa_regions.conf"))) {
                    fileList[i].renameTo(new File(directory.toString() + File.separator + name.split("_")[0] + "_accGps.csv"));
                }
            }

            // find out correct start- and endTimes and write them to metaData.csv
            File metaDataFile = new File(context.getFilesDir() + "/metaData.csv");
            StringBuilder contentOfNewMetaData = new StringBuilder();
            int totalNumberOfIncidents = 0;
            int totalNumberOfRides = 0;
            long totalDuration = 0;
            long totalDistance = 0;
            long totalWaitedTime = 0;
            // 0h - 23h
            Float[] timeBuckets = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};
            try (BufferedReader metaDataReader = new BufferedReader(new InputStreamReader(new FileInputStream(metaDataFile)))) {
                // fileInfoLine (24#2)
                String line = metaDataReader.readLine();
                contentOfNewMetaData.append(line).append(System.lineSeparator());
                // header (key,startTime,endTime,state,numberOfIncidents,waitedTime,distance)
                metaDataReader.readLine();
                contentOfNewMetaData.append(Constants.METADATA_HEADER);

                // loop through the metaData.csv lines
                // rides (0,1553426842081,1553426843354,2)
                while ((line = metaDataReader.readLine()) != null) {
                    String[] lineArray = line.split(",");
                    String key = lineArray[0];
                    boolean isUploaded = lineArray[3].equals("2");
                    String startTime = null;
                    String endTime = null;
                    int incidents = 0;
                    int waitedTime = 0;
                    File gpsFile = context.getFileStreamPath(key + "_accGps.csv");
                    if (gpsFile.exists()) {
                        try (BufferedReader accGpsReader = new BufferedReader(new InputStreamReader(new FileInputStream(gpsFile)))) {

                            String accGpsLine = accGpsReader.readLine();
                            accGpsReader.readLine();
                            Location lastLocation = null;
                            Location thisLocation = null;
                            // loop through the accGps file and find startTime, endtime, distance and waitedTime
                            while ((accGpsLine = accGpsReader.readLine()) != null) {
                                String[] accGpsArray = accGpsLine.split(",", -1);
                                if (startTime == null) {
                                    startTime = accGpsArray[5];
                                } else {
                                    endTime = accGpsArray[5];
                                }

                                if (!accGpsLine.startsWith(",,")) {
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
                                            if (isUploaded) {
                                                totalWaitedTime += 3;
                                            }
                                        }
                                        lastLocation.setLatitude(Double.valueOf(accGpsArray[0]));
                                        lastLocation.setLongitude(Double.valueOf(accGpsArray[1]));
                                    }

                                }
                            }
                        }
                    } else {
                        continue;
                    }

                    File accEventsFile = context.getFileStreamPath("accEvents" + key + ".csv");
                    if (accEventsFile.exists()) {
                        // loop through accEvents lines
                        try (BufferedReader accEventsReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(context.getFilesDir() + "/accEvents" + key + ".csv"))))) {
                            // fileInfoLine (24#2)
                            String accEventsLine = accEventsReader.readLine();
                            // key,lat,lon,ts,bike,childCheckBox,trailerCheckBox,pLoc,incident,i1,i2,i3,i4,i5,i6,i7,i8,i9,scary,desc
                            // 2,52.4251924,13.4405942,1553427047561,0,0,0,0,,,,,,,,,,,,
                            // 20 entries per line (index 0-19)
                            accEventsReader.readLine();
                            while ((accEventsLine = accEventsReader.readLine()) != null) {
                                String[] accEventsArray = accEventsLine.split(",", -1);
                                if (!accEventsArray[8].equals("0") && !accEventsArray[8].equals("")) {
                                    incidents++;
                                    if (isUploaded) {
                                        totalNumberOfIncidents++;
                                    }
                                }
                            }
                        }
                    } else {
                        continue;
                    }
                    Object[] waitedTimeRouteAndDistance = Legacy.calculateWaitedTimePolylineDistance(gpsFile);
                    if (isUploaded) {
                        totalDuration += (Long.valueOf(endTime) - Long.valueOf(startTime));
                        totalNumberOfRides++;
                        totalDistance += (long) waitedTimeRouteAndDistance[2];
                        Date startDate = new Date(Long.valueOf(startTime));
                        Date endDate = new Date(Long.valueOf(endTime));
                        Locale locale = Resources.getSystem().getConfiguration().locale;
                        SimpleDateFormat sdf = new SimpleDateFormat("HH", locale);
                        int startHour = Integer.valueOf(sdf.format(startDate));
                        int endHour = Integer.valueOf(sdf.format(endDate));
                        float duration = endHour - startHour + 1;
                        for (int i = startHour; i <= endHour; i++) {
                            timeBuckets[i] += (1 / duration);
                        }
                        Log.d(TAG, key + " startHour:" + startHour + " endHour: " + endHour);

                    }
                    // key,startTime,endTime,state,numberOfIncidents,waitedTime,distance
                    line = key + "," + startTime + "," + endTime + "," + lineArray[3] + "," + incidents + "," + waitedTime + "," + waitedTimeRouteAndDistance[2];
                    contentOfNewMetaData.append(line).append(System.lineSeparator());
                }
                Log.d(TAG, "metaData.csv new content: " + contentOfNewMetaData.toString());
                Legacy.Utils.overWriteFile(contentOfNewMetaData.toString(), "metaData.csv", context);
            } catch (IOException ioe) {
                Log.d(TAG, "++++++++++++++++++++++++++++++");
                Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
                Log.d(TAG, ioe.getMessage());
            }

            // renew profile.csv
            int birth = Legacy.Utils.lookUpIntSharedPrefs("Profile-Age", 0, "simraPrefs", context);
            int gender = Legacy.Utils.lookUpIntSharedPrefs("Profile-Gender", 0, "simraPrefs", context);
            int region = Legacy.Utils.lookUpIntSharedPrefs("Profile-Region", 0, "simraPrefs", context);
            int experience = Legacy.Utils.lookUpIntSharedPrefs("Profile-Experience", 0, "simraPrefs", context);

            // co2 savings on a bike per kilometer: 138g CO2
            long co2 = calculateCO2Savings(totalDistance);
            Legacy.Utils.updateProfile(true, context, birth, gender, region, experience, totalNumberOfRides, totalDuration, totalNumberOfIncidents, totalWaitedTime, totalDistance, co2, timeBuckets, -2, -1);

        }

    }

    public static void updateToV30(Context context, int lastAppVersion) {
        if (lastAppVersion < 30) {
            File directory = context.getFilesDir();
            File[] fileList = directory.listFiles();
            String name;
            for (int i = 0; i < fileList.length; i++) {
                name = fileList[i].getName();
                if (name.startsWith("accEvents")) {
                    StringBuilder contentOfNewAccEvents = new StringBuilder();
                    try (BufferedReader accEventsReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileList[i])))) {
                        // fileInfoLine (29#2)
                        contentOfNewAccEvents.append(accEventsReader.readLine()).append(System.lineSeparator());
                        // skip old header
                        accEventsReader.readLine();
                        // add new header
                        contentOfNewAccEvents.append(Constants.ACCEVENTS_HEADER);
                        // add rest of content
                        String line;
                        while ((line = accEventsReader.readLine()) != null) {
                            contentOfNewAccEvents.append(line).append(System.lineSeparator());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Legacy.Utils.overWriteFile(contentOfNewAccEvents.toString(), fileList[i].getName(), context);
                }
            }
        }
    }

    public static void updateToV31(Context context, int lastAppVersion) {
        if (lastAppVersion < 31) {
            File metaDataFile = new File(context.getFilesDir() + "/metaData.csv");
            StringBuilder contentOfNewMetaData = new StringBuilder();
            int totalNumberOfScary = 0;
            try (BufferedReader metaDataReader = new BufferedReader(new InputStreamReader(new FileInputStream(metaDataFile)))) {

                // fileInfoLine (24#2)
                String line = metaDataReader.readLine();
                contentOfNewMetaData.append(line).append(System.lineSeparator());
                // header (key,startTime,endTime,state,numberOfIncidents,waitedTime,distance,numberOfScary)
                metaDataReader.readLine();
                contentOfNewMetaData.append(Constants.METADATA_HEADER);

                // loop through the metaData.csv lines
                // rides (0,1553426842081,1553426843354,2,0,0,0)
                while ((line = metaDataReader.readLine()) != null) {
                    String[] lineArray = line.split(",");

                    String key = lineArray[0];
                    boolean isUploaded = lineArray[3].equals("2");

                    int numberOfScary = 0;

                    File accEventsFile = context.getFileStreamPath("accEvents" + key + ".csv");
                    if (accEventsFile.exists()) {
                        // loop through accEvents lines
                        try (BufferedReader accEventsReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(context.getFilesDir() + "/accEvents" + key + ".csv"))))) {
                            // fileInfoLine (24#2)
                            String accEventsLine = accEventsReader.readLine();
                            // key,lat,lon,ts,bike,childCheckBox,trailerCheckBox,pLoc,incident,i1,i2,i3,i4,i5,i6,i7,i8,i9,scary,desc
                            // 2,52.4251924,13.4405942,1553427047561,0,0,0,0,,,,,,,,,,,,
                            // 20 entries per line (index 0-19)
                            accEventsReader.readLine();
                            while ((accEventsLine = accEventsReader.readLine()) != null) {
                                String[] accEventsArray = accEventsLine.split(",", -1);
                                if (accEventsArray[18].equals("1") && !accEventsArray[8].equals("")) {
                                    numberOfScary++;
                                    if (isUploaded) {
                                        totalNumberOfScary++;
                                    }
                                }
                            }
                        }
                    } else {
                        continue;
                    }
                    // key,startTime,endTime,state,numberOfIncidents,waitedTime,distance,numberOfScary
                    if (lineArray.length > 4) {
                        line = lineArray[0] + "," + lineArray[1] + "," + lineArray[2] + "," + lineArray[3] + "," + lineArray[4] + "," + lineArray[5] + "," + lineArray[6] + "," + numberOfScary;
                    } else {
                        line = lineArray[0] + "," + lineArray[1] + "," + lineArray[2] + "," + lineArray[3] + ",0,0,0," + numberOfScary;
                    }
                    contentOfNewMetaData.append(line).append(System.lineSeparator());
                }
                Legacy.Utils.updateProfile(true, context, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, null, -2, totalNumberOfScary);

            } catch (IOException e) {
                Log.d(TAG, "updateToV31() exception: " + e.getLocalizedMessage());
                Log.d(TAG, Arrays.toString(e.getStackTrace()));
            }
            Legacy.Utils.overWriteFile(contentOfNewMetaData.toString(), metaDataFile.getName(), context);
        }
    }

    public static void updateToV32(Context context, int lastAppVersion) {
        if (lastAppVersion < 32) {
            File metaDataFile = new File(context.getFilesDir() + "/metaData.csv");
            int totalNumberOfIncidents = 0;
            try (BufferedReader metaDataReader = new BufferedReader(new InputStreamReader(new FileInputStream(metaDataFile)))) {

                // fileInfoLine (24#2)
                String line = metaDataReader.readLine();
                // header (key,startTime,endTime,state,numberOfIncidents,waitedTime,distance,numberOfScary)
                metaDataReader.readLine();
                // loop through the metaData.csv lines
                // rides (0,1553426842081,1553426843354,2,0,0,0)
                while ((line = metaDataReader.readLine()) != null) {
                    String[] lineArray = line.split(",");

                    boolean isUploaded = lineArray[3].equals("2");
                    String key = lineArray[0];
                    File accEventsFile = context.getFileStreamPath("accEvents" + key + ".csv");
                    if (isUploaded && accEventsFile.exists()) {
                        // loop through accEvents lines
                        try (BufferedReader accEventsReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(context.getFilesDir() + "/accEvents" + key + ".csv"))))) {
                            // fileInfoLine (24#2)
                            String accEventsLine = accEventsReader.readLine();
                            // key,lat,lon,ts,bike,childCheckBox,trailerCheckBox,pLoc,incident,i1,i2,i3,i4,i5,i6,i7,i8,i9,scary,desc
                            // 2,52.4251924,13.4405942,1553427047561,0,0,0,0,,,,,,,,,,,,
                            // 20 entries per line (index 0-19)
                            accEventsReader.readLine();
                            while ((accEventsLine = accEventsReader.readLine()) != null) {
                                String[] accEventsArray = accEventsLine.split(",", -1);
                                if (!accEventsArray[8].equals("") && !accEventsArray[8].equals("0")) {
                                    totalNumberOfIncidents++;
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            Legacy.Utils.updateProfile(true, context, -1, -1, -1, -1, -1, -1, totalNumberOfIncidents, -1, -1, -1, null, -2, -1);

        }
    }

    // There was a bug in which METADATA_HEADER was not written as metaData.csv was overwritten after
    // each "save ride". This is to fix the metaData.csv files without METADATA_HEADER.
    public static void updateToV39(Context context, int lastAppVersion) {
        if (lastAppVersion < 39) {
            File metaDataFile = new File(context.getFilesDir() + "/metaData.csv");
            StringBuilder contentOfNewMetaData = new StringBuilder();
            try (BufferedReader metaDataReader = new BufferedReader(new InputStreamReader(new FileInputStream(metaDataFile)))) {
                String line = metaDataReader.readLine();

                while (line != null) {
                    if (!line.startsWith("key") && !line.startsWith("null") && line.split(",", -1).length > 2)
                        contentOfNewMetaData.append(line).append(System.lineSeparator());
                    line = metaDataReader.readLine();
                }
                String fileInfoLine = Legacy.Utils.getAppVersionNumber(context) + "#1" + System.lineSeparator();

                Legacy.Utils.overWriteFile(fileInfoLine + Constants.METADATA_HEADER + contentOfNewMetaData.toString(), metaDataFile.getName(), context);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void updateToV50(Context context, int lastAppVersion) {
        if (lastAppVersion < 50) {
            Log.d(TAG, "Updating to version 50");
            try {
                Legacy.Utils.recalculateStatistics(context);
            } catch (Exception e) {
                Log.d(TAG, "updateToV50 exception: " + e.getLocalizedMessage());
                Log.d(TAG, Arrays.toString(e.getStackTrace()));
            }
        }
    }

    // changes region from 2 (London) to 1 (Berlin) because they were mixed up in the previous version
    public static void updateToV52(Context context, int lastAppVersion) {
        if (lastAppVersion < 52) {
            // get previous chosen region
            int region = Legacy.Utils.lookUpIntSharedPrefs("Region", 0, "Profile", context);
            // change region from London to Berlin
            if (region == 2) {
                Legacy.Utils.writeIntToSharedPrefs("Region", 1, "Profile", context);
            }

        }
    }

    // introduces profile data per region
    public static void updateToV58(Context context, int lastAppVersion) {
        if (lastAppVersion < 58) {
            int region = Legacy.Utils.lookUpIntSharedPrefs("Region", 0, "Profile", context);
            Legacy.Utils.copySharedPreferences("Profile", "Profile_" + region, context);
            File metaDataFile = new File(context.getFilesDir() + "/metaData.csv");
            StringBuilder contentOfNewMetaData = new StringBuilder();
            try (BufferedReader metaDataReader = new BufferedReader(new InputStreamReader(new FileInputStream(metaDataFile)))) {

                // fileInfoLine (24#2)
                String line = metaDataReader.readLine();
                contentOfNewMetaData.append(line).append(System.lineSeparator());
                // header (kkey,startTime,endTime,state,numberOfIncidents,waitedTime,distance,numberOfScary,region" + System.lineSeparator())
                metaDataReader.readLine();
                contentOfNewMetaData.append(Constants.METADATA_HEADER);

                // loop through the metaData.csv lines
                // rides (2,1553501618290,1553504544190,2,0,1415,14042,0)
                while ((line = metaDataReader.readLine()) != null) {
                    String[] metaDataLine = line.split(",");
                    contentOfNewMetaData.append(metaDataLine[0]).append(",")
                            .append(metaDataLine[1]).append(",")
                            .append(metaDataLine[2]).append(",")
                            .append(metaDataLine[3]).append(",")
                            .append(metaDataLine[4]).append(",")
                            .append(metaDataLine[5]).append(",")
                            .append(metaDataLine[6]).append(",")
                            .append(metaDataLine[7]).append(",")
                            .append(region).append(System.lineSeparator());
                }
                Legacy.Utils.overWriteFile(contentOfNewMetaData.toString(), metaDataFile.getName(), context);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Do not make make PUBLIC!
     * This is Legacy Code only intended for not changing the Migrations
     */
    private static class Constants {
        public static final String METADATA_HEADER = "key,startTime,endTime,state,numberOfIncidents,waitedTime,distance,numberOfScary,region" + System.lineSeparator();

        public static final String ACCEVENTS_HEADER = "key,lat,lon,ts,bike,childCheckBox,trailerCheckBox,pLoc,incident,i1,i2,i3,i4,i5,i6,i7,i8,i9,scary,desc,i10" + System.lineSeparator();
    }

    /**
     * Do not make make PUBLIC!
     * This is Legacy Code only intended for not changing the Migrations
     */
    public static class Legacy {
        // Takes a File which contains all the data and creates a
        // PolyLine to be displayed on the map as a route.
        public static Object[] calculateWaitedTimePolylineDistance(File gpsFile) throws IOException {
            Polyline polyLine = new Polyline();

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
                            thisLocation.setLatitude(Double.valueOf(accGpsLineArray[0]));
                            thisLocation.setLongitude(Double.valueOf(accGpsLineArray[1]));
                            previousLocation = new Location("previousLocation");
                            previousLocation.setLatitude(Double.valueOf(accGpsLineArray[0]));
                            previousLocation.setLongitude(Double.valueOf(accGpsLineArray[1]));
                            thisTimeStamp = Long.valueOf(accGpsLineArray[5]);
                            previousTimeStamp = Long.valueOf(accGpsLineArray[5]);
                        } else {
                            thisLocation.setLatitude(Double.valueOf(accGpsLineArray[0]));
                            thisLocation.setLongitude(Double.valueOf(accGpsLineArray[1]));
                            thisTimeStamp = Long.valueOf(accGpsLineArray[5]);
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
                            previousLocation.setLatitude(Double.valueOf(accGpsLineArray[0]));
                            previousLocation.setLongitude(Double.valueOf(accGpsLineArray[1]));
                            previousTimeStamp = Long.valueOf(accGpsLineArray[5]);
                        }
                    } catch (NumberFormatException nfe) {
                        nfe.printStackTrace();
                    }
                }

            }

            br.close();
            return new Object[]{waitedTime, polyLine, distance};
        }

        public static class Utils {

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
                    Log.d(TAG, "readContentFromFile() Exception: " + Arrays.toString(ioe.getStackTrace()));
                }
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

            public static int lookUpIntSharedPrefs(String key, int defValue, String sharedPrefName, Context context) {
                SharedPreferences sharedPrefs = context.getApplicationContext()
                        .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
                return sharedPrefs.getInt(key, defValue);
            }

            public static void writeIntToSharedPrefs(String key, int value, String sharedPrefName, Context context) {
                SharedPreferences sharedPrefs = context.getApplicationContext()
                        .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putInt(key, value);
                editor.apply();
            }

            // Check if an accEvent has already been annotated based on one line of the accEvents csv file.
            // Returns true, if accEvent was already annotated.
            public static boolean checkForAnnotation(String[] incidentProps) {
                // Only checking for empty strings, which means we are retaining
                // events that were labeled as 'nothing happened'
                return (!incidentProps[8].equals("") && !incidentProps[8].equals("0")) || !incidentProps[19].equals("");
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
                            .append(Constants.METADATA_HEADER);
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
                                    .append(Constants.ACCEVENTS_HEADER);
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
                    totalCO2Savings = calculateCO2Savings(totalDistance);
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

            public static void copySharedPreferences(String sourceSharedPref, String newSharedPref, Context context) {
                SharedPreferences sourceSP = context.getApplicationContext()
                        .getSharedPreferences(sourceSharedPref, Context.MODE_PRIVATE);

                SharedPreferences newSP = context.getApplicationContext()
                        .getSharedPreferences(newSharedPref, Context.MODE_PRIVATE);
                SharedPreferences.Editor newE = newSP.edit();

                for (Map.Entry<String, ?> kvPair : sourceSP.getAll().entrySet()) {
                    String key = kvPair.getKey();
                    Object value = kvPair.getValue();
                    if (value instanceof Float) {
                        newE.putFloat(key, (Float) value);
                    } else if (value instanceof Integer) {
                        newE.putInt(key, (Integer) value);
                    } else if (value instanceof String) {
                        newE.putString(key, (String) value);
                    } else if (value instanceof Boolean) {
                        newE.putBoolean(key, (Boolean) value);
                    } else if (value instanceof Long) {
                        newE.putLong(key, (Long) value);
                    }
                }
                newE.apply();
            }
        }
    }
}