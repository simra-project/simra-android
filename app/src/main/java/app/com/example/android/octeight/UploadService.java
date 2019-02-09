package app.com.example.android.octeight;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadService extends Service {

    public static final String TAG = "UploadService_LOG:";
    private IBinder mBinder = new UploadService.MyBinder();
    private OkHttpClient client = new OkHttpClient();


    @Override
    public void onCreate() {

        Log.d(TAG, "onCreate()");

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind()");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind()");
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // new UpdateTask(intent.getStringExtra("PathToAccGpsFile")).execute();
        new UpdateTask(this).execute();

        stopSelf();
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

        private UpdateTask(/*String path, */Context context) {
            // this.path = path;
            this.context = context;
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

        private void uploadAllFilesTestPhase(Context context) throws IOException {

            String id;

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // SharedPrefs (same as in MainActivity) for unique user id (only in test phase).
            // ID is used as prefix for each file. Server creates a directory for each id.
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

            SharedPreferences sharedPrefs = getApplicationContext()
                    .getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sharedPrefs.edit();

            if (sharedPrefs.contains("USER-ID")) {

                id = sharedPrefs.getString("USER-ID", "00000000");

            } else {

                id = String.valueOf(System.currentTimeMillis());

                editor.putString("USER-ID", id);

                editor.apply();
            }

            // makePostTestPhase("metaData.csv", id);

            // makePostTestPhase("incidentData.csv", id);

            String path = Constants.APP_PATH + "shared_prefs/simraPrefs.xml";

            makePostTestPhase(path, id);

            File[] dirFiles = getFilesDir().listFiles();

            for (int i = 0; i < dirFiles.length; i++) {

                path = dirFiles[i].getName()/*.getPath().replace(prefix, "")*/;
                Log.d(TAG, "path: " + path);
                makePostTestPhase(path, id);
            }


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

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));) {

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

            Log.d(TAG, "sending file to server: " + fileContent.toString());
            Date dateToday = new Date();
            String clientHash = Integer.toHexString((Constants.DATE_PATTERN_SHORT.format(dateToday) + Constants.UPLOAD_HASH_SUFFIX).hashCode());

            Log.d(TAG, "clientHash: " + clientHash);

            Request request = new Request.Builder()
                    .url(Constants.SERVICE_URL + key.replace(Constants.APP_PATH + "shared_prefs/", "") + "?clientHash=" + clientHash)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                Log.d(TAG, "Response Message: " + response.message());
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

                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                Log.d(TAG, "Response Message: " + response.message());
            }
        }

    }
}
