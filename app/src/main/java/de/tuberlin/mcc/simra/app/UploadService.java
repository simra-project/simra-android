package de.tuberlin.mcc.simra.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;

import static de.tuberlin.mcc.simra.app.Constants.BACKEND_VERSION;
import static de.tuberlin.mcc.simra.app.Constants.LOCALE_ABVS;
import static de.tuberlin.mcc.simra.app.Utils.checkForAnnotation;
import static de.tuberlin.mcc.simra.app.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.Utils.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.Utils.lookUpSharedPrefs;
import static de.tuberlin.mcc.simra.app.Utils.overWriteFile;
import static de.tuberlin.mcc.simra.app.Utils.readContentFromFile;
import static de.tuberlin.mcc.simra.app.Utils.readContentFromFileAndIncreaseFileVersion;
import static de.tuberlin.mcc.simra.app.Utils.writeBooleanToSharedPrefs;
import static de.tuberlin.mcc.simra.app.Utils.writeToSharedPrefs;

public class UploadService extends Service {

    NotificationManagerCompat notificationManager;
    private PowerManager.WakeLock wakeLock = null;
    // For Managing the notification shown while the service is running
    int notificationId = 1453;

    int numberOfTasks = 0;

    private boolean uploadSuccessful = true;

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
        UploadService getService() {
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
            Intent intent = new Intent();
            intent.setAction("de.tuberlin.mcc.simra.app.UPLOAD_COMPLETE");
            intent.putExtra("uploadSuccessful", uploadSuccessful);
            sendBroadcast(intent);
            super.onPostExecute(s);
            stopSelf();
        }

