package app.com.example.android.octeight;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ShowRouteActivity extends AppCompatActivity {

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        String date = getIntent().getStringExtra("Date");
        int state = getIntent().getIntExtra("State", 0);
        Log.d(TAG, "onCreate() PathToAccGpsFile:" + pathToAccGpsFile);

        File gpsFile = getFileStreamPath(pathToAccGpsFile);
        // File gpsFile = getFileStreamPath("59_accGps_23.01.2019 09_19_09.csv");
        // File gpsFile = getFileStreamPath("95_accGps_28.01.2019 10:42:05.csv");

        try {
            // Create a ride object with the accelerometer, gps and time data
            ride = new Ride(gpsFile, date, state);
        } catch (NullPointerException nE){
            nE.printStackTrace();
        }
        // Get the Route as a Polyline to be displayed on the map
        Polyline route = ride.getRoute();
        // Get a bounding box of the route so the view can be moved to it and the zoom can be
        // set accordingly
        double[] bounds = getBoundingBox(route);
        BoundingBox bBox = new BoundingBox(bounds[0],bounds[1],bounds[2],bounds[3]);
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

        showIncidents();

    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Event determination and representation happens here

    public void showIncidents() {

        /**for(AccEvent accE : ride.events) {

            Marker marker = new Marker(mMapView);

            ride.incidents.add(marker);

            marker.setPosition(accE.position);

        }*/

        // Create some test data


        List<AccEvent> testIncidentDat = new ArrayList<>();

        testIncidentDat.add(new AccEvent(new GeoPoint(52.517374, 13.338407),
                new Date(), null));

        testIncidentDat.add(new AccEvent(new GeoPoint(52.517592, 13.324816),
                new Date(), null));

        testIncidentDat.add(new AccEvent(new GeoPoint(52.515625, 13.320117),
                new Date(), null));

        testIncidentDat.add(new AccEvent(new GeoPoint(52.507634, 13.320117),
                new Date(), null));



        testIncidentDat = ride.getEvents();
        for (int i = 0; i < testIncidentDat.size() ; i++) {
            Log.d(TAG, String.valueOf(testIncidentDat.get(i).position.getLatitude()));
        }

        Drawable accident = getResources().getDrawable(R.drawable.accident);

        Drawable markerDefault = getResources().getDrawable(R.drawable.marker_default, null);

        for(int i = 0; i < testIncidentDat.size(); i++) {

            Marker incidentMarker = new Marker(mMapView);

            incidentMarker.setPosition(testIncidentDat.get(i).position);

            Log.d(TAG, "incidentMarker.getPosition().getLatitude(): " + incidentMarker.getPosition().getLatitude());
            Log.d(TAG, "incidentMarker.getPosition().getLongitude(): " + incidentMarker.getPosition().getLongitude());

            incidentMarker.setIcon(markerDefault);

            incidentMarker.setTitle("Vorfall " + i);

            InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mMapView,
                    testIncidentDat.get(i), ShowRouteActivity.this);
            incidentMarker.setInfoWindow(infoWindow);

            //incidentMarker.setSnippet("Vorfall " + i);

            mMapView.getOverlays().add(incidentMarker);
            mMapView.invalidate();

        }


    }

    // Returns the longitudes of the southern- and northernmost points
    // as well as the latitudes of the western- and easternmost points
    // in a double Array {South, North, West, East}
    static double[] getBoundingBox(Polyline pl){

        // {North, East, South, West}
        ArrayList<GeoPoint> geoPoints = pl.getPoints();

        double[] result = {geoPoints.get(0).getLatitude(), geoPoints.get(0).getLongitude(), geoPoints.get(0).getLatitude(), geoPoints.get(0).getLongitude()};

        for (int i = 0; i < geoPoints.size(); i++) {
            // Check for south/north
            if (geoPoints.get(i).getLatitude() < result[2]){
                result[2] = geoPoints.get(i).getLatitude()-0.005;
            } if (geoPoints.get(i).getLatitude() > result[0]){
                result[0] = geoPoints.get(i).getLatitude()+0.005;
            }
            // Check for west/east
            if (geoPoints.get(i).getLongitude() < result[3]){
                result[3] = geoPoints.get(i).getLongitude()-0.01;
            } if (geoPoints.get(i).getLongitude() > result[1]){
                result[1] = geoPoints.get(i).getLongitude()+0.01;
            }

        }

        return result;
    }

    protected class MyInfoWindow extends InfoWindow {

        private AccEvent mAccEvent;

        private Activity motherActivity;

        public MyInfoWindow(int layoutResId, MapView mapView, AccEvent mAccEvent,
                            Activity motherActivity) {
            super(layoutResId, mapView);
            this.mAccEvent = mAccEvent;
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

            txtTitle.setText("Title of my marker");
            txtDescription.setText("Click here to view details!");
            txtSubdescription.setText("You can also edit the subdescription");

            layout.setOnClickListener((View v) -> {

                Intent popUpIntent = new Intent(motherActivity,
                        IncidentPopUp.class);

                popUpIntent.putExtra("Incident_latitude",
                        (Serializable) String.valueOf(this.mAccEvent.position.getLatitude()));

                popUpIntent.putExtra("Incident_longitude",
                        (Serializable) String.valueOf(this.mAccEvent.position.getLongitude()));

                popUpIntent.putExtra("Incident_date",
                        (Serializable) String.valueOf(this.mAccEvent.date.toString()));

                //popUpIntent.putExtra("Incident_accDat",
                //        (Serializable) String.valueOf(this.mAccEvent.sensorData.getAbsolutePath()));

                popUpIntent.putExtra("Incident_accDat",
                          (Serializable) "mockSensorDatForIncident.csv");

                startActivity(popUpIntent);

                });
        }
    }

}
