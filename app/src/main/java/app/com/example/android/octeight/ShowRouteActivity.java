package app.com.example.android.octeight;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.LocationManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.mapsforge.map.scalebar.DefaultMapScaleBar;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ShowRouteActivity extends AppCompatActivity {

    String startTime = "";

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Map stuff, Overlays
    private MapView mMapView;
    private TextView copyrightTxt;

    ////~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Our ride
    Ride ride;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Log tag
    private static final String TAG = "ShowRouteActivity_LOG";


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // GEOCODER --> obtain GeoPoint from address

    ExecutorService pool;

    GeocoderNominatim geocoderNominatim;

    GeoPoint destCoords = null;

    List<Address> destination;

    String myLoc = null;

    final String userAgent = "SimRa/alpha";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() started");
        setContentView(R.layout.activity_show_route);
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Map configuration
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        mMapView = findViewById(R.id.showRouteMap);
        mMapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMapView.setMultiTouchControls(true); // gesture zooming
        mMapView.setFlingEnabled(true);
        MapController mMapController = (MapController) mMapView.getController();
        copyrightTxt = (TextView) findViewById(R.id.copyright_text);
        copyrightTxt.setMovementMethod(LinkMovementMethod.getInstance());

        // scales tiles to dpi of current display
        mMapView.setTilesScaledToDpi(true);

        String pathToAccGpsFile = getIntent().getStringExtra("PathToAccGpsFile");
        // String date = getIntent().getStringExtra("Date");
        startTime = getIntent().getStringExtra("StartTime");
        // Log.d(TAG, "onCreate() date: " + date);
        int state = getIntent().getIntExtra("State", 0);
        // Log.d(TAG, "onCreate() PathToAccGpsFile:" + pathToAccGpsFile);
        String duration = getIntent().getStringExtra("Duration");

        File gpsFile = getFileStreamPath(pathToAccGpsFile);
        // File gpsFile = getFileStreamPath("59_accGps_23.01.2019 09_19_09.csv");
        // File gpsFile = getFileStreamPath("95_accGps_28.01.2019 10:42:05.csv");

        Log.d(TAG, "creating ride objects");
        try {
            // Create a ride object with the accelerometer, gps and time data
            ride = new Ride(gpsFile, duration, startTime,/*date,*/ state, this);
        } catch (NullPointerException nE){
            nE.printStackTrace();
        }

        // Get the Route as a Polyline to be displayed on the map
        Polyline route = ride.getRoute();
        // Get a bounding box of the route so the view can be moved to it and the zoom can be
        // set accordingly
        BoundingBox bBox = getBoundingBox(route);
        // Disallow the user to scroll outside the bounding box to prevent her/him from getting lost
        mMapView.setScrollableAreaLimitDouble(bBox);

        // Log.d(TAG, "bBox: " + bBox);
        // Log.d(TAG, "getIntrinsicScreenRect: " + mMapView.getIntrinsicScreenRect(null).toString());

        mMapView.getOverlayManager().add(route);

        mMapView.invalidate();
        // zoom automatically to the bounding box. Usually the command in the if body should suffice
        // but osmdroid is buggy and we need the else part to fix it.
        if((mMapView.getIntrinsicScreenRect(null).bottom-mMapView.getIntrinsicScreenRect(null).top) > 0){
            mMapView.zoomToBoundingBox(bBox, false);
        } else {
            ViewTreeObserver vto = mMapView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {
                    mMapView.zoomToBoundingBox(bBox, false);
                    ViewTreeObserver vto2 = mMapView.getViewTreeObserver();
                    vto2.removeOnGlobalLayoutListener(this);
                }
            });
        }

        mMapView.setMinZoomLevel(7.0);
        if(mMapView.getMaxZoomLevel() > 19.0){
            mMapView.setMaxZoomLevel(19.0);
        }

        // Thread pool to avoid NetworkOnMainThreadException when establishing a server
        // connection during creation of GeocoderNominatim

        pool = Executors.newFixedThreadPool(2);

        pool.execute(new SimpleThreadFactory().newThread(() ->

                geocoderNominatim = new GeocoderNominatim(userAgent)

        ));

        showIncidents();
        Log.d(TAG, "onCreate() finished");

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Event determination and representation happens here

    public void showIncidents() {
        Log.d(TAG, "showIncidents()");


        /**for(AccEvent accE : ride.events) {

            Marker marker = new Marker(mMapView);

            ride.incidents.add(marker);

            marker.setPosition(accE.position);

        }*/

        // Create some test data


        List<AccEvent> incidentDat = new ArrayList<>();
        /*
        incidentDat.add(new AccEvent(new GeoPoint(52.517374, 13.338407),
                new Date(), null));

        incidentDat.add(new AccEvent(new GeoPoint(52.517592, 13.324816),
                new Date(), null));

        incidentDat.add(new AccEvent(new GeoPoint(52.515625, 13.320117),
                new Date(), null));

        incidentDat.add(new AccEvent(new GeoPoint(52.507634, 13.320117),
                new Date(), null));
        */
        incidentDat = ride.getEvents();

        Drawable accident = getResources().getDrawable(R.drawable.accident, null);

        Drawable markerDefault = getResources().getDrawable(R.drawable.marker_default, null);

        for(int i = 0; i < incidentDat.size(); i++) {

            Marker incidentMarker = new Marker(mMapView);

            GeoPoint currentLocHelper = incidentDat.get(i).position;

            incidentMarker.setPosition(currentLocHelper);

            incidentMarker.setIcon(markerDefault);

            String addressForLoc = "";

            try {
                addressForLoc = pool.submit(() -> getAddressFromLocation(currentLocHelper)).get();
            } catch(Exception ex) {
                ex.printStackTrace();

            }

            // DateFormat format = android.text.format.DateFormat.getDateFormat(getApplicationContext());
            // Date date = incidentDat.get(i).date;
            // String date = format.format(incidentDat.get(i).date);



            incidentMarker.setTitle("Vorfall " + i);

            long millis = Long.valueOf(startTime);
            String time = DateUtils.formatDateTime(this, millis, DateUtils.FORMAT_SHOW_TIME);

            InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mMapView,
                    incidentDat.get(i), time + " " +addressForLoc, ShowRouteActivity.this);
            incidentMarker.setInfoWindow(infoWindow);

            //incidentMarker.setSnippet("Vorfall " + i);

            mMapView.getOverlays().add(incidentMarker);
            mMapView.invalidate();

        }

        //*****************************************************************
        // Shutdown pool and await termination to make sure the program
        // doesn't continue without the relevant work being completed

        pool.shutdown();

        try {
            pool.awaitTermination(2, TimeUnit.SECONDS);
        } catch(InterruptedException ie) {
            ie.printStackTrace();
        }

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

                Log.d(TAG, Arrays.deepToString(address.toArray()));
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

    // Returns the longitudes of the southern- and northernmost points
    // as well as the latitudes of the western- and easternmost points
    // in a double Array {South, North, West, East}
    static BoundingBox getBoundingBox(Polyline pl){

        // {North, East, South, West}
        ArrayList<GeoPoint> geoPoints = pl.getPoints();

        double[] border = {geoPoints.get(0).getLatitude(), geoPoints.get(0).getLongitude(), geoPoints.get(0).getLatitude(), geoPoints.get(0).getLongitude()};

        for (int i = 0; i < geoPoints.size(); i++) {
            // Check for south/north
            if (geoPoints.get(i).getLatitude() < border[2]){
                border[2] = geoPoints.get(i).getLatitude();
            } if (geoPoints.get(i).getLatitude() > border[0]){
                border[0] = geoPoints.get(i).getLatitude();
            }
            // Check for west/east
            if (geoPoints.get(i).getLongitude() < border[3]){
                border[3] = geoPoints.get(i).getLongitude();
            } if (geoPoints.get(i).getLongitude() > border[1]){
                border[1] = geoPoints.get(i).getLongitude();
            }

        }

        return new BoundingBox(border[0],border[1],border[2],border[3]);
    }

    protected class MyInfoWindow extends InfoWindow {

        private AccEvent mAccEvent;

        private Activity motherActivity;

        private String addressForLoc;

        public MyInfoWindow(int layoutResId, MapView mapView, AccEvent mAccEvent,
                            String addressForLoc, Activity motherActivity) {
            super(layoutResId, mapView);
            this.mAccEvent = mAccEvent;
            this.addressForLoc = addressForLoc;
            this.motherActivity = motherActivity;
        }
        public void onClose() {
        }

        public void onOpen(Object arg0) {
            LinearLayout layout = (LinearLayout) mView.findViewById(R.id.bubble_layout);
            Button btnMoreInfo = (Button) mView.findViewById(R.id.bubble_moreinfo);
            TextView txtTitle = (TextView) mView.findViewById(R.id.bubble_title);
            TextView txtDescription = (TextView) mView.findViewById(R.id.bubble_description);
            TextView txtSubdescription = (TextView) mView.findViewById(R.id.bubble_subdescription);

            txtTitle.setText(getString(R.string.incidentDetectedDE));
            txtDescription.setText(addressForLoc);
            txtSubdescription.setText("You can also edit the subdescription");

            layout.setOnClickListener((View v) -> {

                Intent popUpIntent = new Intent(motherActivity,
                        IncidentPopUp.class);

                popUpIntent.putExtra("Incident_latitude",
                        (Serializable) String.valueOf(this.mAccEvent.position.getLatitude()));

                popUpIntent.putExtra("Incident_longitude",
                        (Serializable) String.valueOf(this.mAccEvent.position.getLongitude()));

                // Log.d(TAG, "this.mAccEvent.date: " + this.mAccEvent.date);
                popUpIntent.putExtra("Incident_date",
                        (Serializable) String.valueOf(this.mAccEvent.getTimeStamp()));

                //popUpIntent.putExtra("Incident_accDat",
                //        (Serializable) String.valueOf(this.mAccEvent.sensorData.getAbsolutePath()));

                popUpIntent.putExtra("Incident_accDat",
                          (Serializable) "mockSensorDatForIncident.csv");

                startActivity(popUpIntent);

                });
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
