package app.com.example.android.octeight;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.CharSet;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Date;

public class LoggingExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final static String TAG = LoggingExceptionHandler.class.getSimpleName();

    //private final static String ERROR_FILE = "Error_History";

    private final Context context;
    private final Thread.UncaughtExceptionHandler rootHandler;

    public LoggingExceptionHandler(Context context) {
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

            String errorReport = ex.getClass().getSimpleName() + "\n" +
                    ex.getStackTrace() + "\n" + ex.getCause() + "\n" +
                    ex.getCause() + "\n" + ex.getMessage() + "\n" +
                    System.currentTimeMillis();

            //@TODO add device information to errorReport

            FileUtils.writeStringToFile(f, errorReport, (Charset) null);

            //Intent errorIntent = new Intent(LoggingExceptionHandler.this, UploadService.class);

        } catch (Exception e) {
            Log.e(TAG, "Exception Logger failed!", e);
        }

    }
}