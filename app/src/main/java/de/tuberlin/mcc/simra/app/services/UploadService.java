package de.tuberlin.mcc.simra.app.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;
import de.tuberlin.mcc.simra.app.entities.Profile;
import de.tuberlin.mcc.simra.app.util.Constants;
import de.tuberlin.mcc.simra.app.util.ForegroundServiceNotificationManager;
import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.Utils;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.lookUpSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.writeIntToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.writeToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SimRAuthenticator.getClientHash;
import static de.tuberlin.mcc.simra.app.util.Utils.calculateCO2Savings;
import static de.tuberlin.mcc.simra.app.util.Utils.getRegions;
import static de.tuberlin.mcc.simra.app.util.Utils.readContentFromFile;

public class UploadService extends Service {

    public static final String TAG = "UploadService_LOG:";
    boolean foundARideToUpload = false;
    private PowerManager.WakeLock wakeLock = null;
    private boolean uploadSuccessful = false;
    private IBinder mBinder = new UploadService.MyBinder();

    private static Profile updateProfileFromMetaData(Profile profile, MetaDataEntry metaDataEntry) {
        profile.numberOfRides++;
        profile.duration += (metaDataEntry.endTime - metaDataEntry.startTime);
        profile.numberOfIncidents += metaDataEntry.numberOfIncidents;
        profile.waitedTime += metaDataEntry.waitedTime;
        profile.distance += metaDataEntry.distance;
        profile.co2 += calculateCO2Savings(metaDataEntry.distance);
        profile.numberOfScaryIncidents += metaDataEntry.numberOfScaryIncidents;

        // update the timebuckets
        Date startDate = new Date(metaDataEntry.startTime);
        Date endDate = new Date(metaDataEntry.endTime);
        Locale locale = Resources.getSystem().getConfiguration().locale;
        SimpleDateFormat sdf = new SimpleDateFormat("HH", locale);
        int startHour = Integer.parseInt(sdf.format(startDate));
        int endHour = Integer.parseInt(sdf.format(endDate));
        float durationOfThisRide = endHour - startHour + 1;

        for (int i = startHour; i <= endHour; i++) {
            profile.timeDistribution.set(i, profile.timeDistribution.get(i).floatValue() + (1 / durationOfThisRide));
        }

        return profile;
    }

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
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
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

