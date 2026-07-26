package de.tuberlin.mcc.simra.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import de.tuberlin.mcc.simra.app.util.SystemBarInsets;

/**
 * Simra Main Application Class.
 */
public class SimraApplication extends Application {
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                // After onCreate/setContentView: pad content for system bars on every screen.
                SystemBarInsets.install(activity);
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }
}
