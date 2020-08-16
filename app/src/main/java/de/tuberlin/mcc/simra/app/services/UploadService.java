package de.tuberlin.mcc.simra.app.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.Profile;
import de.tuberlin.mcc.simra.app.util.Constants;
import de.tuberlin.mcc.simra.app.util.ForegroundServiceNotificationManager;
import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.SimRAuthenticator;
import de.tuberlin.mcc.simra.app.util.Utils;

import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.writeIntToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.writeToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.getRegions;
import static de.tuberlin.mcc.simra.app.util.Utils.readContentFromFile;

public class UploadService extends Service {

    public static final String TAG = "UploadService_LOG:";
    boolean foundARideToUpload = false;
    private PowerManager.WakeLock wakeLock = null;
    private boolean uploadSuccessful = false;
    private IBinder mBinder = new UploadService.MyBinder();

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":RecorderService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ForegroundServiceNotificationManager.cancelNotification(this);
        wakeLock.release();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand() called");


        Notification notification =
                ForegroundServiceNotificationManager.createOrUpdateNotification(
                        this,
                        getResources().getString(R.string.uploadingNotificationTitle),
                        getResources().getString(R.string.uploadingNotificationBody)
                );
        startForeground(ForegroundServiceNotificationManager.getNotificationId(), notification);
        wakeLock.acquire(1000 * 60 * 15);

