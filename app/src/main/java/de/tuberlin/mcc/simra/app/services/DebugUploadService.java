package de.tuberlin.mcc.simra.app.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;
import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.util.ForegroundServiceNotificationManager;
import de.tuberlin.mcc.simra.app.util.IOUtils;
import static de.tuberlin.mcc.simra.app.util.SimRAuthenticator.getClientHash;

public class DebugUploadService extends Service {

    public static final String TAG = "DebugUploadService_LOG:";
    private PowerManager.WakeLock wakeLock = null;
    private boolean uploadSuccessful = false;
    private IBinder mBinder = new DebugUploadService.MyBinder();

    @Override
    public void onCreate() {
        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":DebugUploadService");
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
        // create and show notification
        Notification notification = ForegroundServiceNotificationManager.createOrUpdateNotification(
                        this,
                        getResources().getString(R.string.uploadingNotificationTitle),
                        getResources().getString(R.string.uploadingDebugNotificationBody)
                );
        startForeground(ForegroundServiceNotificationManager.getNotificationId(), notification);
        // acquire wakelock to stay awake
        wakeLock.acquire(1000 * 60 * 15);

        // start Upload
        new DebugUploadTask(this, intent).execute();
        return Service.START_NOT_STICKY;

    }

    public class MyBinder extends Binder {
        public DebugUploadService getService() {
            return DebugUploadService.this;
        }
    }

    private class DebugUploadTask extends AsyncTask<String, String, String> {

        private Context context;
        private Intent intent;

        private DebugUploadTask(Context context, Intent intent) {
            this.context = context;
            this.intent = intent;
        }

        protected String doInBackground(String... urls) {

            try {
                uploadFile(context);
            } catch (IOException e) {
                Log.e(TAG, "DebugUploadTask - Exception: " + e.getMessage());
                Log.e(TAG, Arrays.toString(e.getStackTrace()));
                e.printStackTrace();
            }
            return null;

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Intent intent = new Intent();
            intent.setAction("de.tuberlin.mcc.simra.app.UPLOAD_COMPLETE");
            intent.putExtra("uploadSuccessful", uploadSuccessful);
            sendBroadcast(intent);
            stopForeground(true);
        }

        private void uploadFile(Context context) throws IOException {
            File fileToUpload = new File(IOUtils.Directories.getBaseFolderPath(context) + "zip.zip");

            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            URL connectURL;
            try {

                connectURL = new URL(BuildConfig.API_ENDPOINT + BuildConfig.API_VERSION  + "debug?clientHash=" + getClientHash(context));

                HttpsURLConnection conn = (HttpsURLConnection)connectURL.openConnection();

                // Allow Inputs
                conn.setDoInput(true);

                // Allow Outputs
                conn.setDoOutput(true);

                // Don't use a cached copy.
                conn.setUseCaches(false);

                // Use a post method.
                conn.setRequestMethod("POST");

                conn.setRequestProperty("Connection", "Keep-Alive");

                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + fileToUpload.getName() +"\"" + lineEnd);
                dos.writeBytes(lineEnd);
                FileInputStream fileInputStream = new FileInputStream(fileToUpload);
                // create a buffer of maximum size
                int bytesAvailable = fileInputStream.available();

                int maxBufferSize = 10240;
                int bufferSize = Math.min(bytesAvailable, maxBufferSize);
                byte[ ] buffer = new byte[bufferSize];

                // read file and write it into form...
                int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0)
                {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable,maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0,bufferSize);
                }
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // retrieve response code and indicate that the upload was successful
                Log.d(TAG,"response message: " + conn.getResponseMessage());
                Log.d(TAG,"response code: " + conn.getResponseCode());
                int status = conn.getResponseCode();
                if (status == 200) {
                    uploadSuccessful = true;
                }
                // close streams
                fileInputStream.close();
                dos.flush();
                dos.close();
                // delete zip.zip
                fileToUpload.delete();


            } catch (IOException ie ) {
                Log.e(TAG, "uploadFile() - Exception: " + ie.getMessage());
                Log.e(TAG, Arrays.toString(ie.getStackTrace()));            }

        }

    }
}
