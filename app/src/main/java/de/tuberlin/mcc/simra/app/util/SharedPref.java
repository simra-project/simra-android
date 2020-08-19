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

    private static Boolean readBooleanFromAppSharedPrefs(String name, Context context) {
        return context.getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREF_NAME, SharedPref.DEFAULT_MODE).getBoolean(name, false);
    }

    private static void writeBooleanToAppSharedPrefsAsync(String name, boolean value, Context context) {
        context.getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREF_NAME, SharedPref.DEFAULT_MODE).edit().putBoolean(name, value).apply();
    }

    private static int readIntegerFromAppSharedPrefs(String name, int defaultValue, Context context) {
        return context.getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREF_NAME, SharedPref.DEFAULT_MODE).getInt(name, defaultValue);
    }

    private static void writeIntegerToAppSharedPrefsAsync(String name, int value, Context context) {
        context.getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREF_NAME, SharedPref.DEFAULT_MODE).edit().putInt(name, value).apply();
    }

    private static String readStringFromAppSharedPrefs(String defaultValue, Context context) {
        return context.getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREF_NAME, SharedPref.DEFAULT_MODE).getString(Settings.DisplayUnit.DISPLAY_UNIT, defaultValue);
    }

    private static void writeStringToAppSharedPrefsAsync(String value, Context context) {
        context.getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREF_NAME, SharedPref.DEFAULT_MODE).edit().putString(Settings.DisplayUnit.DISPLAY_UNIT, value).apply();
    }

    private static long readLongFromAppSharedPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREF_NAME, SharedPref.DEFAULT_MODE).getLong(Settings.Ride.PrivacyDuration.PRIVACY_DURATION, 30);
    }

    private static void writeLongToAppSharedPrefsAsync(long value, Context context) {
        context.getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREF_NAME, SharedPref.DEFAULT_MODE).edit().putLong(Settings.Ride.PrivacyDuration.PRIVACY_DURATION, value).apply();
    }

    public static class App {
        // See top of this class before changing this string
        private static final String APP = "App-";

        /**
         * Grouped State for Crash Data
         */
        public static class Crash {
            /**
             * Whether the use gave his permission to send Crash Reports
             */
            public static class SendCrashReportAllowed {
                private static final String SEND_CRASH_ALLOWED = "SEND-CRASH";
                private static final String ALLOWED = "ALWAYS-SEND";
                private static final String UNKNOWN = "ALLOWED";
                private static final String DISALLOWED = "NEVER-SEND";

                public static void setAllowed(Context context) {
                    setStatus(ALLOWED, context);
                }

                public static void setDisallowed(Context context) {
                    setStatus(DISALLOWED, context);
                }

                public static void reset(Context context) {
                    setStatus(UNKNOWN, context);
                }

                public static boolean isAllowed(Context context) {
                    return getStatus(context).equals(ALLOWED);
                }

                public static boolean isUnknown(Context context) {
                    return getStatus(context).equals(UNKNOWN);
                }

                public static String getStatus(Context context) {
                    return readStringFromAppSharedPrefs(UNKNOWN, context);
                }

                public static void setStatus(String enabled, Context context) {
                    writeStringToAppSharedPrefsAsync(enabled, context);
                }
            }

            public static class NewCrash {
                private static final String NEW_CRASH_REPORT = "NEW-UNSENT-ERROR";

                public static boolean isActive(Context context) {
                    return readBooleanFromAppSharedPrefs(NEW_CRASH_REPORT, context);
                }

                public static void setEnabled(Boolean enabled, Context context) {
                    writeBooleanToAppSharedPrefsAsync(NEW_CRASH_REPORT, enabled, context);
                }
            }
        }
    }


    public static class Settings {
        // See top of this class before changing this string
        private static final String SETTINGS = "Settings-";

        public static class Radmesser {
            private static final String RADMESSER_ENABLED = SETTINGS + "RADMESSER_ENABLED";

            public static boolean isEnabled(Context context) {
                return readBooleanFromAppSharedPrefs(RADMESSER_ENABLED, context);
            }

            public static void setEnabled(Boolean enabled, Context context) {
                writeBooleanToAppSharedPrefsAsync(RADMESSER_ENABLED, enabled, context);
            }
        }

        public static class DisplayUnit {
            // See top of this class before changing this string
            public static final String DISPLAY_UNIT = SETTINGS + "Unit";

            public static void setDisplayUnit(UnitHelper.DISTANCE unit, Context context) {
                writeStringToAppSharedPrefsAsync(unit.getName(), context);
            }

            public static UnitHelper.DISTANCE getDisplayUnit(Context context) {
                return UnitHelper.DISTANCE.parseFromString(readStringFromAppSharedPrefs(UnitHelper.DISTANCE.METRIC.getName(), context));
            }

            public static boolean isImperial(Context context) {
                return getDisplayUnit(context) == UnitHelper.DISTANCE.IMPERIAL;
            }
        }

        public static class IncidentGenerationAIActive {
            // See top of this class before changing this string
            public static final String AI_ENABLED = SETTINGS + "AI";

            public static void setAIEnabled(boolean enabled, Context context) {
                writeBooleanToAppSharedPrefsAsync(AI_ENABLED, enabled, context);
            }

            public static boolean getAIEnabled(Context context) {
                return readBooleanFromAppSharedPrefs(AI_ENABLED,  context);
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
                    int value = readIntegerFromAppSharedPrefs(PRIVACY_DISTANCE, 30, context);

                    if (unit == UnitHelper.DISTANCE.IMPERIAL) {
                        value = (int) UnitHelper.convertMeterToFeet(value);
                    }
                    return value;
                }

                public static void setDistance(int distance, UnitHelper.DISTANCE unit, Context context) {
                    switch (unit) {
                        case METRIC:
                            writeIntegerToAppSharedPrefsAsync(PRIVACY_DISTANCE, distance, context);
                            break;
                        case IMPERIAL:
                            writeIntegerToAppSharedPrefsAsync(PRIVACY_DISTANCE, (int) UnitHelper.convertFeetToMeter(distance), context);
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
                    return readLongFromAppSharedPrefs(context);
                }

                public static void setDuration(long duration, Context context) {
                    writeLongToAppSharedPrefsAsync(duration, context);
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
                    return readIntegerFromAppSharedPrefs(BIKE_TYPE, 0, context);
                }

                public static void setBikeType(int bikeType, Context context) {
                    writeIntegerToAppSharedPrefsAsync(BIKE_TYPE, bikeType, context);
                }
            }

            public static class PhoneLocation {
                // See top of this class before changing this string
                public static final String PHONE_LOCATION = SETTINGS + "PhoneLocation";

                public static int getPhoneLocation(Context context) {
                    return readIntegerFromAppSharedPrefs(PHONE_LOCATION, 0, context);
                }

                public static void setPhoneLocation(int phoneLocation, Context context) {
                    writeIntegerToAppSharedPrefsAsync(PHONE_LOCATION, phoneLocation, context);
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
                    writeIntegerToAppSharedPrefsAsync(CHILD_ON_BOARD, isChildOnBoard, context);
                }

                public static int getValue(Context context) {
                    return readIntegerFromAppSharedPrefs(CHILD_ON_BOARD, 0, context);
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
                    writeIntegerToAppSharedPrefsAsync(BIKE_WITH_TRAILER, hasTrailer, context);
                }

                public static int getValue(Context context) {
                    return readIntegerFromAppSharedPrefs(BIKE_WITH_TRAILER, 0, context);
                }
            }

            public static class OvertakeWidth {
                // See top of this class before changing this string
                public static final String OVERTAKE_WIDTH = SETTINGS + "OVERTAKE_WIDTH";
                /**
                 * Safety Clearance + Side Mirror Length in cm
                 */
                private static final int SAFETY_CLEARANCE_AND_SIDE_MIRROR_WIDTH = 150 + 13;

                public static int getWidth(Context context) {
                    return readIntegerFromAppSharedPrefs(OVERTAKE_WIDTH, SAFETY_CLEARANCE_AND_SIDE_MIRROR_WIDTH, context);
                }

                public static void setWidth(int width, Context context) {
                    writeIntegerToAppSharedPrefsAsync(OVERTAKE_WIDTH, width, context);
                }

                public static int getHandlebarWidth(Context context) {
                    return calculateHandleBarWidthFromTotal(getWidth(context));
                }

                public static void setTotalWidthThroughHandlebarWidth(int width, Context context) {
                    setWidth(calculateTotalWidth(width), context);
                }

                private static int calculateTotalWidth(int handlebarWidth) {
                    return handlebarWidth + SAFETY_CLEARANCE_AND_SIDE_MIRROR_WIDTH;
                }

                private static int calculateHandleBarWidthFromTotal(int totalWidth) {
                    return totalWidth - SAFETY_CLEARANCE_AND_SIDE_MIRROR_WIDTH;
                }
            }

            public static class PicturesDuringRide {
                // See top of this class before changing this string
                public static final String PICTURES_DURING_RIDE = SETTINGS + "PICTURES_DURING_RIDE";

                public static boolean isActivated(Context context) {
                    return readBooleanFromAppSharedPrefs(PICTURES_DURING_RIDE, context);
                }

                public static void setMakePictureDuringRide(boolean activated, Context context) {
                    writeBooleanToAppSharedPrefsAsync(PICTURES_DURING_RIDE, activated, context);
                }
            }

            public static class PicturesDuringRideInterval {
                // See top of this class before changing this string
                public static final String PICTURES_DURING_RIDE_INTERVAL = SETTINGS + "PICTURES_DURING_RIDE_INTERVAL";

                public static int getInterval(Context context) {
                    return readIntegerFromAppSharedPrefs(PICTURES_DURING_RIDE_INTERVAL, 1, context);
                }

                public static void setInterval(int interval, Context context) {
                    writeIntegerToAppSharedPrefsAsync(PICTURES_DURING_RIDE_INTERVAL, interval, context);
                }
            }
        }
    }
}