        new UpdateTask(this, intent).execute();
        return Service.START_NOT_STICKY;
    }

    public class MyBinder extends Binder {
        public UploadService getService() {
            return UploadService.this;
        }
    }

    private class UpdateTask extends AsyncTask<String, String, String> {

        private Context context;
        private Intent intent;

        private UpdateTask(Context context, Intent intent) {
            this.context = context;
            this.intent = intent;
        }

        protected String doInBackground(String... urls) {

            Log.d(TAG, "doInBackground()");

            try {
                uploadFile(context);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (foundARideToUpload) {
                Intent intent = new Intent();
                intent.setAction("de.tuberlin.mcc.simra.app.UPLOAD_COMPLETE");
                intent.putExtra("uploadSuccessful", uploadSuccessful);
                intent.putExtra("foundARideToUpload", foundARideToUpload);
                sendBroadcast(intent);
            }
            stopForeground(true);
        }

        private void uploadFile(Context context) throws IOException {
            File[] dirFiles = new File(IOUtils.Directories.getBaseFolderPath(context)).listFiles();

            boolean crash = intent.getBooleanExtra("CRASH_REPORT", false);
            // If there was a crash and the user permitted to send the crash logs, upload simraPrefs and crash log
            if (crash) {

                for (File dirFile : dirFiles) {
                    String path = dirFile.getName();
                    if (!((new File(path)).isDirectory()) && path.startsWith("CRASH")) {
                        String contentToSend = readContentFromFile(path, context);
                        postFile("crash", contentToSend, -1);
                        context.deleteFile(path);
                    }
                }
                // set the boolean "NEW-UNSENT-ERROR" in simraPrefs.xml to false
                // so that the StartActivity doesn't think there are still unsent
                // crash logs.
                SharedPref.App.Crash.NewCrash.setEnabled(false, context);

                // If there wasn't a crash or the user did not gave us the permission, upload ride(s)
            } else {
                Profile globalProfile = Profile.loadProfile(null, context);

                int numberOfRegions = getRegions(context).length;
                boolean[] regionProfileUpdated = new boolean[numberOfRegions];

                List<Profile> regionProfilesList = new ArrayList<>();
                for (int i = 0; i < numberOfRegions; i++) {
                    regionProfilesList.add(Profile.loadProfile(i, context));
                }

                String fileVersion = "";
                StringBuilder metaDataContent = new StringBuilder();

                MetaData metaData = MetaData.loadMetaData(context);
                Iterator it = mp.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    System.out.println(pair.getKey() + " = " + pair.getValue());
                    it.remove(); // avoids a ConcurrentModificationException
                }

                try {
                    BufferedReader metaDataReader = new BufferedReader(new FileReader(IOUtils.Files.getMetaDataFile(context)));
                    String line;
                    fileVersion = metaDataReader.readLine().split("#")[1];

                    // skip header
                    metaDataReader.readLine();
                    // loop through lines of metaData.csv to find rides ready for upload (state = 1)
                    while ((line = metaDataReader.readLine()) != null) {

                        // key,startTime,endTime,state,numberOfIncidents,waitedTime,distance,numberOfScary,region
                        // 17,1558605594337,1558606191224,2,1,180,648,0,1
                        String[] metaDataLine = line.split(",", -1);
                        Log.d(TAG, "metaDataLine: " + Arrays.toString(metaDataLine));
                        // found a ride which is ready to upload in metaData.csv
                        if (metaDataLine.length > 1 && metaDataLine[3].equals(String.valueOf(MetaData.STATE.ANNOTATED))) {
                            foundARideToUpload = true;
                            String rideKey = metaDataLine[0];

                            // concatenate fileInfoVersion, accEvents and accGps content
                            Pair<String, IncidentLog> contentToUploadAndAccEventsContentToOverwrite = Utils.getConsolidatedRideForUpload(Integer.parseInt(rideKey), context);

                            String contentToUpload = contentToUploadAndAccEventsContentToOverwrite.first;
                            IncidentLog incidentLogToOverwrite = contentToUploadAndAccEventsContentToOverwrite.second;

                            String password = lookUpSharedPrefs(rideKey, "-1", "keyPrefs", context);

                            int region = Integer.parseInt(metaDataLine[8]);
                            Profile regionProfile = regionProfilesList.get(region);

                            Log.d(TAG, "Saved password: " + password);
                            Pair<Integer, String> response;
                            // send data with POST, if it is being sent the first time
                            if (password.equals("-1")) {
                                Log.d(TAG, "sending ride with POST: " + rideKey);
                                response = postFile("ride", contentToUpload, region);

                                if (response.second.split(",").length >= 2) {
                                    writeToSharedPrefs(rideKey, response.second, "keyPrefs", context);
                                }
                                Log.d(TAG, "hashPassword: " + response + " written to keyPrefs");
                                // send data with PUT, if it is being overwritten on the server
                            } else {
                                Log.d(TAG, "sending ride with PUT: " + rideKey);
                                String fileHash = password.split(",")[0];
                                String filePassword = password.split(",")[1];
                                response = putFile("ride", fileHash, filePassword, contentToUpload, region);
                                Log.d(TAG, "PUT response: " + response);
                            }


                            // if the respond is ok, mark ride as uploaded in metaData.csv
                            if (response.first.equals(200)) {
                                // Delete "nothing" events from accEvents.csv
                                IncidentLog.saveIncidentLog(incidentLogToOverwrite, context);

                                metaDataLine[3] = String.valueOf(MetaData.STATE.SYNCED);
                                globalProfile.numberOfRides++;
                                globalProfile.duration += (Long.parseLong(metaDataLine[2]) - Long.parseLong(metaDataLine[1]));
                                globalProfile.numberOfIncidents += Integer.parseInt(metaDataLine[4]);
                                globalProfile.waitedTime += Long.parseLong(metaDataLine[5]);
                                globalProfile.distance += Long.parseLong(metaDataLine[6]);
                                globalProfile.co2 += (long) ((Long.parseLong(metaDataLine[6]) / (float) 1000) * 138);
                                globalProfile.numberOfScaryIncidents += Integer.parseInt(metaDataLine[7]);


                                // update the timebuckets
                                Date startDate = new Date(Long.parseLong(metaDataLine[1]));
                                Date endDate = new Date(Long.parseLong(metaDataLine[2]));
                                Locale locale = Resources.getSystem().getConfiguration().locale;
                                SimpleDateFormat sdf = new SimpleDateFormat("HH", locale);
                                int startHour = Integer.parseInt(sdf.format(startDate));
                                int endHour = Integer.parseInt(sdf.format(endDate));
                                float durationOfThisRide = endHour - startHour + 1;

                                for (int i = startHour; i <= endHour; i++) {
                                    // for global profile
                                    globalProfile.timeDistribution.set(i, globalProfile.timeDistribution.get(i).floatValue() + (1 / durationOfThisRide));
                                    // for region profiles
                                    regionProfile.timeDistribution.set(i, regionProfile.timeDistribution.get(i).floatValue() + (1 / durationOfThisRide));
                                }


                                regionProfile.numberOfRides++;
                                regionProfile.duration += (Long.parseLong(metaDataLine[2]) - Long.parseLong(metaDataLine[1]));
                                regionProfile.numberOfIncidents += Integer.parseInt(metaDataLine[4]);
                                regionProfile.waitedTime += Long.parseLong(metaDataLine[5]);
                                regionProfile.distance += Long.parseLong(metaDataLine[6]);
                                regionProfile.co2 += (long) ((Long.parseLong(metaDataLine[6]) / (float) 1000) * 138);
                                regionProfile.numberOfScaryIncidents = Integer.parseInt(metaDataLine[7]);
                                regionProfileUpdated[region] = true;
                                regionProfilesList.set(region, regionProfile);
                            }

                        }
                        // update metaData.csv. change status from 1 to 2, if upload was successful
                        for (int i = 0; i < metaDataLine.length; i++) {
                            if (i == metaDataLine.length - 1) {
                                metaDataContent.append(metaDataLine[i]).append(System.lineSeparator());
                            } else {
                                metaDataContent.append(metaDataLine[i]).append(",");
                            }
                        }
                        //content.append((Arrays.toString(metaDataLine).replace("[","")
                        //      .replace(", ",",").replace("]","")) + System.lineSeparator());
                    }
                    if (!foundARideToUpload) {
                        Intent intent = new Intent();
                        intent.setAction("de.tuberlin.mcc.simra.app.UPLOAD_COMPLETE");
                        intent.putExtra("uploadSuccessful", uploadSuccessful);
                        intent.putExtra("foundARideToUpload", foundARideToUpload);
                        sendBroadcast(intent);
                        stopSelf();
                        return;
                    }
                    String fileInfoLine = BuildConfig.VERSION_CODE + "#" + fileVersion + System.lineSeparator();
                    Utils.overwriteFile((fileInfoLine + MetaData.METADATA_HEADER + System.lineSeparator() + metaDataContent.toString()), IOUtils.Files.getMetaDataFile(context));
                } catch (IOException e) {
                    e.printStackTrace();
                }


                // Now after the rides have been uploaded, we can update the profile with the new statistics
                Profile.saveProfile(globalProfile, null, context);

                // update region profiles
                for (int p = 0; p < regionProfilesList.toArray().length; p++) {
                    if (regionProfileUpdated[p]) {
                        Profile regionProfile = regionProfilesList.get(p);

                        Profile.saveProfile(regionProfile, p, context);

                        int profileVersion = lookUpIntSharedPrefs("Version", 1, "Profile_" + p, context);
                        StringBuilder profileContentToSend = new StringBuilder().append(BuildConfig.VERSION_CODE).append("#").append(profileVersion).append(System.lineSeparator());
                        profileContentToSend
                                .append(Constants.PROFILE_HEADER)
                                .append(globalProfile.ageGroup).append(",")
                                .append(globalProfile.gender).append(",")
                                .append(globalProfile.region).append(",")
                                .append(globalProfile.experience).append(",")
                                .append(regionProfile.numberOfRides).append(",")
                                .append(regionProfile.duration).append(",")
                                .append(regionProfile.numberOfIncidents).append(",")
                                .append(regionProfile.waitedTime).append(",")
                                .append(regionProfile.distance).append(",")
                                .append(regionProfile.co2).append(",");
                        for (int i = 0; i < 24; i++) {
                            profileContentToSend.append(regionProfile.timeDistribution.get(i)).append(",");
                        }
                        profileContentToSend.append(globalProfile.behaviour).append(",");
                        profileContentToSend.append(regionProfile.numberOfScaryIncidents);

                        String profilePassword = lookUpSharedPrefs("Profile_" + p, "-1", "keyPrefs", context);
                        Log.d(TAG, "Saved password: " + profilePassword);
                        if (profilePassword.equals("-1")) {
                            Log.d(TAG, "sending profile with POST: " + profileContentToSend.toString());
                            String hashPassword = postFile("profile", profileContentToSend.toString(), p).second;
                            if (hashPassword.split(",").length >= 2) {
                                writeToSharedPrefs("Profile_" + regionProfileUpdated[p], hashPassword, "keyPrefs", context);
                                writeIntToSharedPrefs("Version", profileVersion + 1, "Profile", context);
                            }
                            Log.d(TAG, "hashPassword: " + hashPassword + " written to keyPrefs");
                        } else {
                            Log.d(TAG, "sending profile with PUT: " + profileContentToSend.toString());
                            String[] fileHashPassword = profilePassword.split(",");
                            String fileHash = "-1";
                            String filePassword = "-1";
                            if (fileHashPassword.length >= 2) {
                                fileHash = profilePassword.split(",")[0];
                                filePassword = profilePassword.split(",")[1];
                            }
                            String response = putFile("profile", fileHash, filePassword, profileContentToSend.toString(), p).second;
                            writeIntToSharedPrefs("Version", profileVersion + 1, "Profile", context);
                            Log.d(TAG, "PUT response: " + response);
                        }
                    }
                }
            }
        }

        // String fileType = profile | ride | crash
        private Pair<Integer, String> postFile(String fileType, String contentToSend, int region) throws IOException {

            String[] simRa_regions_config = getRegions(context);
            String locale = simRa_regions_config[region].split("=")[2];
            Log.d(TAG, "localeInt: " + region + " locale: " + locale);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // int appVersion = getAppVersionNumber(context);
            // Tell the URLConnection to use a SocketFactory from our SSLContext
            // URL url = new URL(Constants.MCC_VM3 + "upload/" + fileName + "?version=" + appVersion + "&loc=" + locale + "&clientHash=" + clientHash);
            URL url = new URL(BuildConfig.API_ENDPOINT + fileType + "?loc=" + locale + "&clientHash=" + SimRAuthenticator.getClientHash());
            Log.d(TAG, "URL: " + url.toString());
            HttpsURLConnection urlConnection =
                    (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestProperty("Content-Type", "text/plain");
            byte[] outputInBytes = contentToSend.getBytes(StandardCharsets.UTF_8);
            OutputStream os = urlConnection.getOutputStream();
            os.write(outputInBytes);
            os.close();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            int status = urlConnection.getResponseCode();
            if (status == 200) {
                uploadSuccessful = true;
            }
            Log.d(TAG, "Server status: " + status);
            Log.d(TAG, "Server Response: " + response);

            return new Pair<>(status, response.toString());
        }

        // fileType = profile | ride
        private Pair<Integer, String> putFile(String fileType, String fileHash, String filePassword, String contentToSend, int region) throws IOException {

            String[] simRa_regions_config = getRegions(context);
            String locale = simRa_regions_config[region].split("=")[2];
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // int appVersion = getAppVersionNumber(context);
            // Tell the URLConnection to use a SocketFactory from our SSLContext
            // URL url = new URL(Constants.MCC_VM3 + "upload/" + fileHash + "?version=" + appVersion + "&loc=" + locale + "&clientHash=" + clientHash);
            URL url = new URL(BuildConfig.API_ENDPOINT + fileType + "?fileHash=" + fileHash + "&filePassword=" + filePassword + "&loc=" + locale + "&clientHash=" + SimRAuthenticator.getClientHash());
            Log.d(TAG, "URL: " + url.toString());
            HttpsURLConnection urlConnection =
                    (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("PUT");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestProperty("Content-Type", "text/plain");
            Log.d(TAG, "contentToSend.length(): " + contentToSend.length());
            byte[] outputInBytes = contentToSend.getBytes(StandardCharsets.UTF_8);
            OutputStream os = urlConnection.getOutputStream();
            os.write(outputInBytes);
            os.close();
            int status = urlConnection.getResponseCode();
            if (status == 200) {
                uploadSuccessful = true;
            }
            Log.d(TAG, "Server status: " + status);
            String response = urlConnection.getResponseMessage();
            Log.d(TAG, "Server Response: " + response);

            return new Pair<>(status, response);
        }
    }
}
