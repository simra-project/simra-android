package de.tuberlin.mcc.simra.app.net;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import de.tuberlin.mcc.simra.app.util.Constants;
import de.tuberlin.mcc.simra.app.main.HistoryActivity;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.util.Utils;

import static de.tuberlin.mcc.simra.app.util.Constants.BACKEND_VERSION;
import static de.tuberlin.mcc.simra.app.util.Constants.LOCALE_ABVS;
import static de.tuberlin.mcc.simra.app.util.Constants.METADATA_HEADER;
import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.util.Utils.getProfileDemographics;
import static de.tuberlin.mcc.simra.app.util.Utils.getProfileWithoutDemographics;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.lookUpSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.overWriteFile;
import static de.tuberlin.mcc.simra.app.util.Utils.readContentFromFileAndIncreaseFileVersion;
import static de.tuberlin.mcc.simra.app.util.Utils.updateProfile;
import static de.tuberlin.mcc.simra.app.util.Utils.writeBooleanToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.writeIntToSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.Utils.writeToSharedPrefs;

public class UploadService extends Service {

    NotificationManagerCompat notificationManager;
    private PowerManager.WakeLock wakeLock = null;
    // For Managing the notification shown while the service is running
    int notificationId = 1453;

    int numberOfTasks = 0;

    private boolean uploadSuccessful = false;
    boolean foundARideToUpload = false;

    public void decreaseNumberOfTasks() {
        numberOfTasks--;
    }

    public void setNumberOfTasks(int numberOfTasks) {
        this.numberOfTasks = numberOfTasks;
    }

    public int getNumberOfTasks() {
        return this.numberOfTasks;
    }

    public static final String TAG = "UploadService_LOG:";
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
        Log.d(TAG, "onDestroy()");
        notificationManager.cancel(notificationId);
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

