package de.tuberlin.mcc.simra.app.entities;

/**
 * A Ride contains both the Log Entries, as well as the incidents and settings associated with it.
 */
public class Ride {
    public int rideID;
    public int bike;
    public int child;
    public int trailer;
    public int pLoc;

    public Ride(int rideID, int bike, int child, int trailer, int pLoc) {
        this.rideID = rideID;
        this.bike = bike;
        this.child = child;
        this.trailer = trailer;
        this.pLoc = pLoc;
    }

    public static void saveRide() {

    }
}
