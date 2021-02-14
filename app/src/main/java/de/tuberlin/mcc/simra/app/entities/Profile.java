package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class Profile {

    public int ageGroup;
    public int gender;
    public int region;
    public int numberOfRides;
    public int experience;
    public long duration;
    public int numberOfIncidents;
    public long waitedTime;
    public long distance;
    public long co2;
    public List<Float> timeDistribution;
    public int numberOfScaryIncidents;
    public int behaviour;

    public Profile(int ageGroup, int gender, int region, int numberOfRides, int experience, long duration, int numberOfIncidents, long waitedTime, long distance, long co2, List<Float> timeDistribution, int numberOfScaryIncidents, int behaviour) {
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.region = region;
        this.numberOfRides = numberOfRides;
        this.experience = experience;
        this.duration = duration;
        this.numberOfIncidents = numberOfIncidents;
        this.waitedTime = waitedTime;
        this.distance = distance;
        this.co2 = co2;
        this.timeDistribution = timeDistribution;
        this.numberOfScaryIncidents = numberOfScaryIncidents;
        this.behaviour = behaviour;
    }

    private static SharedPreferences getSharedPreferences(Integer regionId, Context context) {
        if (regionId == null) {
            return context.getApplicationContext()
                    .getSharedPreferences("Profile", Context.MODE_PRIVATE);
        } else {
            return context.getApplicationContext()
                    .getSharedPreferences("Profile_" + regionId, Context.MODE_PRIVATE);
        }
    }

    /**
     * @param regionId Global Profile if null
     * @param context
     * @return
     */
    public static Profile loadProfile(Integer regionId, Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(regionId, context);
        ArrayList<Float> timeDistribution = new ArrayList<>();
        for (int i = 0; i <= 23; i++) {
            timeDistribution.add(sharedPreferences.getFloat(i + "", 0f));
        }
        return new Profile(
                sharedPreferences.getInt("Birth", 0),
                sharedPreferences.getInt("Gender", 0),
                sharedPreferences.getInt("Region", 0),
                sharedPreferences.getInt("NumberOfRides", 0),
                sharedPreferences.getInt("Experience", 0),
                sharedPreferences.getLong("Duration", 0),
                sharedPreferences.getInt("NumberOfIncidents", 0),
                sharedPreferences.getLong("WaitedTime", 0),
                sharedPreferences.getLong("Distance", 0),
                sharedPreferences.getLong("Co2", 0),
                timeDistribution,
                sharedPreferences.getInt("NumberOfScary", 0),
                sharedPreferences.getInt("Behaviour", -1)
        );
    }

    /**
     * @param profile
     * @param regionId Global Profile if null
     * @param context
     */
    public static void saveProfile(Profile profile, Integer regionId, Context context) {
        SharedPreferences.Editor editSharedPreferences = getSharedPreferences(regionId, context).edit();
        if (profile.ageGroup > -1) {
            editSharedPreferences.putInt("Birth", profile.ageGroup);
        }
        if (profile.gender > -1) {
            editSharedPreferences.putInt("Gender", profile.gender);
        }
        if (profile.region > -1) {
            editSharedPreferences.putInt("Region", profile.region);
        }
        if (profile.numberOfRides > -1) {
            editSharedPreferences.putInt("NumberOfRides", profile.numberOfRides);
        }
        if (profile.experience > -1) {
            editSharedPreferences.putInt("Experience", profile.experience);
        }
        if (profile.duration > -1) {
            editSharedPreferences.putLong("Duration", profile.duration);
        }
        if (profile.numberOfIncidents > -1) {
            editSharedPreferences.putInt("NumberOfIncidents", profile.numberOfIncidents);
        }
        if (profile.waitedTime > -1) {
            editSharedPreferences.putLong("WaitedTime", profile.waitedTime);
        }
        if (profile.distance > -1) {
            editSharedPreferences.putLong("Distance", profile.distance);
        }
        if (profile.co2 > -1) {
            editSharedPreferences.putLong("Co2", profile.co2);
        }
        for (int i = 0; i < profile.timeDistribution.toArray().length; i++) {
            editSharedPreferences.putFloat(i + "", (Float) profile.timeDistribution.get(i).floatValue());
        }
        if (profile.numberOfScaryIncidents > -1) {
            editSharedPreferences.putInt("NumberOfScary", profile.numberOfScaryIncidents);
        }
        if (profile.behaviour > -2) {
            editSharedPreferences.putInt("Behaviour", profile.behaviour);
        }
        editSharedPreferences.apply();
    }

    /**
     * returns true if region is UNKNOWN (0) or other (3)
     * @param context
     * @return true if region is UNKNOWN (0) or other (3)
     */
    public static boolean profileIsInUnknownRegion(Context context) {
        int region = loadProfile(null, context).region;
        return region == 0 || region == 3;
    }
}
