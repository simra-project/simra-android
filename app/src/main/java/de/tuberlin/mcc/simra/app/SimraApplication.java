package de.tuberlin.mcc.simra.app;

import android.app.Application;

import io.sentry.android.core.SentryAndroid;

public class SimraApplication extends Application {
    public void onCreate() {
        super.onCreate();
        /**
         * Manual Initialization of the Sentry Android SDK
         * @Context - Instance of the Android Context
         * @Options - Call back function that you need to provide to be able to modify the options.
         * The call back function is provided with the options loaded from the manifest.
         *
         */
        if (BuildConfig.BUILD_TYPE != "debug" && BuildConfig.SENTRY_DSN != null) {
            SentryAndroid.init(this, options -> {
                options.setDsn(BuildConfig.SENTRY_DSN);
                options.setEnvironment(BuildConfig.IS_PRODUCTION ? "production" : "pre-production");
                options.setRelease(String.valueOf(BuildConfig.VERSION_CODE));
            });
        }
    }
}