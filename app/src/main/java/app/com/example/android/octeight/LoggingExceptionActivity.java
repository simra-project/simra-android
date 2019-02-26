package app.com.example.android.octeight;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static app.com.example.android.octeight.Utils.appendToFile;
import static app.com.example.android.octeight.Utils.getUniqueUserID;

public class LoggingExceptionActivity extends AppCompatActivity implements Thread.UncaughtExceptionHandler {

    private final static String TAG = LoggingExceptionActivity.class.getSimpleName()+"_LOG";
    private final Context context;
    private final Thread.UncaughtExceptionHandler rootHandler;
    private OkHttpClient client = new OkHttpClient();


    public LoggingExceptionActivity(Context context) {
        this.attachBaseContext(context);
        this.context = context;
        // we should store the current exception handler -- to invoke it for all not handled exceptions ...
        rootHandler = Thread.getDefaultUncaughtExceptionHandler();
        // we replace the exception handler now with us -- we will properly dispatch the exceptions ...
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        try {
            Log.d(TAG, "called for " + ex.getClass());
            // assume we would write each error in one file ...

            // log this exception ...

            String stackTrace = "";
            for (int i = 0; i < ex.getStackTrace().length; i++) {
                stackTrace += ex.getStackTrace()[i] + "\n";
            }
            String causeTrace = "";
            if(ex.getCause() != null) {
                for (int i = 0; i < ex.getCause().getStackTrace().length; i++) {
                    stackTrace += ex.getCause().getStackTrace()[i] + "\n";
                }
            }
            String errorReport = "Exception in: " + context.getClass().getName()
                    + " " + ex.getClass().getName() + "\n" +
                    ex.getMessage() + "\n" +
                    stackTrace + "\n" +
                    causeTrace + "\n" +
                    System.currentTimeMillis() + "\n" +
                    Build.VERSION.RELEASE + "\n" +
                    Build.DEVICE + "\n" +
                    Build.MODEL + "\n" +
                    Build.PRODUCT;

            appendToFile(errorReport, "CRASH_REPORT" + new Date().toString() + ".txt", this);

            SharedPreferences sharedPrefs = getApplicationContext()
                    .getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sharedPrefs.edit();

            editor.putBoolean("NEW-UNSENT-ERROR", true);
            editor.commit();

            restartApp();

        } catch (Exception e) {
            Log.e(TAG, "Exception Logger failed!", e);
        }

    }

    private void restartApp() {
        Intent intent = new Intent(getApplicationContext(), StartActivity.class);
        int mPendingIntentId = 1337;
        PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
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

        // Upload all files to the backend
        private void uploadAllFilesTestPhase(Context context) throws IOException {

            String path = Constants.APP_PATH + "shared_prefs/simraPrefs.xml";

            makePostTestPhase(path, getUniqueUserID(context));

            File[] dirFiles = getFilesDir().listFiles();

            for (int i = 0; i < dirFiles.length; i++) {
                path = dirFiles[i].getName()/*.getPath().replace(prefix, "")*/;
                Log.d(TAG, "path: " + path);
                makePostTestPhase(path, getUniqueUserID(context));
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