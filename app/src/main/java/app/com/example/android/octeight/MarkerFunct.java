package app.com.example.android.octeight;


import android.location.Address;
import android.text.format.DateUtils;
import android.util.Log;

import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import static app.com.example.android.octeight.Utils.appendToFile;
import static app.com.example.android.octeight.Utils.checkForAnnotation;
import static app.com.example.android.octeight.Utils.fileExists;

public class MarkerFunct {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "MarkerFunct_LOG";

    private ShowRouteActivity mother;

    private ExecutorService pool;

    //private List<AccEvent> incidentDat;

    private String rideID;

    private ArrayList<Marker> markers = new ArrayList<>();

    private GeocoderNominatim geocoderNominatim;

    private final String userAgent = "SimRa/alpha";

    String startTime;
    String timeStamp;

    private int numEvents;

    public MarkerFunct(ShowRouteActivity mother) {

        this.mother = mother;

        this.pool = mother.pool;

        //this.incidentDat = mother.ride.getEvents();

        this.rideID = mother.ride.getId();

        pool.execute(new SimpleThreadFactory().newThread(() ->
                geocoderNominatim = new GeocoderNominatim(userAgent)
        ));

        this.startTime = mother.startTime;

        this.timeStamp = mother.timeStamp;

        this.numEvents = (mother.ride.events.size() - 1);

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Event determination and representation happens here

    public void showIncidents() {

        /**for(AccEvent accEvent : incidentDat) {
            Log.d(TAG, "setting Marker at: " + accEvent.position.toString());

            setMarker(accEvent);
        }*/

        try (BufferedReader reader = new BufferedReader(new FileReader
                (mother.getApplicationContext()
                        .getFileStreamPath("accEvents" + rideID + ".csv")))) {

        reader.readLine();

            String line;

            while ((line = reader.readLine()) != null) {

                String[] actualIncident = line.split(",", -1);

                /**String[] eventLine = new String[6];
                //Log.d(TAG, "actualIncident: " + Arrays.toString(actualIncident)
                //        + " id: " + mother.ride.getId());
                //if(actualIncident[0].equals(mother.ride.getId())){
                //    Log.d(TAG, "custom incident found! actualIncident: " + Arrays.toString(actualIncident));
                    eventLine[0] = actualIncident[1];
                    eventLine[1] = actualIncident[2];
                    eventLine[5] = actualIncident[3];
                    AccEvent accEvent = new AccEvent(eventLine);
                 */

                Log.d(TAG, "actualIncident: " + Arrays.toString(actualIncident));

                boolean annotated = checkForAnnotation(actualIncident);

                Log.d(TAG, "annotated:" + annotated);

                AccEvent accEvent = new AccEvent(Integer.parseInt(actualIncident[0]),
                        Double.parseDouble(actualIncident[1]),
                        Double.parseDouble(actualIncident[2]),
                        Long.parseLong(actualIncident[3]),annotated);

                Log.d(TAG, "accEvent key: " + accEvent.key + " accEvent.position" + accEvent.position.toString());
                    setMarker(accEvent);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    public void addCustMarker(GeoPoint p) {

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

        /**String[] eventLine = new String[6];
        eventLine[0] = String.valueOf(closestOnRoute.getLatitude());
        eventLine[1] = String.valueOf(closestOnRoute.getLongitude());
        eventLine[5] = "1337";

        AccEvent newAcc = new AccEvent(eventLine);*/

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Create a new AccEvent

        int eventCount = this.numEvents + 1;

        AccEvent newAcc = new AccEvent(eventCount, closestOnRoute.getLatitude(),
                closestOnRoute.getLongitude(), 1337, false);

        Log.i("NEW_ACC", newAcc.toString());

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Append new acc event to accEvents[rideID].csv

        String pathToAccEventsOfRide = "accEvents" + rideID + ".csv";
        String header = "key,lat,lon,ts,incidentType,phoneLocation,description";
        header += System.lineSeparator();
        String eventLine = eventCount + ","
                + newAcc.position.getLatitude() + "," + newAcc.position.getLongitude()
                + "," + newAcc.timeStamp + ",,," + System.lineSeparator();

        if(!fileExists(pathToAccEventsOfRide, mother.getApplicationContext())){

            appendToFile((header + eventLine), pathToAccEventsOfRide, mother.getApplicationContext());

            } else {

            appendToFile(eventLine, pathToAccEventsOfRide, mother.getApplicationContext());

        }

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Add new AccEvent to ride's AccEvents list

        mother.ride.getEvents().add(newAcc);

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // set Marker for new AccEvent, refresh map

        setMarker(newAcc);

        mother.getmMapView().invalidate();

    }

    public void setMarker(AccEvent event) {

        Marker incidentMarker = new Marker(mother.getmMapView());

        GeoPoint currentLocHelper = event.position;

        incidentMarker.setPosition(currentLocHelper);

        /** Different marker icons for ....
         * A) annotated y/n
         * B) default/custom
         */

        if (! event.annotated) {

            // custom events can be detected via their timeStamp

            if (! (event.timeStamp == 1337)) {

                incidentMarker.setIcon(mother.editMarkerDefault);

            } else {

                incidentMarker.setIcon(mother.editCustMarker);
            }

        } else {

            // custom events can be detected via their timeStamp

            if (! (event.timeStamp == 1337)) {

                incidentMarker.setIcon(mother.editDoneDefault);

            } else {

                incidentMarker.setIcon(mother.editDoneCust);

            }

        }

        String addressForLoc = "";

        Log.d(TAG, "Getting AddressFromLocation; currentLocHelper: " + currentLocHelper.toString());
        try {
            addressForLoc = pool.submit(() -> getAddressFromLocation(currentLocHelper)).get();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        long millis = Long.valueOf(startTime);
        String time = DateUtils.formatDateTime(mother, millis, DateUtils.FORMAT_SHOW_TIME);

        Log.d(TAG, "setting up InfoWindow with address: " + addressForLoc);
        InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble,
                mother.getmMapView(),
                event, addressForLoc, mother, mother.ride.getId(), event.key);
        incidentMarker.setInfoWindow(infoWindow);

        //incidentMarker.setSnippet("Vorfall " + i);

        markers.add(incidentMarker);

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

                Log.d(TAG, "address.get(0): " + address.get(0).toString());

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

        Log.d(TAG, "returning addressForLocation: " + addressForLocation);
        return addressForLocation;

    }

    // Closes all InfoWindows.
    public void closeAllInfoWindows(){
        for (int i = 0; i < markers.size(); i++) {
            markers.get(i).closeInfoWindow();
        }
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