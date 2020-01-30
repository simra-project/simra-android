package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.annotation.Ride;
import de.tuberlin.mcc.simra.app.annotation.ShowRouteActivity;

import static de.tuberlin.mcc.simra.app.util.Constants.ACCEVENTS_HEADER;
import static de.tuberlin.mcc.simra.app.util.Constants.METADATA_HEADER;
import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.overWriteFile;
import static de.tuberlin.mcc.simra.app.util.Utils.recalculateStatistics;
import static de.tuberlin.mcc.simra.app.util.Utils.updateProfile;

public class VersionUpdater {

    private static final String TAG = "VersionUpdater_LOG";


    /**
     * Stuff that needs to be done in version24:
     * updating metaData.csv with column "distance" and "waitTime" and calculating distance for
     * already existing rides.
     * Also, renaming accGps files: removing timestamp from filename.
     *
     */
    public static void updateToV27(Context context, int lastAppVersion) {
        if (lastAppVersion < 27) {

            // rename accGps files
            File directory = context.getFilesDir();
            File[] fileList = directory.listFiles();
            String name;
            for (int i = 0; i < fileList.length; i++) {
                name = fileList[i].getName();
                if (!(name.equals("metaData.csv") || name.equals("profile.csv") || fileList[i].isDirectory() || name.startsWith("accEvents") || name.startsWith("CRASH"))) {
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
            float[] timeBuckets = {0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f};
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
                                String[] accGpsArray = accGpsLine.split(",",-1);
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
                    if (accEventsFile.exists()){
                        // loop through accEvents lines
                        try (BufferedReader accEventsReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(context.getFilesDir() + "/accEvents" + key + ".csv") )))) {
                            // fileInfoLine (24#2)
                            String accEventsLine = accEventsReader.readLine();
                            // key,lat,lon,ts,bike,childCheckBox,trailerCheckBox,pLoc,incident,i1,i2,i3,i4,i5,i6,i7,i8,i9,scary,desc
                            // 2,52.4251924,13.4405942,1553427047561,0,0,0,0,,,,,,,,,,,,
                            // 20 entries per line (index 0-19)
                            accEventsReader.readLine();
                            while ((accEventsLine = accEventsReader.readLine()) != null) {
                                String[] accEventsArray = accEventsLine.split(",",-1);
                                if(!accEventsArray[8].equals("0") && !accEventsArray[8].equals("")) {
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
                    Object[] waitedTimeRouteAndDistance = Ride.calculateWaitedTimePolylineDistance(gpsFile);
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
                            timeBuckets[i] += (1/duration);
                        }
                        Log.d(TAG, key + " startHour:" + startHour + " endHour: " + endHour);

                    }
                    // key,startTime,endTime,state,numberOfIncidents,waitedTime,distance
                    line = key + "," + startTime + "," + endTime + "," + lineArray[3] + "," + incidents  + "," + waitedTime + "," + waitedTimeRouteAndDistance[2];
                    contentOfNewMetaData.append(line).append(System.lineSeparator());
                }
                Log.d(TAG, "metaData.csv new content: " + contentOfNewMetaData.toString());
                overWriteFile(contentOfNewMetaData.toString(),"metaData.csv",context);
            } catch (IOException ioe) {
                Log.d(TAG, "++++++++++++++++++++++++++++++");
                Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
                Log.d(TAG, ioe.getMessage());
            }

            // renew profile.csv
            int birth = lookUpIntSharedPrefs("Profile-Age",0,"simraPrefs", context);
            int gender = lookUpIntSharedPrefs("Profile-Gender",0,"simraPrefs",context);
            int region = lookUpIntSharedPrefs("Profile-Region",0,"simraPrefs",context);
            int experience = lookUpIntSharedPrefs("Profile-Experience",0,"simraPrefs",context);

            // co2 savings on a bike per kilometer: 138g CO2
            long co2 = (long)((totalDistance/(float)1000)*138);
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
            updateProfile(context,birth,gender,region,experience,totalNumberOfRides,totalDuration,totalNumberOfIncidents,totalWaitedTime,totalDistance,co2,timeBuckets,-2,-1);

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
                        contentOfNewAccEvents.append(ACCEVENTS_HEADER);
                        // add rest of content
                        String line;
                        while ((line = accEventsReader.readLine()) != null) {
                            contentOfNewAccEvents.append(line).append(System.lineSeparator());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    overWriteFile(contentOfNewAccEvents.toString(),fileList[i].getName(),context);
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
                    if (accEventsFile.exists()){
                        // loop through accEvents lines
                        try (BufferedReader accEventsReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(context.getFilesDir() + "/accEvents" + key + ".csv") )))) {
                            // fileInfoLine (24#2)
                            String accEventsLine = accEventsReader.readLine();
                            // key,lat,lon,ts,bike,childCheckBox,trailerCheckBox,pLoc,incident,i1,i2,i3,i4,i5,i6,i7,i8,i9,scary,desc
                            // 2,52.4251924,13.4405942,1553427047561,0,0,0,0,,,,,,,,,,,,
                            // 20 entries per line (index 0-19)
                            accEventsReader.readLine();
                            while ((accEventsLine = accEventsReader.readLine()) != null) {
                                String[] accEventsArray = accEventsLine.split(",",-1);
                                if(accEventsArray[18].equals("1") && !accEventsArray[8].equals("")) {
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
                updateProfile(context,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,null,-2,totalNumberOfScary);

            } catch (IOException e) {
                Log.d(TAG, "updateToV31() exception: " + e.getLocalizedMessage());
                Log.d(TAG, Arrays.toString(e.getStackTrace()));
            }
            overWriteFile(contentOfNewMetaData.toString(),metaDataFile.getName(),context);
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
                                String[] accEventsArray = accEventsLine.split(",",-1);
                                if(!accEventsArray[8].equals("") && !accEventsArray[8].equals("0")) {
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
            updateProfile(context,-1,-1,-1,-1,-1,-1,totalNumberOfIncidents,-1,-1,-1,null,-2,-1);

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
                    if(!line.startsWith("key") && !line.startsWith("null") && line.split(",",-1).length > 2)
                    contentOfNewMetaData.append(line).append(System.lineSeparator());
                    line = metaDataReader.readLine();
                }
                String fileInfoLine = getAppVersionNumber(context) + "#1" + System.lineSeparator();

                overWriteFile(fileInfoLine + METADATA_HEADER + contentOfNewMetaData.toString(),metaDataFile.getName(),context);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void updateToV50(Context context, int lastAppVersion) {
        if (lastAppVersion < 50) {
            Log.d(TAG,"Updating to version 50");
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
            int region = Utils.lookUpIntSharedPrefs("Region", 0,"Profile",context);
            // change region from London to Berlin
            if (region == 2) {
                Utils.writeIntToSharedPrefs("Region",1,"Profile",context);
            }

        }
    }
}