        Notification notification = createNotification().build();
        notificationManager = NotificationManagerCompat.from(this);
        // Send the notification.
        notificationManager.notify(notificationId, notification);
        startForeground(notificationId, notification);
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
                UploadService.this.decreaseNumberOfTasks();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Log.d(TAG, "onPostExecute()");
            if(foundARideToUpload) {
                Intent intent = new Intent();
                intent.setAction("de.tuberlin.mcc.simra.app.UPLOAD_COMPLETE");
                intent.putExtra("uploadSuccessful", uploadSuccessful);
                intent.putExtra("foundARideToUpload",foundARideToUpload);
                sendBroadcast(intent);
            }
            super.onPostExecute(s);
            stopSelf();
        }

        private void uploadFile(Context context) throws IOException {
            File[] dirFiles = getFilesDir().listFiles();

            boolean crash = intent.getBooleanExtra("CRASH_REPORT", false);
            // If there was a crash and the user permitted to send the crash logs, upload simraPrefs and crash log
            if (crash) {

                for (int i = 0; i < dirFiles.length; i++) {
                    String path = dirFiles[i].getName();
                    if (!((new File(path)).isDirectory()) && path.startsWith("CRASH")) {
                        String contentToSend = readContentFromFileAndIncreaseFileVersion(path, context);
                        postFile("crash", contentToSend);
                        context.deleteFile(path);
                    }
                }
                // set the boolean "NEW-UNSENT-ERROR" in simraPrefs.xml to false
                // so that the StartActivity doesn't think there are still unsent
                // crash logs.
                writeBooleanToSharedPrefs("NEW-UNSENT-ERROR", false, "simraPrefs", context);

                // If there wasn't a crash or the user did not gave us the permission, upload ride(s)
            } else {
                UploadService.this.setNumberOfTasks(((dirFiles.length - 3) / 2) + 1);
                // String[] profileContentWithoutDemographics = getProfileWithoutDemographics();
                Object[] profileContentWithoutDemographics = getProfileWithoutDemographics(context);
                Log.d(TAG, "profileContentWithoutDemographics:" + Arrays.toString(profileContentWithoutDemographics));
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                int totalNumberOfRides = (int) profileContentWithoutDemographics[0];
                long totalDuration = (long) profileContentWithoutDemographics[1];
                int totalNumberOfIncidents = (int) profileContentWithoutDemographics[2];
                Log.d(TAG, "totalNumberOfIncidents: " + totalNumberOfIncidents);
                long totalWaitedTime = (long) profileContentWithoutDemographics[3];
                long totalDistance = (long) profileContentWithoutDemographics[4];
                long totalCO2 = (long) profileContentWithoutDemographics[5];
                //Object[] timeBuckets = Arrays.copyOfRange(profileContentWithoutDemographics,6,30);
                float[] timeBuckets = new float[24];
                for (int i = 0; i < timeBuckets.length; i++) {
                    timeBuckets[i] = (float)profileContentWithoutDemographics[i+6];
                }
                int totalNumberOfScary = (int) profileContentWithoutDemographics[30];
                int appVersion = getAppVersionNumber(context);
                String fileVersion = "";
                StringBuilder content = new StringBuilder();

                try {
                    BufferedReader metaDataReader = new BufferedReader(new FileReader(context.getFileStreamPath("metaData.csv")));
                    String line;
                    fileVersion = metaDataReader.readLine().split("#")[1];

                    // skip header
                    metaDataReader.readLine();
                    // loop through lines of metaData.csv to find rides ready for upload (state = 1)
                    while ((line = metaDataReader.readLine()) != null) {

                        // key,startTime,endTime,state,numberOfIncidents,waitedTime,distance
                        // 17,1558605594337,1558606191224,1,4,180,725
                        String[] metaDataLine = line.split(",", -1);
                        Log.d(TAG, "metaDataLine: " + Arrays.toString(metaDataLine));
                        // found a ride which is ready to upload in metaData.csv
                        if (metaDataLine.length > 1 && metaDataLine[3].equals("1")) {
                            foundARideToUpload = true;
                            String rideKey = metaDataLine[0];
                            String accGpsName = "";
                            String accEventName = "";
                            // find the accGps and accEvents file of that ride
                            for (int i = 0; i < dirFiles.length; i++) {
                                if (dirFiles[i].getName().startsWith(rideKey + "_accGps")) {
                                    Log.d(TAG, "dirFiles[i]: " + dirFiles[i].getName());
                                    accGpsName = dirFiles[i].getName();
                                    accEventName = "accEvents" + rideKey + ".csv";
                                    break;
                                }
                            }
                            Log.d(TAG, "accEventName: " + accEventName + " accGpsName: " + accGpsName);
                            // concatenate fileInfoVersion, accEvents and accGps content
                            String contentToSend = Utils.appendFromFileToFile(accEventName, accGpsName, context);
                            String password = lookUpSharedPrefs(rideKey, "-1", "keyPrefs", context);

                            Log.d(TAG, "Saved password: " + password);
                            Pair<Integer, String> response;
                            // send data with POST, if it is being sent the first time
                            if (password.equals("-1")) {
                                Log.d(TAG, "sending ride with POST: " + rideKey);
                                response = postFile("ride", contentToSend);

                                if (response.second.split(",").length >= 2) {
                                    writeToSharedPrefs(rideKey, response.second, "keyPrefs", context);
                                }
                                Log.d(TAG, "hashPassword: " + response + " written to keyPrefs");
                                // send data with PUT, if it is being overwritten on the server
                            } else {
                                Log.d(TAG, "sending ride with PUT: " + rideKey);
                                String fileHash = password.split(",")[0];
                                String filePassword = password.split(",")[1];
                                response = putFile("ride", fileHash, filePassword, contentToSend);
                                Log.d(TAG, "PUT response: " + response);
                            }

                            // if the respond is ok, mark ride as uploaded in metaData.csv
                            if (response.first.equals(200)) {
                                metaDataLine[3] = "2";
                                totalNumberOfRides++;
                                totalDuration += (Long.valueOf(metaDataLine[2]) - Long.valueOf(metaDataLine[1]));
                                totalNumberOfIncidents += Integer.valueOf(metaDataLine[4]);
                                totalWaitedTime += Long.valueOf(metaDataLine[5]);
                                totalDistance += Long.valueOf(metaDataLine[6]);
                                totalCO2 += (long) ((Long.valueOf(metaDataLine[6]) / (float) 1000) * 138);
                                // update the timebuckets
                                Date startDate = new Date(Long.valueOf(metaDataLine[1]));
                                Date endDate = new Date(Long.valueOf(metaDataLine[2]));
                                Locale locale = Resources.getSystem().getConfiguration().locale;
                                SimpleDateFormat sdf = new SimpleDateFormat("HH", locale);
                                int startHour = Integer.valueOf(sdf.format(startDate));
                                int endHour = Integer.valueOf(sdf.format(endDate));
                                float duration = endHour - startHour + 1;
                                for (int i = startHour; i <= endHour; i++) {
                                    timeBuckets[i] = timeBuckets[i] + (1 / duration);
                                }
                                totalNumberOfScary += Integer.valueOf(metaDataLine[7]);
                            }

                        }
                        // update metaData.csv. change status from 1 to 2, if upload was successful
                        for (int i = 0; i < metaDataLine.length; i++) {
                            if (i == metaDataLine.length - 1) {
                                content.append(metaDataLine[i]).append(System.lineSeparator());
                            } else {
                                content.append(metaDataLine[i]).append(",");
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
                    String fileInfoLine = appVersion + "#" + fileVersion + System.lineSeparator();
                    overWriteFile((fileInfoLine + METADATA_HEADER + content.toString()), "metaData.csv", context);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /*
                String fileInfoLine = appVersion + "#" + fileVersion + System.lineSeparator();
                int[] demographics = getProfileDemographics(context);
                StringBuilder timeBucketsString = new StringBuilder();
                for (int i = 0; i < timeBuckets.length; i++) {
                    timeBucketsString.append(timeBuckets[i]).append(",");
                }
                overWriteFile(fileInfoLine + PROFILE_HEADER + demographics + "," + totalNumberOfRides + "," + totalDuration + "," + totalNumberOfIncidents + "," + totalWaitedTime + "," + totalDistance + "," + totalCO2 + timeBucketsString.toString() + behaviour, "profile.csv", context);
                */
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                // Sending / Updating profile with each upload


                // get profile.csv data to update it when the uploads are successful
                /*
                String profileInfoLine;
                String[] profile;
                try (BufferedReader profileReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(context.getFilesDir() + "/profile.csv"))))) {
                    // fileInfo
                    profileInfoLine = profileReader.readLine();
                    // header birth,gender,region,experience,numberOfRides,duration,numberOfIncidents,waitedTime,distance,co2,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,behaviour
                    profileReader.readLine();
                    // 0,1,1,4,9,6626289,5,3060,46444,6409,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,3.5,1.5,0.0,3.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,3
                    profile = profileReader.readLine().split(",");
                }
                */
                // Now after the rides have been uploaded, we can update the profile with the new statistics
                updateProfile(context,-1,-1,-1,-1,totalNumberOfRides,totalDuration,totalNumberOfIncidents,totalWaitedTime,totalDistance,totalCO2,timeBuckets,-1,totalNumberOfScary);

                // StringBuilder profileContentToSend = new StringBuilder(profileInfoLine + System.lineSeparator() + PROFILE_HEADER);
                SharedPreferences sharedPrefs = context.getApplicationContext()
                        .getSharedPreferences("Profile", Context.MODE_PRIVATE);
                int profileVersion = sharedPrefs.getInt("Version", 1);
                StringBuilder profileContentToSend = new StringBuilder().append(appVersion).append("#").append(profileVersion).append(System.lineSeparator());
                profileContentToSend.append(Constants.PROFILE_HEADER);
                int[] demographics = getProfileDemographics(context);
                for (int i = 0; i < demographics.length-1; i++) {
                    profileContentToSend.append(demographics[i]).append(",");
                }

                profileContentToSend
                        .append(totalNumberOfRides).append(",")
                        .append(totalDuration).append(",")
                        .append(totalNumberOfIncidents).append(",")
                        .append(totalWaitedTime).append(",")
                        .append(totalDistance).append(",")
                        .append(totalCO2).append(",");
                for (int i = 0; i < timeBuckets.length; i++) {
                    profileContentToSend.append(timeBuckets[i]).append(",");
                }
                profileContentToSend.append(demographics[4]).append(",");
                profileContentToSend.append(totalNumberOfScary);
                /*
                for (int i = 0; i < profile.length; i++) {
                    profileContentToSend.append(profile[i]);
                    if (!(i == profile.length - 1))
                        profileContentToSend.append(",");
                }
                */
                String profilePassword = lookUpSharedPrefs("profile.csv", "-1", "keyPrefs", context);
                Log.d(TAG, "Saved password: " + profilePassword);
                if (profilePassword.equals("-1")) {
                    Log.d(TAG, "sending profile with POST: " + profileContentToSend.toString());
                    String hashPassword = postFile("profile", profileContentToSend.toString()).second;
                    if (hashPassword.split(",").length >= 2) {
                        writeToSharedPrefs("profile.csv", hashPassword, "keyPrefs", context);
                        writeIntToSharedPrefs("Version",profileVersion+1,"Profile",context);
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
                    String response = putFile("profile", fileHash, filePassword, profileContentToSend.toString()).second;
                    writeIntToSharedPrefs("Version",profileVersion+1,"Profile",context);
                    Log.d(TAG, "PUT response: " + response);
                }
            }
        }

        // String fileType = profile | ride | crash
        private Pair<Integer, String> postFile(String fileType, String contentToSend) throws IOException {

            int localeInt = lookUpIntSharedPrefs("Region", 0, "Profile", context);
            String locale = LOCALE_ABVS[localeInt];
            Log.d(TAG, "localeInt: " + localeInt + " locale: " + locale);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // int appVersion = getAppVersionNumber(context);
            // Tell the URLConnection to use a SocketFactory from our SSLContext
            // URL url = new URL(Constants.MCC_VM3 + "upload/" + fileName + "?version=" + appVersion + "&loc=" + locale + "&clientHash=" + clientHash);
            URL url = new URL(Constants.MCC_VM2 + BACKEND_VERSION + "/" + fileType + "?loc=" + locale + "&clientHash=" + SimRAuthenticator.getClientHash(context));
            Log.d(TAG, "URL: " + url.toString());
            HttpsURLConnection urlConnection =
                    (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestProperty("Content-Type", "text/plain");
            byte[] outputInBytes = contentToSend.getBytes("UTF-8");
            OutputStream os = urlConnection.getOutputStream();
            os.write(outputInBytes);
            os.close();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream()));
            String inputLine;
            String response = "";
            while ((inputLine = in.readLine()) != null) {
                response += inputLine;
            }
            in.close();
            int status = urlConnection.getResponseCode();
            if (status == 200) {
                uploadSuccessful = true;
            }
            Log.d(TAG, "Server status: " + status);
            Log.d(TAG, "Server Response: " + response);
            UploadService.this.decreaseNumberOfTasks();

            return new Pair<Integer, String>(status, response);
        }

        // fileType = profile | ride
        private Pair<Integer, String> putFile(String fileType, String fileHash, String filePassword, String contentToSend) throws IOException {

            int localeInt = lookUpIntSharedPrefs("Region", 0, "Profile", context);
            String locale = LOCALE_ABVS[localeInt];

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // int appVersion = getAppVersionNumber(context);
            // Tell the URLConnection to use a SocketFactory from our SSLContext
            // URL url = new URL(Constants.MCC_VM3 + "upload/" + fileHash + "?version=" + appVersion + "&loc=" + locale + "&clientHash=" + clientHash);
            URL url = new URL(Constants.MCC_VM2 + BACKEND_VERSION + "/" + fileType + "?fileHash=" + fileHash + "&filePassword=" + filePassword + "&loc=" + locale + "&clientHash=" + SimRAuthenticator.getClientHash(context));
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
            byte[] outputInBytes = contentToSend.getBytes("UTF-8");
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
            UploadService.this.decreaseNumberOfTasks();

            return new Pair<Integer, String>(status, response);
        }

        /*
        private String getDemographics() {
            int birth = lookUpIntSharedPrefs("Profile-Age", 0, "simraPrefs", context);
            int gender = lookUpIntSharedPrefs("Profile-Gender", 0, "simraPrefs", context);
            int region = lookUpIntSharedPrefs("Profile-Region", 0, "simraPrefs", context);
            int experience = lookUpIntSharedPrefs("Profile-Experience", 0, "simraPrefs", context);
            return birth + "," + gender + "," + region + "," + experience;
        }
        */

        /*private String[] getProfileWithoutDemographics() {
            String[] result = null;
            try (BufferedReader metaDataReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(context.getFilesDir() + "/profile.csv"))))) {
                // fileInfo
                metaDataReader.readLine();
                // header
                metaDataReader.readLine();
                String[] profileEntries = metaDataReader.readLine().split(",");
                Log.d(TAG, "profileEntries: " + Arrays.toString(profileEntries));
                result = new String[profileEntries.length-4];
                for (int i = 0; i < profileEntries.length-4; i++) {
                    result[i] = profileEntries[i+4];
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }*/
    }


    private NotificationCompat.Builder createNotification() {
        String CHANNEL_ID = "UploadServiceNotification";
        Intent contentIntent = new Intent(this, HistoryActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, contentIntent, 0);
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.upload_channel_name);
            String description = getString(R.string.upload_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.helmet)
                .setContentTitle(getResources().getString(R.string.uploadingNotificationTitle))
                .setContentText(getResources().getString(R.string.uploadingNotificationBody))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent);

        return mBuilder;

    }
}
