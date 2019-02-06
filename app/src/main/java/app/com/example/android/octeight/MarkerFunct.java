package app.com.example.android.octeight;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.text.format.DateUtils;
import android.util.Log;

import org.mapsforge.map.android.view.MapView;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class MarkerFunct {

    ShowRouteActivity mother;

    ExecutorService pool;

    List<AccEvent> incidentDat;

    GeocoderNominatim geocoderNominatim;

    final String userAgent = "SimRa/alpha";

    String startTime = "";
    String timeStamp = "";

    boolean custom = false;

    public MarkerFunct(ShowRouteActivity mother) {

        this.mother = mother;

        this.pool = mother.pool;

        this.incidentDat = mother.ride.getEvents();

        pool.execute(new SimpleThreadFactory().newThread(() ->

                geocoderNominatim = new GeocoderNominatim(userAgent)

        ));

        this.startTime = mother.startTime;

        this.timeStamp = mother.timeStamp;

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Event determination and representation happens here

    public void showIncidents() {

        for(AccEvent accEvent : mother.ride.getEvents()) {

            setMarker(accEvent);

        }

    }

    public void addCustMarker(GeoPoint p) {

        custom = true;

        GeoPoint closestOnRoute;

        List<GeoPointWrapper> wrappedGPS = new ArrayList<>();

        for(GeoPoint thisGP : mother.ride.getRoute().getPoints()) {

            wrappedGPS.add(new GeoPointWrapper(thisGP, p));

        }

        Log.i("WRAPPED_GP_LIST", String.valueOf(wrappedGPS.size()));

        Collections.sort(wrappedGPS, new Comparator<GeoPointWrapper>() {
            @Override
            public int compare(GeoPointWrapper o1, GeoPointWrapper o2) {

                if(o1.distToReference < o2.distToReference) return -1;

                if(o1.distToReference > o2.distToReference) return 1;

                else return 0;

            }
        });

        // Collections.sort(wrappedGPS);

        closestOnRoute = wrappedGPS.get(0).wrappedGeoPoint;

        Log.i("WRAPPED_GP_LIST", closestOnRoute.toString());

        String[] eventLine = new String[6];
        eventLine[0] = String.valueOf(closestOnRoute.getLatitude());
        eventLine[1] = String.valueOf(closestOnRoute.getLongitude());
        eventLine[5] = "0";

        AccEvent newAcc = new AccEvent(eventLine);

        Log.i("NEW_ACC", newAcc.toString());

        mother.ride.getEvents().add(newAcc);

        setMarker(new AccEvent(eventLine));

        mother.getmMapView().invalidate();

        custom = false;

        //showIncidents();

    }

    public void setMarker(AccEvent event) {

        Marker incidentMarker = new Marker(mother.getmMapView());

        GeoPoint currentLocHelper = event.position;

        incidentMarker.setPosition(currentLocHelper);

        // Different marker icons for automatically detected & custom incidents, for testing/
        // demonstration purposes

        if (! custom) {

            incidentMarker.setIcon(mother.markerDefault);

        } else {

            incidentMarker.setIcon(mother.custMarker);
        }

        String addressForLoc = "";

        try {
            addressForLoc = pool.submit(() -> getAddressFromLocation(currentLocHelper)).get();
        } catch (Exception ex) {
            ex.printStackTrace();

        }

        long millis = Long.valueOf(startTime);
        String time = DateUtils.formatDateTime(mother, millis, DateUtils.FORMAT_SHOW_TIME);

        InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble,
                mother.getmMapView(),
                event, addressForLoc, mother, mother.ride);
        incidentMarker.setInfoWindow(infoWindow);

        //incidentMarker.setSnippet("Vorfall " + i);

        mother.getmMapView().getOverlays().add(incidentMarker);
        mother.getmMapView().invalidate();

    }

    // Generate a new GeoPoint from address String via Geocoding

    public String getAddressFromLocation(GeoPoint incidentLoc) {

        List<Address> address = new ArrayList<>();

        String addressForLocation = "";

        try {

            // This is the actual geocoding

            address = geocoderNominatim.getFromLocation(incidentLoc.getLatitude(),
                    incidentLoc.getLongitude(),1);

            if(address.size() == 0) {

                Log.i("getFromLoc", "Couldn't find an address for input geoPoint");

            } else {

                // Log.i("getFromLoc", address.get(0).toString());

                // Get address result from geocoding result

                Address location = address.get(0);

                addressForLocation = location.getAddressLine(0);

                // Generate GeoPoint address result

                /**Log.i("StartStop", "Latitude: " +
                 destCoords.getLatitude() + ", Longitude: " + destCoords.getLongitude());
                 myLoc = location.toString();*/

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return addressForLocation;

    }

    // Thread factory implementation: to enable setting priority before new thread is returned

    class SimpleThreadFactory implements ThreadFactory {

        public Thread newThread(Runnable r) {

            Thread myThread = new Thread(r);

            myThread.setPriority(Thread.MIN_PRIORITY);

            return myThread;

        }

    }

}