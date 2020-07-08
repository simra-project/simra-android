package de.tuberlin.mcc.simra.app.annotation;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Address;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.activities.ShowRouteActivity;
import de.tuberlin.mcc.simra.app.entities.AccEvent;
import de.tuberlin.mcc.simra.app.util.IOUtils;
import de.tuberlin.mcc.simra.app.util.SharedPref;

import static de.tuberlin.mcc.simra.app.util.Constants.ACCEVENTS_HEADER;
import static de.tuberlin.mcc.simra.app.util.Utils.appendToFile;
import static de.tuberlin.mcc.simra.app.util.Utils.fileExists;
import static de.tuberlin.mcc.simra.app.util.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.util.Utils.overWriteFile;

public class MarkerFunct {

    private static final String TAG = "MarkerFunct_LOG";
    private final String userAgent = "SimRa/alpha";
    private ShowRouteActivity mother;
    private ExecutorService pool;
    private Integer rideID;
    private ArrayList<Marker> markers = new ArrayList<>();
    private GeocoderNominatim geocoderNominatim;
    private boolean temp;
    private int state;
    private int numEvents;
    private Map<Integer, Marker> markerMap = new HashMap<>();

    public MarkerFunct(ShowRouteActivity mother, boolean temp) {

        this.mother = mother;

        this.temp = temp;

        this.pool = mother.pool;

        if (temp) {
            this.rideID = mother.tempLegacyRide.getKey();
        } else {
            this.rideID = mother.legacyRide.getKey();
        }
        pool.execute(new SimpleThreadFactory().newThread(() -> {
                    geocoderNominatim = new GeocoderNominatim(userAgent);
                    geocoderNominatim.setService("https://nominatim.openstreetmap.org/");
                }
        ));

        this.state = mother.state;
        if (temp) {
            this.numEvents = (mother.tempLegacyRide.events.size() - 1);
        } else {
            this.numEvents = (mother.legacyRide.events.size() - 1);
        }

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Event determination and representation happens here

    public void showIncidents() {

        if (temp) {
            for (int i = 0; i < mother.tempLegacyRide.events.size(); i++) {
                // Log.d(TAG,"showIncidents() mother.tempRide.events.get(i).key: " + mother.tempRide.events.get(i).key);
                setMarker(mother.tempLegacyRide.events.get(i), mother.tempLegacyRide.events.get(i).key);
            }
        } else {
            for (int i = 0; i < mother.legacyRide.events.size(); i++) {
                // Log.d(TAG,"showIncidents() mother.ride.events.get(i).key: " + mother.ride.events.get(i).key);
                setMarker(mother.legacyRide.events.get(i), mother.legacyRide.events.get(i).key);
            }
        }
    }

    public void addCustMarker(GeoPoint p) {

        // Because custom markers should only be placed on the actual route, after the
        // user taps onto the map we're determining the GeoPoint on the route that
        // is clostest to the location the user has actually tapped.
        // => this is done via the GeoPointWrapper class.

        GeoPoint closestOnRoute;

        List<GeoPointWrapper> wrappedGPS = new ArrayList<>();

        if (temp) {
            for (GeoPoint thisGP : mother.tempLegacyRide.getRoute().getPoints()) {
                wrappedGPS.add(new GeoPointWrapper(thisGP, p));
            }
        } else {
            for (GeoPoint thisGP : mother.legacyRide.getRoute().getPoints()) {
                wrappedGPS.add(new GeoPointWrapper(thisGP, p));
            }
        }


        Log.d(TAG, "wrappedGPS.size(): " + wrappedGPS.size());

        Collections.sort(wrappedGPS, (GeoPointWrapper o1, GeoPointWrapper o2) -> {

            if (o1.distToReference < o2.distToReference) return -1;

            if (o1.distToReference > o2.distToReference) return 1;

            else return 0;

        });

        closestOnRoute = wrappedGPS.get(0).wrappedGeoPoint;

        Log.d(TAG, "closestOnRoute: " + closestOnRoute.toString());

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Create a new AccEvent
        int eventCount = ++this.numEvents;
        AccEvent newAcc = new AccEvent(eventCount, closestOnRoute.getLatitude(),
                closestOnRoute.getLongitude(), 1337, false, "", "0");
        Log.d(TAG, "newAcc: " + newAcc.toString());

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // set Marker for new AccEvent, refresh map
        setMarker(newAcc, eventCount);
        mother.getmMapView().invalidate();

        // Now we display a dialog box to allow the user to decide if she/he is happy
        // with the location of the custom marker.
        approveCustMarker(newAcc);

    }

    public void approveCustMarker(AccEvent newAcc) {

        AlertDialog alertDialog = new AlertDialog.Builder(mother).create();
        alertDialog.setTitle(mother.getResources().getString(R.string.customIncidentAddedTitle));
        alertDialog.setMessage(mother.getResources().getString(R.string.customIncidentAddedMessage));
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        // NEGATIVE BUTTON: marker wasn't placed in the right location, remove from
        // map & markerMap.
        // Removal from ride.events and file not necessary as the new event hasn't been
        // added to those structures yet.
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, mother.getResources().getString(R.string.no),
                (DialogInterface dialog, int which) -> {
                    Marker custMarker = markerMap.get(this.numEvents);
                    mother.getmMapView().getOverlays().remove(custMarker);
                    //mother.getmMapView().getOverlayManager().remove(custMarker);
                    mother.getmMapView().invalidate();
                    markerMap.remove(custMarker);
                    this.numEvents--;
                });

        // POSITIVE BUTTON: user approves of button. Add to ride.events & file.
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, mother.getResources().getString(R.string.yes),
                (DialogInterface dialog, int which) -> {

                    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    // Append new acc event to accEvents[rideID].csv
                    String pathToAccEventsOfRide = IOUtils.Files.getEventsFileName(rideID, temp);

                    String fileInfoLine = getAppVersionNumber(mother) + "#1" + System.lineSeparator();

                    int bike = SharedPref.Settings.Ride.BikeType.getBikeType(mother);
                    int child = SharedPref.Settings.Ride.ChildOnBoard.getValue(mother);
                    int trailer = SharedPref.Settings.Ride.BikeWithTrailer.getValue(mother);
                    int pLoc = SharedPref.Settings.Ride.PhoneLocation.getPhoneLocation(mother);

                    // eventline = key,lat,lon,ts,bike,childCheckBox,trailerCheckBox,pLoc,,,,,,,,,,,,
                    String eventLine = newAcc.key + ","
                            + newAcc.position.getLatitude() + "," + newAcc.position.getLongitude()
                            + "," + newAcc.timeStamp + "," + bike + "," + child + "," + trailer + "," + pLoc + "," + /*incident*/"," + /*i1*/"," + /*i2*/"," + /*i3*/"," + /*i4*/"," + /*i5*/"," + /*i6*/"," + /*i7*/"," + /*i8*/"," + /*i9*/"," + /*scary*/"," + /*desc*/"," + /*i10*/"," + System.lineSeparator();

                    if (!fileExists(pathToAccEventsOfRide, mother.getApplicationContext())) {
                        overWriteFile((fileInfoLine + ACCEVENTS_HEADER + eventLine), pathToAccEventsOfRide, mother.getApplicationContext());
                    } else {
                        appendToFile(eventLine, pathToAccEventsOfRide, mother.getApplicationContext());
                    }
                    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    // Add new AccEvent to ride's AccEvents list
                    if (temp) {
                        mother.tempLegacyRide.getEvents().add(newAcc);
                    } else {
                        mother.legacyRide.getEvents().add(newAcc);
                    }
                });

