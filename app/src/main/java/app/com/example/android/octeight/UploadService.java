package app.com.example.android.octeight;

import android.app.Service;
import android.content.Intent;
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

        new UpdateTask(intent.getStringExtra("PathToAccGpsFile")).execute();

        return Service.START_STICKY;
    }

    public class MyBinder extends Binder {
        UploadService getService() {
            return UploadService.this;
        }
    }

    private class UpdateTask extends AsyncTask<String, String, String> {

        private String path;

        private UpdateTask(String path) {
            this.path = path;
        }

        protected String doInBackground(String... urls) {

            try {
                makePost(path);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
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

                Log.d(TAG, response.body().string());
            }
        }

    }
}
