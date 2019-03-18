package app.com.example.android.octeight;

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;


import static app.com.example.android.octeight.Constants.LOCALE_ABVS;
import static app.com.example.android.octeight.Utils.getAppVersionNumber;
import static app.com.example.android.octeight.Utils.lookUpBooleanSharedPrefs;
import static app.com.example.android.octeight.Utils.lookUpIntSharedPrefs;
import static app.com.example.android.octeight.Utils.lookUpSharedPrefs;
import static app.com.example.android.octeight.Utils.readContentFromFile;
import static app.com.example.android.octeight.Utils.readContentFromFileAndIncreaseFileVersion;
import static app.com.example.android.octeight.Utils.writeBooleanToSharedPrefs;
import static app.com.example.android.octeight.Utils.writeToSharedPrefs;

public class UploadService extends Service {

    NotificationManagerCompat notificationManager;
    private PowerManager.WakeLock wakeLock = null;
    // For Managing the notification shown while the service is running
    int notificationId = 1453;


    int numberOfTasks = 0;

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
            Log.d(TAG, "onPostExecute()");
            super.onPostExecute(s);
            notificationManager.cancel(notificationId);
            wakeLock.release();
            stopSelf();
        }

        private void uploadFile(Context context) throws IOException {


            boolean sendCrashReportPermitted = lookUpBooleanSharedPrefs("NEW-UNSENT-ERROR",false,"simraPrefs",context);

            // Sending / Updating profile with each upload
            String profileContentToSend = readContentFromFile("profile.csv",context);
            String profilePassword = lookUpSharedPrefs("profile.csv","-1","keyPrefs",context);
            Log.d(TAG, "Saved password: " + profilePassword);
            if(profilePassword.equals("-1")){
                Log.d(TAG, "sending profile with POST");
                String hashPassword = postUpload("profile.csv",profileContentToSend);
                writeToSharedPrefs("profile.csv",hashPassword,"keyPrefs",context);
                Log.d(TAG, "hashPassword: " + hashPassword + " written to keyPrefs");
            } else {
                Log.d(TAG, "sending profile with PUT");
                String fileHash = profilePassword.split(",")[0];
                String filePassword = profilePassword.split(",")[1];
                String response = putUpload("profile.csv"+fileHash, filePassword, profileContentToSend);
                Log.d(TAG, "PUT response: " + response);
            }

            File[] dirFiles = getFilesDir().listFiles();

            Log.d(TAG, "dirFiles: " + Arrays.deepToString(dirFiles));

            Log.d(TAG, "sendCrashReportPermitted: " + sendCrashReportPermitted);
            // If there was a crash and the user permitted to send the crash logs, upload all data
            // in order to enable reconstructing the error.
            if (sendCrashReportPermitted) {
                String path = Constants.APP_PATH + "shared_prefs/simraPrefs.xml";
                String ts = String.valueOf(System.currentTimeMillis());
                String key = "CRASH_" + ts + "_" + path;
                postUpload(key, readContentFromFile(path,context));

                for (int i = 0; i < dirFiles.length; i++) {
                    path = dirFiles[i].getName();
                    if (!(new File(path)).isDirectory()) {
                        String contentToSend = readContentFromFileAndIncreaseFileVersion(path,context);
                        key = "CRASH_" + ts + "_" + path;
                        postUpload(key,contentToSend);
                        if (path.startsWith("CRASH")) {
                            boolean deleted = context.deleteFile(path);
                            Log.d(TAG, path + " deleted: " + deleted);
                        }
                    }
                }
                // set the boolean "NEW-UNSENT-ERROR" in simraPrefs.xml to false
                // so that the StartActivity doesn't think there are still unsent
                // crash logs.
                writeBooleanToSharedPrefs("NEW-UNSENT-ERROR",false,"simraPrefs", context);

                // If there wasn't a crash or the user did not gave us the permission, upload
            } else {
                ArrayList<String> filesToUpload;
                filesToUpload = intent.getStringArrayListExtra("PathsToUpload");
                Log.d(TAG, "DataString: " + intent.getDataString());
                UploadService.this.setNumberOfTasks((filesToUpload.size()));

                // For each ride to upload...
                for (int i = 0; i < filesToUpload.size(); i++) {
                    // ... find the corresponding ride csv file ...
                    for (int j = 0; j < dirFiles.length; j++) {
                        String nameOfFile = dirFiles[j].getName();
                        if (filesToUpload.get(i).equals(nameOfFile)) {
                            // ... and upload.
                            // makePostTestPhaseFiles(nameOfFile, id);
                            String key = filesToUpload.get(i).split("_")[0];
                            String accEventName = "accEvents" + key + ".csv";
                            // makePostTestPhaseFiles(accEventName, id);
                            String contentToSend = Utils.appendFromFileToFile(accEventName, nameOfFile, context);
                            String password = lookUpSharedPrefs(key, "-1", "keyPrefs", context);
                            Log.d(TAG, "Saved password: " + password);
                            if (password.equals("-1")) {
                                Log.d(TAG, "sending ride with POST" + key);
                                String hashPassword = postUpload(key, contentToSend);
                                writeToSharedPrefs(key, hashPassword, "keyPrefs", context);
                                Log.d(TAG, "hashPassword: " + hashPassword + " written to keyPrefs");
                            } else {
                                Log.d(TAG, "sending ride with PUT" + key);
                                String fileHash = password.split(",")[0];
                                String filePassword = password.split(",")[1];
                                String response = putUpload(fileHash, filePassword, contentToSend);
                                Log.d(TAG, "PUT response: " + response);
                            }
                        }
                    }
                }
            }
        }

        private String postUpload (String fileName, String contentToSend) throws IOException {

            // Calculating hash for server access.
            Date dateToday = new Date();
            String clientHash = Integer.toHexString((Constants.DATE_PATTERN_SHORT.format(dateToday) + Constants.UPLOAD_HASH_SUFFIX).hashCode());

            Log.d(TAG, "clientHash: " + clientHash);
            Log.d(TAG, "dateToday: " + dateToday.toString());
            Log.d(TAG, "beforeHash: " + (Constants.DATE_PATTERN_SHORT.format(dateToday) + Constants.UPLOAD_HASH_SUFFIX));

            int localeInt = lookUpIntSharedPrefs("Profile-Region",0,"simraPrefs",context);
            String locale = LOCALE_ABVS[localeInt];

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            SSLContext sslContext = null;
            try{
                // Load CAs from an InputStream
                // (could be from a resource or ByteArrayInputStream or ...)
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                // From https://www.washington.edu/itconnect/security/ca/load-der.crt
                // File certificateFile = new File (getResources().getAssets().open("server.cer"));// getFileStreamPath("server.cer");
                // Log.d(TAG,"file: " + certificateFile.getAbsolutePath());

                InputStream caInput = new BufferedInputStream(getResources().getAssets().open("server.cer"));//new FileInputStream(certificateFile));
                Certificate ca;

                try {
                    ca = cf.generateCertificate(caInput);
                    Log.d(TAG,"ca=" + ((X509Certificate) ca).getSubjectDN());
                } finally {
                    caInput.close();
                }

                // Create a KeyStore containing our trusted CAs
                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null, null);
                keyStore.setCertificateEntry("ca", ca);
                Log.d(TAG,"subjectDN: " + ((X509Certificate) ca).getSubjectDN());

                // Create a TrustManager that trusts the CAs in our KeyStore
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);

                // Create an SSLContext that uses our TrustManager
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }

            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    HostnameVerifier hv =
                            HttpsURLConnection.getDefaultHostnameVerifier();
                    Log.d(TAG, "hv.verify: " + hv.verify("vm3.mcc.tu-berlin.de", session));
                    Log.d(TAG, "hostname: " + hostname);
                    return true; //hv.verify("vm3.mcc.tu-berlin.de", session);
                }
            };
            int appVersion = getAppVersionNumber(context);
            // Tell the URLConnection to use a SocketFactory from our SSLContext
            // URL url = new URL(Constants.MCC_VM3 + "upload/" + fileName + "?version=" + appVersion + "&loc=" + locale + "&clientHash=" + clientHash);
            URL url = new URL(Constants.MCC_VM2 + appVersion + "/" + "upload?fileName=" + fileName + "&loc=" + locale + "&clientHash=" + clientHash);

            HttpsURLConnection urlConnection =
                    (HttpsURLConnection)url.openConnection();
            urlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setHostnameVerifier(hostnameVerifier);
            urlConnection.setRequestProperty("Content-Type","text/plain");
            byte[] outputInBytes = contentToSend.getBytes("UTF-8");
            OutputStream os = urlConnection.getOutputStream();
            os.write( outputInBytes );
            os.close();
            int status = urlConnection.getResponseCode();
            Log.d(TAG, "Server status: " + status);
            String response = urlConnection.getResponseMessage();
            Log.d(TAG, "Server Response: " + response);
            UploadService.this.decreaseNumberOfTasks();

            return response;
        }

        private String putUpload (String fileHash, String filePassword, String contentToSend) throws IOException {

            // Calculating hash for server access.
            Date dateToday = new Date();
            String clientHash = Integer.toHexString((Constants.DATE_PATTERN_SHORT.format(dateToday) + Constants.UPLOAD_HASH_SUFFIX).hashCode());

            Log.d(TAG, "clientHash: " + clientHash);
            Log.d(TAG, "dateToday: " + dateToday.toString());
            Log.d(TAG, "beforeHash: " + (Constants.DATE_PATTERN_SHORT.format(dateToday) + Constants.UPLOAD_HASH_SUFFIX));

            int localeInt = lookUpIntSharedPrefs("Profile-Region",0,"simraPrefs",context);
            String locale = LOCALE_ABVS[localeInt];

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            SSLContext sslContext = null;
            try{
                // Load CAs from an InputStream
                // (could be from a resource or ByteArrayInputStream or ...)
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                // From https://www.washington.edu/itconnect/security/ca/load-der.crt
                // File certificateFile = new File (getResources().getAssets().open("server.cer"));// getFileStreamPath("server.cer");
                // Log.d(TAG,"file: " + certificateFile.getAbsolutePath());

                InputStream caInput = new BufferedInputStream(getResources().getAssets().open("server.cer"));//new FileInputStream(certificateFile));
                Certificate ca;

                try {
                    ca = cf.generateCertificate(caInput);
                    Log.d(TAG,"ca=" + ((X509Certificate) ca).getSubjectDN());
                } finally {
                    caInput.close();
                }

                // Create a KeyStore containing our trusted CAs
                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null, null);
                keyStore.setCertificateEntry("ca", ca);
                Log.d(TAG,"subjectDN: " + ((X509Certificate) ca).getSubjectDN());

                // Create a TrustManager that trusts the CAs in our KeyStore
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);

                // Create an SSLContext that uses our TrustManager
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }

            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    HostnameVerifier hv =
                            HttpsURLConnection.getDefaultHostnameVerifier();
                    Log.d(TAG, "hv.verify: " + hv.verify("vm3.mcc.tu-berlin.de", session));
                    Log.d(TAG, "hostname: " + hostname);
                    return true; //hv.verify("vm3.mcc.tu-berlin.de", session);
                }
            };
            int appVersion = getAppVersionNumber(context);
            // Tell the URLConnection to use a SocketFactory from our SSLContext
            // URL url = new URL(Constants.MCC_VM3 + "upload/" + fileHash + "?version=" + appVersion + "&loc=" + locale + "&clientHash=" + clientHash);
            URL url = new URL(Constants.MCC_VM2 + appVersion + "/" + "update?fileHash=" + fileHash + "&filePassword=" + filePassword + "&loc=" + locale + "&clientHash=" + clientHash);

            HttpsURLConnection urlConnection =
                    (HttpsURLConnection)url.openConnection();
            urlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
            urlConnection.setRequestMethod("PUT");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setHostnameVerifier(hostnameVerifier);
            urlConnection.setRequestProperty("Content-Type","text/plain");
            Log.d(TAG, "contentToSend.length(): " + contentToSend.length());
            byte[] outputInBytes = contentToSend.getBytes("UTF-8");
            OutputStream os = urlConnection.getOutputStream();
            os.write( outputInBytes );
            os.close();
            int status = urlConnection.getResponseCode();
            Log.d(TAG, "Server status: " + status);
            String response = urlConnection.getResponseMessage();
            Log.d(TAG, "Server Response: " + response);
            UploadService.this.decreaseNumberOfTasks();

            return response;
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
                .setContentTitle(getResources().getString(R.string.recordingNotificationTitle))
                .setContentText(getResources().getString(R.string.recordingNotificationBody))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent);

        return mBuilder;

    }

    //Converts the contents of an InputStream to a String.
    public String readStream(InputStream stream, int maxReadSize)
            throws IOException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] rawBuffer = new char[maxReadSize];
        int readSize;
        StringBuffer buffer = new StringBuffer();
        while (((readSize = reader.read(rawBuffer)) != -1) && maxReadSize > 0) {
            if (readSize > maxReadSize) {
                readSize = maxReadSize;
            }
            buffer.append(rawBuffer, 0, readSize);
            maxReadSize -= readSize;
        }
        return buffer.toString();
    }
}