        Notification notification = ForegroundServiceNotificationManager.createOrUpdateNotification(
                        this,
                        getResources().getString(R.string.uploadingNotificationTitle),
                        getResources().getString(R.string.uploadingNotificationBody)
                );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ForegroundServiceNotificationManager.getNotificationId(), notification,FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(ForegroundServiceNotificationManager.getNotificationId(), notification);
        }
        wakeLock.acquire(1000 * 60 * 15);

        new UploadTask(this, intent).execute();
        return Service.START_NOT_STICKY;
    }

    public class MyBinder extends Binder {
        public UploadService getService() {
            return UploadService.this;
        }
    }

    private class UploadTask extends AsyncTask<String, String, String> {

        private Context context;
        private Intent intent;

        private UploadTask(Context context, Intent intent) {
            this.context = context;
            this.intent = intent;
        }

        protected String doInBackground(String... urls) {

            Log.d(TAG, "doInBackground()");

            try {
                uploadFile(context);
            } catch (IOException e) {
                Log.e(TAG, "UploadTask - Exception: " + e.getMessage());
                Log.e(TAG, Arrays.toString(e.getStackTrace()));
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

                List<MetaDataEntry> metaDataEntries = MetaData.getMetaDataEntries(context);
                ArrayList<File> obsLiteDataToUpload = new ArrayList<>();
                for (MetaDataEntry metaDataEntry : metaDataEntries) {
                    // found a ride which is ready to upload in metaData.csv
                    if (metaDataEntry.state.equals(MetaData.STATE.ANNOTATED)) {
                        if (IOUtils.Files.getOBSLiteSessionFile(metaDataEntry.rideId, context).exists()) {
                            obsLiteDataToUpload.add(IOUtils.Files.getOBSLiteSessionFile(metaDataEntry.rideId, context));
                        }
                        foundARideToUpload = true;
                        // concatenate fileInfoVersion, accEvents and accGps content
                        Pair<String, IncidentLog> contentToUploadAndAccEventsContentToOverwrite = Utils.getConsolidatedRideForUpload(metaDataEntry.rideId, context);

                        String contentToUpload = contentToUploadAndAccEventsContentToOverwrite.first;

                        String password = lookUpSharedPrefs(String.valueOf(metaDataEntry.rideId), "-1", "keyPrefs", context);

                        Profile regionProfile = regionProfilesList.get(metaDataEntry.region);

                        Log.d(TAG, "Saved password: " + password);
                        Pair<Integer, String> response;
                        // send data with POST, if it is being sent the first time
                        if (password.equals("-1")) {
                            Log.d(TAG, "sending ride with POST: " + metaDataEntry.rideId);
                            response = postFile("ride", contentToUpload, metaDataEntry.region);

                            if (response.second.split(",").length >= 2) {
                                writeToSharedPrefs(String.valueOf(metaDataEntry.rideId), response.second, "keyPrefs", context);
                            }
                            Log.d(TAG, "hashPassword: " + response + " written to keyPrefs");
                            // send data with PUT, if it is being overwritten on the server
                        } else {
                            Log.d(TAG, "sending ride with PUT: " + metaDataEntry.rideId);
                            String fileHash = password.split(",")[0];
                            String filePassword = password.split(",")[1];
                            response = putFile("ride", fileHash, filePassword, contentToUpload, metaDataEntry.region);
                            Log.d(TAG, "PUT response: " + response);
                        }


                        // if the respond is ok, mark ride as uploaded in metaData.csv
                        if (response.first.equals(200)) {
                            metaDataEntry.state = MetaData.STATE.SYNCED;
                            MetaData.updateOrAddMetaDataEntryForRide(metaDataEntry, context);


                            globalProfile = updateProfileFromMetaData(globalProfile, metaDataEntry);
                            regionProfile = updateProfileFromMetaData(regionProfile, metaDataEntry);

                            regionProfileUpdated[metaDataEntry.region] = true;
                            regionProfilesList.set(metaDataEntry.region, regionProfile);
                        }
                    }
                }

                for (File obsLiteBinary :obsLiteDataToUpload) {
                    postOBSLiteFile(obsLiteBinary);
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
                                writeToSharedPrefs("Profile_" + p, hashPassword, "keyPrefs", context);
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
            URL url = new URL(BuildConfig.API_ENDPOINT + BuildConfig.API_VERSION + fileType + "?loc=" + locale + "&clientHash=" + getClientHash(context));
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
            URL url = new URL(BuildConfig.API_ENDPOINT + BuildConfig.API_VERSION + fileType + "?fileHash=" + fileHash + "&filePassword=" + filePassword + "&loc=" + locale + "&clientHash=" + getClientHash(context));

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

        private Pair<Integer, String> postOBSLiteFile(File file) throws IOException {

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // int appVersion = getAppVersionNumber(context);
            // Tell the URLConnection to use a SocketFactory from our SSLContext

            String obsLiteUrl = SharedPref.Settings.OBSLite.getObsLiteURL(context);

            URL url = new URL(obsLiteUrl);
            Log.d(TAG, "URL: " + url.toString());
            HttpsURLConnection urlConnection =
                    (HttpsURLConnection) url.openConnection();
            Authenticator.setDefault(new CustomAuthenticator(context));

            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestProperty("Content-Type", "application/octet-stream");
            byte[] outputInBytes = new byte[(int) file.length()];
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(outputInBytes);
            dis.close();
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

        public static class CustomAuthenticator extends Authenticator {

            private final Context context;

            public CustomAuthenticator(Context context) {
                this.context = context;
            }

            // Called when password authorization is needed
            protected PasswordAuthentication getPasswordAuthentication() {

                // Get information about the request
                String prompt = getRequestingPrompt();
                String hostname = getRequestingHost();
                InetAddress ipaddr = getRequestingSite();
                int port = getRequestingPort();

                String username = SharedPref.Settings.OBSLite.getObsLiteUsername(this.context);
                String password = SharedPref.Settings.OBSLite.getObsLiteAPIKey(this.context);

                // Return the information (a data holder that is used by Authenticator)
                return new PasswordAuthentication(username,password.toCharArray());

            }

        }

        /*private String postOBSLiteFile(File file) throws IOException {

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // int appVersion = getAppVersionNumber(context);
            // Tell the URLConnection to use a SocketFactory from our SSLContext
            String url = "http://localhost:3000/api/tracks";
            HashMap<String,String> params = new HashMap<>();
            params.put("body","testfile.bin");


            return multipartRequest(url,params,file.getPath(),file.getName(),"application/octet-stream");;
        }*/

        public String multipartRequest(String urlTo, Map<String, String> params, String filepath, String filefield, String fileMimeType) {
            HttpsURLConnection connection = null;
            DataOutputStream outputStream = null;
            InputStream inputStream = null;

            String twoHyphens = "--";
            String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
            String lineEnd = "\r\n";

            String result = "";

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;

            String[] q = filepath.split("/");
            int idx = q.length - 1;

            try {
                File file = new File(filepath);
                FileInputStream fileInputStream = new FileInputStream(file);

                URL url = new URL(urlTo);
                connection = (HttpsURLConnection) url.openConnection();

                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                outputStream = new DataOutputStream(connection.getOutputStream());
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + q[idx] + "\"" + lineEnd);
                outputStream.writeBytes("Content-Type: " + fileMimeType + lineEnd);
                outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);

                outputStream.writeBytes(lineEnd);

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0) {
                    outputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                outputStream.writeBytes(lineEnd);

                // Upload POST Data
                Iterator<String> keys = params.keySet().iterator();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = params.get(key);

                    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd);
                    outputStream.writeBytes("Content-Type: text/plain" + lineEnd);
                    outputStream.writeBytes(lineEnd);
                    outputStream.writeBytes(value);
                    outputStream.writeBytes(lineEnd);
                }

                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


                if (200 != connection.getResponseCode()) {
                    throw new Exception("Failed to upload code:" + connection.getResponseCode() + " " + connection.getResponseMessage());
                }

                inputStream = connection.getInputStream();

                result = this.convertStreamToString(inputStream);

                fileInputStream.close();
                inputStream.close();
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                Log.d(TAG, Objects.requireNonNull(e.getMessage()));
                e.printStackTrace();
            }
            return result;
        }

        private String convertStreamToString(InputStream is) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        }
    }
}
