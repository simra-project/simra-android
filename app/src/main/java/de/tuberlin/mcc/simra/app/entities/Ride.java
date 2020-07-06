package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;

/**
 * A Ride contains both the Log Entries, as well as the incidents and settings associated with it.
 */
public class Ride {
    public int rideID;
    public DataLog dataLog;
    public IncidentLog incidentLog;


    public Ride(int rideID, DataLog dataLog, IncidentLog incidentLog) {
        this.rideID = rideID;
        this.dataLog = dataLog;
        this.incidentLog = incidentLog;
    }

    public static Ride loadRideById(int rideID, Context context) {
        return new Ride(rideID, new DataLog(rideID, context), new IncidentLog(rideID, context));
    }

    public static void saveRide() {

    }
}
