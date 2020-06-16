package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

public class SharedPref {
    public static final String SHARED_PREF_NAME = "simra";
    public static final int DEFAULT_MODE = Context.MODE_PRIVATE;

    public static String lookUpSharedPrefs(String key, String defValue, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        return sharedPrefs.getString(key, defValue);
    }

    public static int lookUpIntSharedPrefs(String key, int defValue, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        return sharedPrefs.getInt(key, defValue);
    }

    public static long lookUpLongSharedPrefs(String key, long defValue, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        return sharedPrefs.getLong(key, defValue);
    }

    public static boolean lookUpBooleanSharedPrefs(String key, boolean defValue, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        return sharedPrefs.getBoolean(key, defValue);
    }

    public static void writeToSharedPrefs(String key, String value, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void writeIntToSharedPrefs(String key, int value, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void writeLongToSharedPrefs(String key, long value, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static void writeBooleanToSharedPrefs(String key, boolean value, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void copySharedPreferences(String sourceSharedPref, String newSharedPref, Context context) {
        SharedPreferences sourceSP = context.getApplicationContext()
                .getSharedPreferences(sourceSharedPref, Context.MODE_PRIVATE);

        SharedPreferences newSP = context.getApplicationContext()
                .getSharedPreferences(newSharedPref, Context.MODE_PRIVATE);
        SharedPreferences.Editor newE = newSP.edit();

        for (Map.Entry<String, ?> kvPair : sourceSP.getAll().entrySet()) {
            String key = kvPair.getKey();
            Object value = kvPair.getValue();
            if (value instanceof Float) {
                newE.putFloat(key, (Float) value);
            } else if (value instanceof Integer) {
                newE.putInt(key, (Integer) value);
            } else if (value instanceof String) {
                newE.putString(key, (String) value);
            } else if (value instanceof Boolean) {
                newE.putBoolean(key, (Boolean) value);
            } else if (value instanceof Long) {
                newE.putLong(key, (Long) value);
            }
        }
        newE.apply();
    }

    public static class Settings {
        public static class Radmesser {
            public static final String RADMESSER_ENABLED = "RADMESSER_ENABLED";

            public static boolean isEnabled(Context ctx) {
                return ctx.getApplicationContext().getSharedPreferences(SHARED_PREF_NAME, DEFAULT_MODE).getBoolean(RADMESSER_ENABLED, false);
            }

            public static void setEnabled(Boolean enabled, Context ctx) {
                ctx.getApplicationContext().getSharedPreferences(SHARED_PREF_NAME, DEFAULT_MODE).edit().putBoolean(RADMESSER_ENABLED, enabled).apply();
            }
        }

    }
}
