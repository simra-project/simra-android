package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper for central access to all shared prefs used by the app
 * Be aware that changing already implemented Strings to access the preferences resets the Preferences for App Users!
 */
public class SharedPref {
    // EXPLANATION: for different String formats:
    // The inconsistent SharePreference Strings formats are a result of keeping the App Backwards compatible without writing migrations.
    // Otherwise settings in already installed Apps would be lost.
    public static final String SHARED_PREF_NAME = "simraPrefs";
    public static final int DEFAULT_MODE = Context.MODE_PRIVATE;

    /**
     * @deprecated
     */
    public static String lookUpSharedPrefs(String key, String defValue, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        return sharedPrefs.getString(key, defValue);
    }

    /**
     * @deprecated
     */
    public static int lookUpIntSharedPrefs(String key, int defValue, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        return sharedPrefs.getInt(key, defValue);
    }

    /**
     * @deprecated
     */
    public static boolean lookUpBooleanSharedPrefs(String key, boolean defValue, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        return sharedPrefs.getBoolean(key, defValue);
    }

    /**
     * @deprecated
     */
    public static void writeToSharedPrefs(String key, String value, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * @deprecated
     */
    public static void writeIntToSharedPrefs(String key, int value, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * @deprecated
     */
    public static void writeBooleanToSharedPrefs(String key, boolean value, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private static Boolean readBooleanFromAppSharedPrefs(String name, boolean defaultValue, String sharedPrefName, Context context) {
        return context.getApplicationContext().getSharedPreferences(sharedPrefName, SharedPref.DEFAULT_MODE).getBoolean(name, defaultValue);
    }

    private static void writeBooleanToAppSharedPrefsAsync(String name, boolean value, String sharedPrefName, Context context) {
        context.getApplicationContext().getSharedPreferences(sharedPrefName, SharedPref.DEFAULT_MODE).edit().putBoolean(name, value).apply();
    }

    private static int readIntegerFromAppSharedPrefs(String name, int defaultValue, String sharedPrefName, Context context) {
        return context.getApplicationContext().getSharedPreferences(sharedPrefName, SharedPref.DEFAULT_MODE).getInt(name, defaultValue);
    }

    private static void writeIntegerToAppSharedPrefsAsync(String name, int value, String sharedPrefName, Context context) {
        context.getApplicationContext().getSharedPreferences(sharedPrefName, SharedPref.DEFAULT_MODE).edit().putInt(name, value).apply();
    }

    private static String readStringFromAppSharedPrefs(String name, String defaultValue, String sharedPrefName, Context context) {
        return context.getApplicationContext().getSharedPreferences(sharedPrefName, SharedPref.DEFAULT_MODE).getString(name, defaultValue);
    }

    private static void writeStringToAppSharedPrefsAsync(String name, String value, String sharedPrefName, Context context) {
        context.getApplicationContext().getSharedPreferences(sharedPrefName, SharedPref.DEFAULT_MODE).edit().putString(name, value).apply();
    }

    private static long readLongFromAppSharedPrefs(String name, long defaultValue, String sharedPrefName, Context context) {
        return context.getApplicationContext().getSharedPreferences(sharedPrefName, SharedPref.DEFAULT_MODE).getLong(name, defaultValue);
    }

    private static void writeLongToAppSharedPrefsAsync(String name, long value, String sharedPrefName, Context context) {
        context.getApplicationContext().getSharedPreferences(sharedPrefName, SharedPref.DEFAULT_MODE).edit().putLong(name, value).apply();
    }


    public static class Settings {
        // See top of this class before changing this string
        private static final String SETTINGS = "Settings-";

        public static class Radmesser {
            private static final String RADMESSER_ENABLED = SETTINGS + "RADMESSER_ENABLED";

            public static boolean isEnabled(Context context) {
                return readBooleanFromAppSharedPrefs(RADMESSER_ENABLED, false, SHARED_PREF_NAME, context);
            }

            public static void setEnabled(Boolean enabled, Context context) {
                writeBooleanToAppSharedPrefsAsync(RADMESSER_ENABLED, enabled, SHARED_PREF_NAME, context);
            }
        }

        public static class DisplayUnit {
            // See top of this class before changing this string
            public static final String DISPLAY_UNIT = SETTINGS + "Unit";

            public static void setDisplayUnit(UnitHelper.DISTANCE unit, Context context) {
                writeStringToAppSharedPrefsAsync(DISPLAY_UNIT, unit.getName(), SHARED_PREF_NAME, context);
            }

            public static UnitHelper.DISTANCE getDisplayUnit(Context context) {
                return UnitHelper.DISTANCE.parseFromString(readStringFromAppSharedPrefs(DISPLAY_UNIT, UnitHelper.DISTANCE.METRIC.getName(), SHARED_PREF_NAME, context));
            }

            public static boolean isImperial(Context context) {
                return getDisplayUnit(context) == UnitHelper.DISTANCE.IMPERIAL;
            }
        }

        public static class Ride {
            public static final String RIDE = "RIDE";

            public static class PrivacyDistance {
                // See top of this class before changing this string
                public static final String PRIVACY_DISTANCE = "Privacy-Distance";
                /**
                 * Maximum Privacy distance in meter
                 */
                private static final int MAX_DISTANCE = 50;
                /**
                 * Minimum Privacy distance in meter
                 */
                private static final int MIN_DISTANCE = 0;

                public static int getDistance(UnitHelper.DISTANCE unit, Context context) {
                    int value = readIntegerFromAppSharedPrefs(PRIVACY_DISTANCE, 30, SHARED_PREF_NAME, context);

                    if (unit == UnitHelper.DISTANCE.IMPERIAL) {
                        value = (int) UnitHelper.convertMeterToFeet(value);
                    }
                    return value;
                }

                public static void setDistance(int distance, UnitHelper.DISTANCE unit, Context context) {
                    switch (unit) {
                        case METRIC:
                            writeIntegerToAppSharedPrefsAsync(PRIVACY_DISTANCE, distance, SHARED_PREF_NAME, context);
                            break;
                        case IMPERIAL:
                            writeIntegerToAppSharedPrefsAsync(PRIVACY_DISTANCE, (int) UnitHelper.convertFeetToMeter(distance), SHARED_PREF_NAME, context);
                            break;
                    }
                }

                public static int getMaxDistance(UnitHelper.DISTANCE unit) {
                    int value = MAX_DISTANCE;
                    if (unit == UnitHelper.DISTANCE.IMPERIAL) {
                        value = (int) UnitHelper.convertMeterToFeet(value);
                    }
                    return value;
                }

                public static int getMinDistance(UnitHelper.DISTANCE unit) {
                    int value = MIN_DISTANCE;
                    if (unit == UnitHelper.DISTANCE.IMPERIAL) {
                        value = (int) UnitHelper.convertMeterToFeet(value);
                    }
                    return value;
                }
            }

            public static class PrivacyDuration {
                // See top of this class before changing this string
                public static final String PRIVACY_DURATION = "Privacy-Duration";
                /**
                 * Maximum Privacy duration in seconds
                 */
                private static final int MAX_DURATION = 50;
                /**
                 * Minimum Privacy duration in seconds
                 */
                private static final int MIN_DURATION = 0;

                public static long getDuration(Context context) {
                    return readLongFromAppSharedPrefs(PRIVACY_DURATION, 30L, SHARED_PREF_NAME, context);
                }

                public static void setDuration(long duration, Context context) {
                    writeLongToAppSharedPrefsAsync(PRIVACY_DURATION, duration, SHARED_PREF_NAME, context);
                }

                public static long getMaxDuration() {
                    return MAX_DURATION;
                }

                public static long getMinDuration() {
                    return MIN_DURATION;
                }
            }

            public static class BikeType {
                // See top of this class before changing this string
                public static final String BIKE_TYPE = SETTINGS + "BikeType";

                public static int getBikeType(Context context) {
                    return readIntegerFromAppSharedPrefs(BIKE_TYPE, 0, SHARED_PREF_NAME, context);
                }

                public static void setBikeType(int bikeType, Context context) {
                    writeIntegerToAppSharedPrefsAsync(BIKE_TYPE, bikeType, SHARED_PREF_NAME, context);
                }
            }

            public static class PhoneLocation {
                // See top of this class before changing this string
                public static final String PHONE_LOCATION = SETTINGS + "PhoneLocation";

                public static int getPhoneLocation(Context context) {
                    return readIntegerFromAppSharedPrefs(PHONE_LOCATION, 0, SHARED_PREF_NAME, context);
                }

                public static void setPhoneLocation(int phoneLocation, Context context) {
                    writeIntegerToAppSharedPrefsAsync(PHONE_LOCATION, phoneLocation, SHARED_PREF_NAME, context);
                }
            }

            public static class ChildOnBoard {
                // See top of this class before changing this string
                public static final String CHILD_ON_BOARD = SETTINGS + "Child";

                public static boolean isChildOnBoard(Context context) {
                    return getValue(context) == 1;
                }

                public static void setChildOnBoard(boolean isChildOnBoard, Context context) {
                    int value = 0;
                    if (isChildOnBoard) {
                        value = 1;
                    }
                    setChildOnBoardByValue(value, context);
                }

                public static void setChildOnBoardByValue(int isChildOnBoard, Context context) {
                    writeIntegerToAppSharedPrefsAsync(CHILD_ON_BOARD, isChildOnBoard, SHARED_PREF_NAME, context);
                }

                public static int getValue(Context context) {
                    return readIntegerFromAppSharedPrefs(CHILD_ON_BOARD, 0, SHARED_PREF_NAME, context);
                }
            }

            public static class BikeWithTrailer {
                // See top of this class before changing this string
                public static final String BIKE_WITH_TRAILER = SETTINGS + "Trailer";

                public static boolean hasTrailer(Context context) {
                    return getValue(context) == 1;
                }

                public static void setTrailer(boolean hasTrailer, Context context) {
                    int value = 0;
                    if (hasTrailer) {
                        value = 1;
                    }
                    setTrailerByValue(value, context);
                }

                public static void setTrailerByValue(int hasTrailer, Context context) {
                    writeIntegerToAppSharedPrefsAsync(BIKE_WITH_TRAILER, hasTrailer, SHARED_PREF_NAME, context);
                }

                public static int getValue(Context context) {
                    return readIntegerFromAppSharedPrefs(BIKE_WITH_TRAILER, 0, SHARED_PREF_NAME, context);
                }
            }
        }
    }
}
