package de.tuberlin.mcc.simra.app.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.bugsnag.android.Bugsnag;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Date;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.activities.StartActivity;

import static de.tuberlin.mcc.simra.app.util.Utils.overwriteFile;

public class LoggingExceptionActivity extends AppCompatActivity implements Thread.UncaughtExceptionHandler {

    private final static String TAG = LoggingExceptionActivity.class.getSimpleName() + "_LOG";
    private final Context context;


    public LoggingExceptionActivity(Context context) {
        this.attachBaseContext(context);
        this.context = context;
        // we should store the current exception handler -- to invoke it for all not handled exceptions ...
        Thread.UncaughtExceptionHandler rootHandler = Thread.getDefaultUncaughtExceptionHandler();
        // we replace the exception handler now with us -- we will properly dispatch the exceptions ...
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        try {
            Log.d(TAG, "called for " + ex.getClass());
            // assume we would write each error in one file ...

            // log this exception ...

            StringBuilder stackTrace = new StringBuilder();
            for (int i = 0; i < ex.getStackTrace().length; i++) {
                stackTrace.append(ex.getStackTrace()[i]).append("\n");
            }
            String causeTrace = "";
            if (ex.getCause() != null) {
                for (int i = 0; i < ex.getCause().getStackTrace().length; i++) {
                    stackTrace.append(ex.getCause().getStackTrace()[i]).append("\n");
                }
            }
            String fileInfoLine = BuildConfig.VERSION_CODE + "#1" + System.lineSeparator();

            String errorReport =
                    "System Timestamp: " + System.currentTimeMillis() + "\n" +
                            "Build.VERSION.RELEASE: " + Build.VERSION.RELEASE + "\n" +
                            "Build.DEVICE: " + Build.DEVICE + "\n" +
                            "Build.MODEL: " + Build.MODEL + "\n" +
                            "Build.PRODUCT: " + Build.PRODUCT + "\n" +
                            "App Version: " + BuildConfig.VERSION_CODE + "\n" +
                            "Exception in: " + context.getClass().getName()
                            + " " + ex.getClass().getName() + "\n" +
                            ex.getMessage() + "\n" +
                            "stackTrace: " + stackTrace + "\n" +
                            "causeTrace: " + causeTrace + "\n";

            overwriteFile((fileInfoLine + errorReport), new File(IOUtils.Directories.getBaseFolderPath(this) + "CRASH_REPORT" + new Date().toString() + ".txt"));

            SharedPreferences sharedPrefs = getApplicationContext()
                    .getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sharedPrefs.edit();

            editor.putBoolean("NEW-UNSENT-ERROR", true);
            editor.commit();

            // Toast.makeText(this, R.string.crash_error,Toast.LENGTH_LONG).show();

            System.exit(0);
            // restartApp();

        } catch (Exception e) {
            Bugsnag.notify(e);
            Log.e(TAG, "Exception Logger failed!", e);
        }

    }

    private void restartApp() {
        Intent intent = new Intent(getApplicationContext(), StartActivity.class);
        int mPendingIntentId = 1337;
        PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId, intent, PendingIntent.FLAG_IMMUTABLE);
        AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }
}