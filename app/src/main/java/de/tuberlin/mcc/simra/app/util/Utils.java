package de.tuberlin.mcc.simra.app.util;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import javax.net.ssl.HttpsURLConnection;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.Profile;

import static de.tuberlin.mcc.simra.app.activities.ProfileActivity.startProfileActivityForChooseRegion;
import static de.tuberlin.mcc.simra.app.entities.IncidentLog.getEventsFile;
import static de.tuberlin.mcc.simra.app.util.IOUtils.Directories.getSharedPrefsDirectory;
import static de.tuberlin.mcc.simra.app.util.IOUtils.zip;
import static de.tuberlin.mcc.simra.app.util.SimRAuthenticator.getClientHash;

public class Utils {

    private static final String TAG = "Utils_LOG";

    /**
     * @return content from file with given fileName as a String
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
     * @return The ride to upload and the filtered incident log to overwrite after successful upload
     */
    public static Pair<String, IncidentLog> getConsolidatedRideForUpload(int rideId, Context context) {

        StringBuilder content = new StringBuilder();

        File incidentFile = getEventsFile(rideId, context);

        if (incidentFile.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(incidentFile))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line).append(System.lineSeparator());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        IncidentLog incidentLog = IncidentLog.filterIncidentLogUploadReady(IncidentLog.loadIncidentLogFromFileOnly(rideId, context),null,null,null,null,true);
        String dataLog = DataLog.loadDataLog(rideId, context).toString();

        // content.append(incidentLog.toString());
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

    public static String[] getRegions(Context context) {
        String[] simRa_regions_config = (Utils.readContentFromFile("simRa_regions.config", context)).split(System.lineSeparator());

        return simRa_regions_config;
    }

    /**
     *
     * @param context
     * @return String[], where each element is one news element.
     */
    public static String[] getNews(Context context) {
        String locale = Resources.getSystem().getConfiguration().locale.getLanguage();
        boolean languageIsEnglish = locale.equals(new Locale("en").getLanguage());
        if (languageIsEnglish) {
            return readContentFromFile(IOUtils.Files.getENNewsFile(context).getName(),context).split(System.lineSeparator());
        } else {
            return readContentFromFile(IOUtils.Files.getDENewsFile(context).getName(),context).split(System.lineSeparator());
        }
    }

    public static boolean isInTimeFrame(Long startTimeBoundary, Long endTimeBoundary, long timestamp) {
        return (startTimeBoundary == null && endTimeBoundary == null) || (endTimeBoundary == null && timestamp >= startTimeBoundary) || (startTimeBoundary == null && timestamp <= endTimeBoundary) || (timestamp >= startTimeBoundary && timestamp <= endTimeBoundary);
    }

    // co2 savings on a bike: 138g/km
    public static long calculateCO2Savings(Long totalDistance) {
        return (long) ((totalDistance / (float) 1000) * 138);
    }

    // returns the incidents to be proposed and the neuronal network version that calculated the incidents (-1 if local algorithm was used)
    public static Pair<List<IncidentLogEntry>, Integer> findAccEvents(int rideId, int bike, int pLoc, int state, Context context) {
        List<IncidentLogEntry> foundEvents = null;
        Integer nn_version = 0;
        if (SharedPref.Settings.IncidentGenerationAIActive.getAIEnabled(context)) {
            Pair<List<IncidentLogEntry>, Integer> findAccEventOnlineResult = findAccEventOnline(rideId, bike, pLoc, context);
            foundEvents = findAccEventOnlineResult.first;
            nn_version = findAccEventOnlineResult.second;
        }
        if (foundEvents != null && foundEvents.size() > 0)
            return new Pair<>(foundEvents, nn_version);
        else
            return findAccEventsLocal(rideId, state, context);
    }

