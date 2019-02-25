package app.com.example.android.octeight;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static app.com.example.android.octeight.Utils.getUniqueUserID;

public class UploadService extends Service {

    Activity activity;
    NotificationManagerCompat notificationManager;
    private PowerManager.WakeLock wakeLock = null;
    // For Managing the notification shown while the service is running
    int notificationId = 1453;


    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    int numberOfTasks = 0;
    public void decreaseNumberOfTasks(){
        numberOfTasks--;
    }
    public void setNumberOfTasks(int numberOfTasks){
        this.numberOfTasks = numberOfTasks;
    }
    public int getNumberOfTasks(){
        return this.numberOfTasks;
    }

    public static final String TAG = "UploadService_LOG:";
    private IBinder mBinder = new UploadService.MyBinder();
    private OkHttpClient client = new OkHttpClient();


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
        wakeLock.acquire();

        // new UpdateTask(intent.getStringExtra("PathToAccGpsFile")).execute();
        new UpdateTask(this, intent).execute();
        // stopSelf();
        return Service.START_STICKY;
    }

    public class MyBinder extends Binder {
        UploadService getService() {
            return UploadService.this;
        }
    }

    private class UpdateTask extends AsyncTask<String, String, String> {

        private String path;
        private Context context;
        private Intent intent;

        private UpdateTask(/*String path, */Context context, Intent intent) {
            // this.path = path;
            this.context = context;
            this.intent = intent;
        }

        protected String doInBackground(String... urls) {

            Log.d(TAG, "doInBackground()");

            try {
                // makePost(path);
                uploadAllFilesTestPhase(context);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Log.d(TAG, "onPostExecute()");
            super.onPostExecute(s);
            notificationManager.cancel(notificationId);
            wakeLock.release();
            stopSelf();
        }

        private void uploadAllFilesTestPhase(Context context) throws IOException {

            String id = getUniqueUserID(context);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // SharedPrefs (same as in MainActivity) for unique user id (only in test phase).
            // ID is used as prefix for each file. Server creates a directory for each id.
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

            SharedPreferences sharedPrefs = getApplicationContext()
                    .getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sharedPrefs.edit();

            boolean sendCrashReportPermitted = intent.getBooleanExtra("CRASH_REPORT", false);
            // makePostTestPhase("metaData.csv", id);

            // makePostTestPhase("incidentData.csv", id);

            makePostTestPhase("profile.csv", id);

            File[] dirFiles = getFilesDir().listFiles();

            Log.d(TAG, "dirFiles: " + Arrays.deepToString(dirFiles));

            Log.d(TAG, "sendCrashReportPermitted: " + sendCrashReportPermitted);
            // If there was a crash and the user permitted to send the crash logs, upload all data
            // in order to enable reconstructing the error.
            if(sendCrashReportPermitted){
                String path = Constants.APP_PATH + "shared_prefs/simraPrefs.xml";
                makePostTestPhase(path, id);

                for (int i = 0; i < dirFiles.length; i++) {
                    path = dirFiles[i].getName();
                    if(!(new File(path)).isDirectory()){
                        makePostTestPhase(path, id);
                    }
                    if (path.startsWith("CRASH")){
                        boolean deleted = context.deleteFile(path);
                        Log.d(TAG, path + " deleted: " + deleted);
                    }
                }
            // If there wasn't a crash or the user did not gave us the permission, upload
            } else {
                ArrayList<String> ridesToUpload;
                ridesToUpload = intent.getStringArrayListExtra("RidesToUpload");
                UploadService.this.setNumberOfTasks((ridesToUpload.size()*2));
                // For each ride to upload...
                for (int i = 0; i < ridesToUpload.size(); i++) {
                    // ... find the corresponding ride csv file ...
                    for (int j = 0; j < dirFiles.length; j++) {
                        String nameOfFile = dirFiles[j].getName();
                        if(ridesToUpload.get(i).equals(nameOfFile)) {
                            // ... and upload.
                            makePostTestPhase(nameOfFile, id);
                            String key = ridesToUpload.get(i).split("_")[0];
                            String accEventName = "accEvents"+key+".csv";
                            makePostTestPhase(accEventName, id);
                        }
                    }
                }
            }

            // set the boolean "NEW-UNSENT-ERROR" in simraPrefs.xml to false
            // so that the StartActivity doesn't think there are still unsent
            // crash logs.
            editor.putBoolean("NEW-UNSENT-ERROR", false);
            editor.commit();



        }



        private void makePostTestPhase(String pathToFile, String id) throws IOException {

            Log.d(TAG, "pathToFile: " + pathToFile + " id: " + id);
            File file;
            // Log.d(TAG, "File.pathSeparator: " + File.pathSeparator);
            if(pathToFile.contains(File.separator)){
                //Log.d(TAG, "pathToFile contains pathSeparator!");
                file = new File(pathToFile);
            } else {
                file = getFileStreamPath(pathToFile);

            }
            if(file.isDirectory()){
                return;
            }
            //
            final StringBuilder fileContent = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    fileContent.append(line);
                    fileContent.append(System.lineSeparator());
                }

            } catch (IOException e) {
                throw e;
            }


            String key = id + "_" + pathToFile;

            RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), fileContent.toString());

            Log.d(TAG, "sending file with following key to server: " + key);
            Date dateToday = new Date();
            String clientHash = Integer.toHexString((Constants.DATE_PATTERN_SHORT.format(dateToday) + Constants.UPLOAD_HASH_SUFFIX).hashCode());

            Log.d(TAG, "clientHash: " + clientHash);
            Log.d(TAG, "dateToday: " + dateToday.toString());
            Log.d(TAG, "beforeHash: " + (Constants.DATE_PATTERN_SHORT.format(dateToday) + Constants.UPLOAD_HASH_SUFFIX));


            Request request = new Request.Builder()
                    .url(Constants.SERVICE_URL + key.replace(Constants.APP_PATH + "shared_prefs/", "") + "?clientHash=" + clientHash)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                Log.d(TAG, "Response Message: " + response.message());
                UploadService.this.decreaseNumberOfTasks();

            }
        }

        private void makePost(String pathToFile) throws IOException {

            File file = getFileStreamPath(pathToFile);
            final StringBuilder fileContent = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));) {

                String line;
                while ((line = reader.readLine()) != null) {
                    fileContent.append(line);
                    fileContent.append(System.lineSeparator());
                }

            } catch (IOException e) {
                throw e;
            }


            String key = pathToFile;

            RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), fileContent.toString());

            Date dateToday = new Date();
            String clientHash = Integer.toHexString((Constants.DATE_PATTERN_SHORT.format(dateToday) + Constants.UPLOAD_HASH_SUFFIX).hashCode());

            Log.d(TAG, "clientHash: " + clientHash);

            Request request = new Request.Builder()
                    .url(Constants.SERVICE_URL + key + "?clientHash=" + clientHash)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (!response.isSuccessful()){
                    throw new IOException("Unexpected code " + response);
                } else {
                    Log.d(TAG, "Response Message: " + response.message());
                }


            }
        }

    }

    private NotificationCompat.Builder createNotification() {
        String CHANNEL_ID = "UploadServiceNotification";
        /*
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        */
        Intent contentIntent = new Intent(this, HistoryActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, contentIntent, 0);
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.recorder_channel_name);
            String description = getString(R.string.recorder_channel_description);
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
                .setContentTitle("Fahrten werden hochgeladen")
                .setContentText("Ihre Fahrten werden hochgeladen.")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent);

        return mBuilder;

    }
}
