package de.tuberlin.mcc.simra.app.annotation;

import org.osmdroid.util.GeoPoint;

import de.tuberlin.mcc.simra.app.entities.DataLogEntry;

// This class wraps two GeoPoints and their distance (difference between their summed
// lats & lons).
// => PURPOSE: ensure that when users add incidents manually, they cannot include locations
//             which where not a part of the route in question.
//             So every manually added location gets replaced by the closest location included
//             in the route.

public class GeoPointWrapper {

    public GeoPoint wrappedGeoPoint;

    public GeoPoint referencePoint;

    public double distToReference;
    public DataLogEntry dataLogEntry;

    public GeoPointWrapper(GeoPoint wrappedGeoPoint, GeoPoint referenceGeoPoint, DataLogEntry dataLogEntry) {
        this.wrappedGeoPoint = wrappedGeoPoint;
        this.referencePoint = referenceGeoPoint;
        this.dataLogEntry = dataLogEntry;
        this.distToReference = calcDistToReference();
    }

    public double calcDistToReference() {

        long earthRadiusKm = 6371;

        double dLat = Math.toRadians(referencePoint.getLatitude()
                - wrappedGeoPoint.getLatitude());

        double dLon = Math.toRadians(referencePoint.getLongitude()
                - wrappedGeoPoint.getLongitude());

        double lat1 = Math.toRadians(wrappedGeoPoint.getLatitude());
        double lat2 = Math.toRadians(referencePoint.getLatitude());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;

    }

}
