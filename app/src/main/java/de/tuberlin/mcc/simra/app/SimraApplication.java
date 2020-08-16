package de.tuberlin.mcc.simra.app;

import android.app.Application;
import android.os.StrictMode;

import io.sentry.android.core.SentryAndroid;

/**
 * Simra Main Application Class.
 */
public class SimraApplication extends Application {
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.BUILD_TYPE == "debug") {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .build());
        }

        /**
         * Manual Initialization of the Sentry Android SDK
         * @Context - Instance of the Android Context
         * @Options - Call back function that you need to provide to be able to modify the options.
         * The call back function is provided with the options loaded from the manifest.
         *
         */
        // TODO: Uncomment for go live
        //if (BuildConfig.BUILD_TYPE != "debug" && BuildConfig.SENTRY_DSN != null) {
        SentryAndroid.init(this, options -> {
            options.setDsn(BuildConfig.SENTRY_DSN);
            options.setEnvironment(BuildConfig.IS_PRODUCTION ? "production" : "pre-production");
            options.setRelease(String.valueOf(BuildConfig.VERSION_CODE));
        });
        //}
    }
}