        private void uploadFile(Context context) throws IOException {
            File[] dirFiles = getFilesDir().listFiles();

            boolean crash = intent.getBooleanExtra("CRASH_REPORT",false);
            // If there was a crash and the user permitted to send the crash logs, upload simraPrefs and crash log
            if (crash) {
                String ts = String.valueOf(System.currentTimeMillis());

                for (int i = 0; i < dirFiles.length; i++) {
                    String path = dirFiles[i].getName();
                    if (!((new File(path)).isDirectory()) && path.startsWith("CRASH")) {
                        String contentToSend = readContentFromFileAndIncreaseFileVersion(path,context);
                        String key = "CRASH_" + ts + "_" + path;
                        postFile("crash",contentToSend);
                        context.deleteFile(path);
                    }
                }
                // set the boolean "NEW-UNSENT-ERROR" in simraPrefs.xml to false
                // so that the StartActivity doesn't think there are still unsent
                // crash logs.
                writeBooleanToSharedPrefs("NEW-UNSENT-ERROR",false,"simraPrefs", context);

                // If there wasn't a crash or the user did not gave us the permission, upload
            } else {
                UploadService.this.setNumberOfTasks(((dirFiles.length-3)/2)+1);

                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                int numberOfRides = 0;
                long duration = 0;
                int numberOfIncidents = 0;

                int appVersion = getAppVersionNumber(context);
                String fileVersion = "";
                String content = "";

                try {
                    BufferedReader metaDataReader = new BufferedReader(new FileReader(context.getFileStreamPath("metaData.csv")));
                    String line;
                    while ((line = metaDataReader.readLine()) != null) {
                        if (line.contains("#")) {
                            String[] fileInfoArray = line.split("#");
                            fileVersion = fileInfoArray[1]; //"" + (Integer.valueOf(fileInfoArray[1]) + 1);
                            continue;
                        }
                        String[] metaDataLine = line.split(",", -1);
                        Log.d(TAG, "metaDataLine: " + Arrays.toString(metaDataLine));
                        Log.d(TAG, "line: " + line);
                        if (metaDataLine.length > 1 && metaDataLine[3].equals("1")) {
                            String rideKey = metaDataLine[0];
                            String accGpsName = "";
                            String accEventName = "";
                            Log.d(TAG, "dirFiles: " + Arrays.toString(dirFiles));
                            for (int i = 0; i < dirFiles.length; i++) {
                                if (dirFiles[i].getName().startsWith(rideKey + "_accGps")) {
                                    Log.d(TAG, "dirFiles[i]: " + dirFiles[i].getName());
                                    accGpsName = dirFiles[i].getName();
                                    accEventName = "accEvents" + rideKey + ".csv";
                                    break;
                                }
                            }
                            Log.d(TAG, "accEventName: " + accEventName + " accGpsName: " + accGpsName);
                            String contentToSend = Utils.appendFromFileToFile(accEventName, accGpsName, context);
                            String password = lookUpSharedPrefs(rideKey, "-1", "keyPrefs", context);

                            Log.d(TAG, "Saved password: " + password);
                            String response = "";
                            if (password.equals("-1")) {
                                Log.d(TAG, "sending ride with POST: " + rideKey);
                                response = postFile("ride", contentToSend);
                                if (response.split(",").length >= 2) {
                                    writeToSharedPrefs(rideKey, response, "keyPrefs", context);
                                    metaDataLine[3] = "2";
                                }
                                Log.d(TAG, "hashPassword: " + response + " written to keyPrefs");
                            } else {
                                Log.d(TAG, "sending ride with PUT: " + rideKey);
                                String fileHash = password.split(",")[0];
                                String filePassword = password.split(",")[1];
                                response = putFile("ride", fileHash, filePassword, contentToSend);
                                if (response.equals("OK")) {
                                    metaDataLine[3] = "2";
                                }
                                Log.d(TAG, "PUT response: " + response);
                            }

                        }
                        content += ((Arrays.toString(metaDataLine).replace("[","")
                                .replace(", ",",").replace("]","")) + System.lineSeparator());
                        if (!metaDataLine[0].equals("key")) {
                            duration = duration + (Long.valueOf(metaDataLine[2]) - Long.valueOf(metaDataLine[1]));
                            numberOfRides = Integer.valueOf(metaDataLine[0]);
                        }
                    }
                    String fileInfoLine = appVersion + "#" + fileVersion + System.lineSeparator();
                    overWriteFile((fileInfoLine + content), "metaData.csv", context);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < dirFiles.length; i++) {
                    if (dirFiles[i].getName().startsWith("accEvents")) {
                        BufferedReader accEventsReader = new BufferedReader(new FileReader(context.getFileStreamPath(dirFiles[i].getName())));
                        String accEventsLine;
                        accEventsReader.readLine(); // Skip fileInfo line (11#2)
                        accEventsReader.readLine(); // Skip csv header (key,lat,lon,...)
                        while ((accEventsLine = accEventsReader.readLine()) != null) {
                            String[] actualLine = accEventsLine.split(",", -1);
                            if (checkForAnnotation(actualLine)) {
                                numberOfIncidents++;
                            }
                        }
                    }
                }
                String demographicHeader = "birth,gender,region,experience,numberOfRides,duration,numberOfIncidents" + System.lineSeparator();
                String demographics = getDemographics();
                String fileInfoLine = appVersion + "#" + fileVersion + System.lineSeparator();
                overWriteFile(fileInfoLine + demographicHeader + demographics + "," + numberOfRides + "," + duration + "," + numberOfIncidents, "profile.csv", context);

                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                // Sending / Updating profile with each upload
                String profileContentToSend = readContentFromFile("profile.csv",context);
                String profilePassword = lookUpSharedPrefs("profile.csv","-1","keyPrefs",context);
                Log.d(TAG, "Saved password: " + profilePassword);
                if(profilePassword.equals("-1")){
                    Log.d(TAG, "sending profile with POST");
                    String hashPassword = postFile("profile",profileContentToSend);
                    if (hashPassword.split(",").length >= 2) {
                        writeToSharedPrefs("profile.csv",hashPassword,"keyPrefs",context);
                    }
                    Log.d(TAG, "hashPassword: " + hashPassword + " written to keyPrefs");
                } else {
                    Log.d(TAG, "sending profile with PUT");
                    String[] fileHashPassword = profilePassword.split(",");
                    String fileHash = "-1";
                    String filePassword = "-1";
                    if (fileHashPassword.length >= 2) {
                        fileHash = profilePassword.split(",")[0];
                        filePassword = profilePassword.split(",")[1];
                    }
                    String response = putFile("profile", fileHash, filePassword, profileContentToSend);
                    Log.d(TAG, "PUT response: " + response);
                }
            }
        }
        // String fileType = profile | ride | crash
        private String postFile (String fileType, String contentToSend) throws IOException {

            int localeInt = lookUpIntSharedPrefs("Profile-Region",0,"simraPrefs",context);
            String locale = LOCALE_ABVS[localeInt];

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // int appVersion = getAppVersionNumber(context);
            // Tell the URLConnection to use a SocketFactory from our SSLContext
            // URL url = new URL(Constants.MCC_VM3 + "upload/" + fileName + "?version=" + appVersion + "&loc=" + locale + "&clientHash=" + clientHash);
            URL url = new URL(Constants.MCC_VM2 + BACKEND_VERSION + "/" + fileType + "?loc=" + locale + "&clientHash=" + SimRAuthenticator.getClientHash(context));
            Log.d(TAG, "URL: " + url.toString());
            HttpsURLConnection urlConnection =
                    (HttpsURLConnection)url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestProperty("Content-Type","text/plain");
            byte[] outputInBytes = contentToSend.getBytes("UTF-8");
            OutputStream os = urlConnection.getOutputStream();
            os.write( outputInBytes );
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
            if (status != 200) {
                uploadSuccessful = false;
            }
            Log.d(TAG, "Server status: " + status);
            Log.d(TAG, "Server Response: " + response);
            UploadService.this.decreaseNumberOfTasks();

            return response;
        }
        // fileType = profile | ride
        private String putFile(String fileType, String fileHash, String filePassword, String contentToSend) throws IOException {

            int localeInt = lookUpIntSharedPrefs("Profile-Region",0,"simraPrefs",context);
            String locale = LOCALE_ABVS[localeInt];

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // int appVersion = getAppVersionNumber(context);
            // Tell the URLConnection to use a SocketFactory from our SSLContext
            // URL url = new URL(Constants.MCC_VM3 + "upload/" + fileHash + "?version=" + appVersion + "&loc=" + locale + "&clientHash=" + clientHash);
            URL url = new URL(Constants.MCC_VM2 + BACKEND_VERSION + "/" + fileType + "?fileHash=" + fileHash + "&filePassword=" + filePassword + "&loc=" + locale + "&clientHash=" + SimRAuthenticator.getClientHash(context));
            Log.d(TAG, "URL: " + url.toString());
            HttpsURLConnection urlConnection =
                    (HttpsURLConnection)url.openConnection();
            urlConnection.setRequestMethod("PUT");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestProperty("Content-Type","text/plain");
            Log.d(TAG, "contentToSend.length(): " + contentToSend.length());
            byte[] outputInBytes = contentToSend.getBytes("UTF-8");
            OutputStream os = urlConnection.getOutputStream();
            os.write( outputInBytes );
            os.close();
            int status = urlConnection.getResponseCode();
            if (status != 200) {
                uploadSuccessful = false;
            }
            Log.d(TAG, "Server status: " + status);
            String response = urlConnection.getResponseMessage();
            Log.d(TAG, "Server Response: " + response);
            UploadService.this.decreaseNumberOfTasks();

            return response;
        }

        private String getDemographics() {

            int birth = lookUpIntSharedPrefs("Profile-Age", 0, "simraPrefs", context);
            int gender = lookUpIntSharedPrefs("Profile-Gender", 0, "simraPrefs", context);
            int region = lookUpIntSharedPrefs("Profile-Region", 0, "simraPrefs", context);
            int experience = lookUpIntSharedPrefs("Profile-Experience", 0, "simraPrefs", context);
            return birth + "," + gender + "," + region + "," + experience;
        }

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