    /*
     * Uses sophisticated AI to analyze the ride
     * */
    public static Pair<List<IncidentLogEntry>, Integer> findAccEventOnline(int rideId, int bike, int pLoc, Context context) {
        try {
            String responseString = "";

            URL url = new URL(BuildConfig.API_ENDPOINT + BuildConfig.API_VERSION + "classify-ride-cyclesense?clientHash=" + getClientHash(context)
                    + "&os=android");

            Log.d(TAG, "URL for AI-Backend: " + url.toString());
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "text/plain");
            urlConnection.setRequestProperty("Accept","*/*");
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(30000);
            urlConnection.setConnectTimeout(30000);

            //Read log file in to byte Array
            File rideFile = IOUtils.Files.getGPSLogFile(rideId, false, context);
            FileInputStream fileInputStream = new FileInputStream(rideFile);
            long byteLength = rideFile.length();

            byte[] fileContent = new byte[(int) byteLength];
            fileInputStream.read(fileContent, 0, (int) byteLength);

            //upload byteArr
            Log.d(TAG, "send data: ");
            try (OutputStream os = urlConnection.getOutputStream()) {
                long startTime = System.currentTimeMillis();
                long uploadTimeoutMS = 30000;
                int chunkSize = 1024;
                int chunkIndex = 0;

                while (chunkSize * chunkIndex < fileContent.length) {
                    int offset = chunkSize * chunkIndex;
                    int remaining = fileContent.length - offset;
                    os.write(fileContent, offset, remaining > chunkSize ? chunkSize : remaining);
                    chunkIndex += 1;

                    //upload timeout
                    if(startTime + uploadTimeoutMS < System.currentTimeMillis())
                        return null;
                }

                os.flush();
                os.close();
            }

            // receive results
            Log.d(TAG, "receive data: ");
            int status = urlConnection.getResponseCode();
            Log.d(TAG, "Server status: " + status);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream()
                    ));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseString += inputLine;
            }
            in.close();

            Log.d(TAG, "Server Message: " + responseString);

            // response okay
            if (status == 200 && responseString.length() > 1) {
                JSONArray incidentTimestamps = new JSONArray(responseString);
                Integer nn_version = (Integer) incidentTimestamps.remove(0); // remove first element since it is the nn_version
                List<IncidentLogEntry> foundIncidents = new ArrayList<>();
                DataLog allLogs = DataLog.loadDataLog(rideId, context);

                // for each gps data log entries loop through the incident timestamps and create an incident at position, if the timestamps match
                int key = 0;
                int index = 0;
                while (!allLogs.onlyGPSDataLogEntries.isEmpty() && incidentTimestamps.length() > 0 && index < allLogs.onlyGPSDataLogEntries.size()) {
                    DataLogEntry gpsLine = allLogs.onlyGPSDataLogEntries.get(index);
                    for (int i = 0; i < incidentTimestamps.length(); i++) {
                        if(gpsLine.timestamp == incidentTimestamps.getLong(i)) {
                            foundIncidents.add(IncidentLogEntry.newBuilder()
                                    .withBaseInformation(gpsLine.timestamp, gpsLine.latitude, gpsLine.longitude)
                                    .withIncidentType(IncidentLogEntry.INCIDENT_TYPE.AUTO_GENERATED)
                                    .withKey(key++)
                                    .build());
                            incidentTimestamps.remove(index);
                        }
                    }
                    index++;
                }
                return new Pair<>(foundIncidents, nn_version);
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return new Pair<>(null,-2);
    }


    public static Pair<List<IncidentLogEntry>, Integer> findAccEventsLocal(int rideId, int state, Context context) {
        Log.d(TAG, "findAccEventsLocal()");
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
        } catch (NumberFormatException e) {
            e.printStackTrace();
            fixRide(rideId, context);
            Log.d(TAG,"fixed ride");
            List<IncidentLogEntry> dummyEntryList = new ArrayList<>();
            IncidentLogEntry dummyIncident = IncidentLogEntry.newBuilder().withDescription("startShowRouteActivity").build();
            dummyEntryList.add(dummyIncident);
            return new Pair<>(dummyEntryList,-1);
        }

        List<IncidentLogEntry> incidents = new ArrayList<>();
        int key = 0;
        for (AccEvent accEvent : accEvents) {
            if (!(accEvent.position.getLatitude() == 999 || accEvent.position.getLatitude() == 0f)) {
                incidents.add(IncidentLogEntry.newBuilder().withIncidentType(IncidentLogEntry.INCIDENT_TYPE.AUTO_GENERATED).withBaseInformation(accEvent.timeStamp, accEvent.position.getLatitude(), accEvent.position.getLongitude()).withKey(key++).build());
            }
        }

        return new Pair<>(incidents,0);
    }

    private static void fixRide(int rideId, Context context) {
        try {
            StringBuilder fixedRideContent = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(IOUtils.Files.getGPSLogFile(rideId, false, context)));
            String line = br.readLine(); // fileInfo line
            fixedRideContent.append(line).append(System.lineSeparator());
            line = br.readLine(); // csv header
            fixedRideContent.append(line).append(System.lineSeparator());
            // skip to first GPS Line
            while ((line = br.readLine()) != null) {
                if (!line.startsWith(",,")){
                    break;
                }
            }
            fixedRideContent.append(line).append(System.lineSeparator());
            while ((line = br.readLine()) != null) {
                fixedRideContent.append(line).append(System.lineSeparator());
            }
            overwriteFile(fixedRideContent.toString(),IOUtils.Files.getGPSLogFile(rideId, false, context));
        } catch (IOException e) {
            Log.e(TAG, "fixRide() - Exception: " + e.getMessage());
            Log.e(TAG, Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }

    }

    public static String mergeGPSandSensorLines(Queue<DataLogEntry> gpsLines, Queue<DataLogEntry> sensorLines) {
        StringBuilder accGpsString = new StringBuilder();

        while(!gpsLines.isEmpty() || !sensorLines.isEmpty()) {
            DataLogEntry gpsLine = gpsLines.peek();
            DataLogEntry sensorLine = sensorLines.peek();
            long gpsTS = gpsLine != null ? gpsLine.timestamp : Long.MAX_VALUE;
            long sensorTS = sensorLine != null ? sensorLine.timestamp : Long.MAX_VALUE;
            if (gpsTS <= sensorTS) {
                accGpsString.append(gpsLines.poll().stringifyDataLogEntry()).append(System.lineSeparator());
            } else {
                accGpsString.append(sensorLines.poll().stringifyDataLogEntry()).append(System.lineSeparator());
            }
        }

        return accGpsString.toString();
    }

    /**
     * calculates the nearest three regions to given location
     * @param lat Latitude of current location
     * @param lon Longitude of current location
     * @param context
     * @return int array with the nearest three regions to the location represented by their region IDs
     */
    public static int[] nearestRegionsToThisLocation(double lat, double lon, Context context) {
        int[] result = {Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE};
        double[] top3Distances = {Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE};
        String[] simRa_regions_config = getRegions(context);

        for (int i = 0; i < simRa_regions_config.length; i++) {
            String s = simRa_regions_config[i];
            if (s.split("=").length>3) {
                String latlong = s.split("=")[3];
                double regionLat = Double.parseDouble(latlong.split(",")[0]);
                double regionLon = Double.parseDouble(latlong.split(",")[1]);
                GeoPoint location = new GeoPoint(lat, lon);
                GeoPoint thisRegionCenter = new GeoPoint(regionLat, regionLon);
                double distance = location.distanceToAsDouble(thisRegionCenter);
                if(distance < top3Distances[0]) {
                    top3Distances[2] = top3Distances[1];
                    result[2] = result[1];
                    top3Distances[1] = top3Distances[0];
                    result[1] = result[0];
                    top3Distances[0] = distance;
                    result[0] = i;
                } else if (distance < top3Distances[1]) {
                    top3Distances[2] = top3Distances[1];
                    result[2] = result[1];
                    top3Distances[1] = distance;
                    result[1] = i;
                } else if (distance < top3Distances[2]) {
                    top3Distances[2] = distance;
                    result[2] = i;
                }
            }
        }

        return result;
    }

    /**
     * Converts region IDs to their respective names
     * @param regionCodes int array with the region codes e.g. {2,3,5}
     * @param context context (activity) needed for reading regions file
     * @return string array with the region names of given int array in the same order
     */
    public static String[] regionsDecoder(int[] regionCodes, Context context) {
        String[] result = new String[regionCodes.length];
        String[] region = getRegions(context);
        for (int i = 0; i < regionCodes.length; i++) {
            result[i] = region[regionCodes[i]];
        }
        return result;
    }

    /**
     * Converts names to their respective region IDs
     * @param regionName string array with the region IDs e.g., {Berlin/Potsdam, Leipzig, Stuttgart}
     * @param context context (activity) needed for reading regions file
     * @return int array with the region IDs of given string array in the same order
     */
    public static int regionEncoder(String regionName, Context context) {
        String[] region = getRegions(context);
        for (int i = 0; i < region.length; i++) {
            if (regionName.equals(getCorrectRegionName(region[i]))) {
                return i;
            }
        }
        return -1; // region not found
    }

    /**
     * Gets the correct region names from the region lines of the region file. German or English
     * @param regionLines a subset of getRegions()
     * @return the correct display names in a string array according to System locale.
     */
    public static String[] getCorrectRegionNames(String[] regionLines) {
        String[] result = new String[regionLines.length];
        for (int i = 0; i < regionLines.length; i++) {
            result[i] = getCorrectRegionName(regionLines[i]);
        }
        return result;
    }

    /**
     * Gets the correct region name from the region line of the region file. German or English
     * @param regionLine a region line from getRegions() e.g., Munich=München=München=48.13,11.57
     * @return the correct display name as a string according to System locale
     */
    public static String getCorrectRegionName(String regionLine) {
        String locale = Resources.getSystem().getConfiguration().locale.getLanguage();
        boolean languageIsEnglish = locale.equals(new Locale("en").getLanguage());
        if (languageIsEnglish) {
            return regionLine.split("=")[0];
        } else {
            return regionLine.split("=")[1];
        }
    }

    public static void fireProfileRegionPrompt(int regionsID, Context context) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(context.getString(R.string.chooseRegion));
        CharSequence pleaseChooseRegionText = context.getText(R.string.pleaseChooseRegion);
        CharSequence chosenRegion = getCorrectRegionName(regionsDecoder(new int[]{Profile.loadProfile(null, context).region},context)[0]);

        alert.setMessage( pleaseChooseRegionText + System.lineSeparator() + chosenRegion);
        // alert.setMessage(R.string.pleaseChooseRegion);
        alert.setPositiveButton(R.string.selectRegion, (dialogInterface, j) -> {
            SharedPref.App.News.setLastSeenNewsID(regionsID,context);
            SharedPref.App.RegionsPrompt.setRegionPromptShownAfterV81(true,context);
            startProfileActivityForChooseRegion(context);
        });

        alert.setNeutralButton(R.string.doNotShowAgain, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPref.App.RegionsPrompt.setDoNotShowRegionPrompt(true,context);
                SharedPref.App.RegionsPrompt.setRegionPromptShownAfterV81(true,context);
                SharedPref.App.News.setLastSeenNewsID(regionsID,context);
            }
        });
        alert.setNegativeButton(R.string.later,null);
        alert.show();
    }

    /**
     * checks whether location provider is enabled
     * @param locationManager
     * @return true if the gps provider is disabled, false, if it is enabled
     */
    public static boolean isLocationServiceOff(LocationManager locationManager) {
        boolean gps_enabled = false;
        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            Log.d(TAG, ex.getMessage());
        }
        return (!gps_enabled);
    }

    public static void prepareDebugZip(int mode,List<File> ridesAndAccEvents , Context context) {
        List<File> filesToUpload = new ArrayList<File>(ridesAndAccEvents);
        if (mode == 2) {
            filesToUpload.clear();
        } else if (mode == 1) {
            while (filesToUpload.size()>20) {
                filesToUpload.remove(0);
            }
        }
        filesToUpload.add(IOUtils.Files.getMetaDataFile(context));
        Log.d(TAG, "prepareDebugZip - filesToUpload: " + filesToUpload);
        filesToUpload.addAll(Arrays.asList(getSharedPrefsDirectory(context).listFiles()));
        Log.d(TAG, "prepareDebugZip - filesToUpload: " + filesToUpload);
        try {
            zip(filesToUpload,new File(IOUtils.Directories.getBaseFolderPath(context) + "zip.zip"));
        } catch (IOException e) {
            Log.e(TAG, "prepareDebugZip - Exception: " + e.getMessage());
            Log.e(TAG, Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    public static void sortFileListLastModified(List<File> fileList) {
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                long k = file1.lastModified() - file2.lastModified();
                if(k > 0){
                    return 1;
                }else if(k == 0){
                    return 0;
                }else{
                    return -1;
                }
            }
        });
    }

    /**
     * @deprecated Use IncidentLogEntry  instead
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
    /**
     * Prompts user to enable Bluetooth or deactivate OBS.
     * Gets called, if OBS is enabled in the settings but Bluetooth is disabled, so SimRa cannot
     * connect to OBS.
     */
    public static void showBluetoothNotEnableWarning(ActivityResultLauncher<Intent> activityResultLauncher, Context ctx) {
        android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(ctx);
        alert.setTitle(R.string.bluetooth_not_enable_title);
        alert.setMessage(R.string.bluetooth_not_enable_message);
        alert.setPositiveButton(R.string.yes, (dialog, whichButton) -> {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activityResultLauncher.launch(enableBtIntent);
        });
        alert.setNegativeButton(R.string.no, (dialog, whichButton) -> {

        });
        alert.show();
    }

    public static ActivityResultLauncher<Intent> activityResultLauncher(Activity activity) {
        return ((ComponentActivity)activity).registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.e(TAG, "Activity result: OK");
                        // There are no request codes
                        Intent data = result.getData();
                        Log.d(TAG, "data: " + data);
                    }
                }
            }
    );
    }
}
