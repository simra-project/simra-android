package de.tuberlin.mcc.simra.app.entities;

/**
 * A Ride contains both the Log Entries, as well as the incidents and settings associated with it.
 */
public class Ride {
    public int rideID;


    private Ride(int rideID) {
        this.rideID = rideID;
    }

    public static Ride loadRideById(int rideID) {
        return new Ride(rideID);
    }

    public static void saveRide() {

    }

    //public int getState() {
//
//    }


}
