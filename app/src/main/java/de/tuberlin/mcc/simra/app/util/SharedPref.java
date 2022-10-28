package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;

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
    public static void writeLongToSharedPrefs(String key, long value, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    /**
     * @deprecated
     */
    public static long lookUpLongSharedPrefs(String key, long defValue, String sharedPrefName, Context context) {
        SharedPreferences sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        return sharedPrefs.getLong(key, defValue);
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

    private static String readStringFromAppSharedPrefs(String name, String defaultValue, Context context) {
        return context.getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREF_NAME, SharedPref.DEFAULT_MODE).getString(name, defaultValue);
    }

    private static void writeStringToAppSharedPrefsAsync(String name, String value, Context context) {
        context.getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREF_NAME, SharedPref.DEFAULT_MODE).edit().putString(name, value).apply();
    }

    private static void deleteStringFromAppSharedPrefs(String name, Context context) {
        context.getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREF_NAME, SharedPref.DEFAULT_MODE).edit().remove(name).apply();
    }

    private static long readLongFromAppSharedPrefs(String name, long defaultValue, Context context) {
        return context.getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREF_NAME, SharedPref.DEFAULT_MODE).getLong(name, defaultValue);
    }

    private static void writeLongToAppSharedPrefsAsync(String name, long value, Context context) {
        context.getApplicationContext().getSharedPreferences(SharedPref.SHARED_PREF_NAME, SharedPref.DEFAULT_MODE).edit().putLong(name, value).apply();
    }

    public static void createEntry(String sharedPrefName, String line, Context context) {
        String[] sharedPrefsEntry = line.replaceAll("<"," ").replaceAll(">"," ").replaceAll("/","").trim().split(" ");
        // Log.d("SharedPref_LOG:","sharedPrefsEntry: " + Arrays.toString(sharedPrefsEntry));
        String entryType = sharedPrefsEntry[0];
        String entryName = sharedPrefsEntry[1].split("\"")[1];
        String entryValue = "";
        if (entryType.equals("string")) {
            entryValue = sharedPrefsEntry[2];
        } else {
            entryValue = sharedPrefsEntry[2].split("\"")[1];
        }
        switch (entryType) {
            case "string":
                writeToSharedPrefs(entryName, entryValue, sharedPrefName, context);
                break;
            case "boolean":
                writeBooleanToSharedPrefs(entryName, Boolean.parseBoolean(entryValue), sharedPrefName, context);
                break;
            case "int":
                writeIntToSharedPrefs(entryName, Integer.parseInt(entryValue), sharedPrefName, context);
                break;
            case "long":
                writeLongToSharedPrefs(entryName, Long.parseLong(entryValue), sharedPrefName, context);
                break;
        }
        // Log.d("SharedPref_LOG", "entry: " + entryType + " " + entryName + " " + entryValue);
    }

    public static void clearSharedPrefs(String sharedPrefName, Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(sharedPrefName,
                        Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }

    public static class App {
        // See top of this class before changing this string
        private static final String APP = "App-";

        /**
         * Last news number displayed from backend simRa_news.config
         */
        public static class News {
            private static final String LAST_SEEN_NEWS_ID = "LAST_SEEN_NEWS_ID";
            public static void setLastSeenNewsID(int lastSeenNewsID, Context context) {
                writeIntegerToAppSharedPrefsAsync(LAST_SEEN_NEWS_ID,lastSeenNewsID,context);
            }
            public static int getLastSeenNewsID(Context context) {
                return readIntegerFromAppSharedPrefs(LAST_SEEN_NEWS_ID,0,context);
            }
        }

        // RIDE-KEY
        public static class RideKey {
            private static final String RIDE_KEY = "RIDE-KEY";
            public static void setRideKey(int rideKey, Context context) {
                writeIntegerToAppSharedPrefsAsync(RIDE_KEY,rideKey,context);
            }
            public static int getRideKey(Context context) {
                return readIntegerFromAppSharedPrefs(RIDE_KEY,0,context);
            }
        }

        // OBS
        public static class OpenBikeSensor {
            private static final String OBS_START_TIME = "OBS-StartTime";
            private static final String OBS_DEVICE_NAME = "OBS-DeviceName";
            public static void setObsDeviceName(String obsDeviceName, Context context) {
                writeStringToAppSharedPrefsAsync(OBS_DEVICE_NAME, obsDeviceName, context);
            }
            public static String getObsDeviceName(Context context) {
                return readStringFromAppSharedPrefs(OBS_DEVICE_NAME, null, context);
            }
            public static void deleteObsDeviceName(Context context) {
                deleteStringFromAppSharedPrefs(OBS_DEVICE_NAME, context);
            }

        }

        /**
         * Grouped State for Crash Data
         */
        public static class Crash {
            /**
             * Whether the user gave his permission to send Crash Reports
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
                    return readStringFromAppSharedPrefs(SEND_CRASH_ALLOWED, UNKNOWN, context);
                }

                public static void setStatus(String enabled, Context context) {
                    writeStringToAppSharedPrefsAsync(SEND_CRASH_ALLOWED, enabled, context);
                }
            }

            public static class NewCrash {
                private static final String NEW_CRASH_REPORT = "NEW_CRASH_REPORT";

                public static boolean isActive(Context context) {
                    return readBooleanFromAppSharedPrefs(NEW_CRASH_REPORT, context);
                }

                public static void setEnabled(Boolean enabled, Context context) {
                    writeBooleanToAppSharedPrefsAsync(NEW_CRASH_REPORT, enabled, context);
                }
            }
        }

        /**
         * Last checked number of regions from getRegions()
         * Last regions number displayed from backend simRa_regions_coords_ID.config
         */
        public static class Regions {
            private static final String LAST_REGION_NUMBER_KNOWN = "LAST_REGION_NUMBER_KNOWN";
            public static void setLastRegionNumberKnown(int lastRegionNumberKnown, Context context) {
                writeIntegerToAppSharedPrefsAsync(LAST_REGION_NUMBER_KNOWN,lastRegionNumberKnown,context);
            }
            public static int getLastRegionNumberKnown(Context context) {
                return readIntegerFromAppSharedPrefs(LAST_REGION_NUMBER_KNOWN,0,context);
            }

            private static final String LAST_SEEN_REGIONS_ID = "LAST_SEEN_REGIONS_ID";
            public static void setLastSeenRegionsID(int lastSeenRegionsID, Context context) {
                writeIntegerToAppSharedPrefsAsync(LAST_SEEN_REGIONS_ID,lastSeenRegionsID,context);
            }
            public static int getLastSeenRegionsID(Context context) {
                return readIntegerFromAppSharedPrefs(LAST_SEEN_REGIONS_ID,0,context);
            }
        }

        /**
         * whether to show region prompt or not
         */
        public static class RegionsPrompt {
            private static final String DO_NOT_SHOW_REGION_PROMPT = "DONT_SHOW_REGION_PROMPT";
            private static final String REGION_PROMPT_SHOWN_AFTER_V81 = "REGION_PROMPT_SHOWN_AFTER_V81";
            public static void setRegionPromptShownAfterV81(boolean regionPromptShown, Context context) {
                writeBooleanToAppSharedPrefsAsync(REGION_PROMPT_SHOWN_AFTER_V81,regionPromptShown,context);
            }
            public static boolean getRegionPromptShownAfterV81(Context context) {
                return readBooleanFromAppSharedPrefs(REGION_PROMPT_SHOWN_AFTER_V81,context);
            }

            public static void setDoNotShowRegionPrompt(boolean showRegionPrompt, Context context) {
                writeBooleanToAppSharedPrefsAsync(DO_NOT_SHOW_REGION_PROMPT,showRegionPrompt,context);
            }
            public static boolean getDoNotShowRegionPrompt(Context context) {
                return readBooleanFromAppSharedPrefs(DO_NOT_SHOW_REGION_PROMPT,context);
            }
        }
    }


    public static class Settings {
        // See top of this class before changing this string
        private static final String SETTINGS = "Settings-";

        public static class OpenBikeSensor {
            private static final String OBS_ENABLED = SETTINGS + "OBS_ENABLED";

            public static boolean isEnabled(Context context) {
                return readBooleanFromAppSharedPrefs(OBS_ENABLED, context);
            }

            public static void setEnabled(Boolean enabled, Context context) {
                writeBooleanToAppSharedPrefsAsync(OBS_ENABLED, enabled, context);
            }
        }

        public static class DisplayUnit {
            // See top of this class before changing this string
            public static final String DISPLAY_UNIT = SETTINGS + "Unit";

            public static void setDisplayUnit(UnitHelper.DISTANCE unit, Context context) {
                writeStringToAppSharedPrefsAsync(DISPLAY_UNIT,unit.getName(), context);
            }

            public static UnitHelper.DISTANCE getDisplayUnit(Context context) {
                return UnitHelper.DISTANCE.parseFromString(readStringFromAppSharedPrefs(DISPLAY_UNIT,UnitHelper.DISTANCE.METRIC.getName(), context));
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

        public static class IncidentsButtonsDuringRide {
            // See top of this class before changing this string
            public static final String INCIDENT_BUTTONS_ENABLED = SETTINGS + "Incident-Buttons";

            public static void setIncidentButtonsEnabled(boolean enabled, Context context) {
                writeBooleanToAppSharedPrefsAsync(INCIDENT_BUTTONS_ENABLED, enabled, context);
            }

            public static boolean getIncidentButtonsEnabled(Context context) {
                return readBooleanFromAppSharedPrefs(INCIDENT_BUTTONS_ENABLED, context);
            }
        }

        public static class RegionSetting {
            // See top of this class before changing this string
            public static final String REGION_SETTING = SETTINGS + "Region-Setting";

            public static void setRegionDetectionViaGPSEnabled(boolean enabled, Context context) {
                writeBooleanToAppSharedPrefsAsync(REGION_SETTING, enabled, context);
            }

            public static boolean getRegionDetectionViaGPSEnabled(Context context) {
                return readBooleanFromAppSharedPrefs(REGION_SETTING, context);
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
                    return readLongFromAppSharedPrefs(PRIVACY_DURATION, 30, context);
                }

                public static void setDuration(long duration, Context context) {
                    writeLongToAppSharedPrefsAsync(PRIVACY_DURATION, duration, context);
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

        }
    }
}
