package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.util.Log;
import android.util.Pair;

import androidx.appcompat.app.AlertDialog;

import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;

public class Utils {

    private static final String TAG = "Utils_LOG";

    /**
     * Use Utils.overwriteFile(file) instead.
     * Why? For Clarity filename does not say where the file is...
     *
     * @deprecated
     */
    public static String readContentFromFile(String fileName, Context context) {
        File file = new File(IOUtils.Directories.getBaseFolderPath(context) + fileName);
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

    /**
     * Produces the consolidated File which should be uploaded to the backend
     * concatenates content from IncidentLog and DataLog
     * removes incidents that are auto generated
     *
     * @param rideId
     * @param context
     * @return The ride to upload and the filtered incident log to overwrite after successfull upload
     */
    public static Pair<String, IncidentLog> getConsolidatedRideForUpload(int rideId, Context context) {

        StringBuilder content = new StringBuilder();

        IncidentLog incidentLog = IncidentLog.filterIncidentLogUploadReady(IncidentLog.loadIncidentLog(rideId, context));
        String dataLog = DataLog.loadDataLog(rideId, context).toString();

        content.append(incidentLog.toString());
        content.append(System.lineSeparator()).append("=========================").append(System.lineSeparator());
        content.append(dataLog);

        return new Pair<>(content.toString(), incidentLog);
    }

    public static void overwriteFile(String content, File file) {
        try {
            FileOutputStream writer = new FileOutputStream(file);
            writer.write(content.getBytes());
            writer.flush();
            writer.close();
        } catch (IOException ioe) {
            Log.d(TAG, Arrays.toString(ioe.getStackTrace()));
        }
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
            SharedPref.App.Crash.NewCrash.setEnabled(false, context);
            File[] dirFiles = context.getFilesDir().listFiles();
            String path;
            for (File dirFile : dirFiles) {
                path = dirFile.getName();
                if (path.startsWith("CRASH")) {
                    dirFile.delete();
                }
            }
        }
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
     * @param numberOfRegions
     * @param context
     * @return {NumberOfRides,Duration,NumberOfIncidents,WaitedTime,Distance,Co2,0,...,23,NumberOfScary} for each region
     */
    public static Object[][] getRegionProfilesArrays(int numberOfRegions, Context context) {
        Object[][] result = new Object[numberOfRegions][31];
        for (int i = 0; i < result.length; i++) {
            result[i] = getProfileWithoutDemographics("Profile_" + i, context);
        }
        return result;
    }


    public static String[] getRegions(Context context) {
        String[] simRa_regions_config = (Utils.readContentFromFile("simRa_regions.config", context)).split(System.lineSeparator());
        if (simRa_regions_config.length < 8) {
            try {
                AssetManager am = context.getApplicationContext().getAssets();
                InputStream is = am.open("simRa_regions.config");
                InputStreamReader inputStreamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {

                    stringBuilder.append(receiveString.trim()).append(System.lineSeparator());

                }
                is.close();
                simRa_regions_config = stringBuilder.toString().split(System.lineSeparator());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return simRa_regions_config;
    }

    public static boolean isInTimeFrame(Long startTimeBoundary, Long endTimeBoundary, long timestamp) {
        return (startTimeBoundary == null && endTimeBoundary == null) || (endTimeBoundary == null && timestamp >= startTimeBoundary) || (startTimeBoundary == null && timestamp <= endTimeBoundary) || (timestamp >= startTimeBoundary && timestamp <= endTimeBoundary);
    }

    public static List<IncidentLogEntry> findAccEvents(int rideId, Context context) {
        List<AccEvent> accEvents = new ArrayList<>(6);

        // Each String[] in ride is a part of the ride which is approx. 3 seconds long.
        List<String[]> ride = new ArrayList<>();
        List<String[]> events = new ArrayList<>(6);
        accEvents.add(new AccEvent(0, 999.0, 999.0, 0, false, "0", "0"));
        accEvents.add(new AccEvent(1, 999.0, 999.0, 0, false, "0", "0"));
        accEvents.add(new AccEvent(2, 999.0, 999.0, 0, false, "0", "0"));
        accEvents.add(new AccEvent(3, 999.0, 999.0, 0, false, "0", "0"));
        accEvents.add(new AccEvent(4, 999.0, 999.0, 0, false, "0", "0"));
        accEvents.add(new AccEvent(5, 999.0, 999.0, 0, false, "0", "0"));

        String[] template = {"0.0", "0.0", "0.0", "0.0", "0.0", "0"};
        events.add(template);
        events.add(template);
        events.add(template);
        events.add(template);
        events.add(template);
        events.add(template);
        try {
            BufferedReader br = new BufferedReader(new FileReader(IOUtils.Files.getGPSLogFile(rideId, false, context)));
            br.readLine();
            br.readLine();
            String thisLine = br.readLine();
            String nextLine = br.readLine();

            String[] partOfRide;
            boolean newSubPart = false;
            // Loops through all lines. If the line starts with lat and lon, it is consolidated into
            // a part of a ride together with the subsequent lines that don't have lat and lon.
            // Then, it takes the top two X-, Y- and Z-deltas and creates AccEvents from them.
            while ((thisLine != null) && (!newSubPart)) {
                String[] currentLine = thisLine.split(",");
                // currentLine: {lat, lon, maxXDelta, maxYDelta, maxZDelta, timeStamp}
                partOfRide = new String[6];
                String lat = currentLine[0];
                String lon = currentLine[1];
                String timeStamp = currentLine[5];

                partOfRide[0] = lat; // lat
                partOfRide[1] = lon; // lon
                partOfRide[2] = "0"; // maxXDelta
                partOfRide[3] = "0"; // maxYDelta
                partOfRide[4] = "0"; // maxZDelta
                partOfRide[5] = timeStamp; // timeStamp

                double maxX = Double.parseDouble(currentLine[2]);
                double minX = Double.parseDouble(currentLine[2]);
                double maxY = Double.parseDouble(currentLine[3]);
                double minY = Double.parseDouble(currentLine[3]);
                double maxZ = Double.parseDouble(currentLine[4]);
                double minZ = Double.parseDouble(currentLine[4]);
                thisLine = nextLine;

                nextLine = br.readLine();
                if (thisLine != null && thisLine.startsWith(",,")) {
                    newSubPart = true;
                }

                while ((thisLine != null) && newSubPart) {

                    currentLine = thisLine.split(",");
                    if (Double.parseDouble(currentLine[2]) >= maxX) {
                        maxX = Double.parseDouble(currentLine[2]);
                    } else if (Double.parseDouble(currentLine[2]) < minX) {
                        minX = Double.parseDouble(currentLine[2]);
                    }
                    if (Double.parseDouble(currentLine[3]) >= maxY) {
                        maxY = Double.parseDouble(currentLine[3]);
                    } else if (Double.parseDouble(currentLine[3]) < minY) {
                        minY = Double.parseDouble(currentLine[3]);
                    }
                    if (Double.parseDouble(currentLine[4]) >= maxZ) {
                        maxZ = Double.parseDouble(currentLine[4]);
                    } else if (Double.parseDouble(currentLine[4]) < minZ) {
                        minZ = Double.parseDouble(currentLine[4]);
                    }
                    thisLine = nextLine;
                    nextLine = br.readLine();

                    if (thisLine != null && !thisLine.startsWith(",,")) {
                        newSubPart = false;
                    }
                }

                double maxXDelta = Math.abs(maxX - minX);
                double maxYDelta = Math.abs(maxY - minY);
                double maxZDelta = Math.abs(maxZ - minZ);

                partOfRide[2] = String.valueOf(maxXDelta);
                partOfRide[3] = String.valueOf(maxYDelta);
                partOfRide[4] = String.valueOf(maxZDelta);

                ride.add(partOfRide);

                // Checks whether there is a minimum of <threshold> milliseconds
                // between the actual event and the top 6 events so far.
                int threshold = 10000; // 10 seconds
                long minTimeDelta = 999999999;
                for (int i = 0; i < events.size(); i++) {
                    long actualTimeDelta = Long.parseLong(partOfRide[5]) - Long.parseLong(events.get(i)[5]);
                    if (actualTimeDelta < minTimeDelta) {
                        minTimeDelta = actualTimeDelta;
                    }
                }
                boolean enoughTimePassed = minTimeDelta > threshold;

                // Check whether actualX is one of the top 2 events
                boolean eventAdded = false;
                if (maxXDelta > Double.parseDouble(events.get(0)[2]) && !eventAdded && enoughTimePassed) {

                    String[] temp = events.get(0);
                    events.set(0, partOfRide);
                    accEvents.set(0, new AccEvent(0, Double.parseDouble(partOfRide[0]), Double.parseDouble(partOfRide[1]), Long.parseLong(partOfRide[5]), false, "0", "0"));

                    events.set(1, temp);
                    accEvents.set(1, new AccEvent(1, Double.parseDouble(temp[0]), Double.parseDouble(temp[1]), Long.parseLong(temp[5]), false, "0", "0"));
                    eventAdded = true;
                } else if (maxXDelta > Double.parseDouble(events.get(1)[2]) && !eventAdded && enoughTimePassed) {

                    events.set(1, partOfRide);
                    accEvents.set(1, new AccEvent(1, Double.parseDouble(partOfRide[0]), Double.parseDouble(partOfRide[1]), Long.parseLong(partOfRide[5]), false, "0", "0"));
                    eventAdded = true;
                }
                // Check whether actualY is one of the top 2 events
                else if (maxYDelta > Double.parseDouble(events.get(2)[3]) && !eventAdded && enoughTimePassed) {

                    String[] temp = events.get(2);
                    events.set(2, partOfRide);
                    accEvents.set(2, new AccEvent(2, Double.parseDouble(partOfRide[0]), Double.parseDouble(partOfRide[1]), Long.parseLong(partOfRide[5]), false, "0", "0"));
                    events.set(3, temp);
                    accEvents.set(3, new AccEvent(3, Double.parseDouble(temp[0]), Double.parseDouble(temp[1]), Long.parseLong(temp[5]), false, "0", "0"));
                    eventAdded = true;

                } else if (maxYDelta > Double.parseDouble(events.get(3)[3]) && !eventAdded && enoughTimePassed) {
                    events.set(3, partOfRide);
                    accEvents.set(3, new AccEvent(3, Double.parseDouble(partOfRide[0]), Double.parseDouble(partOfRide[1]), Long.parseLong(partOfRide[5]), false, "0", "0"));
                    eventAdded = true;
                }
                // Check whether actualZ is one of the top 2 events
                else if (maxZDelta > Double.parseDouble(events.get(4)[4]) && !eventAdded && enoughTimePassed) {
                    String[] temp = events.get(4);
                    events.set(4, partOfRide);
                    accEvents.set(4, new AccEvent(4, Double.parseDouble(partOfRide[0]), Double.parseDouble(partOfRide[1]), Long.parseLong(partOfRide[5]), false, "0", "0"));
                    events.set(5, temp);
                    accEvents.set(5, new AccEvent(5, Double.parseDouble(temp[0]), Double.parseDouble(temp[1]), Long.parseLong(temp[5]), false, "0", "0"));

                } else if (maxZDelta > Double.parseDouble(events.get(5)[4]) && !eventAdded && enoughTimePassed) {
                    events.set(5, partOfRide);
                    accEvents.set(5, new AccEvent(5, Double.parseDouble(partOfRide[0]), Double.parseDouble(partOfRide[1]), Long.parseLong(partOfRide[5]), false, "0", "0"));
                    eventAdded = true;
                }

                if (nextLine == null) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<IncidentLogEntry> incidents = new ArrayList<>();
        for (AccEvent accEvent : accEvents) {
            if (!(accEvent.position.getLatitude() == 999 || accEvent.position.getLatitude() == 0f)) {
                incidents.add(IncidentLogEntry.newBuilder().withIncidentType(IncidentLogEntry.INCIDENT_TYPE.AUTO_GENERATED).withBaseInformation(accEvent.timeStamp, accEvent.position.getLatitude(), accEvent.position.getLongitude()).build());
            }
        }

        return incidents;
    }

    /**
     * @deprecated Use IncidentLogEntry instead
     */
    private static class AccEvent {

        public long timeStamp;
        public GeoPoint position;
        public int key = 999;              // when an event doesn't have a key yet, the key is 999
        // (can't use 0 because that's an actual valid key)
        public String incidentType = "-1";
        public String scary = "0"; // default is non-scary
        public boolean annotated = false;  // events are labeled as not annotated when first created.
        String TAG = "AccEvent_LOG";

        public AccEvent(int key, double lat, double lon, long timeStamp, boolean annotated, String incidentType, String scary) {
            this.key = key;
            this.position = new GeoPoint(lat, lon);
            this.timeStamp = timeStamp;
            this.annotated = annotated;
            this.incidentType = incidentType;
            this.scary = scary;
        }


        public long getTimeStamp() {
            return this.timeStamp;
        }
    }
}
