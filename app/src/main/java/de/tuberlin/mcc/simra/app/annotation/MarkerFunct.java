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
import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.util.Utils;

/**
 * Convenience functions for working with the Map and setting or deleting markers
 */
public class MarkerFunct {

    private static final String TAG = "MarkerFunct_LOG";
    private final String userAgent = "SimRa/alpha";
    private ShowRouteActivity activity;
    private ExecutorService pool;
    private GeocoderNominatim geocoderNominatim;
    private int state;
    private Map<Integer, Marker> markerMap = new HashMap<>();
    private IncidentLog incidentLog;
    private DataLog gpsDataLog;

    public MarkerFunct(ShowRouteActivity activity, DataLog gpsDataLog, IncidentLog incidentLog) {
        this.activity = activity;
        this.gpsDataLog = gpsDataLog;
        this.pool = activity.pool;
        this.incidentLog = incidentLog;

        pool.execute(new SimpleThreadFactory().newThread(() -> {
                    geocoderNominatim = new GeocoderNominatim(userAgent);
                    geocoderNominatim.setService("https://nominatim.openstreetmap.org/");
                }
        ));

        this.state = activity.state;
    }

    public void updateMarkers(IncidentLog incidentLog) {
        for (Map.Entry<Integer, IncidentLogEntry> entry : incidentLog.getIncidents().entrySet()) {
            IncidentLogEntry incident = entry.getValue();
            setMarker(incident);
        }
    }

    /**
     * Because custom markers should only be placed on the actual route, after the
     * user taps onto the map we're determining the GeoPoint on the route that
     * is clostest to the location the user has actually tapped.
     * => this is done via the GeoPointWrapper class.
     *
     * @param geoPoint
     * @param gpsDataLog
     * @return
     */
    public DataLogEntry getClosesDataLogEntryToGeoPoint(GeoPoint geoPoint, DataLog gpsDataLog) {
        List<GeoPointWrapper> wrappedGPS = new ArrayList<>();
        List<GeoPoint> gpsDataLogGeoPoints = gpsDataLog.rideAnalysisData.route.getPoints();
        for (int i = 0; i < gpsDataLogGeoPoints.size(); i++) {
            wrappedGPS.add(new GeoPointWrapper(gpsDataLogGeoPoints.get(i), geoPoint, gpsDataLog.onlyGPSDataLogEntries.get(i)));
        }
        Collections.sort(wrappedGPS, (GeoPointWrapper o1, GeoPointWrapper o2) -> {
            if (o1.distToReference < o2.distToReference) return -1;
            if (o1.distToReference > o2.distToReference) return 1;
            else return 0;
        });
        return wrappedGPS.get(0).dataLogEntry;
    }


    public void addCustomMarker(GeoPoint geoPoint) {
        DataLogEntry closestDataLogEntry = getClosesDataLogEntryToGeoPoint(geoPoint, gpsDataLog);
        // set Marker for new AccEvent, refresh map
        IncidentLogEntry newIncidentLogEnty = incidentLog.updateOrAddIncident(IncidentLogEntry.newBuilder().withBaseInformation(closestDataLogEntry.timestamp, closestDataLogEntry.latitude, closestDataLogEntry.longitude).withIncidentType(IncidentLogEntry.INCIDENT_TYPE.NOTHING).build());
        setMarker(newIncidentLogEnty);
        activity.getmMapView().invalidate();

        // Now we display a dialog box to allow the user to decide if she/he is happy
        // with the location of the custom marker.

        AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle(activity.getResources().getString(R.string.customIncidentAddedTitle));
        alertDialog.setMessage(activity.getResources().getString(R.string.customIncidentAddedMessage));
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        // NEGATIVE BUTTON: marker wasn't placed in the right location, remove from
        // map & markerMap.
        // Removal from ride.events and file not necessary as the new event hasn't been
        // added to those structures yet.
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, activity.getResources().getString(R.string.no),
                (DialogInterface dialog, int which) -> {
                    Marker custMarker = markerMap.get(newIncidentLogEnty.key);
                    activity.getmMapView().getOverlays().remove(custMarker);
                    //mother.getmMapView().getOverlayManager().remove(custMarker);
                    activity.getmMapView().invalidate();
                    markerMap.remove(custMarker);
                    incidentLog.removeIncident(newIncidentLogEnty);
                });

        // POSITIVE BUTTON: user approves of button. Add to ride.events & file.
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, activity.getResources().getString(R.string.yes),
                (DialogInterface dialog, int which) -> {
                });

        Window window = alertDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.BOTTOM;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        alertDialog.show();
    }

    public void setMarker(IncidentLogEntry incidentLogEntry) {
        Marker incidentMarker = new Marker(activity.getmMapView());
        Marker previousMarker = markerMap.get(incidentLogEntry.key);
        if (previousMarker != null) {
            activity.getmMapView().getOverlays().remove(previousMarker);
        }
        // Add the marker + corresponding key to map so we can manage markers if
        // necessary (e.g., remove them)
        markerMap.put(incidentLogEntry.key, incidentMarker);
        GeoPoint currentLocHelper = new GeoPoint(incidentLogEntry.latitude, incidentLogEntry.longitude);
        incidentMarker.setPosition(currentLocHelper);
        /* Different marker icons for ....
         * A) annotated y/n
         * B) default/custom
         */

        if (Utils.checkForAnnotation(incidentLogEntry)) {
            // custom events can be detected via their timeStamp
            if (!(incidentLogEntry.timestamp == 1337)) {
                incidentMarker.setIcon(activity.markerDefault);
            } else {
                incidentMarker.setIcon(activity.markerNotYetAnnotated);
            }
        } else {
            // custom events can be detected via their timeStamp
            if (!(incidentLogEntry.timestamp == 1337)) {
                incidentMarker.setIcon(activity.markerAutoGenerated);
            } else {
                incidentMarker.setIcon(activity.editDoneCust);
            }
        }

        String addressForLoc = "";

        try {
            addressForLoc = pool.submit(() -> getAddressFromLocation(currentLocHelper)).get(2, TimeUnit.SECONDS);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble,
                activity.getmMapView(),
                addressForLoc, activity, state, incidentLogEntry);
        incidentMarker.setInfoWindow(infoWindow);

        activity.getmMapView().getOverlays().add(incidentMarker);
        activity.getmMapView().invalidate();
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

    public void deleteAllMarkers() {
        for (Map.Entry<Integer, Marker> markerEntry : markerMap.entrySet()) {
            activity.getmMapView().getOverlays().remove(markerEntry.getValue());
        }
        activity.getmMapView().invalidate();
    }

    class SimpleThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread myThread = new Thread(r);
            myThread.setPriority(Thread.MIN_PRIORITY);
            return myThread;
        }
    }

}