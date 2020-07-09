package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import static de.tuberlin.mcc.simra.app.util.Constants.ACCEVENTS_HEADER;
import static de.tuberlin.mcc.simra.app.util.Constants.METADATA_HEADER;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.writeIntToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.util.Utils.overWriteFile;
import static de.tuberlin.mcc.simra.app.util.Utils.recalculateStatistics;
import static de.tuberlin.mcc.simra.app.util.Utils.updateProfile;

public class VersionUpdater {

    private static final String TAG = "VersionUpdater_LOG";

    /**
     * Migrate Shared Prefs Data from previous Versions to the current
     *
     * @param context
     */
    public static void migrate(Context context) {
        int lastAppVersion = lookUpIntSharedPrefs("App-Version", -1, "simraPrefs", context);
        updateToV27(context, lastAppVersion);
        updateToV30(context, lastAppVersion);
        updateToV31(context, lastAppVersion);
        updateToV32(context, lastAppVersion);
        updateToV39(context, lastAppVersion);
        updateToV50(context, lastAppVersion);
        updateToV52(context, lastAppVersion);
        updateToV58(context, lastAppVersion);
        writeIntToSharedPrefs("App-Version", getAppVersionNumber(context), "simraPrefs", context);
    }


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
            for (File file : fileList) {
                name = file.getName();
                if (!(name.equals("metaData.csv") || name.equals("profile.csv") || file.isDirectory() || name.startsWith("accEvents") || name.startsWith("CRASH") || name.startsWith("simRa_regions.conf"))) {
                    file.renameTo(new File(directory.toString() + File.separator + name.split("_")[0] + "_accGps.csv"));
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
                contentOfNewMetaData.append(METADATA_HEADER);

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
                                        thisLocation.setLatitude(Double.parseDouble(accGpsArray[0]));
                                        thisLocation.setLongitude(Double.parseDouble(accGpsArray[1]));
                                        lastLocation = new Location("lastLocation");
                                        lastLocation.setLatitude(Double.parseDouble(accGpsArray[0]));
                                        lastLocation.setLongitude(Double.parseDouble(accGpsArray[1]));
                                    } else {
                                        thisLocation.setLatitude(Double.parseDouble(accGpsArray[0]));
                                        thisLocation.setLongitude(Double.parseDouble(accGpsArray[1]));
                                        if (thisLocation.distanceTo(lastLocation) < 2.5) {
                                            waitedTime += 3;
                                            if (isUploaded) {
                                                totalWaitedTime += 3;
                                            }
                                        }
                                        lastLocation.setLatitude(Double.parseDouble(accGpsArray[0]));
                                        lastLocation.setLongitude(Double.parseDouble(accGpsArray[1]));
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
                        totalDuration += (Long.parseLong(endTime) - Long.parseLong(startTime));
                        totalNumberOfRides++;
                        totalDistance += (long) waitedTimeRouteAndDistance[2];
                        Date startDate = new Date(Long.parseLong(startTime));
                        Date endDate = new Date(Long.parseLong(endTime));
                        Locale locale = Resources.getSystem().getConfiguration().locale;
                        SimpleDateFormat sdf = new SimpleDateFormat("HH", locale);
                        int startHour = Integer.parseInt(sdf.format(startDate));
                        int endHour = Integer.parseInt(sdf.format(endDate));
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
                overWriteFile(contentOfNewMetaData.toString(), "metaData.csv", context);
            } catch (IOException ioe) {
                Log.d(TAG, "++++++++++++++++++++++++++++++");
                Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
                Log.d(TAG, ioe.getMessage());
            }

            // renew profile.csv
            int birth = lookUpIntSharedPrefs("Profile-Age", 0, "simraPrefs", context);
            int gender = lookUpIntSharedPrefs("Profile-Gender", 0, "simraPrefs", context);
            int region = lookUpIntSharedPrefs("Profile-Region", 0, "simraPrefs", context);
            int experience = lookUpIntSharedPrefs("Profile-Experience", 0, "simraPrefs", context);

            // co2 savings on a bike per kilometer: 138g CO2
            long co2 = (long) ((totalDistance / (float) 1000) * 138);
            /*
            StringBuilder timeBucketsEntries = new StringBuilder();
            for (int i = 0; i <= 22; i++) {
                timeBucketsEntries.append(timeBuckets[i]).append(",");
            }
            timeBucketsEntries.append(timeBuckets[23]);
            String profileContent = birth + "," + gender + "," + region + "," + experience + "," + totalNumberOfRides + "," + totalDuration + "," + totalNumberOfIncidents + "," + totalWaitedTime + "," + totalDistance + "," + co2 + "," + timeBucketsEntries.toString() + ",-1," + System.lineSeparator();
            String fileInfoLine = getAppVersionNumber(context) + "#1" + System.lineSeparator();

            overWriteFile((fileInfoLine + PROFILE_HEADER + profileContent), "profile.csv", context);
            */
            updateProfile(true, context, birth, gender, region, experience, totalNumberOfRides, totalDuration, totalNumberOfIncidents, totalWaitedTime, totalDistance, co2, timeBuckets, -2, -1);

        }

    }

    public static void updateToV30(Context context, int lastAppVersion) {
        if (lastAppVersion < 30) {
            File directory = context.getFilesDir();
            File[] fileList = directory.listFiles();
            String name;
            for (File file : fileList) {
                name = file.getName();
                if (name.startsWith("accEvents")) {
                    StringBuilder contentOfNewAccEvents = new StringBuilder();
                    try (BufferedReader accEventsReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                        // fileInfoLine (29#2)
                        contentOfNewAccEvents.append(accEventsReader.readLine()).append(System.lineSeparator());
                        // skip old header
                        accEventsReader.readLine();
                        // add new header
                        contentOfNewAccEvents.append(ACCEVENTS_HEADER);
                        // add rest of content
                        String line;
                        while ((line = accEventsReader.readLine()) != null) {
                            contentOfNewAccEvents.append(line).append(System.lineSeparator());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    overWriteFile(contentOfNewAccEvents.toString(), file.getName(), context);
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
                contentOfNewMetaData.append(METADATA_HEADER);

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
                updateProfile(true, context, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, null, -2, totalNumberOfScary);

            } catch (IOException e) {
                Log.d(TAG, "updateToV31() exception: " + e.getLocalizedMessage());
                Log.d(TAG, Arrays.toString(e.getStackTrace()));
            }
            overWriteFile(contentOfNewMetaData.toString(), metaDataFile.getName(), context);
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
            updateProfile(true, context, -1, -1, -1, -1, -1, -1, totalNumberOfIncidents, -1, -1, -1, null, -2, -1);

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
                /*
                // fileInfoLine (35#2)
                String line = metaDataReader.readLine();
                // first line should be file info
                if (line.contains("#")) {
                    contentOfNewMetaData.append(line).append(System.lineSeparator());
                    line = metaDataReader.readLine();
                } else {
                    contentOfNewMetaData.append(getAppVersionNumber(context)).append("#1").append(System.lineSeparator());
                }
                // second line should be metaData header
                if (line.startsWith("key")) {
                    contentOfNewMetaData.append(line).append(System.lineSeparator());
                    line = metaDataReader.readLine();
                } else {
                    contentOfNewMetaData.append(METADATA_HEADER);
                }
                // following lines should be rides
                */
                while (line != null) {
                    if (!line.startsWith("key") && !line.startsWith("null") && line.split(",", -1).length > 2)
                        contentOfNewMetaData.append(line).append(System.lineSeparator());
                    line = metaDataReader.readLine();
                }
                String fileInfoLine = getAppVersionNumber(context) + "#1" + System.lineSeparator();

                overWriteFile(fileInfoLine + METADATA_HEADER + contentOfNewMetaData.toString(), metaDataFile.getName(), context);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void updateToV50(Context context, int lastAppVersion) {
        if (lastAppVersion < 50) {
            Log.d(TAG, "Updating to version 50");
            try {
                recalculateStatistics(context);
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
            int region = SharedPref.lookUpIntSharedPrefs("Region", 0, "Profile", context);
            // change region from London to Berlin
            if (region == 2) {
                SharedPref.writeIntToSharedPrefs("Region", 1, "Profile", context);
            }

        }
    }

    // introduces profile data per region
    public static void updateToV58(Context context, int lastAppVersion) {
        if (lastAppVersion < 58) {
            int region = lookUpIntSharedPrefs("Region", 0, "Profile", context);
            copySharedPreferences("Profile", "Profile_" + region, context);
            File metaDataFile = new File(context.getFilesDir() + "/metaData.csv");
            StringBuilder contentOfNewMetaData = new StringBuilder();
            try (BufferedReader metaDataReader = new BufferedReader(new InputStreamReader(new FileInputStream(metaDataFile)))) {

                // fileInfoLine (24#2)
                String line = metaDataReader.readLine();
                contentOfNewMetaData.append(line).append(System.lineSeparator());
                // header (kkey,startTime,endTime,state,numberOfIncidents,waitedTime,distance,numberOfScary,region" + System.lineSeparator())
                metaDataReader.readLine();
                contentOfNewMetaData.append(METADATA_HEADER);

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
                overWriteFile(contentOfNewMetaData.toString(), metaDataFile.getName(), context);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
    }
}
