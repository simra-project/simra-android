package de.tuberlin.mcc.simra.app.util;

import android.content.Context;

public class SharedPref {
    public static final String SHARED_PREF_NAME = "simra";
    public static final int DEFAULT_MODE = Context.MODE_PRIVATE;
    public static class Settings {
        public static class Radmesser {
            public static final String RADMESSER_ENABLED = "RADMESSER_ENABLED";
            public static boolean isEnabled(Context ctx){
                return ctx.getSharedPreferences(SHARED_PREF_NAME, DEFAULT_MODE).getBoolean(RADMESSER_ENABLED, false);
            }
            public static void setEnabled(Boolean enabled, Context ctx){
                ctx.getSharedPreferences(SHARED_PREF_NAME, DEFAULT_MODE).edit().putBoolean(RADMESSER_ENABLED, enabled).apply();
            }
        }

    }
}