        Window window = alertDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.BOTTOM;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        alertDialog.show();
    }

    public void setMarker(AccEvent event, int accEventKey) {
        Marker incidentMarker = new Marker(mother.getmMapView());

        // Add the marker + corresponding key to map so we can manage markers if
        // necessary (e.g., remove them)
        markerMap.put(accEventKey, incidentMarker);
        GeoPoint currentLocHelper = event.position;
        incidentMarker.setPosition(currentLocHelper);
        /* Different marker icons for ....
         * A) annotated y/n
         * B) default/custom
         */

        if (!event.annotated) {
            // custom events can be detected via their timeStamp
            if (!(event.timeStamp == 1337)) {
                incidentMarker.setIcon(mother.editMarkerDefault);
            } else {
                incidentMarker.setIcon(mother.editCustMarker);
            }
        } else {
            // custom events can be detected via their timeStamp
            if (!(event.timeStamp == 1337)) {
                incidentMarker.setIcon(mother.editDoneDefault);
            } else {
                incidentMarker.setIcon(mother.editDoneCust);
            }
        }

        String addressForLoc = "";

        try {
            addressForLoc = pool.submit(() -> getAddressFromLocation(currentLocHelper)).get(2, TimeUnit.SECONDS);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble,
                mother.getmMapView(),
                event, addressForLoc, mother, event.key, temp, state);
        incidentMarker.setInfoWindow(infoWindow);

        markers.add(incidentMarker);
        mother.getmMapView().getOverlays().add(incidentMarker);
        mother.getmMapView().invalidate();
    }

    // Generate a new GeoPoint from address String via Geocoding

    public String getAddressFromLocation(GeoPoint incidentLoc) {
        List<Address> address;
        String addressForLocation = "";
        try {
            // This is the actual geocoding
            address = geocoderNominatim.getFromLocation(incidentLoc.getLatitude(),
                    incidentLoc.getLongitude(), 1);
            if (address.size() == 0) {
                Log.d(TAG, "getAddressFromLocation(): Couldn't find an address for input geoPoint");
            } else {
                // Get address result from geocoding result
                Address location = address.get(0);
                addressForLocation = location.getAddressLine(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return addressForLocation;

    }

    // Closes all InfoWindows.
    public void closeAllInfoWindows() {
        for (int i = 0; i < markers.size(); i++) {
            markers.get(i).closeInfoWindow();
        }
    }

    public void deleteAllMarkers() {
        for (int i = 0; i < markers.size(); i++) {
            mother.getmMapView().getOverlays().remove(markers.get(i));

        }
        mother.getmMapView().invalidate();
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