package app.com.example.android.octeight;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.CharSet;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Date;

public class LoggingExceptionHandler extends AppCompatActivity implements Thread.UncaughtExceptionHandler {

    private final static String TAG = LoggingExceptionHandler.class.getSimpleName();
    private final Activity context;
    private final Thread.UncaughtExceptionHandler rootHandler;
    RecorderService mBoundRecorderService;
    UploadService mBoundUploadService;

    public LoggingExceptionHandler(Activity context) {
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

            File f = new File(context.getFilesDir(), new Date().toString() + "_CRASH REPORT");
            // log this exception ...

            String errorReport = "Exception in: " + context.getClass().getSimpleName()
                    + ex.getClass().getSimpleName() + "\n" +
                    ex.getStackTrace() + "\n" + ex.getCause() + "\n" +
                    ex.getCause() + "\n" + ex.getMessage() + "\n" +
                    System.currentTimeMillis() + "\n" +
                    Build.VERSION.RELEASE + "\n" +
                    Build.DEVICE + "\n" +
                    Build.MODEL + "\n" +
                    Build.PRODUCT;

            FileUtils.writeStringToFile(f, errorReport, (Charset) null);

            Intent errorIntent = new Intent(LoggingExceptionHandler.this, this.context.getClass());

            errorIntent.putExtra("CRASH_REPORT", f.getAbsolutePath());
            startService(errorIntent);
            bindService(errorIntent, mUploadServiceConnection, Context.BIND_AUTO_CREATE);

        } catch (Exception e) {
            Log.e(TAG, "Exception Logger failed!", e);
        }

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // ServiceConnection for communicating with RecorderService
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private ServiceConnection mRecorderServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RecorderService.MyBinder myBinder = (RecorderService.MyBinder) service;
            mBoundRecorderService = myBinder.getService();
        }
    };

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // ServiceConnection for communicating with RecorderService
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private ServiceConnection mUploadServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            UploadService.MyBinder myBinder = (UploadService.MyBinder) service;
            mBoundUploadService = myBinder.getService();
        }
    };

}